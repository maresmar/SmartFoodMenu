package cz.maresmar.sfm.builtin.parser;

import android.content.Context;
import android.support.test.InstrumentationRegistry;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Tests of {@link EnclosedXmlParser}
 */

public class EnclosedXmlParserTests {

    InputStream mIs;
    //
    DummyEnclosedXmlParser mParser;

    @Before
    public void init() throws Exception {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        mIs = testContext.getAssets().open("testFile.html");
        mParser = new DummyEnclosedXmlParser(mIs);
    }

    @Test
    public void simpleFindElement() throws Exception {
        mParser.findElement("html", null);
        mParser.findElementID("div", "first", null);
        mParser.findElementClass("div", "foo", null);
        mParser.findElement("div", "class", "sth", EnclosedXmlParser.MATCH_END, null);
        mParser.findElement("div", "class", "last", EnclosedXmlParser.MATCH_EQUAL, null);
    }

    @Test(expected = XmlPullParserException.class)
    public void elementNotFound() throws Exception {
        mParser.findElement("foo", null);
    }

    @Test(expected = XmlPullParserException.class)
    public void elementIdNotFound() throws Exception {
        mParser.findElementID("div", "last", null);
    }

    @Test
    public void valueEndingsTest() throws Exception {
        mParser.findElement("div", "id", "irst", EnclosedXmlParser.MATCH_END, null);
        mParser.findElement("div", "id", "secon", EnclosedXmlParser.MATCH_START, null);
    }

    @Test(expected = XmlPullParserException.class)
    public void valueEndsInsteadOfEquals() throws Exception {
        mParser.findElement("div", "id", "irst", EnclosedXmlParser.MATCH_EQUAL, null);
    }

    @Test(expected = XmlPullParserException.class)
    public void valueEndsInsteadOfStart() throws Exception {
        mParser.findElement("div", "id", "irst", EnclosedXmlParser.MATCH_START, null);
    }

    @Test
    public void findFondCompareEmptyElement() throws Exception {
        mParser.findElement("html", null);
        Assert.assertTrue(mParser.isFoundElement("html", null, null, EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertFalse(mParser.isFoundElement("html", "id", "bar", EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertFalse(mParser.isFoundElement("html", "id", "", EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertTrue(mParser.isFoundElement("html", "id", null, EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertFalse(mParser.isFoundElement("foo", null, null, EnclosedXmlParser.MATCH_EQUAL));
    }

    @Test
    public void findFoundCompare() throws Exception {
        mParser.findElementID("div", "second", null);
        Assert.assertTrue(mParser.isFoundElement("div", null, null, EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertFalse(mParser.isFoundElement("div", "id", "", EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertTrue(mParser.isFoundElement("div", "id", null, EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertFalse(mParser.isFoundElement("div", "id", "", EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertTrue(mParser.isFoundElement("div", "id", "second", EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertTrue(mParser.isFoundElement("div", "id", "second", EnclosedXmlParser.MATCH_START));
        Assert.assertTrue(mParser.isFoundElement("div", "id", "second", EnclosedXmlParser.MATCH_END));
        Assert.assertFalse(mParser.isFoundElement("div", "id", "first", EnclosedXmlParser.MATCH_EQUAL));
        Assert.assertFalse(mParser.isFoundElement("foo", null, null, EnclosedXmlParser.MATCH_EQUAL));
    }

    @Test
    public void findFoundInside() throws Exception {
        mParser.findElementID("div", "big", null);
        Assert.assertTrue(mParser.isFoundElement("div", "id", "big", EnclosedXmlParser.MATCH_EQUAL));

        Assert.assertEquals(XmlPullParser.START_TAG, mParser.findElement("div", "div"));
        Assert.assertTrue(mParser.isFoundElement("div", "id", "firstInner", EnclosedXmlParser.MATCH_EQUAL));

        // <hr> is not found because <div id="firstElement"> ends </div>
        Assert.assertEquals(XmlPullParser.END_TAG, mParser.findElement("hr", "div"));
        Assert.assertTrue(mParser.isFoundElement("div", null, null, EnclosedXmlParser.MATCH_EQUAL));

    }

    @Test
    public void findNext() throws Exception {
        mParser.findElement("div", "class", "last", EnclosedXmlParser.MATCH_EQUAL, null);
        mParser.findNextType(XmlPullParser.END_TAG);
        Assert.assertTrue(mParser.isFoundElement("div", null, null, EnclosedXmlParser.MATCH_EQUAL));
    }

    class DummyEnclosedXmlParser extends EnclosedXmlParser {
        public DummyEnclosedXmlParser(InputStream is) throws XmlPullParserException {
            super(EnclosedXmlParser.creteHtmlPullParser(is));
        }

        public int next() throws IOException, XmlPullParserException {
            return mParser.next();
        }

        public String getText() {
            return mParser.getText();
        }
    }
}
