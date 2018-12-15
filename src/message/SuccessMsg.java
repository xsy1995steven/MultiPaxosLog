package message;

/**
 * Success Message: "SUCCESS:7:Hi"
 * note that
 *      "7" is the index that is chosen
 *      "Hi" is the message value literal
 */

public class SuccessMsg extends Message {

    private final int slotIndex;
    private final String chatMessageLiteral;

    public SuccessMsg(int slotIndex, String chatMessageLiteral) {
        this.slotIndex = slotIndex;
        this.chatMessageLiteral = chatMessageLiteral;
        this.messageType = MESSAGE_TYPE.SUCCESS;
        this.messageLiteral = new String("SUCCESS:" + slotIndex + ":" + chatMessageLiteral);
    }

    public String toString() {
        return this.messageLiteral;
    }

    public static SuccessMsg fromString(final String messageLiteral) {
        final String[] subStrArr = messageLiteral.split(":");
        final StringBuilder builder = new StringBuilder(subStrArr[2]);
        for (int i = 3; i < subStrArr.length; i++) {
            builder.append(':');
            builder.append(subStrArr[i]);
        }
        return new SuccessMsg(Integer.parseInt(subStrArr[1]), builder.toString());
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public String getChatMessageLiteral() {
        return chatMessageLiteral;
    }
}
