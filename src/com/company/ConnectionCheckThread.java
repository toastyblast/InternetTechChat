package com.company;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Timer;
import java.util.TimerTask;

public class ConnectionCheckThread implements Runnable {
    private Singleton singleton = Singleton.getInstance();

    public ConnectionCheckThread() {

    }

    /**
     * The task that is scheduled in this method is run every 8 seconds.
     * It sends a message to the server to check if there is connection.
     * If the message is received the 10s message timeout exception will not be thrown.
     * If this message is not received the exception will be thrown, meaning that the connection was cut. After the
     * exception the user will be reconnected.
     */
    @Override
    public void run() {
            Timer t = new Timer();

            t.scheduleAtFixedRate(
                    new TimerTask() {
                        public void run() {
                            PrintWriter writer = new PrintWriter(singleton.getOutputStream());
                            writer.println("BCST TEST_CONNECTION");
                            writer.flush();
                            if (!singleton.isContinueToChat()){
                                t.cancel();
                                t.purge();
                            }
                        }
                    },
                    0,      // run first occurrence immediately
                    8000);

    }
}
// KEEPING THIS CLASS FOR NOW, JUST IN CASE.!!!!!!!!!