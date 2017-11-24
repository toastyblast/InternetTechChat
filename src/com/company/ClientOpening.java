package com.company;

import java.io.*;
import java.net.Socket;

public class ClientOpening {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1337;
    private Socket socket;

    private InputStream inputStream;
    private OutputStream outputStream;

    public static void main(String[] args) {
        new ClientOpening().run();
    }

    public void run() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            ServerHandler serverHandler = new ServerHandler(outputStream);
            BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

            while (socket.isConnected()) {
                String serverMessage = reader.readLine();

                //Write your code here...

//                if (serverMessage.equalsIgnoreCase("QUIT")) {
//                    //...
//                }

                System.out.println(serverMessage);
            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Socket getSocket() {
        return socket;
    }

    //Other methods...
}
