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
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.proto4.protopaja.DaliGear;
import com.proto4.protopaja.R;


public class GearFragment extends Fragment {

    private static final String TAG = GearFragment.class.getSimpleName();

    private static final int DEFAULT_POWER_MIN = 0;
    private static final int DEFAULT_POWER_MAX = 100;
    private static final int DEFAULT_COLOR_WARMEST = 0;
    private static final int DEFAULT_COLOR_COOLEST = 0;


    private RelativeLayout controlView;
    private RelativeLayout infoView;
    private TextView infoViewText;
    private String infoText, newName;
    private boolean showInfo;

    private ImageButton powerButton, infoButton;
    private RoundSlider powerSlider, colorTempSlider;

    private Listener listener;

    private int itemId;
    private boolean itemIsGroup;


    private int minPower = DEFAULT_POWER_MIN, maxPower = DEFAULT_POWER_MAX;
    private int powerLevel = minPower, lastPowerLevel = minPower;
    private int colorWarmest = DEFAULT_COLOR_WARMEST, colorCoolest = DEFAULT_COLOR_COOLEST;
    private int colorTemp = colorWarmest;



    public static final int ACTION_POWER = 0;
    public static final int ACTION_STEP = 2;
    public static final int ACTION_QUERY = 3;
    public static final int ACTION_CLOSE = 4;
    public static final int ACTION_RENAME = 5;
    public static final int ACTION_COLOR_TEMP = 6;

    public GearFragment() {
        // Required empty public constructor
    }

    public static GearFragment newInstance(Listener listener) {
        GearFragment fragment = new GearFragment();
        fragment.itemId = 255;
        fragment.itemIsGroup = false;
        fragment.minPower = fragment.maxPower = fragment.lastPowerLevel = 0;
        fragment.infoText = "No info available";
        fragment.newName = "?";
        fragment.listener = listener;
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
        powerSlider.setMinValue(minPower);
        powerSlider.setMaxValue(maxPower);
        powerSlider.setValue(powerLevel);
        powerSlider.setShowPercentage(true);
        powerSlider.setSliderBackground(activity.getResources().getColor(R.color.main_bg, null));

        powerSlider.setVisibility(maxPower > minPower ? View.VISIBLE : View.GONE);

        colorTempSlider = activity.findViewById(R.id.color_temp_slider);
        colorTempSlider.setListener(new RoundSlider.Listener() {
            @Override
            public void onValueSet(int value) {
                onColorTempSet(value);
            }
        });
        colorTempSlider.setMinValue(colorWarmest);
        colorTempSlider.setMaxValue(colorCoolest);
        colorTempSlider.setValue(colorWarmest);
        colorTempSlider.setShowKelvins(true);
        colorTempSlider.setFlipped(true);
        colorTempSlider.setSliderBackground(activity.getResources().getColor(R.color.main_bg, null));

        colorTempSlider.setVisibility(colorCoolest > colorWarmest ? View.VISIBLE : View.GONE);

        powerButton = activity.findViewById(R.id.gear_power_button);
        //powerButton.setColorFilter(gear.getPowerInt() > 0 ? 0 : 0x80000000); // dimColorButton(!(power > 0))
        powerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onPowerSwitched();
            }
        });

        infoButton = activity.findViewById(R.id.gear_info_button);
        //infoButton.setColorFilter(0x40000000);
        infoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                toggleView();
            }
        });

        infoView = activity.findViewById(R.id.gear_info_view);
        infoViewText = activity.findViewById(R.id.gear_info_view_text);
        infoViewText.setTextSize(20);
        if (infoText != null)
            infoViewText.setText(infoText);

        showInfo = true;
        toggleView();
    }

    public void onPowerSwitched() {
        if (powerLevel > 0) {
            powerSlider.setValue(0);
            powerSlider.update();
            onPowerSet(0);
        } else {
            powerSlider.setValue(lastPowerLevel > 0 ? lastPowerLevel: maxPower);
            powerSlider.update();
            onPowerSet(lastPowerLevel > 0 ? lastPowerLevel: maxPower);
        }
        if (showInfo) infoViewText.setText(infoText);
    }

    public void onPowerSet(int power) {
        Log.d(TAG, "power set: " + power);
        lastPowerLevel = powerLevel;
        powerLevel = power;
        dimPowerButton(!(powerLevel > 0));
        if (listener != null)
            listener.onGearFragmentAction(ACTION_POWER, powerLevel, itemId, itemIsGroup);
    }

    public void onColorTempSet(int colorTemp) {
        Log.d(TAG, "color temp set: " + colorTemp);
        if (listener != null)
            listener.onGearFragmentAction(ACTION_COLOR_TEMP, colorTemp, itemId, itemIsGroup);
    }

    private void dimPowerButton(boolean dim) {
        powerButton.setColorFilter(dim ? 0x80000000 : 0);
    }

    public int getItemId() {
        return itemId;
    }

    public boolean isItemGroup() {
        return itemIsGroup;
    }

    public void setItemIsGroup(boolean isGroup) {
        Log.d(TAG, "setItemIsGroup: isGroup=" + (isGroup ? "true" : "false"));
        this.itemIsGroup = isGroup;
    }

    public void setItemId(int itemId) {
        this.itemId = itemId;
    }

    public void setGearValues(DaliGear gear) {
        minPower = gear.getMinPowerInt();
        maxPower = gear.getMaxPowerInt();
        powerLevel = gear.getPowerInt();
        powerSlider.setValue(powerLevel);
        powerButton.setColorFilter(powerLevel > 0 ? 0 : 0x80000000);
        colorWarmest = gear.getDataByteInt(DaliGear.DATA_COLOR_WARMEST);
        colorCoolest = gear.getDataByteInt(DaliGear.DATA_COLOR_COOLEST);
        colorTemp = gear.getDataByteInt(DaliGear.DATA_COLOR_TEMP);
        infoText = DaliGear.getInfoString(gear);
    }

    public void setSliderLimits(int powerMin, int powerMax, int colorTempWarmest, int colorTempCoolest) {
        Log.d(TAG, "setSliderLimits: powerMin=" + powerMin + ", powerMax=" + powerMax +
                ", warmest=" + colorTempWarmest + ", coolest=" + colorTempCoolest);
        minPower = powerMin;
        maxPower = powerMax;
        colorWarmest = colorTempWarmest;
        colorCoolest = colorTempCoolest;
    }

    public void setSliderValues(int powerLevel, int colorTemp) {
        this.powerLevel = powerLevel;
        this.colorTemp = colorTemp;
    }

    public void setInfoText(String infoText) {
        this.infoText = infoText;
        if (infoViewText != null)
            infoViewText.setText(infoText);
    }

    public void toggleView() {
        showInfo = !showInfo;
        if (showInfo)
            infoViewText.setText(infoText);
        infoButton.setColorFilter(showInfo ? 0 : 0x40000000);
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
            listener.onGearFragmentAction(ACTION_RENAME, 0, itemId, itemIsGroup);
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
        Log.d(TAG, "update()");

        // TODO: update contents

        infoViewText.setText(infoText);
    }

    public interface Listener {
        void onGearFragmentAction(int which, int value, int gearId, boolean group);
    }
}
