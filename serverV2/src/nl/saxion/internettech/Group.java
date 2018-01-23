package nl.saxion.internettech;

import java.util.ArrayList;

/**
 * Class that represents a group of people in a server, created using 'GRP /grp create <groupName>'
 */
public class Group {
    private ClientThreadPC owner;
    private String groupName;
    private ArrayList<ClientThreadPC> members = new ArrayList<>();

    public Group(String groupName, ClientThreadPC owner) {
        this.groupName = groupName;
        this.owner = owner;
        members.add(owner);
    }

    /**
     * Method used by the user to join a new client to this group, as they requested to do so.
     *
     * @param user ClientThreadPC is a currently connected client instance that we want to join into this group.
     * @return String message to inform if we were able to add the client to the group or not.
     */
    public String join(ClientThreadPC user) {
        ClientThreadPC temp = exists(user);

        if (temp == null) {
            //If the user isn't already in the group, add them to the list of members in this group.
            members.add(user);
            return "You have joined the group";
        }
        return "You are already a member of this group";
    }

    /**
     * Method that is used for when a client lets us know they want to leave a group.
     *
     * @param user ClientThreadPC is a currently connected client instance that we want to remove from this group.
     * @return String message to inform if we were able to remove the client from the group or not.
     */
    public String leave(ClientThreadPC user) {
        ClientThreadPC temp = exists(user);

        if (temp != null) {
            //If the user is even a registered member of the group.
            members.remove(temp);

            return "You have left the group.";
        }

        return "You are not a member of this group.";
    }

    /**
     * Method that is used when any client requests to kick another user from a certain group.
     *
     * @param owner ClientThreadPC is the owner of the group where we want to kick a member from.
     * @param user ClientThreadPC is a currently connected client instance that the owner wants to remove from this group.
     * @return String message to inform if we were able to remove the client from the group or not.
     */
    public String kickMember(ClientThreadPC owner, ClientThreadPC user) {
        if (owner.equals(this.owner)) {
            //If this person is even the owner of the group.
            if (!user.getUsername().equals(this.owner.getUsername())) {
                //If the username of the person to kick is not the same as the owner's
                ClientThreadPC temp = exists(user);

                if (temp != null) {
                    //If the given user is even in this group
                    members.remove(temp);
                    return "Member " + user.getUsername() + " has been kicked.";
                } else {
                    return "The member that you are trying to kick does not belong to this group.";
                }
            } else {
                return "You cannot kick yourself, as you are the owner.";
            }
        } else {
            return "You have no permissions to kick.";
        }
    }

    /**
     * Method that can be used to check if the given ClientThreadPC thread is in this group or not.
     *
     * @param user ClientThreadPC is the client instance to check if they are in this group.
     * @return ClientThreadPC is the given user is in the group, null if it is not.
     */
    public ClientThreadPC exists(ClientThreadPC user) {
        if (members.size() > 0) {
            //If there are members in the list
            for (ClientThreadPC member : members) {
                //Go through the list of members
                if (user.getUsername().equals(member.getUsername())) {
                    //And you find one that has the same username as this one
                    return member;
                }
            }
        }

        return null;
    }

    /**
     * Method that can be used to check if a client with the given String username is in this group or not.
     *
     * @param username String is the username of the client that supposedly is in this group and we want to get
     * @return ClientThreadPC is the given user is in the group, null if it is not.
     */
    public ClientThreadPC getMember(String username) {
        if (members.size() > 0) {
            //If there are members in the list
            for (ClientThreadPC member : members) {
                //Go through the list of members
                if (username.equals(member.getUsername())) {
                    //And you find one that has the same username as this one
                    return member;
                }
            }
        }

        return null;
    }

    public ClientThreadPC getOwner() {
        return owner;
    }

    public String getGroupName() {
        return groupName;
    }

    public ArrayList<ClientThreadPC> getMembers() {
        return members;
    }

    @Override
    public String toString() {
        return "Group name: " + this.groupName +
                " || Owner: " + this.owner +
                " || Members: " + this.members;
    }
}
