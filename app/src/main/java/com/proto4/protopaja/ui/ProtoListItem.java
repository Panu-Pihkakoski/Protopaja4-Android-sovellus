package com.proto4.protopaja.ui;

public class ProtoListItem {

    private static final String TAG = ProtoListItem.class.getSimpleName();


    public static final int TYPE_GEAR = 0;
    public static final int TYPE_GROUP = 1;
    public static final int TYPE_DEVICE = 2;

    private int type, id;
    private float value;
    private String name;

    private boolean isChecked, checkBoxVisible;

    public ProtoListItem(String name, int type, int id) {
        isChecked = checkBoxVisible = false;
        this.name = name;
        this.type = type;
        this.id = id;
        this.value = 0;
    }

    public int getId() {
        return id;
    }

    public int getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public boolean isCheckBoxVisible() {
        return checkBoxVisible;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }

    public void setCheckBoxVisible(boolean visible) {
        checkBoxVisible = visible;
    }
}
