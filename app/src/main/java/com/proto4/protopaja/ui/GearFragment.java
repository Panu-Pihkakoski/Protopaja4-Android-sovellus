package com.proto4.protopaja.ui;


import android.app.Activity;
import android.os.Bundle;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.proto4.protopaja.DaliGear;
import com.proto4.protopaja.R;


public class GearFragment extends Fragment {

    private static final String TAG = GearFragment.class.getSimpleName();

    private Button toggleViewButton, backButton, renameButton;
    private EditText editText;

    private RelativeLayout controlView;
    private RelativeLayout infoView;
    private TextView infoViewText;
    private String infoText;
    private boolean showInfo;

    private RoundSlider powerSlider, colorTempSlider;

    private GearFragmentListener listener;

    private DaliGear gear;

    private int powerLevel, lastPowerLevel, minPower, maxPower;

    public static final int ACTION_POWER = 0;
    public static final int ACTION_POWER_LEVEL = 1;
    public static final int ACTION_STEP = 2;
    public static final int ACTION_QUERY = 3;
    public static final int ACTION_CLOSE = 4;
    public static final int ACTION_RENAME = 5;
    public static final int ACTION_COLOR_TEMP = 6;


    public GearFragment() {
        // Required empty public constructor
    }


    public static GearFragment newInstance(DaliGear gear) {
        return newInstance(gear, null);
    }

    public static GearFragment newInstance(DaliGear gear, GearFragmentListener listener) {
        GearFragment fragment = new GearFragment();
        fragment.gear = gear;
        fragment.listener = listener;
        fragment.lastPowerLevel = fragment.powerLevel = gear.getPowerInt();
        fragment.minPower = gear.getMinPowerInt();
        fragment.maxPower = gear.getMaxPowerInt();
        fragment.infoText = gear.getInfoString();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_gear, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Activity activity = getActivity();

        toggleViewButton = activity.findViewById(R.id.gear_toggle_button);
        toggleViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "infoButton onClick");
                toggleView();
            }
        });

        backButton = activity.findViewById(R.id.gear_back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "backButton onClick");
                close();
            }
        });

        renameButton = activity.findViewById(R.id.gear_rename_button);
        renameButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "renameButton onClick");
                if (editText.getVisibility() == View.GONE) {
                    toggleViewButton.setVisibility(View.GONE);
                    backButton.setVisibility(View.GONE);
                    editText.setVisibility(View.VISIBLE);
                    editText.requestFocus();
                } else {
                    Log.d(TAG, "renaming gear...");
                    renameGear();
                    editText.setVisibility(View.GONE);
                    toggleViewButton.setVisibility(View.VISIBLE);
                    backButton.setVisibility(View.VISIBLE);
                }
            }
        });

        editText = activity.findViewById(R.id.gear_edit_text);
        editText.setVisibility(View.GONE);

        controlView  = activity.findViewById(R.id.gear_control_view);
        powerSlider = activity.findViewById(R.id.power_slider);
        powerSlider.setListener(new RoundSlider.Listener(){
            @Override
            public void onValueSet(int value) {
                onPowerSet(value);
            }
        });
        powerSlider.setMinValue(gear.getMinPowerInt());
        powerSlider.setMaxValue(gear.getMaxPowerInt());
        powerSlider.setValue(gear.getPowerInt());
        powerSlider.setShowPercentage(true);
        //powerSlider.setVisibility(View.GONE);

        colorTempSlider = activity.findViewById(R.id.color_temp_slider);
        colorTempSlider.setListener(new RoundSlider.Listener() {
            @Override
            public void onValueSet(int value) {
                onColorTempSet(value);
            }
        });
        colorTempSlider.setMinValue(gear.getDataByteInt(DaliGear.DATA_COLOR_COOLEST));
        //colorTempSlider.setMaxValue(gear.getDataByteInt(DaliGear.DATA_COLOR_WARMEST));
        colorTempSlider.setMaxValue(100);
        colorTempSlider.setValue(gear.getDataByteInt(DaliGear.DATA_COLOR_TEMP));
        colorTempSlider.setShowValue(true);
        colorTempSlider.setFlipped(true);

        infoView = activity.findViewById(R.id.gear_info_view);
        infoViewText = activity.findViewById(R.id.gear_info_view_text);
        if (infoText != null)
            infoViewText.setText(infoText);

        showInfo = true;
        toggleView();
    }

    public void onPowerSet(int power) {
        Log.d(TAG, "power set: " + power);
        powerLevel = power;
        if (listener != null)
            listener.onGearFragmentAction(ACTION_POWER_LEVEL, powerLevel, gear.getId());
    }

    public void onColorTempSet(int colorTemp) {
        Log.d(TAG, "color temp set: " + colorTemp);
        if (listener != null)
            listener.onGearFragmentAction(ACTION_COLOR_TEMP, colorTemp, gear.getId());
    }

    public void setGear(DaliGear _gear) {
        gear = _gear;
        powerLevel = gear.getPowerInt();
        powerSlider.setValue(powerLevel);
        colorTempSlider.setValue(gear.getDataByteInt(DaliGear.DATA_COLOR_TEMP));
        infoText = gear.getInfoString();
    }

    private void toggleView() {
        if (infoText != null)
            infoViewText.setText(infoText);
        showInfo = !showInfo;
        if (showInfo)
            infoViewText.setText(gear.getInfoString());
        infoView.setVisibility(showInfo ? View.VISIBLE : View.GONE);
        controlView.setVisibility(showInfo ? View.GONE : View.VISIBLE);
        toggleViewButton.setText(showInfo ? "Control" : "Info");
    }

    private void renameGear() {
        if (listener != null)
            listener.onGearFragmentAction(ACTION_RENAME, 0, gear.getId());
    }

    public String getNewName() {
        return editText.getText().toString();
    }

    private void close() {
        if (listener != null)
            listener.onGearFragmentAction(ACTION_CLOSE, 0, (byte)0);
        else
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }

    public interface GearFragmentListener {
        void onGearFragmentAction(int which, int value, byte gearId);
    }
}
