package com.company;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public class ListeningThread implements Runnable {

    private Singleton singleton = Singleton.getInstance();

    public ListeningThread() {

    }

    @Override
    public void run() {
        while (true) {


            String serverMessage = null;


            try {

                serverMessage = singleton.getBufferedReader().readLine();

            } catch (SocketTimeoutException ignored) {
                //If a message is not received for 10 seconds this means that there is no connection.
                //When that happens a new socket will be created and the user will be logged again with his username.

                System.out.println("Connection was lost. But you are now reconnected again.");
                try {

                    //Create the new socket.
                    singleton.setSocket(new Socket(ClientOpening.SERVER_ADDRESS, ClientOpening.SERVER_PORT));

                    //Log in the user.
                    PrintWriter writer = new PrintWriter(singleton.getOutputStream());
                    writer.println("HELO " + singleton.getUserName());
                    writer.flush();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }


            if (serverMessage != null) {

                if (serverMessage.contains("BCST")) { //If it is a normal message display it to the user.
                    //When a normal message is recieved it will be shown to the client along with the time that it was
                    //send at.

                    if (serverMessage.contains("TEST_CONNECTION")) {
                        //This message is only for testing the connection, so when the user receives it do not do
                        //anything with it.
                    } else {
                        //PURELY COSMETIC CODE.
                        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); //Get the current time.
                        LocalDateTime now = LocalDateTime.now();
                        System.out.println(dtf.format(now) + " " + serverMessage.replaceAll("BCST", ""));
                        //PURELY COSMETIC CODE.
                    }
                } else if (serverMessage.equals("+OK Goodbye")) { // If this is the final message stop the timer.
                    //This message is received when the user types "quit".
                    //When the message is received the thread is stopped from listening.

                    System.out.println(serverMessage);
                    singleton.setContinueToChat(false);
                    break;
                } else if (serverMessage.contains("-ERR")) {
                    //Do nothing.
                } else if (serverMessage.contains("+OK")) {
                    //Do nothing.
                } else if (serverMessage.contains("HELO")) {
                    //Do nothing.
                } else {
                    //If a message from unknown type has been received this means it was corrupted.
                    //The program prints it to the user so he knows that the message was not sent.
                    System.out.println("Your message was corrupt:");
                }
            }


        }
    }
}
