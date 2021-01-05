package test;

import server.Logger;

public class LoggerTest {
    public static void main(String[] args) {
        Logger.log("player connected");
        Logger.log("connection failed");
    }
}
