package megabyte.vlcsync;

import java.io.IOException;
import java.io.InputStream;
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
    private static final int SEEK_MESSAGE_CODE = 3;
    private static final int SEEK_MESSAGE_SIZE = 5;

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
        while (!Thread.interrupted()) {
            try {
                Socket connectionSocket = serverSocket.accept();
                System.err.println("Client connected: " + connectionSocket.getInetAddress().getHostAddress());
                connections.add(connectionSocket);
                new Thread(() -> resendMessages(connectionSocket)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void resendMessages(Socket clientSocket) {
        try {
            InputStream inputStream = clientSocket.getInputStream();
            while (true) {
                int r = inputStream.read();
                if (r == -1) {
                    return;
                }
                byte[] message;
                if (r == PLAY_MESSAGE[0]) {
                    System.err.println("Play message received");
                    message = PLAY_MESSAGE;
                } else if (r == PAUSE_MESSAGE[0]) {
                    System.err.println("Pause message received");
                    message = PAUSE_MESSAGE;
                } else if (r == SEEK_MESSAGE_CODE) {
                    System.err.println("Seek message received");
                    message = new byte[SEEK_MESSAGE_SIZE];
                    message[0] = SEEK_MESSAGE_CODE;
                    read(inputStream, message, 1, SEEK_MESSAGE_SIZE - 1);
                    sendToClients(message, clientSocket);
                } else {
                    System.err.println("Received a message of unknown type: " + r);
                    continue;
                }
                sendToClients(message, clientSocket);
            }
        } catch (IOException e) {
            System.err.println(readErrMsg(clientSocket, e));
        }
    }

    private void read(InputStream inputStream, byte[] buffer, int off, int len) throws IOException {
        int fetched = 0;
        while (fetched != len) {
            int r = inputStream.read(buffer, off + fetched, len - fetched);
            if (r == -1) {
                throw new IOException("EOF reached");
            } else {
                fetched += r;
            }
        }
    }

    private String readErrMsg(Socket socket, IOException e) {
        return "Could not read from client's socket; " +
                "IP: " + socket.getInetAddress().getHostAddress() +
                "; cause: " + e.getMessage();
    }

    private void sendToClients(byte[] message) {
        sendToClients(message, null);
    }

    private void sendToClients(byte[] message, Socket except) {
        for (Iterator<Socket> it = connections.iterator(); it.hasNext(); ) {
            Socket clientSocket = it.next();
            if (clientSocket == except) {
                continue;
            }
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
