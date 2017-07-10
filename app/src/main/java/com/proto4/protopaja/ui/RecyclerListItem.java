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
}
