
package com.sensirion.libble.peripheral.sensor;

public interface PeriodicalSensorI {

    public int getMinPeriod();

    public int getMaxPeriod();

    public void setPeriod(int period);

    public int getPeriod();
}
