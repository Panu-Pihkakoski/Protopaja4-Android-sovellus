package com.proto4.protopaja.ui;


public class RecyclerListItem {

    private String title, extra;
    private int action;
    private byte id;


    public RecyclerListItem(String _title, int _action) {
        this(_title, "", _action, (byte)0);
    }

    public RecyclerListItem(String _title, int _action, byte _id) {
        this(_title, "", _action, _id);
    }

    public RecyclerListItem(String _title, String _extra, int _action) {
        this(_title, _extra, _action, (byte)0);
    }

    public RecyclerListItem(String _title, String _extra, int _action, byte _id) {
        title = _title;
        extra = _extra;
        action = _action;
        id = _id;
    }

    public String getTitle() {
        return title;
    }

    public String getExtra() {
        return extra;
    }

    public int getAction() {
        return action;
    }

    public byte getId() {
        return id;
    }

    public void setTitle(String _title) {
        title = _title;
    }

    public void setExtra(String _extra) {
        extra = _extra;
    }

    public void setAction(int _action) {
        action = _action;
    }

    public void setId(byte _id) {
        id = _id;
    }
}
