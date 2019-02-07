/*
 * SmartFoodMenu - Android application for canteens extendable with plugins
 *
 * Copyright © 2016-2018  Martin Mareš <mmrmartin[at]gmail[dot]com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package cz.maresmar.sfm.provider.schema;

import android.content.ContentValues;
import android.net.Uri;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Deque;

/**
 * Saves {@link android.content.UriMatcher} schemas with methods that help read existing {@link Uri}s
 * and create new (with same schema)
 */
public abstract class AbstractUriSchema {

    /**
     * Mine kind of one row
     * @see android.content.ContentProvider#getType(Uri)
     */
    public static final int MINE_KIND_ITEM = 0;

    /**
     * Mine kind of whole table
     * @see android.content.ContentProvider#getType(Uri)
     */
    public static final int MINE_KIND_DIR = 1;

    @IntDef(flag = true, value = {
            MINE_KIND_ITEM,
            MINE_KIND_DIR
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface MineKind {
    }

    /**
     * Get supported schemas
     *
     * @return Array of supported schemas
     * @see android.content.UriMatcher
     */
    @NonNull
    public abstract String[] getUris();

    /**
     * Returns kind of {@link Uri}
     *
     * @param uri Uri to be checked
     * @return {@link AbstractUriSchema#MINE_KIND_DIR} if it points to whole table or
     * {@link AbstractUriSchema#MINE_KIND_ITEM} if it points to one row
     * @see android.content.ContentProvider#getType(Uri)
     */
    @MineKind
    public abstract int getKind(@NonNull Uri uri);

    /**
     * Parse values from Uri. This method prepare (future selection) params for database operations.
     *
     * @param uri Uri to get params from
     * @return Queue of params parsed and counted from params
     */
    @NonNull
    public abstract Deque<Param> parseParamsFromUri(@NonNull Uri uri);

    /**
     * Parse values from Uri, this method allow also use Content values for finding of parameters. This
     * method finds params only for insert or update.
     *
     * @param uri    Uri to get params from
     * @param values Values used for getting result
     * @return Queue of params parsed and counted from params
     */
    @NonNull
    public Deque<Param> parseParamsFromUriAndValues(@NonNull Uri uri, @Nullable ContentValues values) {
        return parseParamsFromUri(uri);
    }

    /**
     * Build Uri for entry with specific ID. The returned {@link Uri} will be {@link AbstractUriSchema#MINE_KIND_ITEM}.
     * This method is only used in inserts.
     *
     * @param insertUri Uri used for insert
     * @param entryId   New entry ID
     * @return Uri for new entry
     */
    @NonNull
    public abstract Uri buildEntryUri(Uri insertUri, Long entryId);
}
