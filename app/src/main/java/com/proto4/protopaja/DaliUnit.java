package com.proto4.protopaja;

import java.util.ArrayList;

/**
 * Created by user on 30.06.17.
 */

public class DaliUnit {

    private String name;
    ArrayList<DaliUnit> group;

    public DaliUnit(String _name) {
        name = _name;
    }

    public void addGroupMember(DaliUnit device) {
        if (group == null)
            group = new ArrayList<>();
        group.add(device);
    }

    public void removeGroupMember(DaliUnit device){
        if (group == null)
            return;
        group.remove(device);
        if (group.size() == 0)
            group = null;
    }

    public boolean isGroup(){
        return group != null;
    }

    public String getName(){
        return name;
    }

    public ArrayList<DaliUnit> getGroup(){
        return group;
    }

}