package com.company;

import java.io.IOException;
import java.net.Socket;

public class ClientOpening {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1337;
    private Socket socket;

    public static void main(String[] args) {
        new ClientOpening().run();
    }

    public void run() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //Write your code here...
    }

    public Socket getSocket() {
        return socket;
    }

    //Other methods...
}
