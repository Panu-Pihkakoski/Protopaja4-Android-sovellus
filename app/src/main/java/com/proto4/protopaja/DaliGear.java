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

    private byte[] data; // should be {id, status, power, ...}

    public static final int DATA_LEN = 13;

    // data byte index
    public static final int DATA_ID = 0;
    public static final int DATA_STATUS = 1;
    public static final int DATA_POWER = 2;
    public static final int DATA_POWER_MIN = 3;
    public static final int DATA_POWER_MAX = 4;
    public static final int DATA_COLOR_TEMP_CAP = 5;
    public static final int DATA_COLOR_TEMP = 6;
    public static final int DATA_COLOR_COOLEST = 7;
    public static final int DATA_COLOR_WARMEST = 8;
    public static final int DATA_TEMP_0 = 9;
    public static final int DATA_TEMP_0d = 10;
    public static final int DATA_TEMP_1 = 11;
    public static final int DATA_TEMP_1d = 12;


    public static final int STATUS_BALLAST_FAILURE = 1;
    public static final int STATUS_LAMP_FAILURE = 2;
    public static final int STATUS_POWER_ON = 4;
    public static final int STATUS_LIMIT_ERROR = 8;
    public static final int STATUS_FADE_RUNNING = 16;
    public static final int STATUS_RESET_STATE = 32;
    public static final int STATUS_MISSING_S_ADDR = 64;
    public static final int STATUS_POWER_FAILURE = 128;


    private static final byte DEFAULT_POWER_MIN = 0;
    private static final byte DEFAULT_POWER_MAX = (byte)254;


    public DaliGear(byte[] _data) {
        this("unknown", _data);
    }

    public DaliGear(String _name) {
        this(_name, new byte[]{0, 0, 0});
    }

    public DaliGear(String _name, byte _id) {
        this(_name, new byte[]{_id, 0, 0});
    }

    public DaliGear(String _name, byte[] _data) {
        name = _name;
        data = new byte[DATA_LEN];

        for (int i = 0; i < DATA_LEN; i++)
            data[i] = i < _data.length ? _data[i] : 0;
        if (data[DATA_POWER_MIN] == 0)
            data[DATA_POWER_MIN] = DEFAULT_POWER_MIN;
        if (data[DATA_POWER_MAX] == 0)
            data[DATA_POWER_MAX] = DEFAULT_POWER_MAX;
    }

    // gets
    public byte[] getData() {
        return data;
    }

    public byte getDataByte(int index) {
        if (index < 0 || index >= DATA_LEN)
            return -1;
        return data[index];
    }

    public int getDataByteInt(int index) {
        byte b = getDataByte(index);
        return (b < 0) ? 256+b : b;
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

    public int getPowerInt() {
        return (data[DATA_POWER] < 0) ? 256+data[DATA_POWER] : data[DATA_POWER];
    }

    public int getMinPowerInt() {
        return (data[DATA_POWER_MIN] < 0) ? 256+data[DATA_POWER_MIN] : data[DATA_POWER_MIN];
    }

    public int getMaxPowerInt() {
        return (data[DATA_POWER_MAX] < 0) ? 256+data[DATA_POWER_MAX] : data[DATA_POWER_MAX];
    }

    public String getName(){
        return name;
    }

    public float getPowerRatio() {
        float pr = (float)(getPowerInt() - getMinPowerInt())/(getMaxPowerInt() - getMinPowerInt());
        Log.d(TAG, "getPowerRatio() returning " + Float.toString(pr));
        return pr;
    }

    // sets
    public void setData(byte[] _data) {
        for (int i = 0; i < DATA_LEN; i++)
            data[i] = i < _data.length ? _data[i] : 0;
    }

    public void setDataByte(int index, byte value) {
        if (index < 0 || index >= DATA_LEN)
            return;
        data[index] = value;
    }

    public void setId(byte _id) {
        data[DATA_ID] = _id;
    }

    public void setStatus(byte _status) {
        data[DATA_STATUS] = _status;
    }

    public void setPower(byte power) {
        Log.d(TAG, "power=" + (power < 0 ? power + 256 : power));
        data[DATA_POWER] = power;
        if (power > 0 && (data[DATA_STATUS] & STATUS_POWER_ON) == 0)
            data[DATA_STATUS] |= STATUS_POWER_ON;
        else if (power == 0)
            data[DATA_STATUS] = (byte)(data[DATA_STATUS] & (0xff ^ STATUS_POWER_ON));
    }

    public void setMinPower(byte minPower) {
        data[DATA_POWER_MIN] = minPower;
    }

    public void setMaxPower(byte maxPower) {
        data[DATA_POWER_MAX] = maxPower;
    }

    public void setName(String _name){
        name = _name;
    }

    public void setConstants(byte[] bytes) {
        if (bytes.length < 5) {
            Log.w(TAG, "setConstants() invalid constant array");
            return;
        }
        data[DATA_POWER_MIN] = bytes[0];
        data[DATA_POWER_MAX] = bytes[1];
        data[DATA_COLOR_TEMP_CAP] = bytes[2];
        data[DATA_COLOR_COOLEST] = bytes[3];
        data[DATA_COLOR_WARMEST] = bytes[4];
    }

    public void update(byte[] bytes) {
        if (bytes.length < 7) {
            Log.w(TAG, "update(): invalid update array, returning");
            return;
        }
        data[DATA_STATUS] = bytes[0];
        data[DATA_POWER] = bytes[1];
        data[DATA_COLOR_TEMP] = bytes[2];
        data[DATA_TEMP_0] = bytes[3];
        data[DATA_TEMP_0d] = bytes[4];
        data[DATA_TEMP_1] = bytes[5];
        data[DATA_TEMP_1d] = bytes[6];
    }

    public boolean isGroup(){
        return group != null;
    }


    // returns true if gear was added
    public boolean addGroupMember(DaliGear gear) {
        Log.d(TAG, "adding group member: " + gear.getName());
        if (group == null)
            group = new ArrayList<>();
        if (group.contains(gear))
            return false;
        group.add(gear);
        return true;
    }

    // returns true if gear was removed
    public boolean removeGroupMember(DaliGear gear){
        if (group == null)
            return false;
        boolean removed = group.remove(gear);
        if (group.size() == 0)
            group = null;
        return removed;
    }

    public ArrayList<DaliGear> getGroup(){
        return group;
    }


    public String getInfoString() {
        Log.d(TAG, "getInfoString()");
        byte status = data[DATA_STATUS];
        String info = "";
        info += "Power level = " + getPowerInt() + "\n";
        info += "Power min = " + getDataByteInt(DATA_POWER_MIN) + "\n";
        info += "Power max = " + getDataByteInt(DATA_POWER_MAX) + "\n";
        info += "Color temperature = " + getDataByteInt(DATA_COLOR_TEMP) + "\n";
        info += "Color coolest = " + getDataByteInt(DATA_COLOR_COOLEST) + "\n";
        info += "Color warmest = " + getDataByteInt(DATA_COLOR_WARMEST) + "\n";
        info += "Temp0 = " + (getDataByteInt(DATA_TEMP_0) + (float)getDataByteInt(DATA_TEMP_0d)/100) + "\n";
        info += "Temp1 = " + (getDataByteInt(DATA_TEMP_1) + (float)getDataByteInt(DATA_TEMP_1d)/100) + "\n";
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
}
