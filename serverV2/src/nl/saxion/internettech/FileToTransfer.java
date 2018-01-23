package nl.saxion.internettech;

public class FileToTransfer {

    private String sender;
    private String receiver;
    private byte[] file;
    private long fileSize;
    private long uniqueNumber;

    public FileToTransfer(String sender, String receiver, byte[] file, long fileSize, long uniqueNumber){
        this.sender = sender;
        this.receiver = receiver;
        this.file = file;
        this.fileSize = fileSize;
        this.uniqueNumber = uniqueNumber;
    }

    public String getReceiver() {
        return receiver;
    }

    public String getSender() {
        return sender;
    }

    public byte[] getFile() {
        return file;
    }

    public void setFile(byte[] file) {
        this.file = file;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public long getUniqueNumber() {
        return uniqueNumber;
    }
}
