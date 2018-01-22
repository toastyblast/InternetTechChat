package com.company;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

public class ServerHandler extends Thread {
    private String userName;
    private Singleton singleton = Singleton.getInstance();
    private OutputStream outputStream;

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
        if (userInput.equalsIgnoreCase("/quit")) {
            singleton.getOutputStream().flush();
            quit();
        } else if (userInput.toLowerCase().startsWith("/help")) {
            sendAnyMessage(writer, "HELP ", userInput);
        } else if (userInput.toLowerCase().startsWith("/whisper")) {
            sendAnyMessage(writer, "WSPR ", userInput);
        } else if (userInput.toLowerCase().startsWith("/users")) {
            sendAnyMessage(writer, "USRS ", userInput);
        } else if (userInput.toLowerCase().startsWith("/grp")) {
            sendAnyMessage(writer, "GRP ", userInput);
        } else if (userInput.toLowerCase().startsWith("/send")) {
            try {
                //Get the file.
                System.out.println("Please input the file path. ");
                Scanner newReader = new Scanner(System.in);
                String path = newReader.nextLine();
                path = path.replace("\\", "\\\\");
                File file = new File(path);
                FileInputStream fis = new FileInputStream(file);
                //Create a new socket connection to upload the file.
                createUploadSocket();
                //Warn the server that you are sending a file.
                PrintWriter newWriter = new PrintWriter(outputStream);
                //Send information about the transfer(clients + file length).
                String[] splits = userInput.split(" ");
                String message = "UPLD " + userName + " " + splits[1] + " " + file.length() + " " + getFileExtension(file);
                outputStream.write(message.getBytes());
                newWriter.println();
                newWriter.flush();
                //Stream the file to the server.
                DataOutputStream dos = new DataOutputStream(outputStream);

                byte[] buffer = new byte[16000];

                while (fis.read(buffer) > 0) {
                    dos.write(buffer);
                }
                fis.close();
                dos.close();
            } catch (FileNotFoundException FNFE) {
                System.out.println("The file could not be found.");
            }
        } else if (userInput.toLowerCase().startsWith("/receive")) {
            String[] splits = userInput.split(" ");

            if (splits[1].equals("accept")) {
                boolean validpath = false;
                while (!validpath) {
                    System.out.println("Please input the download destination.");
                    Scanner newReader = new Scanner(System.in);
                    String path = newReader.nextLine();
                    Path realpath = Paths.get(path);
                    File file = realpath.toFile();
                    if (!file.getParentFile().exists()) {
                        System.out.println("Invalid path given.");
                    } else {
                        if (file.exists()) {
                            System.out.println("File already exists. (At this path there is another file.)");
                        } else {
                            if (!singleton.getFileExtension().equals(getFileExtension(file))) {
                                System.out.println("You are trying to save the file with an extension that is not compatible.");
                            } else {
                                singleton.setFilePath(path.replace("\\", "\\\\"));
                                writer.println("DNLD " + splits[1]);
                                writer.flush();
                                validpath = true;
                            }
                        }
                    }
                }
            }

        } else {
            //If it is a normal message, just send it.
            sendAnyMessage(writer, "BCST ", userInput);
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

        writer.flush();
    }

    /**
     * Method for quiting the server.
     */
    private void quit() {
        PrintWriter writer = new PrintWriter(singleton.getOutputStream());
//        writer.println("has left the chat.");
        writer.println("QUIT");

        writer.flush();
        singleton.setStateOfTheUser("Disconnected");
    }

    /**
     * Private helper method that sends a message to the server
     *
     * @param writer PrintWriter is the writer to put the message into to print to the server.
     * @param type String is the type of message we have to send to the server.
     * @param userInput String is the actual message the user wants to send over.
     * @throws IOException is an exception that can happen with the PrintWriter.
     */
    private void sendAnyMessage(PrintWriter writer, String type, String userInput) throws IOException {
        singleton.getOutputStream().write(type.getBytes());
        writer.println(userInput);
        writer.flush();

        singleton.setMessageSent(true);
        singleton.setLastMessage(userInput);
    }

    /**
     * Method to create a new socket connection, with which the file is going to be uploaded to the server.
     *
     * @throws IOException is an exception that can happen due to the usage of getting input and output streams.
     */
    private void createUploadSocket() throws IOException {
        System.setProperty("javax.net.ssl.trustStore", "./truststore.txt");
        System.setProperty("javax.net.ssl.trustStorePassword", "storepass");
        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket newUploadSocket = (SSLSocket) sslsocketfactory.createSocket(Singleton.SERVER_ADDRESS, Singleton.SERVER_PORT);

        this.outputStream = newUploadSocket.getOutputStream();
    }

    private static String getFileExtension(File file) {
        String fileName = file.getName();
        if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
            return fileName.substring(fileName.lastIndexOf(".") + 1);
        else return "";
    }
}
