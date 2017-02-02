package megabyte.vlcsync;

import java.net.Socket;
import java.util.ArrayDeque;
import java.util.Deque;

public class Client {

    private static final long DEFAULT_LATENCY = 100;
    private static final long HISTORY_SIZE = 10;

    private final Socket socket;
    private final Deque<Long> latencyHistory = new ArrayDeque<>();
    private volatile long latencySum;
    private volatile long lastPing = -1;

    public Client(Socket socket) {
        this.socket = socket;
    }

    public Socket getSocket() {
        return socket;
    }

    public String getHost() {
        return socket.getInetAddress().getHostAddress();
    }

    public synchronized void ping() {
        if (lastPing == -1) {
            lastPing = System.currentTimeMillis();
        }
    }

    public synchronized void pong() {
        if (lastPing != -1) {
            long latency = (System.currentTimeMillis() - lastPing) / 2;
            latencyHistory.addLast(latency);
            latencySum += latency;
            if (latencyHistory.size() > HISTORY_SIZE) {
                latencySum -= latencyHistory.removeFirst();
            }
            lastPing = -1;
        }
    }

    public synchronized long getLatency() {
        if (latencyHistory.isEmpty()) {
            return DEFAULT_LATENCY;
        } else {
            return latencySum / latencyHistory.size();
        }
    }
}
