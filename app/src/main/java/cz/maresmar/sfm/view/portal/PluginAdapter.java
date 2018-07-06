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

package cz.maresmar.sfm.view.portal;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.List;

import cz.maresmar.sfm.R;

/**
 * {@link android.widget.Spinner} adapter that shows plugins
 *
 * @see PluginListLoader
 */
public class PluginAdapter extends BaseAdapter {

    Context mContext;
    List<PluginInfo> mPortalInfoData;

    /**
     * Creates new adapter
     *
     * @param context        Some valid context
     * @param pluginInfoData Loaded portals
     * @see PluginListLoader
     */
    public PluginAdapter(Context context, List<PluginInfo> pluginInfoData) {
        mContext = context;
        mPortalInfoData = pluginInfoData;
    }

    @Override
    public int getCount() {
        return mPortalInfoData.size();
    }

    @Override
    public PluginInfo getItem(int position) {
        return mPortalInfoData.get(position);
    }

    @Override
    public long getItemId(int position) {
        return -1;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        // first check to see if the view is null. if so, we have to inflate it.
        // to inflate it basically means to render, or show, the view.
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item_plugin, null);
        }

        /*
         * Recall that the variable position is sent in as an argument to this method.
         * The variable simply refers to the position of the current object in the list. (The ArrayAdapter
         * iterates through the list we sent it)
         *
         * Therefore, i refers to the current Item object.
         */
        PluginInfo info = getItem(position);

        if (info != null) {

            // This is how you obtain a reference to the TextViews.
            // These TextViews are created in the XML files we defined.

            TextView pluginName = (TextView) convertView.findViewById(R.id.pluginName);
            pluginName.setText(info.name);
            TextView pluginId = (TextView) convertView.findViewById(R.id.pluginId);
            pluginId.setText(info.id);
        }

        // the view must be returned to our activity
        return convertView;
    }
}
