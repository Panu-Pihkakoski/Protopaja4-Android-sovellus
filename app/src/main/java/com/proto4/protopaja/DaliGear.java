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


    private byte[] data;

    private static final int DATA_ID = 0;
    private static final int DATA_STATUS = 1;
    private static final int DATA_POWER = 2;

    public static final int STATUS_BALLAST_FAILURE = 1;
    public static final int STATUS_LAMP_FAILURE = 2;
    public static final int STATUS_POWER_ON = 4;
    public static final int STATUS_LIMIT_ERROR = 8;
    public static final int STATUS_FADE_RUNNING = 16;
    public static final int STATUS_RESET_STATE = 32;
    public static final int STATUS_MISSING_S_ADDR = 64;
    public static final int STATUS_POWER_FAILURE = 128;



    public DaliGear(byte[] _data) {
        this("unknown", _data);
    }

    public DaliGear(String _name) {
        this(_name, new byte[]{0, 0, 0});
    }

    public DaliGear(String _name, byte[] _data) {
        name = _name;
        data = _data;
    }

    // gets
    public byte[] getData() {
        return data;
    }

    public byte getId() {
        return data[DATA_ID];
    }

    public byte getStatus(){
        return data[DATA_STATUS];
    }

    public byte getPower() {
        return data[DATA_POWER];
    }

    public String getName(){
        return name;
    }

    // sets
    public void setData(byte[] _data) {
        data = _data;
    }

    public void setId(byte _id) {
        data[DATA_ID] = _id;
    }

    public void setStatus(byte _status) {
        data[DATA_STATUS] = _status;
    }

    public void setPower(byte power) {
        data[DATA_POWER] = power;
    }

    public void setName(String _name){
        name = _name;
    }


    // TODO: remove or put to use
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
        byte status = data[DATA_STATUS];
        String info = "";
        info += "Power level = " + Byte.toString(data[DATA_POWER]);
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

    public interface StatusUpdateListener {
        void onGearStatusUpdated(DaliGear gear);
    }
}
