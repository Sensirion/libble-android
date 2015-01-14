package com.sensirion.libble.peripherals;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.os.DeadObjectException;
import android.util.Log;

import java.util.LinkedList;
import java.util.Queue;

/**
 * This class solves the bug 58381 that provokes that the Bluetooth LE stack becomes really unstable.
 * <p/>
 * This class acts as an intermediate between the user an the stack preventing that the stacks
 * receives more than one order at a time, think that makes that the probability of a stack crash gets reduced.
 * <p/>
 * If the stack crashes this class will capture the exception and cleans it's cache.
 * <p/>
 * http://stackoverflow.com/questions/17870189/android-4-3-bluetooth-low-energy-unstable
 * http://code.google.com/p/android/issues/detail?id=58381
 */
class BleStackProtector extends BluetoothGattCallback {
    private static final String TAG = BleStackProtector.class.getSimpleName();
    private final Queue<ServiceAction> mActionQueue = new LinkedList<ServiceAction>() {
        @Override
        public boolean add(ServiceAction newAction) {
            if (newAction == null) {
                Log.e(TAG, ".mActionQueue -> Received a null action.");
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
    public void onCharacteristicRead(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        mCurrentAction = null;
        execute(gatt);
    }

    @Override
    public void onCharacteristicWrite(final BluetoothGatt gatt, final BluetoothGattCharacteristic characteristic, final int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        mCurrentAction = null;
        execute(gatt);
    }

    @Override
    public void onDescriptorWrite(final BluetoothGatt gatt, final BluetoothGattDescriptor descriptor, final int status) {
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
     * Method that controls that all the instructions stored in the queue are executed one by one.
     */
    public synchronized void execute(final BluetoothGatt gatt) {
        try {
            if (mCurrentAction == null) {
                executeQueue(gatt);
            } else {
                checkQueueTimeout();
            }
        } catch (final Exception e) {
            Log.e(TAG, String.format("execute() --> BleStack has collapsed with %s objects. Cache has been cleared. (Exception type = %s)", mActionQueue.size(), e.getClass()));
            cleanCharacteristicCache();
        }
    }

    private void executeQueue(final BluetoothGatt gatt) {
        while (mActionQueue.size() > 0) {
            mLastAccessQueueTimestamp = System.currentTimeMillis();
            final ServiceAction action = mActionQueue.element();
            if (action == null) {
                break;
            }
            mActionQueue.remove();
            mCurrentAction = action;
            if (!action.execute(gatt)) {
                break;
            }
            mCurrentAction = null;
        }
    }

    private void checkQueueTimeout() {
        if (mLastAccessQueueTimestamp < System.currentTimeMillis() - TIMEOUT_MILLISECONDS) {
            mActionQueue.poll();
            Log.e(TAG, String.format("execute() -> Timeout produced with a command from type %s, so the element has been deleted.", mCurrentAction));
            mCurrentAction = null;
        }
    }

    /**
     * Adds a bluetooth read petition to the queue.
     *
     * @param characteristic for read.
     */
    public void addReadCharacteristic(final BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            Log.e(TAG, "addReadCharacteristic -> Received a null characteristic.");
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
            Log.e(TAG, "addWriteCharacteristic -> Received a null characteristic.");
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
            Log.e(TAG, "addDescriptorCharacteristic -> Received a null descriptor.");
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
            Log.e(TAG, "addCharacteristicNotification -> Received a null characteristic.");
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
        public final boolean execute(final BluetoothGatt gatt) {
            try {
                return unsafeExecute(gatt);
            } catch (final Exception e) {
                Log.e(TAG, String.format("execute -> The %s threw the following exception of the type %s -> ", BluetoothGatt.class.getSimpleName(), e.getClass().getSimpleName()), e);
            }
            return false;
        }

        abstract boolean unsafeExecute(final BluetoothGatt gatt) throws DeadObjectException;

        @Override
        public String toString() {
            return this.getClass().getSimpleName();
        }

        protected final String TAG = String.format("%s.%s", BleStackProtector.TAG, ServiceAction.class.getSimpleName());


    }

    private static class WriteDescriptorAction extends ServiceAction {
        private final BluetoothGattDescriptor descriptor;

        private WriteDescriptorAction(final BluetoothGattDescriptor descriptor) {
            this.descriptor = descriptor;
        }

        @Override
        boolean unsafeExecute(BluetoothGatt gatt) {
            return gatt.writeDescriptor(this.descriptor);
        }
    }


    private static class ReadCharacteristicAction extends ServiceAction {
        private final BluetoothGattCharacteristic characteristic;

        private ReadCharacteristicAction(final BluetoothGattCharacteristic characteristic) {
            this.characteristic = characteristic;
        }

        @Override
        protected boolean unsafeExecute(final BluetoothGatt gatt) throws DeadObjectException {
            return gatt.readCharacteristic(this.characteristic);
        }
    }

    private static class WriteCharacteristicAction extends ServiceAction {

        private final BluetoothGattCharacteristic characteristic;
        private final String TAG = String.format("%s.%s", super.TAG, WriteCharacteristicAction.class.getSimpleName());

        WriteCharacteristicAction(final BluetoothGattCharacteristic characteristic) {
            this.characteristic = characteristic;
        }

        @Override
        boolean unsafeExecute(final BluetoothGatt gatt) throws DeadObjectException {
            final boolean characteristicSend = gatt.writeCharacteristic(this.characteristic);
            Log.w(TAG, String.format("execute -> Written characteristic with UUID: %s was a %s", this.characteristic.getUuid(), characteristicSend));
            return characteristicSend;
        }
    }

    private static class WriteCharacteristicNotification extends ServiceAction {
        private final BluetoothGattCharacteristic characteristic;
        private final boolean enable;

        WriteCharacteristicNotification(final BluetoothGattCharacteristic characteristic, final boolean enable) {
            this.characteristic = characteristic;
            this.enable = enable;
        }

        @Override
        boolean unsafeExecute(final BluetoothGatt gatt) throws DeadObjectException {
            return gatt.setCharacteristicNotification(this.characteristic, this.enable);
        }
    }
}