package nl.saxion.internettech;

import java.util.ArrayList;

public class Group {

    private ClientThreadPC owner;
    private String groupName;
    private ArrayList<ClientThreadPC> members = new ArrayList<>();

    public Group(String groupName, ClientThreadPC owner) {
        this.groupName = groupName;
        this.owner = owner;
        members.add(owner);
    }

    public void addMember(ClientThreadPC owner, ClientThreadPC username) {
        if (owner.equals(this.owner)) {
            if (!exists(username)) {
                members.add(username);
            }
        }
    }

    public String join(ClientThreadPC user) {
        if (!exists(user)) {
            members.add(user);
            return "You have joined the group";
        }
        return "You are already a member of this group";
    }

    public String leave(ClientThreadPC user){
        if (exists(user)){
            for (int i = 0; i < members.size(); i++) {
                if (user.getUsername().equals(members.get(i).getUsername())) {
                    members.remove(i);
                    return "You have left the group.";
                }
            }
        }
        return "You are not a member of this group.";
    }
    public String kickMember(ClientThreadPC owner, ClientThreadPC user) {
        if (owner.equals(this.owner)) {
            if (!user.getUsername().equals(this.owner.getUsername())) {
                if (exists(user)) {
                    for (int i = 0; i < members.size(); i++) {
                        if (members.get(i).getUsername().equals(user.getUsername())){
                            members.remove(i);
                            return "Member has been kicked.";
                        }
                    }
                } else {
                    return "The member that you are trying to kick does not belong to this group.";
                }
            } else {
                return "You cannot kick the Owner of the group.";
            }
        } else {
            return "You have no permissions to kick.";
        }
        return null;
    }

    public boolean exists(ClientThreadPC user) {
        if (members.size() > 0) {
            for (ClientThreadPC member : members) {
                if (user.getUsername().equals(member.getUsername())) {
                    return true;
                }
            }
        }
        return false;
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
                "|| Owner: " + this.owner +
                "|| Members: " + this.members;
    }
}
