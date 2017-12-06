package nl.saxion.internettech;

import java.util.ArrayList;

public class Group {

    private String owner;
    private String groupName;
    private ArrayList<String> members = new ArrayList<>();

    public Group(String groupName, String owner) {
        this.groupName = groupName;
        this.owner = owner;
        members.add(owner);
    }

    public void addMember(String owner, String username) {
        if (owner.equals(this.owner)) {
            if (!exists(username)) {
                members.add(username);
            }
        }
    }

    public String join(String username) {
        if (!exists(username)) {
            members.add(username);
            return "You have joined the group";
        }
        return "You are already a member of this group";
    }

    public String leave(String username){
        if (exists(username)){
            for (int i = 0; i < members.size(); i++) {
                if (username.equals(members.get(i))) {
                    members.remove(i);
                    return "You have left the group.";
                }
            }
        }
        return "You are not a member of this group.";
    }
    public String kickMember(String owner, String username) {
        if (owner.equals(this.owner)) {
            if (exists(username)) {
                for (int i = 0; i < members.size(); i++) {
                    if (username.equals(members.get(i)) && !username.equals(this.owner)) {
                        members.remove(i);
                        return "Member kicked.";
                    } else {
                        return "Member could not be kicked.";
                    }
                }
            }
            return "Member doesn't exist";
        }
        return "You have no permissions to kick.";
    }

    public boolean exists(String username) {
        if (members.size() > 0) {
            for (String member : members) {
                if (username.equals(member)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getOwner() {
        return owner;
    }

    public String getGroupName() {
        return groupName;
    }

    public ArrayList<String> getMembers() {
        return members;
    }

    @Override
    public String toString() {
        return "Group name: " + this.groupName +
                "|| Owner: " + this.owner +
                "|| Members: " + this.members;
    }
}
