package com.proto4.protopaja.ble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.LinkedList;
import java.util.UUID;

/**
 * Created by user on 30.06.17.
 */

public class BleGattHandler extends BluetoothGattCallback {
    private final static String TAG = BleGattHandler.class.getSimpleName();


    // Constants
    private static String CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";

    interface ServiceAction {
        ServiceAction NULL = new ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                return true;
            }
        };

        boolean execute(BluetoothGatt bluetoothGatt);
    }

    private final LinkedList<BleGattHandler.ServiceAction> actionQueue = new LinkedList<>();
    private volatile ServiceAction currentAction;

    protected void read(BluetoothGattService gattService, String characteristicUUID, String descriptorUUID) {
        ServiceAction action = serviceReadAction(gattService, characteristicUUID, descriptorUUID);
        actionQueue.add(action);
    }

    private BleGattHandler.ServiceAction serviceReadAction(final BluetoothGattService gattService, final String characteristicUuidString, final String descriptorUuidString) {
        return new BleGattHandler.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                final UUID characteristicUuid = UUID.fromString(characteristicUuidString);
                final BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(characteristicUuid);
                if (characteristic != null) {
                    if (descriptorUuidString == null) {
                        // Read Characteristic
                        if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                            bluetoothGatt.readCharacteristic(characteristic);
                            return false;
                        } else {
                            Log.w(TAG, "read: characteristic not readable: " + characteristicUuidString);
                            return true;
                        }
                    } else {
                        // Read Descriptor
                        final UUID descriptorUuid = UUID.fromString(descriptorUuidString);
                        final BluetoothGattDescriptor descriptor = characteristic.getDescriptor(descriptorUuid);
                        if (descriptor != null) {
                            bluetoothGatt.readDescriptor(descriptor);
                            return false;
                        } else {
                            Log.w(TAG, "read: descriptor not found: " + descriptorUuidString);
                            return true;
                        }
                    }
                } else {
                    Log.w(TAG, "read: characteristic not found: " + characteristicUuidString);
                    return true;
                }
            }
        };
    }

    void enableNotification(BluetoothGattService gattService, String characteristicUUID, boolean enable) {
        ServiceAction action = serviceNotifyAction(gattService, characteristicUUID, enable);
        actionQueue.add(action);
    }

    private BleGattHandler.ServiceAction serviceNotifyAction(final BluetoothGattService gattService, final String characteristicUuidString, final boolean enable) {
        return new BleGattHandler.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                if (characteristicUuidString != null) {
                    final UUID characteristicUuid = UUID.fromString(characteristicUuidString);
                    final BluetoothGattCharacteristic dataCharacteristic = gattService.getCharacteristic(characteristicUuid);

                    if (dataCharacteristic == null) {
                        Log.w(TAG, "Characteristic with UUID " + characteristicUuidString + " not found");
                        return true;
                    }

                    final UUID clientCharacteristicConfiguration = UUID.fromString(CHARACTERISTIC_CONFIG);
                    final BluetoothGattDescriptor config = dataCharacteristic.getDescriptor(clientCharacteristicConfiguration);
                    if (config == null)
                        return true;

                    // enableNotification/disable locally
                    bluetoothGatt.setCharacteristicNotification(dataCharacteristic, enable);
                    // enableNotification/disable remotely
                    config.setValue(enable ? BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(config);

                    return false;
                } else {
                    Log.w(TAG, "Characteristic UUID is null");
                    return true;
                }
            }
        };
    }

    void enableIndication(BluetoothGattService gattService, String characteristicUUID, boolean enable) {
        ServiceAction action = serviceIndicateAction(gattService, characteristicUUID, enable);
        actionQueue.add(action);
    }

    private BleGattHandler.ServiceAction serviceIndicateAction(final BluetoothGattService gattService, final String characteristicUuidString, final boolean enable) {
        return new BleGattHandler.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                if (characteristicUuidString != null) {
                    final UUID characteristicUuid = UUID.fromString(characteristicUuidString);
                    final BluetoothGattCharacteristic dataCharacteristic = gattService.getCharacteristic(characteristicUuid);

                    if (dataCharacteristic == null) {
                        Log.w(TAG, "Characteristic with UUID " + characteristicUuidString + " not found");
                        return true;
                    }

                    final UUID clientCharacteristicConfiguration = UUID.fromString(CHARACTERISTIC_CONFIG);
                    final BluetoothGattDescriptor config = dataCharacteristic.getDescriptor(clientCharacteristicConfiguration);
                    if (config == null)
                        return true;

                    // enableNotification/disable remotely
                    config.setValue(enable ? BluetoothGattDescriptor.ENABLE_INDICATION_VALUE : BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                    bluetoothGatt.writeDescriptor(config);

                    return false;
                } else {
                    Log.w(TAG, "Characteristic UUID is null");
                    return true;
                }
            }
        };
    }


    void write(BluetoothGattService gattService, String uuid, byte[] value) {
        ServiceAction action = serviceWriteAction(gattService, uuid, value);
        actionQueue.add(action);
    }


    private BleGattHandler.ServiceAction serviceWriteAction(final BluetoothGattService gattService, final String uuid, final byte[] value) {
        return new BleGattHandler.ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                final UUID characteristicUuid = UUID.fromString(uuid);
                final BluetoothGattCharacteristic characteristic = gattService.getCharacteristic(characteristicUuid);
                if (characteristic != null) {
                    characteristic.setValue(value);
                    bluetoothGatt.writeCharacteristic(characteristic);
                    return false;
                } else {
                    Log.w(TAG, "write: characteristic not found: " + uuid);
                    return true;
                }
            }
        };
    }

    protected void clear() {
        currentAction = null;
        actionQueue.clear();
    }

    void execute(BluetoothGatt gatt) {
        if (currentAction == null) {
            while (!actionQueue.isEmpty()) {
                final BleGattHandler.ServiceAction action = actionQueue.pop();
                currentAction = action;
                if (!action.execute(gatt))
                    break;
                currentAction = null;
            }
        }
    }

    @Override
    public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorRead(gatt, descriptor, status);

        currentAction = null;
        execute(gatt);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

        currentAction = null;
        execute(gatt);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);

        currentAction = null;
        execute(gatt);
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            actionQueue.clear();
            currentAction = null;
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);

        characteristic = null;
        execute(gatt);
    }

    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
    }

    static BleGattHandler createHandler(final GattListener listener) {
        return new BleGattHandler() {
            @Override
            public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
                super.onConnectionStateChange(gatt, status, newState);
                listener.onConnectionStateChange(gatt, status, newState);
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt, int status) {
                super.onServicesDiscovered(gatt, status);
                listener.onServicesDiscovered(gatt, status);
            }

            @Override
            public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
                super.onCharacteristicRead(gatt, characteristic, status);
                listener.onCharacteristicRead(gatt, characteristic, status);
            }

            @Override
            public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                super.onCharacteristicChanged(gatt, characteristic);
                listener.onCharacteristicChanged(gatt, characteristic);
            }

            @Override
            public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
                super.onDescriptorRead(gatt, descriptor, status);
                listener.onDescriptorRead(gatt, descriptor, status);
            }

            @Override
            public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
                super.onReadRemoteRssi(gatt, rssi, status);
                listener.onReadRemoteRssi(gatt, rssi, status);
            }
        };
    }

    public interface GattListener {
        void onConnectionStateChange(BluetoothGatt gatt, int status, int newState);

        void onServicesDiscovered(BluetoothGatt gatt, int status);

        void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status);

        void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic);

        void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status);

        void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status);
    }
}
