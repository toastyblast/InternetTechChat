package nl.saxion.internettech;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import static nl.saxion.internettech.ServerState.*;

public class Server {

    private ServerSocket serverSocket;
    private Set<ClientThread> threads;
    private ServerConfiguration conf;


    public Server(ServerConfiguration conf) {
        this.conf = conf;
    }

    /**
     * Runs the server. The server listens for incoming client connections
     * by opening a socket on a specific port.
     */
    public void run() {
        // Create a socket to wait for clients.
        try {
            serverSocket = new ServerSocket(conf.SERVER_PORT);
            threads = new HashSet<>();

            while (true) {
                // Wait for an incoming client-connection request (blocking).
                Socket socket = serverSocket.accept();

                // When a new connection has been established, start a new thread.
                ClientThread ct = new ClientThread(socket);
                threads.add(ct);
                new Thread(ct).start();
                System.out.println("Num clients: " + threads.size());

                // Simulate lost connections if configured.
                if(conf.doSimulateConnectionLost()){
                    DropClientThread dct = new DropClientThread(ct);
                    new Thread(dct).start();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * This thread sleeps for somewhere between 10 tot 20 seconds and then drops the
     * client thread. This is done to simulate a lost in connection.
     */
    private class DropClientThread implements Runnable {
        ClientThread ct;

        DropClientThread(ClientThread ct){
            this.ct = ct;
        }

        public void run() {
            try {
                // Drop a client thread between 10 to 20 seconds.
                int sleep = (10 + new Random().nextInt(10)) * 1000;
                Thread.sleep(sleep);
                ct.kill();
                threads.remove(ct);
                System.out.println("Num clients: " + threads.size());
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This inner class is used to handle all communication between the server and a
     * specific client.
     */
    private class ClientThread implements Runnable {

        private DataInputStream is;
        private OutputStream os;
        private Socket socket;
        private ServerState state;
        private String username;

        public ClientThread(Socket socket) {
            this.state = INIT;
            this.socket = socket;
        }

        public String getUsername() {
            return username;
        }

        public OutputStream getOutputStream() {
            return os;
        }

        public void run() {
            try {
                // Create input and output streams for the socket.
                os = socket.getOutputStream();
                is = new DataInputStream(socket.getInputStream());
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                // According to the protocol we should send HELO <welcome message>
                state = CONNECTING;
                String welcomeMessage = "HELO " + conf.WELCOME_MESSAGE;
                writeToClient(welcomeMessage);

                while (!state.equals(FINISHED)) {
                    // Wait for message from the client.
                    String line = reader.readLine();
                    if (line != null) {
                        // Log incoming message for debug purposes.
                        boolean isIncomingMessage = true;
                        logMessage(isIncomingMessage, line);

                        // Parse incoming message.
                        Message message = new Message(line);

                        // Process message.
                        switch (message.getMessageType()) {
                            case HELO:
                                // Check username format.
                                boolean isValidUsername = message.getPayload().matches("[a-zA-Z0-9_]{3,14}");
                                if(!isValidUsername) {
                                    state = FINISHED;
                                    writeToClient("-ERR username has an invalid format (only characters, numbers and underscores are allowed)");
                                } else {
                                    // Check if user already exists.
                                    boolean userExists = false;
                                    for (ClientThread ct : threads) {
                                        if (ct != this && message.getPayload().equals(ct.getUsername())) {
                                            userExists = true;
                                            break;
                                        }
                                    }
                                    if (userExists) {
                                        state = FINISHED;
                                        writeToClient("-ERR user already logged in");
                                    } else {
                                        state = CONNECTED;
                                        this.username = message.getPayload();
                                        writeToClient("+OK " + getUsername());
                                    }
                                }
                                break;
                            case BCST:
                                // Broadcast to other clients.
                                for (ClientThread ct : threads) {
                                    if (ct != this) {
                                        ct.writeToClient("BCST [" + getUsername() + "] " + message.getPayload());
                                    }
                                }
                                writeToClient("+OK");
                                break;
                            case QUIT:
                                // Close connection
                                state = FINISHED;
                                writeToClient("+OK Goodbye");
                                break;
                            case UNKOWN:
                                // Unkown command has been sent
                                writeToClient("-ERR Unkown command");
                                break;
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * An external process can stop the client using this methode.
         */
        public void kill() {
            System.out.println("KILL - User " + getUsername());
            state = FINISHED;
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Write a message to this client thread.
         * @param message   The message to be sent to the (connected) client.
         */
        private void writeToClient(String message) {
            boolean shouldDropPacket = false;
            boolean shouldCorruptPacket = false;

            // Check if we need to behave badly by dropping some messages.
            if (conf.doSimulateDroppedPackets()) {
                // Randomly select if we are going to drop this message or not.
                int random = new Random().nextInt(6);
                if (random == 0) {
                    // Drop message.
                    shouldDropPacket = true;
                    System.out.println("[DROPPED] " + message);
                }
            }

            // Check if we need to behave badly by corrupting some messages.
            if (conf.doSimulateCorruptedPackets()) {
                // Randomly select if we are going to corrupt this message or not.
                int random = new Random().nextInt(4);
                if (random == 0) {
                    // Corrupt message.
                    shouldCorruptPacket = true;
                }
            }

            // Do the actual message sending here.
            if (!shouldDropPacket) {
                if (shouldCorruptPacket){
                    message = corrupt(message);
                    System.out.println("[CORRUPT] " + message);
                }
                PrintWriter writer = new PrintWriter(os);
                writer.println(message);
                writer.flush();

                // Echo the message to the server console for debugging purposes.
                boolean isIncomingMessage = false;
                logMessage(isIncomingMessage, message);
            }
        }

        /**
         * This methods implements a (naive) simulation of a corrupt message by replacing
         * some charaters at random indexes with the charater X.
         * @param message   The message to be corrupted.
         * @return  Returns the message with some charaters replaced with X's.
         */
        private String corrupt(String message) {
            Random random = new Random();
            int x = random.nextInt(4);
            char[] messageChars =  message.toCharArray();

            while (x < messageChars.length) {
                messageChars[x] = 'X';
                x = random.nextInt(10);
            }

            return new String(messageChars);
        }

        /**
         * Util method to print (debug) information about the server's incoming and outgoing messages.
         * @param isIncoming    Indicates whether the message was an incoming message. If false then
         *                      an outgoing message is assumed.
         * @param message       The message received or sent.
         */
        private void logMessage(boolean isIncoming, String message) {
            String directionString = conf.CLI_COLOR_OUTGOING + ">> ";  // Outgoing message.
            if (isIncoming) {
                directionString = conf.CLI_COLOR_INCOMING + "<< ";     // Incoming message.
            }

            // Add username to log if present.
            // Note when setting up the connection the user is not known.
            if (getUsername() == null) {
                System.out.println(directionString + message + conf.RESET_CLI_COLORS);
            } else {
                System.out.println(directionString + "[" + getUsername() + "] " + message + conf.RESET_CLI_COLORS);
            }
        }
    }
}
