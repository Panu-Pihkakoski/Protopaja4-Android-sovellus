package com.proto4.protopaja.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.proto4.protopaja.R;

/**
 * Created by user on 17.08.17.
 */

public class HelpFragment extends Fragment {

    private static final String TAG = HelpFragment.class.getSimpleName();


    public HelpFragment() {

    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_help, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        TextView helpText = getActivity().findViewById(R.id.help_text_view);
        helpText.setMovementMethod(new ScrollingMovementMethod());
    }
}
