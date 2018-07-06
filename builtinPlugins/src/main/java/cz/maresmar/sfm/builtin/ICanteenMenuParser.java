package cz.maresmar.sfm.builtin;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import cz.maresmar.sfm.builtin.parser.EnclosedXmlParser;
import cz.maresmar.sfm.plugin.model.Action;
import cz.maresmar.sfm.plugin.model.GroupMenuEntry;
import cz.maresmar.sfm.plugin.model.LogData;
import cz.maresmar.sfm.plugin.model.MenuEntry;
import cz.maresmar.sfm.plugin.service.ServerMaintainException;
import cz.maresmar.sfm.plugin.service.WebPageFormatChangedException;
import cz.maresmar.sfm.provider.PublicProviderContract;
import cz.maresmar.sfm.provider.PublicProviderContract.MenuStatus;

/**
 * Parser than can parse menu from specific web page using {@link EnclosedXmlParser}
 */
public class ICanteenMenuParser extends EnclosedXmlParser {

    // Constants

    private static final @MenuStatus
    int FEATURES_DEFAULT_ENABLE_FOOD =
            PublicProviderContract.MENU_STATUS_ORDERABLE | PublicProviderContract.MENU_STATUS_CANCELABLE;
    private static final @MenuStatus
    int FEATURES_DISABLE_FOOD = 0;

    private static final String ORDER_URL_DISABLED = "-disabledUrl-";

    private static final Pattern FOOD_LABEL_PATTERN = Pattern.compile(" *(.*[^ ]) +([0-9]+) *");
    private static final int FOOD_LABEL_PATTERN_GROUP_NAME = 1;
    private static final int FOOD_LABEL_PATTERN_POSITION_IN_GROUP = 2;

    private static final String MULTI_SPACE_TRIM_PATTERN = " *( ([,.]|)) *";
    private static final String MULTI_SPACE_TRIM_REPLACE = "$2 ";

    private static final int REPARSE_READ_LIMIT = 2_000_000;

    // Parser config

    private int mPortalVersion;
    private String mAllergyPattern;
    private boolean mPortalAutoUpdate;

    // Parser internal data

    private BufferedInputStream mIs;
    private long mLastDate = 0;
    private HashMap<String, Integer> mLastDateIds;

    // Parser inner data

    private int mCredit = PublicProviderContract.NO_INFO;
    private List<MenuEntry> mMenuEntries;
    private List<Action.MenuEntryAction> mActionEntries;
    private List<GroupMenuEntry> mGroupMenuEntries;
    private LogData mParseContext;

    /**
     * Create new menu parser
     *
     * @param portalVersion  Portal version to be parsed
     * @param allergyPattern Regex pattern that will be removed from food
     * @param autoUpdate     Tels if portal can autodetect portal updates
     */
    public ICanteenMenuParser(int portalVersion, String allergyPattern, boolean autoUpdate) {
        mPortalVersion = portalVersion;
        mPortalAutoUpdate = autoUpdate;
        mAllergyPattern = allergyPattern;
    }

    private void resetParser() throws XmlPullParserException, IOException {
        XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
        factory.setValidating(false);
        factory.setFeature(Xml.FEATURE_RELAXED, true);
        factory.setNamespaceAware(false);
        mParser = factory.newPullParser();
        mParser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
        mParser.setInput(mIs, null);
        mParser.defineEntityReplacementText("nbsp", " ");
        mParser.defineEntityReplacementText("#39", "'");
        mParser.next();
    }

    /**
     * Returns parsed credit
     *
     * @return Returns credit in in farthing eg. ({@code 5 CZK} is {@code 500})
     */
    public int getCredit() {
        return mCredit;
    }

    /**
     * Returns used portal version
     *
     * @return Portal version where 1.20 is 120
     */
    public int getPortalVersion() {
        return mPortalVersion;
    }

    /**
     * Returns parsed menu
     *
     * @return Parsed menu
     */
    public List<MenuEntry> getMenuEntries() {
        return mMenuEntries;
    }

    /**
     * Returns parsed actions (orders)
     *
     * @return Actions
     */
    public List<Action.MenuEntryAction> getActionEntries() {
        return mActionEntries;
    }

    /**
     * Returns group menu info like price
     *
     * @return Group menu info
     */
    public List<GroupMenuEntry> getGroupMenuEntries() {
        return mGroupMenuEntries;
    }

    /**
     * Parse all data from given {@link InputStream}
     *
     * @param is           InputStream from page to be read
     * @param parseContext LogData context (portal ID etc...)
     * @throws XmlPullParserException Thrown when page cannot be read
     * @throws IOException            Thrown when IO exception occurs
     */
    public void parseData(@NonNull InputStream is, @NonNull LogData parseContext) throws XmlPullParserException, IOException {
        mParseContext = parseContext;

        mIs = new BufferedInputStream(is);
        mIs.mark(REPARSE_READ_LIMIT);
        resetParser();

        try {
            mMenuEntries = new ArrayList<>();
            mActionEntries = new ArrayList<>();
            mGroupMenuEntries = new ArrayList<>();

            //Test portal version
            if (mPortalAutoUpdate & mPortalVersion < 210) {
                findElement("head", null);
                int type = findElement("script", "type", "text/JavaScript", EnclosedXmlParser.MATCH_EQUAL, "head");
                if (type == XmlPullParser.END_TAG) {
                    if (mPortalVersion <= 206) {
                        mPortalVersion = 205;
                    }
                } else {
                    String scr = mParser.getAttributeValue(EnclosedXmlParser.ns, "src");
                    if (scr.endsWith("functions.js")) {
                        if (mPortalVersion < 206) {
                            mPortalVersion = 206;
                        }
                    } else if (scr.matches(".*functions-2.[7-9].[0-9]+\\.js")) {
                        mPortalVersion = 207;
                    } else {
                        mPortalVersion = 210;
                    }
                }
            }

            findElement("body", null);
            findNextType(XmlPullParser.START_TAG);

            switch (mPortalVersion) {
                case 205:
                    findElement("form", "name", "reader", EnclosedXmlParser.MATCH_EQUAL, null);
                    break;
                case 206:
                case 207:
                case 210: {
                    findElementID("span", "Kredit", null);
                    findNextType(XmlPullParser.START_TAG);
                    final String creditStr = readText();
                    mCredit = (int) (Double.parseDouble(creditStr) * 100);
                    findElementID("div", "mainContext", null);
                    break;
                }
                default: { // starting from 213
                    findElementClass("div", "topMenu", null);
                    final int found = findElementID("span", "Kredit", "div");
                    if (found != XmlPullParser.END_TAG) {
                        String creditStr = readText();
                        if (creditStr.endsWith(" Kč")) {
                            creditStr = creditStr.substring(0, creditStr.length() - 3);
                            creditStr = creditStr.replace(',', '.');
                        }
                        if ("volný účet".equals(creditStr)) {
                            mCredit = PublicProviderContract.NO_INFO;
                        } else {
                            mCredit = (int) (Double.parseDouble(creditStr) * 100);
                        }
                    }
                    findElementID("div", "mainContext", null);
                }
            }

            findElement("table", null);

            int found;
            try {
                found = findElement("form", "table");
            } catch (XmlPullParserException e) {
                throw new ServerMaintainException("Server is under maintain");
            }
            if ((found == XmlPullParser.END_TAG) && (mPortalVersion == 205)) {
                throw new ReparseException("Reparse from END_TAG");
            }
            while (found != XmlPullParser.END_TAG) {
                readDay();
                try {
                    found = findElement("form", "table");
                } catch (XmlPullParserException e) {
                    throw new ServerMaintainException("Server is under maintain");
                }
            }
        } catch (ReparseException e) {
            mIs.reset();
            resetParser();
            parseData(mIs, mParseContext);
        }
    }

    private void readDay() throws XmlPullParserException, IOException {
        mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "form");
        long date;
        if (findElement("div", "form") == XmlPullParser.START_TAG)
            date = readDate();
        else
            return;
        if (findFoodElement() == XmlPullParser.START_TAG) {
            readFood(date);
        }
    }

    private long readDate() throws IOException, XmlPullParserException {
        mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "div");
        @SuppressLint("SimpleDateFormat")
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date date;
        try {
            String dateStr = mParser.getAttributeValue(EnclosedXmlParser.ns, "id");
            if (dateStr.startsWith("day-")) {
                dateStr = dateStr.substring(4);
            }
            date = formatter.parse(dateStr);
        } catch (ParseException e) {
            throw new WebPageFormatChangedException("Bad web format (in Date)", null);
        }
        return date.getTime();
    }

    private int findFoodElement() throws IOException, XmlPullParserException {
        switch (mPortalVersion) {
            case 205:
            case 206:
            case 207:
                return findElement("tr", "form");
            default:
                return findElementClass("div", "jidelnicekItem", "form");
        }
    }

    private void readFood(long date) throws IOException, XmlPullParserException {
        switch (mPortalVersion) {
            case 205:
            case 206:
            case 207:
                mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "tr");
                break;
            default:
                mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "div");
        }
        do {
            switch (mPortalVersion) {
                case 205:
                case 206:
                case 207:
                    readFoodElement205(date);
                    break;
                default:
                    readFoodElement(date);
            }
        } while (findFoodElement() != XmlPullParser.END_TAG);
    }

    private void readFoodElement(long date) throws IOException, XmlPullParserException {
        // Food extra (order data)
        findElement("a", null);
        mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "a");

        String value = mParser.getAttributeValue(EnclosedXmlParser.ns, "class");

        String orderUrl;
        if (value.contains("disabled")) {
            orderUrl = ORDER_URL_DISABLED;
        } else {
            value = mParser.getAttributeValue(EnclosedXmlParser.ns, "onClick");
            if (value == null) // For versions <= 210
                value = mParser.getAttributeValue(EnclosedXmlParser.ns, "onclick");
            int pos = value.indexOf("ajaxOrder");
            pos = value.indexOf("'", pos);
            orderUrl = value.substring(pos + 1, value.indexOf("'", pos + 1));
        }

        // Food label
        findElementClass("span", "smallBoldTitle", null);
        String label = readName();

        // Food price
        findElementClass("span", "important", null);
        int price = readPrice();

        // Food quantity
        int quantity;
        if (findElementClass("i", "fa", "a") == XmlPullParser.START_TAG) {
            quantity = 1;
        } else {
            quantity = 0;
        }

        // Food text
        findElement("span", "style", "min-width: 250px;", EnclosedXmlParser.MATCH_START, null);

        StringBuilder sb = new StringBuilder();
        boolean reading = true;
        readingBlock:
        while (reading) {
            switch (mParser.next()) {
                case XmlPullParser.TEXT: {
                    String readText = mParser.getText();
                    if (readText == null) {
                        continue readingBlock;
                    }
                    sb.append(readText);
                    break;
                }
                case XmlPullParser.START_TAG:
                    if (mParser.getName().equals("br")) {
                        reading = false;
                    }
                    break;
            }
        }
        String text = sb.toString();

        // Remove extra spaces
        text = text.replaceAll(MULTI_SPACE_TRIM_PATTERN, MULTI_SPACE_TRIM_REPLACE).trim();

        if (mAllergyPattern != null) {
            text = text.replaceAll(mAllergyPattern, "");
        }

        // Parse data from label
        Matcher labelMatcher = FOOD_LABEL_PATTERN.matcher(label);
        if (!labelMatcher.matches()) {
            throw new WebPageFormatChangedException("Cannot extract food group from label " + label);
        }
        String groupName = labelMatcher.group(FOOD_LABEL_PATTERN_GROUP_NAME);
        int positionInGroup;
        try {
            positionInGroup = Integer.parseInt(labelMatcher.group(FOOD_LABEL_PATTERN_POSITION_IN_GROUP));
        } catch (NumberFormatException e) {
            throw new WebPageFormatChangedException("Cannot parse position of food in menu group");
        }

        final long menuEntryRelId = countMenuEntryRelativeId(date, groupName, positionInGroup);

        MenuEntry menuEntry = new MenuEntry(menuEntryRelId);
        menuEntry.date = date;
        menuEntry.label = label;
        menuEntry.group = groupName;
        menuEntry.text = text;
        menuEntry.extra = orderUrl;

        GroupMenuEntry groupMenuEntry = new GroupMenuEntry(menuEntryRelId, mParseContext.credentialGroupId);
        groupMenuEntry.price = price;
        // credentials group will be added later
        mGroupMenuEntries.add(groupMenuEntry);

        if (canOrder(orderUrl))
            groupMenuEntry.menuStatus = FEATURES_DEFAULT_ENABLE_FOOD;
        else
            groupMenuEntry.menuStatus = FEATURES_DISABLE_FOOD;

        mMenuEntries.add(menuEntry);

        if (quantity > 0) {
            long actionId = countActionRelativeId(menuEntryRelId);

            Action.MenuEntryAction action = new Action.MenuEntryAction(actionId, menuEntryRelId, mParseContext.portalId);
            action.syncStatus = PublicProviderContract.ACTION_SYNC_STATUS_SYNCED;
            action.reservedAmount = quantity;
            action.price = price;
            mActionEntries.add(action);
        }
    }

    private void readFoodElement205(long date) throws IOException, XmlPullParserException {
        // Food extra (order data)
        findElement("input", null);
        String orderUrl = readOrderUrl();

        // Food quantity
        findElement("b", null);
        int selected = readQuantity();

        // Food price
        switch (mPortalVersion) {
            case 205:
            case 206:
                findElementID("span", "cena", null);
                break;
            case 207:
                findElementClass("span", "important", null);
                break;
            default:
                throw new AssertionError("This method should be called only on portal <= 207");
        }
        int price = readPrice();

        // Food label
        findElement("span", "class", "smallBoldTitle", MATCH_END, null);
        String label = readName();

        switch (mPortalVersion) {
            case 205:
                findElement("td", "align", "left", EnclosedXmlParser.MATCH_EQUAL, null);
                break;
            default:
                findElement("td", "class", "bottomRow", MATCH_END, null);
        }

        // Food text
        String text = readFoodText();
        if (mAllergyPattern != null) {
            text = text.replaceAll(mAllergyPattern, "");
        }

        // Parse data from label
        Matcher labelMatcher = FOOD_LABEL_PATTERN.matcher(label);
        if (!labelMatcher.matches()) {
            throw new WebPageFormatChangedException("Cannot extract food group from label " + label);
        }
        String groupName = labelMatcher.group(FOOD_LABEL_PATTERN_GROUP_NAME);
        int positionInGroup;
        try {
            positionInGroup = Integer.parseInt(labelMatcher.group(FOOD_LABEL_PATTERN_POSITION_IN_GROUP));
        } catch (NumberFormatException e) {
            throw new WebPageFormatChangedException("Cannot parse position of food in menu group");
        }

        final long menuEntryRelId = countMenuEntryRelativeId(date, groupName, positionInGroup);

        MenuEntry menuEntry = new MenuEntry(menuEntryRelId);
        menuEntry.date = date;
        menuEntry.label = label;
        menuEntry.group = groupName;
        menuEntry.text = text;
        menuEntry.extra = orderUrl;

        GroupMenuEntry groupMenuEntry = new GroupMenuEntry(menuEntryRelId, mParseContext.credentialGroupId);
        groupMenuEntry.price = price;
        // credentials group will be added later
        mGroupMenuEntries.add(groupMenuEntry);

        if (canOrder(orderUrl))
            groupMenuEntry.menuStatus = FEATURES_DEFAULT_ENABLE_FOOD;
        else
            groupMenuEntry.menuStatus = FEATURES_DISABLE_FOOD;
        mMenuEntries.add(menuEntry);

        if (selected > 0) {
            long actionId = countActionRelativeId(menuEntryRelId);

            Action.MenuEntryAction action = new Action.MenuEntryAction(actionId, menuEntryRelId, mParseContext.portalId);
            action.syncStatus = PublicProviderContract.ACTION_SYNC_STATUS_SYNCED;
            action.reservedAmount = selected;
            action.price = price;
            mActionEntries.add(action);
        }
    }

    private String readOrderUrl() throws IOException, XmlPullParserException {
        mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "input");
        String url = mParser.getAttributeValue(EnclosedXmlParser.ns, "onClick");
        if (url == null) // For version <= 210
            url = mParser.getAttributeValue(EnclosedXmlParser.ns, "onclick");
        int pos;
        switch (mPortalVersion) {
            case 205:
            case 206:
                pos = url.indexOf("'");
                break;
            default:
                pos = url.indexOf("ajax");
                pos = url.indexOf("'", pos + 4);
        }
        if (pos == -1) {
            return ORDER_URL_DISABLED;
        } else {
            int endPos = url.indexOf("'", pos + 1);
            if (pos + 1 == endPos)
                return ORDER_URL_DISABLED;
            else
                return url.substring(pos + 1, endPos);
        }
    }

    private int readQuantity() throws IOException, XmlPullParserException {
        mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "b");
        try {
            String quantityText = readText().replaceFirst(" ks", "").trim();
            return Integer.valueOf(quantityText);
        } catch (NumberFormatException e) {
            throw new WebPageFormatChangedException("Cannot reed quantity", e);
        }
    }

    private String readName() throws IOException, XmlPullParserException {
        mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "span");
        return readText();
    }

    private int readPrice() throws IOException, XmlPullParserException {
        mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "span");
        String priceText = readText().trim();

        if (priceText.endsWith(" Kč")) {
            priceText = priceText.substring(0, priceText.length() - 3);
        }
        priceText = priceText.replace(',', '.');
        double price = Double.parseDouble(priceText);
        return (int) (price * 100);
    }

    private String readFoodText() throws IOException, XmlPullParserException {
        mParser.require(XmlPullParser.START_TAG, EnclosedXmlParser.ns, "td");
        // Parser data from input stream
        String text = readText();

        // Remove extra spaces
        text = text.replaceAll(MULTI_SPACE_TRIM_PATTERN, MULTI_SPACE_TRIM_REPLACE).trim();

        return text;
    }

    private long countMenuEntryRelativeId(long dateInMillis, String groupName, int positionInGroup) {
        // Each day could use different groups so I treat them separately
        if (mLastDate != dateInMillis) {
            mLastDate = dateInMillis;
            mLastDateIds = new HashMap<>();
        }

        // Ads group if not used now
        if (!mLastDateIds.containsKey(groupName)) {
            mLastDateIds.put(groupName, mLastDateIds.size());
        }

        // groupName -> groupNameId
        long groupCode = mLastDateIds.get(groupName);
        long dateInDays = dateInMillis / 1000 / 60 / 60 / 24;

        // Count relative id
        return dateInDays * 100_00L + groupCode * 100 + (long) positionInGroup;
    }

    private long countActionRelativeId(long menuEntryRelId) {
        // Count relative id
        if (BuildConfig.DEBUG) {
            checkMultiplyOverflow(PublicProviderContract.CUSTOM_DATA_OFFSET * 10, menuEntryRelId);
        }

        return menuEntryRelId * PublicProviderContract.CUSTOM_DATA_OFFSET * 10 + mParseContext.portalId;
    }

    private void checkMultiplyOverflow(long a, long b) {
        if (a < 0 || b < 0)
            throw new IllegalArgumentException("Parameters should be positive for this test");

        if (a * b < 0)
            throw new RuntimeException("Long overflow found in multiply operation");

    }

    private static boolean canOrder(String url) {
        return url != null && !url.equals(ORDER_URL_DISABLED);
    }
}
