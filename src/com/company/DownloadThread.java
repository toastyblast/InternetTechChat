package com.company;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DownloadThread implements Runnable {
    private int fileSize;
    private Singleton singleton = Singleton.getInstance();
    private SSLSocket newDownloadSocket;
    private OutputStream outputStream;
    private InputStream inputStream;
    private BufferedReader bufferedReader;

    public DownloadThread(int fileSize){
        this.fileSize = fileSize;
    }

    @Override
    public void run() {
        try {
            createDownloadSocket();
            String serverMessage = null;

            try {

                PrintWriter writer = new PrintWriter(outputStream);
                outputStream.write(("DNLD ready " + singleton.getUserName()).getBytes());
                writer.println();
                writer.flush();

                DataInputStream dis;
                try {
                    dis = new DataInputStream(inputStream);
                    byte[] buffer = new byte[16000];

                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    int filesize = fileSize; // Send file size in separate msg
                    int read;
                    int totalRead = 0;
                    int remaining = filesize;
                    while ((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                        totalRead += read;
                        remaining -= read;
//                                System.out.println("read " + totalRead + " bytes.");
                        stream.write(buffer, 0, read);
                    }
                    stream.close();
                    dis.close();
                    System.out.println(singleton.getFilePath());
                    Path path = Paths.get(singleton.getFilePath());
                    Files.write(path, stream.toByteArray());
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createDownloadSocket() throws IOException {
        System.setProperty("javax.net.ssl.trustStore", "./truststore.txt");
        System.setProperty("javax.net.ssl.trustStorePassword", "storepass");
        SSLSocketFactory sslsocketfactory = (SSLSocketFactory )SSLSocketFactory.getDefault();
        this.newDownloadSocket = (SSLSocket) sslsocketfactory.createSocket(Singleton.SERVER_ADDRESS, Singleton.SERVER_PORT);

        this.outputStream = this.newDownloadSocket.getOutputStream();
        this.inputStream = this.newDownloadSocket.getInputStream();
        this.bufferedReader = new BufferedReader(new InputStreamReader(this.inputStream));
    }
}
