package nl.saxion.internettech;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

import static nl.saxion.internettech.ServerState.*;

public class Server {

    private ServerSocket serverSocket;
    private Set<ClientThreadPC> threads;
    private ServerConfiguration conf;
    private ArrayList<Group> groups = new ArrayList<>();

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
                ClientThreadPC ct = new ClientThreadPC(this, socket);
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

    public Set<ClientThreadPC> getThreads() {
        return threads;
    }

    public ServerConfiguration getConf() {
        return conf;
    }

    public ArrayList<Group> getGroups() {
        return groups;
    }

    /**
     * This thread sleeps for somewhere between 10 tot 20 seconds and then drops the
     * client thread. This is done to simulate a lost in connection.
     */
    private class DropClientThread implements Runnable {
        ClientThreadPC ct;

        DropClientThread(ClientThreadPC ct){
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
}
