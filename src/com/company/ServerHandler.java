package com.company;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.Scanner;

public class ServerHandler extends Thread {
    private OutputStream outputStream;
    private String userName;

    public ServerHandler(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public void sendMessage() {
        PrintWriter writer = new PrintWriter(outputStream);
        Scanner reader = new Scanner(System.in);
        String userInput = reader.nextLine();
        if (userInput.equals("quit")){
            quit();
        } else {
            writer.println("BCST " + userInput);
            writer.flush();
        }
    }

    public void login(){
        PrintWriter writer = new PrintWriter(outputStream);
        Scanner reader = new Scanner(System.in);
        System.out.println("Please enter a username: ");
        String userInput = reader.next();
        userName = userInput;
        writer.println("HELO " + userInput);
        writer.println("BCST " + userInput + " USER_ENTERED_CHAT");
        writer.flush();
    }

    public void quit(){
        PrintWriter writer = new PrintWriter(outputStream);
        writer.println("BCST " + userName + " USER_EXITED_CHAT");
        writer.println("QUIT");
        writer.flush();
    }

    //Methods...
}
