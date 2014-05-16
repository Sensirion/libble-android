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
 * Holds all the UI elements needed for showing the peripherals in range in a list
 */
public class PeripheralScanFragment extends ListFragment {
    private static final String TAG = PeripheralScanFragment.class.getSimpleName();

    private SectionedAdapter mSectionedAdapter;
    private ListItemAdapter mConnectedPeripheralsAdapter;
    private ListItemAdapter mDiscoveredPeripheralsAdapter;

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

    private BroadcastReceiver mPeripheralsChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(TAG, "mPeripheralsChangedReceiver.onReceive() -> refreshing discovered and connected list!");
            //FIXME: for future optimizations we might want to only change the updated peripherals, to make the UI more static for the user
            updateList();
        }
    };

    private void updateList() {
        //TODO: find a generic way for all activities that want to use this fragment
        Iterable<? extends BleDevice> connectedPeripherals = ((BleActivity) getActivity()).getConnectedBleDevices();
        Iterable<? extends BleDevice> discoveredPeripherals = ((BleActivity) getActivity()).getDiscoveredBleDevices();

        mConnectedPeripheralsAdapter.clear();
        mConnectedPeripheralsAdapter.addAll(connectedPeripherals);

        mDiscoveredPeripheralsAdapter.clear();
        mDiscoveredPeripheralsAdapter.addAll(discoveredPeripherals);

        setListAdapter(mSectionedAdapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        Log.i(TAG, "onCreateView()");
        View rootView = inflater.inflate(R.layout.fragment_peripheralscan, container, false);

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

        mConnectedPeripheralsAdapter = new ListItemAdapter();
        mDiscoveredPeripheralsAdapter = new ListItemAdapter();

        mSectionedAdapter.addSection(getActivity().getString(R.string.label_connected), mConnectedPeripheralsAdapter);
        mSectionedAdapter.addSection(getActivity().getString(R.string.label_discovered), mDiscoveredPeripheralsAdapter);

        setListAdapter(mSectionedAdapter);
    }

    private void initToggleButton(final View rootView) {
        final ToggleButton toggleButton = (ToggleButton) rootView.findViewById(R.id.togglebutton_scan);

        toggleButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //TODO: find a generic way for all activities that want to use this fragment
                    ((BleActivity) getActivity()).startScanning();
                    rootView.findViewById(R.id.progressbar_scanning).setVisibility(View.VISIBLE);
                } else {
                    //TODO: find a generic way for all activities that want to use this fragment
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
            //TODO: find a generic way for all activities that want to use this fragment
            ((BleActivity) getActivity()).onConnectedPeripheralSelected(device.getAddress());
        } else {
            //TODO: find a generic way for all activities that want to use this fragment
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

        IntentFilter filterPeripherals = new IntentFilter();
        filterPeripherals.addAction(BlePeripheralService.ACTION_PERIPHERAL_DISCOVERY);
        filterPeripherals.addAction(BlePeripheralService.ACTION_PERIPHERAL_CONNECTION_CHANGED);
        LocalBroadcastManager.getInstance(getActivity()).registerReceiver(mPeripheralsChangedReceiver,
                filterPeripherals);
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.i(TAG, "onPause() -> UNREGISTERING receivers from LocalBroadCastManager");

        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mScanStateReceiver);
        LocalBroadcastManager.getInstance(getActivity()).unregisterReceiver(mPeripheralsChangedReceiver);
    }

}
