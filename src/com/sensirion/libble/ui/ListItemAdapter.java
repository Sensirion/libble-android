package com.sensirion.libble.ui;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sensirion.libble.BleDevice;
import com.sensirion.libble.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Represents a discovered or connected peripheral in a ListView
 */
public class ListItemAdapter extends BaseAdapter {

    private List<BleDevice> mBleDevices;

    private Comparator<BleDevice> mRssiComparator = new Comparator<BleDevice>() {
        public int compare(BleDevice device1, BleDevice device2) {
            return device2.getRSSI() - device1.getRSSI();
        }
    };

    public ListItemAdapter() {
        mBleDevices = Collections.synchronizedList(new ArrayList<BleDevice>());
    }

    @Override
    public int getCount() {
        return mBleDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mBleDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View view;
        if (convertView == null) {
            view = View.inflate(parent.getContext(), R.layout.listitem_scan_result, null);
        } else {
            view = convertView;
        }

        BleDevice bleDevice = mBleDevices.get(position);

        TextView advertisedName = (TextView) view.findViewById(R.id.listitem_advertised_name);
        advertisedName.setText(bleDevice.getAdvertisedName());

        TextView deviceAddress = (TextView) view.findViewById(R.id.device_address);
        deviceAddress.setText(bleDevice.getAddress());

        TextView rssi = (TextView) view.findViewById(R.id.listitem_value_rssi);
        rssi.setText(Integer.toString(bleDevice.getRSSI()));

        return view;
    }

    public synchronized void clear() {
        mBleDevices.clear();
        notifyDataSetChanged();
    }

    public synchronized void addAll(Iterable<? extends BleDevice> devices) {
        for (BleDevice device : devices) {
            if (mBleDevices.contains(device)) {
                continue;
            }
            mBleDevices.add(device);
        }

        Collections.sort(mBleDevices, mRssiComparator);
        notifyDataSetChanged();
    }

}
