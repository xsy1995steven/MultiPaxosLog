package message;

/**
 * Accept Message: "ACCEPT:0:1:7:1539876988101:45:Hello"
 * note that
 *      "0" represents the round number
 *      "1" denotes the index
 *      "7" means the first unchosen index
 *      "1539876988101" means the client ID
 *      "45" indicates the message's sequence number
 *      "Hello" means the value
 */
public class AcceptMsg extends Message {

    private final int roundNumber;
    private final int slotIndex;
    private final int firstUnchosenIndex;
    private final long clientID;
    private final int messageSequenceNumber;
    private final String chatMessageLiteral;

    public AcceptMsg(
            int roundNumber,
            int slotIndex,
            int firstUnchosenIndex,
            long clientID,
            int messageSequenceNumber,
            String chatMessageLiteral
    ) {
        this.roundNumber = roundNumber;
        this.slotIndex = slotIndex;
        this.firstUnchosenIndex = firstUnchosenIndex;
        this.clientID = clientID;
        this.messageSequenceNumber = messageSequenceNumber;
        this.chatMessageLiteral = chatMessageLiteral;
        this.messageType = MESSAGE_TYPE.ACCEPT;
        this.messageLiteral = new String("ACCEPT:" + roundNumber + ":" + slotIndex + ":" +
                firstUnchosenIndex + ":" + clientID + ":" + messageSequenceNumber + ":" + chatMessageLiteral);
    }

    public String toString() {
        return this.messageLiteral;
    }

    public static AcceptMsg fromString(final String messageLiteral) {
        final String[] subStrArr = messageLiteral.split(":");
        final StringBuilder builder = new StringBuilder(subStrArr[6]);
        for (int i = 7; i < subStrArr.length; i++) {
            builder.append(':');
            builder.append(subStrArr[i]);
        }
        return new AcceptMsg(
                Integer.parseInt(subStrArr[1]),
                Integer.parseInt(subStrArr[2]),
                Integer.parseInt(subStrArr[3]),
                Long.parseLong(subStrArr[4]),
                Integer.parseInt(subStrArr[5]),
                builder.toString());
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public int getFirstUnchosenIndex() {
        return firstUnchosenIndex;
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
