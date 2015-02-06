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
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;

import com.astuetz.PagerSlidingTabStrip;
import com.kevinshen.beyondupnp.BeyondApplication;
import com.kevinshen.beyondupnp.service.BeyondUpnpService;
import com.kevinshen.beyondupnp.Intents;
import com.kevinshen.beyondupnp.R;
import com.kevinshen.beyondupnp.service.SystemService;
import com.kevinshen.beyondupnp.core.SystemManager;

import org.fourthline.cling.android.AndroidUpnpService;

import java.util.HashMap;

public class MainActivity extends Activity implements ViewPager.OnPageChangeListener {
    public static final String TAG = MainActivity.class.getSimpleName();

    public static final String DIALOG_FRAGMENT_TAG = "dialog";
    private PagerSlidingTabStrip mTabs;
    private ViewPager mPager;
    private PagerAdapter mPagerAdapter;

    private static final int NOWPLAYING_FRAGMENT_INDEX = 0;
    private static final int PLAYLIST_FRAGMENT_INDEX = 1;
    private static final int LIBRARY_FRAGMENT_INDEX = 2;

    private HashMap<Integer,Fragment> mFragmentArrayMap;
    private BeyondApplication mBeyondApplication;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBeyondApplication = (BeyondApplication)getApplication();

        mTabs = (PagerSlidingTabStrip) findViewById(R.id.tabs);
        mTabs.setShouldExpand(true);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new PagerAdapter(getFragmentManager());

        mPager.setAdapter(mPagerAdapter);
        mPager.setOffscreenPageLimit(2);

        mTabs.setViewPager(mPager);
        mTabs.setOnPageChangeListener(this);
        //Init fragment map
        mFragmentArrayMap = new HashMap<>(3);
        // Bind UPnP service
        Intent upnpServiceIntent = new Intent(MainActivity.this, BeyondUpnpService.class);
        bindService(upnpServiceIntent, mUpnpServiceConnection, Context.BIND_AUTO_CREATE);
        // Bind System service
        Intent systemServiceIntent = new Intent(MainActivity.this, SystemService.class);
        bindService(systemServiceIntent, mSystemServiceConnection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_select) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            Fragment prev = getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG);
            if (prev != null) {
                ft.remove(prev);
            }
            ft.addToBackStack(null);

            // Create and show the dialog.
            DialogFragment newFragment = DeviceListDialogFragment.newInstance();
            newFragment.show(ft, DIALOG_FRAGMENT_TAG);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        // Unbind UPnP service
        unbindService(mUpnpServiceConnection);
        // Unbind System service
        unbindService(mSystemServiceConnection);

        mFragmentArrayMap.clear();
        mFragmentArrayMap = null;

        mBeyondApplication.stopServer();
    }

    private ServiceConnection mUpnpServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            BeyondUpnpService.LocalBinder binder = (BeyondUpnpService.LocalBinder) service;
            BeyondUpnpService beyondUpnpService = binder.getService();

            SystemManager systemManager =  SystemManager.getInstance();
            systemManager.setUpnpService(beyondUpnpService);
            //Search on service created.
            systemManager.searchAllDevices();
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            SystemManager.getInstance().setUpnpService(null);
        }
    };

    private ServiceConnection mSystemServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            SystemService.SystemServiceBinder systemServiceBinder = (SystemService.SystemServiceBinder) service;
            //Set binder to SystemManager
            SystemManager systemManager = SystemManager.getInstance();
            systemManager.setSystemServiceBinder(systemServiceBinder);
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            SystemManager.getInstance().setUpnpService(null);
        }
    };

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        Fragment fragment = mPagerAdapter.getItem(position);
        //Refresh LibraryFragment when it selected
        if (position == LIBRARY_FRAGMENT_INDEX){
            ((LibraryFragment)fragment).refreshDeviceList();
        }
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        SystemManager systemManager = SystemManager.getInstance();
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP){
            int volume = systemManager.getDeviceVolume();
            volume += 5;
            if (volume > 100)
                volume = 100;
            sendBroadcast(new Intent(Intents.ACTION_SET_VOLUME).putExtra("currentVolume",volume));
            return true;
        }else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN){
            int volume = systemManager.getDeviceVolume();
            volume -= 5;
            if (volume < 0)
                volume = 0;
            sendBroadcast(new Intent(Intents.ACTION_SET_VOLUME).putExtra("currentVolume",volume));
            return true;
        }else {
            return super.onKeyDown(keyCode, event);
        }
    }

    private class PagerAdapter extends FragmentPagerAdapter {

        private final String[] TITLES = {"Playing", "Playlist", "Library"};

        public PagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = mFragmentArrayMap.get(position);
            if (fragment == null){
                switch (position) {
                    case NOWPLAYING_FRAGMENT_INDEX:
                        fragment = NowplayingFragment.newInstance();
                        mFragmentArrayMap.put(NOWPLAYING_FRAGMENT_INDEX,fragment);
                        break;
                    case PLAYLIST_FRAGMENT_INDEX:
                        fragment = PlaylistFragment.newInstance();
                        mFragmentArrayMap.put(PLAYLIST_FRAGMENT_INDEX,fragment);
                        break;
                    case LIBRARY_FRAGMENT_INDEX:
                        fragment = LibraryFragment.newInstance();
                        mFragmentArrayMap.put(LIBRARY_FRAGMENT_INDEX,fragment);
                        break;
                }
            }
            return fragment;
        }
    }
}
