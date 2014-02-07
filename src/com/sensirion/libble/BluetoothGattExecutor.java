
package com.sensirion.libble;

import java.util.LinkedList;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;

import com.sensirion.libble.peripheral.sensor.AbstractSensor;

public class BluetoothGattExecutor extends BluetoothGattCallback {

    public interface ServiceAction {
        public static final ServiceAction NULL = new ServiceAction() {
            @Override
            public boolean execute(BluetoothGatt bluetoothGatt) {
                // it is null action. do nothing.
                return true;
            }
        };

        /***
         * Executes action.
         * 
         * @param bluetoothGatt
         * @return true - if action was executed instantly. false if action is
         *         waiting for feedback.
         */
        public boolean execute(BluetoothGatt bluetoothGatt);
    }

    private final LinkedList<BluetoothGattExecutor.ServiceAction> mQueue = new LinkedList<ServiceAction>();
    private volatile ServiceAction mCurrentAction;

    public void update(final AbstractSensor sensor) {
        mQueue.add(sensor.update());
    }

    public void enable(AbstractSensor sensor, boolean enable) {
        final ServiceAction[] actions = sensor.enable(enable);
        for (ServiceAction action : actions) {
            this.mQueue.add(action);
        }
    }

    public void execute(BluetoothGatt gatt) {
        if (mCurrentAction != null)
            return;

        boolean next = !mQueue.isEmpty();
        while (next) {
            final BluetoothGattExecutor.ServiceAction action = mQueue.pop();
            mCurrentAction = action;
            if (!action.execute(gatt))
                break;

            mCurrentAction = null;
            next = !mQueue.isEmpty();
        }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);

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
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            mQueue.clear();
        }
    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt,
            BluetoothGattCharacteristic characteristic,
            int status) {
        mCurrentAction = null;
        execute(gatt);
    }
}
