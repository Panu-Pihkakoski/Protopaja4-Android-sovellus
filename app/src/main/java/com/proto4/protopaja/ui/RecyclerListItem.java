package com.proto4.protopaja.ui;


public class RecyclerListItem {

    private String title, extra;
    private byte id;
    private boolean isChecked, showCheckBox;

    private int type;

    public static final int TYPE_BT_DEVICE = 0;
    public static final int TYPE_GEAR = 1;
    public static final int TYPE_GROUP = 2;

    public RecyclerListItem(String _title, int _type) {
        this(_title, "", _type, (byte)0);
    }

    public RecyclerListItem(String _title, int _type, byte _id) {
        this(_title, "", _type, _id);
    }

    public RecyclerListItem(String _title, String _extra, int _type) {
        this(_title, _extra, _type, (byte)0);
    }

    public RecyclerListItem(String _title, String _extra, int _type, byte _id) {
        title = _title;
        extra = _extra;
        type = _type;
        id = _id;

        isChecked = false;
        showCheckBox = false;
    }

    public String getTitle() {
        return title;
    }

    public String getExtra() {
        return extra;
    }

    public int getType() {
        return type;
    }

    public byte getId() {
        return id;
    }

    public boolean isChecked() {
        return isChecked;
    }

    public boolean showCheckBox() {
        return showCheckBox;
    }

    public void setTitle(String _title) {
        title = _title;
    }

    public void setExtra(String _extra) {
        extra = _extra;
    }

    public void setType(int _type) {
        type = _type;
    }

    public void setId(byte _id) {
        id = _id;
    }

    public void showCheckBox(boolean show) {
        showCheckBox = show;
    }

    public void setChecked(boolean checked) {
        isChecked = checked;
    }
}
