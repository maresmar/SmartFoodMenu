package cz.maresmar.sfm.plugin.controller;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Annotation used for objects loading and saving
 *
 * @see ObjectHandler
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface ProviderColumn {
    /**
     * Corresponding column name
     */
    String name();

    /**
     * Tells if column act as ID column
     */
    boolean id() default false;

    /**
     * Tells if field is read only (won't be inserted or updated)
     */
    boolean ro() default false;

    /**
     * Tells if field should have {@code 0} when the value in {@link android.database.Cursor} for this
     * column in {@code null}
     */
    boolean zeroOnNull() default false;
}
