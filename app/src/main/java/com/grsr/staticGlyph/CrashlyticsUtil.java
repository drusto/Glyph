package com.grsr.staticGlyph;

import com.google.firebase.crashlytics.FirebaseCrashlytics;

/**
 * Utility wrapper around {@link FirebaseCrashlytics} to simplify logging of
 * breadcrumbs and exceptions throughout the app.
 */
public final class CrashlyticsUtil {

    private CrashlyticsUtil() {
        // Utility class
    }

    /**
     * Adds a breadcrumb entry to Crashlytics.
     */
    public static void log(String message) {
        FirebaseCrashlytics.getInstance().log(message);
    }

    /**
     * Records a non-fatal exception in Crashlytics.
     */
    public static void recordException(Throwable throwable) {
        FirebaseCrashlytics.getInstance().recordException(throwable);
    }
}

