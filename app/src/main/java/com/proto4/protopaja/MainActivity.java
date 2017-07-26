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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private Map<Byte, DaliGear> gearMap;

    private BluetoothGattService uartService;

    private byte[] currentMessage;

    private static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String AUTO_CONNECT = "AUTO_CONNECT";

    // list item actions on click
    private static final int ITEM_ACTION_CONNECT = 0;
    private static final int ITEM_ACTION_OPEN = 1;


    private static final byte NO_GEAR_DATA = (byte)255;

    private static final byte DALI_COMMAND_OFF = 0;
    private static final byte DALI_COMMAND_DIM_UP = 1;
    private static final byte DALI_COMMAND_DIM_DOWN = 2;
    private static final byte DALI_COMMAND_STEP_UP = 3;
    private static final byte DALI_COMMAND_STEP_DOWN = 4;
    private static final byte DALI_COMMAND_RECALL_MAX_LEVEL = 5;
    private static final byte DALI_COMMAND_RECALL_MIN_LEVEL = 6;
    private static final byte DALI_COMMAND_STEP_DOWN_AND_OFF = 7;
    private static final byte DALI_COMMAND_ON_AND_STEP_UP = 8;
    private static final byte DALI_COMMAND_QUERY_STATUS = (byte)0x90;
    private static final byte DALI_COMMAND_QUERY_POWER_LEVEL = (byte)0xa0;


    // controller to phone
    private static final byte MESSAGE_TYPE_GEAR_DATA = 'U';

    private static final byte MESSAGE_TYPE_RESPONSE = 'A';

    // phone to controller
    private static final byte MESSAGE_TYPE_QUERY = 'Q';
    private static final byte MESSAGE_TYPE_COMMAND = 'D';
    private static final byte MESSAGE_TYPE_POWER = 'P';

    private static final byte MESSAGE_TYPE_BROADCAST = 'B';

    // common
    private static final byte MESSAGE_TYPE_EXCEPTION = 'E';

    private static final byte MESSAGE_END = '!';

    private static final int MESSAGE_IND_TYPE = 0;
    private static final int MESSAGE_IND_ID = 1;


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
        gearMap = new HashMap();

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

                final RecyclerListItem item = recyclerListItems.get(position);
                if (item.getAction() == ITEM_ACTION_CONNECT) { // connect to address in item extra
                    stopScan(); // stops scan if it's running
                    connect(item.getExtra());
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            titleView.setText(item.getTitle());
                        }
                    });
                } else if (item.getAction() == ITEM_ACTION_OPEN) { // open gear fragment
                    showGearFragment(item.getId());
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
        /*if (deviceAddress != null) { // check whether saved device address exists. if yes, connect to it
            Log.d(TAG, "Found saved device address ("+ deviceAddress + "). Connecting...");
            connect(deviceAddress);
        }*/

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
            case R.id.action_query:
                sendStatusQuery((byte)0);
                return true;
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
    private void showGearFragment(final byte gearId) {
        if (gearMap.get(gearId) == null) {
            Log.d(TAG, "unable to show gear fragment: id " + (int)gearId + " not found");
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.setVisibility(View.GONE); // hide list of gears
                fragmentLayout.setVisibility(View.VISIBLE); // show fragment container
                titleView.setText(gearMap.get(gearId).getName()); // set title
            }
        });

        gearFragment = GearFragment.newInstance(gearMap.get(gearId), this);

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
    public void onGearFragmentAction(int which, int value, final byte gearId) {
        if (which != GearFragment.ACTION_CLOSE && gearMap.get(gearId) == null) {
            Log.d(TAG, "onGearFragmentAction: gear id not valid");
            return;
        }
        Log.d(TAG, "onGearFragmentAction: gear id = " + gearId);
        switch (which) {
            case GearFragment.ACTION_POWER: // set gear power on (value != 0) / off (value == 0)
                //setGearPower(gearId, value != 0);
                //if (value == 0) broadcastPowerOff();
                break;
            case GearFragment.ACTION_POWER_LEVEL: // set gear power level
                if (value == 0) setGearPower(gearId, false);
                else setGearPowerLevel(gearId, value);
                break;
            case GearFragment.ACTION_STEP: // not implemented
                // step
                break;
            case GearFragment.ACTION_CLOSE: // close the fragment
                closeActiveFragment();
                break;
            case GearFragment.ACTION_RENAME: // rename gear
                final String newName = gearFragment.getNewName(); // get new name from fragment
                gearMap.get(gearId).setName(newName); // set name
                int i;
                for (i = 0; i < recyclerListItems.size(); i++) {
                    if (recyclerListItems.get(i).getId() == gearId) {
                        recyclerListItems.get(i).setTitle(newName);
                        break;
                    }
                }
                final int idx = i;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.getAdapter().notifyItemChanged(idx); // notify list adapter
                        titleView.setText(newName); // update title
                    }
                });
                break;
            default:
                Log.d(TAG, "Invalid GearFragment action");
        }
    }

    // set power on/off for gear with id=gearId
    private void setGearPower(byte gearId, boolean on) {
        if (on) return; // no command to set power on... handled by setting gear power level
        byte[] data;
        if (gearId != NO_GEAR_DATA)
            data = new byte[]{MESSAGE_TYPE_COMMAND, gearId, 1, DALI_COMMAND_OFF, MESSAGE_END};
        else data = new byte[]{MESSAGE_TYPE_BROADCAST, DALI_COMMAND_OFF, MESSAGE_END};
        sendData(data);
    }
    // set power level for gear with id=gearId
    private void setGearPowerLevel(byte gearId, int powerLevel) {
        byte[] data;
        if (gearId != NO_GEAR_DATA)
            data = new byte[]{MESSAGE_TYPE_POWER, gearId, 0, (byte) powerLevel, MESSAGE_END};
        else data = new byte[]{MESSAGE_TYPE_BROADCAST, (byte)powerLevel, MESSAGE_END};
        sendData(data);
    }

    private void sendStatusQuery(byte gearId) {
        Log.d(TAG, "Sending status query...");
        byte[] data = {MESSAGE_TYPE_COMMAND, gearId, 1, DALI_COMMAND_QUERY_STATUS, MESSAGE_END};
        sendData(data);
    }

    // updates gears
    // if matching id isn't found, new gear will be added
    private int updateGears(byte[] bytes) {

        if (bytes[0] == NO_GEAR_DATA) {
            Log.d(TAG, "No devices to update");
            return 0;
        }

        int count = 0;
        int i, dataEndIdx;
        i = 0;
        while (i < bytes.length) {
            dataEndIdx = i+1;
            while (dataEndIdx < bytes.length && bytes[dataEndIdx] != '!') {
                dataEndIdx++;
            }

            DaliGear toUpdate = (DaliGear)gearMap.get(bytes[i]);
            if (toUpdate == null) {
                Log.d(TAG, "New gear with id=" + (int)bytes[i]);
                String gearName = "New gear [" + (int)bytes[i] + "]";
                gearMap.put(bytes[i], new DaliGear(gearName, Arrays.copyOfRange(bytes, i, dataEndIdx)));
                recyclerListItems.add(new RecyclerListItem(gearName, ITEM_ACTION_OPEN, bytes[i]));
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        recyclerView.getAdapter().notifyDataSetChanged();
                    }
                });
            } else
                toUpdate.setData(Arrays.copyOfRange(bytes, i, dataEndIdx));
            i = dataEndIdx+1;
            count++;
        }
        return count;
    }

    private void parseResponse(byte[] bytes) {
        if (bytes.length == 1) { // no id
            Log.d(TAG, "Response: value=" + bytes[0]);
        } else { // id, value
            Log.d(TAG, "Response: id=" + bytes[0] + " value=" + bytes[1]);
        }
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
        recyclerListItems.add(new RecyclerListItem(device.getName() != null ?
                device.getName() : device.getAddress(), device.getAddress(), ITEM_ACTION_CONNECT));


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
        if (connectedDevice != null)
            deviceAddress = connectedDevice.getAddress();

        gearMap.clear();
        recyclerListItems.clear();

        // this gear instance will be used to send broadcast commands
        DaliGear g = new DaliGear("All");
        g.setId(NO_GEAR_DATA);
        gearMap.put(g.getId(), g);
        recyclerListItems.add(new RecyclerListItem("All", ITEM_ACTION_OPEN, NO_GEAR_DATA));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                recyclerView.getAdapter().notifyDataSetChanged();
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
                int startIndex = 0;
                int endIndex;
                int index = 0;
                while (index < bytes.length) {
                    if (bytes[index] == MESSAGE_END) {
                        endIndex = index;
                        if (currentMessage != null) {
                            byte[] message = new byte[currentMessage.length + endIndex - startIndex];
                            System.arraycopy(currentMessage, 0, message, 0, currentMessage.length);
                            System.arraycopy(bytes, startIndex, message, currentMessage.length, endIndex-startIndex);
                            parseMessage(message);
                            currentMessage = null;
                        } else {
                            parseMessage(Arrays.copyOfRange(bytes, startIndex, endIndex));
                        }
                        startIndex = endIndex+1;
                    }
                    index++;
                }
                if (startIndex < bytes.length) {
                    if (currentMessage != null) {
                        byte[] newCurrentMessage = new byte[bytes.length-startIndex + currentMessage.length];
                        System.arraycopy(currentMessage, 0, newCurrentMessage, 0, currentMessage.length);
                        System.arraycopy(bytes, startIndex, newCurrentMessage,
                                currentMessage.length, bytes.length-startIndex);
                        currentMessage = newCurrentMessage;
                    } else {
                        currentMessage = new byte[bytes.length-startIndex];
                        System.arraycopy(bytes, startIndex, currentMessage, 0, bytes.length-startIndex);
                    }
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
        //onConnected(); // skip actual connection
        bleManager.connect(this, address);
    }

    private void connect(BluetoothDevice device){
        Log.d(TAG, "Connecting to " + device.getAddress());
        //onConnected(); // skip actual connection
        bleManager.connect(this, device.getAddress());
    }


    // bluetooth send data to UART
    private void sendData(String text) {
        final byte[] value = text.getBytes(Charset.forName("UTF-8"));
        sendData(value);
    }

    private void sendData(byte[] data) {
        if (uartService != null) {
            final String datastr = new String(data, Charset.forName("UTF-8"));
            Log.d(TAG, "Sending data:\n" + datastr);
            // Split the value into chunks (UART service has a maximum number of characters that can be written )
            for (int i = 0; i < data.length; i += TX_MAX_CHARS) {
                final byte[] chunk = Arrays.copyOfRange(data, i, Math.min(i + TX_MAX_CHARS, data.length));
                bleManager.writeService(uartService, UUID_TX, chunk);
            }
        } else {
            Log.w(TAG, "Uart Service not discovered. Unable to send data");
        }
    }

    private void parseMessage(byte[] bytes) {
        final String data = new String(bytes, Charset.forName("UTF-8"));
        Log.d(TAG, "parsing message: \"" + data + "\"");
        switch (bytes[MESSAGE_IND_TYPE]) {
            case MESSAGE_TYPE_GEAR_DATA:
                Log.d(TAG, "message: gear data");
                updateGears(Arrays.copyOfRange(bytes, 1, bytes.length));
                break;
            case MESSAGE_TYPE_RESPONSE:
                parseResponse(Arrays.copyOfRange(bytes, 1, bytes.length));
                break;
            case MESSAGE_TYPE_EXCEPTION:
                Log.d(TAG, "message: exception");
                // handle exception
                break;
            default:
                Log.d(TAG, "message: not recognized (" + (int)bytes[MESSAGE_IND_TYPE] + ")");
                break;
        }
    }


    private void enableRxNotifications() {
        bleManager.enableNotification(uartService, UUID_RX, true);
    }
}
