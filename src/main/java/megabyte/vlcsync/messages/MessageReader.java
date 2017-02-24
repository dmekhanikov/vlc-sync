package megabyte.vlcsync.messages;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class MessageReader implements Closeable {

    private final InputStream inputStream;

    public MessageReader(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public Message readMessage() throws IOException {
        int ret = inputStream.read();
        if (ret == -1) {
            return null;
        }
        MessageType messageType = MessageType.fromCode(ret);
        switch (messageType) {
            case PLAY:
            case SEEK:
                return new Message(messageType, read(messageType.size - 1));
            case PAUSE:
            case PING:
                return new Message(messageType);
            default:
                throw new IOException("Received a message of unknown type: " + ret);
        }
    }

    private byte[] read(int len) throws IOException {
        byte[] data = new byte[len];
        int fetched = 0;
        while (fetched != len) {
            int r = inputStream.read(data, fetched, len - fetched);
            if (r == -1) {
                throw new IOException("EOF reached");
            } else {
                fetched += r;
            }
        }
        return data;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
