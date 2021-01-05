package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

class Server {
    ClientHandler clientHandler;

    public Server() {
        MatchMaker matchMaker = new MatchMaker();
        clientHandler = new ClientHandler(matchMaker);
    }

    public static void main(String[] args) {
        Server server = new Server();
        server.start();
    }

    /**
     * keeps checking for new connections
     */
    private void start() {
        try (ServerSocket server = new ServerSocket(8000)) {
            server.setReuseAddress(true);
            while (true) {
                Socket client = server.accept();
                Logger.log("connected to " + client);
                new Thread(() -> clientHandler.handle(client)).start();
            }
        } catch (IOException e) {
            Logger.log("server failed");
        }
    }
}
