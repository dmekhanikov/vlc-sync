package megabyte.vlcsync;

import megabyte.vlcsync.messages.Message;
import megabyte.vlcsync.messages.MessageReader;
import megabyte.vlcsync.messages.MessageType;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SyncServer {

    public static final int DEFAULT_PORT = 7773;
    private static final int WAKEUP_RATE = 500;

    private final ServerSocket serverSocket;
    private final Set<Client> clients = Collections.newSetFromMap(new ConcurrentHashMap<>());

    public SyncServer(int port) throws IOException {
        this.serverSocket = new ServerSocket(port);
    }

    private void acceptConnections() {
        while (!Thread.interrupted()) {
            try {
                Socket clientSocket = serverSocket.accept();
                System.err.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());
                Client client = new Client(clientSocket);
                clients.add(client);
                new Thread(() -> processMessages(client)).start();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void processMessages(Client client) {
        try (MessageReader messageReader = new MessageReader(client.getSocket().getInputStream())) {
            for (Message message = messageReader.readMessage(); message != null; message = messageReader.readMessage()) {
                if (message.messageType != MessageType.PING) {
                    System.err.println("Message received: " + message.messageType);
                }
                switch (message.messageType) {
                    case PLAY:
                        long time = Serializer.decodeTime(message.data);
                        sendPlay(time, client);
                        break;
                    case PING:
                        client.pong();
                        break;
                    default:
                        sendToClients(message, client);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not read from client's socket; " +
                    "IP: " + client.getHost() + "; cause: " + e.getMessage());
        }
    }

    private void sendPlay(long time, Client from) {
        long fromLatency = from.getLatency();
        for (Iterator<Client> it = clients.iterator(); it.hasNext(); ) {
            Client client = it.next();
            long sendingTime = time + fromLatency + client.getLatency();
            Message message = new Message(MessageType.PLAY, Serializer.encodeTime(sendingTime));
            if (client != from && !sendToClient(message, client)) {
                it.remove();
            }
        }
    }

    private void sendToClients(Message message, Client except) {
        for (Iterator<Client> it = clients.iterator(); it.hasNext(); ) {
            Client client = it.next();
            if (client != except && !sendToClient(message, client)) {
                it.remove();
            }
        }
    }

    private boolean sendToClient(Message message, Client client) {
        Socket clientSocket = client.getSocket();
        if (clientSocket.isClosed()) {
            return false;
        } else {
            try {
                client.getSocket().getOutputStream().write(message.bytes());
            } catch (IOException e) {
                System.err.println("Couldn't send message to " + client.getHost() + ": " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    private void schedulePings() {
        Message pingMessage = new Message(MessageType.PING);
        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(() -> {
            for (Iterator<Client> it = clients.iterator(); it.hasNext(); ) {
                Client client = it.next();
                if (!sendToClient(pingMessage, client)) {
                    it.remove();
                }
                client.ping();
            }
        }, WAKEUP_RATE, WAKEUP_RATE, TimeUnit.MILLISECONDS);
    }

    private void doMain() throws IOException {
        new Thread(this::acceptConnections).start();
        schedulePings();
        System.err.println("Sync server started");
    }

    public static void main(String[] args) throws IOException {
        new SyncServer(DEFAULT_PORT).doMain();
    }
}
