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
import android.view.View;

import com.proto4.protopaja.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by user on 29.06.17.
 */

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

    public BleScanner(BluetoothAdapter adapter, ScanListener listener){
        if (adapter != null)
            scanner = adapter.getBluetoothLeScanner();
        scanSettings = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build();
        scanFilters = new ArrayList<>();
        foundDevices = new ArrayList<>();
        scanListener = listener;
        handler = new Handler();
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
                for(byte byteChar : data)
                    stringBuilder.append(String.format("%02X ", byteChar));
                Log.d(TAG, "[" + result.getDevice().getAddress() + "] " +
                        "Scan record bytes:\n" + stringBuilder.toString() + "\n");
            }

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
