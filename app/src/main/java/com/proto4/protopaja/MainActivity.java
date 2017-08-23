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
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
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
import com.proto4.protopaja.ui.HelpFragment;
import com.proto4.protopaja.ui.ListFragment;
import com.proto4.protopaja.ui.ProtoListItem;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity implements BleScanner.ScanListener,
        BleManager.BleManagerListener, GearFragment.Listener, ListFragment.Listener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private Toolbar toolbar;
    private ProgressBar progressBar;

    private Menu overflowMenu;

    private TextView infoTextView;

    private FrameLayout fragmentLayout;

    private Fragment activeFragment;
    private GearFragment gearFragment;
    private ListFragment listFragment;
    private HelpFragment helpFragment;

    private ProtoListItem lastExpandedItem;

    private BleManager bleManager;
    private BleScanner bleScanner;

    private boolean scanning, skipConnect;

    private BluetoothDevice connectedDevice;
    private String deviceAddress;

    private ArrayList<BluetoothDevice> foundDevices;

    private DaliGear[] gears;
    private int[] groups;

    private String[] gearNames;
    private String[] groupNames;


    private BluetoothGattService uartService;

    private byte[] currentMessage;


    private Handler handler;


    public static final int GEARS_LEN = 32;
    public static final int GROUPS_LEN = 4;


    private static final String SHARED_PREFS_MAIN = "SHARED_PREFS_MAIN";

    private static final String DEVICE_ADDRESS = "DEVICE_ADDRESS";
    private static final String LAST_DEVICE_ADDRESS = "LAST_DEVICE_ADDRESS";
    private static final String AUTO_CONNECT = "AUTO_CONNECT";
    private static final String SAVED_GEARS = "SAVED_GEARS";

    private static final byte F_MENU_MAIN = 1;
    private static final byte F_MENU_GEAR = 2;
    private static final byte F_MENU_GEAR_SELECTION = 4;
    private static final byte F_MENU_DEBUG = 8;


    private static final byte GROUP_ALL = (byte)255;
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
    private static final byte MESSAGE_TYPE_CTEMP = 'T';
    private static final byte MESSAGE_TYPE_INIT = 'I';


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

        handler = new Handler();

        // toolbar, shows app name / "scanning..." / "connecting...", has overflow menu
        toolbar = (Toolbar) findViewById(R.id.main_toolbar);
        setSupportActionBar(toolbar);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "navigation onClick: show gear list");
                showGears();
            }
        });

        // progress bar on toolbar, visible while scanning / connecting
        progressBar = (ProgressBar) findViewById(R.id.main_toolbar_progress_bar);
        progressBar.setVisibility(View.GONE);

        foundDevices = new ArrayList<>(); // scanned bluetooth devices

        gears = new DaliGear[GEARS_LEN];
        gearNames = new String[GEARS_LEN];
        groups = new int[GROUPS_LEN];
        Arrays.fill(groups, 255);
        groupNames = new String[GROUPS_LEN];

        infoTextView = (TextView) findViewById(R.id.main_info_text_view);
        infoTextView.setText("No devices");
        infoTextView.setTextSize(32); // maybe should check screen dimensions before setting text size


        // layout for holding fragments
        fragmentLayout = (FrameLayout) findViewById(R.id.main_fragment_content);
        fragmentLayout.setVisibility(View.VISIBLE);
        fragmentLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "FRAGMENT LAYOUT CLICKED");
            }
        });

        listFragment = ListFragment.newInstance();
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

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu()");
        overflowMenu = menu;
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        setVisibleMenuGroups((byte)(F_MENU_MAIN)); // | F_MENU_DEBUG));
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_scan:
                startScan();
                return true;
            case R.id.action_disconnect:
                disconnect();
                return true;
            case R.id.action_show_help:
                showHelp();
                return true;
            case R.id.action_show_found_devices:
                showFoundDevices();
                return true;
            case R.id.action_show_gear_list:
                showGears();
                return true;
            case R.id.action_create_group:
                addCheckedItemsToGroup(255);
                return true;
            case R.id.action_remove_from_group:
                if (listFragment == null) {
                    Log.d(TAG, "options item action: remove from group: gearListFragment==null");
                    return true;
                }
                int groupId = listFragment.getExpandedGroupId();
                if (groupId == 255) {
                    Log.d(TAG, "options item action: remove from group: cannot remove from base group");
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            infoTextView.setText("Unable to remove item(s)");
                        }
                    });
                } else removeCheckedItemsFromGroup(groupId);
                return true;
            case R.id.action_skip_connect:
                skipConnect = true;
                stopScan();
                onConnected();
                return true;
            case R.id.action_print_groups:
                String str = "";
                for (int g = 0; g < GROUPS_LEN; g++) {
                    str += "group #" + g + ":";
                    for (int i = 0; i < GEARS_LEN; i++) {
                        if ((groups[g] & (1 << i)) != 0)
                            str += "_" + i;
                    }
                    str += "\n";
                }
                Log.d(TAG, str);
                return true;
            // gear fragment actions
            case R.id.action_rename_gear:
                Log.d(TAG, "menu action: rename gear");
                gearFragment.renameGear();
                return true;
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
        overflowMenu.setGroupVisible(R.id.menu_group_gear_fragment, (flags & F_MENU_GEAR) != 0);
        overflowMenu.setGroupVisible(R.id.menu_group_grouping, (flags & F_MENU_GEAR_SELECTION) != 0);
        overflowMenu.setGroupVisible(R.id.menu_group_debug, (flags & F_MENU_DEBUG) != 0);
    }

    private void showBackNavigation(boolean show) {
        getSupportActionBar().setDisplayHomeAsUpEnabled(show);
        getSupportActionBar().setDisplayShowHomeEnabled(show);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
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
    protected void onResume() {
        super.onResume();
        loadValues();
        showFoundDevices();     // or showGears() ?

        if (deviceAddress == null || !BluetoothAdapter.checkBluetoothAddress(deviceAddress))
            Log.d(TAG, "onResume: no valid device address");

        if (bleScanner != null && foundDevices.size() == 0) {
            Log.d(TAG, "onResume: starting scan");
            startScan();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause: saving values");
        saveValues();
    }

    @Override
    protected void onDestroy(){
        // release used bluetooth resources
        disconnect();
        
        super.onDestroy();
    }
    
    // save names and groups in file named [deviceAddress]
    private void saveValues() {
        Log.d(TAG, "saveValues");
        String filename = deviceAddress != null ? deviceAddress : "";
        if (filename.length() == 0) {
            Log.w(TAG, "saveValues: no device address, not saving values");
            return;
        }
        SharedPreferences.Editor editor0 = getSharedPreferences(SHARED_PREFS_MAIN, MODE_PRIVATE).edit();
        editor0.putString(LAST_DEVICE_ADDRESS, deviceAddress);
        editor0.apply();
        
        SharedPreferences.Editor editor = getSharedPreferences(filename, MODE_PRIVATE).edit();

        int savedGears = 0;
        
        for (int i = 0; i < GEARS_LEN; i++) {
            savedGears |= gears[i] != null ? (1 << i) : 0;
            editor.putString("GEARNAME#" + i, (gears[i] != null && gearNames[i] != null) ? gearNames[i] : "");
        }
        editor.putInt(SAVED_GEARS, savedGears);
        for (int i = 0; i < GROUPS_LEN; i++) {
            editor.putInt("GROUP#" + i, groups[i]);
            editor.putString("GROUPNAME#" + i, (groupNames[i] != null) ? groupNames[i] : "");
        }
        editor.apply();
    }

    // loads saved values for last device connected
    private void loadValues() {
        SharedPreferences prefs = getSharedPreferences(SHARED_PREFS_MAIN, MODE_PRIVATE);
        if (prefs.contains(LAST_DEVICE_ADDRESS)) {
            Log.d(TAG, "loadValues: shared preferences found");
            
            String addr = prefs.getString(LAST_DEVICE_ADDRESS, "");
            if (addr.length() > 0) {
                deviceAddress = addr;
                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                if (adapter != null) {
                    if (BluetoothAdapter.checkBluetoothAddress(deviceAddress)) {  // checks if address is valid
                        BluetoothDevice device = adapter.getRemoteDevice(deviceAddress);
                        if (!foundDevices.contains(device)) foundDevices.add(device);
                    } else Log.d(TAG, "loadValues: address found but device couldn't be added: invalid address");
                } else Log.d(TAG, "loadValues: address found but device couldn't be added: adapter == null");
                
                // load groups and names
                SharedPreferences devPrefs = getSharedPreferences(addr, MODE_PRIVATE);
                int gs = devPrefs.getInt(SAVED_GEARS, 0);
                for (int i = 0; i < GEARS_LEN; i++) {
                    if ((gs & (1 << i)) != 0) gears[i] = new DaliGear((byte)i);
                    gearNames[i] = devPrefs.getString("GEARNAME#" + i, "Lamp " + i);
                }
                for (int i = 0; i < GROUPS_LEN; i++) {
                    groups[i] = devPrefs.getInt("GROUP#" + i, 0);
                    if (groups[i] != 0)
                        groupNames[i] = devPrefs.getString("GROUPNAME#" + i, "Group " + i);
                }
            }
        } else Log.d(TAG, "loadValues: shared preferences not found");
    }
    
    private void showHelp() {
        if (helpFragment == null) {
            helpFragment = new HelpFragment();
        }

        getFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_content, helpFragment)
                .commit();

        showBackNavigation(true);

        activeFragment = helpFragment;
    }

    // show gear fragment containing control and info ui for gear at specified position,
    // hides recycler list
    private void showGearFragment(final int id, final boolean isGroup) {
        Log.d(TAG, "showGearFragment(id=" + id + ", isGroup=" + (isGroup ? "true)" : "false)"));
        if (!isGroup && id > GEARS_LEN-1) {
            Log.w(TAG, "showGearFragment(): illegal id");
            return;
        }

        if (gearFragment == null) {
            Log.d(TAG, "showGearFragment: gearFragment was null");
            gearFragment = GearFragment.newInstance(this);
        }

        // parameters for gearFragment.setLimits() and .setValues()
        int powerLevel = -1, powerMin = 0, powerMax = 100, colorTemp = -1, ctWarmest = 0, ctCoolest = 0;

        if (isGroup) {
            int group = id < 0 || id >= GROUPS_LEN ? 0xffff : groups[id];
            boolean ctDefined = true; // colorTemp == -1; ^^
            DaliGear g = gears[0];
            if (g != null) {
                if (g.getDataByteInt(DaliGear.DATA_COLOR_TEMP_CAP) != 0)
                    colorTemp = g.getDataByteInt(DaliGear.DATA_COLOR_TEMP);
            }
            // get parameters for gearFragment.setLimits()
            for (int i = 0; i < GEARS_LEN; i++) {
                if ((group & (1 << i)) == 0) continue; // gears[i] is not in this group
                g = gears[i];
                if (g == null) break;
                if (ctDefined && g.getDataByteInt(DaliGear.DATA_COLOR_TEMP_CAP) != 0) {
                    if (colorTemp == -1)
                        colorTemp = g.getDataByteInt(DaliGear.DATA_COLOR_TEMP);
                    else
                        ctDefined = colorTemp == g.getDataByteInt(DaliGear.DATA_COLOR_TEMP);
                }

                if (g.getDataByteInt(DaliGear.DATA_COLOR_TEMP_CAP) != 0) {
                    // keep warmest & coolest in bounds
                    if (ctWarmest < g.getDataByteInt(DaliGear.DATA_COLOR_WARMEST))
                        ctWarmest = g.getDataByteInt(DaliGear.DATA_COLOR_WARMEST);
                    if (ctCoolest == 0 || ctCoolest > g.getDataByteInt(DaliGear.DATA_COLOR_COOLEST))
                        ctCoolest = g.getDataByteInt(DaliGear.DATA_COLOR_COOLEST);
                }
            }
            if (!ctDefined) colorTemp = -1;

        } else {
            DaliGear g = gears[id];
            if (g == null) {
                Log.d(TAG, "unable to show gear fragment: groups[id=" + id + "] == null");
                return;
            }
            powerLevel = g.getDataByteInt(DaliGear.DATA_POWER);
            powerMin = g.getDataByteInt(DaliGear.DATA_POWER_MIN);
            powerMax = g.getDataByteInt(DaliGear.DATA_POWER_MAX);
            if (g.getDataByteInt(DaliGear.DATA_COLOR_TEMP_CAP) != 0) {
                colorTemp = g.getDataByteInt(DaliGear.DATA_COLOR_TEMP);
                ctWarmest = g.getDataByteInt(DaliGear.DATA_COLOR_WARMEST);
                ctCoolest = g.getDataByteInt(DaliGear.DATA_COLOR_COOLEST);
            }
            gearFragment.setInfoText(DaliGear.getInfoString(g));
        }

        gearFragment.setSliderLimits(powerMin, powerMax, ctWarmest, ctCoolest);
        gearFragment.setSliderValues(powerLevel, colorTemp);
        gearFragment.setItemIsGroup(isGroup);
        gearFragment.setItemId(id);

        getFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_content, gearFragment)
                .commit(); // sets gear fragment on fragment container

        showBackNavigation(true);
        setVisibleMenuGroups(F_MENU_GEAR);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                infoTextView.setVisibility(View.GONE);
                toolbar.setTitle(isGroup ? (id < 0 || id >= GROUPS_LEN) ?
                        getResources().getString(R.string.all_lamps) : groupNames[id] : gearNames[id]);
            }
        });

        activeFragment = gearFragment;
    }

    private void updateGearFragment() {
        Log.d(TAG, "updateGearFragment");
        if (activeFragment != gearFragment || gearFragment == null) {
            Log.d(TAG, "updateGearFragment: unable to update gearFragment");
            return;
        }
        int id = gearFragment.getItemId();
        if (!gearFragment.isItemGroup() && gears[id] != null)
            gearFragment.setGearValues(gears[id]);
    }

    private void showListFragment() {
        if (listFragment == null) {
            listFragment = ListFragment.newInstance();
            listFragment.setListener(this);
        }
        getFragmentManager().beginTransaction()
                .replace(R.id.main_fragment_content, listFragment)
                .commit(); // sets gear list fragment on fragment container

        showBackNavigation(false);
        setVisibleMenuGroups((byte)(F_MENU_MAIN));// | F_MENU_DEBUG));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //toolbar.setTitle("Lights");
            }
        });

        activeFragment = listFragment;
    }


    private void showFoundDevices() {
        if (activeFragment != listFragment) showListFragment();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFragment.clear();
                for (int i = 0; i < foundDevices.size(); i++) {
                    String name = foundDevices.get(i).getName();
                    if (name == null) name = foundDevices.get(i).getAddress();
                    listFragment.addItem(name, ProtoListItem.TYPE_DEVICE, i);
                }
                if (foundDevices.size() > 0) {
                    toolbar.setTitle(R.string.app_name);
                    listFragment.update();
                    infoTextView.setVisibility(View.GONE);
                } else {
                    infoTextView.setText(R.string.no_devices);
                    infoTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showGears() {
        if (activeFragment != listFragment) showListFragment();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFragment.clear();
                // add group containing all gears
                ProtoListItem item = new ProtoListItem(getResources().getString(R.string.all_lamps),
                        ProtoListItem.TYPE_GROUP, 255);
                listFragment.addItem(item);
                // add other groups
                for (int i = 0; i < GROUPS_LEN; i++) {
                    if (groups[i] == 0) continue;
                    if (groupNames[i] == null || groupNames[i].length() == 0)
                        groupNames[i] = "Group [" + i + "]";
                    listFragment.addItem(groupNames[i], ProtoListItem.TYPE_GROUP, i);
                }
                expandListGroup(null, true);
                listFragment.update();

                if (gears[0] != null) {
                    toolbar.setTitle(R.string.gear_list_title);
                    showRoomTemperature();
                } else {
                    infoTextView.setText(R.string.no_gears);
                    infoTextView.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    private void showRoomTemperature() {
        if (gears[0] == null) return;

        // calculate average temperature
        float tempSum = 0;
        int i = 0;
        for (i = 0; i < GEARS_LEN; i++) {
            if (gears[i] == null) break;
            tempSum += gears[i].getTemp1Float();
        }

        final float roomTemp = i == 0 ? 0 : tempSum/i;
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoTextView.setText("Room temperature: " + roomTemp + "°C");
                infoTextView.setVisibility(View.VISIBLE);
            }
        });
    }


    // callback to handle gear fragment ui actions
    @Override
    public void onGearFragmentAction(int which, int value, final int id, final boolean isGroup) {
        if (which != GearFragment.ACTION_CLOSE) {
            if ((id < GROUPS_LEN && (isGroup && groups[id] == 0)) ||
                    (id < GEARS_LEN && (!isGroup && gears[id] == null))) {
                Log.d(TAG, "onGearFragmentAction(): invalid id");
                return;
            }
        }
        Log.d(TAG, "onGearFragmentAction(): id=" + id + (isGroup ? " (group)" : " (gear)") + ", value=" + value);
        switch (which) {
            case GearFragment.ACTION_POWER: // set gear/group power level
                if (isGroup) setGroupPower(id, value);
                else setGearPower(id, value);
                break;
            case GearFragment.ACTION_COLOR_TEMP:
                if (isGroup) setGroupColorTemperature(id, value);
                else setGearColorTemperature(id, value);
                break;
            // case below may not be needed
            case GearFragment.ACTION_CLOSE: // close the fragment (show gear list fragment)
                showGears();
                break;
            case GearFragment.ACTION_RENAME: // rename gear
                final String newName = gearFragment.getNewName(); // get new name from fragment
                if (isGroup && id < GROUPS_LEN) groupNames[id] = newName;
                //else if (isGroup) /*tried to rename group for all lamps*/;
                else if (!isGroup) gearNames[id] = newName;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        toolbar.setTitle(newName);
                    }
                });

                break;
            default:
                Log.d(TAG, "Invalid GearFragment action");
        }
    }

    @Override
    public void onListFragmentAction(int which, ProtoListItem item) {

        switch (which) {
            case ListFragment.ITEM_ACTION_OPEN:
                if (item == null) break;
                showGearFragment(item.getId(), item.getType() == ProtoListItem.TYPE_GROUP);
                break;
            case ListFragment.ITEM_ACTION_CONNECT:
                stopScan();
                if (item == null) break;
                if (item.getId() > foundDevices.size()-1) break;
                connect(foundDevices.get(item.getId()));
                break;
            case ListFragment.ACTION_EXPAND_GROUP:
                if (item == null) break;
                expandListGroup(item, false);
                break;
            case ListFragment.ACTION_GROUP_SELECTED:
                if (item == null) break;
                addCheckedItemsToGroup(item.getId());
                break;
            case ListFragment.ACTION_EXPANDED_GROUP_SELECTED:
                if (item == null) break;
                removeCheckedItemsFromGroup(item.getId());
                break;
            case ListFragment.ACTION_SELECTION_START:
                setVisibleMenuGroups(F_MENU_GEAR_SELECTION);
                break;
            case ListFragment.ACTION_SELECTION_END:
                setVisibleMenuGroups((byte)(F_MENU_MAIN));// | F_MENU_DEBUG));
                break;
            default:
                Log.d(TAG, "unknown gear list fragment action");
                break;
        }
    }

    // set power level for gear with id=gearId
    private void setGearPower(int id, int powerLevel) {
        DaliGear g = gears[id];
        if (g == null) {
            Log.w(TAG, "unable to set gear power: gear not found");
            return;
        }

        Log.d(TAG, "setGearPower(): setting power " + powerLevel + " for gear \"" + gearNames[id] + "\"(id=" + id + ")");

        g.setDataByte(DaliGear.DATA_POWER, (byte)powerLevel);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateGearFragment();
            }
        });

        // send power level to central unit
        byte[] data;
        if (id != 255)
            data = new byte[]{MESSAGE_TYPE_COMMAND, (byte)id, 0, (byte) powerLevel, MESSAGE_END};
        else data = new byte[]{MESSAGE_TYPE_BROADCAST, (byte)powerLevel, MESSAGE_END};
        sendData(data);
    }

    // calls setGearPower for each group member
    // powerLevel should be from 0 to 100
    private void setGroupPower(int id, int powerLevel) {
        int group = id < 0 || id >= GROUPS_LEN ? 0xffff : groups[id];
        if (group == 0) {
            Log.w(TAG, "unable to set group power: group " + id + "not found");
            return;
        }

        DaliGear gear;
        int relativePower;
        for (int i = 0; i < GEARS_LEN; i++) {
            if ((group & (1 << i)) == 0) continue;
            gear = gears[i];
            if (gear == null) continue;
            int gpMin = gear.getDataByteInt(DaliGear.DATA_POWER_MIN);
            int gpMax = gear.getDataByteInt(DaliGear.DATA_POWER_MAX);

            relativePower = gpMin + (int)((float)powerLevel/100 * (gpMax - gpMin));
            setGearPower(i, relativePower);
        }

    }

    private void setGearColorTemperature(int id, int colorTemp) {
        DaliGear gear = gears[id];
        if (gear == null) return;
        if (gear.getDataByteInt(DaliGear.DATA_COLOR_TEMP_CAP) == 0) return;
        // check color temp range
        colorTemp = Math.min(colorTemp, gear.getDataByteInt(DaliGear.DATA_COLOR_COOLEST));
        colorTemp = Math.max(colorTemp, gear.getDataByteInt(DaliGear.DATA_COLOR_WARMEST));

        gear.setDataByte(DaliGear.DATA_COLOR_TEMP, (byte)colorTemp);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateGearFragment();
            }
        });

        // send color temperature to central unit
        byte[] data;
        data = new byte[]{MESSAGE_TYPE_CTEMP, (byte)id, (byte)colorTemp, MESSAGE_END};
        sendData(data);
    }

    // calls setGearColorTemperature for each group member
    private void setGroupColorTemperature(int id, int colorTemp) {

        int group = id < 0 || id > GROUPS_LEN ? 0xffff : groups[id];
        if (group == 0) {
            Log.w(TAG, "unable to set group color temperature: group not found");
            return;
        }

        for (int i = 0; i < GEARS_LEN; i++) {
            if ((group & (1 << i)) == 0) continue;
            setGearColorTemperature(i, colorTemp);
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

    // methods for manipulating groups

    private void addCheckedItemsToGroup(int groupId) {

        boolean newGroup = false;

        if ((groupId < 0 || groupId > GROUPS_LEN -1) && groupId != 255) {
            Log.d(TAG, "addCheckedItemsToGroup: invalid group id: " + groupId);
            return;
        }

        if (groupId == 255) {   // new group
            int i = 0;
            while (i < GROUPS_LEN && groups[i] != 0) {
                i++;
            }
            if (i == GROUPS_LEN) {
                Log.d(TAG, "addCheckedItemsToGroup: unable to create new group: group array is full");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        infoTextView.setText("Unable to create more groups");
                    }
                });
                return;
            }
            newGroup = true;
            groupId = i;
        }

        int selected = listFragment.getSelected();
        for (int i = 0; i < GEARS_LEN; i++) {
            //groups[groupId] |= (selected & (1 << i));
            if ((selected & (1 << i)) != 0) {
                groups[groupId] |= 1 << i;
                byte[] bytes = new byte[]{MESSAGE_TYPE_GROUP, (byte)groupId, '+', (byte)i, MESSAGE_END};
                sendData(bytes);
            }
        }

        if (newGroup) {
            Log.d(TAG, "adding new group with id=" + groupId);

            listFragment.addItem("New group [" + groupId + "]", ProtoListItem.TYPE_GROUP, groupId);
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFragment.clearSelection();
            }
        });

    }

    private void removeCheckedItemsFromGroup(final int groupId) {
        if (groupId == NO_GEAR_DATA) return;

        int selected = listFragment.getSelected();


        for (int i = 0; i < GEARS_LEN; i++) {
            if ((selected & (1 << i)) != 0) {
                groups[groupId] &= (0xffff ^ (1 << i));
                Log.d(TAG, "removeCheckedItemsFromGroup: group=" + groups[groupId]);
                Log.d(TAG, "sending group message: remove member");
                byte[] bytes = new byte[]{MESSAGE_TYPE_GROUP, (byte) groupId, '-', (byte) i, MESSAGE_END};
                sendData(bytes);
            }
        }

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (groups[groupId] == 0) listFragment.removeExpandedGroup();
                listFragment.removeSelectedItems();
                listFragment.clearSelection();
            }
        });
    }

    private void expandListGroup(ProtoListItem item, boolean force) {
        if (item == null) item = lastExpandedItem;
        if (item == null) return;
        int id = item.getId();
        int group = id < 0 || id >= GROUPS_LEN ? 0xffff : groups[id];
        ArrayList<ProtoListItem> items = new ArrayList<>();
        ProtoListItem it;
        for (int i = 0; i < GEARS_LEN; i++) {
            if ((group & (1 << i)) != 0 && gears[i] != null) {
                it = new ProtoListItem(gearNames[i], ProtoListItem.TYPE_GEAR, i);
                it.setValue(gears[i].getPowerRatio());
                items.add(it);
                Log.d(TAG, "expandListGroup: added item with id=" + i);
            }
        }
        if (items.size() == 0) {
            Log.d(TAG, "expandListGroup: nothing to expand");
            return;
        }
        listFragment.expand(id, items, force);
        lastExpandedItem = item;
    }


    // sets gear constants
    // if matching id isn't found, new gear will be added
    private int setGearConstants(byte[] bytes) {
        // bytes = { id, min power, max power, color temp cap, coolest, warmest }
        final int gearId = bytes[0] < 0 ? bytes[0] + 256 : bytes[0];
        if (gearId == NO_GEAR_DATA) {
            Log.d(TAG, "setGearConstants(): No device specified");
            return -1;
        }
        if (gearId < 0 || gearId > GEARS_LEN) {
            Log.d(TAG, "setGearConstants: invalid gear id");
            return -1;
        }

        DaliGear toUpdate = gears[gearId];
        if (toUpdate == null) {     // add new gear
            Log.d(TAG, "New gear with id=" + (int)gearId);

            final String gearName = "New lamp [" + (int)gearId + "]";
            toUpdate = new DaliGear((byte)gearId);
            gears[gearId] = toUpdate;
            gearNames[gearId] = gearName;
        } else {
            Log.d(TAG, "setting constants for existing gear");
        }

        toUpdate.setConstants(Arrays.copyOfRange(bytes, 1, bytes.length));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateGearFragment();
            }
        });

        return 0;
    }

    // updates gear, should be called only when received update message from central
    // if matching id isn't found, new gear will be added
    private int updateGear(byte[] bytes) {
        // bytes = { id, status, power level, color temp }
        final int gearId = bytes[0] < 0 ? bytes[0] + 256 : bytes[0];

        if (gearId == NO_GEAR_DATA) {
            Log.d(TAG, "updateGear(): No device specified");
            return -1;
        }
        if (gearId < 0 || gearId > GEARS_LEN) {
            Log.d(TAG, "updateGear: invalid gear id " + gearId);
            return -1;
        }

        DaliGear toUpdate = gears[gearId];
        if (toUpdate == null) {     // add new gear
            Log.d(TAG, "New gear with id=" + gearId);

            final String gearName = "New lamp [" + gearId + "]";
            toUpdate = new DaliGear((byte)gearId);
            gears[gearId] = toUpdate;
            gearNames[gearId] = gearName;
        } else {
            Log.d(TAG, "updating existing gear");
        }

        toUpdate.update(Arrays.copyOfRange(bytes, 1, bytes.length));

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activeFragment == gearFragment && gearFragment != null) {
                    if (gearFragment.getItemId() == gearId && !gearFragment.isItemGroup()) {
                        gearFragment.setGearValues(gears[gearId]);
                        gearFragment.update();
                    }
                }
            }
        });

        if (activeFragment == listFragment)
            showRoomTemperature(); // update shown room temperature

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateGearFragment();
            }
        });

        return 0;
    }

    private void sendInitialization() {
        byte[] bytes = {MESSAGE_TYPE_INIT, MESSAGE_END};

        if (bluetoothEnabled() && uartService != null) {
            Log.d(TAG, "sending init message");
            sendData(bytes);
        } else {
            Log.d(TAG, "bluetooth not ready, resend init message");
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendInitialization();
                }
            }, 1500);
        }
    }


    private void parseResponse(byte[] bytes) {
        if (bytes.length == 1) { // no id
            Log.d(TAG, "Response: value=" + bytes[0]);
        } else { // id, value
            Log.d(TAG, "Response: id=" + bytes[0] + " value=" + bytes[1]);
        }
    }

    // bluetooth

    private void disconnect() {
        if (bleManager == null) {
            Log.d(TAG, "disconnect(): bleManager == null");
            return;
        }
        bleManager.disconnect();
        bleManager.close();
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                infoTextView.setText("Disconnected");
                infoTextView.setVisibility(View.VISIBLE);
            }
        });
    }

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

        String name = device.getName();
        if (name == null) name = device.getAddress();
        final ProtoListItem item = new ProtoListItem(name, ProtoListItem.TYPE_DEVICE, foundDevices.size()-1);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (foundDevices.size() == 1) listFragment.clear();
                listFragment.addItem(item);
                listFragment.update();
                infoTextView.setText(foundDevices.size() + (foundDevices.size() > 1 ? " devices found" : " device found"));
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

        boolean sameAsLastTime = false;

        if (connectedDevice != null) {
            if (deviceAddress != null)
                sameAsLastTime = deviceAddress.equals(connectedDevice.getAddress());
            deviceAddress = connectedDevice.getAddress();
        } else deviceAddress = "00:00:00:00:00:00";

        if (!sameAsLastTime) {
            gears = new DaliGear[GEARS_LEN];
            Arrays.fill(groups, 0);
        }

        // add group to hold all gears
        final ProtoListItem item = new ProtoListItem(getResources().getString(R.string.all_lamps),
                ProtoListItem.TYPE_GROUP, 255);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                listFragment.clear();
                listFragment.addItem(item);
                listFragment.update();
                progressBar.setVisibility(View.GONE);
                toolbar.setTitle(R.string.app_name);
                infoTextView.setVisibility(View.GONE);
            }
        });
        showGears();

        if (!skipConnect)
            sendInitialization();

        // DEBUG
        if (skipConnect) {
            // add some dummy gears
            for (int i = 0; i < 4; i++) {
                byte[] bytes = {(byte)i, 0, (byte)254, 1, 64, 27};
                setGearConstants(bytes);
            }
            for (int i = 4; i < 8; i++) {
                byte[] bytes = {(byte)i, 0, (byte)100, 0, 0, 0};
                setGearConstants(bytes);
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
                infoTextView.setText("Disconnected");
                infoTextView.setVisibility(View.VISIBLE);
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
        if (bleScanner == null) {
            Log.d(TAG, "startScan: bleScanner was null");
            bleScanner = new BleScanner(bleManager.getAdapter(this), this);
        }
        if (!bleScanner.isReady()) {
            Log.d(TAG, "startScan: bleScanner was not ready");
            bleScanner = new BleScanner(bleManager.getAdapter(this), this);
        }
        foundDevices.clear();
        showFoundDevices();

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //showListFragment();
                listFragment.clear();
                listFragment.update();
                infoTextView.setText("No devices");
                infoTextView.setVisibility(View.VISIBLE);
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
        if (device == null) {
            Log.d(TAG, "connect: device==null; returning");
            return;
        }
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
        int groupId = bytes[0] < 0 ? bytes[0] + 256 : bytes[0];
        if (groupId > GROUPS_LEN-1)
            return;
        int gearId = bytes[2] < 0 ? bytes[2] + 256 : bytes[2];
        if (gearId > GEARS_LEN-1)
            return;
        if (bytes[1] == '+') {
            groups[groupId] |= (1 << gearId);
        } else if (bytes[1] == '-') {
            groups[groupId] &= (0xffff ^ (1 << gearId));
        }

        Log.d(TAG, "group " + groupId + " after modification: " + groups[groupId]);
    }


    private void enableRxNotifications() {
        bleManager.enableNotification(uartService, UUID_RX, true);
    }
}
