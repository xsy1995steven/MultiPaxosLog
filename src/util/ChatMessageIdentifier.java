package util;

import java.util.Objects;

/**
 * A POJO that model the distinctness of messages sent from different clients
 * Note that a pair of client ID and message sequence number can
 */
public class ChatMessageIdentifier {
    private final long clinetId;
    private final int messageSequenceNumber;

    public ChatMessageIdentifier(long clinetId, int messageSequenceNumber) {
        this.clinetId = clinetId;
        this.messageSequenceNumber = messageSequenceNumber;
    }

    public long getClinetId() {
        return clinetId;
    }

    public int getMessageSequenceNumber() {
        return messageSequenceNumber;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ChatMessageIdentifier that = (ChatMessageIdentifier) o;
        return clinetId == that.clinetId &&
                messageSequenceNumber == that.messageSequenceNumber;
    }

    @Override
    public int hashCode() {
        return Objects.hash(clinetId, messageSequenceNumber);
    }
}
