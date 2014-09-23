package com.sensirion.libble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

public class BleGattExecutor extends BluetoothGattCallback {
    private static final String TAG = BleGattExecutor.class.getSimpleName();
    private final Queue<ServiceAction> mActionQueue = new LinkedList<ServiceAction>() {
        @Override
        public boolean add(ServiceAction newAction) {
            if (newAction == null) {
                Log.e(TAG, "A null was received in the ActionQueue.");
                return false;
            }
            return super.add(newAction);
        }

    };
    private static final short TIMEOUT_MILLISECONDS = 1500;
    private ServiceAction mCurrentAction;
    private long mLastAccessQueueTimestamp = -1;

    @Override
    public void onConnectionStateChange(final BluetoothGatt gatt, final int status, final int newState) {
        Log.i(TAG, "onConnectionStateChange()");
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            mActionQueue.clear();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     int status) {
        mCurrentAction = null;
        execute(gatt);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        mCurrentAction = null;
        execute(gatt);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        mCurrentAction = null;
        execute(gatt);
    }

    @Override
    public void onReliableWriteCompleted(final BluetoothGatt gatt, final int status) {
        super.onReliableWriteCompleted(gatt, status);
        execute(gatt);
    }

    /**
     * Method that controls that all the orders made by the queue are executed one by one,
     * for making the Android stack more stable.
     */
    public synchronized void execute(final BluetoothGatt gatt) {
        try {
            if (mCurrentAction == null) {
                while (mActionQueue.size() > 0) {
                    mLastAccessQueueTimestamp = System.currentTimeMillis();
                    final ServiceAction action = mActionQueue.element();
                    if (action == null) {
                        break;
                    }
                    mActionQueue.remove();
                    mCurrentAction = action;
                    if (!action.execute(gatt))
                        break;

                    mCurrentAction = null;
                }
            } else {
                if (mLastAccessQueueTimestamp < System.currentTimeMillis() - TIMEOUT_MILLISECONDS) {
                    mActionQueue.poll();
                    Log.e(TAG, String.format("execute() -> Timeout produced with a command from type %s, so the element has been deleted.", mCurrentAction));
                    mCurrentAction = null;
                } else {
                    Log.w(TAG, "execute() -> there is another action in progress!");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, String.format("execute() --> Queue have collapse (%d elements), so it has been cleared).", mActionQueue.size()));
            cleanCharacteristicCache();
        }
    }

    /**
     * Adds a bluetooth read petition to the queue.
     *
     * @param characteristic for read.
     */
    public void addReadCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            Log.e(TAG, "Ble Stack Manager has received a null characteristic.");
        } else {
            mActionQueue.add(new ReadCharacteristicAction(characteristic));
        }
    }

    /**
     * Adds a bluetooth write petition to the queue.
     *
     * @param characteristic that wants to be wrote in the device.
     */
    public void addWriteCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            Log.e(TAG, "Ble Stack Manager has received a null characteristic.");
        } else {
            mActionQueue.add(new WriteCharacteristicAction(characteristic));
        }
    }

    /**
     * Adds a descriptor that wants to be wrote in the queue.
     *
     * @param descriptor for the queue.
     */
    public void addDescriptorCharacteristic(final BluetoothGattDescriptor descriptor) {
        if (descriptor == null) {
            Log.e(TAG, "Ble Stack Manager has received a null descriptor.");
        } else {
            mActionQueue.add(new WriteDescriptorAction(descriptor));
        }
    }

    /**
     * Adds a descriptor notification for wrote in the queue.
     *
     * @param notificationCharacteristic for the queue.
     * @param enable                     if the notifications is going to be enabled or disabled
     */
    public void addCharacteristicNotification(final BluetoothGattCharacteristic notificationCharacteristic, final boolean enable) {
        if (notificationCharacteristic == null) {
            Log.e(TAG, "Ble Stack Manager has received a null characteristic.");
        } else {
            mActionQueue.add(new WriteCharacteristicNotification(notificationCharacteristic, enable));
        }
    }

    /**
     * This method cleans the characteristic stack of the device.
     */
    public void cleanCharacteristicCache() {
        mActionQueue.clear();
    }


    //CLASSES THAT REPRESENTS THE ACTIONS
    private abstract static class ServiceAction {
        abstract boolean execute(BluetoothGatt gatt);

        @Override
        public String toString() {
            return this.getClass().getSimpleName();
        }
    }

    private static class WriteDescriptorAction extends ServiceAction {
        private final BluetoothGattDescriptor descriptor;

        private WriteDescriptorAction(final BluetoothGattDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        public boolean execute(BluetoothGatt gatt) {
            return gatt.writeDescriptor(this.descriptor);
        }
    }


    private static class ReadCharacteristicAction extends ServiceAction {
        private final BluetoothGattCharacteristic characteristic;

        private ReadCharacteristicAction(final BluetoothGattCharacteristic characteristic) {
            this.characteristic = characteristic;
        }

        @Override
        public boolean execute(final BluetoothGatt gatt) {
            return gatt.readCharacteristic(this.characteristic);
        }
    }

    private static class WriteCharacteristicAction extends ServiceAction {
        private final BluetoothGattCharacteristic characteristic;

        protected WriteCharacteristicAction(final BluetoothGattCharacteristic characteristic) {
            this.characteristic = characteristic;
        }

        @Override
        public boolean execute(final BluetoothGatt gatt) {
            final boolean characteristicSend = gatt.writeCharacteristic(this.characteristic);
            Log.w(TAG, String.format("Written characteristic with UUID: %s was a %s", this.characteristic.getUuid(), characteristicSend));
            return characteristicSend;
        }
    }


    private static class WriteCharacteristicNotification extends ServiceAction {
        private final BluetoothGattCharacteristic characteristic;
        private final boolean enable;

        protected WriteCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enable) {
            this.characteristic = characteristic;
            this.enable = enable;
        }

        @Override
        public boolean execute(final BluetoothGatt gatt) {
            return gatt.setCharacteristicNotification(this.characteristic, this.enable);
        }
    }
}