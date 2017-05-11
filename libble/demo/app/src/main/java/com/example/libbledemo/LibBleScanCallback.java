package com.example.libbledemo;

import android.bluetooth.le.ScanResult;

import com.sensirion.libble.BleScanCallback;

import java.util.List;

class LibBleScanCallback extends BleScanCallback {

    private final MainActivity mCallback;

    LibBleScanCallback(MainActivity activity) {
        mCallback = activity;
    }

    @Override
    public void onScanResult(int callbackType, ScanResult result) {
        mCallback.onScanResult(result);
    }

    @Override
    public void onBatchScanResults(List<ScanResult> results) {
        for (ScanResult res : results) {
            mCallback.onScanResult(res);
        }
    }

    @Override
    public void onScanStopped() {
        mCallback.onScanStopped();
    }

    @Override
    public void onScanFailed(int errorCode) {
        mCallback.onScanFailed(errorCode);
    }

}
