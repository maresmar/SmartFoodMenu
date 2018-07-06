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

import android.content.ContentUris;
import android.net.Uri;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;

import cz.maresmar.sfm.Assert;
import cz.maresmar.sfm.BuildConfig;

/**
 * Schema model for {@link Uri}s that can end on ID of row. Eg. {@code .../xxxx/} for dir and
 * {@code .../xxxx/ID} for items. It is standard behaviour how {@link Uri}s work.
 */
public class IdEndingUriSchema extends AbstractUriSchema {

    private String mIdColumnName;
    private String mUri;

    /**
     * Create new handler from dir schema and ID column name
     * @param uri Schema of dir
     * @param idColumnName Name of ID column
     */
    public IdEndingUriSchema(@NonNull String uri, @NonNull String idColumnName) {
        mUri = uri;
        mIdColumnName = idColumnName;
    }

    @Override
    @NonNull
    public String[] getUris() {
        return new String[]{ mUri, mUri + "/#" };
    }

    @Override
    @MineKind
    public int getKind(@NonNull Uri uri) {
        if(isIdUri(uri))
            return MINE_KIND_ITEM;
        else
            return MINE_KIND_DIR;
    }

    private boolean isIdUri(@NonNull Uri uri) {
        String lastSegment = uri.getLastPathSegment();

        return lastSegment.matches("[0-9]+");
    }

    @Override
    @NonNull
    @CallSuper
    public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
        ArrayDeque<Param> params = new ArrayDeque<>();

        if(isIdUri(uri)) {
            String lastSegment = uri.getLastPathSegment();
            params.add(new Param(mIdColumnName, Long.valueOf(lastSegment)));
        }

        return params;
    }

    @NonNull
    @Override
    public Uri buildEntryUri(Uri insertUri, Long entryId) {
        if(isIdUri(insertUri)) {
            if(BuildConfig.DEBUG) {
                Assert.that(ContentUris.parseId(insertUri) == entryId, "Inserted uri does not match id from before");
            }
            return insertUri;
        } else {
            return ContentUris.withAppendedId(insertUri, entryId);
        }
    }
}
