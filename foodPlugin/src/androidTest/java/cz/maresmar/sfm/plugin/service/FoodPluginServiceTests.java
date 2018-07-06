package cz.maresmar.sfm.plugin.service;

import android.support.annotation.NonNull;
import android.support.test.runner.AndroidJUnit4;
import android.util.SparseIntArray;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;

import cz.maresmar.sfm.plugin.ActionContract;
import cz.maresmar.sfm.plugin.BroadcastContract;
import cz.maresmar.sfm.plugin.model.LogData;

import static org.junit.Assert.*;

/**
 * Tests of {@link FoodPluginService}. Mainly test task calling and tasks dependencies.
 */
@RunWith(AndroidJUnit4.class)
public class FoodPluginServiceTests {

    /**
     * Simple dummy testing child class
     * <ul>
     * <li>only _DOWNLOAD task are supported</li>
     * <li>TASK_MENU_SYNC did only TASK_MENU_SYNC</li>
     * <li>TASK_HISTORY_DOWNLOAD did TASK_HISTORY_DOWNLOAD and TASK_CREDIT_SYNC</li>
     * <li>TASK_REMAINING_TO_TAKE_SYNC throws bad credentials error</li>
     * <li>TASK_REMAINING_TO_ORDER_SYNC throws unknown format error</li>
     * <li>TASK_STOCK_DOWNLOAD throws portal inaccessible error</li>
     * </ul>
     */
    class DummyFoodPluginService extends FoodPluginService {
        public int onMethodVisited = 0;
        public int handleTaskVisited = 0;

        @Override
        protected int testPortalData(@NonNull LogData logData) {
            throw new UnsupportedOperationException();
        }

        @Override
        protected void onSyncRequestStarted(@NonNull LogData data) throws IOException {
            super.onSyncRequestStarted(data);
            onMethodVisited++;
        }

        @Override
        protected void onSyncRequestFinished(@NonNull LogData data) throws IOException {
            super.onSyncRequestFinished(data);
            onMethodVisited *= 10;
        }

        @Override
        protected int supportedSyncTasks() {
            return ActionContract.TASK_GROUP_DATA_MENU_SYNC | ActionContract.TASK_CREDIT_SYNC |
                    ActionContract.TASK_ACTION_HISTORY_SYNC | ActionContract.TASK_MENU_SYNC |
                    ActionContract.TASK_REMAINING_TO_TAKE_SYNC | ActionContract.TASK_REMAINING_TO_ORDER_SYNC;
        }

        @Override
        protected int handleSyncTasks(@NonNull LogData data, @ActionContract.SyncTask int task) {
            if ((task & ActionContract.TASK_MENU_SYNC) != 0) {
                handleTaskVisited++;
                return ActionContract.TASK_MENU_SYNC;
            }
            if ((task & ActionContract.TASK_ACTION_HISTORY_SYNC) != 0 || (task & ActionContract.TASK_CREDIT_SYNC) != 0) {
                handleTaskVisited++;
                return ActionContract.TASK_ACTION_HISTORY_SYNC | ActionContract.TASK_CREDIT_SYNC;
            }
            if ((task & ActionContract.TASK_REMAINING_TO_TAKE_SYNC) != 0) {
                handleTaskVisited++;
                throw new WrongPassException();
            }
            if ((task & ActionContract.TASK_REMAINING_TO_ORDER_SYNC) != 0) {
                handleTaskVisited++;
                throw new WebPageFormatChangedException();
            }
            if ((task & ActionContract.TASK_GROUP_DATA_MENU_SYNC) != 0) {
                handleTaskVisited++;
                throw new ServerMaintainException();
            }

            return 0;
        }
    }

    private DummyFoodPluginService service;
    private LogData data = new LogData(-1, -1, -1);

    @Before
    public void init() {
        service = new DummyFoodPluginService();
    }

    @Test
    public void lifecycle() {
        service.handleActionSync(ActionContract.TASK_MENU_SYNC);
        assertEquals(10, service.onMethodVisited);
    }


    @Test
    public void okTask() {
        // Tests task interface
        SparseIntArray results = service.handleActionSync(ActionContract.TASK_MENU_SYNC);

        // Test results
        assertEquals(1, results.size());
        assertTrue(results.indexOfKey(ActionContract.TASK_MENU_SYNC) >= 0);
        assertEquals(BroadcastContract.RESULT_OK, results.get(ActionContract.TASK_MENU_SYNC));
    }

    @Test
    public void unsupportedTask() {
        SparseIntArray results = service.handleActionSync(ActionContract.TASK_ACTION_PRESENT_SYNC);
        assertEquals(1, results.size());
        assertTrue(results.indexOfKey(ActionContract.TASK_ACTION_PRESENT_SYNC) >= 0);
        assertEquals(BroadcastContract.RESULT_NOT_SUPPORTED, results.get(ActionContract.TASK_ACTION_PRESENT_SYNC));
    }

    @Test
    public void emptyTask() {
        SparseIntArray results = service.handleActionSync(0);
        assertEquals(0, results.size());
    }

    @Test
    public void combinationOfTasks() {
        SparseIntArray results = service.handleActionSync(ActionContract.TASK_ACTION_HISTORY_SYNC | ActionContract.TASK_MENU_SYNC);
        assertEquals(3, results.size());
        assertEquals(2, service.handleTaskVisited);
        assertTrue(results.indexOfKey(ActionContract.TASK_MENU_SYNC) >= 0);
        assertTrue(results.indexOfKey(ActionContract.TASK_ACTION_HISTORY_SYNC) >= 0);
        assertEquals(BroadcastContract.RESULT_OK, results.get(ActionContract.TASK_ACTION_HISTORY_SYNC));
        assertEquals(BroadcastContract.RESULT_OK, results.get(ActionContract.TASK_MENU_SYNC));
    }

    @Test
    public void oneTaskDidTwo() {
        SparseIntArray results = service.handleActionSync(ActionContract.TASK_ACTION_HISTORY_SYNC);
        assertEquals(2, results.size());
        assertEquals(1, service.handleTaskVisited);
        assertTrue(results.indexOfKey(ActionContract.TASK_ACTION_HISTORY_SYNC) >= 0);
        assertTrue(results.indexOfKey(ActionContract.TASK_CREDIT_SYNC) >= 0);
        assertEquals(BroadcastContract.RESULT_OK, results.get(ActionContract.TASK_ACTION_HISTORY_SYNC));
        assertEquals(BroadcastContract.RESULT_OK, results.get(ActionContract.TASK_CREDIT_SYNC));
    }

    @Test
    public void twoTaskDidInOneCall() {
        SparseIntArray results = service.handleActionSync(ActionContract.TASK_ACTION_HISTORY_SYNC | ActionContract.TASK_CREDIT_SYNC);
        assertEquals(2, results.size());
        assertEquals(1, service.handleTaskVisited);
        assertTrue(results.indexOfKey(ActionContract.TASK_ACTION_HISTORY_SYNC) >= 0);
        assertTrue(results.indexOfKey(ActionContract.TASK_CREDIT_SYNC) >= 0);
        assertEquals(BroadcastContract.RESULT_OK, results.get(ActionContract.TASK_ACTION_HISTORY_SYNC));
        assertEquals(BroadcastContract.RESULT_OK, results.get(ActionContract.TASK_CREDIT_SYNC));
    }

    @Test
    public void wrongCredentialsTest() {
        // Tests task interface
        SparseIntArray results = service.handleActionSync(ActionContract.TASK_REMAINING_TO_TAKE_SYNC);

        // Test results
        assertEquals(1, results.size());
        assertTrue(results.indexOfKey(ActionContract.TASK_REMAINING_TO_TAKE_SYNC) >= 0);
        assertEquals(BroadcastContract.RESULT_WRONG_CREDENTIALS, results.get(ActionContract.TASK_REMAINING_TO_TAKE_SYNC));
    }

    @Test
    public void unknownFormatTest() {
        // Tests task interface
        SparseIntArray results = service.handleActionSync(ActionContract.TASK_REMAINING_TO_ORDER_SYNC);

        // Test results
        assertEquals(1, results.size());
        assertTrue(results.indexOfKey(ActionContract.TASK_REMAINING_TO_ORDER_SYNC) >= 0);
        assertEquals(BroadcastContract.RESULT_UNKNOWN_PORTAL_FORMAT, results.get(ActionContract.TASK_REMAINING_TO_ORDER_SYNC));
    }

    @Test
    public void temporaryInaccessibleTest() {
        // Tests task interface
        SparseIntArray results = service.handleActionSync(ActionContract.TASK_GROUP_DATA_MENU_SYNC);

        // Test results
        assertEquals(1, results.size());
        assertTrue(results.indexOfKey(ActionContract.TASK_GROUP_DATA_MENU_SYNC) >= 0);
        assertEquals(BroadcastContract.RESULT_PORTAL_TEMPORALLY_INACCESSIBLE, results.get(ActionContract.TASK_GROUP_DATA_MENU_SYNC));
    }

    @Test
    public void testFailedAndOkTask() {
        // Tests task interface
        SparseIntArray results = service.handleActionSync(ActionContract.TASK_REMAINING_TO_ORDER_SYNC | ActionContract.TASK_MENU_SYNC);

        // Test results
        assertEquals(2, results.size());
        assertTrue(results.indexOfKey(ActionContract.TASK_REMAINING_TO_ORDER_SYNC) >= 0);
        assertTrue(results.indexOfKey(ActionContract.TASK_MENU_SYNC) >= 0);
        assertEquals(BroadcastContract.RESULT_UNKNOWN_PORTAL_FORMAT, results.get(ActionContract.TASK_REMAINING_TO_ORDER_SYNC));
        assertEquals(BroadcastContract.RESULT_OK, results.get(ActionContract.TASK_MENU_SYNC));
    }

    @Test
    public void testDummyClassValidity() {
        for (int i = 0; i < ActionContract.PLUGIN_TASKS_LENGTH; i++) {
            try {
                @SuppressWarnings("WrongConstant")
                int res = service.handleSyncTasks(data, (1 << i));
                assertEquals((service.supportedSyncTasks() & (1 << i)), (res & (1 << i)));
            } catch (FoodPluginException e) {
                // FoodPlugin exception are allowed, so I don't need to care about them
            }
        }
    }
}
