package service;

import message.*;
import thread.HeartBeatTracker;
import thread.ThreadHandler;
import util.AddressPortPair;
import util.ChatMessageIdentifier;
import util.LogEntrySlotManager;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * A Paxos Replica instance that utilize Multi-Paxos to make consensus among other replicas.
 */
public class PaxosLogServer {

    private final static int HEART_BEAT_PERIOD_MILLS = 2000;

    private final int serverId;
    private final String serverAddr;
    private final int serverPort;

    private volatile boolean isLeader;
    private int viewNumber;

    private final int numOfToleratedFailures;
    private final int totalNumOfReplicas;
    private final List<AddressPortPair> allReplicasInfo;

    private final int skipSlotSeqNum;
    private final double messageLossRate;

    // all receive sockets accepted from the server socket
    private final List<Socket> allReceiveSockets;

    // for allReplicaSendSockets, the key is the replica ID and value is the socket used to send message to other replicas
    private final Map<Integer, Socket> allReplicaSendSockets;

    // for allClientSendSockets, the key is the client ID and value is the socket used to send message to client
    private final Map<Long, Socket> allClientSendSockets;

    // a thread safe message queue caching all messages from all other replicas
    private final Queue<String> replicasMessageQueue;

    // a thread safe message queue caching all messages from all connected clients
    // note that the client chat message can only be send to the leader replica
    private final Queue<String> clientChatMessageQueue;

    // a worker that track the heartbeat from the leader
    private final HeartBeatTracker tracker;

    //  a manager that manage every log entry of the replica
    private final LogEntrySlotManager logEntrySlotManager;

    // use a set to make sure a message can only be executed once
    private final Set<ChatMessageIdentifier> chosenChatMessages;

    private ClientToServerMsg.ChatMsg nextChatMsg;
    private boolean prepared;
    private ClientToServerMsg.ChatMsg writeValueThisTime;
    private int currentIndex;
    private int nextIndex;
    private int curProposalNumber;
    private int maxRound;
    private Set<Integer> receivedDistinctPrepareResponse;
    private Set<Integer> receivedDistinctNoMoreAccepted;
    private Set<Integer> receivedDistinctAcceptResponse;

    // use for simulate message drop
    private final Random randomGenerator;

    public PaxosLogServer(
            final int serverId,
            final String serverAddr,
            final int serverPort,
            boolean isLeader,
            int viewNumber,
            final int numOfToleratedFailures,
            final List<AddressPortPair> allReplicasInfo,
            final int skipSlotSeqNum,
            final double messageLossRate) {
        this.serverId = serverId;
        this.serverAddr = serverAddr;
        this.serverPort = serverPort;
        this.isLeader = isLeader;
        this.viewNumber = viewNumber;
        this.numOfToleratedFailures = numOfToleratedFailures;
        this.totalNumOfReplicas = numOfToleratedFailures * 2 + 1;
        this.allReplicasInfo = allReplicasInfo;
        this.skipSlotSeqNum = skipSlotSeqNum;
        this.messageLossRate = messageLossRate;
        this.allReceiveSockets = new Vector<>();
        this.allReplicaSendSockets = new ConcurrentHashMap<>();
        this.allClientSendSockets = new ConcurrentHashMap<>();
        this.replicasMessageQueue = new ConcurrentLinkedQueue<>();
        this.clientChatMessageQueue = new ConcurrentLinkedQueue<>();
        this.tracker = new HeartBeatTracker(
                this::increaseViewNumber,
                this::tryToBecomeLeader,
                System.currentTimeMillis(),
                HEART_BEAT_PERIOD_MILLS);
        this.logEntrySlotManager = new LogEntrySlotManager(this);
        this.chosenChatMessages = new HashSet<>();
        this.prepared = false;
        this.currentIndex = 0;
        this.nextIndex = 1;
        this.curProposalNumber = 0;
        this.maxRound = 0;
        this.randomGenerator = new Random(10);
        System.out.println("Server with ID: " + serverId + " initialize at address: " + serverAddr + ':' + serverPort);
    }

    /**
     * Entrance of the server
     */
    public void start() {
        final Thread inComingSocketHandler = new Thread(new IncomingSocketHandler(serverPort));
        final Thread heartBeatLogger = new Thread(new HeartBeatLogger());
        inComingSocketHandler.start();  // start listing to its port for incoming sockets
        createSendSocketsForReplicasIfNecessary();  // try to connect all other replicas at beginning
        heartBeatLogger.start();    // start heartbeat logger
        tracker.start();    // start heartbeat tracker
        while (true) {
            if (isLeader) {
                actAsLeader();
            } else {
                actAsAcceptor();
            }
        }
    }

    /**
     * Create replica sending sockets if we don't have 2f such sockets or some such sockets are died
     */
    private void createSendSocketsForReplicasIfNecessary() {
        // If all replicas sending sockets are alive and # of those equal to 2f, we no longer
        if (areAllSendSocketsAlive() && allReplicaSendSockets.size() == totalNumOfReplicas) {
            return;
        } else {
            createSendSocketsForReplicas();
        }
    }

    /**
     * Create replica sending sockets if we don't have that connection and save it to allReplicaSendSockets
     */
    private void createSendSocketsForReplicas() {
        for (int i = 0; i < allReplicasInfo.size(); i++) {
            try {
                if (!allReplicaSendSockets.containsKey(i)) {
                    final Socket socket = new Socket(allReplicasInfo.get(i).getIp(), allReplicasInfo.get(i).getPort());
                    if (socket != null && socket.isConnected()) {
                        allReplicaSendSockets.put(i, socket);
                    }
                }
            } catch (Exception e) {
                System.out.println("Replica whose address is " + allReplicasInfo.get(i).getIp()
                        + ':' + allReplicasInfo.get(i).getPort() + " is not accessible now");
            }
        }
    }

    /**
     * @return Whether all sockets in allReplicaSendSockets are alive
     */
    private boolean areAllSendSocketsAlive() {
        if (allReplicaSendSockets.size() == 0) {
            return false;
        }
        for (final Integer replicaID : this.allReplicaSendSockets.keySet()) {
            if (!allReplicaSendSockets.get(replicaID).isConnected()) {
                allReplicaSendSockets.remove(replicaID);   // remove the dead sockets if necessary
                return false;
            }
        }
        return true;
    }

    /**
     * A worker for listening the port of server and create receiving sockets for any incoming sockets (client & other replicas)
     * Note that the sockets created in this worker is only responsible for receiving messages
     */
    public class IncomingSocketHandler implements Runnable {

        private ServerSocket serverSocket;

        public IncomingSocketHandler(final int port) {
            try {
                serverSocket = new ServerSocket(port);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Server with ID: " + serverId + "fail to listen to port:" + serverPort + ". Terminating...");
                System.exit(1);
            }
        }

        @Override
        public void run() {
            while (true) {
                try {
                    Socket acceptedSocket = serverSocket.accept();
                    allReceiveSockets.add(acceptedSocket);
                    new Thread(new ReceiveMessageHandler(acceptedSocket)).start();
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Server with ID: " + serverId + "fail to accept connection");
                }
            }
        }
    }

    /**
     * A worker for receive messages from each socket which is responsible for receiving messages
     * According to the different message received (client, heartbeat or replica message), perform different operations
     */
    public class ReceiveMessageHandler extends ThreadHandler {

        public ReceiveMessageHandler(Socket socket) {
            super(socket);
        }

        @Override
        public void run() {
            try {
                String line;
                while ((line = super.bufferedReader.readLine()) != null) {
                    System.out.println(line);
                    switch (Message.getMessageType(line)) {
                        case CLIENT_TO_SERVER:
                            switch (ClientToServerMsg.getClientToServerType(line)) {
                                case HELLO:
                                    handleClientHello(ClientToServerMsg.HelloMsg.fromString(line));
                                    break;
                                case CHAT:
                                    clientChatMessageQueue.offer(line);
                                    break;
                                default:
                                    throw new IllegalStateException("Unresolvable client to server message!");
                            }
                            break;
                        case SERVER_TO_CLIENT:
                            throw new IllegalStateException("Server should never receive the message that supposed to be sent to client!");
                        case HEART_BEAT:
                            handleClientHeartBeat(HeartBeatMsg.fromString(line));
                            break;
                        case PREPARE:
                        case PREPARE_RESPONSE:
                        case ACCEPT:
                        case ACCEPT_RESPONSE:
                        case SUCCESS:
                        case SUCCESS_RESPONSE:
                            replicasMessageQueue.offer(line);
                            break;
                        default:
                            throw new IllegalStateException("Unresolvable message received!");
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void handleClientHeartBeat(final HeartBeatMsg heartBeatMsg) {
        updateViewNumber(heartBeatMsg.getViewNumber());
        tracker.setLatestReceivedTimeStamp(heartBeatMsg.getTimeStamp());
    }

    /**
     * Handle the hello message from client, if the current replica is leader, it should send ACK, or rather NACK.
     * Also every time we receive such message, if we haven't connect to that client before, create a new send socket for that client.
     *
     * @param helloMsg
     */
    private void handleClientHello(final ClientToServerMsg.HelloMsg helloMsg) {
        if (!allClientSendSockets.containsKey(helloMsg.getClientID())) {
            try {
                final Socket clientSocket = new Socket(helloMsg.getListeningIPAddr(), helloMsg.getListeningPort());
                if (clientSocket != null && clientSocket.isConnected()) {
                    allClientSendSockets.put(helloMsg.getClientID(), clientSocket);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Failed to connect to client with " + helloMsg.getListeningIPAddr() + ":" + helloMsg.getListeningPort());
            }
        }
        final Socket clientSocket = allClientSendSockets.get(helloMsg.getClientID());
        try {
            final PrintWriter writer = new PrintWriter(clientSocket.getOutputStream(), true);
            if (isLeader) {
                if (randomGenerator.nextFloat() >= messageLossRate) {
                    writer.println(new ServerToClientMsg.ServerAckMsg().toString());
                }
            } else {
                if (randomGenerator.nextFloat() >= messageLossRate) {
                    writer.println(new ServerToClientMsg.ServerNackMsg(getCurrentLeader()).toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Fail to send message to client");
        }
    }

    private void increaseViewNumber() {
        this.viewNumber += 1;
    }

    private void updateViewNumber(final int newViewNumber) {
        this.viewNumber = newViewNumber > this.viewNumber ? newViewNumber : this.viewNumber;
    }

    private void tryToBecomeLeader() {
        if (getCurrentLeader() == this.serverId) {
            System.out.println("Proposer " + serverId + " is trying to become leader");
            this.isLeader = true;
        }
    }

    private int getCurrentLeader() {
        return this.viewNumber % this.totalNumOfReplicas;
    }

    /**
     * Broadcast a message to all replicas (include itself) through send replica socket.
     *
     * @param message
     * @throws IOException
     */
    private void broadcastToAllReplicas(final String message) throws IOException {
        createSendSocketsForReplicasIfNecessary();
        for (final Socket replicaSendSocket : allReplicaSendSockets.values()) {
            final PrintWriter writer = new PrintWriter(replicaSendSocket.getOutputStream(), true);
            writer.println(message);
        }
    }

    /**
     * Multicast a message to all other replicas through send replica socket.
     *
     * @param message
     * @throws IOException
     */
    private void multicastToAllOtherReplicas(final String message) throws IOException {
        createSendSocketsForReplicasIfNecessary();
        for (final Integer replicaID : allReplicaSendSockets.keySet()) {
            if (replicaID == this.serverId) {
                continue;
            }
            final PrintWriter writer = new PrintWriter(allReplicaSendSockets.get(replicaID).getOutputStream(), true);
            if (randomGenerator.nextFloat() >= messageLossRate) {
                writer.println(message);
            }
        }
    }

    /**
     * If the current process is leader, it will send heartbeat messages to all other replicas periodically.
     */
    public class HeartBeatLogger implements Runnable {

        @Override
        public void run() {
            while (true) {
                if (getCurrentLeader() != serverId) {
                    isLeader = false;
                }
                if (isLeader) {
                    try {
                        final long currentTimeStamp = System.currentTimeMillis();
                        broadcastToAllReplicas("HEART_BEAT:" + viewNumber + ":" + currentTimeStamp);
                        Thread.sleep(HEART_BEAT_PERIOD_MILLS);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /**
     * If a replica is a leader, it should only behave as a leader.
     * The functionality of a leader includes send out heartbeats to all other replicas, handle client chat message and
     * respond to client, send out PREPARE, ACCEPT and SUCCESS messages and handle the response of PREPARE, ACCEPT and SUCCESS.
     */
    private void actAsLeader() {
        while (isLeader) {
            String nextString = clientChatMessageQueue.poll();
            while (nextString == null) {
                nextString = clientChatMessageQueue.poll();
            }
            if (Message.getMessageType(nextString) == Message.MESSAGE_TYPE.CLIENT_TO_SERVER) {
                if (ClientToServerMsg.getClientToServerType(nextString) == ClientToServerMsg.CLIENT_TO_SERVER_TYPE.CHAT) {
                    nextChatMsg = ClientToServerMsg.ChatMsg.fromString(nextString);
                } else {
                    throw new IllegalStateException("Illegal message type of CLIENT_TO_SERVER in clientChatMegQueue");
                }
            } else {
                throw new IllegalStateException("Illegal message type in clientChatMegQueue");
            }

            // we should never execute the message that is already executed before
            if (chosenChatMessages.contains(new ChatMessageIdentifier(nextChatMsg.getClientID(), nextChatMsg.getMessageSequenceNumber()))) {
                sendResponseBackToClient();
                break;
            }
            if (proposeValue(nextChatMsg)) {
                sendResponseBackToClient();
            } else {
                try {
                    PrintWriter ClientPrintWriter = new PrintWriter(allClientSendSockets.get(nextChatMsg.getClientID()).getOutputStream(), true);
                    if (randomGenerator.nextFloat() >= messageLossRate) {
                        ClientPrintWriter.println(new ServerToClientMsg.ServerNackMsg(getCurrentLeader()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.printf("fail to build printWriter to client ID : %s", nextChatMsg.getClientID());
                }

            }
        }
    }

    private void sendResponseBackToClient() {
        try {
            PrintWriter ClientPrintWriter = new PrintWriter(allClientSendSockets.get(nextChatMsg.getClientID()).getOutputStream(), true);
            if (randomGenerator.nextFloat() >= messageLossRate) {
                ClientPrintWriter.println(new ServerToClientMsg.ServerResponseMsg(nextChatMsg.getMessageSequenceNumber()));
            }
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("fail to build printWriter to client ID: " + nextChatMsg.getClientID());
        }
    }

    public boolean proposeValue(ClientToServerMsg.ChatMsg InputValue) {
        if (!isLeader) {
            return false;
        }
        return proposeValueInNextIndex(InputValue);
    }

    public boolean proposeValueInNextIndex(ClientToServerMsg.ChatMsg InputValue) {
        writeValueThisTime = InputValue;
        if (prepared == true) {
            currentIndex = nextIndex;
            nextIndex += 1;
            if (currentIndex == skipSlotSeqNum) {
                currentIndex += 1;
                nextIndex += 1;
            }
            return handleWhenPrepared(InputValue);
        } else {
            sendPrepare(InputValue);
        }
        handlePrepareResponse(InputValue);
        return handleWhenPrepared(InputValue);
    }

    public boolean handleWhenPrepared(ClientToServerMsg.ChatMsg InputValue) {
        sendAccept(InputValue);
        handleAcceptResponse(InputValue);
        if (prepared == false && curProposalNumber < maxRound) {
            return proposeValue(InputValue);
        }
        if (InputValue.equals(writeValueThisTime)) {
            return true;
        } else {
            return proposeValueInNextIndex(InputValue);
        }
    }

    public void sendPrepare(ClientToServerMsg.ChatMsg InputValue) {
        currentIndex = logEntrySlotManager.getFirstUnchosenIndex();
        nextIndex = currentIndex + 1;
        if (currentIndex == skipSlotSeqNum) {
            currentIndex += 1;
            nextIndex += 1;
        }
        curProposalNumber = maxRound + 1;
        maxRound += 1;
        receivedDistinctPrepareResponse = new HashSet<>();
        receivedDistinctNoMoreAccepted = new HashSet<>();

        PrepareMsg SendPrepareMsg = new PrepareMsg(curProposalNumber, currentIndex, InputValue.getClientID(), InputValue.getMessageSequenceNumber());
        new WaitRepeatSendPrepare(SendPrepareMsg, 20);
    }


    public void handlePrepareResponse(ClientToServerMsg.ChatMsg InputValue) {
        int maxReplyAcceptedProposal = 0;
        while (receivedDistinctPrepareResponse.size() < numOfToleratedFailures) {

            String ReceivedMsg = replicasMessageQueue.poll();
            while (ReceivedMsg == null) {
                ReceivedMsg = replicasMessageQueue.poll();
            }
            if (Message.getMessageType(ReceivedMsg) == Message.MESSAGE_TYPE.PREPARE_RESPONSE) {
                PrepareResponseMsg ReceivedPreparedResponse = PrepareResponseMsg.fromString(ReceivedMsg);

                if (ReceivedPreparedResponse.getClientID() == InputValue.getClientID()
                        && ReceivedPreparedResponse.getMessageSequenceNumber() == InputValue.getMessageSequenceNumber()
                        && ReceivedPreparedResponse.getSlotIndex() == currentIndex) {

                    receivedDistinctPrepareResponse.add(ReceivedPreparedResponse.getResponseServerID());
                    if (ReceivedPreparedResponse.getRoundNumber() > maxReplyAcceptedProposal) {
                        maxReplyAcceptedProposal = ReceivedPreparedResponse.getRoundNumber();
                        writeValueThisTime = new ClientToServerMsg.ChatMsg(ReceivedPreparedResponse.getClientID(), ReceivedPreparedResponse.getMessageSequenceNumber(), ReceivedPreparedResponse.getChatMessageLiteral());///
                    }
                    if (ReceivedPreparedResponse.isNoMoreAccepted()) {
                        receivedDistinctNoMoreAccepted.add(ReceivedPreparedResponse.getResponseServerID());
                    }
                    if (receivedDistinctNoMoreAccepted.size() >= numOfToleratedFailures) {
                        prepared = true;
                    }
                } else {
                    if (ReceivedPreparedResponse.getSlotIndex() == currentIndex) {
                        System.out.println("received prepare response with inconsistent ClientID and MessageSequence Number");
                    }
                }
            }
        }
    }

    public void sendAccept(ClientToServerMsg.ChatMsg InputValue) {
        receivedDistinctAcceptResponse = new HashSet<>();
        AcceptMsg sendAcceptMsg = new AcceptMsg(curProposalNumber, currentIndex,
                logEntrySlotManager.getFirstUnchosenIndex(), writeValueThisTime.getClientID(),
                writeValueThisTime.getMessageSequenceNumber(), writeValueThisTime.getChatMessageLiteral());
        new WaitRepeatSendAccept(sendAcceptMsg, 20);
    }

    public void handleAcceptResponse(ClientToServerMsg.ChatMsg InputValue) {
        while (receivedDistinctAcceptResponse.size() < numOfToleratedFailures) {
            String ReceivedMsg = replicasMessageQueue.poll();
            while (ReceivedMsg == null) {
                ReceivedMsg = replicasMessageQueue.poll();
            }
            if (Message.getMessageType(ReceivedMsg) == Message.MESSAGE_TYPE.ACCEPT_RESPONSE) {
                AcceptResponseMsg ReceivedAcceptResponse = AcceptResponseMsg.fromString(ReceivedMsg);
                if (ReceivedAcceptResponse.getClientID() == writeValueThisTime.getClientID()
                        && ReceivedAcceptResponse.getMessageSequenceNumber() == writeValueThisTime.getMessageSequenceNumber()) {
                    receivedDistinctAcceptResponse.add(ReceivedAcceptResponse.getResponseServerID());
                    if (ReceivedAcceptResponse.getMinProposal() > curProposalNumber) {
                        maxRound = ReceivedAcceptResponse.getMinProposal();
                        prepared = false;
                        return;
                    }
                    if (ReceivedAcceptResponse.getFirstUnchosenIndex() <= logEntrySlotManager.getLastLogIndex() && logEntrySlotManager.isEntryChosen(ReceivedAcceptResponse.getFirstUnchosenIndex())) {
                        try {
                            PrintWriter SuccessPrintWriter = new PrintWriter(allReplicaSendSockets.get(ReceivedAcceptResponse.getResponseServerID()).getOutputStream(), true);
                            for (int sendSuccessIndex = ReceivedAcceptResponse.getFirstUnchosenIndex(); sendSuccessIndex < logEntrySlotManager.getFirstUnchosenIndex(); sendSuccessIndex++) {
                                if (randomGenerator.nextFloat() >= messageLossRate) {
                                    SuccessPrintWriter.println(new SuccessMsg(sendSuccessIndex, logEntrySlotManager.getLogEntryValue(sendSuccessIndex)));
                                }
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            System.out.printf("fail to build printwriter to replica ID: %s", ReceivedAcceptResponse.getResponseServerID());
                        }
                    }
                } else {
                    System.out.println("received accept response with inconsistent ClientID and MessageSequence Number");
                }
            }
        }
        logEntrySlotManager.insertLogEntry(currentIndex, curProposalNumber, writeValueThisTime.getChatMessageLiteral());
        logEntrySlotManager.chooseLogEntry(currentIndex);
        logEntrySlotManager.write();
        chosenChatMessages.add(new ChatMessageIdentifier(writeValueThisTime.getClientID(), writeValueThisTime.getMessageSequenceNumber()));
        try {
            multicastToAllOtherReplicas((new SuccessMsg(currentIndex, writeValueThisTime.getChatMessageLiteral())).toString());
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Error multicast success!");
        }
    }

    /**
     * A scheduled task to resend PREPARE message when timeout (timeout but not received PREPARE RESPONSE from majority)
     */
    public class WaitRepeatSendPrepareTask extends TimerTask {

        final PrepareMsg sendPrepareMsg;
        final int MessageSeqNum;

        public WaitRepeatSendPrepareTask(PrepareMsg SendPrepareMsg) {
            this.sendPrepareMsg = SendPrepareMsg;
            this.MessageSeqNum = SendPrepareMsg.getMessageSequenceNumber();
        }

        @Override
        public void run() {

            if (MessageSeqNum == nextChatMsg.getMessageSequenceNumber() && receivedDistinctPrepareResponse.size() < numOfToleratedFailures) {
                try {
                    multicastToAllOtherReplicas(sendPrepareMsg.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Multicast Prepare Message Failed!");
                }
            } else {
                cancel();
            }
        }
    }

    public class WaitRepeatSendPrepare {
        final Timer timer;

        public WaitRepeatSendPrepare(PrepareMsg SendPrepareMsg, int milliseconds) {
            timer = new Timer();
            Calendar calendar = Calendar.getInstance();
            Date time = calendar.getTime();
            timer.schedule(new WaitRepeatSendPrepareTask(SendPrepareMsg), time, milliseconds * 1000);
        }
    }


    /**
     * A scheduled task to resend ACCEPT message when timeout (timeout but not received ACCEPT RESPONSE from majority)
     */
    public class WaitRepeatSendAcceptTask extends TimerTask {
        final AcceptMsg sendAcceptMsg;
        final int MessageSequenceNum;

        public WaitRepeatSendAcceptTask(AcceptMsg sendAcceptMsg) {
            this.sendAcceptMsg = sendAcceptMsg;
            this.MessageSequenceNum = sendAcceptMsg.getMessageSequenceNumber();
        }

        @Override
        public void run() {
            if (nextChatMsg.getMessageSequenceNumber() == MessageSequenceNum && receivedDistinctAcceptResponse.size() < numOfToleratedFailures) {
                try {
                    multicastToAllOtherReplicas(sendAcceptMsg.toString());
                } catch (IOException e) {
                    e.printStackTrace();
                    System.out.println("Multicast Accept Message Failed!");
                }
            } else {
                cancel();
            }
        }
    }

    public class WaitRepeatSendAccept {
        final Timer timer;

        public WaitRepeatSendAccept(AcceptMsg sendAcceptMsg, int milliseconds) {
            timer = new Timer();
            Calendar calendar = Calendar.getInstance();
            Date time = calendar.getTime();
            timer.schedule(new WaitRepeatSendAcceptTask(sendAcceptMsg), time, milliseconds * 1000);
        }
    }

    /**
     * If a replica is a acceptor, it should only behave as a acceptor.
     * The functionality of a acceptor includes track heartbeats and try to become leader when necessary. As an acceptor,
     * they need to handle the PREPARE, ACCEPT and SUCCESS messages from leader and behave accordingly. They should also
     * generate and send response back to leader.
     */
    private void actAsAcceptor() {
        while (!isLeader) {
            final String currentMessage = replicasMessageQueue.poll();
            if (currentMessage == null) {
                continue;
            }
            final Message.MESSAGE_TYPE currentType = Message.getMessageType(currentMessage);
            if (currentType.equals(Message.MESSAGE_TYPE.PREPARE_RESPONSE)
                    || currentType.equals(Message.MESSAGE_TYPE.ACCEPT_RESPONSE)
                    || currentType.equals(Message.MESSAGE_TYPE.SUCCESS_RESPONSE)) {
                continue;
            } else {
                if (currentType.equals(Message.MESSAGE_TYPE.PREPARE)) {
                    handlePrepareMessage(currentMessage);
                } else if (currentType.equals(Message.MESSAGE_TYPE.ACCEPT)) {
                    handleAcceptMessage(currentMessage);
                } else if (currentType.equals(Message.MESSAGE_TYPE.SUCCESS)) {
                    handleSuccessMessage(currentMessage);
                }
            }
        }
    }

    private void handlePrepareMessage(final String currentMessage) {
        final PrepareMsg prepareMsg = PrepareMsg.fromString(currentMessage);
        maxRound = Integer.max(maxRound, prepareMsg.getRoundNumber());
        if (prepareMsg.getRoundNumber() >= logEntrySlotManager.getMinProposal()) {
            logEntrySlotManager.setMinProposal(prepareMsg.getRoundNumber());
            final PrepareResponseMsg prepareResponseMsg = new PrepareResponseMsg(
                    logEntrySlotManager.getProposalID(prepareMsg.getSlotIndex()),
                    prepareMsg.getSlotIndex(),
                    this.serverId,
                    prepareMsg.getSlotIndex() > logEntrySlotManager.getLastLogIndex(),
                    prepareMsg.getClientID(),
                    prepareMsg.getMessageSequenceNumber(),
                    logEntrySlotManager.getLogEntryValue(prepareMsg.getSlotIndex())
            );
            final Socket sendSocket = allReplicaSendSockets.get(getCurrentLeader());
            try {
                final PrintWriter writer = new PrintWriter(sendSocket.getOutputStream(), true);
                if (randomGenerator.nextFloat() >= messageLossRate) {
                    writer.println(prepareResponseMsg.toString());
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Fail to send prepare response to leader!");
            }
        }
    }

    private void handleAcceptMessage(final String currentMessage) {
        final AcceptMsg acceptMsg = AcceptMsg.fromString(currentMessage);
        if (acceptMsg.getRoundNumber() >= logEntrySlotManager.getMinProposal()) {
            logEntrySlotManager.setMinProposal(acceptMsg.getRoundNumber());
            logEntrySlotManager.insertLogEntry(acceptMsg.getSlotIndex(), acceptMsg.getRoundNumber(), acceptMsg.getChatMessageLiteral());
            for (int i = 0; i < logEntrySlotManager.getFirstUnchosenIndex(); i++) {
                if (logEntrySlotManager.getProposalID(i) == acceptMsg.getRoundNumber()) {
                    logEntrySlotManager.chooseLogEntry(i);
                }
            }
            final AcceptResponseMsg acceptResponseMsg = new AcceptResponseMsg(
                    logEntrySlotManager.getMinProposal(),
                    logEntrySlotManager.getFirstUnchosenIndex(),
                    this.serverId,
                    acceptMsg.getClientID(),
                    acceptMsg.getMessageSequenceNumber()
            );
            final Socket sendSocket = allReplicaSendSockets.get(getCurrentLeader());
            try {
                final PrintWriter writer = new PrintWriter(sendSocket.getOutputStream(), true);
                if (randomGenerator.nextFloat() >= messageLossRate) {
                    writer.println(acceptResponseMsg.toString());

                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("Fail to send accept response to leader!");
            }
        }
    }

    private void handleSuccessMessage(final String currentMessage) {
        final SuccessMsg successMsg = SuccessMsg.fromString(currentMessage);
        if (!logEntrySlotManager.isEntryChosen(successMsg.getSlotIndex())) {
            logEntrySlotManager.successLogEntry(successMsg.getSlotIndex(), successMsg.getChatMessageLiteral());
            final SuccessResponseMsg successResponseMsg = new SuccessResponseMsg(
                    logEntrySlotManager.getFirstUnchosenIndex(),
                    successMsg.getSlotIndex(),
                    this.serverId
            );
            logEntrySlotManager.write();
        }
    }

    public int getServerId() {
        return serverId;
    }

    public boolean isLeader() {
        return isLeader;
    }

    public int getSkipSlotSeqNum() {
        return skipSlotSeqNum;
    }
}
