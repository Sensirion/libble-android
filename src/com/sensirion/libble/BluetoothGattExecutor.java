package com.sensirion.libble;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.util.Log;

import com.sensirion.libble.old.peripheral.sensor.AbstractSensor;

import java.util.LinkedList;

public class BluetoothGattExecutor extends BluetoothGattCallback {
    private static final String TAG = BluetoothGattExecutor.class.getSimpleName();

    private final LinkedList<BluetoothGattExecutor.ServiceAction> mQueue = new LinkedList<ServiceAction>();
    private volatile ServiceAction mCurrentAction;

    public void update(final AbstractSensor sensor) {
        Log.i(TAG, "update()");
        mQueue.add(sensor.update());
    }

    public void enable(AbstractSensor sensor, boolean enable) {
        Log.i(TAG, "enable() sensor: " + sensor.getName() + " is " + enable);
        final ServiceAction[] actions = sensor.enable(enable);
        for (ServiceAction action : actions) {
            this.mQueue.add(action);
        }
    }

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        Log.i(TAG, "onConnectionStateChange()");
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            mQueue.clear();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
                                     BluetoothGattCharacteristic characteristic,
                                     int status) {
        Log.i(TAG, "onCharacteristicRead()");
        mCurrentAction = null;
        execute(gatt);
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt,
                                      BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        Log.i(TAG, "onCharacteristicWrite()");

        mCurrentAction = null;
        execute(gatt);
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        Log.i(TAG, "onDescriptorWrite()");

        mCurrentAction = null;
        execute(gatt);
    }

    public void execute(BluetoothGatt gatt) {
        if (mCurrentAction == null) {
            Log.i(TAG, "execute() -> next action from mQueue");
            boolean next = !mQueue.isEmpty();
            while (next) {
                final BluetoothGattExecutor.ServiceAction action = mQueue.pop();
                mCurrentAction = action;
                if (!action.execute(gatt))
                    break;

                mCurrentAction = null;
                next = !mQueue.isEmpty();
            }
        } else {
            Log.w(TAG, "execute() -> there is another action in progress!");
        }
    }

    public interface ServiceAction {
        public static final ServiceAction NULL = new ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                // it is null action. do nothing.
                return true;
            }
        };

        /**
         * Executes action.
         *
         * @param bluetoothGatt
         * @return true - if action was executed instantly. false if action is
         * waiting for feedback.
         */
        public boolean execute(BluetoothGatt bluetoothGatt);
    }
}
