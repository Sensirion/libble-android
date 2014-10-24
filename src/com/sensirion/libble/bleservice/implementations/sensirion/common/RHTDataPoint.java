package com.sensirion.libble.bleservice.implementations.sensirion.common;

/**
 * Convenience class for storing the obtained data points.
 */
public class RHTDataPoint {

    private final String mDeviceAddress;
    private final float mHumidity;
    private final float mTemperature;
    private final long mTimestampMs;
    private final boolean mComesFromLog;

    public RHTDataPoint(final String address, final float humidity, final float temperature, final int epochTime){
        this(address, humidity, temperature, epochTime, false);
    }

    public RHTDataPoint(final String address, final float humidity, final float temperature, final int epochTime, final boolean comesFromLog) {
        this(address, humidity, temperature, epochTime * 1000l, comesFromLog);
    }

    public RHTDataPoint(final String address, final float humidity, final float temperature, final long timestampMs){
        this(address, humidity, temperature, timestampMs, false);
    }

    public RHTDataPoint(final String address, final float humidity, final float temperature, final long timestampMs, final boolean comesFromLog) {
        mDeviceAddress = address;
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
     * Obtains the address of the device.
     *
     * @return {@link java.lang.String} with the device address - <code>null</code> in case it's not known.
     */
    public String getDeviceAddress() {
        return mDeviceAddress;
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

    /**
     * Returns if the data come from device notifications.
     *
     * @return <code>true</code> in case the data came from notifications - <code>false</code> otherwise.
     */
    public boolean getComesFromNotifications() {
        return !getComesFromLog();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
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