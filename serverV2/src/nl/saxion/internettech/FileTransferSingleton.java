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

    public FileToTransfer getFile(long number){
        for (FileToTransfer file : files) {
            if (file.getUniqueNumber() == number) {
                return file;
            }
        }
        return null;
    }
}
