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

package cz.maresmar.sfm.service.web;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.InputStream;
import java.util.Scanner;

/**
 * Tests {@link FirstLineInputStream}
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class FirstLineInputTests {

    private FirstLineInputStream fis;
    private Context testContext;

    @Before
    public void init() {
        testContext = InstrumentationRegistry.getInstrumentation().getContext();
    }

    @After
    public void finish() throws Exception {
        fis.close();
    }

    public void open(String file) throws Exception {
        InputStream is = testContext.getAssets().open(file);

        fis = new FirstLineInputStream(is);
    }

    @Test
    public void simpleRead() throws Exception {
        open("FirstLineInputStream/simple.txt");

        Assert.assertEquals('f', fis.read());
        Assert.assertEquals('i', fis.read());
        Assert.assertEquals('r', fis.read());
        Assert.assertEquals('s', fis.read());
        Assert.assertEquals('t', fis.read());
        Assert.assertEquals(-1, fis.read());
        Assert.assertEquals(-1, fis.read());
    }

    @Test
    public void oneLine() throws Exception {
        open("FirstLineInputStream/oneLine.txt");

        Assert.assertEquals('f', fis.read());
        Assert.assertEquals('i', fis.read());
        Assert.assertEquals('r', fis.read());
        Assert.assertEquals('s', fis.read());
        Assert.assertEquals('t', fis.read());
        Assert.assertEquals(-1, fis.read());
        Assert.assertEquals(-1, fis.read());
    }

    @Test
    public void withFreeLine() throws Exception {
        open("FirstLineInputStream/withFreeLine.txt");

        Assert.assertEquals('f', fis.read());
        Assert.assertEquals('i', fis.read());
        Assert.assertEquals('r', fis.read());
        Assert.assertEquals('s', fis.read());
        Assert.assertEquals('t', fis.read());
        Assert.assertEquals(-1, fis.read());
        Assert.assertEquals(-1, fis.read());
    }

    @Test
    public void usingScanner01() throws Exception {
        open("FirstLineInputStream/simple.txt");

        Scanner sc = new Scanner(fis);
        Assert.assertEquals("first", sc.nextLine());
        Assert.assertFalse(sc.hasNext());
    }

    @Test
    public void usingScanner02() throws Exception {
        open("FirstLineInputStream/withFreeLine.txt");

        Scanner sc = new Scanner(fis);
        Assert.assertEquals("first", sc.nextLine());
        Assert.assertFalse(sc.hasNext());
    }

}
