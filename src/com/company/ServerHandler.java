package com.company;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Scanner;

public class ServerHandler extends Thread {
    private String userName;
    private Singleton singleton = Singleton.getInstance();

    public ServerHandler() {
        //Nothing, a constructor is not needed.
    }

    /**
     * Method for sending message to the user.
     */
    public void sendMessage() throws IOException {
        singleton.getOutputStream().write("BCST ".getBytes());

        Scanner reader = new Scanner(System.in);
        String userInput = reader.nextLine();
        PrintWriter writer = new PrintWriter(singleton.getOutputStream());

        //If the user types "quit", quit him out.
        if (userInput.equalsIgnoreCase("quit")) {
            singleton.getOutputStream().flush();
            quit();
        } else {
            //If it is a normal message, just send it.
            writer.println(userInput);
            writer.flush();
            singleton.setLastMessage(userInput);
        }
    }

    /**
     * Method for logging in.
     */
    public void login() {
        Scanner reader = new Scanner(System.in);

        System.out.println("Please enter a username: ");
        String userInput = reader.next();
        userName = userInput;
        //Store the username for when you need to reconnect.
        singleton.setUserName(userName);

        PrintWriter writer = new PrintWriter(singleton.getOutputStream());
        writer.println("HELO " + userInput);
        writer.println("BCST entered the chat. Say hi!");

        writer.flush();
    }

    /**
     * Method for quiting the server.
     */
    private void quit() {
        PrintWriter writer = new PrintWriter(singleton.getOutputStream());
        writer.println("has left the chat.");
        writer.println("QUIT");

        writer.flush();
    }

    //Methods...
}
