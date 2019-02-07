package cz.maresmar.sfm.plugin.controller;

import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import cz.maresmar.sfm.plugin.BuildConfig;

/**
 * Class that for loading and saving of one object to {@link android.content.ContentProvider}
 * <p>
 * The model objects needs to be annotated with {@link ProviderColumn}
 * </p>
 */
public class ObjectHandler {

    // To prevent someone from accidentally instantiating the this class,
    // make the constructor private.
    private ObjectHandler() {
    }

    /**
     * Sets the value as excluded from insert or update. This is useful when you want to update some
     * object in {@link android.content.ContentProvider} but you don't want to some original values
     */
    public static final int EXCLUDED = -2;

    /**
     * Loads object from actual {@link Cursor} row
     *
     * @param cursor      Cursor with projection from {@link Initializer#getInflateProjection()}
     * @param initializer {@link Initializer} that will be used for crating new object
     * @param <T>         Type of object
     * @return New object that contains data from {@link Cursor}
     */
    @NonNull
    static public <T> T inflate(@NonNull Cursor cursor, @NonNull Initializer<T> initializer) {
        Class<? extends T> clazz = initializer.getClass(cursor);
        T element = initializer.newInstance(cursor);

        HashMap<String, Integer> projectionHashMap = initializer.getColumnMap();

        for (Field field : clazz.getFields()) {
            // Skip final fields, they should be set using constructor
            if ((field.getModifiers() & Modifier.FINAL) == Modifier.FINAL)
                continue;

            ProviderColumn fieldAnnotation = field.getAnnotation(ProviderColumn.class);

            if (fieldAnnotation != null) {
                String columnName = fieldAnnotation.name();

                if (BuildConfig.DEBUG) {
                    if (!projectionHashMap.containsKey(columnName)) {
                        throw new UnsupportedOperationException("Unknown column " + columnName);
                    }
                }

                int columnIndex = projectionHashMap.get(columnName);

                // Skip null values (leaving the defaults)
                if (cursor.isNull(columnIndex) && !fieldAnnotation.zeroOnNull())
                    continue;

                Class<?> elementType = field.getType();
                try {
                    if (elementType.equals(int.class)) {
                        if (!cursor.isNull(columnIndex)) {
                            field.setInt(element, cursor.getInt(columnIndex));
                        } else if (fieldAnnotation.zeroOnNull()) {
                            field.setInt(element, 0);
                        }
                    } else if (elementType.equals(long.class)) {
                        if (!cursor.isNull(columnIndex)) {
                            field.setLong(element, cursor.getLong(columnIndex));
                        } else if (fieldAnnotation.zeroOnNull()) {
                            field.setLong(element, 0);
                        }
                    } else if (elementType.equals(float.class)) {
                        field.setFloat(element, cursor.getFloat(columnIndex));
                    } else if (elementType.equals(double.class)) {
                        field.setDouble(element, cursor.getDouble(columnIndex));
                    } else if (elementType.equals(Short.class)) {
                        field.setShort(element, cursor.getShort(columnIndex));
                    } else if (elementType.equals(String.class)) {
                        field.set(element, cursor.getString(columnIndex));
                    } else {
                        throw new UnsupportedTypeException("Unknown type " + elementType.toString());
                    }
                } catch (IllegalAccessException e) {
                    // It shouldn't happen
                    throw new RuntimeException("Accessed private field that should be public", e);
                }
            }
        }

        return element;
    }

    /**
     * Saves object values to {@link ContentValues} that can be later inserted to {@link android.content.ContentProvider}
     *
     * @param element Element to be saved
     * @param <T>     Type of element
     * @return Return {@link ContentValues} created from object
     */
    @NonNull
    static public <T> ContentValues deflate(@NonNull T element) {
        ContentValues values = new ContentValues();

        for (Field field : element.getClass().getFields()) {
            ProviderColumn fieldAnnotation = field.getAnnotation(ProviderColumn.class);

            if (fieldAnnotation != null) {
                String columnName = fieldAnnotation.name();

                // Skip read only values
                if (fieldAnnotation.ro())
                    continue;

                Class<?> elementType = field.getType();
                try {
                    if (elementType.equals(int.class)) {
                        int value = field.getInt(element);
                        if (value == EXCLUDED)
                            continue;
                        values.put(columnName, value);
                    } else if (elementType.equals(long.class)) {
                        long value = field.getLong(element);
                        if (value == EXCLUDED)
                            continue;
                        values.put(columnName, value);
                    } else if (elementType.equals(float.class)) {
                        values.put(columnName, field.getFloat(element));
                    } else if (elementType.equals(double.class)) {
                        values.put(columnName, field.getDouble(element));
                    } else if (elementType.equals(Short.class)) {
                        values.put(columnName, field.getShort(element));
                    } else if (elementType.equals(String.class)) {
                        values.put(columnName, (String) field.get(element));
                    } else {
                        throw new UnsupportedTypeException("Unknown type " + elementType.toString());
                    }
                } catch (IllegalAccessException e) {
                    // It shouldn't happen
                    throw new RuntimeException("Accessed private field that should be public", e);
                }
            }
        }

        return values;
    }

    /**
     * Counts {@link android.content.ContentProvider#update(Uri, ContentValues, String, String[])}
     * selection that can be used in update. This is used when object needs to be updated in {@link android.content.ContentProvider}
     *
     * @param element Element where the selection is needed
     * @param <T>     Type of element
     * @return SQL selection String
     */
    @NonNull
    public static <T> String getIdSelection(@NonNull T element) {
        StringBuilder selection = new StringBuilder();

        for (Field field : element.getClass().getFields()) {
            ProviderColumn fieldAnnotation = field.getAnnotation(ProviderColumn.class);

            if (fieldAnnotation != null) {
                String columnName = fieldAnnotation.name();
                boolean isIdColumn = fieldAnnotation.id();

                if (isIdColumn) {
                    if (selection.length() > 0) {
                        selection.append(" AND ");
                    }
                    selection.append(columnName);
                    selection.append(" == ");
                    try {
                        selection.append(field.get(element));
                    } catch (IllegalAccessException e) {
                        // It shouldn't happen
                        throw new RuntimeException("Accessed private field that should be public", e);
                    }
                }
            }
        }

        return selection.toString();
    }

    /**
     * Abstract class used for creating new object from {@link Cursor}
     *
     * @param <T> Type of object
     */
    public static abstract class Initializer<T> {

        Class<T> mClass;

        /**
         * Creates new Initializer with {@link Class} of object
         *
         * @param clazz Class of {@code <T>} type
         */
        protected Initializer(@Nullable Class<T> clazz) {
            mClass = clazz;
        }

        /**
         * Creates new object from actual cursor row using object's constructor (another fields are left
         * with defaults).
         *
         * @param cursor Cursor on row that containing object values
         * @return New object
         */
        @NonNull
        public abstract T newInstance(@NonNull Cursor cursor);

        /**
         * Return class that correspond with actual {@link Cursor} row (it can differ because of
         * polymorphism of object)
         *
         * @param cursor Cursor on row that containing object values
         * @return Class or subclass of {@code <T>} type
         */
        @NonNull
        public abstract Class<? extends T> getClass(@NonNull Cursor cursor);

        /**
         * Gets projection that are needed to inflate object
         *
         * @return Projection that can be used in {@link android.content.ContentProvider#query(Uri, String[], String, String[], String)}
         * @see ObjectHandler#inflate(Cursor, Initializer)
         */
        @NonNull
        public String[] getInflateProjection() {
            Field[] fields = mClass.getFields();
            List<String> columns = new ArrayList<>();

            for (Field field : fields) {
                ProviderColumn fieldAnnotation = field.getAnnotation(ProviderColumn.class);

                if (fieldAnnotation != null) {
                    columns.add(fieldAnnotation.name());
                }
            }

            String[] array = new String[columns.size()];
            int i = 0;
            for (String col : columns) {
                array[i] = col;
                i++;
            }

            return array;
        }

        /**
         * Returns {@link HashMap} from projection. It can be used for column searching
         *
         * @return {@link HashMap} from projection
         * @see #getInflateProjection()
         */
        public HashMap<String, Integer> getColumnMap() {
            String[] projection = getInflateProjection();

            HashMap<String, Integer> projectionHashMap = new HashMap<>();
            for (int i = 0; i < projection.length; i++) {
                projectionHashMap.put(projection[i], i);
            }

            return projectionHashMap;
        }
    }
}
