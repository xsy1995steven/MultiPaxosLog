package message;

/**
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
 */

public class ServerToClientMsg extends Message {

    public enum SERVER_TO_CLIENT_TYPE {
        NACK,
        ACK,
        RESPONSE,
    }

    public static SERVER_TO_CLIENT_TYPE getServerToClientType(final String messageLiteral) {
        final String subType = messageLiteral.split(":")[1];
        switch (subType) {
            case "NACK":
                return SERVER_TO_CLIENT_TYPE.NACK;
            case "ACK":
                return SERVER_TO_CLIENT_TYPE.ACK;
            case "RESPONSE":
                return SERVER_TO_CLIENT_TYPE.RESPONSE;
            default:
                throw new IllegalArgumentException("Can not detect message type!");
        }
    }

    public static class ServerNackMsg extends Message {
        private final int currentLeaderId;

        public ServerNackMsg(int currentLeaderId) {
            this.currentLeaderId = currentLeaderId;
            this.messageType = MESSAGE_TYPE.SERVER_TO_CLIENT;
            this.messageLiteral = new String("SERVER_TO_CLIENT:NACK:" + currentLeaderId);
        }

        public String toString() {
            return this.messageLiteral;
        }

        public static ServerNackMsg fromString(final String messageLiteral) {
            final String[] subStrArr = messageLiteral.split(":");
            return new ServerNackMsg(Integer.parseInt(subStrArr[2]));
        }

        public int getCurrentLeaderId() {
            return currentLeaderId;
        }
    }

    public static class ServerAckMsg extends Message {

        public ServerAckMsg() {
            this.messageType = MESSAGE_TYPE.SERVER_TO_CLIENT;
            this.messageLiteral = new String("SERVER_TO_CLIENT:ACK");
        }

        public String toString() {
            return this.messageLiteral;
        }

        public static ServerAckMsg fromString(final String messageLiteral) {
            return new ServerAckMsg();
        }
    }

    public static class ServerResponseMsg extends Message {
        private final int messageSequenceNumber;

        public ServerResponseMsg(int messageSequenceNumber) {
            this.messageSequenceNumber = messageSequenceNumber;
            this.messageType = MESSAGE_TYPE.SERVER_TO_CLIENT;
            this.messageLiteral = new String("SERVER_TO_CLIENT:RESPONSE:" + messageSequenceNumber);
        }

        public String toString() {
            return this.messageLiteral;
        }

        public static ServerResponseMsg fromString(final String messageLiteral) {
            final String[] subStrArr = messageLiteral.split(":");
            return new ServerResponseMsg(Integer.parseInt(subStrArr[2]));
        }

        public int getMessageSequenceNumber() {
            return messageSequenceNumber;
        }
    }

}
