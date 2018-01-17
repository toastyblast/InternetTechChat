package nl.saxion.internettech;

import javax.net.ssl.*;
import java.io.*;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.*;

import static nl.saxion.internettech.ServerState.*;

public class Server {

    private SSLServerSocket sslServerSocket;
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
            SSLContext context = SSLContext.getInstance("TLS");
            KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("SunX509");
            KeyStore keyStore = KeyStore.getInstance("JKS");

            keyStore.load(new FileInputStream("D:/IdeaProjects/InternetTechChatSSLTest/keystore.txt"), "storepass".toCharArray());
            keyManagerFactory.init(keyStore, "keypass".toCharArray());
            context.init(keyManagerFactory.getKeyManagers(), null, null);

            SSLServerSocketFactory factory = context.getServerSocketFactory();
            sslServerSocket = (SSLServerSocket)factory.createServerSocket(1337);
            threads = new HashSet<>();

            while (true) {
                // Wait for an incoming client-connection request (blocking).
                SSLSocket socket = (SSLSocket) sslServerSocket.accept();

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
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (UnrecoverableKeyException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
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
}
