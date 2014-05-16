package com.sensirion.libble.ui;


import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.sensirion.libble.BleDevice;
import com.sensirion.libble.PeripheralServiceBattery;
import com.sensirion.libble.R;

public class PeripheralDetailsFragment extends Fragment {
    private static final String TAG = PeripheralDetailsFragment.class.getSimpleName();
    private static final String PREFIX = PeripheralDetailsFragment.class.getName();

    public static final String ARGUMENT_PERIPHERAL_ADDRESS = PREFIX + "/ARGUMENT_PERIPHERAL_ADDRESS";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View rootView = inflater.inflate(R.layout.fragment_peripheraldetails, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        String deviceAddress = getArguments().getString(ARGUMENT_PERIPHERAL_ADDRESS);
        Toast.makeText(getActivity(), "showing peripheral details for: " + deviceAddress, Toast.LENGTH_SHORT).show();

        BleDevice device = ((BleActivity) getActivity()).getConnectedDevice(deviceAddress);
        PeripheralServiceBattery batteryService = device.getPeripheralService(PeripheralServiceBattery.class);
        int batteryLevel = -42;
        if (batteryService != null) {
            batteryLevel = batteryService.getBatteryLevel();
        }
        ((TextView) view.findViewById(R.id.tv_batterylevel)).setText(batteryLevel + "%");
    }

    //TODO: add button to disconnect
}
