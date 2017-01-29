package megabyte.vlcsync;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Iterator;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SyncServer {

    public static final int DEFAULT_PORT = 7773;
    private static final byte[] PLAY_MESSAGE = {1};
    private static final byte[] PAUSE_MESSAGE = {2};

    private final ServerSocket serverSocket;
    private final Set<Socket> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SyncServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    private void doMain() throws IOException {
        new Thread(this::acceptConnections).start();
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNextLine()) {
            String command = scanner.nextLine().trim();
            switch (command) {
                case "play":
                    sendToClients(PLAY_MESSAGE);
                    break;
                case "pause":
                    sendToClients(PAUSE_MESSAGE);
                    break;
                case "exit":
                    System.exit(1);
                    break;
            }
        }
    }

    private void acceptConnections() {
        while (true) {
            try {
                Socket connectionSocket = serverSocket.accept();
                System.err.println("Client connected: " + connectionSocket.getInetAddress().getHostAddress());
                connections.add(connectionSocket);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void sendToClients(byte[] message) {
        for (Iterator<Socket> it = connections.iterator(); it.hasNext(); ) {
            Socket clientSocket = it.next();
            if (clientSocket.isClosed()) {
                it.remove();
            } else {
                try {
                    clientSocket.getOutputStream().write(message);
                } catch (IOException e) {
                    System.err.println("Couldn't send message to " + clientSocket.getInetAddress().getHostAddress() +
                            ": " + e.getMessage());
                    it.remove();
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        new SyncServer(DEFAULT_PORT).doMain();
    }
}
