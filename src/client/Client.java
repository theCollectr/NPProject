package client;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Timer;
import java.util.TimerTask;

public class Client {
    private static final int PORT = 8000;
    private static final int REFRESH_TIME = 10;
    private final String HOST = "localhost";
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final UserInterface user;
    private final Timer timer;

    public Client() throws IOException {
        user = new UserInterface();
        socket = new Socket(HOST, PORT);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        timer = new Timer();
        connectToSever();
    }

    public static void main(String[] args) {
        try {
            Client client = new Client();
            client.listenToServer();
        } catch (IOException e) {
            System.out.println("Couldn't connect to the server. Exiting...");
        }
    }

    /**
     * Takes name from user and sends it to the server then waits for response from server
     */
    private void connectToSever() {
        String name = user.getName();
        sendNameToSever(name);
        checkConnectionResponse();
    }

    private void sendNameToSever(String name) {
        JSONObject nameJson = new JSONObject();
        nameJson.put("type", "name");
        nameJson.put("name", name);
        writer.println(nameJson);
    }

    private void checkConnectionResponse() {
        boolean response = getConnectionResponse();
        if (response) {
            user.printMessage("Connected successfully to the server");
        } else {
            user.printMessage("Couldn't connect to the sever. Exiting...");
            System.exit(-1);
        }
    }

    private boolean getConnectionResponse() {
        boolean response = false;
        try {
            String packet = reader.readLine();
            JSONObject responseJson = new JSONObject(packet);
            String type = responseJson.getString("type");
            if (type.equals("response")) {
                response = responseJson.getBoolean("successful");
            }
        } catch (IOException ignored) {
            // failed to connect. will return false
        }
        return response;
    }

    /**
     * Keeps checking for requests from the sever
     */
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

    private void checkForRequest() throws IOException {
        if (!reader.ready()) {
            return;
        }
        String packet = reader.readLine();
        processRequest(packet);
    }

    private void processRequest(String request) {
        JSONObject packetJson = new JSONObject(request);
        String type = packetJson.getString("type");
        switch (type) {
            case ("message") -> sendMessageToUser(packetJson);
            case ("question") -> sendQuestionToUser(packetJson);
            default -> user.printMessage("Invalid packet was received");
        }
    }

    private void sendMessageToUser(JSONObject messageJson) {
        String message = messageJson.getString("content");
        user.printMessage(message);
    }

    private void sendQuestionToUser(JSONObject questionJson) {
        String question = questionJson.getString("question");
        user.clearInput();
        user.printMessage(question);
        waitForResponse();
    }

    /**
     * waits until the user enters an answer or for a response from the server
     * indicating the time going up.
     */
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

    private void checkForResponse() {
        if (user.isAnswered()) {
            int answer = user.getAnswer();
            sendAnswerToServer(answer);
            synchronized (this) {
                notify();
            }
        } else if (isPacketRevived()) {
            synchronized (this) {
                user.printMessage("time's up!");
                notify();
            }
        }
    }

    private void sendAnswerToServer(int answer) {
        JSONObject answerJson = new JSONObject();
        answerJson.put("type", "answer");
        answerJson.put("answer", answer);
        writer.println(answerJson);
    }

    /**
     * checks if the client received a request from the server
     *
     * @return true if a request is available
     */
    private boolean isPacketRevived() {
        boolean isReceived = false;
        try {
            isReceived = reader.ready();
        } catch (IOException e) {
            user.printMessage("An error happened while talking to the server.");
        }
        return isReceived;
    }
}