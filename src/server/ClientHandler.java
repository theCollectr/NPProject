package server;

import java.io.IOException;
import java.net.Socket;

public class ClientHandler {
    MatchMaker matchMaker;

    public ClientHandler(MatchMaker matchMaker) {
        this.matchMaker = matchMaker;
        matchMaker.start();
    }

    /**
     * wraps client socket with a Player object and adds to the match making queue
     *
     * @param clientSocket represent the new connection request
     */
    public void handle(Socket clientSocket) {
        try {
            Player player = new Player(clientSocket);
            Logger.log("connected with " + player + " at " + clientSocket);
            matchMaker.add(player);
        } catch (IOException e) {
            Logger.log("failed to connect with player at " + clientSocket);
        }
    }
}
