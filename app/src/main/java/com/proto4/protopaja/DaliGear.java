package com.proto4.protopaja;

import java.util.ArrayList;

/**
 * Created by user on 4.07.17.
 */

public class DaliGear {

    private String name;
    ArrayList<DaliGear> group;

    public DaliGear(String _name) {
        name = _name;
    }

    public void setName(String _name){
        name = _name;
    }

    public void addGroupMember(DaliGear gear) {
        if (group == null)
            group = new ArrayList<>();
        group.add(gear);
    }

    public void removeGroupMember(DaliGear gear){
        if (group == null)
            return;
        group.remove(gear);
        if (group.size() == 0)
            group = null;
    }

    public boolean isGroup(){
        return group != null;
    }

    public String getName(){
        return name;
    }

    public ArrayList<DaliGear> getGroup(){
        return group;
    }
}
