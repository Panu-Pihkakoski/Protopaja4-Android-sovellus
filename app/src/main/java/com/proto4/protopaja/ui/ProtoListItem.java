package com.proto4.protopaja.ui;

import android.bluetooth.BluetoothDevice;

import com.proto4.protopaja.DaliGear;

/**
 * Created by user on 15.08.17.
 */

public class ProtoListItem {

    private static final String TAG = ProtoListItem.class.getSimpleName();


    public static final int TYPE_GEAR = 0;
    public static final int TYPE_GROUP = 1;
    public static final int TYPE_DEVICE = 2;

    private DaliGear gear;
    private BluetoothDevice device;

    private int type;
    private String name;

    private boolean isChecked, checkBoxVisible;

    public ProtoListItem(DaliGear gear) {
        this.gear = gear;
        isChecked = checkBoxVisible = false;
        name = gear.getName();
        type = gear.isGroup() ? TYPE_GROUP : TYPE_GEAR;
    }

    public ProtoListItem(BluetoothDevice device) {
        this.device = device;
        isChecked = checkBoxVisible = false;
        name = device.getName();
        if (name == null) name = device.getAddress();
        type = TYPE_DEVICE;
    }

    public int getType() {
        if (type == TYPE_GEAR || type == TYPE_GROUP)
            type = gear.isGroup() ? TYPE_GROUP : TYPE_GEAR;
        return type;
    }

    public String getName() {
        return name;
    }

    public DaliGear getGear() {
        return gear;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public boolean isCheckBoxVisible() {
        return checkBoxVisible;
    }

    public boolean isGroup() {
        return gear.isGroup();
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public void setCheckBoxVisible(boolean visible) {
        checkBoxVisible = visible;
    }

    public void setGear(DaliGear gear) {
        this.gear = gear;
        name = gear.getName();
        type = gear.isGroup() ? TYPE_GROUP : TYPE_GEAR;
    }

    public void setDevice(BluetoothDevice device) {
        this.device = device;
        name = device.getName();
        if (name == null) name = device.getAddress();
    }
}
