package client;

import java.io.IOException;
import java.util.InputMismatchException;
import java.util.Scanner;

public class UserInterface {
    private static final String GET_NAME = "Enter your name: ";
    private final Scanner scanner;

    public UserInterface() {
        scanner = new Scanner(System.in);
    }

    public String getName() {
        System.out.println(GET_NAME);
        return scanner.nextLine();
    }

    public void printMessage(String message) {
        System.out.println(message);
    }

    public void clearInput() {
        try {
            while (System.in.available() != 0) {
                scanner.nextLine();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isAnswered() {
        boolean isAnswered = false;
        try {
            isAnswered = System.in.available() != 0;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return isAnswered;
    }


    public int getAnswer() {
        while (true) {
            try {
                return scanner.nextInt();
            } catch (InputMismatchException e) {
                scanner.nextLine();
                System.out.println("Please enter a number");
            }
        }
    }
}
