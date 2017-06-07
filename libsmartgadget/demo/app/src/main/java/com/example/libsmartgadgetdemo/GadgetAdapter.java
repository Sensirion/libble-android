package com.example.libsmartgadgetdemo;


import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import com.sensirion.libsmartgadget.Gadget;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

class GadgetAdapter extends BaseAdapter {
    private final static Comparator<GadgetModel> RSSI_COMPARATOR = new Comparator<GadgetModel>() {
        @Override
        public int compare(GadgetModel g1, GadgetModel g2) {
            if (g1.getRssi() == g2.getRssi()) {
                return g1.getGadget().getAddress().compareTo(g2.getGadget().getAddress());
            }
            return g2.getRssi() - g1.getRssi();
        }
    };

    private final boolean mConnected;
    private final MainActivity mSub;
    private final List<GadgetModel> mGadgets;

    GadgetAdapter(MainActivity activity, boolean connected) {
        mGadgets = Collections.synchronizedList(new ArrayList<GadgetModel>());
        mConnected = connected;
        mSub = activity;
    }

    @Override
    public int getCount() {
        return mGadgets.size();
    }

    @Override
    public Object getItem(int position) {
        return mGadgets.get(position);
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

            final GadgetModel model = (GadgetModel) getItem(position);
            if (model == null) {
                return view;
            }

            Button btn = (Button) view.findViewById(R.id.connection_button);
            btn.setTag(model.getGadget());
            btn.setEnabled(true);

            Button infobtn = (Button) view.findViewById(R.id.info_button);
            infobtn.setTag(model.getGadget());

            TextView device_name_view = (TextView) view.findViewById(R.id.device_name);
            device_name_view.setText(model.getGadget().getName());

            TextView device_address_view = (TextView) view.findViewById(R.id.device_address);
            device_address_view.setText(model.getGadget().getAddress());

            TextView device_rssi_view = (TextView) view.findViewById(R.id.device_rssi);

            if (mConnected) {
                device_rssi_view.setText("");
                btn.setText("DC");
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSub.onDisconnectButtonClick(v);
                    }
                });

                infobtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSub.onInfoButtonClick(v);
                    }
                });
            } else {
                device_rssi_view.setText(String.format(Locale.ENGLISH, " (RSSI: %d)", model.getRssi()));
                btn.setText("Connect");
                btn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        mSub.onConnectButtonClick(v);
                    }
                });

                infobtn.setVisibility(View.GONE);
            }

            return view;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    void add(Gadget new_gadget, int rssi) {
        GadgetModel new_model = new GadgetModel(new_gadget, rssi);
        synchronized (mGadgets) {
            for (GadgetModel model : mGadgets) {
                if (new_model.equals(model)) {
                    model.setRssi(rssi);
                    return;
                }
            }
            mGadgets.add(new_model);
            sort();
            notifyDataSetChanged();
        }
    }

    Gadget getFromAddress(String address) {
        synchronized (mGadgets) {
            for (GadgetModel model : mGadgets) {
                if (model.getGadget().getAddress().equals(address)) {
                    return model.getGadget();
                }
            }
        }
        return null;
    }

    void remove(Gadget gadget) {
        synchronized (mGadgets) {
            for (GadgetModel model : mGadgets) {
                if (model.getGadget().getAddress().equals(gadget.getAddress())) {
                    mGadgets.remove(model);
                    notifyDataSetChanged();
                    return;
                }
            }
        }
    }

    void clear() {
        mGadgets.clear();
        notifyDataSetChanged();
    }

    void disconectAllGadgets() {
        synchronized (mGadgets) {
            for (GadgetModel model : mGadgets) {
                model.getGadget().disconnect();
            }
        }
    }

    private void sort() {
        Collections.sort(mGadgets, RSSI_COMPARATOR);
    }
}
