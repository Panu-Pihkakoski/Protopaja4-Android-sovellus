package com.proto4.protopaja.ui;


public class RecyclerListItem {

    private String title;
    private int action;

    public RecyclerListItem(String _title, int _action) {
        title = _title;
        action = _action;
    }

    public String getTitle() {
        return title;
    }

    public int getAction() {
        return action;
    }

    public void setTitle(String _title) {
        title = _title;
    }

    public void setAction(int _action) {
        action = _action;
    }
}
