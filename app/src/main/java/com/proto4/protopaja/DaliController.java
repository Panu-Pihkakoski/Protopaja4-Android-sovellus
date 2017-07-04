package com.proto4.protopaja;

import java.util.ArrayList;

/**
 * Created by user on 4.07.17.
 */

public class DaliController {

    private ArrayList<DaliGear> gears;

    public DaliController(){
        gears = new ArrayList<>();
    }

    public DaliController(ArrayList<DaliGear> _gears){
        gears = _gears;
    }


    public void addGear(DaliGear gear){
        if (gears == null)
            gears = new ArrayList<>();
        gears.add(gear);
    }

    public void removeGear(DaliGear gear){
        if (gears == null) return;
        gears.remove(gear);
    }
}
