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

import android.net.Uri;
import android.provider.BaseColumns;

public class BeyondUpnpContract {

    interface PlaylistItemColumns {
        /**
         * The item title for this table.
         */
        public static final String ITEM_TITLE = "item_title";

        /**
         * The item uri for this table.
         */
        public static final String ITEM_URI = "item_uri";

        /**
         * The item thumb for this table.
         */
        public static final String ITEM_THUMB = "item_thumb";

        /**
         * The item metadata for this table.
         */
        public static final String ITEM_METADATA = "item_metadata";

        /**
         * The item join date for this table.
         */
        public static final String ITEM_DATE = "item_date";

        /**
         * The item verification code(md5) for this table.
         */
        public static final String ITEM_VERIFICATION_CODE = "verification_code";
    }

    public static class PlaylistItem implements PlaylistItemColumns,BaseColumns {
        public static String PLAYLIST_ITEM_TABLE = "playlist_item";

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLAYLIST_ITEM).build();

        public static final String CONTENT_TYPE =
                "vnd.android.cursor.dir/vnd.beyondupnp.playlist_item";
        public static final String CONTENT_ITEM_TYPE =
                "vnd.android.cursor.item/vnd.beyondupnp.playlist_item";

    }

    public static final String CONTENT_AUTHORITY = "com.kevinshen.beyondupnp";

    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    private static final String PATH_PLAYLIST_ITEM = "playlist_item";
}
