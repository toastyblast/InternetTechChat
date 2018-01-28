package nl.saxion.internettech;

import javax.net.ssl.SSLSocket;
import java.io.*;
import java.util.*;

import static nl.saxion.internettech.ServerState.*;
import static nl.saxion.internettech.ServerState.FINISHED;

/**
 * Class that represents one client connected to the server, to be able to get messages from and send responses back to.
 */
public class ClientThreadPC implements Runnable {
    private OutputStream os;
    private SSLSocket socket;
    private ServerState state;
    private String username = "";

    //Data for file sending.
    private boolean sending = false;
    private boolean receiving = false;
    private String sender;
    private String receiver;
    private int fileSize;
    private String fileExtension;
    private FileTransferSingleton singleton = FileTransferSingleton.getInstance();
    private long uniqueNumber;

    private Set<ClientThreadPC> threads;
    private ServerConfiguration conf;
    private ArrayList<Group> groups = new ArrayList<>();

    public ClientThreadPC(Server server, SSLSocket socket) {
        this.state = INIT;
        this.socket = socket;

        this.threads = server.getThreads();
        this.conf = server.getConf();
        this.groups = server.getGroups();
    }

    public String getUsername() {
        return username;
    }

    public boolean isReceivingAFile(){
        return receiving;
    }

    public void setReceiving(boolean receiving) {
        this.receiving = receiving;
    }

    public void run() {
        try {
            // Create input and output streams for the socket.
            os = socket.getOutputStream();
            DataInputStream is = new DataInputStream(socket.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // According to the protocol we should send HELO <welcome message>
            state = CONNECTING;
            String welcomeMessage = "HELO " + conf.WELCOME_MESSAGE;
            writeToClient(welcomeMessage);

            while (!state.equals(FINISHED)) {
                if (!sending) {
                    // Wait for message from the client.
                    String line = reader.readLine();

                    if (line != null) {
                        // Log incoming message for debug purposes.
                        boolean isIncomingMessage = true;
                        logMessage(isIncomingMessage, line);

                        // Parse incoming message.
                        Message message = new Message(line);

                        // Handle the type of message you received accordingly.
                        switch (message.getMessageType()) {
                            case HELO:
                                handleUserLogin(message);
                                break;
                            case BCST:
                                broadcastMessage(message);
                                break;
                            case QUIT:
                                // Close connection
                                state = FINISHED;
                                writeToClient("+OK Goodbye");
                                broadcastSystemMessage(this.username + " has left the chat!");
                                break;
                            case TEST:
                                break;
                            case UNKOWN:
                                // Unkown command has been sent
                                writeToClient("-ERR Unkown command");
                                break;
                            case WSPR:
                                //The user is trying to whisper to another user.
                                sendWhisper(message);
                                break;
                            case USRS:
                                sendUsersList(message);
                                break;
                            case GRP:
                                handleGroupMessages(message);
                                break;
                            case UPLD:
                                startUploading(message);
                                break;
                            case DNLD:
                                handlingDownloadResponse(message);
                                break;
                            case HELP:
                                returnCommandList();
                                break;
                        }
                    }
                } else if (sending) {
                    uploadFileToServer();
                }
            }
            // Remove from the list of client threads and close the socket.
            threads.remove(this);
            socket.close();
        } catch (IOException e) {
            System.out.println("Server Exception: " + e.getMessage());
        }
    }

    /**
     * Helper method that extracts a part of the given string (given as an ArrayList) at the given index.
     *
     * @param strings ArrayList<String> is the sentence split up into single words by any means.
     * @param index int is the index of the word we want to grab from the string.
     * @return String is the extracted word at the given index of the string.
     */
    private String extractCertainPartOfString(ArrayList<String> strings, int index) {
        String desiredString;

        if (strings.size() > index) {
            desiredString = strings.get(index);
        } else {
            return "e";
        }

        return desiredString;
    }

    /**
     * Method that is called upon the user requesting a list of all commands possible (by '/help'). Returns said list of commands.
     */
    private void returnCommandList() {
        StringBuilder sb = new StringBuilder();

        sb.append(" ~+=+ Showing a list of all available commands. +=+~ ");
        sb.append("- To send a private message: /whisper <username> <message>");
        sb.append("- To get a list of all users in global: /users");
        sb.append("- To get a list of all users in a specific group: /users <groupName>");
        sb.append("- To get a list of all groups: /grp allgroups");
        sb.append("- To create a new group: /grp create <groupName>");
        sb.append("- To join an existing group: /grp join <groupName>");
        sb.append("- To leave a group you're in: /grp leave <groupName>");
        sb.append("- To send a message for members of the group only: /grp msg <groupName> <message>");
        sb.append("- To kick a member of a group (creator of the group only-command): /grp kick <groupName> <userName>");
        sb.append("- To send a file to someone: /send");
        sb.append("- To leave the chat service: /quit");

        writeToClient("HELP " + sb.toString());
    }

    /**
     * Method that is used for when the user requested a list of users globally, or by a group.
     *
     * @param message Message is the message we received from the user, that by the run has been checked to be a 'USRS /user' message.
     */
    private void sendUsersList(Message message) {
        //Split the message up into pieces.
        ArrayList<String> commandString = new ArrayList<>(Arrays.asList(message.getPayload().split(" ")));

        StringBuilder sb = new StringBuilder();

        if (commandString.size() > 1) {
            //If there's at least more than one word (aka '/users <groupName>')
            String groupToGetUsersOf = commandString.get(1);

            if (groupExists(groupToGetUsersOf)) {
                //If the group exists that was located as the second word in the command.
                sb.append("USRS,");
                sb.append(groupToGetUsersOf);

                Group group = findGroup(groupToGetUsersOf);

                if (group != null) {
                    //Get the members of the group and append them, so that they can be displayed. The owner will have an ::[OWNER] tag added and appended as the first.
                    ArrayList<ClientThreadPC> groupMembers = group.getMembers();

                    sb.append(",");
                    sb.append(group.getOwner());
                    sb.append("::[OWNER]");

                    for (int i = 1; i < groupMembers.size(); i++) {
                        //Start from the second index (1), as on the first index (0) of the list the owner is always located at the start of the list.
                        sb.append(",");
                        sb.append(groupMembers.get(i).getUsername());
                    }
                }
            } else {
                sb.append("-ERR Group does not exist.");
            }
        } else {
            //Change the part after the dash to the name of the group this is a list of users of.
            sb.append("USRS,GLOBAL");

            for (ClientThreadPC ct : threads) {
                sb.append(",");
                sb.append(ct.getUsername());
            }
        }

        writeToClient(sb.toString());
    }

    /**
     * Private helper method that is called by the run if the received message from the client is of type 'WSPR /whisper'.
     *
     * @param message Message is the user's message we received and was checked by the run.
     */
    private void sendWhisper(Message message) {
        ArrayList<String> myList = new ArrayList<>(Arrays.asList(message.getPayload().split(" ")));

        String userToWhisper = extractCertainPartOfString(myList, 1);

        if (userToWhisper.equalsIgnoreCase("e")) {
            //If we couldn't find an extractable part of the string at index one (meaning only '/whisper' was sent.)
            writeToClient("-ERR Wrong usage of command /whisper. Should be used like: /whisper <username> <message...>");
            return;
        }

        boolean userExists = false;
        ClientThreadPC threadToMessage = null;

        for (ClientThreadPC ct : threads) {
            //Check if the user isn't trying to message themselves or someone who doesn't exist.
            if (ct != this && ct.getUsername().equals(userToWhisper)) {
                userExists = true;
                threadToMessage = ct;
                break;
            }
        }

        if (userExists) {
            //Remove the /whisper and then the <username> parts of the message.
            myList.remove(0);
            myList.remove(0);

            StringBuilder sb = new StringBuilder();
            for (String s : myList) {
                sb.append(s);
                sb.append(" ");
            }

            threadToMessage.writeToClient("WSPR - " + getUsername() + " whispers: " + sb.toString());

            writeToClient("+OK");
        } else {
            writeToClient("-ERR User you tried to whisper to does not exist (or it's you yourself).");
        }
    }

    /**
     * Helper method that is called after the first message we should receive from our client: HELO (+ username).
     *
     * @param message Message is the user's message we received and was checked by the run.
     */
    private void handleUserLogin(Message message) {
        // Check username format.
        if (state != CONNECTED) {
            boolean isValidUsername = message.getPayload().matches("[a-zA-Z0-9_]{3,14}");

            if (!isValidUsername) {
                //Check if the username is of a valid format, and if not, let the user know.
                state = CONNECTING;
                writeToClient("-ERR username has an invalid format (only characters, numbers and underscores are allowed)");
            } else {
                // Check if user already exists.
                boolean userExists = false;

                for (ClientThreadPC ct : threads) {

                    if (ct != this && message.getPayload().equals(ct.getUsername())) {
                        userExists = true;
                        break;
                    }
                }

                if (userExists) {
                    writeToClient("-ERR user already logged in");
                } else {
                    //The username is valid and unique, so connect the user!
                    state = CONNECTED;

                    this.username = message.getPayload();
                    //Let the user know they have been connected.
                    writeToClient("+OK " + getUsername());

                    for (ClientThreadPC ct : threads) {
                        if (ct != this) {
                            //Let all the users that are not this new one know a new user joined!
                            ct.writeToClient("BCST - " + "SYSTEM" + ": " + this.username + " has entered the chat!");
                        }
                    }
                }
            }
        } else {
            writeToClient("-ERR You are already logged in. You cannot log in while you are already connected.");
        }
    }

    /**
     * Method that can be invoked to send a broadcast of a system message, reserved for special messages that are
     * server-side, caused by a user under special circumstances (like a kick or a leave.)
     *
     * @param message Message is the user's message we received and was checked by the run.
     */
    private void broadcastSystemMessage(String message) {
        // Broadcast to other clients.
        for (ClientThreadPC ct : threads) {
            if (ct != this) {
                ct.writeToClient("BCST - SYSTEM: " + message);
            }
        }
        //Let the client know their message has been broadcasted.
        writeToClient("+OK");
    }

    /**
     * Helper method that will be called by the run if the message received is of type "BCST", meaning we have to send it to all other clients.
     *
     * @param message Message is the user's message we received and was checked by the run.
     */
    private void broadcastMessage(Message message) {
        // Broadcast to other clients.
        for (ClientThreadPC ct : threads) {
            if (ct != this) {
                //Send this message to all other clients that aren't this one.
                ct.writeToClient("BCST - " + getUsername() + ": " + message.getPayload());
            }
        }
        //Let the client know their message has been broadcasted.
        writeToClient("+OK");
    }

    /*--- START OF FILE-TRANSFER HANDLING --------------------------------------------------------------------*/

    private void startUploading(Message message) {
        ArrayList<String> commandStringGRP = new ArrayList<>(Arrays.asList(message.getPayload().split(" ")));
        sender = extractCertainPartOfString(commandStringGRP, 0);
        receiver = extractCertainPartOfString(commandStringGRP, 1);
        fileSize = Integer.parseInt(extractCertainPartOfString(commandStringGRP, 2));
        fileExtension = extractCertainPartOfString(commandStringGRP, 3);
        ClientThreadPC receiverThread = getMember(receiver);
        if (receiverThread != null){
            if (!receiverThread.isReceivingAFile()){
                receiverThread.setReceiving(true);
                sending = true;
            } else {
                getMember(sender).writeToClient("-ERR User is busy with another file transfer." +
                        " Please wait a moment and try again! :)");
                deleteThread();
                try {
                    socket.close();
                } catch (IOException e) {
                    System.out.println("Socket was closed. Upload  was stopped. Reason: User is busy.");
                }
            }
        } else {
            getMember(sender).writeToClient("-ERR User does not exist.");
        }

    }

    private void uploadFileToServer() throws IOException {
        DataInputStream dis = null;
        try  {
            dis = new DataInputStream(socket.getInputStream());
            byte[] buffer = new byte[16000];

            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            int filesize = fileSize; // Send file size in separate msg
            int read;
            int totalRead = 0;
            int remaining = filesize;
            while ((read = dis.read(buffer, 0, Math.min(buffer.length, remaining))) > 0) {
                totalRead += read;
                remaining -= read;
//            System.out.println("read " + totalRead + " bytes.");
                stream.write(buffer, 0, read);
            }
            stream.close();
            uniqueNumber = System.currentTimeMillis();
            singleton.getFiles().add(new FileToTransfer(sender, receiver, stream.toByteArray(), filesize, uniqueNumber));
            askUserForDownloadApproval();
            dis.close();
        } catch (OutOfMemoryError OFMR){
            getMember(sender).writeToClient("-ERR Too big file.");
            getMember(receiver).setReceiving(false);
        } finally {
            deleteThread();
            socket.close();
        }
    }

    private void askUserForDownloadApproval() {
        if (findMember(receiver)) {
            for (ClientThreadPC ct : threads) {
                if (ct != this && ct.getUsername().equals(receiver)) {
                    ct.writeToClient("RQST " + sender + " " + fileSize + " " + fileExtension + " " + uniqueNumber);
                    break;
                }
            }
        }
        else {
            for (ClientThreadPC ct : threads) {
                if (ct != this && ct.getUsername().equals(sender)) {
                    ct.writeToClient("-ERR User does not exist.");
                    break;
                }
            }
        }
    }

    private void handlingDownloadResponse(Message message) throws IOException {
        ArrayList<String> commandStringGRP = new ArrayList<>(Arrays.asList(message.getPayload().split(" ")));
        String response = extractCertainPartOfString(commandStringGRP, 0);
        String receiver = extractCertainPartOfString(commandStringGRP, 1);
        String receiverName = extractCertainPartOfString(commandStringGRP, 2);
        if (response.equals("ready")) {
            getMember(receiverName).setReceiving(false);
            sendBytes(singleton.getFile(Long.parseLong(receiver)));
        } else if (response.equals("accept")) {
            String uniqueNumber = extractCertainPartOfString(commandStringGRP, 1);
            writeToClient("BEGIN_DNLD " + singleton.getFile(Long.parseLong(uniqueNumber)).getFileSize());
        } else if (response.equals("decline")) {
            String fileNumber = extractCertainPartOfString(commandStringGRP, 1);
            String senderName = singleton.getFile(Long.parseLong(fileNumber)).getSender();
            getMember(senderName).writeToClient("-ERR Client declined the file transfer.");
            setReceiving(false);
        }
    }

    private void sendBytes(FileToTransfer fileToTransfer) throws IOException {
        DataOutputStream dos = new DataOutputStream(os);
        ByteArrayInputStream fis = new ByteArrayInputStream(fileToTransfer.getFile());
        byte[] buffer = new byte[4096];

        while (fis.read(buffer) > 0) {
            dos.write(buffer);
        }

//        fis.close();
        deleteThread();
//        dos.close();
    }

    /*--- END OF FILE-TRANSFER HANDLING --------------------------------------------------------------------*/

    /*--- START OF GROUP-RELATED MESSAGE HANDLING --------------------------------------------------------------------*/

    /**
     * Method that checks the type of GROUP message and decides to which group method to send it to.
     *
     * @param message is the message received from the client.
     */
    private void handleGroupMessages(Message message) {
        ArrayList<String> commandStringGRP = new ArrayList<>(Arrays.asList(message.getPayload().split(" ")));

        String command = extractCertainPartOfString(commandStringGRP, 1);

        switch (command) {
            //See which type of group command to handle and call the according helper methods.
            case "create": {
                createGroup(commandStringGRP);
                break;
            }
            case "join": {
                joinGroup(commandStringGRP);
                break;
            }
            case "allgroups":
                writeToClient("+GRP " + groups);
                break;
            case "leave": {
                leaveGroup(commandStringGRP);
                break;
            }
            case "kick": {
                kickGroupMember(commandStringGRP);
                break;
            }
            case "msg": {
                sendGroupMessage(commandStringGRP);
                break;
            }
            default:
                writeToClient("-ERR Wrong use of command /grp. Should be used like: /grp [create <group-name> || join <group-name> || leave <group-name> || kick <group-name> <username> || msg <message> || allgroups]");
                break;
        }
    }

    /**
     * Method that handles a user's request to create a new group. Makes the group and adds the user to it as an owner if possible.
     *
     * @param commandStringGRP is a split version of the message the user typed to the server.
     */
    private void createGroup(ArrayList<String> commandStringGRP) {
        String groupName = extractCertainPartOfString(commandStringGRP, 2);

        if (groupName.equalsIgnoreCase("e")) {
            //If the groupname could not be extracted, meaning the user only typed "/grp create".
            writeToClient("-ERR Wrong usage of command /grp create. Should be used like: /grp create <group-name>");
            return;
        }

        if (groupExists(groupName)) {
            writeToClient("-ERR Group name is already in use.");
        } else if (!groupExists(groupName)) {
            groups.add(new Group(groupName, this));
            Group lastGroup = groups.get(groups.size() - 1);

            writeToClient("+GRP Group created. " + lastGroup.getGroupName() + ". Owned by: " + lastGroup.getOwner());
        }
    }

    /**
     * Method that handles a user's request to join a group. Puts the user in the group if possible, otherwise returns feedback error to user.
     *
     * @param commandStringGRP is a split version of the message the user typed to the server.
     */
    private void joinGroup(ArrayList<String> commandStringGRP) {
        String groupName = extractCertainPartOfString(commandStringGRP, 2);

        if (groupName.equalsIgnoreCase("e")) {
            //If the groupname could not be extracted, meaning the user only typed "/grp join".
            writeToClient("-ERR Wrong usage of command /grp join. Should be used like: /grp join <group-name>");
            return;
        }

        if (groupExists(groupName)) {
            Group groupToJoin = findGroup(groupName);
            ClientThreadPC isAlreadyAMember = groupToJoin.exists(this);

            writeToClient("+GRP " + groupToJoin.join(this));

            if (isAlreadyAMember == null) {
                String notifyGroupMembers = "/grp msg " + groupName + " has joined the group! Welcome them to the brotherhood!";
                ArrayList<String> temporary = new ArrayList<>(Arrays.asList(notifyGroupMembers.split(" ")));

                sendGroupMessage(temporary);
            }
        } else {
            writeToClient("-ERR Group that you are trying to join does not exist.");
        }
    }

    /**
     * ...
     *
     * @param commandStringGRP is a split version of the message the user typed to the server.
     */
    private void leaveGroup(ArrayList<String> commandStringGRP) {
        String groupName = extractCertainPartOfString(commandStringGRP, 2);

        if (groupName.equalsIgnoreCase("e")) {
            //If the groupname could not be extracted, meaning the user only typed "/grp leave".
            writeToClient("-ERR Wrong usage of command /grp leave. Should be used like: /grp leave <group-name>");
            return;
        }

        if (groupExists(groupName)) {
            String notifyGroupMembers = "/grp msg " + groupName + " has left the group. A moment of silence for the loss.";
            ArrayList<String> temporary = new ArrayList<>(Arrays.asList(notifyGroupMembers.split(" ")));

            sendGroupMessage(temporary);
            writeToClient("+GRP " + findGroup(groupName).leave(this));
        } else {
            writeToClient("-ERR Group that you are trying to leave does not exist.");
        }
    }

    /**
     * Helper method that is called when the user send a message starting with the "GRP /grp kick" command.
     *
     * @param commandStringGRP is a split version of the message the user typed to the server.
     */
    private void kickGroupMember(ArrayList<String> commandStringGRP) {
        String groupName;
        String memberToKick;

        if (commandStringGRP.size() > 3) {
            groupName = commandStringGRP.get(2);
            memberToKick = commandStringGRP.get(3);
        } else {
            //If something could not be extracted, meaning the user only typed "/grp kick" or "/grp kick <groupName>".
            writeToClient("-ERR Wrong command use, please use it like: /grp kick <group-name> <user-name>");
            return;
        }

        Group specifiedGroup = findGroup(groupName);

        if (findGroup(groupName) != null && specifiedGroup.exists(this) != null) {
            //Check if the specified group exists and if this client is a member of said group.
            ClientThreadPC ct = specifiedGroup.getMember(memberToKick);

            if (ct != null) {
                //If the member we want to kick exists.
                writeToClient("+GRP " + specifiedGroup.kickMember(this, ct));

                if (this.getUsername().equals(specifiedGroup.getOwner().getUsername()) && !specifiedGroup.getOwner().getUsername().equals(memberToKick)) {
                    //If the user that's trying to kick someone isn't the owner trying to kick themselves, then notify the rest of the group. Also do not allow
                    ct.writeToClient("+GRP You have been kicked from the group " + groupName + " by the owner.");
                    String notifyGroupMembers = "/grp msg " + groupName + " (the owner) has kicked the user " + memberToKick + " from the group. Let this be a lesson for them.";
                    ArrayList<String> temporary = new ArrayList<>(Arrays.asList(notifyGroupMembers.split(" ")));

                    sendGroupMessage(temporary);
                }
            } else {
                writeToClient("-ERR The member you're trying to kick is not in this group or does not exist at all.");
            }
        } else {
            writeToClient("-ERR The group that you are trying to kick members from does not exist, or you're not a member of this group.");
        }
    }

    /**
     * Helper method that is called when the user send a message starting with the "GRP /grp msg" command.
     *
     * @param commandStringGRP is a split version of the message the user typed to the server.
     */
    private void sendGroupMessage(ArrayList<String> commandStringGRP) {
        String groupName = extractCertainPartOfString(commandStringGRP, 2);

        if (groupName.equalsIgnoreCase("e")) {
            //If the groupname could not be extracted, meaning the user only typed "/grp msg".
            writeToClient("-ERR Wrong usage of command /grp msg. Should be used like: /grp msg <group-name> <message...>");
            return;
        }

        if (groupExists(groupName)) {
            //If the group with the given name exists.
            Group group = findGroup(groupName);

            if (group.exists(this) != null) {
                //If this client is a member of the group.
                List<String> messageStrings = commandStringGRP.subList(3, commandStringGRP.size());
                StringBuilder fullMessage = new StringBuilder();

                for (String msg : messageStrings) {
                    fullMessage.append(msg).append(" ");
                }

                for (ClientThreadPC ct : group.getMembers()) {
                    if (ct != this) {
                        //Send the message to all clients that are in this group and that are not this client.
                        ct.writeToClient("GRPMSG - " + groupName + " " + getUsername() + ": " + fullMessage);
                    }
                }

                writeToClient("+OK");
            } else {
                writeToClient("-ERR You are not a member of this group.");
            }
        } else {
            writeToClient("-ERR Group doesn't exist.");
        }
    }

    /**
     * Helper method that checks if the group with the given name exists in the server.
     *
     * @param groupName String is the name of the group we want to know of if it exists.
     * @return true if the group exists, false if it does not.
     */
    private boolean groupExists(String groupName) {
        if (groups.size() > 0) {
            //If there are groups.
            for (Group group : groups) {
                //Loop through all groups.
                if (groupName.equals(group.getGroupName())) {
                    //If this group has the same name as the given name, return true.
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Helper method that can be used to retrieve a group with the given name.
     *
     * @param groupName String is the name of the group we want to get the instance of.
     * @return group Group with the given name that we found. Null if we cannot find a group with that name.
     */
    private Group findGroup(String groupName) {
        for (Group group : groups) {
            if (groupName.equals(group.getGroupName())) {
                //If we found a group with the given name, return it.
                return group;
            }
        }

        return null;
    }

    /**
     * Helper method that can be used to know if a user with the given username exists in the system.
     *
     * @param user String is the username of which we want to get the knowledge if it exists or not.
     * @return boolean true if the user with the username exists, false if it does not.
     */
    private boolean findMember(String user) {
        for (ClientThreadPC ct : threads) {
            if (ct.getUsername() != null) {
                if (ct.getUsername().equals(user)) {
                    //If the username of the thread equals the given username.
                    return true;
                }
            }
        }
        return false;
    }

    private ClientThreadPC getMember(String user) {
        for (ClientThreadPC ct : threads) {
            if (ct.getUsername() != null) {
                if (ct.getUsername().equals(user)) {
                    //If the username of the thread equals the given username.
                    return ct;
                }
            }
        }
        return null;
    }

    /*--- END OF GROUP-RELATED MESSAGE HANDLING ----------------------------------------------------------------------*/

    /**
     * An external process can stop the client using this method.
     */
    public void kill() {
        try {
            // Log connection drop and close the outputstream.
            System.out.println("[DROP CONNECTION] " + getUsername());
            threads.remove(this);
            socket.close();
        } catch (Exception ex) {
            System.out.println("Exception when closing outputstream: " + ex.getMessage());
        }
        state = FINISHED;
    }

    /**
     * Write a message to this client thread.
     *
     * @param message The message to be sent to the (connected) client.
     */
    private void writeToClient(String message) {
        boolean shouldDropPacket = false;
        boolean shouldCorruptPacket = false;

        // Check if we need to behave badly by dropping some messages.
        if (conf.doSimulateDroppedPackets()) {
            // Randomly select if we are going to drop this message or not.
            int random = new Random().nextInt(6);
            if (random == 0) {
                // Drop message.
                shouldDropPacket = true;
                System.out.println("[DROPPED] " + message);
            }
        }

        // Check if we need to behave badly by corrupting some messages.
        if (conf.doSimulateCorruptedPackets()) {
            // Randomly select if we are going to corrupt this message or not.
            int random = new Random().nextInt(4);
            if (random == 0) {
                // Corrupt message.
                shouldCorruptPacket = true;
            }
        }

        // Do the actual message sending here.
        if (!shouldDropPacket) {
            if (shouldCorruptPacket) {
                message = corrupt(message);
                System.out.println("[CORRUPT] " + message);
            }
            PrintWriter writer = new PrintWriter(os);
            writer.println(message);
            writer.flush();

            // Echo the message to the server console for debugging purposes.
            boolean isIncomingMessage = false;
            logMessage(isIncomingMessage, message);
        }
    }

    /**
     * This methods implements a (naive) simulation of a corrupt message by replacing
     * some charaters at random indexes with the charater X.
     *
     * @param message The message to be corrupted.
     * @return Returns the message with some charaters replaced with X's.
     */
    private String corrupt(String message) {
        Random random = new Random();
        int x = random.nextInt(4);
        char[] messageChars = message.toCharArray();

        while (x < messageChars.length) {
            messageChars[x] = 'X';
            x = x + random.nextInt(10);
        }

        return new String(messageChars);
    }

    /**
     * Util method to print (debug) information about the server's incoming and outgoing messages.
     *
     * @param isIncoming Indicates whether the message was an incoming message. If false then
     *                   an outgoing message is assumed.
     * @param message    The message received or sent.
     */
    private void logMessage(boolean isIncoming, String message) {
        String logMessage;
        String colorCode = conf.CLI_COLOR_OUTGOING;
        String directionString = ">> ";  // Outgoing message.
        if (isIncoming) {
            colorCode = conf.CLI_COLOR_INCOMING;
            directionString = "<< ";     // Incoming message.
        }

        // Add username to log if present.
        // Note when setting up the connection the user is not known.
        if (getUsername() == null) {
            logMessage = directionString + message;
        } else {
            logMessage = directionString + "[" + getUsername() + "] " + message;
        }

        // Log debug messages with or without colors.
        if (conf.isShowColors()) {
            System.out.println(colorCode + logMessage + conf.RESET_CLI_COLORS);
        } else {
            System.out.println(logMessage);
        }
    }

    private void deleteThread() {
        for (ClientThreadPC ct : threads) {
            if (ct == this) {
                threads.remove(ct);
                break;
            }
        }
    }

    @Override
    public String toString() {
        return this.getUsername();
    }
}
