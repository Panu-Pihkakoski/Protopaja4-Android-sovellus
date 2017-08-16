package com.proto4.protopaja.ui;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.app.Fragment;
import android.text.InputType;
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

    private RelativeLayout controlView;
    private RelativeLayout infoView;
    private TextView infoViewText;
    private String infoText, newName;
    private boolean showInfo;

    private RoundSlider powerSlider, colorTempSlider;

    private GearFragmentListener listener;

    private DaliGear gear;

    private int powerLevel, lastPowerLevel, minPower, maxPower;

    public static final int ACTION_POWER = 0;
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
        fragment.newName = gear.getName();
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
        powerSlider.setSliderBackground(activity.getResources().getColor(R.color.main_bg, null));


        colorTempSlider = activity.findViewById(R.id.color_temp_slider);
        colorTempSlider.setListener(new RoundSlider.Listener() {
            @Override
            public void onValueSet(int value) {
                onColorTempSet(value);
            }
        });
        colorTempSlider.setMinValue(gear.getDataByteInt(DaliGear.DATA_COLOR_WARMEST));
        colorTempSlider.setMaxValue(gear.getDataByteInt(DaliGear.DATA_COLOR_COOLEST));
        colorTempSlider.setValue(gear.getDataByteInt(DaliGear.DATA_COLOR_TEMP));
        colorTempSlider.setShowKelvins(true);
        colorTempSlider.setFlipped(true);
        colorTempSlider.setSliderBackground(activity.getResources().getColor(R.color.main_bg, null));
        if (gear.getDataByteInt(DaliGear.DATA_COLOR_COOLEST) == 0)
            colorTempSlider.setVisibility(View.GONE);

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
            listener.onGearFragmentAction(ACTION_POWER, powerLevel, gear.getId(), gear.isGroup());
    }

    public void onColorTempSet(int colorTemp) {
        Log.d(TAG, "color temp set: " + colorTemp);
        if (listener != null)
            listener.onGearFragmentAction(ACTION_COLOR_TEMP, colorTemp, gear.getId(), gear.isGroup());
    }

    public void setGear(DaliGear _gear) {
        gear = _gear;
        powerLevel = gear.getPowerInt();
        powerSlider.setValue(powerLevel);
        colorTempSlider.setValue(gear.getDataByteInt(DaliGear.DATA_COLOR_TEMP));
        colorTempSlider.setVisibility(gear.getDataByteInt(DaliGear.DATA_COLOR_COOLEST) > 0 ? View.VISIBLE : View.GONE);
        infoText = gear.getInfoString();
    }

    public void toggleView() {
        if (infoText != null)
            infoViewText.setText(infoText);
        showInfo = !showInfo;
        if (showInfo)
            infoViewText.setText(gear.getInfoString());
        infoView.setVisibility(showInfo ? View.VISIBLE : View.GONE);
        controlView.setVisibility(showInfo ? View.GONE : View.VISIBLE);
    }

    public void renameGear() {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Enter new name");

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                setNewName(input.getText().toString());
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();
    }

    private void setNewName(String newName) {
        this.newName = newName;
        if (listener != null)
            listener.onGearFragmentAction(ACTION_RENAME, 0, gear.getId(), gear.isGroup());
    }

    public String getNewName() {
        return newName;
    }

    private void close() {
        if (listener != null)
            listener.onGearFragmentAction(ACTION_CLOSE, 0, (byte)0, false);
        else
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }

    public void update() {

        powerSlider.setMinValue(gear.getMinPowerInt());
        powerSlider.setMaxValue(gear.getMaxPowerInt());
        powerSlider.setValue(gear.getPowerInt());
        colorTempSlider.setMinValue(gear.getDataByteInt(DaliGear.DATA_COLOR_WARMEST));
        colorTempSlider.setMaxValue(gear.getDataByteInt(DaliGear.DATA_COLOR_COOLEST));
        colorTempSlider.setValue(gear.getDataByteInt(DaliGear.DATA_COLOR_TEMP));
    }

    public interface GearFragmentListener {
        void onGearFragmentAction(int which, int value, byte gearId, boolean group);
    }
}
