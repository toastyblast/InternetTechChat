package com.company;

import java.io.*;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class Singleton {
    private static Singleton ourInstance = new Singleton();

    private Socket socket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;
    private boolean continueToChat = true;
    private String userName;
    private String lastMessage;

    public static Singleton getInstance() {
        return ourInstance;
    }

    private Singleton() {
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;

        try {
            socket.setSoTimeout(10000);
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
        OutputStream outputStream = null;
        try {
            outputStream = this.socket.getOutputStream();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
