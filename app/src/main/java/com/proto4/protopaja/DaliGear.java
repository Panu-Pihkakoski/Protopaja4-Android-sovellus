package com.proto4.protopaja;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by user on 4.07.17.
 */

public class DaliGear {

    private static final String TAG = DaliGear.class.getSimpleName();

    private String name;
    ArrayList<DaliGear> group;

    private int status;
    private boolean showInfo;

    public static final int STATUS_BALLAST_FAILURE = 1;
    public static final int STATUS_LAMP_FAILURE = 2;
    public static final int STATUS_POWER_ON = 4;
    public static final int STATUS_LIMIT_ERROR = 8;
    public static final int STATUS_FADE_RUNNING = 16;
    public static final int STATUS_RESET_STATE = 32;
    public static final int STATUS_MISSING_S_ADDR = 64;
    public static final int STATUS_POWER_FAILURE = 128;

    public DaliGear(){
        this("unknown");
    }

    public DaliGear(String _name) {
        Log.d(TAG, "constructing new gear object");
        name = _name;
        status = 0;
        showInfo = false;
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

    public String getInfoString() {
        Log.d(TAG, "getInfoString()");
        String info = "";
        info += (status & STATUS_BALLAST_FAILURE) == 0 ? "" : "(!) Ballast failure\n";
        info += (status & STATUS_LAMP_FAILURE) == 0 ? "" : "(!) Lamp failure\n";
        info += (status & STATUS_POWER_ON) == 0 ? "Power off\n" : "Power on\n";
        info += (status & STATUS_LIMIT_ERROR) == 0 ? "" : "(!) Limit error\n";
        info += (status & STATUS_FADE_RUNNING) == 0 ? "" : "Fade running\n";
        info += (status & STATUS_RESET_STATE) == 0 ? "" : "Reset state\n";
        info += (status & STATUS_MISSING_S_ADDR) == 0 ? "" : "(!) Missing short addr\n";
        info += (status & STATUS_POWER_FAILURE) == 0 ? "" : "(!) Power failure\n";

        return info;
    }

    public void showInfo(boolean show){
        showInfo = show;
    }

    public boolean showInfo(){
        return showInfo;
    }

    public ArrayList<DaliGear> getGroup(){
        return group;
    }

    public int getStatus(){
        return status;
    }

    public void setStatus(int _status){
        status = _status;
    }
}
