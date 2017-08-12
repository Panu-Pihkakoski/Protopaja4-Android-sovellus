package com.proto4.protopaja.ui;

import com.proto4.protopaja.DaliGear;

/**
 * Created by user on 12.08.17.
 */

public class GearListItem {

    private static final String TAG = GearListItem.class.getSimpleName();

    private DaliGear gear;
    private boolean isChecked, checkBoxVisible;

    public GearListItem(DaliGear gear) {
        this.gear = gear;
        isChecked = checkBoxVisible = false;
    }

    public DaliGear getGear() {
        return gear;
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
    }
}
