package com.sensirion.libble.ui;

import android.database.DataSetObserver;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.BaseAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Taken from http://blogingtutorials.blogspot.ch/2010/11/android-listview-header-two-or-more-in.html
 */
abstract public class SectionedAdapter extends BaseAdapter {

    private static final String TAG = SectionedAdapter.class.getSimpleName();
    private static final int TYPE_SECTION_HEADER = 1;

    private class Section extends DataSetObserver {
        String caption;
        Adapter adapter;

        Section(String caption, Adapter adapter) {
            this.caption = caption;
            this.adapter = adapter;
            adapter.registerDataSetObserver(this);
        }

        @Override
        public void onChanged() {
            Log.v(Section.class.getSimpleName(), "onChanged() -> SectionedAdapter.this.notifyDataSetChanged()");
            SectionedAdapter.this.notifyDataSetChanged();
        }
    }

    private List<Section> mSectionsList = Collections.synchronizedList(new ArrayList<Section>());

    abstract protected View getHeaderView(String caption, int index, View convertView, ViewGroup parent);

    public synchronized void addSection(String caption, Adapter adapter) {
        Log.v(TAG, "addSection() with header name: " + caption);
        mSectionsList.add(new Section(caption, adapter));
    }

    @Override
    public boolean areAllItemsEnabled() {
        return false;
    }

    public boolean isEnabled(int position) {
        return (getItemViewType(position) != TYPE_SECTION_HEADER);
    }

    public synchronized int getItemViewType(int position) {
        Log.v(TAG, "getItemViewType() for position: " + position);
        int typeOffset = TYPE_SECTION_HEADER + 1;

        for (Section section : mSectionsList) {
            if (position == 0) {
                return (TYPE_SECTION_HEADER);
            }

            int size = section.adapter.getCount() + 1;

            if (position < size) {
                return (typeOffset + section.adapter.getItemViewType(position - 1));
            }

            position -= size;
            typeOffset += section.adapter.getViewTypeCount();
        }

        return -1;
    }

    public synchronized int getViewTypeCount() {
        Log.v(TAG, "getViewTypeCount()");
        // one for the header, plus those from mSectionsList
        int total = 1;

        for (Section section : mSectionsList) {
            total += section.adapter.getViewTypeCount();
        }

        return total;
    }

    public synchronized Object getItem(int position) {
        Log.v(TAG, "getItem() for position: " + position);
        for (Section section : mSectionsList) {
            if (position == 0) {
                // header
                return section;
            }

            int size = section.adapter.getCount() + 1;

            if (position < size) {
                return (section.adapter.getItem(position - 1));
            }

            position -= size;
        }

        return null;
    }

    public synchronized int getCount() {
        Log.v(TAG, "getCount()");
        int total = 0;

        for (Section section : mSectionsList) {
            // add one for header
            total += section.adapter.getCount() + 1;
            Log.v(TAG, "getCount() -> calculating total: " + total);
        }

        return total;
    }

    @Override
    public synchronized View getView(int position, View convertView, ViewGroup parent) {
        Log.v(TAG, "getView() for position: " + position);
        int sectionIndex = 0;

        for (Section section : mSectionsList) {
            if (position == 0) {
                return getHeaderView(section.caption, sectionIndex, convertView, parent);
            }

            int size = section.adapter.getCount() + 1;

            if (position < size) {
                return section.adapter.getView(position - 1, convertView, parent);
            }

            position -= size;
            sectionIndex++;
        }

        throw new IndexOutOfBoundsException("Position: " + position + " is outside the bounds of this adapter. (size: " + getCount() + ")");
    }

    @Override
    public long getItemId(int position) {
        Log.v(TAG, "getItemId() for position: " + position);
        return position;
    }

}
