package message;

/**
 * Accept Response Message: "ACCEPT_RESPONSE:23:10:2:1539876988101:45"
 * note that
 *      "23" is the minimum the number of the smallest proposal this server will accept for any log entry
 *      "10" means the first unchosen index
 *      "2" indicates the server ID who send out this response message
 *      "1539876988101" means the client ID
 *      "45" indicates the message's sequence number
 */

public class AcceptResponseMsg extends Message {

    private final int minProposal;
    private final int firstUnchosenIndex;
    private final int responseServerID;
    private final long clientID;
    private final int messageSequenceNumber;

    public AcceptResponseMsg(int minProposal, int firstUnchosenIndex, int responseServerID, long clientID, int messageSequenceNumber) {
        this.minProposal = minProposal;
        this.firstUnchosenIndex = firstUnchosenIndex;
        this.responseServerID = responseServerID;
        this.clientID = clientID;
        this.messageSequenceNumber = messageSequenceNumber;
        this.messageType = MESSAGE_TYPE.ACCEPT_RESPONSE;
        this.messageLiteral = new String("ACCEPT_RESPONSE:" + minProposal + ":" + firstUnchosenIndex + ":" + responseServerID + ":" + clientID + ":" + messageSequenceNumber);
    }

    public String toString() {
        return this.messageLiteral;
    }

    public static AcceptResponseMsg fromString(final String messageLiteral) {
        final String[] subStrArr = messageLiteral.split(":");
        return new AcceptResponseMsg(
                Integer.parseInt(subStrArr[1]),
                Integer.parseInt(subStrArr[2]),
                Integer.parseInt(subStrArr[3]),
                Long.parseLong(subStrArr[4]),
                Integer.parseInt(subStrArr[5])
        );
    }

    public int getMinProposal() {
        return minProposal;
    }

    public int getFirstUnchosenIndex() {
        return firstUnchosenIndex;
    }

    public int getResponseServerID() {
        return responseServerID;
    }

    public long getClientID() {
        return clientID;
    }

    public int getMessageSequenceNumber() {
        return messageSequenceNumber;
    }
}
