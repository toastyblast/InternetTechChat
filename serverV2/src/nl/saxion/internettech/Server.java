package nl.saxion.internettech;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

/**
 * Class that represents the instance of the server. It keeps track of all clients connected, all the groups and also
 * the SSLServerSocket to which clients can connect to.
 */
public class Server {
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
            //Set up all the security needed to run an SSLServerSocket for clients to connect to.
            SSLContext context = SSLContext.getInstance("TLS");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            KeyStore keyStore = KeyStore.getInstance("JKS");

            keyStore.load(new FileInputStream("./keystore.txt"), "storepass".toCharArray());
            keyManagerFactory.init(keyStore, "keypass".toCharArray());
            context.init(keyManagerFactory.getKeyManagers(), null, null);

            SSLServerSocketFactory factory = context.getServerSocketFactory();
            SSLServerSocket sslServerSocket = (SSLServerSocket) factory.createServerSocket(conf.SERVER_PORT);
            threads = new HashSet<>();

            while (true) {
                //Just keep running until the end of times (or until you're stopped.)
                // Wait for an incoming client-connection request (blocking).
                SSLSocket socket = (SSLSocket) sslServerSocket.accept();

                // When a new connection has been established, start a new thread.
                ClientThreadPC ct = new ClientThreadPC(this, socket);
                threads.add(ct);
                new Thread(ct).start();
                System.out.println("Num clients: " + threads.size());

                // Simulate lost connections if configured.
                if (conf.doSimulateConnectionLost()) {
                    DropClientThread dct = new DropClientThread(ct);
                    new Thread(dct).start();
                }
            }
        } catch (IOException | KeyManagementException | KeyStoreException | UnrecoverableKeyException |
                NoSuchAlgorithmException | CertificateException e) {
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

        DropClientThread(ClientThreadPC ct) {
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
}
