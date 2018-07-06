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

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import timber.log.Timber;

/**
 * {@link JobService} that handles sync when is planned (in background or when waiting for network)
 * <p>
 * This allows app to start sync on background when Android API >= Oreo
 * </p>
 *
 * @see SyncHandler
 */
public class SyncHandlerJob extends JobService {

    private SyncHandler mHandler = new SyncHandler();

    @Override
    public boolean onStartJob(JobParameters job) {

        @SyncHandler.Action
        int action = job.getExtras().getInt(SyncHandler.ACTION, SyncHandler.ACTION_FULL_SYNC);

        new Thread(() -> {
            boolean resultOk = mHandler.startAction(action, job.getExtras());

            jobFinished(job, !resultOk);
        }).start();

        Timber.d("Job %s started", job.getTag());

        // Return true as there's more work to be done with this job.
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        Timber.e("Job %s stopped before finished", job.getTag());
        return true; // Answers the question: "Should this job be retried?"
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
