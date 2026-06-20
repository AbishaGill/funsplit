package com.finsplit.app.models;

import com.google.firebase.firestore.DocumentId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Group {

    @DocumentId
    private String groupId;
    private String groupName;
    private List<String> members;           // list of uids
    private Map<String, Double> balances;   // uid → net balance (positive = owed to you)

    public Group() {
        this.balances = new HashMap<>();
    }

    public Group(String groupName, List<String> members) {
        this.groupName = groupName;
        this.members = members;
        this.balances = new HashMap<>();
        for (String uid : members) {
            balances.put(uid, 0.0);
        }
    }

    public String getGroupId()               { return groupId; }
    public String getGroupName()             { return groupName; }
    public List<String> getMembers()         { return members; }
    public Map<String, Double> getBalances() { return balances; }

    public void setGroupId(String groupId)               { this.groupId = groupId; }
    public void setGroupName(String groupName)           { this.groupName = groupName; }
    public void setMembers(List<String> members)         { this.members = members; }
    public void setBalances(Map<String, Double> balances){ this.balances = balances; }
}
