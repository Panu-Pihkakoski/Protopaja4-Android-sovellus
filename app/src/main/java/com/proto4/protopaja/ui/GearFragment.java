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
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import com.proto4.protopaja.DaliGear;
import com.proto4.protopaja.R;


public class GearFragment extends Fragment implements DaliGear.StatusUpdateListener {

    private static final String TAG = GearFragment.class.getSimpleName();

    private Button toggleViewButton;

    private RelativeLayout controlView;
    private RelativeLayout infoView;
    private TextView infoViewText;
    private String infoText;
    private boolean showInfo;


    // TODO: remove or add to layout and implement
    private Button stepUpButton;
    private Button stepDownButton;


    private GearFragmentListener listener;

    private int gearPosition;



    public static final int ACTION_POWER = 0;
    public static final int ACTION_POWER_LEVEL = 1;
    public static final int ACTION_STEP = 2;
    public static final int ACTION_QUERY = 3;
    public static final int ACTION_CLOSE = 4;


    public GearFragment() {
        // Required empty public constructor
    }


    public static GearFragment newInstance(int gearPosition) {
        return newInstance(gearPosition, null);
    }

    public static GearFragment newInstance(int gearPosition, GearFragmentListener listener) {
        GearFragment fragment = new GearFragment();
        fragment.gearPosition = gearPosition;
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

        toggleViewButton = activity.findViewById(R.id.gear_toggle_button);
        toggleViewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "infoButton onClick");
                toggleView();
            }
        });

        Button backButton = activity.findViewById(R.id.gear_back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "backButton onClick");
                close();
            }
        });

        controlView  = activity.findViewById(R.id.gear_control_view);
        initControls(activity);
        infoView = activity.findViewById(R.id.gear_info_view);
        infoViewText = activity.findViewById(R.id.gear_info_view_text);
        if (infoText != null)
            infoViewText.setText(infoText);

        showInfo = true;
        toggleView();
    }

    private void initControls(Activity activity) {
        Switch powerSwitch = activity.findViewById(R.id.gear_power_switch);
        powerSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Log.d(TAG, "powerSwitch onCheckedChanged " + (b ? "ON":"OFF"));
                if (listener != null)
                    listener.onGearFragmentAction(ACTION_POWER, b ? 1 : 0, gearPosition);
            }
        });

        SeekBar powerSeekBar = activity.findViewById(R.id.gear_power_seekBar);
        powerSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                Log.d(TAG, "powerSeekBar onProgressChanged " + i);
                if (listener != null)
                    listener.onGearFragmentAction(ACTION_POWER_LEVEL, i, gearPosition);
            }
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

    }

    public void setListener(GearFragmentListener _listener) {
        listener = _listener;
    }

    public void setInfoText(String text) {
        infoText = text;
    }

    private void toggleView() {
        if (infoText != null)
            infoViewText.setText(infoText);
        showInfo = !showInfo;
        infoView.setVisibility(showInfo ? View.VISIBLE : View.GONE);
        controlView.setVisibility(showInfo ? View.GONE : View.VISIBLE);
        toggleViewButton.setText(showInfo ? "Control" : "Info");
    }

    private void close() {
        if (listener != null)
            listener.onGearFragmentAction(ACTION_CLOSE, 0, 0);
        else
            getActivity().getFragmentManager().beginTransaction().remove(this).commit();
    }

    @Override
    public void onGearStatusUpdated(DaliGear gear) {
        infoViewText.setText(gear.getInfoString());
    }

    public interface GearFragmentListener {
        void onGearFragmentAction(int which, int value, int gearPosition);
    }
}
