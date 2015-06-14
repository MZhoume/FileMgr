package com.example.ming.filemgr;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;

class FilesAdapter extends BaseAdapter {

    private MainActivity mMainActivity;
    Context mContext;
    int mLayoutID;
    File[] mFiles;
    LayoutInflater mInflater;

    public FilesAdapter(MainActivity mainActivity, Context context, int layoutID, File[] arr) {
        mMainActivity = mainActivity;
        mContext = context;
        mLayoutID = layoutID;
        mFiles = arr;

        mInflater = (LayoutInflater) mMainActivity.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    public void setFiles(File[] files) {
        mFiles = files;
    }

    @Override
    public int getCount() {
        return mFiles.length;
    }

    @Override
    public Object getItem(int position) {
        return mFiles[position];
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = mInflater.inflate(mLayoutID, parent, false);
        }

        TextView txt = (TextView) convertView.findViewById(android.R.id.text1);

        txt.setText(mFiles[position].getName());

        if (mMainActivity.mSelectedIndex == position) {
            convertView.setBackgroundColor(Color.LTGRAY);
        } else {
            convertView.setBackgroundColor(mMainActivity.mListView.getDrawingCacheBackgroundColor());
        }

        return convertView;
    }
}
