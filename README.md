# Networks Programming Project
This is a multiplayer console game that connects players over the network in a sever-client model.

# Design and System Flow
## Server
<img src="assets/Package server.png"/> <br />

The server side consists of four major components that can each be running concurrently.  
First, when a new connection request is sent by a client, the `Server` receives it and
forwards to the `ClientHandler` to deal with it in a new thread to make sure the server stays as
responsive as possible:
```java
while (true) {
    Socket client = server.accept();
    Logger.log("connected to " + client);
    new Thread(() -> clientHandler.handle(client)).start();
}
```
The `ClientHandler` wraps the received socket with a `Player` object and sends it the `MatchMaker`:
```java
public void handle(Socket clientSocket) {
    try {
        Player player = new Player(clientSocket);
        Logger.log("connected with " + player + " at " + clientSocket);
        matchMaker.add(player);
    } catch (IOException e) {
        Logger.log("failed to connect with player at " + clientSocket);
    }
}
```
When the `MatchMaker` receives a new player it puts the player in a waiting list.
Meanwhile, the `MatchMaker` has another thread running that keeps checking if the waiting list has
enough players to create a new game. Currently, the game size is set to 2 by the `GAME_SIZE` property
so, when it finds 2 players in the waiting list, it creates a new game and adds those
two players to the new game and runs it concurrently.  
```java
private synchronized void createGame() {
    Game game = new Game(questionsGenerator);
    Logger.log("created " + game);
    for (int i = 0; i < GAME_SIZE; i++) {
        game.addPlayer(nextPlayer());
    }
    game.start();
}
```
The `GAME_SIZE` can be increased to allow more than 2 players to join a game.  
At the construction of a new `Game`, it requests 5 random questions from the `questionsGenerator`,
and when it's started it goes through 5 rounds. In each round, it goes through four steps:  
- The question is sent to all players in the game each in a separate thread that waits for an answer
  from the client and adds it to the answers queue when it's received.
- It sets a timer to 30 seconds and waits either for the timer to go up or for
  all players to send  answers.
- It goes through the answers queue and finds the first correct answer and discards the rest.
- It gives the player who answered first the question points and sends a message to all players
  telling them who got the points  

After all 5 rounds, it finds the player with the most points and tells all players about the result.  

### Client
I tried to make the client side as simple as possible
