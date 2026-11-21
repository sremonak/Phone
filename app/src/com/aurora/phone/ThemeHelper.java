package com.aurora.phone;

import android.content.Context;
import android.content.res.Configuration;

/** Resolves the active theme id and accent overlay based on user preferences. */
public final class ThemeHelper {
    private ThemeHelper() {
    }

    /** Accent overlay style ids; index 0 (violet) needs no overlay. */
    private static final int[] ACCENT_STYLES = {
            0,
            R.style.Accent_Blue,
            R.style.Accent_Teal,
            R.style.Accent_Rose,
            R.style.Accent_Amber,
            R.style.Accent_Green
    };

    public static boolean isNight(Context c) {
        int mode = c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return mode == Configuration.UI_MODE_NIGHT_YES;
    }

    public static int getThemeId(Context c) {
        String pref = Prefs.getTheme(c);
        if (Prefs.THEME_LIGHT.equals(pref)) {
            return R.style.AppTheme_Light;
        }
        if (Prefs.THEME_DARK.equals(pref)) {
            return R.style.AppTheme_Dark;
        }
        return isNight(c) ? R.style.AppTheme_Dark : R.style.AppTheme_Light;
    }

    /** Overlay style id for the chosen accent, or 0 when the default violet applies. */
    public static int getAccentId(Context c) {
        int index = Prefs.accent(c);
        if (index < 0 || index >= ACCENT_STYLES.length) {
            return 0;
        }
        return ACCENT_STYLES[index];
    }
}
