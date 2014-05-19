package com.sensirion.libble.ui;


import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sensirion.libble.BleDevice;
import com.sensirion.libble.BlePeripheralService;
import com.sensirion.libble.PeripheralServiceBattery;
import com.sensirion.libble.R;

public class PeripheralDetailsFragment extends Fragment {
    private static final String TAG = PeripheralDetailsFragment.class.getSimpleName();
    private static final String PREFIX = PeripheralDetailsFragment.class.getName();

    public static final String ARGUMENT_PERIPHERAL_ADDRESS = PREFIX + "/ARGUMENT_PERIPHERAL_ADDRESS";

    private String mShownDeviceAddress;

    private BroadcastReceiver mPeripheralsChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "mPeripheralsChangedReceiver.onReceive() -> updating details.");
            final String changedDeviceAddress = intent.getStringExtra(BlePeripheralService.EXTRA_PERIPHERAL_ADDRESS);

            if (changedDeviceAddress.equals(mShownDeviceAddress)) {
                //TODO: find a generic way for all activities that want to use this fragment
                if (((BleActivity) getActivity()).getConnectedDevice(mShownDeviceAddress) == null) {
                    Toast.makeText(context, context.getString(R.string.text_disconnected) + changedDeviceAddress, Toast.LENGTH_SHORT).show();
                    getFragmentManager().popBackStack();
                } else {
                    Toast.makeText(context, context.getString(R.string.text_connected) + changedDeviceAddress, Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View rootView = inflater.inflate(R.layout.fragment_peripheraldetails, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mShownDeviceAddress = getArguments().getString(ARGUMENT_PERIPHERAL_ADDRESS);
        Toast.makeText(getActivity(), "showing peripheral details for: " + mShownDeviceAddress, Toast.LENGTH_SHORT).show();

        //TODO: find a generic way for all activities that want to use this fragment
        BleDevice device = ((BleActivity) getActivity()).getConnectedDevice(mShownDeviceAddress);

        if(device == null) {
            throw new IllegalArgumentException("No connected peripheral for address: " + mShownDeviceAddress);
        }

        initPeripheralInfo(view, device);
        initBatteryLevelView(view, device);
        initDisconnectButton(view, device);
    }

    private void initPeripheralInfo(View view, BleDevice device) {
        ((TextView) view.findViewById(R.id.tv_peripheral_advertised_name)).setText(device.getAdvertisedName());
        ((TextView) view.findViewById(R.id.tv_peripheral_address)).setText(device.getAddress());
    }

    private void initBatteryLevelView(View view, BleDevice device) {
        PeripheralServiceBattery batteryService = device.getPeripheralService(PeripheralServiceBattery.class);

        int batteryLevel = -42;
        if (batteryService != null) {
            batteryLevel = batteryService.getBatteryLevel();
        }

        ((TextView) view.findViewById(R.id.tv_batterylevel)).setText(batteryLevel + "%");
    }

    private void initDisconnectButton(View view, final BleDevice device) {
        Button disconnectButton = (Button) view.findViewById(R.id.button_disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: find a generic way for all activities that want to use this fragment
                ((BleActivity) getActivity()).disconnectPeripheral(device.getAddress());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() -> REGISTERING receivers to LocalBroadCastManager");

        IntentFilter filterPeripherals = new IntentFilter();
        filterPeripherals.addAction(BlePeripheralService.ACTION_PERIPHERAL_CONNECTION_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mPeripheralsChangedReceiver,
                filterPeripherals);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause() -> UNREGISTERING receivers from LocalBroadCastManager");

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mPeripheralsChangedReceiver);
    }
}
