package com.proto4.protopaja.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.pm.PackageManager;
import android.util.Log;

import java.util.List;
import java.util.UUID;

/**
 * Created by user on 29.06.17.
 */

public class BleManager implements BleGattHandler.GattListener{

    private static final String TAG = BleManager.class.getSimpleName();


    public static final int STATUS_BT_ENABLED = 1;
    public static final int STATUS_BT_DISABLED = 2;
    public static final int STATUS_BT_UNAVAILABLE = 3;

    public static final int STATE_CONNECTING = 0;
    public static final int STATE_CONNECTED = 1;
    public static final int STATE_DISCONNECTED = 2;

    private BluetoothAdapter adapter;
    private BleManagerListener listener;
    private BluetoothDevice device;
    private BluetoothGatt gatt;
    private BleGattHandler gattHandler;
    private int connectionState;
    private String deviceAddress;

    public BleManager(Context context, BleManagerListener _listener){
        adapter = getAdapter(context);
        if (adapter == null || !adapter.isEnabled())
            Log.e(TAG, "Unable to obtain bluetooth adapter");
        listener = _listener;
        gattHandler = BleGattHandler.createHandler(this);
        connectionState = STATE_DISCONNECTED;
    }

    public int getBluetoothStatus(Context context){
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
            return STATUS_BT_UNAVAILABLE;
        final BluetoothAdapter adapter = getAdapter(context);
        if (adapter == null)
            return STATUS_BT_UNAVAILABLE;
        if (!adapter.isEnabled())
            return STATUS_BT_DISABLED;
        return STATUS_BT_ENABLED;
    }

    public BluetoothAdapter getAdapter(Context context){
        final BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        if (bluetoothManager == null) {
            return null;
        } else {
            return bluetoothManager.getAdapter();
        }
    }

    public int getConnectionState(){
        return connectionState;
    }

    public BluetoothDevice getConnectedDevice(){
        return device;
    }

    public boolean connect(Context context, String address) {
        if (adapter == null || address == null) {
            Log.w(TAG, "connect: BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Reuse existing connection
        if (deviceAddress != null && address.equalsIgnoreCase(deviceAddress) && gatt != null) {
            Log.d(TAG, "Trying to use an existing BluetoothGatt for connection.");
            if (gatt.connect()) {
                connectionState = STATE_CONNECTING;
                if (listener != null)
                    listener.onConnecting();
                return true;
            } else {
                return false;
            }
        }

        device = adapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found. Unable to connect.");
            return false;
        }

        Log.d(TAG, "Trying to create a new connection.");
        deviceAddress = address;
        connectionState = STATE_CONNECTING;
        if (listener != null) {
            listener.onConnecting();
        }

        gatt = device.connectGatt(context, false, gattHandler); // autoconnect false

        return true;
    }

    public void disconnect() {
        device = null;
        if (adapter == null || gatt == null) {
            Log.w(TAG, "disconnect: BluetoothAdapter not initialized");
            return;
        }
        gatt.disconnect();
    }

    public void clearHandler() {
        if (gattHandler != null) {
            gattHandler.clear();
        }
    }

    public void close() {
        if (gatt != null) {
            gatt.close();
            gatt = null;
            deviceAddress = null;
            device = null;
        }
    }

    public List<BluetoothGattService> getGattServices(){
        if (gatt != null)
            return gatt.getServices();
        else
            return null;
    }

    public BluetoothGattService getGattService(String uuid) {
        if (gatt != null) {
            final UUID serviceUuid = UUID.fromString(uuid);
            return gatt.getService(serviceUuid);
        } else {
            return null;
        }
    }

    public void readCharacteristic(BluetoothGattService service, String characteristicUUID) {
        readService(service, characteristicUUID, null);
    }

    public void readDescriptor(BluetoothGattService service, String characteristicUUID, String descriptorUUID) {
        readService(service, characteristicUUID, descriptorUUID);
    }

    private void readService(BluetoothGattService service, String characteristicUUID, String descriptorUUID) {
        if (service != null) {
            if (adapter == null || gatt == null) {
                Log.w(TAG, "readService: BluetoothAdapter not initialized");
                return;
            }

            gattHandler.read(service, characteristicUUID, descriptorUUID);
            gattHandler.execute(gatt);
        }
    }

    public void writeService(BluetoothGattService service, String uuid, byte[] value)
    {
        if (service != null) {
            if (adapter == null || gatt == null) {
                Log.w(TAG, "writeService: BluetoothAdapter not initialized");
                return;
            }

            gattHandler.write(service, uuid, value);
            gattHandler.execute(gatt);
        }
    }

    public void enableNotification(BluetoothGattService service, String uuid, boolean enabled) {
        if (service != null) {

            if (adapter == null || gatt == null) {
                Log.w(TAG, "enableNotification: BluetoothAdapter not initialized");
                return;
            }

            gattHandler.enableNotification(service, uuid, enabled);
            gattHandler.execute(gatt);
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState){

        if (newState == BluetoothProfile.STATE_CONNECTED) {
            connectionState = STATE_CONNECTED;

            if (listener != null)
                listener.onConnected();

            gatt.discoverServices();

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            connectionState = STATE_DISCONNECTED;

            if (listener != null)
                listener.onDisconnected();
        } else if (newState == BluetoothProfile.STATE_CONNECTING) {
            connectionState = STATE_CONNECTING;

            if (listener != null)
                listener.onConnecting();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status){
        if (status == BluetoothGatt.GATT_SUCCESS){
            if (listener != null)
                listener.onServicesDiscovered();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status){
        if (listener != null)
            listener.onDataAvailable(characteristic);
    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){
        if (listener != null)
            listener.onDataAvailable(characteristic);
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status){
        if (listener != null)
            listener.onDataAvailable(descriptor);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status){

    }

    public interface BleManagerListener {
        void onConnecting();
        void onConnected();
        void onDisconnected();
        void onServicesDiscovered();
        void onDataAvailable(BluetoothGattCharacteristic characteristic);
        void onDataAvailable(BluetoothGattDescriptor descriptor);

    }
}
