package com.sensirion.libble.services.sensirion.smartgadget;

import android.bluetooth.BluetoothGattService;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.sensirion.libble.devices.Peripheral;
import com.sensirion.libble.listeners.services.TemperatureListener;
import com.sensirion.libble.utils.TemperatureUnit;

import java.util.Iterator;

import static com.sensirion.libble.utils.TemperatureConverter.convertTemperatureToCelsius;
import static com.sensirion.libble.utils.TemperatureConverter.convertTemperatureToFahrenheit;
import static com.sensirion.libble.utils.TemperatureConverter.convertTemperatureToKelvin;

public class SmartgadgetTemperatureService extends AbstractSmartgadgetRHTService<TemperatureListener> {

    //SERVICE UUID.
    public static final String SERVICE_UUID = "00002234-b38d-4985-720e-0f993a68ee41";

    //CHARACTERISTICS UUID.
    private static final String TEMPERATURE_NOTIFICATIONS_UUID = "00002235-b38d-4985-720e-0f993a68ee41";

    private TemperatureUnit mValueUnit = null;

    public SmartgadgetTemperatureService(@NonNull final Peripheral peripheral, @NonNull final BluetoothGattService bluetoothGattService) {
        super(peripheral, bluetoothGattService, TEMPERATURE_NOTIFICATIONS_UUID);
    }

    /**
     * Notifies the service listeners of a new live temperature value.
     */
    @Override
    void notifyListenersNewLiveValue() {
        Log.d(TAG, String.format("notifyListenersNewLiveValue -> Notifying temperature value: %f%s from sensor %s.", mLastValue, mValueUnit, mSensorName));
        final Iterator<TemperatureListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewTemperature(mPeripheral, mLastValue, mSensorName, mValueUnit);
            } catch (final Exception e) {
                Log.e(TAG, "notifyListenersNewLiveValue -> The following exception was produced -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Notifies the service listeners of the reading of a new historical value.
     */
    @Override
    void notifyListenersNewHistoricalValue(final float value, final long timestamp) {
        Log.d(TAG, String.format("notifyListenersNewLiveValue -> Notifying historical temperature value: %f%s from sensor %s.", mLastValue, mValueUnit, mSensorName));
        final Iterator<TemperatureListener> iterator = mListeners.iterator();
        while (iterator.hasNext()) {
            try {
                iterator.next().onNewHistoricalTemperature(mPeripheral, value, timestamp, mSensorName, mValueUnit);
            } catch (final Exception e) {
                Log.e(TAG, "notifyListenersNewLiveValue -> The following exception was produced -> ", e);
                iterator.remove();
            }
        }
    }

    /**
     * Parses the extracted value unit to {@link com.sensirion.libble.utils.TemperatureUnit}.
     *
     * @param valueUnit {@link java.lang.String} with the value unit specified in the device.
     */
    @Override
    void setValueUnit(@NonNull final String valueUnit) {
        if (valueUnit.endsWith("C")) {
            mValueUnit = TemperatureUnit.CELSIUS;
        } else if (valueUnit.endsWith("F")) {
            mValueUnit = TemperatureUnit.FAHRENHEIT;
        } else if (valueUnit.endsWith("K")) {
            mValueUnit = TemperatureUnit.KELVIN;
        } else {
            Log.e(TAG, String.format("setValueUnit -> The service does not know how to manage value unit %s.", valueUnit));
        }
    }

    @Override
    public boolean isServiceReady() {
        if (mLastValue == null) {
            registerDeviceCharacteristicNotifications();
        } else if (getSensorName() != null && mValueUnit == null) {
            return true;
        }
        return false;
    }

    /**
     * Obtains the latest temperature in Celsius.
     *
     * @return {@link java.lang.Float} with the temperature in Celsius - <code>null</code> if the temperature is not available yet.
     */
    @SuppressWarnings("unused")
    @Nullable
    public Float getTemperatureInCelsius() {
        if (mLastValue == null || mValueUnit == null) {
            registerDeviceCharacteristicNotifications();
            Log.w(TAG, "getTemperatureInCelsius -> Temperature is not available yet.");
            return null;
        }
        return convertTemperatureToCelsius(mLastValue, mValueUnit);
    }

    /**
     * Obtains the latest temperature in Fahrenheit.
     *
     * @return {@link java.lang.Float} with the temperature in Fahrenheit - <code>null</code> if the temperature is not available yet.
     */
    @SuppressWarnings("unused")
    @Nullable
    public Float getTemperatureInFahrenheit() {
        if (mLastValue == null || mValueUnit == null) {
            registerDeviceCharacteristicNotifications();
            Log.w(TAG, "getTemperatureInFahrenheit -> Temperature is not available yet.");
            return null;
        }
        return convertTemperatureToFahrenheit(mLastValue, mValueUnit);
    }

    /**
     * Obtains the latest temperature in Kelvin.
     *
     * @return {@link java.lang.Float} with the temperature in Fahrenheit - <code>null</code> if the temperature is not available yet.
     */
    @SuppressWarnings("unused")
    @Nullable
    public Float getTemperatureInKelvin() {
        if (mLastValue == null || mValueUnit == null) {
            Log.w(TAG, "getTemperatureInKelvin -> Temperature is not available yet.");
            registerDeviceCharacteristicNotifications();
            return null;
        }
        return convertTemperatureToKelvin(mLastValue, mValueUnit);
    }
}