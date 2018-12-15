package message;

/**
 * A Message class models every message pass across the clients and replicas with a string literal for simplicity
 */
public abstract class Message {
    /**
     * Examples:
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
     *
     * SERVER_TO_CLIENT
     *
     *      NACK Message: "SERVER_TO_CLIENT:NACK:2"
     *      note that
     *          "2" denotes the ID of current leader
     *
     *      ACK Message: "SERVER_TO_CLIENT:ACK"
     *      note that
     *          this message indicates that the client find the correct leader
     *
     *      Response Message: "SERVER_TO_CLIENT:RESPONSE:23"
     *      note that
     *          "23" denotes the sequence number of the message that send to server before
     *
     * AMONG_REPLICAS
     *
     *      HeartBeat Message: "HEART_BEAT:0:1539876988101"
     *      note that
     *          "0" is the view number and "1539876988101" is the timestamp
     *
     *      Prepare Message: "PREPARE:0:1:1539876988101:45"
     *      note that
     *          "0" denotes the round number
     *          "1" denotes the slot index,
     *          "1539876988101" means the client ID
     *          "45" means the sequence number of this client
     *
     *      Prepare Response Message: "PREPARE_RESPONSE:0:1:2:true:1539876988101:45:Hello"
     *      note that
     *          "0" denotes the round number at that log entry
     *          "1" means the slot index
     *          "2" means the server ID who send out this response message
     *          "true" represents no more accepted flag
     *          "1539876988101" indicates where is the message in the prepare request comes from
     *          "45" means that message's sequence number
     *          "hello" means the message in current slot ("|EMPTY_MESSAGE|" literal means there is nothing in that slot)
     *
     *      Accept Message: "ACCEPT:0:1:7:1539876988101:45:Hello"
     *      note that
     *          "0" represents the round number (proposal ID)
     *          "1" denotes the index
     *          "7" means the first unchosen index
     *          "1539876988101" means the client ID
     *          "45" indicates the message's sequence number
     *          "Hello" means the value
     *
     *      Accept Response Message: "ACCEPT_RESPONSE:23:10:2:1539876988101:45"
     *      note that
     *          "23" is the minimum the number of the smallest proposal this server will accept for any log entry
     *          "10" means the first unchosen index
     *          "2" indicates the server ID who send out this response message
     *          "1539876988101" means the client ID
     *          "45" indicates the message's sequence number
     *
     *      Success Message: "SUCCESS:7:Hi"
     *      note that
     *          "7" is the index that is chosen
     *          "Hi" is the message value literal
     *
     *      Success Response Message: "SUCCESS_RESPONSE:9:7:12"
     *      note that
     *          "9" is the first unchosen index after updating,
     *          "7" is the modified index
     *          "12" is the server ID who send out this response message
     */

    public enum MESSAGE_TYPE {
        CLIENT_TO_SERVER,
        SERVER_TO_CLIENT,
        HEART_BEAT,
        PREPARE,
        PREPARE_RESPONSE,
        ACCEPT,
        ACCEPT_RESPONSE,
        SUCCESS,
        SUCCESS_RESPONSE,
    }

    protected String messageLiteral;
    protected MESSAGE_TYPE messageType;

    public Message() {
        this.messageLiteral = null;
        this.messageType = null;
    }

    public Message(String messageLiteral) {
        this.messageLiteral = messageLiteral;
        this.messageType = getMessageType(messageLiteral);
    }

    public String getMessageLiteral() {
        return messageLiteral;
    }

    public MESSAGE_TYPE getMessageType() {
        return messageType;
    }

    public static MESSAGE_TYPE getMessageType(final String messageLiteral) {
        final String type = messageLiteral.split(":")[0];
        switch (type) {
            case "CLIENT_TO_SERVER":
                return MESSAGE_TYPE.CLIENT_TO_SERVER;
            case "SERVER_TO_CLIENT":
                return MESSAGE_TYPE.SERVER_TO_CLIENT;
            case "HEART_BEAT":
                return MESSAGE_TYPE.HEART_BEAT;
            case "PREPARE":
                return MESSAGE_TYPE.PREPARE;
            case "PREPARE_RESPONSE":
                return MESSAGE_TYPE.PREPARE_RESPONSE;
            case "ACCEPT":
                return MESSAGE_TYPE.ACCEPT;
            case "ACCEPT_RESPONSE":
                return MESSAGE_TYPE.ACCEPT_RESPONSE;
            case "SUCCESS":
                return MESSAGE_TYPE.SUCCESS;
            case "SUCCESS_RESPONSE":
                return MESSAGE_TYPE.SUCCESS_RESPONSE;
            default:
                throw new IllegalArgumentException("Can not detect message type!");
        }
    }
}
