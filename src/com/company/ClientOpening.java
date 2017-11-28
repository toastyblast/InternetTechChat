package com.company;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class ClientOpening {
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 1337;
    private Thread listen;
    private Thread connect;
    private Singleton singleton = Singleton.getInstance();

    public static void main(String[] args) {
        new ClientOpening().run();
    }

    public void run() {
        try {
            //Connect to the socket.
            singleton.setSocket(new Socket(SERVER_ADDRESS, SERVER_PORT));
            //Set up the messaging thread.
            ServerHandler serverHandler = new ServerHandler();
            //Keep looping until you are actually connected to the server.

            boolean logged = false;
            while (!logged) {
                try {
                    System.out.println(singleton.getBufferedReader().readLine()); // Show welcoming message.
                    logged = true;
                } catch (SocketTimeoutException ste) {
                    //If the server doesn't send a connection confirmation message for 10s exception will be thrown
                    //and the client will try to reconnect again.
                    System.out.println("Could not connect. Trying to reconnect.");
                    singleton.setSocket(new Socket(SERVER_ADDRESS, SERVER_PORT));
                }
            }

            //Ask the user for username.
            serverHandler.login();

            System.out.println("Successfully logged in. \n" + "You can now type a message and press enter to chat.");

            //Create the thread that listens for messages from the server.
            listen = new Thread(new ListeningThread());
            //Create the thread that sends a message each 8 second in order to determine if there is connection.
            connect = new Thread(new ConnectionCheckThread());

            listen.start();
            connect.start();

            while (singleton.isContinueToChat()) {
                serverHandler.sendMessage();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //Other methods...
}
