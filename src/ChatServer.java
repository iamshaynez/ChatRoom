
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int MAX_CLIENTS = 1000;
    //private static final int MAX_NICK_LEN = 32;

    private static int serverPort = 8972;

    public static void main(String[] args) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(MAX_CLIENTS);
        Map<Socket, ClientHandler> clients = new ConcurrentHashMap<>();
        ServerSocket serverSocket = new ServerSocket(serverPort);

        while (true) {
            Socket clientSocket = serverSocket.accept();
            ClientHandler clientHandler = new ClientHandler(clientSocket, clients);
            clients.put(clientSocket, clientHandler);

            executorService.submit(clientHandler);
        }
    }

    static class ClientHandler implements Runnable {
        private Socket clientSocket;
        private Map<Socket, ClientHandler> clients;
        private String nickname;
        private BufferedReader in;
        private PrintWriter out;

        public ClientHandler(Socket clientSocket, Map<Socket, ClientHandler> clients) throws IOException {
            this.clientSocket = clientSocket;
            this.clients = clients;
            this.nickname = "user" + clientSocket.getPort();
            this.in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            this.out = new PrintWriter(clientSocket.getOutputStream(), true);

            out.println("Welcome Simple Chat! Use /nick to change nick name.");
        }

        @Override
        public void run() {
            String inputLine;
            try {
                while ((inputLine = in.readLine()) != null) {
                    inputLine = inputLine.trim();
                    if (inputLine.startsWith("/")) {
                        // handle commands
                        String[] parts = inputLine.split(" ", 2);
                        String cmd = parts[0];
                        if (cmd.equals("/nick") && parts.length > 1) {
                            this.nickname = parts[1];
                        }
                        continue;
                    }

                    System.out.println(nickname + ": " + inputLine);
                    // broadcast the message to other clients
                    for (ClientHandler clientHandler : clients.values()) {
                        if (clientHandler != this) {
                            clientHandler.out.println(nickname + ": " + inputLine);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("client left: " + clientSocket.getRemoteSocketAddress());
                clients.remove(clientSocket);
            } finally {
                try {
                    clientSocket.close();
                } catch (IOException ignored) {}
            }
        }
    }
}