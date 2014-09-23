package com.sensirion.libble.bleservice.implementations.humigadget;

import com.sensirion.libble.BleDevice;

/**
 * Convenience class for storing the obtained data points.
 */
public class RHTDataPoint {
    private final BleDevice mDevice;
    private final float mHumidity;
    private final float mTemperature;
    private final long mTimestampMs;
    private final boolean mComesFromLog;

    public RHTDataPoint(final BleDevice device, final float humidity, final float temperature, final int epochTime, final boolean comesFromLog) {
        this(device, humidity, temperature, epochTime * 1000l, comesFromLog);
    }

    public RHTDataPoint(final BleDevice device, final float humidity, final float temperature, final long timestampMs, final boolean comesFromLog) {
        mDevice = device;
        mHumidity = humidity;
        mTemperature = temperature;
        mTimestampMs = timestampMs;
        mComesFromLog = comesFromLog;
    }

    public float getHumidity() {
        return mHumidity;
    }

    public float getTemperature() {
        return mTemperature;
    }

    /**
     * Returns the mDevice of the datapoint.
     *
     * @return {@link com.sensirion.libble.BleDevice} of the mDevice that send the datapoint.
     */
    public BleDevice getDevice() {
        return mDevice;
    }

    /**
     * Returns the moment when it was obtained the data point in seconds.
     *
     * @return the epoch time in seconds.
     */
    public int getEpochTime() {
        return (int) (getTimestamp() / 1000l);
    }

    /**
     * Returns the moment when it was obtained the data point in milliseconds.
     *
     * @return the timestamp in milliseconds.
     */
    public long getTimestamp() {
        return mTimestampMs;
    }

    /**
     * Returns if this data came from logging.
     *
     * @return <code>true</code> in case the data came from logging - <code>false</code> otherwise.
     */
    public boolean getComesFromLog() {
        return mComesFromLog;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Relative Humidity: ").append(getHumidity());
        sb.append(" Temperature: ").append(getTemperature());
        if (getComesFromLog()) {
            sb.append(" Epoch Time: ").append(getEpochTime());
        } else {
            sb.append(" TimestampMs: ").append(getTimestamp());
        }
        sb.append(" Comes from log: ").append(getComesFromLog());
        sb.append(" Seconds from now: ").append((int) ((System.currentTimeMillis() - getTimestamp()) / 1000l)).append(" second(s).");
        return sb.toString();
    }
}