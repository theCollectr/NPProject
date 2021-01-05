# Networks Programming Project
This is a simple multiplayer console game that connects players over the network in a sever-client model.

# Design and System Flow
### Server
<img src="assets/Package server.png"/> <br/>

The server side consists of four major components that can each be running concurrently.  
First, when a new connection request is sent by a client, the `Server` receives it and
forwards to the `ClientHandler` to deal with it in a new thread to make sure the server stays as
responsive as possible.  
The `ClientHandler` then wraps the received socket with a `Player` object and sends it the `MatchMaker`.
When the `MatchMaker` receives a new player it puts the player in a waiting list.
Meanwhile, the `MatchMaker` has another thread running that keeps checking if the waiting list has
enough players to create a new game. Currently, the game size is set to 2 by the `GAME_SIZE` property
so, when it finds 2 players in the waiting list, it creates a new game and adds those
two players to the new game and runs it concurrently.
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
I tried to make the client side as simple as possible so, it only has two classes.
The first and main one is the `Client` class which takes instructions from the server
and executes them. The second is the `UserInterface` which deals with the interaction with the user.  
At the start, the `Client` asks the user for to enter a name, then tries to connect with the server.
When a connection is established, it sends the name to the server so it can be added to the waiting list.  
Then, it goes into a loop checking for new requests from the server to execute them. Currently, there only
two requests it can respond to:
- `message` which Client only has to print to the user.
- `question` where it has to print the question to user and wait for either to user to type an answer
  or for a request from the server telling it that the time went up.

# Important methods
- `server.Server.start()`
  Runs in an infinite loop checking for new clients trying to connect to the server and
  forwards them to the `ClientHandler`.

  ```java
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
  ```

- `server.ClientHandler.handle()`
  Receives new client from the `Server`, packages them in a `Player` object and
  sends them to the `MatchMaker`.  

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

- `server.MatchMaker.createGame()`
  Creates a new game and moves players from the waiting list to the new game.
  This method gets called by the `MatchMaker` when the waiting list has enough people to start a new game.

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

- `server.Game.run()`
  Controls the flow of the game by going through the steps mentioned before.

  ```java
  public void run() {
      Logger.log(this + " has started");
      notifyAllPlayers("You were added to a match.\nMatch is starting...");
      for (Question question : questions) {
          waitFor(TIME_BETWEEN_QUESTIONS);
          askQuestion(question);
          waitForAnswers();
          findFirstToAnswer();
          processResult();
      }
      findWinner();
  }
  ```

- `server.Game.waitForAnswers()`
  Schedules a task to check if all players answered the current questions that runs every 100ms
  and another that waits for 30 seconds setting a timer and then waits until one of these tasks notify it.

  ```java
  private void waitForAnswers() {
      TimerTask timeUpTask = new TimerTask() {
          @Override
          public void run() {
              timeUpCallback();
          }
      };
      TimerTask checkAnswersTask = new TimerTask() {
          @Override
          public void run() {
              checkIfAllPlayersAnswered();
          }
      };

      timer.schedule(timeUpTask, QUESTIONS_TIME);
      timer.schedule(checkAnswersTask, DELAY_BEFORE_START_LISTENING, REFRESH_TIME);

      synchronized (this) {
          try {
              wait();
          } catch (InterruptedException e) {
              Logger.log("wait interrupted at " + this);
          }
      }

      timeUpTask.cancel();
      checkAnswersTask.cancel();
  }
  ```

- `server.PlayerSocket.sendMessage(String)`
  Takes a message as String, wraps it in a `JSONObject` and sends it to the client.

  ```java
  public void sendMessage(String message) {
      JSONObject jsonObject = new JSONObject();
      jsonObject.put("type", "message");
      jsonObject.put("content", message);
      writer.println(jsonObject);
      writer.flush();
      Logger.log("sent message to" + socket);
  }
  ```

- `server.PlayerSocket.sendQuestion(String)`
  Takes a question as a string and sends it to the client then waits for a response
  for 30 seconds. If it doesn't receive an answer, it returns -1.

  ```java
  public int sendQuestion(String question) {
      JSONObject questionJson = new JSONObject();
      questionJson.put("type", "question");
      questionJson.put("question", question);
      writer.println(questionJson);
      writer.flush();
      Logger.log("sent question to" + socket);
      int answer = -1;
      try {
          String packet = reader.readLine();
          JSONObject answerJson = new JSONObject(packet);
          String type = answerJson.getString("type");
          if (type.equals("answer")) {
              answer = answerJson.getInt("answer");
              Logger.log("received answer from " + socket);
          } else {
              Logger.log(INVALID_PACKET + socket);
          }
      } catch (IOException e) {
          Logger.log("didn't receive an answer from " + socket);
      }
      return answer;
  }
  ```
- `client.Client.litsenToServer()`
  Schedules a task to run every 10ms that checks if there are requests coming from the server.

  ```java
  private void listenToServer() {
      Timer timer = new Timer();
      TimerTask task = new TimerTask() {
          @Override
          public void run() {
              try {
                  checkForRequest();
              } catch (IOException e) {
                  user.printMessage("An error happened while talking to the server.");
              }
          }
      };

      timer.schedule(task, 0, REFRESH_TIME);
  }
  ```

- `client.Client.processRequest(String)`
  Takes a request as a String, translates it to JSON and deals with it according to its type.

  ```java
  private void processRequest(String request) {
      JSONObject packetJson = new JSONObject(request);
      String type = packetJson.getString("type");
      switch (type) {
          case ("message") -> sendMessageToUser(packetJson);
          case ("question") -> sendQuestionToUser(packetJson);
          default -> user.printMessage("Invalid packet was received");
      }
  }
  ```

- `client.Client.waitForResponse()`
  Schedules a task that runs every 10ms to check if the user entered an answer or if a request was
  received from the server.
  ```java
  private void waitForResponse() {
      TimerTask task = new TimerTask() {
          @Override
          public void run() {
              checkForResponse();
          }
      };
      timer.schedule(task, 0, REFRESH_TIME);

      synchronized (this) {
          try {
              wait();
          } catch (InterruptedException ignored) {
          }
      }

      task.cancel();
  }
  ```

# Questions Class
The provided questions were serialized using the `java.io.Serializable` interface which introduced a problem
because deserialization fails when the package of the `Question` class is changed and there is no
way import it when it is placed in the default package. Moreover, I wanted to add an `ID` member to
the `Question` that's generated for each question on run time as a kind of verification.  
So, I moved the questions to a JSON format which allows me to have more control and it also has the
added bonus of being readable and editable by humans, meaning new questions can be easily added to
the questions set.  
The code I used to transform the questions file into JSON is provided with the project and here's the function
I use to translate JSON objects to Question objects:  
```java
public static Question createFromJson(JSONObject questionJson) {
    String question = questionJson.getString("question");
    int correctAnswer = questionJson.getInt("correct");
    int points = questionJson.getInt("points");

    JSONArray choicesJson = questionJson.getJSONArray("choices");
    String[] choices = new String[choicesJson.length()];
    for (int i = 0; i < choices.length; i++) {
        choices[i] = (String) choicesJson.get(i);
    }

    return new Question(question, choices, correctAnswer, points);
}
```
Please note that for you to be able to run the program, you need to install the `json.org` library
which can be installed using maven:
```xml
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20201115</version>
</dependency>
```
