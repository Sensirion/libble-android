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
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.sensirion.libble.BleDevice;
import com.sensirion.libble.BlePeripheralService;
import com.sensirion.libble.R;

/**
 * Holds all the UI elements needed for showing the devices in range in a list
 */
public class DeviceScanFragment extends ListFragment {
    private static final String TAG = DeviceScanFragment.class.getSimpleName();

    private SectionedAdapter mSectionedAdapter;
    private ListItemAdapter mConnectedDevicesAdapter;
    private ListItemAdapter mDiscoveredDevicesAdapter;

    private BroadcastReceiver mScanStateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final ToggleButton b = (ToggleButton) getView().findViewById(R.id.togglebutton_scan);

            if (intent.getAction().equals(BlePeripheralService.ACTION_SCANNING_STARTED)) {
                Log.i(TAG, "mScanStateReceiver.onReceive() -> scanning STARTED!");
                b.setChecked(true);
            } else if (intent.getAction().equals(BlePeripheralService.ACTION_SCANNING_STOPPED)) {
                Log.i(TAG, "mScanStateReceiver.onReceive() -> scanning STOPPED!");
                b.setChecked(false);
            }
        }
    };

    private BroadcastReceiver mDeviceConnectionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "mDeviceConnectionReceiver.onReceive() -> refreshing discovered and connected list!");
            //TODO: for future optimizations we might want to only change the updated devices, to make the UI more static for the user
            updateList();
        }
    };

    private void updateList() {
        //FIXME: find a generic way for all activities that want to use this fragment
        Iterable<? extends BleDevice> connectedDevices = ((BleActivity) getActivity()).getConnectedBleDevices();
        Iterable<? extends BleDevice> discoveredDevices = ((BleActivity) getActivity()).getDiscoveredBleDevices();

        mConnectedDevicesAdapter.clear();
        mConnectedDevicesAdapter.addAll(connectedDevices);

        mDiscoveredDevicesAdapter.clear();
        mDiscoveredDevicesAdapter.addAll(discoveredDevices);

        setListAdapter(mSectionedAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View rootView = inflater.inflate(R.layout.fragment_device_scan, container, false);

        initListAdapter();
        initToggleButton(rootView);

        return rootView;
    }

    private void initListAdapter() {
        mSectionedAdapter = new SectionedAdapter() {
            @Override
            protected View getHeaderView(String caption, int index, View convertView, ViewGroup parent) {
                TextView result = (TextView) convertView;

                if (convertView == null) {
                    result = (TextView) View.inflate(getActivity(), R.layout.listitem_header, null);
                }

                result.setText(caption);
                return result;
            }
        };

        mConnectedDevicesAdapter = new ListItemAdapter();
        mDiscoveredDevicesAdapter = new ListItemAdapter();

        mSectionedAdapter.addSection(getActivity().getString(R.string.label_connected), mConnectedDevicesAdapter);
        mSectionedAdapter.addSection(getActivity().getString(R.string.label_discovered), mDiscoveredDevicesAdapter);

        setListAdapter(mSectionedAdapter);
    }

    private void initToggleButton(final View rootView) {
        final ToggleButton toggleButton = (ToggleButton) rootView.findViewById(R.id.togglebutton_scan);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //FIXME: find a generic way for all activities that want to use this fragment
                    ((BleActivity) getActivity()).startScanning();
                    rootView.findViewById(R.id.progressbar_scanning).setVisibility(View.VISIBLE);
                } else {
                    //FIXME: find a generic way for all activities that want to use this fragment
                    ((BleActivity) getActivity()).stopScanning();
                    rootView.findViewById(R.id.progressbar_scanning).setVisibility(View.INVISIBLE);
                }
            }
        });

        // start scanning immediately when fragment is active
        toggleButton.performClick();
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);

        BleDevice device = (BleDevice) mSectionedAdapter.getItem(position);

        if (device.isConnected()) {
            //FIXME: find a generic way for all activities that want to use this fragment
            ((BleActivity) getActivity()).onConnectedPeripheralSelected(device.getAddress());
        } else {
            //FIXME: find a generic way for all activities that want to use this fragment
            ((BleActivity) getActivity()).onDiscoveredPeripheralSelected(device.getAddress());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.i(TAG, "onResume() -> REGISTERING receivers to LocalBroadCastManager");
        updateList();

        IntentFilter filterScanState = new IntentFilter();
        filterScanState.addAction(BlePeripheralService.ACTION_SCANNING_STARTED);
        filterScanState.addAction(BlePeripheralService.ACTION_SCANNING_STOPPED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mScanStateReceiver,
                filterScanState);

        IntentFilter filterDeviceState = new IntentFilter();
        filterDeviceState.addAction(BlePeripheralService.ACTION_PERIPHERAL_DISCOVERY);
        filterDeviceState.addAction(BlePeripheralService.ACTION_PERIPHERAL_CONNECTION_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mDeviceConnectionReceiver,
                filterDeviceState);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause() -> UNREGISTERING receivers from LocalBroadCastManager");

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mScanStateReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mDeviceConnectionReceiver);
    }

}
