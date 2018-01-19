package nl.saxion.internettech;

import java.util.ArrayList;

public class FileTransferSingleton {
    private static FileTransferSingleton ourInstance = new FileTransferSingleton();
    private ArrayList<FileToTransfer> files =  new ArrayList<>();

    public static FileTransferSingleton getInstance() {
        return ourInstance;
    }

    private FileTransferSingleton() {
    }

    public ArrayList<FileToTransfer> getFiles() {
        return files;
    }

    public FileToTransfer getFile(String receiver){
        for (int i = 0; i < files.size() ; i++) {
            if (files.get(i).getReceiver().equals(receiver)){
                return files.get(i);
            }
        }
        return null;
    }
}
