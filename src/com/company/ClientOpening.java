package com.company;

import java.io.*;
import java.net.SocketTimeoutException;

public class ClientOpening {
    private Thread listen;
    private ServerHandler serverHandler;
    private Singleton singleton = Singleton.getInstance();

    public static void main(String[] args) {
        new ClientOpening().run();
    }

    public void run() {
        try {
            //Connect to the socket.
            singleton.setSocket();
            //Set up the messaging thread.
            serverHandler = new ServerHandler();
            boolean logged = false;
            //Keep looping until you are actually connected to the server.
            while (!logged) {
                try {
                    System.out.println(singleton.getBufferedReader().readLine()); // Show welcoming message.
                    logged = true;
                    singleton.setStateOfTheUser("Connected");
                } catch (SocketTimeoutException ste) {
                    //If the server doesn't send a connection confirmation message for 10s exception will be thrown
                    //and the client will try to reconnect again.
                    System.out.println("Could not connect. Trying to reconnect.");
                    singleton.setSocket();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Ask the user for username.
        String loginResponseFromServer;
        boolean validCredentials = false;
        while (!validCredentials) {
            serverHandler.login();
            try {
                loginResponseFromServer = singleton.getBufferedReader().readLine();
                if (loginResponseFromServer.equals("+OK " + singleton.getUserName())) {
                    validCredentials = true;
                    singleton.setStateOfTheUser("Logged");
                } else {
                    System.out.println(loginResponseFromServer);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Successfully logged in. \n" + "You can now type a message and press enter to chat.");

        //Create the thread that listens for messages from the server.
        listen = new Thread(new ListeningThread());
        listen.start();

        while (!singleton.getStateOfTheUser().equals("Disconnected")) {
            try {
                serverHandler.sendMessage();
            } catch (IOException e) {
                System.out.println("You have been disconnected.");
                singleton.reconnect();
            }
        }
    }
}
