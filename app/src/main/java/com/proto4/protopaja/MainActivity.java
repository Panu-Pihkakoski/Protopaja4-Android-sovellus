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

    private ArrayList<RecyclerListItem> recyclerListItems;

    private BleManager bleManager;
    private BleScanner bleScanner;

    private BluetoothDevice connectedDevice;

    private ArrayList<BluetoothDevice> foundDevices;
    private ArrayList<DaliGear> daliGears;

    private DaliGear.StatusUpdateListener gearUpdateListener;

    protected BluetoothGattService uartService;


    // TODO: give proper values to constants

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

        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        progressBar = (ProgressBar) findViewById(R.id.main_toolbar_progress_bar);
        progressBar.setVisibility(View.GONE);

        foundDevices = new ArrayList<>();
        daliGears = new ArrayList<>();

        titleView = (TextView) findViewById(R.id.main_title_view);
        titleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activeFragment != null)
                    closeActiveFragment();
            }
        });
        titleView.setText("No devices");
        titleView.setTextSize(32);


        recyclerListItems = new ArrayList<>();

        recyclerView = (RecyclerView) findViewById(R.id.main_recycler_view);
        recyclerView.setAdapter(new RecyclerListAdapter(this, recyclerListItems));
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(RecyclerView recyclerView, int position, View v) {
                Log.d(TAG, "on recycler view list item clicked");

                RecyclerListItem item = recyclerListItems.get(position);
                if (item.getAction() == ITEM_ACTION_CONNECT) {
                    stopScan();
                    connect(item.getTitle());
                    titleView.setText(item.getTitle());
                } else if (item.getAction() == ITEM_ACTION_OPEN) {
                    showGearFragment(position);
                }
            }
        });

        fragmentLayout = (FrameLayout) findViewById(R.id.main_fragment_content);
        fragmentLayout.setVisibility(View.GONE);

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

    private void showGearFragment(final int gearPosition) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setVisibility(View.GONE);
                fragmentLayout.setVisibility(View.VISIBLE);
                titleView.setText(daliGears.get(gearPosition).getName());
            }
        });

        GearFragment fragment = GearFragment.newInstance(gearPosition, this);
        fragment.setInfoText(daliGears.get(gearPosition).getInfoString());
        getFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_content, fragment)
                .commit();
        activeFragment = fragment;
    }

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

    @Override
    public void onGearFragmentAction(int which, int value, int gearPosition) {
        switch (which) {
            case GearFragment.ACTION_POWER:
                setGearPower(gearPosition, value != 0);
                break;
            case GearFragment.ACTION_POWER_LEVEL:
                setGearPowerLevel(gearPosition, value);
                break;
            case GearFragment.ACTION_STEP:
                // step
                break;
            case GearFragment.ACTION_CLOSE:
                closeActiveFragment();
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
        byte[] data = {daliGears.get(gearPosition).getId(), SET_POWER, (byte)powerLevel};
        sendData(data);
    }

    private void initGearList(byte[][] data, int count) {

        for (int i = 0; i < count; i++) {
            DaliGear gear = new DaliGear(data[i]);
            daliGears.add(gear);
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerListItems.clear();
                recyclerView.getAdapter().notifyDataSetChanged();
                for (DaliGear gear : daliGears) {
                    recyclerListItems.add(new RecyclerListItem(gear.getName(), ITEM_ACTION_OPEN));
                    recyclerView.getAdapter().notifyItemInserted(recyclerListItems.size() - 1);
                }
            }
        });
    }

    private boolean updateGear(byte[] bytes) {
        byte id = bytes[1];

        DaliGear toUpdate = null;
        for (DaliGear gear : daliGears) {
            if (id == gear.getId()) {
                toUpdate = gear;
            }
        }
        if (toUpdate == null)
            return false;

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
    protected void sendData(String text) {
        final byte[] value = text.getBytes(Charset.forName("UTF-8"));
        sendData(value);
    }

    protected void sendData(byte[] data) {
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

    protected void enableRxNotifications() {
        bleManager.enableNotification(uartService, UUID_RX, true);
    }

}
