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
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.widget.SwipeRefreshLayout;
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

public class LibraryFragment extends Fragment {
    private static final String TAG = LibraryFragment.class.getSimpleName();

    private ArrayAdapter<Device> mArrayAdapter;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ListView mCddListView;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @return A new instance of fragment LibraryFragment.
     */
    public static LibraryFragment newInstance() {
        LibraryFragment fragment = new LibraryFragment();
        return fragment;
    }

    public LibraryFragment() {
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
        View view = inflater.inflate(R.layout.fragment_library, container, false);
        //Init content directory device ListView
        mCddListView = (ListView)view.findViewById(R.id.content_directory_devices);
        mArrayAdapter = new LibraryAdapter(getActivity());
        mCddListView.setAdapter(mArrayAdapter);
        mCddListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Device device = mArrayAdapter.getItem(position);
                String identifierString = device.getIdentity().getUdn().getIdentifierString();
                String objectId = "0";
                //Create new activity
                Intent intent = new Intent(getActivity(),ContentContainerActivity.class);
                intent.putExtra(ContentContainerActivity.OBJECT_ID_TAG,objectId);
                intent.putExtra(ContentContainerActivity.IDENTIFIER_STRING_TAG,identifierString);
                intent.putExtra(ContentContainerActivity.CONTENT_CONTAINER_TITLE,device.getDetails().getFriendlyName());
                getActivity().startActivity(intent);
            }
        });

        mSwipeRefreshLayout = (SwipeRefreshLayout)view.findViewById(R.id.swipe_refresh_layout_library);
        mSwipeRefreshLayout.setOnRefreshListener(onRefreshListener);
        //SwipeRefresh style.
        mSwipeRefreshLayout.setColorSchemeResources(android.R.color.holo_blue_bright);

        return view;
    }

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

        refreshDeviceList();
    }

    public void refreshDeviceList(){
        Log.i("Library","Refresh List.");
        Collection<Device> devices = SystemManager.getInstance().getDmcDevices();
        mArrayAdapter.clear();
        mArrayAdapter.addAll(devices);
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
        }
    };

    private SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {
        @Override
        public void onRefresh() {
            mSwipeRefreshLayout.setRefreshing(true);
            mCddListView.setEnabled(false);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    mSwipeRefreshLayout.setRefreshing(false);
                    refreshDeviceList();
                    mCddListView.setEnabled(true);
                }
            });
        }
    };

    private class LibraryAdapter extends ArrayAdapter<Device> {
        private LayoutInflater mInflater;

        public LibraryAdapter(Context context) {
            super(context, 0);
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
}
