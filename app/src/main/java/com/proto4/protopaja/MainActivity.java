package com.proto4.protopaja;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.proto4.protopaja.ble.BleManager;
import com.proto4.protopaja.ble.BleScanner;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.Scanner;

public class MainActivity extends AppCompatActivity implements BleScanner.ScanListener, BleManager.BleManagerListener{

    private static final String TAG = MainActivity.class.getSimpleName();

    private Toolbar toolbar;
    private ProgressBar progressBar;
    private ListView listView;
    private TextView noDevicesTextView;
    private boolean listScan;

    private BleManager bleManager;
    private BleScanner bleScanner;

    private BluetoothDevice connectedDevice;

    private ArrayList<BluetoothDevice> foundDevices;
    private ArrayList<DaliUnit> daliUnits;

    private static final int ITEM_TYPE_BT_DEVICE = 0;
    private static final int ITEM_TYPE_DALI_UNIT = 1;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar) findViewById(R.id.main_toolbar_progress_bar);
        progressBar.setVisibility(View.GONE);

        foundDevices = new ArrayList<>();
        daliUnits = new ArrayList<>();

        noDevicesTextView = (TextView) findViewById(R.id.no_devices_text);

        listView = (ListView) findViewById(R.id.main_listview);
        setListItems(ITEM_TYPE_BT_DEVICE);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position,
                                    long id){
                if (listScan) {
                    stopScan();
                    connect(foundDevices.get(position));
                } else {
                    Intent intent = new Intent(MainActivity.this, DaliDeviceActivity.class);
                    //intent.putExtra(DaliDeviceActivity.EXTRA_UNIT, daliUnits.get(position));
                    startActivity(intent);
                }

            }
        });
        listView.setVisibility(View.GONE);


        bleManager = new BleManager(this, this);

        if (!bluetoothEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        requestLocationPermissionIfNeeded();
        bleScanner = new BleScanner(bleManager.getAdapter(this), this);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                startScan();
                return true;
            // ...
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private boolean bluetoothEnabled(){
        return bleManager.getBluetoothStatus(this) == BleManager.STATUS_BT_ENABLED;
    }

    private void requestLocationPermissionIfNeeded(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("This app needs location access");
                builder.setMessage("Please grant location access so this app can scan for Bluetooth peripherals");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                    public void onDismiss(DialogInterface dialog) {
                        requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION},
                                PERMISSION_REQUEST_COARSE_LOCATION);
                    }
                });
                builder.show();
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_COARSE_LOCATION: {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "coarse location permission granted");
                } else {
                    final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setTitle("Functionality limited");
                    builder.setMessage("Since location access has not been granted, this app will not be able to discover beacons when in the background.");
                    builder.setPositiveButton(android.R.string.ok, null);
                    builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                        }
                    });
                    builder.show();
                }
                return;
            }
        }
    }

    @Override
    protected void onDestroy(){
        // release used bluetooth resources appropriately
        bleManager.disconnect();
        bleManager.close();

        super.onDestroy();
    }

    private void setListItems(int type){
        Log.d(TAG, "setting list items of type " + type);
        if (type == ITEM_TYPE_BT_DEVICE){  // listing scanned bluetooth devices
            ArrayAdapter<BluetoothDevice> adapter = new ArrayAdapter<BluetoothDevice>(this,
                    android.R.layout.simple_list_item_1, foundDevices){
                @Override
                public View getView(int position, View covertView, ViewGroup parent){
                    BluetoothDevice device = foundDevices.get(position);

                    TextView view = new TextView(getApplicationContext());
                    if (device.getName() != null)
                        view.setText(device.getName() + "\n");
                    else
                        view.setText("unknown device\n");
                    view.append(device.getAddress());
                    view.setTextSize(32);
                    view.setBackgroundResource(R.color.unitItem);
                    return view;
                }
            };
            listView.setAdapter(adapter);
            listScan = true;
            if (foundDevices.isEmpty()){
                listView.setVisibility(View.GONE);
                noDevicesTextView.setVisibility(View.VISIBLE);
            } else {
                noDevicesTextView.setVisibility(View.GONE);
                listView.setVisibility(View.VISIBLE);
            }
        } else if (type == ITEM_TYPE_DALI_UNIT) {  // listing controllable dali units/groups
            ArrayAdapter<DaliUnit> adapter = new ArrayAdapter<DaliUnit>(this,
                    android.R.layout.simple_list_item_1, daliUnits){
                @Override
                public View getView(int position, View covertView, ViewGroup parent){
                    DaliUnit unit = daliUnits.get(position);

                    TextView view = new TextView(getApplicationContext());
                    if (unit.isGroup()) {
                        view.setText("Group " + unit.getName() + ":");
                        for (DaliUnit u : unit.getGroup())
                            view.append("\n" + u.getName());
                        view.setTextSize(16);
                        view.setBackgroundResource(R.color.groupItem);
                    } else {
                        view.setText(unit.getName());
                        view.setTextSize(32);
                        view.setBackgroundResource(R.color.unitItem);
                    }
                    return view;
                }
            };
            listView.setAdapter(adapter);
            listScan = false;
            if (listView.getVisibility() == View.GONE)
                listView.setVisibility(View.VISIBLE);
        }
    }

    // bluetooth
    @Override
    public void onDeviceFound(BluetoothDevice device){
        Log.d(TAG, "Device found (" + device.getAddress() + ")");
        foundDevices.add(device);
        ((ArrayAdapter<?>)listView.getAdapter()).notifyDataSetChanged();
        if (foundDevices.size() == 1) {
            noDevicesTextView.setVisibility(View.GONE);
            listView.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onScanStopped(){
        Log.d(TAG, "Scan stopped");
        progressBar.setVisibility(View.GONE);
        toolbar.setTitle(R.string.app_name);
    }

    @Override
    public void onConnecting(){
        Log.d(TAG, "Connecting to device...");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                toolbar.setTitle("Connecting...");
            }
        });
    }

    @Override
    public void onConnected(){
        Log.d(TAG, "Device connected");
        connectedDevice = bleManager.getConnectedDevice();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                toolbar.setTitle(R.string.app_name);

                setListItems(ITEM_TYPE_DALI_UNIT);
            }
        });

    }

    @Override
    public void onDisconnected(){
        Log.d(TAG, "Device disconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                toolbar.setTitle(R.string.app_name);
            }
        });
    }

    @Override
    public void onServicesDiscovered(){
        Log.d(TAG, "Services discovered");
    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic){
        Log.d(TAG, "Data available");
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor){
        Log.d(TAG, "Data available");
    }

    public void startScan(){
        if (!listScan)
            setListItems(ITEM_TYPE_BT_DEVICE);
        if (!bluetoothEnabled()){
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            return;
        }
        bleManager.disconnect();
        if (bleScanner == null)
            bleScanner = new BleScanner(bleManager.getAdapter(this), this);
        if (!bleScanner.isReady())
            bleScanner = new BleScanner(bleManager.getAdapter(this), this);
        foundDevices.clear();
        ((ArrayAdapter<?>)listView.getAdapter()).notifyDataSetChanged();
        listView.setVisibility(View.GONE);
        noDevicesTextView.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        toolbar.setTitle("Scanning...");
        bleScanner.start(BleScanner.DEFAULT_SCAN_PERIOD);
    }

    public void stopScan(){
        bleScanner.stop();
    }

    private void connect(String address){
        Log.d(TAG, "Connecting to " + address);
        bleManager.connect(this, address);
    }

    private void connect(BluetoothDevice device){
        Log.d(TAG, "Connecting to " + device.getAddress());
        bleManager.connect(this, device.getAddress());
    }
}
