//package com.company;
//
//import java.io.BufferedReader;
//import java.io.IOException;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.Timer;
//import java.util.TimerTask;
//
//public class ListeningThread implements Runnable {
//    private BufferedReader readLine;
//
//    public ListeningThread(BufferedReader reader){
//        this.readLine = reader;
//    }
//
//    @Override
//    public void run() {
//        Timer t = new Timer();
//
//        t.scheduleAtFixedRate(
//                new TimerTask()
//                {
//                    public void run()
//                    {
//                        String serverMessage = null;
//                        try {
//                            serverMessage = readLine.readLine();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        }
//
//                        if (!serverMessage.isEmpty() && !serverMessage.contains("+OK")){ //If it is a normal message display it to the user.
//                            //PURELY COSMETIC CODE.
//                            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss"); //Get the current time.
//                            LocalDateTime now = LocalDateTime.now();
//                            System.out.println(dtf.format(now) + " " + serverMessage.replaceAll("BCST",""));
//                            //PURELY COSMETIC CODE.
//                        } else if (serverMessage.equals("+OK Goodbye")){ // If this is the final message stop the timer.
//                            System.out.println(serverMessage);
//                            t.cancel();
//                            t.purge();
//                        }
//                    }
//                },
//                0,      // run first occurrence immediately
//                1000);
//    }
//}
// KEEPING THIS CLASS FOR NOW, JUST IN CASE.!!!!!!!!!