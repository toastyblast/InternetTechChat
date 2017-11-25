package com.company;

import java.io.*;
import java.net.Socket;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

public class ClientOpening {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 1337;
    private final String ERROR_MESSAGE_USER_ALREADY_LOGGED = "-ERR user already logged in";
    private Socket socket;

    private InputStream inputStream;
    private OutputStream outputStream;
    BufferedReader reader;

    public static void main(String[] args) {
        new ClientOpening().run();
    }

    public void run() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);

            inputStream = socket.getInputStream();
            outputStream = socket.getOutputStream();

            ServerHandler serverHandler = new ServerHandler(outputStream);
             reader = new BufferedReader(new InputStreamReader(inputStream));

            System.out.println(reader.readLine()); // Show welcoming message.

            serverHandler.login();

            String loginMessage = reader.readLine();

            if (loginMessage.contains("+OK")){ //If the user manages to log ing successfully...
                System.out.println("Successfully logged in. \n" + "You can now chat.");

                listen();
            } else {
                System.out.println(loginMessage); // Show error message:
            }

            while (socket.isConnected()) {

                serverHandler.sendMessage();

            }

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public Socket getSocket() {
        return socket;
    }

    public void listen(){
        Timer t = new Timer();

        t.scheduleAtFixedRate(
                new TimerTask()
                {
                    public void run()
                    {
                        String serverMessage = null;
                        try {
                            serverMessage = reader.readLine();
                        } catch (IOException e) {
//                            e.printStackTrace();
                            System.out.println("Connection lost.");
                        }

                        if (!serverMessage.isEmpty() && serverMessage.contains("BCST")){ //If it is a normal message display it to the user.
                            //PURELY COSMETIC CODE.
                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); //Get the current time.
                            LocalDateTime now = LocalDateTime.now();
                            System.out.println(dtf.format(now) + " " + serverMessage.replaceAll("BCST",""));
                            //PURELY COSMETIC CODE.
                        } else if (serverMessage.equals("+OK Goodbye")){ // If this is the final message stop the timer.
                            System.out.println(serverMessage);
                            t.cancel();
                            t.purge();
                        }
                    }
                },
                0,      // run first occurrence immediately
                1000);
    }

    //Other methods...
}
