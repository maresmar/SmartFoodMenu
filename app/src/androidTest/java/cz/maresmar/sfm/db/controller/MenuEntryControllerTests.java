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

package cz.maresmar.sfm.db.controller;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import cz.maresmar.sfm.db.DbHelper;

/**
 * Test some methods of {@link MenuEntryController} that is not really testable from higher level of abstraction
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MenuEntryControllerTests {

    private SQLiteDatabase db;
    private Context testContext;

    @Before
    public void init() {
        testContext = InstrumentationRegistry.getTargetContext();

        DbHelper helper = new DbHelper(testContext);
        db = helper.getWritableDatabase();
    }

    @After
    public void finish() {
        db.close();
    }

    @Test
    public void testFindOrInsertGroupMenu() {
        long entryId = SimpleMenuEntryController.findOrInsertMenuGroup(db, "testGroup");

        Assert.assertTrue("Hasn't any value", entryId >= 0);

        long nextId = SimpleMenuEntryController.findOrInsertMenuGroup(db, "testGroup");
        Assert.assertEquals("Inserted new value", entryId, nextId);

        nextId = SimpleMenuEntryController.findOrInsertMenuGroup(db, "testGroup");
        Assert.assertEquals("Inserted new value on second try", entryId, nextId);
    }

    @Test
    public void testDifferentValue() {
        long entryId = SimpleMenuEntryController.findOrInsertMenuGroup(db, "testGroup");
        long anotherEntryId = SimpleMenuEntryController.findOrInsertMenuGroup(db, "testGroup2");

        Assert.assertNotEquals(entryId, anotherEntryId);
    }

    @Test
    public void testFindOrInsertFood() {
        long entryId = SimpleMenuEntryController.findOrInsertFood(db, "testFood");

        Assert.assertTrue("Hasn't any value", entryId >= 0);

        long nextId = SimpleMenuEntryController.findOrInsertFood(db, "testFood");
        Assert.assertEquals("Inserted new value", entryId, nextId);

        nextId = SimpleMenuEntryController.findOrInsertFood(db, "testFood");
        Assert.assertEquals("Inserted new value on second try", entryId, nextId);
    }
}
