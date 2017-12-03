package com.company;

import java.io.*;
import java.net.Socket;

public class Singleton {
    private static Singleton ourInstance = new Singleton();

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;
    private boolean continueToChat = true;
    private String userName;
    private String lastMessage;

    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 1337;
    public static Singleton getInstance() {
        return ourInstance;
    }

    private Singleton() {
        //Constructor is not needed.
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket() throws IOException {
        this.socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

        try {
            socket.setSoTimeout(200);
            this.outputStream = this.socket.getOutputStream();
            this.inputStream = this.socket.getInputStream();
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public BufferedReader getBufferedReader() {
        return this.bufferedReader;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public boolean isContinueToChat() {
        return this.continueToChat;
    }

    public void setContinueToChat(boolean continueToChat) {
        this.continueToChat = continueToChat;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

    public void setLastMessage(String lastMessage) {
        this.lastMessage = lastMessage;
    }

    public String getLastMessage() {
        return lastMessage;
    }

    public void reconnect(){
        try {
            setSocket();
            PrintWriter writer = new PrintWriter(this.outputStream);

            writer.println("HELO " + this.userName);
            writer.flush();

            System.out.println("You have been reconnected.");
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
