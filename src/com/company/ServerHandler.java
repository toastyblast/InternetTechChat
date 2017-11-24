package com.company;

import java.io.OutputStream;
import java.io.PrintWriter;

public class ServerHandler extends Thread {
    private OutputStream outputStream;

    public ServerHandler(OutputStream outputStream) {
        this.outputStream = outputStream;

        //Give user options to write input here...

        sendMessage();
    }

    public void sendMessage() {
        PrintWriter writer = new PrintWriter(outputStream);
        writer.println("HELO toastyblast");
        writer.flush();
    }

    //Methods...
}
