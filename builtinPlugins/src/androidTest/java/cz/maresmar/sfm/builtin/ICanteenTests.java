package cz.maresmar.sfm.builtin;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.runners.Enclosed;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import cz.maresmar.sfm.plugin.model.GroupMenuEntry;
import cz.maresmar.sfm.plugin.model.LogData;
import cz.maresmar.sfm.plugin.model.MenuEntry;
import cz.maresmar.sfm.plugin.model.Action;
import cz.maresmar.sfm.provider.PublicProviderContract;

import static cz.maresmar.sfm.provider.PublicProviderContract.*;

/**
 * Tests of {@link ICanteenMenuParser}
 */

@RunWith(Enclosed.class)
public class ICanteenTests {

    static LogData mLogData = new LogData(-1, -1, -1);

    public static class Global {
        @Test
        public void portalVersionAutoUpdate() {
            ICanteenService service = new ICanteenService();

            // Test
            service.autoUpdatePortalVersion("by iCanteen version 2.5.15 | 2014");
            Assert.assertEquals(205, service.mPortalVersion);

            service.autoUpdatePortalVersion("by iCanteen version 2.6.08 | 2014-");
            Assert.assertEquals(206, service.mPortalVersion);

            service.autoUpdatePortalVersion("by iCanteen version 2.7.13 | 2014-");
            Assert.assertEquals(207, service.mPortalVersion);

            service.autoUpdatePortalVersion(" | iCanteen verze 2.10.29 | ");
            Assert.assertEquals(210, service.mPortalVersion);

            service.autoUpdatePortalVersion("iCanteen verze 2.13.26 | 2017");
            Assert.assertEquals(213, service.mPortalVersion);

            service.autoUpdatePortalVersion("iCanteen 2.14.14 | 2017");
            Assert.assertEquals(213, service.mPortalVersion);
        }
    }

    public static class Version205 {

        InputStream mIs;
        int mPortalVersion = 205;
        String mAllergenPattern = null;
        boolean mAutoUpdate = false;
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

        List<MenuEntry> menuEntries;
        List<GroupMenuEntry> groupMenuEntries;
        List<Action.MenuEntryAction> orderEntries;

        @Before
        public void init() throws Exception {
            mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
            mIs = testContext.getAssets().open("205/iCanteen.html");

            ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

            parser.parseData(mIs, mLogData);

            Assert.assertEquals(NO_INFO, parser.getCredit());

            menuEntries = parser.getMenuEntries();
            groupMenuEntries = parser.getGroupMenuEntries();
            orderEntries = parser.getActionEntries();
        }

        @Test
        public void wholeMenu() {
            Assert.assertEquals(29, menuEntries.size());
            Assert.assertEquals(8, orderEntries.size());
        }

        @Test
        public void disabledNotOrderedMenuEntry() throws Exception {
            MenuEntry menuEntry = menuEntries.get(0);
            GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

            Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

            Date date = mFormatter.parse("2014-04-23");
            Assert.assertEquals(date.getTime(), menuEntry.date);
            Assert.assertEquals("Oběd 1", menuEntry.label);
            Assert.assertEquals(3000, groupMenuEntry.price);
            Assert.assertEquals(
                    "Španělský ptáček, rýže, porce rajčete",
                    menuEntry.text
            );

            // MenuEntry entry menuStatus
            // Portal supports food stock but I don't have data for implementation and this version is detracted
            Assert.assertEquals(0,
                    groupMenuEntry.menuStatus);

            // Some extras
            Assert.assertEquals("Oběd", menuEntry.group);
        }

        @Test
        public void disabledOrderedMenuEntry() throws Exception {
            MenuEntry menuEntry = menuEntries.get(5);
            GroupMenuEntry groupMenuEntry = groupMenuEntries.get(5);

            Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

            Date date = mFormatter.parse("2014-04-24");
            Assert.assertEquals(date.getTime(), menuEntry.date);
            Assert.assertEquals("Oběd 3", menuEntry.label);
            Assert.assertEquals(3000, groupMenuEntry.price);
            Assert.assertEquals(
                    "Rockové halušky - slanina, uzené maso 80 g, kysané zelí 100 g - mix",
                    menuEntry.text
            );

            // MenuEntry entry menuStatus
            Assert.assertEquals(0,
                    groupMenuEntry.menuStatus);

            // Some extras
            Assert.assertEquals("Oběd", menuEntry.group);
        }

        @Test
        public void orderedMenuEntry() throws Exception {
            MenuEntry menuEntry = menuEntries.get(7);
            GroupMenuEntry groupMenuEntry = groupMenuEntries.get(7);

            Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

            Date date = mFormatter.parse("2014-04-25");
            Assert.assertEquals(date.getTime(), menuEntry.date);
            Assert.assertEquals("Oběd 2", menuEntry.label);
            Assert.assertEquals(3000, groupMenuEntry.price);
            Assert.assertEquals(
                    "Kapustový karbanátek, brambory s máslem, porce rajčete",
                    menuEntry.text
            );

            // MenuEntry entry menuStatus
            Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                    groupMenuEntry.menuStatus);

            // Some extras
            Assert.assertEquals("Oběd", menuEntry.group);
            Assert.assertEquals(
                    "db/dbProcessOrder.jsp?time=1398258207155&ID=1095844&day=2014-04-25&type=delete&week=null",
                    menuEntry.extra);
        }

        @Test
        public void notOrderedMenuEntry() throws Exception {
            MenuEntry menuEntry = menuEntries.get(18);
            GroupMenuEntry groupMenuEntry = groupMenuEntries.get(18);

            Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

            Date date = mFormatter.parse("2014-05-02");
            Assert.assertEquals(date.getTime(), menuEntry.date);
            Assert.assertEquals("Oběd 1", menuEntry.label);
            Assert.assertEquals(3000, groupMenuEntry.price);
            Assert.assertEquals(
                    "Svíčková na smetaně, knedlík",
                    menuEntry.text
            );

            // MenuEntry entry menuStatus
            Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                    groupMenuEntry.menuStatus);

            // Some extras
            Assert.assertEquals("Oběd", menuEntry.group);
            Assert.assertEquals(
                    "db/dbProcessOrder.jsp?time=1398258207155&ID=2&day=2014-05-02&type=make&week=null",
                    menuEntry.extra);
        }

        @Test
        public void disabledOrderEntry() {
            Action.MenuEntryAction action = orderEntries.get(0);

            Assert.assertEquals(1, action.reservedAmount);
            Assert.assertEquals(0, action.offeredAmount);
            Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
            Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
            Assert.assertEquals(menuEntries.get(5).relativeId, action.relativeMenuEntryId);
        }

        @Test
        public void orderEntry() {
            Action.MenuEntryAction action = orderEntries.get(1);

            Assert.assertEquals(1, action.reservedAmount);
            Assert.assertEquals(0, action.offeredAmount);
            Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
            Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
            Assert.assertEquals(menuEntries.get(7).relativeId, action.relativeMenuEntryId);
        }
    }

    @RunWith(Enclosed.class)
    public static class Version206 {

        public static class MenuPage01 {
            InputStream mIs;
            int mPortalVersion = 206;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");
            String mAllergenPattern = null;

            List<MenuEntry> menuEntries;
            List<GroupMenuEntry> groupMenuEntries;
            List<Action.MenuEntryAction> orderEntries;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("206/01/iCanteen.html");

                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(87500, parser.getCredit());

                menuEntries = parser.getMenuEntries();
                groupMenuEntries = parser.getGroupMenuEntries();
                orderEntries = parser.getActionEntries();
            }

            @Test
            public void wholeMenu() {
                Assert.assertEquals(24, menuEntries.size());
                Assert.assertEquals(4, orderEntries.size());
            }

            @Test
            public void disabledNotOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(1);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(1);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2014-04-02");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 2", menuEntry.label);
                Assert.assertEquals(2500, groupMenuEntry.price);
                Assert.assertEquals(
                        "pečený filet ze pstruha,bramb.kaše,zel.salát",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            @Test
            public void disabledOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(0);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2014-04-02");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2500, groupMenuEntry.price);
                Assert.assertEquals(
                        "polévka slepičí s játr.knedlíčky a nudlemi,hovězí plátek v cibulovo-česnekové omáčce,kynutý bramb.knedlík s mrkví a pohankou",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            @Test
            public void orderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(6);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(6);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2014-04-04");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2500, groupMenuEntry.price);
                Assert.assertEquals(
                        "polévka čočková,kuřecí plátek na paprice,těstoviny,ovoce",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
                Assert.assertEquals(
                        "db/dbProcessOrder.jsp?time=1396461851540&ID=152979624&day=2014-04-04&type=delete&week=null",
                        menuEntry.extra);
            }

            @Test
            public void notOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(12);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(12);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2014-04-08");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2500, groupMenuEntry.price);
                Assert.assertEquals(
                        "polévka hrachová,kuřecí plátek po srbsku,bramb.kaše,zelenina s cizrnou,ovoce",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
                Assert.assertEquals(
                        "db/dbProcessOrder.jsp?time=1396461851540&ID=3&day=2014-04-08&type=make&week=null",
                        menuEntry.extra);
            }

            @Test
            public void disabledOrderEntry() {
                Action.MenuEntryAction action = orderEntries.get(0);

                Assert.assertEquals(1, action.reservedAmount);
                Assert.assertEquals(0, action.offeredAmount);
                Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
                Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
                Assert.assertEquals(menuEntries.get(0).relativeId, action.relativeMenuEntryId);
            }

            @Test
            public void orderEntry() {
                Action.MenuEntryAction action = orderEntries.get(2);

                Assert.assertEquals(1, action.reservedAmount);
                Assert.assertEquals(0, action.offeredAmount);
                Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
                Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
                Assert.assertEquals(menuEntries.get(6).relativeId, action.relativeMenuEntryId);
            }

        }

        public static class MenuPage02 {

            InputStream mIs;
            int mPortalVersion = 206;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");
            String mAllergenPattern = null;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("206/02/iCanteen.html");
            }

            @Test
            public void parseInaccessibleMenu() throws Exception {
                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(60000, parser.getCredit());

                List<MenuEntry> menuEntries = parser.getMenuEntries();
                List<Action.MenuEntryAction> orderEntries = parser.getActionEntries();
            }
        }
    }

    public static class Version207 {

        InputStream mIs;
        int mPortalVersion = 207;
        String mAllergenPattern = null;
        boolean mAutoUpdate = false;
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

        List<MenuEntry> menuEntries;
        List<GroupMenuEntry> groupMenuEntries;
        List<Action.MenuEntryAction> orderEntries;

        @Before
        public void init() throws Exception {
            mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

            Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
            mIs = testContext.getAssets().open("207/iCanteen.htm");

            ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

            parser.parseData(mIs, mLogData);

            Assert.assertEquals(600, parser.getCredit());

            menuEntries = parser.getMenuEntries();
            groupMenuEntries = parser.getGroupMenuEntries();
            orderEntries = parser.getActionEntries();
        }

        @Test
        public void wholeMenu() {
            Assert.assertEquals(22, menuEntries.size());
            Assert.assertEquals(3, orderEntries.size());
        }

        @Test
        public void disabledNotOrderedMenuEntry() throws Exception {
            MenuEntry menuEntry = menuEntries.get(0);
            GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

            Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

            Date date = mFormatter.parse("2014-04-22");
            Assert.assertEquals(date.getTime(), menuEntry.date);
            Assert.assertEquals("Oběd 1", menuEntry.label);
            Assert.assertEquals(3800, groupMenuEntry.price);
            Assert.assertEquals(
                    "Polévka květáková, hovězí guláš, houskové knedlíky, ovoce, čaj,",
                    menuEntry.text
            );

            // MenuEntry entry menuStatus
            Assert.assertEquals(0,
                    groupMenuEntry.menuStatus);

            // Some extras
            Assert.assertEquals("Oběd", menuEntry.group);
        }

        @Test
        public void disabledOrderedMenuEntry() throws Exception {
            MenuEntry menuEntry = menuEntries.get(1);
            GroupMenuEntry groupMenuEntry = groupMenuEntries.get(1);

            Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

            Date date = mFormatter.parse("2014-04-22");
            Assert.assertEquals(date.getTime(), menuEntry.date);
            Assert.assertEquals("Oběd 2", menuEntry.label);
            Assert.assertEquals(3800, groupMenuEntry.price);
            Assert.assertEquals(
                    "Polévka květáková, kuřecí na kari, špagety, ovoce, čaj,",
                    menuEntry.text
            );

            // MenuEntry entry menuStatus
            Assert.assertEquals(0,
                    groupMenuEntry.menuStatus);
            // Some extras
            Assert.assertEquals("Oběd", menuEntry.group);
        }

        @Test
        public void orderedMenuEntry() throws Exception {
            MenuEntry menuEntry = menuEntries.get(7);
            GroupMenuEntry groupMenuEntry = groupMenuEntries.get(7);

            Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

            Date date = mFormatter.parse("2014-04-24");
            Assert.assertEquals(date.getTime(), menuEntry.date);
            Assert.assertEquals("Oběd 2", menuEntry.label);
            Assert.assertEquals(3800, groupMenuEntry.price);
            Assert.assertEquals(
                    "Polévka hovězí s těstovinami, rizoto z vepřového masa, salát mrkvový, čaj,",
                    menuEntry.text
            );

            // MenuEntry entry menuStatus
            Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                    groupMenuEntry.menuStatus);

            // Some extras
            Assert.assertEquals("Oběd", menuEntry.group);
            Assert.assertEquals(
                    "db/dbProcessOrder.jsp?time=1398093385044&ID=7163883&day=2014-04-24&type=delete&week=null",
                    menuEntry.extra);
        }

        // Test for not ordered menu entry is missing because of lack of test data

        @Test
        public void disabledOrderEntry() {
            Action.MenuEntryAction action = orderEntries.get(0);

            Assert.assertEquals(1, action.reservedAmount);
            Assert.assertEquals(0, action.offeredAmount);
            Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
            Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
            Assert.assertEquals(menuEntries.get(1).relativeId, action.relativeMenuEntryId);
        }

        @Test
        public void orderEntry() {
            Action.MenuEntryAction action = orderEntries.get(1);

            Assert.assertEquals(1, action.reservedAmount);
            Assert.assertEquals(0, action.offeredAmount);
            Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
            Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
            Assert.assertEquals(menuEntries.get(7).relativeId, action.relativeMenuEntryId);
        }
    }

    @RunWith(Enclosed.class)
    public static class Version210 {

        public static class MenuPage01 {

            InputStream mIs;
            int mPortalVersion = 210;
            String mAllergenPattern = null;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

            List<MenuEntry> menuEntries;
            List<GroupMenuEntry> groupMenuEntries;
            List<Action.MenuEntryAction> orderEntries;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("210/01/iCanteen.html");

                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(-39600, parser.getCredit());

                menuEntries = parser.getMenuEntries();
                groupMenuEntries = parser.getGroupMenuEntries();
                orderEntries = parser.getActionEntries();
            }

            @Test
            public void wholeMenu() {
                Assert.assertEquals(12, menuEntries.size());
                Assert.assertEquals(6, orderEntries.size());
            }

            @Test
            public void disabledNotOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(0);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2015-02-20");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2200, groupMenuEntry.price);
                Assert.assertEquals(
                        "Fazolová pikantní [9], ;kuskusové rizoto s krůtím masem [1, 3, 7, 9, 12], salát z červeného zelí, ovocná šťáva,",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            // Test of disabledOrderedMenuEntry is missing because of lack of test data

            @Test
            public void orderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(1);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(1);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2015-02-20");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 2", menuEntry.label);
                Assert.assertEquals(2200, groupMenuEntry.price);
                Assert.assertEquals(
                        "Fazolová pikantní [9], ;zapečená kotleta [1, 7], brambory vařené maštěné máslem [7], salát z červeného zelí, ovocná šťáva,",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
                Assert.assertEquals(
                        "db/dbProcessOrder.jsp?time=1424390767520&ID=29135753&day=2015-02-20&type=delete&week=&terminal=false&keyboard=false&printer=false",
                        menuEntry.extra);
            }

            // Test of notOrderedMenuEntry is missing because of lack of test data

            // Test of disabledOrderEntry is missing because of lack of test data

            @Test
            public void orderEntry() {
                Action.MenuEntryAction action = orderEntries.get(0);

                Assert.assertEquals(1, action.reservedAmount);
                Assert.assertEquals(0, action.offeredAmount);
                Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
                Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
                Assert.assertEquals(menuEntries.get(1).relativeId, action.relativeMenuEntryId);
            }
        }

        public static class MenuPage02 {

            InputStream mIs;
            int mPortalVersion = 210;
            String mAllergenPattern = null;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

            List<MenuEntry> menuEntries;
            List<GroupMenuEntry> groupMenuEntries;
            List<Action.MenuEntryAction> orderEntries;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("210/02/month.html");

                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(87500, parser.getCredit());

                menuEntries = parser.getMenuEntries();
                groupMenuEntries = parser.getGroupMenuEntries();
                orderEntries = parser.getActionEntries();
            }

            @Test
            public void wholeMenu() {
                Assert.assertEquals(2, menuEntries.size());
                Assert.assertEquals(0, orderEntries.size());
            }

            @Test
            public void disabledNotOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(0);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2017-06-30");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2500, groupMenuEntry.price);
                Assert.assertEquals(
                        "Slepičí polévka s těstovinami [1, 9], ;vepřový závitek přírodní(faleš.) [1, 3, 7], rýže dušená, zelenina, čaj černý, citrónová voda, sirup, ovocné mléko [7], výdej 10, 30-13, 00 hod.",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            // Test of disabledOrderedMenuEntry is missing because of lack of test data

            // Test of orderedMenuEntry is missing because of lack of test data

            // Test of notOrderedMenuEntry is missing because of lack of test data

            // Test of disabledOrderEntry is missing because of lack of test data

            // Test of orderEntry is missing because of lack of test data
        }

        public static class MenuPage03 {

            InputStream mIs;
            int mPortalVersion = 210;
            String mAllergenPattern = null;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

            List<MenuEntry> menuEntries;
            List<GroupMenuEntry> groupMenuEntries;
            List<Action.MenuEntryAction> orderEntries;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("210/03/iCanteen.html");

                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(35000, parser.getCredit());

                menuEntries = parser.getMenuEntries();
                groupMenuEntries = parser.getGroupMenuEntries();
                orderEntries = parser.getActionEntries();
            }

            @Test
            public void wholeMenu() {
                Assert.assertEquals(30, menuEntries.size());
                Assert.assertEquals(10, orderEntries.size());
            }

            @Test
            public void disabledNotOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(1);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(1);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2017-10-11");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 2", menuEntry.label);
                Assert.assertEquals(2500, groupMenuEntry.price);
                Assert.assertEquals(
                        "Zeleninová polévka [6, 7, 9], ;fazolový guláš s bramborem a klobásou [7], chléb [1], moučník, čaj černý, citrónová voda, sirup, ovocné mléko [7],",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            @Test
            public void disabledOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(0);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2017-10-11");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2500, groupMenuEntry.price);
                Assert.assertEquals(
                        "Zeleninová polévka [6, 7, 9], ;krůtí plátek v hroznové omáčce [1, 6], bramborová kaše [7], zel.salát, čaj černý, citrónová voda, sirup, ovocné mléko [7],",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);

                Action.MenuEntryAction action = orderEntries.get(0);

                Assert.assertEquals(1, action.reservedAmount);
                Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
                Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
                Assert.assertEquals(menuEntry.relativeId, action.relativeMenuEntryId);
            }

            @Test
            public void orderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(27);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(27);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2017-10-25");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2500, groupMenuEntry.price);
                Assert.assertEquals(
                        "Slepičí polévka se strouháním [1, 9], ;segedínský guláš [1, 6, 7, 12], houskové knedlíky (kynuté) [1, 3, 7], ovoce, čaj černý, citrónová voda [12], sirup, ovocné mléko [7],",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
                Assert.assertEquals(
                        "db/dbProcessOrder.jsp?time=1507739939293&ID=3&day=2017-10-25&type=make&week=&terminal=false&keyboard=false&printer=false",
                        menuEntry.extra);
            }

            //TODO test change of order

            // Test of notOrderedMenuEntry is missing because of lack of test data

            // Test of disabledOrderEntry is missing because of lack of test data

            // Test of orderEntry is missing because of lack of test data
        }
    }

    @RunWith(Enclosed.class)
    public static class Version213 {

        public static class MenuPage01 {

            InputStream mIs;
            int mPortalVersion = 213;
            String mAllergenPattern = null;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

            List<MenuEntry> menuEntries;
            List<GroupMenuEntry> groupMenuEntries;
            List<Action.MenuEntryAction> orderEntries;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("213/01/iCanteen.html");

                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(43200, parser.getCredit());

                menuEntries = parser.getMenuEntries();
                groupMenuEntries = parser.getGroupMenuEntries();
                orderEntries = parser.getActionEntries();
            }

            @Test
            public void wholeMenu() {
                Assert.assertEquals(2, menuEntries.size());
                Assert.assertEquals(0, orderEntries.size());
            }

            @Test
            public void disabledNotOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(0);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2017-06-30");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2400, groupMenuEntry.price);
                Assert.assertEquals(
                        "Z kysaného zelí [1, 7], ;těstovinová musaka (těst. s masem a zeleninou) [1, 3, 6, 7], červená řepa, voda s citronem",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            // Test of disabledOrderedMenuEntry is missing because of lack of test data

            // Test of orderedMenuEntry is missing because of lack of test data

            // Test of notOrderedMenuEntry is missing because of lack of test data

            // Test of disabledOrderEntry is missing because of lack of test data

            // Test of orderEntry is missing because of lack of test data
        }

        public static class MenuPage02 {

            InputStream mIs;
            int mPortalVersion = 213;
            String mAllergenPattern = null;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

            List<MenuEntry> menuEntries;
            List<GroupMenuEntry> groupMenuEntries;
            List<Action.MenuEntryAction> orderEntries;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("213/02/month.html");

                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(NO_INFO, parser.getCredit());

                menuEntries = parser.getMenuEntries();
                groupMenuEntries = parser.getGroupMenuEntries();
                orderEntries = parser.getActionEntries();
            }

            @Test
            public void wholeMenu() {
                Assert.assertEquals(24, menuEntries.size());
                Assert.assertEquals(0, orderEntries.size());
            }

            @Test
            public void disabledNotOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(0);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2017-09-29");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2600, groupMenuEntry.price);
                Assert.assertEquals(
                        "polévka s jáhl. vločkami, vepřové maso pečené dušená mrkev, brambory",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            // Test of disabledOrderedMenuEntry is missing because of lack of test data

            // Test of orderedMenuEntry is missing because of lack of test data

            @Test
            public void notOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(1);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(1);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2017-10-02");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2600, groupMenuEntry.price);
                Assert.assertEquals(
                        "polévka cibulová, zapečené těstoviny se šunkou, okurkový salát",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
                Assert.assertEquals(
                        "db/dbProcessOrder.jsp?time=1506645839442&ID=1&day=2017-10-02&type=make&week=&terminal=false&keyboard=false&printer=false",
                        menuEntry.extra);
            }

            // Test of disabledOrderEntry is missing because of lack of test data

            // Test of orderEntry is missing because of lack of test data
        }

        public static class MenuPage03 {

            InputStream mIs;
            int mPortalVersion = 213;
            String mAllergenPattern = null;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

            List<MenuEntry> menuEntries;
            List<GroupMenuEntry> groupMenuEntries;
            List<Action.MenuEntryAction> orderEntries;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("213/03/month.html");

                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(12000, parser.getCredit());

                menuEntries = parser.getMenuEntries();
                groupMenuEntries = parser.getGroupMenuEntries();
                orderEntries = parser.getActionEntries();
            }

            @Test
            public void wholeMenu() {
                Assert.assertEquals(16, menuEntries.size());
                Assert.assertEquals(8, orderEntries.size());
            }

            @Test
            public void disabledNotOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(0);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2017-10-11");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2400, groupMenuEntry.price);
                Assert.assertEquals(
                        "Hrstková, ;moravský vrabec, zelí dušené [1], houskový knedlík [1, 3, 7], voda s citronem",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            @Test
            public void disabledOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(1);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(1);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2017-10-11");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 2", menuEntry.label);
                Assert.assertEquals(2400, groupMenuEntry.price);
                Assert.assertEquals(
                        "Hrstková, ;vepřové na kmíně [1], kolínka [1, 3], voda s citronem",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);

                Action.MenuEntryAction action = orderEntries.get(0);

                Assert.assertEquals(1, action.reservedAmount);
                Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
                Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
                Assert.assertEquals(menuEntry.relativeId, action.relativeMenuEntryId);
            }

            //TODO more
            // Test of orderedMenuEntry is missing because of lack of test data

            // Test of notOrderedMenuEntry is missing because of lack of test data

            // Test of disabledOrderEntry is missing because of lack of test data

            // Test of orderEntry is missing because of lack of test data
        }
    }

    @RunWith(Enclosed.class)
    public static class Version214 {

        public static class MenuPage01 {

            InputStream mIs;
            int mPortalVersion = 214;
            String mAllergenPattern = null;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

            List<MenuEntry> menuEntries;
            List<GroupMenuEntry> groupMenuEntries;
            List<Action.MenuEntryAction> orderEntries;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("214/01/iCanteen214.html");

                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(PublicProviderContract.NO_INFO, parser.getCredit());

                menuEntries = parser.getMenuEntries();
                groupMenuEntries = parser.getGroupMenuEntries();
                orderEntries = parser.getActionEntries();
            }

            @Test
            public void wholeMenu() {
                Assert.assertEquals(15, menuEntries.size());
                Assert.assertEquals(0, orderEntries.size());
            }

            @Test
            public void disabledNotOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(0);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2018-06-18");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2600, groupMenuEntry.price);
                Assert.assertEquals(
                        "polévka cibulová, vep. maso v mrkvi, brambory",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            // Test of disabledOrderedMenuEntry is missing because of lack of test data

            // Test of orderedMenuEntry is missing because of lack of test data

            @Test
            public void notOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(2);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(2);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2018-06-19");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2600, groupMenuEntry.price);
                Assert.assertEquals(
                        "polévka rychlá s vejcem, hrachová kaše, kuřecí roláda, salát z kys. zelí",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
                Assert.assertEquals(
                        "db/dbProcessOrder.jsp?time=1529272174025&token=EF4F2jQQnhaYs0GcB30QXUjQo1ROGIiQrvUnE8p7WY4%3D&ID=1&day=2018-06-19&type=make&week=&terminal=false&keyboard=false&printer=false",
                        menuEntry.extra);
            }

            // Test of disabledOrderEntry is missing because of lack of test data

            // Test of orderEntry is missing because of lack of test data
        }

        public static class MenuPage02 {

            InputStream mIs;
            int mPortalVersion = 214;
            String mAllergenPattern = null;
            boolean mAutoUpdate = false;
            @SuppressLint("SimpleDateFormat")
            SimpleDateFormat mFormatter = new SimpleDateFormat("yyyy-MM-dd");

            List<MenuEntry> menuEntries;
            List<GroupMenuEntry> groupMenuEntries;
            List<Action.MenuEntryAction> orderEntries;

            @Before
            public void init() throws Exception {
                mFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));

                Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
                mIs = testContext.getAssets().open("214/02/iCanteen214_02.html");

                ICanteenMenuParser parser = new ICanteenMenuParser(mPortalVersion, mAllergenPattern, mAutoUpdate);

                parser.parseData(mIs, mLogData);

                Assert.assertEquals(PublicProviderContract.NO_INFO, parser.getCredit());

                menuEntries = parser.getMenuEntries();
                groupMenuEntries = parser.getGroupMenuEntries();
                orderEntries = parser.getActionEntries();
            }

            @Test
            public void wholeMenu() {
                Assert.assertEquals(10, menuEntries.size());
                Assert.assertEquals(4, orderEntries.size());
            }

            @Test
            public void disabledNotOrderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(0);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(0);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2018-06-18");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2400, groupMenuEntry.price);
                Assert.assertEquals(
                        "Maďarská rybí [4, 9], ;mletý řízek se sýrem [1, 3, 6, 7], bramborová kaše maštěná máslem [7], ledový salát s rajčaty a mrkví, voda s citronem, mléko [7],",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(0,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
            }

            // Test of disabledOrderedMenuEntry is missing because of lack of test data

            @Test
            public void orderedMenuEntry() throws Exception {
                MenuEntry menuEntry = menuEntries.get(2);
                GroupMenuEntry groupMenuEntry = groupMenuEntries.get(2);

                Assert.assertEquals(menuEntry.relativeId, groupMenuEntry.menuEntryRelativeId);

                Date date = mFormatter.parse("2018-06-19");
                Assert.assertEquals(date.getTime(), menuEntry.date);
                Assert.assertEquals("Oběd 1", menuEntry.label);
                Assert.assertEquals(2400, groupMenuEntry.price);
                Assert.assertEquals(
                        "S masem a rýží [9], ;kuřecí po znojemsku (vejce,párek), kolínka [1, 3], frutelka (s ovocným podílem), ovoce",
                        menuEntry.text
                );

                // MenuEntry entry menuStatus
                Assert.assertEquals(MENU_STATUS_ORDERABLE | MENU_STATUS_CANCELABLE,
                        groupMenuEntry.menuStatus);

                // Some extras
                Assert.assertEquals("Oběd", menuEntry.group);
                Assert.assertEquals(
                        "db/dbProcessOrder.jsp?time=1529272301501&token=HyUwIYLNkUctim2tA7ocY2NdgEuvmCTpLcfOLMDzFFY%3D&ID=40433111&day=2018-06-19&type=delete&week=&terminal=false&keyboard=false&printer=false",
                        menuEntry.extra);
            }

            // Test of notOrderedMenuEntry is missing because of lack of test data

            // Test of disabledOrderEntry is missing because of lack of test data

            @Test
            public void orderEntry() {
                Action.MenuEntryAction action = orderEntries.get(0);

                Assert.assertEquals(1, action.reservedAmount);
                Assert.assertEquals(0, action.offeredAmount);
                Assert.assertEquals(ACTION_SYNC_STATUS_SYNCED, action.syncStatus);
                Assert.assertEquals(ACTION_ENTRY_TYPE_STANDARD, action.entryType);
                Assert.assertEquals(menuEntries.get(1).relativeId, action.relativeMenuEntryId);
            }
        }
    }
}
