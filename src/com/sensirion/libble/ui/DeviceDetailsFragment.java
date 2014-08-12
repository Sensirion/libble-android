package com.sensirion.libble.ui;


import android.app.ListFragment;
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
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.sensirion.libble.ui.BleActivity;
import com.sensirion.libble.bleservice.impl.BatteryPeripheralService;
import com.sensirion.libble.BleDevice;
import com.sensirion.libble.BlePeripheralService;
import com.sensirion.libble.R;

import java.util.ArrayList;

public class DeviceDetailsFragment extends ListFragment {
    private static final String TAG = DeviceDetailsFragment.class.getSimpleName();
    private static final String PREFIX = DeviceDetailsFragment.class.getName();

    public static final String ARGUMENT_DEVICE_ADDRESS = PREFIX + "/ARGUMENT_DEVICE_ADDRESS";

    private String mSelectedDeviceAddress;

    private BroadcastReceiver mDeviceConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String changedDeviceAddress = intent.getStringExtra(BlePeripheralService.EXTRA_PERIPHERAL_ADDRESS);

            if (changedDeviceAddress.equals(mSelectedDeviceAddress)) {
                Log.i(TAG, "mDeviceConnectionReceiver.onReceive() for selected device: " + changedDeviceAddress);
                //FIXME: find a generic way for all activities that want to use this fragment
                if (((BleActivity) getActivity()).getConnectedDevice(mSelectedDeviceAddress) == null) {
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
        Log.i(TAG, "onCreateView() -> inflate layout");
        View rootView = inflater.inflate(R.layout.fragment_device_details, container, false);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Log.i(TAG, "onViewCreated() -> init view");

        mSelectedDeviceAddress = getArguments().getString(ARGUMENT_DEVICE_ADDRESS);

        //FIXME: find a generic way for all activities that want to use this fragment
        BleDevice device = ((BleActivity) getActivity()).getConnectedDevice(mSelectedDeviceAddress);

        if (device == null) {
            throw new IllegalArgumentException("No connected device for address: " + mSelectedDeviceAddress);
        }

        initDeviceInfo(view, device);
        initBatteryLevelView(view, device);
        initServiceList(view, device);
        initDisconnectButton(view, device);
    }

    private void initDeviceInfo(View view, BleDevice device) {
        ((TextView) view.findViewById(R.id.tv_device_advertised_name)).setText(device.getAdvertisedName());
        ((TextView) view.findViewById(R.id.tv_device_address)).setText(device.getAddress());
    }

    private void initBatteryLevelView(View view, BleDevice device) {
        BatteryPeripheralService batteryService = device.getPeripheralService(BatteryPeripheralService.class);

        if (batteryService == null) {
            ((TextView) view.findViewById(R.id.tv_batterylevel)).setText(getString(R.string.peripheralservice_not_available));
        } else {
            int batteryLevel = batteryService.getBatteryLevel();
            ((TextView) view.findViewById(R.id.tv_batterylevel)).setText(batteryLevel + getString(R.string.char_percent));
        }
    }

    private void initServiceList(View view, BleDevice device) {
        ArrayList<String> services = new ArrayList<String>();
        for(String s : device.getDiscoveredPeripheralServices()) {
            services.add(s);
        }

        ArrayAdapter<String> serviceAdapter = new ArrayAdapter<String>(view.getContext(),
                android.R.layout.simple_list_item_1,
                services);

        setListAdapter(serviceAdapter);
    }

    private void initDisconnectButton(View view, final BleDevice device) {
        Button disconnectButton = (Button) view.findViewById(R.id.button_disconnect);
        disconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //FIXME: find a generic way for all activities that want to use this fragment
                ((BleActivity) getActivity()).disconnectPeripheral(device.getAddress());
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() -> REGISTERING receivers to LocalBroadCastManager");

        IntentFilter filter = new IntentFilter();
        filter.addAction(BlePeripheralService.ACTION_PERIPHERAL_CONNECTION_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDeviceConnectionReceiver, filter);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause() -> UNREGISTERING receivers from LocalBroadCastManager");
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDeviceConnectionReceiver);
    }
}
