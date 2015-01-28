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
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.kevinshen.beyondupnp.R;
import com.kevinshen.beyondupnp.core.PlaybackCommand;
import com.kevinshen.beyondupnp.core.SystemManager;
import com.kevinshen.beyondupnp.database.BeyondUpnpContract.PlaylistItem;
import com.kevinshen.beyondupnp.util.MD5;

import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.UDN;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.contentdirectory.callback.Browse;
import org.fourthline.cling.support.model.BrowseFlag;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.Res;
import org.fourthline.cling.support.model.SortCriterion;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

import java.security.NoSuchAlgorithmException;

public class ContentContainerActivity extends Activity {
    private static final String TAG = ContentContainerActivity.class.getSimpleName();

    public static final String OBJECT_ID_TAG = "object_id";
    public static final String IDENTIFIER_STRING_TAG = "identifier_string";
    public static final String CONTENT_CONTAINER_TITLE = "container_title";

    private static final int ADD_OBJECTS = 0x01;

    private String mObjectId;
    private String mIdentifierString;
    private DIDLObjectAdapter mDidlObjectAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.listview);

        mObjectId = getIntent().getStringExtra(OBJECT_ID_TAG);
        mIdentifierString = getIntent().getStringExtra(IDENTIFIER_STRING_TAG);
        String title = getIntent().getStringExtra(CONTENT_CONTAINER_TITLE);
        //Display container title.
        if (title != null) {
            setTitle(title);
        }else{
            setTitle("");
        }

        mDidlObjectAdapter = new DIDLObjectAdapter(ContentContainerActivity.this);
        ListView listView = (ListView)findViewById(android.R.id.list);
        listView.setAdapter(mDidlObjectAdapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                DIDLObject didlObject = mDidlObjectAdapter.getItem(position);

                if (didlObject instanceof Container){
                    Intent intent = new Intent(ContentContainerActivity.this,ContentContainerActivity.class);
                    intent.putExtra(ContentContainerActivity.OBJECT_ID_TAG,didlObject.getId());
                    intent.putExtra(ContentContainerActivity.IDENTIFIER_STRING_TAG,mIdentifierString);
                    intent.putExtra(ContentContainerActivity.CONTENT_CONTAINER_TITLE,didlObject.getTitle());
                    startActivity(intent);
                }else if (didlObject instanceof Item){
                    Item item = (Item)didlObject;
                    playItem(item);
                }
            }
        });

        loadContent();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_content_container, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_close) {
            Intent intent = new Intent(getApplication(),MainActivity.class);
            startActivity(intent);
            
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private Handler handler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case ADD_OBJECTS:
                    DIDLContent didlContent = (DIDLContent)msg.obj;
                    mDidlObjectAdapter.addAll(didlContent.getContainers());
                    mDidlObjectAdapter.addAll(didlContent.getItems());
                    break;
            }
        }
    };

    private void playItem(Item item){
        if (item == null) return;

        Res res = item.getFirstResource();
        String uri = res.getValue();

        DIDLContent content = new DIDLContent();
        content.addItem(item);
        DIDLParser didlParser = new DIDLParser();
        String metadata = null;
        try {
            metadata = didlParser.generate(content);
        } catch (Exception e) {
            //ignore
        }
        //Log.d(TAG,"Item metadata:" + metadata);
        //Play on the selected device.
        PlaybackCommand.playNewItem(uri,metadata);
    }

    private void addToPlaylist(Item item){
        if (item == null) return;

        Res res = item.getFirstResource();
        String uri = res.getValue();

        //Parse content.
        DIDLContent content = new DIDLContent();
        content.addItem(item);
        DIDLParser didlParser = new DIDLParser();
        String metadata = null;
        try {
            //Generate track metadata.
            metadata = didlParser.generate(content);
        } catch (Exception e) {
            //ignore
        }

        String albumUri = null;
        try{
            for (DIDLObject.Property property: item.getProperties()){
                if (property instanceof DIDLObject.Property.UPNP.ALBUM_ART_URI){
                    albumUri = ((DIDLObject.Property.UPNP.ALBUM_ART_URI) property).getValue().toString();
                }
            }
        }catch (Exception e){
            //ignore
        }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(item.getCreator());
        stringBuilder.append(item.getTitle());
        stringBuilder.append(res.getSize());

        //Create md5 code
        String md5 = null;
        try {
            md5 = MD5.createMD5(stringBuilder.toString());
        } catch (NoSuchAlgorithmException e) {
            md5 = null;
        }

        ContentValues values = new ContentValues();
        values.put(PlaylistItem.ITEM_TITLE,item.getTitle());
        values.put(PlaylistItem.ITEM_URI,uri);
        values.put(PlaylistItem.ITEM_THUMB,albumUri);
        values.put(PlaylistItem.ITEM_METADATA,metadata);
        values.put(PlaylistItem.ITEM_DATE,System.currentTimeMillis());
        values.put(PlaylistItem.ITEM_VERIFICATION_CODE,md5);

        getContentResolver().insert(PlaylistItem.CONTENT_URI, values);
    }

    private void loadContent() {
        SystemManager systemManager = SystemManager.getInstance();
        Device device = null;
        try {
            device = systemManager.getRegistry().getDevice(new UDN(mIdentifierString), false);
        } catch (NullPointerException e) {
            Log.e(TAG, "Get device error.");
        }

        if (device != null) {
            //Get cds to browse children directories.
            Service contentDeviceService = device.findService(SystemManager.CONTENT_DIRECTORY_SERVICE);
            //Execute Browse action and init list view
            systemManager.getControlPoint().execute(new Browse(contentDeviceService, mObjectId, BrowseFlag.DIRECT_CHILDREN, "*", 0,
                    null, new SortCriterion(true, "dc:title")) {
                @Override
                public void received(ActionInvocation actionInvocation, DIDLContent didl) {
                    Message msg = Message.obtain(handler,ADD_OBJECTS,didl);
                    msg.sendToTarget();
                }

                @Override
                public void updateStatus(Status status) {
                }

                @Override
                public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
                }
            });
        }
    }

    private class DIDLObjectAdapter extends ArrayAdapter<DIDLObject> {
        private LayoutInflater mLayoutInflater;

        public DIDLObjectAdapter(Context context) {
            super(context, 0);
            mLayoutInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null)
                convertView = mLayoutInflater.inflate(R.layout.listview_items, null);

            final DIDLObject item = getItem(position);
            if (item == null) {
                return convertView;
            }

            //Init container icon
            if (item instanceof Container){
                ImageView imageView = (ImageView)convertView.findViewById(R.id.listview_item_image);
                imageView.setBackgroundResource(R.drawable.ic_action_dock);
            }else if (item instanceof Item){
                ImageView moreBtn = (ImageView)convertView.findViewById(R.id.listview_item_popup_menu);
                moreBtn.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        //Init popup menu.
                        PopupMenu popupMenu = new PopupMenu(ContentContainerActivity.this, v);
                        popupMenu.inflate(R.menu.menu_item_actions);
                        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                switch (menuItem.getItemId()){
                                    case R.id.action_play :
                                        playItem((Item)item);
                                        break;
                                    case R.id.action_add_to_playlist :
                                        addToPlaylist((Item)item);
                                        break;
                                    default:
                                        break;
                                }

                                return true;
                            }
                        });
                        popupMenu.show();
                    }
                });
                moreBtn.setVisibility(View.VISIBLE);
            }

            TextView titleView = (TextView)convertView.findViewById(R.id.listview_item_line_one);
            titleView.setText(item.getTitle());

            TextView creatorView = (TextView)convertView.findViewById(R.id.listview_item_line_two);
            creatorView.setText(item.getCreator());

            return convertView;
        }
    }
}
