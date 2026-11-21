package com.aurora.phone;

import android.content.Context;
import android.content.res.Configuration;

/** Resolves the active theme id based on the user's preference. */
public final class ThemeHelper {
    private ThemeHelper() {
    }

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
}
