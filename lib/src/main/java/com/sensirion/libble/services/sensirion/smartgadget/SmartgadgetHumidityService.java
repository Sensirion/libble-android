package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.services.HumidityListener;
import com.sensirion.libble.utils.HumidityUnit;

import java.util.Iterator;

public class SmartgadgetHumidityService extends AbstractSmartgadgetRHTService<HumidityListener> {

    //SERVICE UUID.
    public static final String SERVICE_UUID = "00001234-b38d-4985-720e-0f993a68ee41";

    //CHARACTERISTICS UUID.
    private static final String HUMIDITY_NOTIFICATIONS_UUID = "00001235-b38d-4985-720e-0f993a68ee41";

    //CLASS ATTRIBUTES.
    private HumidityUnit mValueUnit;

    public SmartgadgetHumidityService(@NonNull final Peripheral peripheral, @NonNull final BluetoothGattService bluetoothGattService) {
        super(peripheral, bluetoothGattService, HUMIDITY_NOTIFICATIONS_UUID);
    }

    /**
     * Notifies the service listeners of the new humidity value.
     */
    @Override
    void notifyListenersNewLiveValue() {
        Log.d(TAG, String.format("notifyListenersNewLiveValue -> Notifying humidity value: %f%s from sensor %s.", mLastValue, mValueUnit, mSensorName));
        final Iterator<HumidityListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewHumidity(mPeripheral, mLastValue, mSensorName, mValueUnit);
            } catch (final Exception e) {
                Log.e(TAG, "notifyListenersNewLiveValue -> The following exception was produced -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Notifies the service listeners the reading of the new historical value.
     */
    @Override
    void notifyListenersNewHistoricalValue(final float value, final long timestamp) {
        Log.d(TAG, String.format("notifyListenersNewLiveValue -> Notifying humidity value: %f%s from sensor %s.", mLastValue, mValueUnit, mSensorName));
        final Iterator<HumidityListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewHistoricalHumidity(mPeripheral, value, timestamp, mSensorName, mValueUnit);
            } catch (final Exception e) {
                Log.e(TAG, "notifyListenersNewLiveValue -> The following exception was produced -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Parses the extracted value unit to {@link com.sensirion.libble.utils.HumidityUnit}
     *
     * @param valueUnit {@link java.lang.String} with the value unit specified in the device.
     */
    @Override
    void setValueUnit(@NonNull final String valueUnit) {
        if (valueUnit.endsWith("RH")) {
            mValueUnit = HumidityUnit.RELATIVE_HUMIDITY;
        } else {
            Log.w(TAG, String.format("setValueUnit -> Value unit %s is unknown.", valueUnit));
        }
    }

    /**
     * Checks if the service has all the information it needs.
     * @return <code>true</code> if the service is ready - <code>false</code> otherwise.
     */
    @Override
    public boolean isSynchronized() {
        return mLastValue != null && mValueUnit != null && mSensorName != null;
    }

    /**
     * Obtains the latest relative humidity.
     *
     * @return {@link java.lang.Float} with the relative humidity - <code>null</code> if the relative humidity is not known.
     */
    public Float getRelativeHumidity() {
        if (mLastValue == null) {
            Log.e(TAG, "getRelativeHumidity -> Humidity is not known yet.");
            return null;
        }
        return mLastValue;
    }
}