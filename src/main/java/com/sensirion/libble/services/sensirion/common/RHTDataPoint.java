package com.sensirion.libble.services.sensirion.common;

/**
 * Convenience class for storing the obtained data points.
 */
public class RHTDataPoint implements Comparable<RHTDataPoint> {

    private final float mTemperature;
    private final float mHumidity;
    private final long mTimestampMs;
    private final boolean mIsFromHistory;

    public RHTDataPoint(final float temperature, final float humidity, final int epochTime) {
        this(humidity, temperature, epochTime, false);
    }

    public RHTDataPoint(final float temperature, final float humidity, final int epochTime, final boolean isFromHistory) {
        this(humidity, temperature, epochTime * 1000l, isFromHistory);
    }

    public RHTDataPoint(final float temperature, final float humidity, final long timestampMs) {
        this(temperature, humidity, timestampMs, false);
    }

    public RHTDataPoint(final float temperature, final float humidity, final long timestampMs, final boolean isFromHistory) {
        mTemperature = temperature;
        mHumidity = humidity;
        mTimestampMs = timestampMs;
        mIsFromHistory = isFromHistory;
    }

    public float getHumidity() {
        return mHumidity;
    }

    public float getTemperature() {
        return mTemperature;
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
    public boolean getIsFromHistory() {
        return mIsFromHistory;
    }

    @Override
    @SuppressWarnings("StringBufferReplaceableByString")
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Temperature: ").append(getTemperature());
        sb.append(" Relative Humidity: ").append(getHumidity());
        sb.append(" TimestampMs: ").append(getTimestamp());
        sb.append(" Comes from log: ").append(getIsFromHistory());
        sb.append(" Seconds from now: ").append((int) ((System.currentTimeMillis() - getTimestamp()) / 1000l)).append(" second(s).");
        return sb.toString();
    }

    @Override
    @SuppressWarnings("NullableProblems")
    public int compareTo(final RHTDataPoint anotherDatapoint) {
        if (anotherDatapoint.getTimestamp() - mTimestampMs > 0) {
            return -1;
        }
        return 1;
    }
}