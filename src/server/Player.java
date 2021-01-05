package server;

import java.io.IOException;
import java.net.Socket;

public class Player {
    private static int idCounter = 0;
    private final int id;
    private final String name;
    private final PlayerSocket socket;
    private int points;

    public Player(Socket socket) throws IOException {
        this.socket = new PlayerSocket(socket);
        this.name = this.socket.getName();
        this.id = nextId();
        points = 0;
    }

    private synchronized int nextId() {
        int id = idCounter;
        idCounter += 1;
        return id;
    }

    @Override
    public String toString() {
        return name + " (" + id + ")";
    }

    public void setQuestionTime(long time) {
        socket.setTimeOut(time);
    }

    public void sendMessage(String message) {
        socket.sendMessage(message);
    }

    public Answer sendQuestion(Question question) {
        int answerNumber = socket.sendQuestion(question.toString());
        int questionId = question.getId();
        return new Answer(questionId, answerNumber, id);
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void addPoints(int points) {
        this.points += points;
    }

    public int getPoints() {
        return points;
    }
}
