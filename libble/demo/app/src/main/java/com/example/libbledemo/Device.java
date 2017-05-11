package com.example.libbledemo;


import android.bluetooth.le.ScanResult;

class Device {
    private final String mAddress;
    private String mDeviceName;
    private int mRssi;

    Device(ScanResult result) {
        mAddress = result.getDevice().getAddress();
        mDeviceName = result.getDevice().getName();
        mRssi = result.getRssi();
    }

    String getAddress() {
        return mAddress;
    }

    String getName() {
        return mDeviceName;
    }

    int getRssi() {
        return mRssi;
    }

    void setRssi(int rssi) {
        mRssi = rssi;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Device && mAddress.equals(((Device) o).getAddress());
    }
}
