package com.company;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

public class ListeningThread implements Runnable {
    private Singleton singleton = Singleton.getInstance();

    public ListeningThread() {
        //Constructor is not needed.
    }

    @Override
    public void run() {
        while (true) {
            String serverMessage = null;

            try {
                serverMessage = singleton.getBufferedReader().readLine();
            } catch (SocketTimeoutException ignored) {

            } catch (IOException e) {
                e.printStackTrace();
            }

            if (serverMessage != null) {

                if (serverMessage.contains("BCST")) { //If it is a normal message display it to the user.
                    //When a normal message is received it will be shown to the client along with the time that it was
                    //send at.
                    //PURELY COSMETIC CODE.
                    DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); //Get the current time.
                    LocalDateTime now = LocalDateTime.now();
                    System.out.println(dtf.format(now) + " " + serverMessage.replaceAll("BCST", ""));
                    //PURELY COSMETIC CODE.
                } else if (serverMessage.equals("+OK Goodbye")) { // If this is the final message stop the timer.
                    //This message is received when the user types "quit".
                    //When the message is received the thread is stopped from listening.
                    System.out.println(serverMessage);
                    singleton.setContinueToChat(false);
                    break;
                } else if (serverMessage.contains("-ERR") || serverMessage.contains("HELO") || serverMessage.contains("+OK")) {
                    //Do nothing.
                } else {
                    //If a message from unknown type has been received this means it was corrupted.
                    //The program prints it to the user so he knows that the message was not sent.
                    if (singleton.isMessageSent()){
                        System.out.println("Your message was corrupted. " + singleton.getLastMessage());
                        singleton.setMessageSent(false);
                    } else if (!singleton.isMessageSent()) {
                        System.out.println("You received a corrupted message. " + serverMessage);
                    }
                }
            }
        }
    }

    private void sendMessage(String message){
        PrintWriter writer = new PrintWriter(singleton.getOutputStream());
        writer.println(message);
        writer.flush();
        singleton.setLastMessage(message);
    }

}
