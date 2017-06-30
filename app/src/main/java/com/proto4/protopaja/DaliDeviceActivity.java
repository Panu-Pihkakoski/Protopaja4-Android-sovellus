package com.proto4.protopaja;

import android.app.Fragment;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

public class DaliDeviceActivity extends AppCompatActivity {

    private static final String TAG = DaliDeviceActivity.class.getSimpleName();

    private BottomNavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dali_device);

        navigationView = (BottomNavigationView) findViewById(R.id.device_navigation_view);
        navigationView.setOnNavigationItemSelectedListener
                (new BottomNavigationView.OnNavigationItemSelectedListener() {
                    @Override
                    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_navi_control:
                                showControlFragment();
                                break;
                            case R.id.action_navi_data:
                                showDataFragment();
                                break;
                            default:
                                Log.d(TAG, "invalid navigation item");
                                break;
                        }
                        return true;
                    }
                });
    }


    private void showControlFragment(){
        getFragmentManager().beginTransaction()
                .replace(R.id.device_content, new ControlFragment())
                .commit();
    }

    private void showDataFragment(){
        getFragmentManager().beginTransaction()
                .replace(R.id.device_content, new DataFragment())
                .commit();
    }

    public static class ControlFragment extends Fragment{

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
        }

    }

    public static class DataFragment extends Fragment{

        @Override
        public void onCreate(Bundle savedInstanceState){
            super.onCreate(savedInstanceState);
        }

    }
}
