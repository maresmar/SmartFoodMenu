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

package cz.maresmar.sfm.builtin.parser;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.util.Xml;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Retention;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * HTML parser that asks {@link XmlPullParser} for data. Works as SAX parser allowing to finds HTML
 * elements inside another element
 * <p>
 * This is often better approach, because HTML pages are not always valid. Browsers usually can read
 * pages that not valid but not so many parsers has defined how they read non-valid pages.
 * </p>
 */
public class EnclosedXmlParser {

    protected static final String ns = null;

    public static final int MATCH_EQUAL = 0;
    public static final int MATCH_START = 1;
    public static final int MATCH_END = 2;
    public static final int MATCH_CONTAINS = 3;

    @Retention(SOURCE)
    @IntDef({MATCH_EQUAL, MATCH_START, MATCH_END, MATCH_CONTAINS})
    @interface MatchType {
    }

    /**
     * Internal XML parser
     */
    protected XmlPullParser mParser;

    /**
     * Create new parser with from {@link XmlPullParser}
     *
     * @param parser Used {@link XmlPullParser}
     */
    public EnclosedXmlParser(@NonNull XmlPullParser parser) {
        this.mParser = parser;
    }

    /**
     * Create new parser that will be fully initialized later
     */
    protected EnclosedXmlParser() {
    }

    /**
     * Initialize {@link XmlPullParser} that can be used in this class for parsing HTML pages
     *
     * @param is Data {@link InputStream}
     * @return {@link XmlPullParser} based on {@link InputStream}
     * @throws XmlPullParserException if some parsing feature is not supported
     */
    protected static XmlPullParser creteHtmlPullParser(@NonNull InputStream is) throws XmlPullParserException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setValidating(false);
        factory.setFeature(Xml.FEATURE_RELAXED, true);
        factory.setNamespaceAware(false);

        XmlPullParser parser = factory.newPullParser();
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        parser.setInput(is, null);
        parser.defineEntityReplacementText("nbsp", " ");
        parser.defineEntityReplacementText("#39", "'");
        return parser;
    }

    /**
     * Finds next elements inside some element
     *
     * @param name          Name of element to be found
     * @param insideElement Name of element's end that will stop searching
     * @return {@link XmlPullParser#START_TAG} if element is found, {@link XmlPullParser#END_TAG} if
     * insideElement's end is found
     * @throws XmlPullParserException Thrown when element or inside element wasn't found
     * @throws IOException            Thrown if IO Exception occurs
     */
    public int findElement(@NonNull String name, @Nullable String insideElement) throws XmlPullParserException, IOException {
        return findElement(name, "id", null, MATCH_EQUAL, insideElement);
    }

    /**
     * Finds next elements with specific ID inside some element
     *
     * @param name          Name of element to be found
     * @param id            ID of element to be found
     * @param insideElement Name of element's end that will stop searching
     * @return {@link XmlPullParser#START_TAG} if element is found, {@link XmlPullParser#END_TAG} if
     * insideElement's end is found
     * @throws XmlPullParserException Thrown when element or inside element wasn't found
     * @throws IOException            Thrown if IO Exception occurs
     */
    public int findElementID(@NonNull String name, @NonNull String id, @Nullable String insideElement) throws XmlPullParserException, IOException {
        return findElement(name, "id", id, MATCH_EQUAL, insideElement);
    }

    /**
     * Finds next elements with specific class inside some element
     *
     * @param name          Name of element to be found
     * @param cls           Class of element to be found
     * @param insideElement Name of element's end that will stop searching
     * @return {@link XmlPullParser#START_TAG} if element is found, {@link XmlPullParser#END_TAG} if
     * insideElement's end is found
     * @throws XmlPullParserException Thrown when element or inside element wasn't found
     * @throws IOException            Thrown if IO Exception occurs
     */
    public int findElementClass(@NonNull String name, @NonNull String cls, @Nullable String insideElement) throws XmlPullParserException, IOException {
        return findElement(name, "class", cls, MATCH_CONTAINS, insideElement);
    }

    /**
     * Finds next elements with specific key value pair inside some element
     *
     * @param name          Name of element to be found
     * @param fieldName     Key of element to be found
     * @param fieldVal      Value of key  to be found
     * @param matchType     Match type of key's value
     * @param insideElement Name of element's end that will stop searching
     * @return {@link XmlPullParser#START_TAG} if element is found, {@link XmlPullParser#END_TAG} if
     * insideElement's end is found
     * @throws XmlPullParserException Thrown when element or inside element wasn't found
     * @throws IOException            Thrown if IO Exception occurs
     */
    public int findElement(@NonNull String name, String fieldName, String fieldVal, @MatchType int matchType, @Nullable String insideElement) throws XmlPullParserException, IOException {
        int type;
        while ((type = mParser.next()) != XmlPullParser.END_DOCUMENT) {
            switch (type) {
                case XmlPullParser.END_TAG:
                    if (isFoundElement(insideElement, null, null, MATCH_EQUAL))
                        return type;
                    break;
                case XmlPullParser.START_TAG:
                    if (isFoundElement(name, fieldName, fieldVal, matchType))
                        return type;
                    break;
            }
        }
        if (fieldVal == null) {
            throw new XmlPullParserException("Element \"" + name + "\" not found");
        } else {
            throw new XmlPullParserException("Element \"" + name + "\" (" + fieldName + "=\"" + fieldVal + "\") not found");
        }
    }

    /**
     * Tests if actual element follow specification
     *
     * @param elementName Name of element
     * @param fieldName   One of element's field name, if {@code null} no restriction is applied
     * @param fieldVal    Value of field, if value is {@code null} and {@code MATCH_EQUAL} is used
     *                    returns {@code true} only if field value is {@code null}, otherwise it returns
     *                    {@code true} always
     * @param matchType   Match type of field's value
     * @return true if element follow specification, false otherwise
     */
    public boolean isFoundElement(@Nullable String elementName, @Nullable String fieldName, @Nullable String fieldVal, @MatchType int matchType) {
        String foundElName = mParser.getName();

        // Relax compare to ignore case
        if (foundElName.equals(elementName)) {
            if (fieldName == null || fieldVal == null)
                return true;

            String foundElValue = mParser.getAttributeValue(ns, fieldName);
            switch (matchType) {
                case MATCH_EQUAL:
                    return fieldVal.equals(foundElValue);
                case MATCH_START:
                    return foundElValue != null && foundElValue.startsWith(fieldVal);
                case MATCH_END:
                    return foundElValue != null && foundElValue.endsWith(fieldVal);
                case MATCH_CONTAINS:
                    return foundElValue != null && foundElValue.contains(fieldVal);
                default:
                    throw new IllegalArgumentException("Wrong matchType value");
            }
        } else
            return false;
    }

    /**
     * Finds next tag with specific type
     *
     * @param tagType Type of tag
     * @throws XmlPullParserException Thrown when tag with specific type wasn't found
     * @throws IOException            Thrown if IO Exception occurs
     * @see XmlPullParser#next()
     */
    public void findNextType(int tagType) throws XmlPullParserException, IOException {
        int type;
        while ((type = mParser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == tagType)
                return;
        }
        throw new XmlPullParserException("Tag type not found", mParser, null);
    }


    /**
     * Returns text under parser
     *
     * @return Read text
     * @throws XmlPullParserException Thrown when parser is not on {@link XmlPullParser#TEXT}
     * @throws IOException            Thrown if IO Exception occurs
     * @see XmlPullParser#next()
     */
    protected String readText() throws IOException, XmlPullParserException {
        if (mParser.next() == XmlPullParser.TEXT) {
            return mParser.getText();
        } else {
            throw new XmlPullParserException("Cannot read text from non TEXT type", mParser, null);
        }
    }

    /**
     * Returns first text after parser
     *
     * @return Read text
     * @throws XmlPullParserException Thrown when parser is not on {@link XmlPullParser#TEXT}
     * @throws IOException            Thrown if IO Exception occurs
     * @see XmlPullParser#next()
     */
    protected String readFirstText() throws IOException, XmlPullParserException {
        int type;
        while ((type = mParser.next()) != XmlPullParser.END_DOCUMENT) {
            if (type == XmlPullParser.TEXT) {
                return mParser.getText();
            }
        }
        throw new XmlPullParserException("Cannot find any TEXT type", mParser, null);
    }
}
