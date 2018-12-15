package message;

/**
 * Prepare Message: "PREPARE:0:1:1539876988101:45"
 * note that
 *      "0" denotes the round number
 *      "1" denotes the slot index,
 *      "1539876988101" means the client ID
 *      "45" means the sequence number of this client
 */

public class PrepareMsg extends Message {

    private final int roundNumber;
    private final int slotIndex;
    private final long clientID;
    private final int messageSequenceNumber;

    public PrepareMsg(int roundNumber, int slotIndex, long clientID, int messageSequenceNumber) {
        this.roundNumber = roundNumber;
        this.slotIndex = slotIndex;
        this.clientID = clientID;
        this.messageSequenceNumber = messageSequenceNumber;
        this.messageType = MESSAGE_TYPE.PREPARE;
        this.messageLiteral = new String("PREPARE:" + roundNumber + ":" + slotIndex + ":" + clientID + ":" + messageSequenceNumber);
    }

    public String toString() {
        return this.messageLiteral;
    }

    public static PrepareMsg fromString(final String messageLiteral) {
        final String[] subStrArr = messageLiteral.split(":");
        return new PrepareMsg(Integer.parseInt(subStrArr[1]), Integer.parseInt(subStrArr[2]), Long.parseLong(subStrArr[3]), Integer.parseInt(subStrArr[4]));
    }

    public int getRoundNumber() {
        return roundNumber;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public long getClientID() {
        return clientID;
    }

    public int getMessageSequenceNumber() {
        return messageSequenceNumber;
    }
}
