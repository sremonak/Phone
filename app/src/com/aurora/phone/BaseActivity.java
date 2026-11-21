package com.aurora.phone;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.TypedValue;
import android.widget.Toast;

/** Base activity: applies the chosen theme and hosts small helpers. */
public abstract class BaseActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(ThemeHelper.getThemeId(this));
        int accent = ThemeHelper.getAccentId(this);
        if (accent != 0) {
            getTheme().applyStyle(accent, true);
        }
        super.onCreate(savedInstanceState);
    }

    protected boolean hasPerm(String permission) {
        return checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED;
    }

    public void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    protected int colorAttr(int attr) {
        TypedValue value = new TypedValue();
        if (getTheme().resolveAttribute(attr, value, true)) {
            return value.data;
        }
        return 0xFF000000;
    }

    public boolean isUiAlive() {
        return !isFinishing() && !isDestroyed();
    }
}
