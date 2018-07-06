package cz.maresmar.sfm.plugin.controller;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.List;

import cz.maresmar.sfm.provider.PublicProviderContract;

/**
 * Class that loads, saves, delete or updates multiple object to {@link android.content.ContentProvider}
 * <p>
 * The model objects needs to be annotated with {@link ProviderColumn}
 * </p>
 *
 * @see ObjectHandler
 */
public class ObjectsController {

    // To prevent someone from accidentally instantiating the this class,
    // make the constructor private.
    private ObjectsController() {
    }

    /**
     * Loads more objects from {@link android.content.ContentProvider}
     *
     * @param context     Some valid context
     * @param uri         Uri pointing to place where object are stored
     * @param initializer Creator of new object
     * @param <T>         Type of objects
     * @return List of objects loaded from {@link android.content.ContentProvider}
     */
    static public <T> List<T> loadElements(Context context, Uri uri, ObjectHandler.Initializer<T> initializer) {
        return loadElements(context, uri, initializer, null, null, null);
    }

    /**
     * Loads more objects from {@link android.content.ContentProvider}
     *
     * @param context       Some valid context
     * @param uri           Uri pointing to place where object are stored
     * @param initializer   Creator of new objects
     * @param selection     Provider selection
     * @param selectionArgs Provider selection args
     * @param sortOrder     Provider sort order
     * @param <T>           Type of objects
     * @return List of objects loaded from {@link android.content.ContentProvider}
     */
    static public <T> List<T> loadElements(Context context, Uri uri, ObjectHandler.Initializer<T> initializer, String selection,
                                           String[] selectionArgs, String sortOrder) {
        List<T> elements = new ArrayList<>();

        // Does a query against the table and returns a Cursor object
        try (Cursor cursor = context.getContentResolver().query(uri, initializer.getInflateProjection(), selection,
                selectionArgs, sortOrder)) {
            // For each row of result
            while (cursor.moveToNext()) {
                T newElement = ObjectHandler.inflate(cursor, initializer);
                elements.add(newElement);
            }
        }

        return elements;
    }

    /**
     * Inserts more objects to {@link android.content.ContentProvider}
     *
     * @param context  Some valid context
     * @param elements Objects to be inserted
     * @param uri      Uri pointing to place where objects will be stored
     * @param <T>      Type of objects
     * @throws IllegalArgumentException If insert failed
     */
    static public <T> void saveElements(Context context, Uri uri, List<T> elements) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        // Build insert request for each element
        for (T element : elements) {
            // Get values
            ContentValues values = ObjectHandler.deflate(element);
            // Save to operation
            operations.add(
                    ContentProviderOperation.newInsert(uri)
                            .withValues(values)
                            .withYieldAllowed(true)
                            .build());
        }

        // Apply them once for performance boost
        try {
            context.getContentResolver().
                    applyBatch(PublicProviderContract.AUTHORITY, operations);
        } catch (OperationApplicationException e) {
            throw new IllegalArgumentException("Cannot save elements to db ", e);
        } catch (RemoteException e) {
            throw new IllegalArgumentException("Cannot save elements to db ", e);
        }
    }

    /**
     * Delete more objects from {@link android.content.ContentProvider}
     *
     * @param context  Some valid context
     * @param elements Objects to be deleted
     * @param uri      Uri pointing to place where objects should be stored
     * @param <T>      Type of objects
     * @throws IllegalArgumentException If delete failed
     */
    static public <T> void deleteElements(Context context, Uri uri, List<T> elements) {
        ArrayList<ContentProviderOperation> operations = new ArrayList<>();

        // Build insert request for each element
        for (T element : elements) {
            // Get corresponding ID
            String idSelection = ObjectHandler.getIdSelection(element);
            // Save to operation
            operations.add(
                    ContentProviderOperation.newDelete(uri)
                            .withSelection(idSelection, null)
                            .withExpectedCount(1)
                            .withYieldAllowed(true)
                            .build());
        }

        // Apply them once for performance boost
        try {
            context.getContentResolver().
                    applyBatch(PublicProviderContract.AUTHORITY, operations);
        } catch (OperationApplicationException e) {
            throw new IllegalArgumentException("Cannot save elements to db ", e);
        } catch (RemoteException e) {
            throw new IllegalArgumentException("Cannot save elements to db ", e);
        }
    }

    /**
     * Updates one element in {@link android.content.ContentProvider}
     *
     * @param context Some valid context
     * @param uri     Uri pointing to place where object should be stored
     * @param element Objects to be deleted
     * @param <T>     Type of objects
     * @throws IllegalArgumentException If update failed
     */
    static public <T> void changeElement(Context context, Uri uri,
                                         T element) {
        // Get values
        ContentValues values = ObjectHandler.deflate(element);
        // Save to operation
        int changedRows = context.getContentResolver().update(uri, values, null, null);

        if (changedRows != 1) {
            throw new IllegalArgumentException("Cannot save element to db ");
        }
    }
}
