package megabyte.vlcsync.messages;

public enum MessageType {

    PLAY((byte) 1, 5),
    PAUSE((byte) 2, 1),
    SEEK((byte) 3, 5),
    PING((byte) 127, 1);

    public final byte code;
    public final int size;

    MessageType(byte code, int size) {
        this.code = code;
        this.size = size;
    }

    public static MessageType fromCode(int code) {
        for (MessageType messageType : values()) {
            if (messageType.code == code) {
                return messageType;
            }
        }
        throw new IllegalArgumentException("No such message type: " + code);
    }
}
