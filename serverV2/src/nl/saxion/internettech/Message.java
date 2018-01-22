package nl.saxion.internettech;

public class Message {
    public enum MessageType {
        HELO,
        BCST,
        QUIT,
        TEST,
        UNKOWN,
        WSPR,
        USRS,
        GRP,
        UPLD,
        DNLD,
        HELP
    }

    private String line;

    public Message(String line) {
        this.line = line;
    }

    /**
     * Parses the first word in the message in an attempt to get the message type.
     * @return  Return a message type if it can be parsed correctly or UKNOWN if
     *          the message type cannot be derived.
     */
    public MessageType getMessageType() {
        MessageType result = MessageType.UNKOWN;
        try{
            if (line != null && line.length() > 0) {
                String[] splits = line.split("\\s+");
                result = MessageType.valueOf(splits[0]);
            }
        } catch (IllegalArgumentException iaex) {
            System.out.println("[ERROR] Unknown command");
        }
        return  result;
    }

    /**
     * Gets the payload of the message. This is interpreted as the raw message line
     * without the message type.
     * @return  Returns the raw line minus the message type. If the message type is
     *          unkown then raw line is returned.
     */
    public String getPayload() {

        // Return the raw line if we don't know what the message type is.
        if (getMessageType().equals(MessageType.UNKOWN)) {
            return line;
        }

        // Return an empty string if the raw line was null or
        // the length of the line is smaller than the message type plus one (this
        // should prevent index out of bounds in the substring).
        if (line == null || line.length() < getMessageType().name().length() + 1) {
            return "";
        }

        // Return the part after the message type (excluding whitespace).
        return line.substring(getMessageType().name().length() + 1);
    }
}
