package message;

/**
 * CLIENT_TO_SERVER
 *
 *      Hello Message: "CLIENT_TO_SERVER:HELLO:1539876988101:68.232.15.233:28779"
 *      note that
 *          "1539876988101" denotes the client ID
 *          "68.232.15.233" and "28779" denotes the IP address and port number for which the client is listening
 *
 *      Chat Message: "CLIENT_TO_SERVER:CHAT:1539876988101:23:Hi there!"
 *      note that
 *          "1539876988101" denotes the client ID
 *          "23" represents the sequence number of current message
 *          "Hi there" is the message literal
 */

public class ClientToServerMsg extends Message {

    public enum CLIENT_TO_SERVER_TYPE {
        HELLO,
        CHAT,
    }

    public static CLIENT_TO_SERVER_TYPE getClientToServerType(final String messageLiteral) {
        final String subType = messageLiteral.split(":")[1];
        switch (subType) {
            case "HELLO":
                return CLIENT_TO_SERVER_TYPE.HELLO;
            case "CHAT":
                return CLIENT_TO_SERVER_TYPE.CHAT;
            default:
                throw new IllegalArgumentException("Can not detect message type!");
        }
    }

    public static class HelloMsg extends Message {

        private final long clientID;
        private final String listeningIPAddr;
        private final int listeningPort;

        public HelloMsg(long clientID, String listeningIPAddr, int listeningPort) {
            this.clientID = clientID;
            this.listeningIPAddr = listeningIPAddr;
            this.listeningPort = listeningPort;
            this.messageType = MESSAGE_TYPE.CLIENT_TO_SERVER;
            this.messageLiteral = new String("CLIENT_TO_SERVER:HELLO:" + clientID + ":" + listeningIPAddr + ":" + listeningPort);
        }

        public String toString() {
            return this.messageLiteral;
        }

        public static HelloMsg fromString(final String messageLiteral) {
            final String[] subStrArr = messageLiteral.split(":");
            return new HelloMsg(Long.parseLong(subStrArr[2]), subStrArr[3], Integer.parseInt(subStrArr[4]));
        }

        public long getClientID() {
            return clientID;
        }

        public String getListeningIPAddr() {
            return listeningIPAddr;
        }

        public int getListeningPort() {
            return listeningPort;
        }
    }

    public static class ChatMsg extends Message {
        private final long clientID;
        private final int messageSequenceNumber;
        private final String chatMessageLiteral;

        public ChatMsg(long clientID, int messageSequenceNumber, String chatMessageLiteral) {
            this.clientID = clientID;
            this.messageSequenceNumber = messageSequenceNumber;
            this.chatMessageLiteral = chatMessageLiteral;
            this.messageType = MESSAGE_TYPE.CLIENT_TO_SERVER;
            this.messageLiteral = new String("CLIENT_TO_SERVER:CHAT:" + clientID + ":" + messageSequenceNumber + ":" + chatMessageLiteral);
        }

        public String toString() {
            return this.messageLiteral;
        }

        public static ChatMsg fromString(final String messageLiteral) {
            final String[] subStrArr = messageLiteral.split(":");
            final StringBuilder builder = new StringBuilder(subStrArr[4]);
            for (int i = 5; i < subStrArr.length; i++) {
                builder.append(':');
                builder.append(subStrArr[i]);
            }
            return new ChatMsg(Long.parseLong(subStrArr[2]), Integer.parseInt(subStrArr[3]), builder.toString());
        }

        public long getClientID() {
            return clientID;
        }

        public int getMessageSequenceNumber() {
            return messageSequenceNumber;
        }

        public String getChatMessageLiteral() {
            return chatMessageLiteral;
        }
    }

}
