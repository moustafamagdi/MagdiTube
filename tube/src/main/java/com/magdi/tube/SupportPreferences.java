package com.magdi.tube;

import android.content.SharedPreferences;

/** Versioned defaults. A saved off setting always wins over a new default. */
final class SupportPreferences {
    static final int VERSION = 3;

    static boolean initialize(SharedPreferences prefs) {
        if (prefs.getInt("sharingPolicyVersion", 0) < VERSION) {
            boolean on = prefs.getBoolean("enabled", true);
            prefs.edit().putBoolean("enabled", on).putInt("sharingPolicyVersion", VERSION)
                    .putInt("noticeVersion", on ? 0 : VERSION).commit();
        }
        return prefs.getBoolean("enabled", true);
    }

    static boolean noticePending(SharedPreferences prefs) {
        return prefs.getBoolean("enabled", false) && prefs.getInt("noticeVersion", 0) < VERSION;
    }

    static boolean canSendAutomatically(SharedPreferences prefs) {
        return prefs.getBoolean("enabled", false) && !noticePending(prefs);
    }

    static void choose(SharedPreferences prefs, boolean enabled) {
        prefs.edit().putBoolean("enabled", enabled).putInt("consentVersion", VERSION)
                .putInt("sharingPolicyVersion", VERSION).putInt("noticeVersion", VERSION)
                .putLong("ack", System.currentTimeMillis()).commit();
    }

    private SupportPreferences() {}
}
