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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import cz.maresmar.sfm.app.ServerContract;
import cz.maresmar.sfm.service.web.FirstLineInputStream;
import cz.maresmar.sfm.service.web.PortalEntry;
import cz.maresmar.sfm.service.web.PortalsUpdateService;
import cz.maresmar.sfm.service.web.SfmJsonPortalResponse;
import cz.maresmar.sfm.service.web.UnsupportedApiException;

/**
 * Tests {@link PortalsUpdateService}
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class PortalUpdateServiceTests {

    @Test
    public void fileJSONDataParse() throws Exception {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream is = testContext.getAssets().open("PortalUpdateService/portalJson.txt");

        FirstLineInputStream fis = new FirstLineInputStream(is);

        SfmJsonPortalResponse response = PortalsUpdateService
                .parseEntries(ServerContract.REPLY_TYPE_PORTALS, SfmJsonPortalResponse.class, fis);
        List<PortalEntry> portalEntries = response.data;

        Assert.assertEquals(1,portalEntries.size());

        PortalEntry entry = portalEntries.get(0);
        Assert.assertEquals("Example",entry.name);
        Assert.assertEquals(null,entry.extra);
        Assert.assertEquals(49.400301,entry.locN,  1e-15);
    }

    @Test(expected = IOException.class)
    public void invalidFileJSONDataParse() throws Exception {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream is = testContext.getAssets().open("PortalUpdateService/invalidPortalJson.txt");

        FirstLineInputStream fis = new FirstLineInputStream(is);

        SfmJsonPortalResponse response = PortalsUpdateService
                .parseEntries(ServerContract.REPLY_TYPE_PORTALS, SfmJsonPortalResponse.class, fis);
        List<PortalEntry> portalEntries = response.data;
    }

    @Test(expected = UnsupportedApiException.class)
    public void apiUpdateJSONDataParse() throws Exception {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        InputStream is = testContext.getAssets().open("PortalUpdateService/apiUpdateJson.txt");

        FirstLineInputStream fis = new FirstLineInputStream(is);

        SfmJsonPortalResponse response = PortalsUpdateService
                .parseEntries(ServerContract.REPLY_TYPE_PORTALS, SfmJsonPortalResponse.class, fis);
        List<PortalEntry> portalEntries = response.data;
    }
}
