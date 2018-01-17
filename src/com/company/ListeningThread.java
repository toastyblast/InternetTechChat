package com.company;


import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;

public class ListeningThread implements Runnable {
    private Singleton singleton = Singleton.getInstance();
    boolean receiving = false;
    //Data for new downloading socket.
    private Socket newDownloadSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;
    private int fileSize;

    public ListeningThread() {
        //Constructor is not needed.
    }

    @Override
    public void run() {
        while (true) {
            if (!receiving) {
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
                    } else if (serverMessage.contains("USRS")) {
                        showUsersList(serverMessage);
                    } else if (serverMessage.equals("+OK Goodbye")) { // If this is the final message stop the timer.
                        //This message is received when the user types "quit".
                        //When the message is received the thread is stopped from listening.
                        System.out.println(serverMessage);
                        singleton.setContinueToChat(false);
                        break;
                    } else if (serverMessage.contains("+GRP") || serverMessage.contains("-ERR")) {
                        System.out.println(serverMessage);
                    } else if (serverMessage.contains("HELO") || serverMessage.contains("+OK")) {
                        //Do nothing.
                    } else if (serverMessage.contains("RQST")) {
                        System.out.println(serverMessage);
                    } else if (serverMessage.contains("BEGIN_DNLD")) {
                        String[] splits = serverMessage.split(" ");
                        fileSize = Integer.parseInt(splits[1]);
                        receiving = true;
                    } else {
                        //If a message from unknown type has been received this means it was corrupted.
                        //The program prints it to the user so he knows that the message was not sent.
                        if (singleton.isMessageSent()) {
                            System.out.println("Your message was corrupted. " + singleton.getLastMessage());
                            singleton.setMessageSent(false);
                        } else if (!singleton.isMessageSent()) {
                            System.out.println("You received a corrupted message. " + serverMessage);
                        }
                    }
                }
            } else if (receiving){
                try {
                    createDownloadSocket();
                    String serverMessage = null;

                    try {
                        serverMessage = bufferedReader.readLine();
                        System.out.println(serverMessage);

                        PrintWriter writer = new PrintWriter(outputStream);
                        outputStream.write("DNLD ready".getBytes());
                        writer.println();
                        writer.flush();

                        DataInputStream dis = null;
                        try {
                            dis = new DataInputStream(inputStream);
                            byte[] buffer = new byte[16000];

                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            int filesize = fileSize; // Send file size in separate msg
                            int read = 0;
                            int totalRead = 0;
                            int remaining = filesize;
                            while ((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                                totalRead += read;
                                remaining -= read;
//                                System.out.println("read " + totalRead + " bytes.");
                                stream.write(buffer, 0, read);
                            }
                            stream.close();
                            dis.close();
                            System.out.println(singleton.getFilePath());
                            Path path = Paths.get(singleton.getFilePath());
                            Files.write(path, stream.toByteArray());
                            receiving = false;
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
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

    private void createDownloadSocket() throws IOException {
        this.newDownloadSocket = new Socket("localhost", 1337);
        this.outputStream = this.newDownloadSocket.getOutputStream();
        this.inputStream = this.newDownloadSocket.getInputStream();
        this.bufferedReader = new BufferedReader(new InputStreamReader(this.inputStream));
    }
}
