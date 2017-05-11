package com.example.libbledemo;

import android.bluetooth.le.ScanResult;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

class DeviceAdapter extends BaseAdapter {

    private static final Comparator<Device> RSSI_COMPARATOR = new Comparator<Device>() {
        @Override
        public int compare(Device d1, Device d2) {
            if (d1.getRssi() == d2.getRssi()) {
                return d1.getAddress().compareTo(d2.getAddress());
            }
            return d2.getRssi() - d1.getRssi();
        }
    };

    private final boolean mConnected;
    private final MainActivity mSub;
    private final List<Device> mDevices;

    DeviceAdapter(MainActivity ma, boolean connected) {
        mDevices = Collections.synchronizedList(new ArrayList<Device>());
        mConnected = connected;
        mSub = ma;
    }

    @Override
    public int getCount() {
        return mDevices.size();
    }

    @Override
    public Object getItem(int position) {
        return mDevices.get(position);
    }

    Object getItem(String device_address) {
        synchronized (mDevices) {
            for (Device device : mDevices) {
                if (device.getAddress().equals(device_address)) {
                    return device;
                }
            }
            return null;
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {
        try {
            if (view == null) {
                view = View.inflate(parent.getContext(), R.layout.list_item, null);
            }

            Device result = (Device) getItem(position);
            if (result == null) {
                return view;
            }

            Button btn = (Button) view.findViewById(R.id.connection_button);
            btn.setTag(result.getAddress());
            btn.setEnabled(true);

            TextView device_name_view = (TextView) view.findViewById(R.id.device_name);
            device_name_view.setText(result.getName());

            TextView device_address_view = (TextView) view.findViewById(R.id.device_address);
            device_address_view.setText(result.getAddress());

            TextView device_rssi_view = (TextView) view.findViewById(R.id.device_rssi);

            if (mConnected) {
                device_rssi_view.setText("");
                btn.setText(R.string.device_button_disconnect);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSub.onDisconnectButtonPressed(v);
                    }
                });
            } else {
                device_rssi_view.setText(String.format(Locale.ENGLISH, " (RSSI: %d)", result.getRssi()));
                btn.setText(R.string.device_button_connect);
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSub.onConnectButtonPressed(v);
                    }
                });
            }

            return view;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void add(ScanResult result) {
        Device new_device = new Device(result);
        add(new_device);

    }

    void add(Device new_device) {
        synchronized (mDevices) {
            for (Device device : mDevices) {
                if (new_device.equals(device)) {
                    device.setRssi(new_device.getRssi());
                    notifyDataSetChanged();
                    return;
                }
            }
            mDevices.add(new_device);
            sort();
            notifyDataSetChanged();
        }
    }

    void clear() {
        mDevices.clear();
        notifyDataSetChanged();
    }

    void remove(String device_address) {
        synchronized (mDevices) {
            for (Device device : mDevices) {
                if (device.getAddress().equals(device_address)) {
                    mDevices.remove(device);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    private void sort() {
        Collections.sort(mDevices, RSSI_COMPARATOR);
    }
}
