package message;

/**
 * Prepare Response Message: "PREPARE_RESPONSE:0:1:2:true:1539876988101:45:Hello"
 * note that
 *      "0" denotes the round number at that log entry
 *      "1" means the slot index
 *      "2" means the server ID who send out this response message
 *      "true" represents no more accepted flag
 *      "1539876988101" indicates where is the message in the prepare request comes from
 *      "45" means that message's sequence number
 *      "hello" means the message in current slot ("|EMPTY_MESSAGE|" literal means there is nothing in that slot)
 */

public class PrepareResponseMsg extends Message {

    private final int roundNumber;
    private final int slotIndex;
    private final int responseServerID;
    private final boolean noMoreAccepted;
    private final long clientID;
    private final int messageSequenceNumber;
    private final String chatMessageLiteral;

    public PrepareResponseMsg(
            int roundNumber,
            int slotIndex,
            int responseServerID,
            boolean noMoreAccepted,
            long clientID,
            int messageSequenceNumber,
            String chatMessageLiteral
    ) {
        this.roundNumber = roundNumber;
        this.slotIndex = slotIndex;
        this.responseServerID = responseServerID;
        this.noMoreAccepted = noMoreAccepted;
        this.clientID = clientID;
        this.messageSequenceNumber = messageSequenceNumber;
        this.chatMessageLiteral = chatMessageLiteral;
        this.messageType = MESSAGE_TYPE.PREPARE_RESPONSE;
        if (chatMessageLiteral == null) {
            this.messageLiteral = new String("PREPARE_RESPONSE:" + roundNumber + ":"
                    + slotIndex + ":" + responseServerID + ":" + noMoreAccepted + ":"
                    + clientID + ":" + messageSequenceNumber + ":|EMPTY_MESSAGE|");
        } else {
            this.messageLiteral = new String("PREPARE_RESPONSE:" + roundNumber + ":"
                    + slotIndex + ":" + responseServerID + ":" + noMoreAccepted + ":"
                    + clientID + ":" + messageSequenceNumber + ":" + chatMessageLiteral);
        }
    }

    public String toString() {
        return this.messageLiteral;
    }

    public static PrepareResponseMsg fromString(final String messageLiteral) {
        final String[] subStrArr = messageLiteral.split(":");
        if (subStrArr[7].equals("|EMPTY_MESSAGE|")) {
            return new PrepareResponseMsg(
                    Integer.parseInt(subStrArr[1]),
                    Integer.parseInt(subStrArr[2]),
                    Integer.parseInt(subStrArr[3]),
                    Boolean.parseBoolean(subStrArr[4]),
                    Long.parseLong(subStrArr[5]),
                    Integer.parseInt(subStrArr[6]),
                    null);
        } else {
            final StringBuilder builder = new StringBuilder(subStrArr[7]);
            for (int i = 8; i < subStrArr.length; i++) {
                builder.append(':');
                builder.append(subStrArr[i]);
            }
            return new PrepareResponseMsg(
                    Integer.parseInt(subStrArr[1]),
                    Integer.parseInt(subStrArr[2]),
                    Integer.parseInt(subStrArr[3]),
                    Boolean.parseBoolean(subStrArr[4]),
                    Long.parseLong(subStrArr[5]),
                    Integer.parseInt(subStrArr[6]),
                    builder.toString());
        }

    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public int getResponseServerID() {
        return responseServerID;
    }

    public boolean isNoMoreAccepted() {
        return noMoreAccepted;
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
