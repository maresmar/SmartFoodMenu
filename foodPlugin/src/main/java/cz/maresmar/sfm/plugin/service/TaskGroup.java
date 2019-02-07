package cz.maresmar.sfm.plugin.service;

import androidx.annotation.NonNull;

import java.io.IOException;

import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.plugin.model.LogData;

/**
 * TaskGroup that can handle multiple plugin tasks (like menu sync, action sync etc....) in one task.
 * Optimally this task can depend on another tasks meaning these tasks have to done before this task.
 *
 * @see TasksPluginService
 */
public abstract class TaskGroup {

    /**
     * Sync tasks that support this task group
     */
    public abstract @ActionContract.SyncTask
    int provides();

    /**
     * Tasks that have to be done before this task group
     */
    public @ActionContract.SyncTask
    int depends() {
        return 0;
    }

    /**
     * Task body - the actual function
     *
     * @param data LogData contains credential and portal info
     * @throws IOException         Thrown when IO exception occurs
     * @throws FoodPluginException Thrown when some operation fails
     */
    public abstract void run(@NonNull LogData data) throws IOException;
}
