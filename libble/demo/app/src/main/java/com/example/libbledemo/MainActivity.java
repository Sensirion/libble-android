package com.example.libbledemo;

import android.Manifest;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.sensirion.libble.BleScanCallback;
import com.sensirion.libble.BleService;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int SCAN_DURATION_MS = 60_000;
    private static final int BUTTON_RESET_DELAY_MS = 2000;
    private static final int LOCATION_PERMISSION = 0;

    private Button mButton;

    private DeviceAdapter mDiscoveredDevicesAdapter;
    private ListView mDiscoveredDevicesView;

    private DeviceAdapter mConnectedDevicesAdapter;
    private ListView mConnectedDevicesView;

    private BleService mBleService;
    private BleScanCallback mScanCallback;
    private BroadcastReceiver mBroadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mButton = (Button) findViewById(R.id.scan_button);

        mDiscoveredDevicesView = (ListView) findViewById(R.id.device_list);
        mDiscoveredDevicesAdapter = new DeviceAdapter(this, false);
        mDiscoveredDevicesView.setAdapter(mDiscoveredDevicesAdapter);

        mConnectedDevicesView = (ListView) findViewById(R.id.connected_list);
        mConnectedDevicesAdapter = new DeviceAdapter(this, true);
        mConnectedDevicesView.setAdapter(mConnectedDevicesAdapter);

        mBroadcastReceiver = new LibBleBroadcastReceiver(this);

        mScanCallback = new LibBleScanCallback(this);
        Intent intent = new Intent(this, BleService.class);
        ServiceConnection connection = new LibBleServiceConnection(this);
        if (!bindService(intent, connection, Context.BIND_AUTO_CREATE)) {
            throw new IllegalStateException("bindService not successful");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BleService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(BleService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(BleService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(BleService.ACTION_DID_WRITE_CHARACTERISTIC);
        intentFilter.addAction(BleService.ACTION_DID_FAIL);
        registerReceiver(mBroadcastReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mBroadcastReceiver);
    }

    public void startDiscovery(View view) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_PERMISSION);
            return;
        }
        if (mButton != null) {
            mButton.setText(R.string.button_scan_started);
            mButton.setEnabled(false);
            clearDiscoveredList();
            mBleService.startScan(mScanCallback, SCAN_DURATION_MS, null, null);
        } else {
            throw new IllegalStateException("Button can't be null");
        }
    }

    public void clearDiscoveredList() {
        mDiscoveredDevicesAdapter.clear();
        rescaleListView(mDiscoveredDevicesView);
    }

    public void clearConnectedList() {
        for (String address : mBleService.getConnectedDevices()) {
            mBleService.disconnect(address);
        }
    }

    public void clearLists(View view) {
        clearLists();
    }

    public void clearLists() {
        clearDiscoveredList();
        clearConnectedList();
    }

    private void rescaleListView(ListView listView) {
        /*
         * Code found here: http://www.java2s.com/Code/Android/UI/setListViewHeightBasedOnChildren.htm
         */
        ListAdapter listAdapter = listView.getAdapter();
        if (listAdapter == null) {
            return;
        }

        int totalHeight = 0;
        for (int i = 0; i < listAdapter.getCount(); i++) {
            View listItem = listAdapter.getView(0, null, listView);
            listItem.measure(0, 0);
            totalHeight += listItem.getMeasuredHeight();
        }

        ViewGroup.LayoutParams params = listView.getLayoutParams();
        params.height = totalHeight + (listView.getDividerHeight() * (listAdapter.getCount() - 1));
        listView.setLayoutParams(params);
        listView.requestLayout();
    }

    private void rescaleListViews() {
        rescaleListView(mConnectedDevicesView);
        rescaleListView(mDiscoveredDevicesView);
    }

    /*
     * Connection Callbacks
     */

    public void onConnectButtonPressed(View view) {
        if (view instanceof Button) {
            Button btn = (Button) view;
            btn.setEnabled(false);
            btn.setText(R.string.device_button_connecting);

            String address = (String) btn.getTag();

            if (!mBleService.connect(address)) {
                Toast.makeText(this, String.format(Locale.ENGLISH, getString(R.string.connection_failed), address), Toast.LENGTH_SHORT).show();
                btn.setText(R.string.device_button_connection_failed);
            }
        }
    }

    public void onDisconnectButtonPressed(View view) {
        if (view instanceof Button) {
            Button btn = (Button) view;
            btn.setEnabled(false);
            btn.setText(R.string.device_button_disconnecting);

            String address = (String) btn.getTag();
            mBleService.disconnect(address);
        }
    }

    public void onConnection(String device_address) {
        Toast.makeText(this, String.format(Locale.ENGLISH, getString(R.string.connection_success), device_address), Toast.LENGTH_SHORT).show();
        Device connected_device = (Device) mDiscoveredDevicesAdapter.getItem(device_address);
        mDiscoveredDevicesAdapter.remove(device_address);
        mConnectedDevicesAdapter.add(connected_device);
        rescaleListViews();
    }

    public void onDisconnection(String device_address) {
        Toast.makeText(this, String.format(Locale.ENGLISH, getString(R.string.connection_lost), device_address), Toast.LENGTH_SHORT).show();
        mConnectedDevicesAdapter.remove(device_address);
        mDiscoveredDevicesAdapter.remove(device_address);
        rescaleListViews();
    }

    /*
     * Scan Callbacks
     */

    public void onScanResult(ScanResult result) {
        mDiscoveredDevicesAdapter.add(result);
        rescaleListView(mDiscoveredDevicesView);
    }

    public void onScanStopped() {
        mButton.setText(R.string.button_scan_stopped);
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                mButton.setText(R.string.button_service_connected);
                mButton.setEnabled(true);
            }
        };
        mButton.postDelayed(runnable, BUTTON_RESET_DELAY_MS);
    }

    public void onScanFailed(int errorCode) {
        mButton.setEnabled(false);
        mButton.setText(String.format(Locale.ENGLISH, getString(R.string.button_scan_failed), errorCode));
    }

    /*
     * Service Callbacks
     */

    public void onServiceConnected(BleService service) {
        mBleService = service;
        mButton.setText(R.string.button_service_connected);
        mButton.setEnabled(true);
    }

    public void onServiceDisconnected() {
        mBleService = null;
        mButton.setText(R.string.button_service_disconnected);
        mButton.setEnabled(false);
        clearLists();
    }
}
