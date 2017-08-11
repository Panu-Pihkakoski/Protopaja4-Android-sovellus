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
import com.proto4.protopaja.ui.ListFragment;
import com.proto4.protopaja.ui.RecyclerListAdapter;
import com.proto4.protopaja.ui.RecyclerListItem;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements BleScanner.ScanListener,
                BleManager.BleManagerListener, GearFragment.GearFragmentListener,
                ListFragment.ListFragmentListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Toolbar toolbar;
    private ProgressBar progressBar;

    private Menu overflowMenu;

    private TextView titleView;

    private FrameLayout fragmentLayout;

    private Fragment activeFragment;
    private GearFragment gearFragment;
    private ListFragment listFragment;

    private BleManager bleManager;
    private BleScanner bleScanner;

    private boolean scanning, skipConnect;

    private BluetoothDevice connectedDevice;
    private String deviceAddress;

    private ArrayList<BluetoothDevice> foundDevices;
    private Map<Byte, DaliGear> gearMap;
    private Map<Byte, DaliGear> groupMap;

    private BluetoothGattService uartService;

    private byte[] currentMessage;

    private static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String AUTO_CONNECT = "AUTO_CONNECT";


    private static final byte F_MENU_MAIN = 1;
    private static final byte F_MENU_GEAR = 2;
    private static final byte F_MENU_GEAR_SELECTION = 4;
    private static final byte F_MENU_DEBUG = 8;



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
    private static final byte MESSAGE_TYPE_GEAR_CONST = 'C';
    private static final byte MESSAGE_TYPE_GEAR_UPDATE = 'U';
    private static final byte MESSAGE_TYPE_RESPONSE = 'A';

    // phone to controller
    private static final byte MESSAGE_TYPE_QUERY = 'Q';
    private static final byte MESSAGE_TYPE_COMMAND = 'D';
    //private static final byte MESSAGE_TYPE_POWER = 'P'; // ?
    private static final byte MESSAGE_TYPE_CTEMP = 'T';


    private static final byte MESSAGE_TYPE_BROADCAST = 'B';

    // common
    private static final byte MESSAGE_TYPE_EXCEPTION = 'E';
    private static final byte MESSAGE_TYPE_GROUP = 'G';

    private static final byte MESSAGE_END = '!';

    private static final int MESSAGE_IND_TYPE = 0;
    private static final int MESSAGE_IND_ID = 1;


    private static final int REQUEST_ENABLE_BT = 1;
    private static final int PERMISSION_REQUEST_COARSE_LOCATION = 1;


    // Service Constants from adafruit's example
    public static final String UUID_SERVICE = "6e400001-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_RX = "6e400003-b5a3-f393-e0a9-e50e24dcca9e";
    public static final String UUID_TX = "6e400002-b5a3-f393-e0a9-e50e24dcca9e";

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
        groupMap = new HashMap();

        // clickable title below toolbar, shows connected device address / dali gear name
        titleView = (TextView) findViewById(R.id.main_title_view);
        /*titleView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (activeFragment != null)
                    closeActiveFragment();
            }
        });*/
        titleView.setText("No devices");
        titleView.setTextSize(32); // maybe should check screen dimensions before setting text size


        // layout for holding fragments
        fragmentLayout = (FrameLayout) findViewById(R.id.main_fragment_content);
        fragmentLayout.setVisibility(View.VISIBLE);
        listFragment = new ListFragment();
        listFragment.setListener(this);
        showListFragment();

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
        scanning = skipConnect = false;

        loadValues(); // load saved values
        /*if (deviceAddress != null) { // check whether saved device address exists. if yes, connect to it
            Log.d(TAG, "Found saved device address ("+ deviceAddress + "). Connecting...");
            connect(deviceAddress);
        }*/

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu()");
        overflowMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        setVisibleMenuGroups((byte)(F_MENU_MAIN | F_MENU_DEBUG));
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
            case R.id.action_create_group:
                addCheckedItemsToGroup((byte)255);
                return true;
            case R.id.action_remove_from_group:
                if (listFragment == null) {
                    Log.d(TAG, "action_remove_from_group: listFragment == null");
                    return true; // false?
                }
                byte groupId = listFragment.getExpandedGroupId();
                removeCheckedItemsFromGroup(groupId);
                return true;
            case R.id.action_skip_connect:
                skipConnect = true;
                stopScan();
                onConnected();
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void setVisibleMenuGroups(byte flags) {
        if (overflowMenu == null) {
            Log.w(TAG, "setVisibleMenuGroups(): overflowMenu==null");
            return;
        }

        overflowMenu.setGroupVisible(R.id.menu_group_main, (flags & F_MENU_MAIN) != 0);
        overflowMenu.setGroupVisible(R.id.menu_group_gear, (flags & F_MENU_GEAR) != 0);
        overflowMenu.setGroupVisible(R.id.menu_group_grouping, (flags & F_MENU_GEAR_SELECTION) != 0);
        overflowMenu.setGroupVisible(R.id.menu_group_debug, (flags & F_MENU_DEBUG) != 0);
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
        // release used bluetooth resources
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
    private void showGearFragment(final byte gearId, boolean isGroup) {
        final DaliGear gear;
        if (!isGroup)
            gear = gearMap.get(gearId);
        else
            gear = groupMap.get(gearId);
        if (gear == null) {
            Log.d(TAG, "unable to show gear fragment: id " + (int)gearId + " not found");
            return;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                titleView.setText(gear.getName()); // set title
            }
        });

        gearFragment = GearFragment.newInstance(gear, this);

        getFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_content, gearFragment)
                .commit(); // sets gear fragment on fragment container

        activeFragment = gearFragment;
    }

    private void showListFragment() {
        Log.d(TAG, "showListFragment()");
        if (listFragment == null) {
            Log.d(TAG, "showListFragment(): listFragment was null");
            listFragment = ListFragment.newInstance(this);
            listFragment.setListener(this);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                titleView.setText("");
            }
        });
        getFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_content, listFragment)
                .commit(); // sets list fragment on fragment container
        activeFragment = listFragment;
    }

    // callback to handle gear fragment ui actions
    @Override
    public void onGearFragmentAction(int which, int value, final byte gearId, boolean isGroup) {
        if (which != GearFragment.ACTION_CLOSE && !gearMap.containsKey(gearId) && !groupMap.containsKey(gearId)) {
            Log.d(TAG, "onGearFragmentAction: gear id not valid");
            return;
        }
        Log.d(TAG, "onGearFragmentAction: gear id = " + gearId);
        switch (which) {
            case GearFragment.ACTION_POWER: // set gear/group power level
                if (isGroup) setGroupPower(gearId, value);
                else setGearPower(gearId, value);
                break;
            case GearFragment.ACTION_COLOR_TEMP:
                if (isGroup) setGroupColorTemperature(gearId, value);
                else setGearColorTemperature(gearId, value);
                break;
            case GearFragment.ACTION_STEP: // not implemented
                // step
                break;
            case GearFragment.ACTION_CLOSE: // close the fragment (show list fragment)
                showListFragment();
                break;
            case GearFragment.ACTION_RENAME: // rename gear
                final String newName = gearFragment.getNewName(); // get new name from fragment
                DaliGear gear;
                gear = gearMap.get(gearId); // set name
                if (gear == null)
                    gear = groupMap.get(gearId);
                gear.setName(newName);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listFragment.renameItem(gearId, newName);
                        titleView.setText(newName); // update title
                    }
                });
                break;
            default:
                Log.d(TAG, "Invalid GearFragment action");
        }
    }

    // list fragment interaction
    @Override
    public void onListFragmentAction(int which, RecyclerListItem item) {
        String itemTitle = "null";
        if (item != null)
            itemTitle = item.getTitle();
        Log.d(TAG, "onListFragmentAction(which=" + which + ", item=\"" + itemTitle + "\")");
        switch (which) {
            case ListFragment.ITEM_ACTION_CONNECT:  // connect to bt device
                stopScan(); // stops ble scan if running
                connect(item.getExtra()); // device address should be stored in item.extra
                break;
            case ListFragment.ITEM_ACTION_OPEN:     // open gear fragment
                showGearFragment(item.getId(), item.getType() == RecyclerListItem.TYPE_GROUP);
                break;
            case ListFragment.ITEM_ACTION_EXPAND:
                DaliGear group = groupMap.get(item.getId());
                if (group == null) break;
                listFragment.expandGroup(item, group.getGroup());
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listFragment.update();
                    }
                });
                break;
            case ListFragment.ACTION_SELECTION_START:
                setVisibleMenuGroups(F_MENU_GEAR_SELECTION);
                break;
            case ListFragment.ACTION_SELECTION_END:
                setVisibleMenuGroups((byte)(F_MENU_MAIN | F_MENU_DEBUG));
                break;
            case ListFragment.ACTION_GROUP_SELECTED:
                addCheckedItemsToGroup(item.getId());
                break;
            case ListFragment.ACTION_EXPANDED_GROUP_SELECTED:
                removeCheckedItemsFromGroup(item.getId());
                break;
            default:
                Log.d(TAG, "unknown list fragment action");
                break;
        }
    }

    // set power level for gear with id=gearId
    private void setGearPower(byte gearId, int powerLevel) {
        DaliGear g = gearMap.get(gearId);
        if (g == null) {
            Log.w(TAG, "unable to set gear power: gear not found");
            return;
        }

        Log.d(TAG, "setGearPowerLevel(): setting power for gear " + gearId);
        g.setPower((byte)powerLevel);
        float br = (float)(powerLevel-g.getMinPowerInt())/(g.getMaxPowerInt()-g.getMinPowerInt())*255;
        listFragment.getItemById(gearId).setBrightness((int)br);


        // send power level to central unit
        byte[] data;
        if (gearId != NO_GEAR_DATA)
            data = new byte[]{MESSAGE_TYPE_COMMAND, gearId, 0, (byte) powerLevel, MESSAGE_END};
        else data = new byte[]{MESSAGE_TYPE_BROADCAST, (byte)powerLevel, MESSAGE_END};
        sendData(data);
    }

    // calls setGearPower for every group member
    private void setGroupPower(byte groupId, int powerLevel) {
        DaliGear group = groupMap.get(groupId);
        if (group == null) {
            Log.w(TAG, "unable to set group power: group not found");
            return;
        }
        group.setDataByte(DaliGear.DATA_POWER, (byte)powerLevel);
        ArrayList<DaliGear> gears = group.getGroup();
        for (int i = 0; i < gears.size(); i++) {
            setGearPower(gears.get(i).getId(), powerLevel);
        }

    }

    private void setGearColorTemperature(byte gearId, int colorTemp) {
        DaliGear gear = gearMap.get(gearId);
        if (gear == null)
            return;

        gear.setDataByte(DaliGear.DATA_COLOR_TEMP, (byte)colorTemp);

        byte[] data;
        data = new byte[]{MESSAGE_TYPE_CTEMP, gearId, (byte)colorTemp, MESSAGE_END};
        sendData(data);
    }

    private void setGroupColorTemperature(byte groupId, int colorTemp) {
        DaliGear group = groupMap.get(groupId);
        if (group == null) {
            Log.w(TAG, "unable to set group color temperature: group not found");
            return;
        }
        group.setDataByte(DaliGear.DATA_COLOR_TEMP, (byte)colorTemp);
        ArrayList<DaliGear> gears = group.getGroup();
        for (int i = 0; i < gears.size(); i++) {
            setGearColorTemperature(gears.get(i).getId(), colorTemp);
        }
    }

    private void sendStatusQuery(byte gearId) {
        Log.d(TAG, "Sending status query...");
        byte[] data = {MESSAGE_TYPE_COMMAND, gearId, 1, DALI_COMMAND_QUERY_STATUS, MESSAGE_END};
        sendData(data);
    }

    private void sendQuery(byte gearId) {
        byte[] msg = new byte[]{MESSAGE_TYPE_QUERY, gearId, MESSAGE_END};
        sendData(msg);
    }

    /*
    // not needed anymore...
    private void defineGroup(byte groupId) {
        DaliGear group = groupMap.get(groupId);
        if (group == null) {
            Log.w(TAG, "unable to define group with id=" + groupId);
            return;
        }

        ArrayList<DaliGear> list = group.getGroup();
        byte[] msg = new byte[3 + list.size()];
        msg[0] = MESSAGE_TYPE_GROUP;
        msg[1] = groupId;
        for (int i = 0; i < list.size(); i++) {
            msg[i+2] = list.get(i).getId();
        }
        msg[msg.length-1] = MESSAGE_END;
        sendData(msg);
    }*/

    private void addCheckedItemsToGroup(byte groupId) {

        boolean newGroup = false;
        DaliGear group = null, baseGroup = groupMap.get(NO_GEAR_DATA);

        if (groupId == NO_GEAR_DATA) { // new group
            newGroup = true;
            byte newId = 0;
            while (groupMap.containsKey(newId)) {
                newId++;
                if (newId == NO_GEAR_DATA) {
                    Log.w(TAG, "all group ids reserved");
                    return;
                }
            }
            groupId = newId;
        } else
            group = groupMap.get(groupId);
        if (group == null) {    // group with id=groupId not found; create new group
            newGroup = true;
            group = new DaliGear("New group [" + groupId + "]");
            group.setId(groupId);
        }

        // get selected items from list fragment and add to group
        for (RecyclerListItem item : listFragment.getSelectedItems()) {
            DaliGear gear = gearMap.get(item.getId());
            if (gear != null) {
                baseGroup.addGroupMember(gear);
                if (group.addGroupMember(gear)) {
                    Log.d(TAG, "sending group message: add member");
                    // send group message
                    byte[] bytes = new byte[]{MESSAGE_TYPE_GROUP, groupId, '+', gear.getId(), MESSAGE_END};
                    sendData(bytes);
                }
            }
        }
        if (!group.isGroup()) {   // group is empty?
            Log.d(TAG, "tried to create empty group");
            return;
        }
        if (newGroup) {
            Log.d(TAG, "adding new group with id=" + groupId);
            groupMap.put(groupId, group);
            final RecyclerListItem item = new RecyclerListItem("New group [" + groupId + "]",
                    RecyclerListItem.TYPE_GROUP, groupId);

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listFragment.addItem(item);
                }
            });
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFragment.clearSelection();
            }
        });
    }

    private void removeCheckedItemsFromGroup(byte groupId) {
        if (groupId == NO_GEAR_DATA) return;
        DaliGear group = groupMap.get(groupId);
        if (group == null) {
            Log.w(TAG, "removeCheckedItemsFromGroup(): group " + groupId + " not found");
            return;
        }

        for (RecyclerListItem item : listFragment.getSelectedItems()) {
            DaliGear gear = gearMap.get(item.getId());
            if (gear == null) continue;
            if (group.removeGroupMember(gear)) {
                Log.d(TAG, "sending group message: remove member");
                // send group message
                byte[] bytes = new byte[]{MESSAGE_TYPE_GROUP, groupId, '-', gear.getId(), MESSAGE_END};
                sendData(bytes);
            }
        }
        final boolean groupEmpty;
        if (!group.isGroup()) {
            groupMap.remove(groupId);
            groupEmpty = true;
        } else groupEmpty = false;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (groupEmpty)
                    listFragment.removeExpandedGroup();
                listFragment.removeSelectedItems();
                listFragment.clearSelection();
            }
        });
    }

    // sets gear constants
    // if matching id isn't found, new gear will be added
    private int setGearConstants(byte[] bytes) {
        // bytes = { id, min power, max power, color temp cap, coolest, warmest }
        final byte gearId = bytes[0];
        if (gearId == NO_GEAR_DATA) {
            Log.d(TAG, "setGearConstants(): No device specified");
            return -1;
        }

        DaliGear toUpdate = gearMap.get(gearId);
        if (toUpdate == null) {     // add new gear
            Log.d(TAG, "New gear with id=" + (int)gearId);

            final String gearName = "New gear [" + (int)gearId + "]";
            toUpdate = new DaliGear(gearName, gearId);
            gearMap.put(gearId, toUpdate);
            groupMap.get(NO_GEAR_DATA).addGroupMember(toUpdate);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listFragment.addItem(gearName, RecyclerListItem.TYPE_GEAR, gearId);
                }
            });
        } else {
            Log.d(TAG, "setting constants for existing gear");
        }

        toUpdate.setConstants(Arrays.copyOfRange(bytes, 1, bytes.length));

        return 0;
    }

    // updates gear
    // if matching id isn't found, new gear will be added
    private int updateGear(byte[] bytes) {
        // bytes = { id, status, power level, color temp }
        final byte gearId = bytes[0];
        if (gearId == NO_GEAR_DATA) {
            Log.d(TAG, "updateGear(): No device specified");
            return -1;
        }

        DaliGear toUpdate = gearMap.get(gearId);
        if (toUpdate == null) {     // add new gear
            Log.d(TAG, "New gear with id=" + (int)gearId);

            final String gearName = "New gear [" + (int)gearId + "]";
            toUpdate = new DaliGear(gearName, gearId);
            gearMap.put(gearId, toUpdate);
            groupMap.get(NO_GEAR_DATA).addGroupMember(toUpdate);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    listFragment.addItem(gearName, RecyclerListItem.TYPE_GEAR, gearId);
                }
            });
        } else {
            Log.d(TAG, "updating existing gear");
        }

        toUpdate.update(Arrays.copyOfRange(bytes, 1, bytes.length));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activeFragment == gearFragment) {
                    gearFragment.update();
                }
            }
        });

        return 0;
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
        if (!scanning) {
            Log.d(TAG, "Found device ignored - scan was stopped");
            return;
        }

        foundDevices.add(device);
        final RecyclerListItem item = new RecyclerListItem(device.getName() != null ?
                device.getName() : device.getAddress(), device.getAddress(), RecyclerListItem.TYPE_BT_DEVICE);
        final BluetoothDevice d = device;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFragment.addItem(d.getName() != null ?
                        d.getName() : d.getAddress(), d.getAddress(), RecyclerListItem.TYPE_BT_DEVICE, (byte)0);
                if (foundDevices.size() == 1)
                    titleView.setText("Scanned devices");
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
        groupMap.clear();

        // add group to hold all gears
        DaliGear g = new DaliGear("All");
        g.setId(NO_GEAR_DATA);
        groupMap.put(NO_GEAR_DATA, g);
        final RecyclerListItem item = new RecyclerListItem("All", RecyclerListItem.TYPE_GROUP, NO_GEAR_DATA);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFragment.clear();
                listFragment.addItem(item);
                progressBar.setVisibility(View.GONE);
                toolbar.setTitle(R.string.app_name);
                titleView.setText("Connected");
            }
        });

        if (skipConnect) {
            // add some dummy gears; remove sometime
            for (int i = 0; i < 5; i++) {
                byte[] bytes = {(byte) i, 0, 0, 0};
                updateGear(bytes);
                gearMap.get((byte)i).setDataByte(DaliGear.DATA_COLOR_COOLEST, (byte)67);
                gearMap.get((byte)i).setDataByte(DaliGear.DATA_COLOR_WARMEST, (byte)27);
            }
        }
    }

    @Override
    public void onDisconnected(){
        Log.d(TAG, "Device disconnected");
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.GONE);
                toolbar.setTitle(R.string.app_name);
                titleView.setText("Disconnected");
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
                String datastr = new String(bytes, Charset.forName("UTF-8"));
                datastr += "\n";
                for (int i = 0; i < bytes.length; i++) {
                    datastr += " | " + (bytes[i] < 0 ? bytes[i] + 256 : bytes[i]);
                }
                Log.d(TAG, "data received:\n" + datastr);
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
        showListFragment();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFragment.clear();
                titleView.setText("No devices");
                progressBar.setVisibility(View.VISIBLE);
                toolbar.setTitle("Scanning...");
            }
        });
        scanning = true;
        bleScanner.start(BleScanner.DEFAULT_SCAN_PERIOD);
    }

    public void stopScan(){
        scanning = false;
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
            String datastr = new String(data, Charset.forName("UTF-8"));
            datastr += "\n";
            for (int i = 0; i < data.length; i++) {
                datastr += " | " + (data[i] < 0 ? data[i] + 256 : data[i]);
            }
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
            case MESSAGE_TYPE_GEAR_CONST:
                Log.d(TAG, "message: gear constants");
                setGearConstants(Arrays.copyOfRange(bytes, 1, bytes.length));
                break;
            case MESSAGE_TYPE_GEAR_UPDATE:
                Log.d(TAG, "message: gear update");
                updateGear(Arrays.copyOfRange(bytes, 1, bytes.length));
                break;
            case MESSAGE_TYPE_GROUP:
                parseGroupMessage(Arrays.copyOfRange(bytes, 1, bytes.length));
                break;
            case MESSAGE_TYPE_RESPONSE:     // not implemented
                parseResponse(Arrays.copyOfRange(bytes, 1, bytes.length));
                break;
            case MESSAGE_TYPE_EXCEPTION:    // not implemented
                Log.d(TAG, "message: exception");
                // handle exception
                break;
            default:
                Log.d(TAG, "message: not recognized (" + (int)bytes[MESSAGE_IND_TYPE] + ")");
                break;
        }
    }

    private void parseGroupMessage(byte[] bytes) {
        if (bytes.length < 3) return;
        byte groupId = bytes[0];
        DaliGear group = groupMap.get(groupId);
        if (group == null) {
            Log.d(TAG, "group message: couldn't find group " + groupId + ":\tcreating new group");
            group = new DaliGear("New group [" + (groupId < 0 ? groupId + 256 : groupId) + "]", groupId);
            groupMap.put(groupId, group);
        }
        if (bytes[1] == '+') {
            DaliGear gearToAdd = gearMap.get(bytes[2]);
            if (gearToAdd == null) {
                Log.w(TAG, "group message (+): couldn't find gear " + bytes[2]);
                return;
            }
            group.addGroupMember(gearToAdd);
        } else if (bytes[1] == '-') {
            DaliGear gearToRemove = gearMap.get(bytes[2]);
            if (gearToRemove == null) {
                Log.w(TAG, "group message (-): coundn't find gear " + bytes[2]);
                return;
            }
            group.removeGroupMember(gearToRemove);
        }
        // debug
        String groupContents = "";
        for (DaliGear g : group.getGroup()) {
            groupContents += g.getName() + "\n";
        }
        Log.d(TAG, "group " + groupId + " after modification:\n" + groupContents);
    }


    private void enableRxNotifications() {
        bleManager.enableNotification(uartService, UUID_RX, true);
    }
}
