package cz.maresmar.sfm.plugin.service;

import android.support.annotation.CheckResult;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

import cz.maresmar.sfm.plugin.model.LogData;
import cz.maresmar.sfm.plugin.ActionContract.SyncTask;

/**
 * {@link FoodPluginService} that uses {@link TaskGroup} to handle sync actions from app
 * <p>
 * The tasks are registered using {@link #registerTaskGroup(TaskGroup)} in {@link #onCreate()}
 * method. Tasks must be registered in right order - the registered task can {@link TaskGroup#depends()}
 * only on already registered tasks.
 * </p><p>
 * The recommended way how to write plugins is extending this class. See {@code TestPlugin} for examples.
 * </p>
 */
public abstract class TasksPluginService extends FoodPluginService {

    Deque<TaskGroup> mTaskGroups = new ArrayDeque<>();
    @SyncTask
    int mCommonSupportedTasks = 0;

    /**
     * Register new {@link TaskGroup}. The tasks group can only depend on already registered tasks
     *
     * @param group Task group to be registered
     */
    protected void registerTaskGroup(TaskGroup group) {
        @SyncTask int provides = group.provides();

        // Check if there is no conflict with existing tasks
        if ((mCommonSupportedTasks & provides) != 0) {
            throw new IllegalArgumentException("Group providing such feature is already registered " +
                    "(feature " + (mCommonSupportedTasks & provides) + ")");
        }

        // Check if there is already registered task that this task need to to start
        // (will be executed before this task)
        @SyncTask int depends = group.depends();
        if ((mCommonSupportedTasks & depends) != depends) {
            throw new IllegalArgumentException("Group depends on task that are not yet registered " +
                    "(so this task cannot be started before this task)");
        }

        // Register task
        mCommonSupportedTasks |= provides;
        mTaskGroups.addLast(group);
    }


    @Override
    @CheckResult
    protected @SyncTask
    int addSyncTasksDependencies(@SyncTask int todo) {
        // Iterate from end to begin
        Iterator<TaskGroup> it = mTaskGroups.descendingIterator();
        while (it.hasNext()) {
            TaskGroup taskGroup = it.next();
            // It the task will be used
            if ((taskGroup.provides() & todo) != 0) {
                todo |= taskGroup.depends();
            }
        }

        return todo;
    }

    @Override
    protected @SyncTask
    int supportedSyncTasks() {
        return mCommonSupportedTasks;
    }

    @Override
    @VisibleForTesting(otherwise = VisibleForTesting.PROTECTED)
    public @SyncTask
    int handleSyncTasks(@NonNull LogData data, @SyncTask int task)
            throws IOException {
        for (TaskGroup taskGroup : mTaskGroups) {
            @SyncTask int provides = taskGroup.provides();
            // If task provides thing we looking for
            if ((task & provides) != 0) {
                taskGroup.run(data);
                return provides;
            }
        }
        throw new IllegalArgumentException("Received unsupported task");
    }
}
