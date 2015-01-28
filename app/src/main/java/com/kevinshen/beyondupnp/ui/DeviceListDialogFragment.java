/*
 * Copyright (C) 2014 Kevin Shen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kevinshen.beyondupnp.ui;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.kevinshen.beyondupnp.R;
import com.kevinshen.beyondupnp.core.SystemManager;

import org.fourthline.cling.model.meta.Device;

import java.util.Collection;

public class DeviceListDialogFragment extends DialogFragment {
    private static final String TAG = DeviceListDialogFragment.class.getSimpleName();
    private ArrayAdapter<Device> mArrayAdapter;
    private AsyncTask mUpdateDeviceTask;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment DeviceListDialogFragment.
     */
    public static DeviceListDialogFragment newInstance() {
        DeviceListDialogFragment fragment = new DeviceListDialogFragment();
        return fragment;
    }

    public DeviceListDialogFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.listview, container, false);
        // Set dialog title
        getDialog().setTitle("Select Active Device");

        ListView listView = (ListView) view.findViewById(android.R.id.list);
        mArrayAdapter = new DeviceListAdapter(getActivity(), 0);
        listView.setAdapter(mArrayAdapter);
        listView.setOnItemClickListener(selectDeviceListener);

        return view;
    }

    private AdapterView.OnItemClickListener selectDeviceListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Device device = mArrayAdapter.getItem(position);
            getActivity().setTitle(device.getDetails().getFriendlyName());
            //Set selected device,there is only one device can be selected.
            SystemManager.getInstance().setSelectedDevice(device);
            //Close the dialog.
            dismiss();
        }
    };

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Create task
        mUpdateDeviceTask = new UpdateDeviceListTask().execute();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mUpdateDeviceTask != null) {
            mUpdateDeviceTask.cancel(true);
            mUpdateDeviceTask = null;
        }
    }

    @Override
    public void onDestroy() {
        //Cancel working task.
        if (mUpdateDeviceTask != null)
            mUpdateDeviceTask.cancel(true);

        super.onDestroy();
    }

    private class DeviceListAdapter extends ArrayAdapter<Device> {
        private LayoutInflater mInflater;

        public DeviceListAdapter(Context context, int resource) {
            super(context, resource);
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mInflater.inflate(R.layout.listview_items, null);

            Device item = getItem(position);
            if (item == null) {
                return convertView;
            }

            ImageView imageView = (ImageView)convertView.findViewById(R.id.listview_item_image);
            imageView.setBackgroundResource(R.drawable.ic_action_dock);

            TextView textView = (TextView) convertView.findViewById(R.id.listview_item_line_one);
            textView.setText(item.getDetails().getFriendlyName());
            return convertView;
        }
    }

    private class UpdateDeviceListTask extends AsyncTask<Void, Collection<Device>, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            final SystemManager deviceManager = SystemManager.getInstance();
            while (true) {
                Log.i(TAG, "Search devices");
                //Send search command
                deviceManager.searchAllDevices();
                //Update list values
                Collection<Device> devices = deviceManager.getDmrDevices();
                publishProgress(devices);
                //Break immediately while task was cancelled.
                if (isCancelled()) break;

                try {
                    //Sleep 3 second
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Interrupt update thread!");
                    break;
                }
            }
            return null;
        }

        @Override
        protected void onProgressUpdate(Collection<Device>... values) {
            Collection<Device> devices = values[0];
            mArrayAdapter.clear();
            mArrayAdapter.addAll(devices);
            Log.i(TAG, "Device list update.");
        }
    }
}
