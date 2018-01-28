package com.company;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;

/**
 * Singleton class for a single instance of a client, that stores all kind of data that needs to be remembered, like
 * the socket the connection is on, what the username is, etc.
 */
public class Singleton {
    private static Singleton ourInstance = new Singleton();

    private OutputStream outputStream;
    private BufferedReader bufferedReader;
    private boolean continueToChat = true;
    private String userName = "";
    private String lastMessage;
    private boolean messageSent;
    private String stateOfTheUser;
    private String filePath;
    private String fileExtension;
    private long uniqueNumber;

    //Change these two values depending on what port the server is hosted, and on what IP on the same network.
    public static final String SERVER_ADDRESS = "localhost";
    public static final int SERVER_PORT = 1337;

    public static Singleton getInstance() {
        return ourInstance;
    }

    private Singleton() {
        //Constructor is not needed.
    }

    /**
     * Method that is used by the ClientOpening to create a new SSLSocket to connect over.
     *
     * @throws IOException can be thrown due to the calling of input and output streams.
     */
    public void setSocket() throws IOException {
        //Create a new SSLSocket by informing the system there's a trustStore to get a certificate from. Then set up
        // a factory with a new SSLSocket to create, with the server's address and port to connect to.
        System.setProperty("javax.net.ssl.trustStore", "./truststore.txt");
        System.setProperty("javax.net.ssl.trustStorePassword", "storepass");
        SSLSocketFactory sslsocketfactory = (SSLSocketFactory) SSLSocketFactory.getDefault();
        SSLSocket sslSocket = (SSLSocket) sslsocketfactory.createSocket(SERVER_ADDRESS, SERVER_PORT);

        try {
            //If we don't get a response quick enough, timeout the socket and let the caller repeat.
            sslSocket.setSoTimeout(200);
            //Try to get an input and output reader from the socket, which can only happen if we're connected.
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

    /**
     * Method that is called by the client's backend when it notices it got disconnected by the server, to try to reconnect.
     */
    public void reconnect() {
        try {
            //Set a new socket, as the old one was apparently broken.
            setSocket();
            PrintWriter writer = new PrintWriter(this.outputStream);
            //Since we got disconnected, we can try to reconnect with out old username again, as the server should've discarded it.
            writer.println("HELO " + this.userName);
            writer.flush();
            //If we get here it means you got reconnected, if not, the caller should repeat this method again.
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

    public long getUniqueNumber() {
        return uniqueNumber;
    }

    public void setUniqueNumber(long uniqueNumber) {
        this.uniqueNumber = uniqueNumber;
    }

    public void clearFileHistory(){
        this.uniqueNumber = 0;
        this.fileExtension = "";
        this.filePath = "";
    }
}
