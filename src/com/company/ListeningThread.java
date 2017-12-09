package com.company;


import java.io.IOException;
import java.io.PrintWriter;
import java.net.SocketTimeoutException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

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
                //TODO: Do nothing..?
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (serverMessage != null) {
                //Check if the message received from the server is not empty.
                if (serverMessage.contains("BCST") || serverMessage.contains("WSPR") || serverMessage.contains("GRPMSG")) { //If it is a normal message display it to the user.
                    //When a normal message is received it will be shown to the client along with the time that it was
                    //send at.
                    showReceivedUserMessage(serverMessage);
                }
                else if (serverMessage.contains("USRS")) {
                    showUsersList(serverMessage);
                }
                else if (serverMessage.equals("+OK Goodbye")) { // If this is the final message stop the timer.
                    //This message is received when the user types "quit".
                    //When the message is received the thread is stopped from listening.
                    System.out.println(serverMessage);
                    singleton.setContinueToChat(false);
                    break;
                }
                else if (serverMessage.contains("+GRP") || serverMessage.contains("-ERR")){
                    System.out.println(serverMessage);
                }
                else if (serverMessage.contains("HELO") || serverMessage.contains("+OK")) {
                    //Do nothing.
                }
                else {
                    //If a message from unknown type has been received this means it was corrupted.
                    //The program prints it to the user so he knows that the message was not sent.
                    if (singleton.isMessageSent()){
                        System.out.println("Your message was corrupted. " + singleton.getLastMessage());
                        singleton.setMessageSent(false);
                    }
                    else if (!singleton.isMessageSent()) {
                        System.out.println("You received a corrupted message. " + serverMessage);
                    }
                }
            }
        }
    }

    private void showReceivedUserMessage(String serverMessage) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); //Get the current time.
        LocalDateTime now = LocalDateTime.now();

        if (serverMessage.contains("WSPR")) {
            System.out.println(dtf.format(now) + " " + serverMessage.replaceAll("WSPR", "[WHISPER]"));
        }
        else if (serverMessage.contains("GRPMSG")) {
            ArrayList<String> message = new ArrayList<>(Arrays.asList(serverMessage.split(" ")));

            String groupName = "ERROR:INIT";
            if (message.size()  >= 3) {
                groupName = message.get(2);
                message.remove(2);
            }

            String completedMessage = buildMessage(message);

            System.out.println(dtf.format(now) + " " + completedMessage.replaceAll("GRPMSG", "[" + groupName + "]"));
        }
        else {
            System.out.println(dtf.format(now) + " " + serverMessage.replaceAll("BCST", "[GLOBAL]"));
        }
    }

    private String buildMessage(ArrayList<String> message) {
        StringBuilder sb = new StringBuilder();

        for (String messagePart : message) {
            sb.append(messagePart);
            sb.append(" ");
        }

        return sb.toString();
    }

    private void showUsersList(String serverMessage) {
        //Get an arrayList of names from the string.
        ArrayList<String> listOfUsers = new ArrayList<>(Arrays.asList(serverMessage.split(",")));
        //The message first starts with "USRS", then "," and then "<group>". We need to get that group.
        String messageGroup = listOfUsers.get(1);
        //Remove the USRS and <group> items from the arrayList.
        listOfUsers.remove(0);
        listOfUsers.remove(0);

        StringBuilder sb = new StringBuilder();

        for (String name : listOfUsers) {
            sb.append("\n - ");
            sb.append(name);
        }

        //Change the part after the - and IN to the name of the actual group this is a list of users of.
        System.out.println("~-= List of the users in chat group [" + messageGroup + "] =-~" + sb.toString().replaceAll("::", " "));
    }

    private void sendMessage(String message){
        PrintWriter writer = new PrintWriter(singleton.getOutputStream());
        writer.println(message);
        writer.flush();
        singleton.setLastMessage(message);
    }
}
