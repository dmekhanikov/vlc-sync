package megabyte.vlcsync.messages;

public class Message {
    public final MessageType messageType;
    public final byte[] data;

    public Message(MessageType messageType) {
        this(messageType, null);
    }

    public Message(MessageType messageType, byte[] data) {
        this.messageType = messageType;
        this.data = data;
    }

    public byte[] bytes() {
        byte[] bytes = new byte[messageType.size];
        bytes[0] = messageType.code;
        for (int i = 1; i < messageType.size; i++) {
            bytes[i] = data[i - 1];
        }
        return bytes;
    }
}
