package message;

/**
 * Success Response Message: "SUCCESS_RESPONSE:9:7:12"
 * note that
 *      "9" is the first unchosen index after updating,
 *      "7" is the modified index
 *      "12" is the server ID who send out this response message
 */

public class SuccessResponseMsg extends Message {

    private final int firstUnchosenIndexAfterUpdate;
    private final int modifiedIndex;
    private final int responseServerID;

    public SuccessResponseMsg(int firstUnchosenIndexAfterUpdate, int modifiedIndex, int responseServerID) {
        this.firstUnchosenIndexAfterUpdate = firstUnchosenIndexAfterUpdate;
        this.modifiedIndex = modifiedIndex;
        this.responseServerID = responseServerID;
        this.messageType = MESSAGE_TYPE.SUCCESS_RESPONSE;
        this.messageLiteral = new String("SUCCESS_RESPONSE:" + firstUnchosenIndexAfterUpdate + ":" + modifiedIndex + ":" + responseServerID);
    }

    public String toString() {
        return this.messageLiteral;
    }

    public static SuccessResponseMsg fromString(final String messageLiteral) {
        final String[] subStrArr = messageLiteral.split(":");
        return new SuccessResponseMsg(
                Integer.parseInt(subStrArr[1]),
                Integer.parseInt(subStrArr[2]),
                Integer.parseInt(subStrArr[3])
        );
    }

    public int getFirstUnchosenIndexAfterUpdate() {
        return firstUnchosenIndexAfterUpdate;
    }

    public int getModifiedIndex() {
        return modifiedIndex;
    }

    public int getResponseServerID() {
        return responseServerID;
    }
}
