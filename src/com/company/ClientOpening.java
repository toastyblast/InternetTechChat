package com.company;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;

public class ClientOpening {

    private Thread listen;
    private Thread connect;
    ServerHandler serverHandler;
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
                    singleton.setSocket();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        //Ask the user for username.
        serverHandler.login();

        System.out.println("Successfully logged in. \n" + "You can now type a message and press enter to chat.");

        //Create the thread that listens for messages from the server.
        listen = new Thread(new ListeningThread());
        //Create the thread that sends a message each 8 second in order to determine if there is connection.
//        connect = new Thread(new ConnectionCheckThread());

        listen.start();
//        connect.start();

        while (singleton.isContinueToChat()) {
            try {

                serverHandler.sendMessage();

            } catch (IOException e) {
                System.out.println("You have been disconnected.");
                singleton.reconnect();
            }
        }
    }

    //Other methods...
}
