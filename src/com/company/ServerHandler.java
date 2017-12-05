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
        PrintWriter writer = new PrintWriter(singleton.getOutputStream());

        //Writing to the server before every message to check if the connection is OK.
        //If this line throws an exception this means the socket is closed.
        singleton.getOutputStream().write("TEST ".getBytes());
        writer.println();
        writer.flush();

        Scanner reader = new Scanner(System.in);
        String userInput = reader.nextLine();

        //If the user types "quit", quit him out.
        if (userInput.equalsIgnoreCase("quit")) {
            singleton.getOutputStream().flush();
            quit();
        } else if (userInput.toLowerCase().startsWith("/whisper")) {
            singleton.getOutputStream().write("WSPR ".getBytes());
            writer.println(userInput);
            writer.flush();

            singleton.setMessageSent(true);
            singleton.setLastMessage(userInput);
        } else if (userInput.toLowerCase().startsWith("/users")) {
            //TODO - IDEA: Let the user either add the name of a group ("/users" returns GLOBAL, "/users chat1" returns CHAT1) OR
            //TODO: Only let them return the list of the group they are currently in, if they type "/users", ignoring any input after it and adding their current group's name to the output here at the backend.
            singleton.getOutputStream().write("USRS ".getBytes());
            writer.println(userInput);
            writer.flush();

            singleton.setMessageSent(true);
            singleton.setLastMessage(userInput);
        } else if (userInput.toLowerCase().startsWith("/grp")){
            singleton.getOutputStream().write("GRP ".getBytes());
            writer.println(userInput);
            writer.flush();
        } else {
            //If it is a normal message, just send it.
            singleton.getOutputStream().write("BCST ".getBytes());
            writer.println(userInput);
            writer.flush();

            singleton.setMessageSent(true);
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
        writer.println("BCST has entered WhatsUpp. Say hi!");

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
