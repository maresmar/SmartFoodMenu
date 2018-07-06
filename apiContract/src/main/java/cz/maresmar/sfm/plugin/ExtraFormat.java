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

package cz.maresmar.sfm.plugin;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;

/**
 * Plugin Extra format that specify that portal needs some extra values. These values are shown in UI
 * and user can change it. The format support some basic (regex) validation but the real validation
 * should be done in {@link ActionContract#ACTION_PORTAL_TEST}.
 * <p>
 * Extras specify value-pair settings that are specific for each plugin.
 * </p>
 *
 * @see BroadcastContract#broadcastExtraFormat(Context, String, String, int, JSONArray)
 */
public class ExtraFormat {

    private static final String EXTRA_CODE = "code";
    private static final String EXTRA_NAME = "name";
    private static final String EXTRA_PATTERN = "pattern";
    private static final String EXTRA_DESCRIPTION = "description";
    private static final String EXTRA_VALUES_LIST = "valuesList";

    /**
     * Internal code of value
     */
    @NonNull
    final public String code;

    /***
     * UI name of field
     */
    @NonNull
    final public String name;

    /**
     * Regex pattern that must match the value
     */
    @NonNull
    public String pattern = ".*";

    /**
     * UI description of field
     */
    @Nullable
    public String description;

    /**
     * UI values to select from (if it's empty, the field act as edit text)
     */
    @NonNull
    public String[] valuesList = new String[0];

    /**
     * Create new Extra with specific (internal) code and user visible name.
     * @param code The code of param
     * @param name The UI name to show to user
     */
    public ExtraFormat(@NonNull String code, @NonNull String name) {
        this.code = code;
        this.name = name;
    }

    /**
     * Parse Extra format from JSON
     * @param jsonObject Data to be parsed
     * @throws JSONException Thrown when format is invalid
     */
    public ExtraFormat(@NonNull JSONObject jsonObject) throws JSONException {
        code = jsonObject.getString(EXTRA_CODE);
        name = jsonObject.getString(EXTRA_NAME);
        pattern = jsonObject.getString(EXTRA_PATTERN);
        if(!jsonObject.isNull(EXTRA_DESCRIPTION))
            description = jsonObject.getString(EXTRA_DESCRIPTION);

        // Values list
        JSONArray valuesArray = jsonObject.getJSONArray(EXTRA_VALUES_LIST);
        if(valuesArray != null) {
            valuesList = new String[valuesArray.length()];
            for (int i = 0; i < valuesArray.length(); i++) {
                valuesList[i] = valuesArray.getString(i);
            }
        }

    }

    /**
     * Prints format object to JSON
     * @return Parsed data
     * @throws JSONException Thrown when {@link ExtraFormat#valuesList} is null
     */
    @NonNull
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put(EXTRA_CODE, code);
        jsonObject.put(EXTRA_NAME, name);
        jsonObject.put(EXTRA_PATTERN, pattern);
        jsonObject.put(EXTRA_DESCRIPTION, description == null ? JSONObject.NULL : description);
        jsonObject.put(EXTRA_VALUES_LIST, new JSONArray(Arrays.asList(valuesList)));
        return jsonObject;
    }
}
