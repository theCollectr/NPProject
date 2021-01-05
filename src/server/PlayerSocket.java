package server;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;

public class PlayerSocket {
    private static final String INVALID_PACKET = "received invalid packet from ";
    private final Socket socket;
    private final BufferedReader reader;
    private final PrintWriter writer;
    private final String name;


    public PlayerSocket(Socket socket) throws IOException {
        this.socket = socket;
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        writer = new PrintWriter(socket.getOutputStream(), true);
        name = loadName();
    }

    public void setTimeOut(long timeout) {
        try {
            socket.setSoTimeout((int) timeout);
        } catch (SocketException e) {
            Logger.log("failed to set SO timeout at " + socket);
        }
    }

    /**
     * waits for the client to send a name and sends back a response confirming the connection
     *
     * @return the name sent by the client
     * @throws IOException indicates failing to read a packet from the client or receiving an invalid packet
     */
    private String loadName() throws IOException {
        String name;
        boolean successful = false;
        try {
            String packet = reader.readLine();
            JSONObject packetJson = new JSONObject(packet);
            name = getNameFromPacket(packetJson);
            successful = true;
        } catch (IOException e) {
            Logger.log("couldn't read player name from " + socket);
            throw e;
        } finally {
            sendResponse(successful);
        }
        return name;
    }

    private String getNameFromPacket(JSONObject packetJson) throws IOException {
        String name;
        String type = packetJson.getString("type");
        if (type.equals("name")) {
            name = packetJson.getString("name");
            Logger.log("received name from" + socket);
        } else {
            Logger.log(INVALID_PACKET + socket);
            throw new IOException();
        }
        return name;
    }

    private void sendResponse(boolean successful) {
        JSONObject response = new JSONObject();
        response.put("type", "response");
        response.put("successful", successful);
        writer.println(response);
        Logger.log("sent response to " + socket);
    }

    public String getName() {
        return name;
    }

    public void sendMessage(String message) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("type", "message");
        jsonObject.put("content", message);
        writer.println(jsonObject);
        writer.flush();
        Logger.log("sent message to" + socket);
    }

    /**
     * sends a question to the client and waits for answer. if request times out it returns -1.
     *
     * @param question the question to be sent to client
     * @return answers sent by the client
     */
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
}
