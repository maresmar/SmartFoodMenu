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

import android.net.Uri;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;

import java.util.ArrayDeque;
import java.util.Deque;

import cz.maresmar.sfm.provider.PublicProviderContract;

/**
 * Special schema for LogData {@link Uri}. The LogData has schema {@code .../xxx/PORTAL_ID/CREDENTIAL_ID}.
 * The last {@code CREDENTIAL_ID} is optimal. The schemas with ID always acts as item
 */
public class LogDataUriSchema extends AbstractUriSchema {

    private String mLastColumnName, mPenultimateColumnName;
    private String mUri;

    public LogDataUriSchema(@NonNull String uri, @NonNull String penultimateColName,
                            @NonNull String lastColName) {
        mUri = uri;
        mPenultimateColumnName = penultimateColName;
        mLastColumnName = lastColName;
    }

    @Override
    @NonNull
    public String[] getUris() {
        return new String[]{ mUri, mUri + "/#/*" };
    }

    @Override
    @MineKind
    public int getKind(@NonNull Uri uri) {
        if(isIdsUri(uri))
            return MINE_KIND_ITEM;
        else
            return MINE_KIND_DIR;
    }

    private boolean isIdsUri(@NonNull Uri uri) {
        String lastSegment = uri.getPath();

        return lastSegment.matches(".+/[0-9]+/[^/]+");
    }

    @Override
    @NonNull
    @CallSuper
    public Deque<Param> parseParamsFromUri(@NonNull Uri uri) {
        ArrayDeque<Param> params = new ArrayDeque<>();

        if(isIdsUri(uri)) {
            int pathSize = uri.getPathSegments().size();
            String lastSegment = uri.getPathSegments().get(pathSize - 1);
            String penultimateSegment = uri.getPathSegments().get(pathSize - 2);

            params.add(new Param(mPenultimateColumnName, Long.valueOf(penultimateSegment)));

            long segmentValue = Long.valueOf(lastSegment);
            if(segmentValue != PublicProviderContract.PORTAL_ONLY_CREDENTIAL_ID) {
                params.add(new Param(mLastColumnName, segmentValue));
            }
        }

        return params;
    }

    @NonNull
    @Override
    public Uri buildEntryUri(Uri insertUri, Long entryId) {
        throw new UnsupportedOperationException("You cannot use crete entry uri using this method");
    }
}
