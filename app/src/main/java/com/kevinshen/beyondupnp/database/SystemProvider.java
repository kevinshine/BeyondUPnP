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

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.kevinshen.beyondupnp.database.BeyondUpnpContract.PlaylistItem;

public class SystemProvider extends ContentProvider {
    private static final String TAG = SystemProvider.class.getSimpleName();

    private SQLiteHelper mOpenHelper;

    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private static final int PLAYLIST_ITEM = 101;
    private static final int PLAYLIST_ITEM_ID = 102;

    /**
     * Build and return a {@link UriMatcher} that catches all {@link Uri}
     * variations supported by this {@link ContentProvider}.
     */
    private static UriMatcher buildUriMatcher() {
        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = BeyondUpnpContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, "playlist_item", PLAYLIST_ITEM);
        matcher.addURI(authority, "playlist_item/*", PLAYLIST_ITEM_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new SQLiteHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        final SQLiteDatabase db = mOpenHelper.getReadableDatabase();

        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PLAYLIST_ITEM: {
                break;
            }
            case PLAYLIST_ITEM_ID: {
                long id = ContentUris.parseId(uri);
                selection = PlaylistItem._ID + " = ?";
                selectionArgs = new String[]{String.valueOf(id)};
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown URI: " + uri);
            }
        }

        return db.query(PlaylistItem.PLAYLIST_ITEM_TABLE, projection, selection, selectionArgs, null, null, sortOrder);
    }

    @Override
    public String getType(Uri uri) {
        int match = sUriMatcher.match(uri);
        switch (match) {
            case PLAYLIST_ITEM:
                return PlaylistItem.CONTENT_TYPE;
            case PLAYLIST_ITEM_ID:
                return PlaylistItem.CONTENT_ITEM_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        switch (match) {
            case PLAYLIST_ITEM: {
                long id = db.insertOrThrow(PlaylistItem.PLAYLIST_ITEM_TABLE, null, values);
                notifyChange(uri);
                return ContentUris.withAppendedId(uri, id);
            }
        }

        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count = 0;
        switch (match) {
            case PLAYLIST_ITEM_ID: {
                long id = ContentUris.parseId(uri);
                selection = PlaylistItem._ID + " = ?";
                selectionArgs = new String[]{String.valueOf(id)};
                count = db.delete(PlaylistItem.PLAYLIST_ITEM_TABLE, selection, selectionArgs);
                break;
            }
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }

        if (count > 0) {
            notifyChange(uri);
        }

        return count;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);

        switch (match) {
            case PLAYLIST_ITEM_ID:
                long id = ContentUris.parseId(uri);
                selection = PlaylistItem._ID + " = ?";
                selectionArgs = new String[]{String.valueOf(id)};
                break;
            default:
                throw new IllegalArgumentException("Unknown URI: " + uri);
        }
        int count = db.update(PlaylistItem.PLAYLIST_ITEM_TABLE, values, selection, selectionArgs);
        if (count > 0) {
            notifyChange(uri);
        }
        return count;
    }

    private void notifyChange(Uri uri) {
        getContext().getContentResolver().notifyChange(uri, null);
    }

}
