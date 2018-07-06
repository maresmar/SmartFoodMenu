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

package cz.maresmar.sfm.service.plugin.sync;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;

/**
 * {@link IntentService} that handles sync when app is started
 * <p>
 * This allows app to run sync without delay (caused sometimes by {@link android.app.job.JobScheduler})
 * </p>
 *
 * @see SyncHandler
 */
public class SyncHandleIntentService extends IntentService {

    SyncHandler mHandler = new SyncHandler();

    /**
     * Creates new IntentService
     */
    public SyncHandleIntentService() {
        super("SyncHandlerIntentService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null || intent.getExtras() == null) {
            throw new IllegalArgumentException("You have to specifi correct extras");
        }

        @SyncHandler.Action
        int action = intent.getIntExtra(SyncHandler.ACTION, SyncHandler.ACTION_FULL_SYNC);

        mHandler.startAction(action, intent.getExtras());
    }

    @Override
    public void onCreate() {
        super.onCreate();

        mHandler.onCreate(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        mHandler.onDestroy();
    }
}
