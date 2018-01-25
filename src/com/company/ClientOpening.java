package com.company;

import java.io.*;
import java.net.SocketTimeoutException;

/**
 * Class that can be run to connect to the server. It sets the socket and keeps the connection of said socket in check
 */
public class ClientOpening {
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
                    //If we read this, we successfully connected to the server, so we can keep that in mind.
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

        //Ask the user for username, now that they have an established connection.
        String loginResponseFromServer;
        boolean validCredentials = false;
        while (!validCredentials) {
            //Every time they enter a username that's already in use, request a new name again.
            //Start the request to the server to login
            serverHandler.login();
            try {
                while(!validCredentials) {
                    loginResponseFromServer = singleton.getBufferedReader().readLine();

                    if (loginResponseFromServer.equals("+OK " + singleton.getUserName())) {
                        //If the server responded that the credentials are valid, move on out of the loop.
                        validCredentials = true;
                        singleton.setStateOfTheUser("Logged");
                    } else if (loginResponseFromServer.contains("-ERR")){
                        //TODO: This looping being an else causes the issue of the unneeded repeat question for a username!
                        //The server said the name is already in use, so we need to have the user loop again.
                        System.out.println(loginResponseFromServer);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.out.println("Successfully logged in. \n" + "You can now type a message and press enter to chat.");

        //Create the thread that listens for messages from the server.
        Thread listen = new Thread(new ListeningThread());
        listen.start();

        while (!singleton.getStateOfTheUser().equals("Disconnected")) {
            //As long as the user's not disconnected, keep on listening for messages.
            try {
                serverHandler.sendMessage();
            } catch (IOException e) {
                System.out.println("You have been disconnected.");
                singleton.reconnect();
            }
        }
    }
}
