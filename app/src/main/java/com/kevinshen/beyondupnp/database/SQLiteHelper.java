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
package com.kevinshen.beyondupnp.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class SQLiteHelper extends SQLiteOpenHelper {
    public static final String TAG = SQLiteHelper.class.getSimpleName();

    private static final String DATABASE_NAME = "beyondupnp.db";
    private static final int DATABASE_VERSION = 1;

    public SQLiteHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String playlistItemSql = "CREATE TABLE `" + BeyondUpnpContract.PlaylistItem.PLAYLIST_ITEM_TABLE + "` (  `"
                + BeyondUpnpContract.PlaylistItem._ID + "` INTEGER PRIMARY KEY AUTOINCREMENT,  `"
                + BeyondUpnpContract.PlaylistItem.ITEM_TITLE + "` TEXT , `"
                + BeyondUpnpContract.PlaylistItem.ITEM_THUMB + "` TEXT , `"
                + BeyondUpnpContract.PlaylistItem.ITEM_METADATA + "` TEXT , `"
                + BeyondUpnpContract.PlaylistItem.ITEM_VERIFICATION_CODE + "` TEXT , `"
                + BeyondUpnpContract.PlaylistItem.ITEM_DATE + "` INTEGER , `"
                + BeyondUpnpContract.PlaylistItem.ITEM_URI + "` TEXT );";
        db.execSQL(playlistItemSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion);

        if (oldVersion < DATABASE_VERSION) {
            db.execSQL("DROP TABLE IF EXISTS `" + BeyondUpnpContract.PlaylistItem.PLAYLIST_ITEM_TABLE + "`;");
            onCreate(db);
        }
    }
}
