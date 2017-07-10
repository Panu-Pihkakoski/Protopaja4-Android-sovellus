package com.proto4.protopaja;

import android.util.Log;

import java.util.ArrayList;

/**
 * Created by user on 4.07.17.
 */

public class DaliGear {

    private static final String TAG = DaliGear.class.getSimpleName();

    private String name;
    private ArrayList<DaliGear> group;

    private int id;
    private int status;

    public static final int STATUS_BALLAST_FAILURE = 1;
    public static final int STATUS_LAMP_FAILURE = 2;
    public static final int STATUS_POWER_ON = 4;
    public static final int STATUS_LIMIT_ERROR = 8;
    public static final int STATUS_FADE_RUNNING = 16;
    public static final int STATUS_RESET_STATE = 32;
    public static final int STATUS_MISSING_S_ADDR = 64;
    public static final int STATUS_POWER_FAILURE = 128;

    public DaliGear(){
        this("unknown", 0);
    }

    public DaliGear(String _name) {
        this(_name, 0);
    }

    public DaliGear(int _id) {
        this("unknown", _id);
    }

    public DaliGear(String _name, int _id) {
        name = _name;
        id = _id;
        status = 0;
    }


    public void setName(String _name){
        name = _name;
    }

    public String getName(){
        return name;
    }

    public void setId(int _id) {
        id = _id;
    }

    public int getId() {
        return id;
    }

    public void setStatus(int _status) {
        status = _status;
    }

    public int getStatus(){
        return status;
    }

    public boolean isGroup(){
        return group != null;
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

    public ArrayList<DaliGear> getGroup(){
        return group;
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




    /*public void setStatus(int _status){
        status = _status;
        if (listener != null)
            listener.onGearStatusUpdated(this);
    }*/

    public interface StatusUpdateListener {
        void onGearStatusUpdated(DaliGear gear);
    }
}
