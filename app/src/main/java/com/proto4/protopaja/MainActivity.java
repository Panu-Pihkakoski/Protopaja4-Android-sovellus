package com.proto4.protopaja;

import android.Manifest;
import android.app.Fragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.proto4.protopaja.ble.BleManager;
import com.proto4.protopaja.ble.BleScanner;
import com.proto4.protopaja.ui.GearFragment;
import com.proto4.protopaja.ui.ItemClickSupport;
import com.proto4.protopaja.ui.RecyclerListAdapter;
import com.proto4.protopaja.ui.RecyclerListItem;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements BleScanner.ScanListener,
                BleManager.BleManagerListener, GearFragment.GearFragmentListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Toolbar toolbar;
    private ProgressBar progressBar;

    private TextView titleView;

    private RecyclerView recyclerView;

    private FrameLayout fragmentLayout;

    private Fragment activeFragment;
    private GearFragment gearFragment;

    private ArrayList<RecyclerListItem> recyclerListItems;

    private BleManager bleManager;
    private BleScanner bleScanner;

    private BluetoothDevice connectedDevice;
    private String deviceAddress;

    private ArrayList<BluetoothDevice> foundDevices;
    private ArrayList<DaliGear> daliGears;

    private BluetoothGattService uartService;

    private static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String AUTO_CONNECT = "AUTO_CONNECT";

    private static final int ITEM_ACTION_CONNECT = 0;
    private static final int ITEM_ACTION_OPEN = 1;


    private static final byte SET_POWER = 0;

    private static final byte MESSAGE_INIT = 0;
    private static final byte MESSAGE_UPDATE = 1;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    // Service Constants from adafruit's example
    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_DFU = "00001530-1212-EFDE-1523-785FEABCD123";


    public static final int TX_MAX_CHARS = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // toolbar, shows app name / "scanning..." / "connecting...", has overflow menu
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        // progress bar on toolbar, visible while scanning / connecting
        progressBar = (ProgressBar) findViewById(R.id.main_toolbar_progress_bar);
        progressBar.setVisibility(View.GONE);

        foundDevices = new ArrayList<>(); // scanned bluetooth devices
        daliGears = new ArrayList<>(); // dali gears obtained from connected device

        // clickable title below toolbar, shows connected device address / dali gear name
        titleView = (TextView) findViewById(R.id.main_title_view);
        titleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activeFragment != null)
                    closeActiveFragment();
            }
        });
        titleView.setText("No devices");
        titleView.setTextSize(32); // maybe should check screen dimensions before setting text size

        // item list to display on recyclerView
        recyclerListItems = new ArrayList<>();

        // view for holding list items (scanned devices, dali gears)
        recyclerView = (RecyclerView) findViewById(R.id.main_recycler_view);
        recyclerView.setAdapter(new RecyclerListAdapter(this, recyclerListItems));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // set on item click listener for recycler list items
        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Log.d(TAG, "on recycler view list item clicked");

                RecyclerListItem item = recyclerListItems.get(position);
                if (item.getAction() == ITEM_ACTION_CONNECT) { // connect to address in item title
                    stopScan();
                    connect(item.getTitle());
                    titleView.setText(item.getTitle());
                } else if (item.getAction() == ITEM_ACTION_OPEN) { // open gear fragment for gear at 'position'
                    showGearFragment(position);
                }
            }
        });

        // layout for holding fragments
        fragmentLayout = (FrameLayout) findViewById(R.id.main_fragment_content);
        fragmentLayout.setVisibility(View.GONE);

        // bleManager provides bluetooth methods
        bleManager = new BleManager(this, this);

        // check whether bluetooth is enabled or not, request enable if not
        if (!bluetoothEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }
        requestLocationPermissionIfNeeded(); // location permission is needed to get scan results

        // bleScanner provides bluetooth scan methods
        bleScanner = new BleScanner(bleManager.getAdapter(this), this);

        loadValues(); // load saved values
        if (deviceAddress != null) { // check whether saved device address exists. if yes, connect to it
            Log.d(TAG, "Found saved device address ("+ deviceAddress + "). Connecting...");
            connect(deviceAddress);
        }
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

    // request location permissions if needed
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

        // save address of connected device
        saveValue(DEVICE_ADDRESS);
        // save auto connect
        saveValue(AUTO_CONNECT);

        super.onDestroy();
    }

    // saves current value with matching key
    private void saveValue(String key) {
        SharedPreferences.Editor edit = getPreferences(MODE_PRIVATE).edit();
        if (key.equals(DEVICE_ADDRESS) && deviceAddress != null)
            edit.putString(key, deviceAddress);
        else if (key.equals(AUTO_CONNECT) && bleManager != null)
            edit.putBoolean(key, bleManager.getAutoConnect());
        edit.commit();
    }

    // gets saved values and assigns them to appropriate variables
    private void loadValues() {
        SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        deviceAddress = prefs.getString(DEVICE_ADDRESS, null);
        if (bleManager != null)
            bleManager.setAutoConnect(prefs.getBoolean(AUTO_CONNECT, false));
    }

    // show gear fragment containing control and info ui for gear at specified position,
    // hides recycler list
    private void showGearFragment(final int gearPosition) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setVisibility(View.GONE); // hide list of gears
                fragmentLayout.setVisibility(View.VISIBLE); // show fragment container
                titleView.setText(daliGears.get(gearPosition).getName()); // set title
            }
        });

        if (gearFragment == null) // if gearFragment isn't initialized...
            gearFragment = GearFragment.newInstance(gearPosition, this);
        gearFragment.setGearData(daliGears.get(gearPosition)); // to make fragment ui match gear data
        getFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_content, gearFragment)
                .commit(); // sets gear fragment on fragment container
        activeFragment = gearFragment;
    }

    // closes fragment if one is being showed, shows recycler list
    private void closeActiveFragment() {
        if (activeFragment != null) {
            getFragmentManager().beginTransaction().remove(activeFragment).commit();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    fragmentLayout.setVisibility(View.GONE);
                    recyclerView.setVisibility(View.VISIBLE);
                    titleView.setText(connectedDevice != null ? connectedDevice.getAddress()
                                : "No connected device");
                }
            });
            activeFragment = null;
        }
    }

    // callback to handle gear fragment ui actions
    @Override
    public void onGearFragmentAction(int which, int value, int gearPosition) {
        switch (which) {
            case GearFragment.ACTION_POWER: // set gear power on (value != 0) / off (value == 0)
                setGearPower(gearPosition, value != 0);
                break;
            case GearFragment.ACTION_POWER_LEVEL: // set gear power level
                setGearPowerLevel(gearPosition, value);
                break;
            case GearFragment.ACTION_STEP: // not implemented
                // step
                break;
            case GearFragment.ACTION_CLOSE: // close the fragment
                closeActiveFragment();
                break;
            case GearFragment.ACTION_RENAME: // rename gear
                final String newName = gearFragment.getNewName(); // get new name from fragment
                daliGears.get(gearPosition).setName(newName); // set name
                recyclerListItems.get(gearPosition).setTitle(newName); // update list item title
                recyclerView.getAdapter().notifyItemChanged(gearPosition); // notify list adapter
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        titleView.setText(newName); // update title
                    }
                });
                break;
            default:
                Log.d(TAG, "Invalid GearFragment action");
        }
    }

    // set power on/off for gear at gearPosition
    private void setGearPower(int gearPosition, boolean on) {
        byte[] data = {daliGears.get(gearPosition).getId(), SET_POWER, (byte)(on?100:0)};
        sendData(data);
    }
    // set power level for gear at gearPosition
    private void setGearPowerLevel(int gearPosition, int powerLevel) {
        // TODO: remove this test and uncomment lines below
        daliGears.get(gearPosition).setPower((byte)powerLevel);
        gearFragment.setGearData(daliGears.get(gearPosition));

        //byte[] data = {daliGears.get(gearPosition).getId(), SET_POWER, (byte)powerLevel};
        //sendData(data);
    }

    // initialize gear list of <count> items with specified data
    private void initGearList(byte[][] data, int count) {
        daliGears.clear(); // clear gear list
        for (int i = 0; i < count; i++) {
            DaliGear gear = new DaliGear(data[i]);
            daliGears.add(gear);
        }

        // not sure if these should be run on ui thread
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // clear recycler list
                recyclerListItems.clear();
                recyclerView.getAdapter().notifyDataSetChanged();
                // add gears to recycler list
                for (DaliGear gear : daliGears) {
                    recyclerListItems.add(new RecyclerListItem(gear.getName(), ITEM_ACTION_OPEN));
                    recyclerView.getAdapter().notifyItemInserted(recyclerListItems.size() - 1);
                }
            }
        });
    }

    // updates gear, gear update message (including gear id) is given as a parameter
    // if matching id isn't found in the gear list, new gear object is created
    private boolean updateGear(byte[] bytes) {
        byte id = bytes[1]; // bytes[0] == MESSAGE_UPDATE

        DaliGear toUpdate = null;
        for (DaliGear gear : daliGears) {
            if (id == gear.getId()) {
                toUpdate = gear;
            }
        }
        if (toUpdate == null) { // if matching id wasn't found
            daliGears.add(new DaliGear(Arrays.copyOfRange(bytes, 1, DaliGear.DATA_LEN + 1)));
            recyclerListItems.add(new RecyclerListItem("New Gear", ITEM_ACTION_OPEN));
            recyclerView.getAdapter().notifyItemInserted(recyclerListItems.size() - 1);
            return true;
        }

        toUpdate.setData(Arrays.copyOfRange(bytes, 1, bytes.length));

        Log.d(TAG, "Gear data updated:\n" + toUpdate.getInfoString());

        return true;
    }

    // bluetooth

    private boolean bluetoothEnabled(){
        return bleManager.getBluetoothStatus(this) == BleManager.STATUS_BT_ENABLED;
    }

    // bluetooth callback methods
    @Override
    public void onDeviceFound(BluetoothDevice device){
        Log.d(TAG, "Device found (" + device.getAddress() + ")");

        foundDevices.add(device);
        recyclerListItems.add(new RecyclerListItem(device.getAddress(), ITEM_ACTION_CONNECT));


        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //recyclerView.getAdapter().notifyItemInserted(recyclerListItems.size()-1);
                recyclerView.getAdapter().notifyDataSetChanged();
                if (foundDevices.size() == 1)
                    titleView.setText("Scanned Devices");
            }
        });
    }

    @Override
    public void onScanStopped(){
        Log.d(TAG, "Scan stopped");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                toolbar.setTitle(R.string.app_name);
            }
        });
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
        deviceAddress = connectedDevice.getAddress();

        // TODO: remove this test and get actual gear data from controller
        daliGears.clear();
        for (int i = 0; i < 20; i++){
            DaliGear gear = new DaliGear("Dummy Gear #" + i, new byte[]{(byte)i, 0, 0});
            daliGears.add(gear);
        }
        recyclerListItems.clear();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // TODO: remove
                recyclerView.getAdapter().notifyDataSetChanged();
                for (DaliGear gear : daliGears) {
                    recyclerListItems.add(new RecyclerListItem(gear.getName(), ITEM_ACTION_OPEN));
                    recyclerView.getAdapter().notifyItemInserted(recyclerListItems.size() - 1);
                }


                progressBar.setVisibility(View.GONE);
                toolbar.setTitle(R.string.app_name);
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
        uartService = bleManager.getGattService(UUID_SERVICE);
        if (uartService != null) {
            Log.d(TAG, "uartService set");
            enableRxNotifications();
        } else
            Log.d(TAG, "couldn't get uartService");
    }

    @Override
    public void onDataAvailable(BluetoothGattCharacteristic characteristic){
        Log.d(TAG, "Data available");
        if (characteristic.getService().getUuid().toString().equalsIgnoreCase(UUID_SERVICE)) {
            if (characteristic.getUuid().toString().equalsIgnoreCase(UUID_RX)) {
                final byte[] bytes = characteristic.getValue();
                final String data = new String(bytes, Charset.forName("UTF-8"));

                Log.d(TAG, "data received:\n" + data);

                // testing simple protocol
                if (bytes[0] == MESSAGE_INIT) {     // get gears
                    int dataByteCount = 3; // may be changed
                    int gearCount = (int)bytes[1];
                    byte[][] gearData = new byte[gearCount][3];
                    for (int i = 0; i < gearCount; i++) {
                        for (int j = 0; j < dataByteCount; j++) {
                            gearData[i][j] = bytes[2 + j + dataByteCount * i];
                        }
                    }
                    initGearList(gearData, gearCount);
                } else if (bytes[0] == MESSAGE_UPDATE) {
                    updateGear(bytes);
                }

            }
        }
    }

    @Override
    public void onDataAvailable(BluetoothGattDescriptor descriptor){
        Log.d(TAG, "Descriptor data available");
    }

    // bluetooth scan and connect

    public void startScan(){

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
        recyclerListItems.clear();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeActiveFragment();
                recyclerView.getAdapter().notifyDataSetChanged();
                titleView.setText("No Devices");
                progressBar.setVisibility(View.VISIBLE);
                toolbar.setTitle("Scanning...");
            }
        });

        bleScanner.start(BleScanner.DEFAULT_SCAN_PERIOD);
    }

    public void stopScan(){
        bleScanner.stop();
    }

    private void connect(String address){
        Log.d(TAG, "Connecting to " + address);
        // TODO: remove this test and uncomment line below
        onConnected();
        //bleManager.connect(this, address);
    }

    private void connect(BluetoothDevice device){
        Log.d(TAG, "Connecting to " + device.getAddress());

        // TODO: remove this test and uncomment line below
        onConnected();
        //bleManager.connect(this, device.getAddress());
    }


    // bluetooth send data to UART
    private void sendData(String text) {
        final byte[] value = text.getBytes(Charset.forName("UTF-8"));
        sendData(value);
    }

    private void sendData(byte[] data) {
        if (uartService != null) {
            // Split the value into chunks (UART service has a maximum number of characters that can be written )
            for (int i = 0; i < data.length; i += TX_MAX_CHARS) {
                final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + TX_MAX_CHARS, data.length));
                bleManager.writeService(uartService, UUID_TX, chunk);
            }
        } else {
            Log.w(TAG, "Uart Service not discovered. Unable to send data");
        }
    }

    private void decodeMessage(byte[] bytes) {
        switch (bytes[0]) {
            case MESSAGE_INIT:
                Log.d(TAG, "message: init");

                break;
            case MESSAGE_UPDATE:
                updateGear(bytes);
                break;
            default:
                Log.d(TAG, "message: not recognized");
                break;
        }
    }


    private void enableRxNotifications() {
        bleManager.enableNotification(uartService, UUID_RX, true);
    }

}
