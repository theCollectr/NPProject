package server;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Logger {
    public static void log(String message) {
        LocalDateTime now = java.time.LocalDateTime.now();
        System.out.print("[" + DateTimeFormatter.ISO_DATE_TIME.format(now) + "] ");
        System.out.println(message);
    }
}
