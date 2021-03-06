// Source: https://github.com/adafruit/Bluefruit_LE_Connect_Android (MIT License)

package com.proto4.protopaja.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class BleScanner {

    private static final String TAG = BleScanner.class.getSimpleName();

    private BluetoothLeScanner scanner;
    private ScanSettings scanSettings;
    private List<ScanFilter> scanFilters;
    private boolean scanning;
    private ArrayList<BluetoothDevice> foundDevices;
    private ScanListener scanListener;
    private Handler handler;

    public static final int DEFAULT_SCAN_PERIOD = 10000; // 10 secs

    public BleScanner(BluetoothAdapter adapter){
        if (adapter != null)
            scanner = adapter.getBluetoothLeScanner();
        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanFilters = new ArrayList<>();
        foundDevices = new ArrayList<>();
        handler = new Handler();
    }

    public void setListener(ScanListener listener) {
        this.scanListener = listener;
    }

    public ArrayList<BluetoothDevice> getFoundDevices() {
        return foundDevices;
    }

    public void setSettings(ScanSettings settings){
        scanSettings = settings;
    }

    public void setFilters(ArrayList<ScanFilter> filters){
        scanFilters = filters;
    }

    public void addFilter(ScanFilter filter){
        scanFilters.add(filter);
    }

    public boolean isReady(){
        return scanner != null;
    }

    public void start(){
        if (scanning) return;
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                scanner.startScan(scanFilters, scanSettings, leScanCallback);
            }
        });
        scanning = true;
        Log.d(TAG, "Scan started\n");
    }

    public void start(long timeout){
        if (scanning) return;
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                stop();
            }
        }, timeout);
        start();
    }

    public void stop(){
        if (!scanning) return;
        handler.removeCallbacksAndMessages(null);
        AsyncTask.execute(new Runnable() {
            @Override
            public void run() {
                scanner.stopScan(leScanCallback);
            }
        });
        foundDevices.clear();
        scanning = false;
        scanListener.onScanStopped();
        Log.d(TAG, "Scan stopped\n");
    }

    private ScanCallback leScanCallback = new ScanCallback() {

        @Override
        public void onScanFailed(int errorCode){
            Log.d(TAG, "Scan failed with error code " + errorCode + "\n");
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            Log.d(TAG, "Scan result:\n\tDevice Name: " + result.getDevice().getName() +
                    "\n\tAddress: " + result.getDevice().getAddress() +
                    "\n\trssi: " + result.getRssi());

            // print advertisement data
            byte[] data = result.getScanRecord().getBytes();
            String dataString = new String(data);
            if (data != null && data.length > 0) {
                final StringBuilder stringBuilder = new StringBuilder(data.length);
                int i = 0;
                for(byte byteChar : data) {
                    stringBuilder.append(String.format("%02X ", byteChar));
                    if (i % 8 == 7) stringBuilder.append('\n');
                    i++;
                }
                Log.d(TAG, "[" + result.getDevice().getAddress() + "] " +
                        "Scan record bytes:\n" + stringBuilder.toString() + "\n("
                        + dataString + ")");
            }

            // filter irrelevant results
            // data[25] : 0x19, data[26] : 0x31, data[27] : 0xd4
            boolean isBlec = (data[25] == (byte)0x19 && data[26] == (byte)0x31 && data[27] == (byte)0xd4);

            if (!isBlec) return;

            BluetoothDevice device = result.getDevice();
            if (foundDevices.contains(device))
                return;
            foundDevices.add(device);
            if (scanListener != null)
                scanListener.onDeviceFound(device);
        }
    };

    public interface ScanListener {
        void onDeviceFound(BluetoothDevice device);
        void onScanStopped();
    }
}
