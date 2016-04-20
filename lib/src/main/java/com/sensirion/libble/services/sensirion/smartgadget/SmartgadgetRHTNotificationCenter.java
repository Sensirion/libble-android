package com.sensirion.libble.services.sensirion.smartgadget;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.util.LongSparseArray;
import android.util.Log;

import com.sensirion.libble.devices.BleDevice;
import com.sensirion.libble.listeners.services.HumidityListener;
import com.sensirion.libble.listeners.services.RHTListener;
import com.sensirion.libble.listeners.services.TemperatureListener;
import com.sensirion.libble.utils.HumidityUnit;
import com.sensirion.libble.utils.RHTDataPoint;
import com.sensirion.libble.utils.TemperatureUnit;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static com.sensirion.libble.utils.TemperatureConverter.convertTemperatureToCelsius;

/**
 * Class used by the Humidity and temperature services in order to wrap the Temperature and Humidity data for the connected devices.
 */
class SmartgadgetRHTNotificationCenter implements TemperatureListener, HumidityListener {

    //Class TAG
    private static final String TAG = SmartgadgetRHTNotificationCenter.class.getSimpleName();

    //Maps with latest RHT data of each device and each sensor.
    private static final Map<String, RHTValue> mLastHumidityAndTemperature = Collections.synchronizedMap(new HashMap<String, RHTValue>());
    private static final Map<String, HistoricalRHTValues> mRHTDownloadManagers = Collections.synchronizedMap(new HashMap<String, HistoricalRHTValues>());

    //Singleton instance.
    private static SmartgadgetRHTNotificationCenter mInstance = null;

    //Listeners by devices.
    private final Map<BleDevice, List<RHTListener>> mListeners = Collections.synchronizedMap(new HashMap<BleDevice, List<RHTListener>>());

    private SmartgadgetRHTNotificationCenter() {
    }

    public static synchronized SmartgadgetRHTNotificationCenter getInstance() {
        if (mInstance == null) {
            mInstance = new SmartgadgetRHTNotificationCenter();
        }
        return mInstance;
    }

    /**
     * Obtains a map key using the sensor name and the device.
     */
    private static String getMapKey(@NonNull final BleDevice device, @NonNull final String sensorName) {
        return String.format("%s - %s", device.getAddress(), sensorName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewHumidity(@NonNull final BleDevice device, final float humidity, @NonNull final String sensorName, @NonNull final HumidityUnit unit) {
        Log.i(TAG, String.format("onNewHumidity -> Received humidity %f%s from sensor %s on device %s.", humidity, unit, sensorName, device.getAddress()));
        final String mapKey = getMapKey(device, sensorName);
        final RHTValue value = mLastHumidityAndTemperature.get(mapKey);
        if (value == null) {
            mLastHumidityAndTemperature.put(mapKey, new RHTValue(null, humidity));
        } else {
            final Float temperatureInCelsius = value.temperatureInCelsius;
            if (temperatureInCelsius == null) {
                value.relativeHumidity = humidity;
            } else {
                final RHTDataPoint dataPoint = new RHTDataPoint(temperatureInCelsius, humidity, System.currentTimeMillis());
                notifyListeners(device, sensorName, dataPoint, false);
                mLastHumidityAndTemperature.remove(mapKey);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewTemperature(@NonNull final BleDevice device, final float temperature, @NonNull final String sensorName, @NonNull final TemperatureUnit unit) {
        Log.i(TAG, String.format("onNewTemperature -> Received temperature %f%s from sensor %s on device %s.", temperature, unit, sensorName, device.getAddress()));
        final String mapKey = getMapKey(device, sensorName);
        final RHTValue value = mLastHumidityAndTemperature.get(mapKey);
        if (value == null) {
            mLastHumidityAndTemperature.put(mapKey, new RHTValue(temperature, null));
        } else {
            final float temperatureInCelsius = convertTemperatureToCelsius(temperature, unit);
            if (value.relativeHumidity == null) {
                value.temperatureInCelsius = temperatureInCelsius;
            } else {
                final float relativeHumidity = value.relativeHumidity;
                final RHTDataPoint dataPoint = new RHTDataPoint(temperatureInCelsius, relativeHumidity, System.currentTimeMillis());
                notifyListeners(device, sensorName, dataPoint, false);
                mLastHumidityAndTemperature.remove(mapKey);
            }
        }
    }

    private void notifyListeners(@NonNull final BleDevice device, @NonNull final String sensorName, final RHTDataPoint datapoint, final boolean comesFromHistory) {
        Log.i(TAG, String.format("notifyListenersNewLiveValue -> Notifying relativeHumidity %f%%RH temperatureInCelsius %fºC from sensor %s in device %s.", datapoint.getRelativeHumidity(), datapoint.getTemperatureCelsius(), sensorName, device.getAddress()));
        final List<RHTListener> listeners = mListeners.get(device);
        if (listeners == null) {
            Log.e(TAG, String.format("notifyListenersNewLiveValue -> No listeners for device %s.", device));
            return;
        }
        final Iterator<RHTListener> iterator = listeners.iterator();
        while (iterator.hasNext()) {
            final RHTListener listener = iterator.next();
            try {
                if (comesFromHistory) {
                    listener.onNewHistoricalRHTValue(device, datapoint, sensorName);
                } else {
                    listener.onNewRHTValue(device, datapoint, sensorName);
                }
            } catch (final Exception e) {
                Log.e(TAG, String.format("notifyListenersNewLiveValue -> The following exception was thrown when informing listener %s -> ", listener), e);
                iterator.remove();
            }
        }
    }

    /**
     * Adds a download listener to the listener list.
     *
     * @param listener that wants to listener for notifications anymore.
     * @param device   that will send the RHT notifications.
     */
    void registerDownloadListener(@NonNull final RHTListener listener, @NonNull final BleDevice device) {
        synchronized (mListeners) {
            List<RHTListener> listenerList = mListeners.get(device);
            if (listenerList == null) {
                listenerList = new LinkedList<>();
                listenerList.add(listener);
                Log.i(TAG, String.format("registerNotificationListener -> Device %s received a new download listener: %s", device.getAddress(), listener));
                mListeners.put(device, listenerList);
            } else if (listenerList.contains(listener)) {
                Log.w(TAG, String.format("registerNotificationListener -> Listener %s was already in the list %s", device.getAddress(), listener));
            } else {
                listenerList.add(listener);
            }
        }
    }

    /**
     * Removes a download listener from the listener list.
     *
     * @param listener to unregister.
     * @param device   that will not send RHT notifications to the {@param listener} until it's registered again.
     */
    void unregisterDownloadListener(@NonNull final RHTListener listener, @NonNull final BleDevice device) {
        final List<RHTListener> listenerList = mListeners.get(device);
        if (listenerList == null) {
            Log.w(TAG, String.format("unregisterNotificationListener -> No listeners found for the device %s", device));
            return;
        }
        listenerList.remove(listener);
        if (listenerList.isEmpty()) {
            // avoid locking if not needed, but check again after locking
            synchronized (mListeners) {
                if (listenerList.isEmpty()) {
                    mListeners.remove(device);
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewHistoricalTemperature(@NonNull final BleDevice device, final float temperature, final long timestampMillis, @NonNull final String sensorName, @NonNull final TemperatureUnit unit) {
        Log.i(TAG, String.format("onNewHistoricalHumidity -> Received new historical relativeHumidity %f%s from sensor %s in device %s.", temperature, unit, sensorName, device.getAddress()));
        getDownloadDataManager(device, sensorName).newHistoricalTemperatureValue(timestampMillis, temperature);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onNewHistoricalHumidity(@NonNull final BleDevice device, final float relativeHumidity, final long timestampMilliseconds, @NonNull final String sensorName, @NonNull final HumidityUnit unit) {
        Log.i(TAG, String.format("onNewHistoricalHumidity -> Received new historical relativeHumidity %f%s from sensor %s in device %s.", relativeHumidity, unit, sensorName, device.getAddress()));
        getDownloadDataManager(device, sensorName).newHistoricalHumidityValue(timestampMilliseconds, relativeHumidity);
    }

    private HistoricalRHTValues getDownloadDataManager(@NonNull final BleDevice device, @NonNull final String sensorName) {
        final String mapKey = getMapKey(device, sensorName);
        HistoricalRHTValues downloadDataManager = mRHTDownloadManagers.get(mapKey);
        if (downloadDataManager == null) {
            downloadDataManager = new HistoricalRHTValues(device, sensorName);
            mRHTDownloadManagers.put(mapKey, downloadDataManager);
        }
        return downloadDataManager;
    }

    /**
     * Class used internally for controlling
     */
    private static class RHTValue {
        private Float temperatureInCelsius;
        private Float relativeHumidity;

        private RHTValue(@Nullable final Float temperatureInCelsius, @Nullable final Float relativeHumidity) {
            this.temperatureInCelsius = temperatureInCelsius;
            this.relativeHumidity = relativeHumidity;
        }
    }

    /**
     * Private class used for controlling the received RHT historical data.
     */
    private class HistoricalRHTValues {
        private final BleDevice device;
        private final String sensorName;

        private final LongSparseArray<RHTValue> mRHTValues = new LongSparseArray<>();

        private HistoricalRHTValues(@NonNull final BleDevice device, @NonNull final String sensorName) {
            this.device = device;
            this.sensorName = sensorName;
        }

        private void newHistoricalHumidityValue(final long timestamp, final float relativeHumidity) {
            RHTValue rhtValue = mRHTValues.get(timestamp);
            if (rhtValue == null) {
                rhtValue = new RHTValue(null, relativeHumidity);
                mRHTValues.put(timestamp, rhtValue);
            } else {
                rhtValue.relativeHumidity = relativeHumidity;
                if (rhtValue.temperatureInCelsius != null) {
                    final RHTDataPoint dataPoint = new RHTDataPoint(rhtValue.temperatureInCelsius, relativeHumidity, timestamp, TemperatureUnit.CELSIUS);
                    notifyListeners(this.device, this.sensorName, dataPoint, true);
                    mRHTValues.remove(timestamp);
                }
            }
        }

        private void newHistoricalTemperatureValue(final long timestamp, final float temperatureInCelsius) {
            RHTValue rhtValue = mRHTValues.get(timestamp);
            if (rhtValue == null) {
                rhtValue = new RHTValue(temperatureInCelsius, null);
                mRHTValues.put(timestamp, rhtValue);
            } else {
                if (rhtValue.relativeHumidity != null) {
                    final RHTDataPoint dataPoint = new RHTDataPoint(temperatureInCelsius, rhtValue.relativeHumidity, timestamp, TemperatureUnit.CELSIUS);
                    notifyListeners(this.device, this.sensorName, dataPoint, true);
                    mRHTValues.remove(timestamp);
                }
            }
        }
    }
}
