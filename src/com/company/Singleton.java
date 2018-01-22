package com.company;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

public class Singleton {
    private static Singleton ourInstance = new Singleton();

    private OutputStream outputStream;
    private BufferedReader bufferedReader;
    private boolean continueToChat = true;
    private String userName;
    private String lastMessage;
    private boolean messageSent;
    private String stateOfTheUser;
    private String filePath;
    private String fileExtension;

    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 1337;

    public static Singleton getInstance() {
        return ourInstance;
    }

    private Singleton() {
        //Constructor is not needed.
    }

    public void setSocket() throws IOException {
        System.setProperty("javax.net.ssl.trustStore", "./truststore.txt");
        System.setProperty("javax.net.ssl.trustStorePassword", "storepass");
        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslsocketfactory.createSocket(SERVER_ADDRESS, SERVER_PORT);

        try {
            sslSocket.setSoTimeout(200);
            this.outputStream = sslSocket.getOutputStream();
            InputStream inputStream = sslSocket.getInputStream();
            this.bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        } catch (IOException e) {
            e.printStackTrace();
        }
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

    public boolean isMessageSent() {
        return messageSent;
    }

    public void setMessageSent(boolean messageSent) {
        this.messageSent = messageSent;
    }

    public String getStateOfTheUser() {
        return stateOfTheUser;
    }

    public void setStateOfTheUser(String stateOfTheUser) {
        this.stateOfTheUser = stateOfTheUser;
    }

    public void reconnect() {
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileExtension() {
        return fileExtension;
    }

    public void setFileExtension(String fileExtension) {
        this.fileExtension = fileExtension;
    }
}
