package message;

/**
 * HeartBeat Message: "HEART_BEAT:0:1539876988101"
 * note that
 *      "0" is the view number and "1539876988101" is the timestamp
 */
public class HeartBeatMsg extends Message {

    private final int viewNumber;
    private final long timeStamp;

    public HeartBeatMsg(int viewNumber, long timeStamp) {
        this.viewNumber = viewNumber;
        this.timeStamp = timeStamp;
        this.messageType = MESSAGE_TYPE.HEART_BEAT;
        this.messageLiteral = new String("HEART_BEAT:" + viewNumber + ":" + timeStamp);
    }

    public String toString() {
        return this.messageLiteral;
    }

    public static HeartBeatMsg fromString(final String messageLiteral) {
        final String[] subStrArr = messageLiteral.split(":");
        return new HeartBeatMsg(Integer.parseInt(subStrArr[1]), Long.parseLong(subStrArr[2]));
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public long getTimeStamp() {
        return timeStamp;
    }
}
