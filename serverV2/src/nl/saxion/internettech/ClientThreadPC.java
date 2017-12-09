package nl.saxion.internettech;

import java.io.*;
import java.net.Socket;
import java.util.*;

import static nl.saxion.internettech.ServerState.*;
import static nl.saxion.internettech.ServerState.FINISHED;

/**
 * The public class of client thread.
 */
public class ClientThreadPC implements Runnable {
    private DataInputStream is;
    private OutputStream os;
    private Socket socket;
    private ServerState state;
    private String username;

    private Server server;

    private Set<ClientThreadPC> threads;
    private ServerConfiguration conf;
    private ArrayList<Group> groups = new ArrayList<>();

    public ClientThreadPC(Server server, Socket socket) {
        this.state = INIT;
        this.socket = socket;

        this.server = server;
        this.threads = this.server.getThreads();
        this.conf = this.server.getConf();
        this.groups = this.server.getGroups();
    }

    public String getUsername() {
        return username;
    }

    public OutputStream getOutputStream() {
        return os;
    }

    public void run() {
        try {
            // Create input and output streams for the socket.
            os = socket.getOutputStream();
            is = new DataInputStream(socket.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

            // According to the protocol we should send HELO <welcome message>
            state = CONNECTING;
            String welcomeMessage = "HELO " + conf.WELCOME_MESSAGE;
            writeToClient(welcomeMessage);

            while (!state.equals(FINISHED)) {
                // Wait for message from the client.
                String line = reader.readLine();

                if (line != null) {
                    // Log incoming message for debug purposes.
                    boolean isIncomingMessage = true;
                    logMessage(isIncomingMessage, line);

                    // Parse incoming message.
                    Message message = new Message(line);

                    // Process message.
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
                    }
                }
            }
            // Remove from the list of client threads and close the socket.
            threads.remove(this);
            socket.close();
        } catch (IOException e) {
            System.out.println("Server Exception: " + e.getMessage());
        }
    }

    private String extractCertainPartOfString(ArrayList<String> strings, int index) {
        String desiredString;

        if (strings.size() > index) {
            desiredString = strings.get(index);
        } else {
            return "e";
        }

        return desiredString;
    }

    private void sendUsersList(Message message) {
        ArrayList<String> commandString = new ArrayList<>(Arrays.asList(message.getPayload().split(" ")));

        StringBuilder sb = new StringBuilder();

        if (commandString.size() > 1) {
            String groupToGetUsersOf = commandString.get(1);

            if (groupExists(groupToGetUsersOf)) {
                sb.append("USRS," + groupToGetUsersOf);

                Group group = findGroup(groupToGetUsersOf);
                ArrayList<ClientThreadPC> groupMembers = group.getMembers();

                sb.append(",");
                sb.append(group.getOwner());
                sb.append("::[OWNER]");

                for (int i = 1; i < groupMembers.size(); i++) {
                    //Start from the second index (1), as on the first index (0) of the list the owner is always located at the start of the list.
                    sb.append(",");
                    sb.append(groupMembers.get(i).getUsername());
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

    private void sendWhisper(Message message) {
        ArrayList<String> myList = new ArrayList<>(Arrays.asList(message.getPayload().split(" ")));

        String userToWhisper = extractCertainPartOfString(myList, 1);

        if (userToWhisper.equalsIgnoreCase("e")) {
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

    private void handleUserLogin(Message message) {
        // Check username format.
        boolean isValidUsername = message.getPayload().matches("[a-zA-Z0-9_]{3,14}");

        if (!isValidUsername) {
            state = FINISHED;
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
                state = CONNECTED;

                this.username = message.getPayload();
                writeToClient("+OK " + getUsername());
            }
        }
    }

    private void broadcastMessage(Message message) {
        // Broadcast to other clients.
        for (ClientThreadPC ct : threads) {
            if (ct != this) {
                ct.writeToClient("BCST - " + getUsername() + ": " + message.getPayload());
            }
        }

        writeToClient("+OK");
    }

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
            writeToClient("-ERR Wrong usage of command /grp join. Should be used like: /grp join <group-name>");
            return;
        }

        if (groupExists(groupName)) {
            Group groupToJoin = findGroup(groupName);
            boolean wasPreviouslyAMember = groupToJoin.exists(this);

            writeToClient("+GRP " + groupToJoin.join(this));

            if (!wasPreviouslyAMember) {
                String notifyGroupMembers = "/grp msg " + groupName + " has joined the group! Welcome them to the brotherhood!";
                ArrayList<String> temporary = new ArrayList<>(Arrays.asList(notifyGroupMembers.split(" ")));

                sendGroupMessage(temporary);
            }
        } else {
            writeToClient("-ERR Group that you are trying to join does not exist.");
        }
    }

    private void leaveGroup(ArrayList<String> commandStringGRP) {
        String groupName = extractCertainPartOfString(commandStringGRP, 2);

        if (groupName.equalsIgnoreCase("e")) {
            writeToClient("-ERR Wrong usage of command /grp join. Should be used like: /grp join <group-name>");
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

    private void kickGroupMember(ArrayList<String> commandStringGRP) {
        String groupName;
        String memberToKick;

        if (commandStringGRP.size() > 3) {
            groupName = commandStringGRP.get(2);
            memberToKick = commandStringGRP.get(3);
        } else {
            writeToClient("-ERR Wrong command use, please use it like: /grp kick <group-name> <user-name>");
            return;
        }

        Group specifiedGroup = findGroup(groupName);

        if (findGroup(groupName) != null && specifiedGroup.exists(this)) {
            ClientThreadPC ct = specifiedGroup.getMember(memberToKick);

            writeToClient("+GRP " + specifiedGroup.kickMember(this, ct));

            if (this.getUsername().equals(specifiedGroup.getOwner().getUsername()) && !specifiedGroup.getOwner().getUsername().equals(memberToKick)) {
                //If the user that's trying to kick someone isn't the owner trying to kick themselves, then notify the rest of the group. Also do not allow
                ct.writeToClient("+GRP You have been kicked from the group " + groupName + " by the owner.");
                String notifyGroupMembers = "/grp msg " + groupName + " (the owner) has kicked the user " + memberToKick + " from the group. Let this be a lesson for them.";
                ArrayList<String> temporary = new ArrayList<>(Arrays.asList(notifyGroupMembers.split(" ")));

                sendGroupMessage(temporary);
            }
        } else {
            writeToClient("-ERR The group that you are trying to kick members from does not exist, or you're not a member of this group.");
        }
    }

    private void sendGroupMessage(ArrayList<String> commandStringGRP) {
        String groupName = extractCertainPartOfString(commandStringGRP, 2);

        if (groupName.equalsIgnoreCase("e")) {
            writeToClient("-ERR Wrong usage of command /grp msg. Should be used like: /grp msg <group-name> <message...>");
            return;
        }

        if (groupExists(groupName)) {
            Group group = findGroup(groupName);

            if (group.exists(this)) {
                List<String> messageStrings = commandStringGRP.subList(3, commandStringGRP.size());
                StringBuilder fullMessage = new StringBuilder();

                for (String msg : messageStrings) {
                    fullMessage.append(msg).append(" ");
                }

                for (ClientThreadPC ct : group.getMembers()) {
                    if (ct != this) {
                        ct.writeToClient("GRPMSG - " + groupName + " " + getUsername() + ": " + fullMessage);
                    }
                }
            } else {
                writeToClient("-ERR You are not a member of this group.");
            }
        } else {
            writeToClient("-ERR Group doesn't exist.");
        }
    }

    private boolean groupExists(String groupName) {
        if (groups.size() > 0) {
            for (int i = 0; i < groups.size(); i++) {
                if (groupName.equals(groups.get(i).getGroupName())) {
                    return true;
                }
            }
        }
        return false;
    }

    private Group findGroup(String groupName) {
        for (int i = 0; i < groups.size(); i++) {
            if (groupName.equals(groups.get(i).getGroupName())) {
                return groups.get(i);
            }
        }
        return null;
    }

    private boolean findMember(String user) {
        for (ClientThreadPC ct : threads) {
            if (ct.getUsername().equals(user)) {
                return true;
            }
        }
        return false;
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

    @Override
    public String toString() {
        return this.getUsername();
    }
}
