package com.sensirion.libble.old.peripheral.sensor;

public interface PeriodicalSensorI {

    public int getMinPeriod();

    public int getMaxPeriod();

    public int getPeriod();

    public void setPeriod(int period);
}
