package com.aurora.phone;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;

import java.util.List;

import com.aurora.phone.data.CallLogStore;
import com.aurora.phone.data.ContactsStore;
import com.aurora.phone.data.SampleData;

/** Settings: appearance, behavior, demo tools, permissions, about. */
public class SettingsActivity extends BaseActivity {

    private static final int REQ_PERMS = 51;

    private Switch switchBlockSpam;
    private Switch switchShowRisk;
    private Switch switchHaptics;
    private LinearLayout permissionsCard;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        RadioGroup themeGroup = (RadioGroup) findViewById(R.id.themeGroup);
        RadioButton themeSystem = (RadioButton) findViewById(R.id.themeSystem);
        RadioButton themeLight = (RadioButton) findViewById(R.id.themeLight);
        RadioButton themeDark = (RadioButton) findViewById(R.id.themeDark);
        String pref = Prefs.getTheme(this);
        if (Prefs.THEME_DARK.equals(pref)) {
            themeGroup.check(R.id.themeDark);
        } else if (Prefs.THEME_LIGHT.equals(pref)) {
            themeGroup.check(R.id.themeLight);
        } else {
            themeGroup.check(R.id.themeSystem);
        }
        themeGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                String mode = Prefs.THEME_SYSTEM;
                if (checkedId == R.id.themeDark) {
                    mode = Prefs.THEME_DARK;
                } else if (checkedId == R.id.themeLight) {
                    mode = Prefs.THEME_LIGHT;
                }
                Prefs.setTheme(SettingsActivity.this, mode);
                recreate();
            }
        });

        switchBlockSpam = (Switch) findViewById(R.id.switchBlockSpam);
        switchShowRisk = (Switch) findViewById(R.id.switchShowRisk);
        switchHaptics = (Switch) findViewById(R.id.switchHaptics);

        switchBlockSpam.setChecked(Prefs.blockSpam(this));
        switchShowRisk.setChecked(Prefs.showRisk(this));
        switchHaptics.setChecked(Prefs.haptics(this));

        switchBlockSpam.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                Prefs.setBlockSpam(SettingsActivity.this, isChecked);
            }
        });
        switchShowRisk.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                Prefs.setShowRisk(SettingsActivity.this, isChecked);
            }
        });
        switchHaptics.setOnCheckedChangeListener(new android.widget.CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.CompoundButton buttonView, boolean isChecked) {
                Prefs.setHaptics(SettingsActivity.this, isChecked);
            }
        });

        permissionsCard = (LinearLayout) findViewById(R.id.permissionsCard);

        findViewById(R.id.rowSimulate).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                simulateIncoming();
            }
        });

        TextView version = (TextView) findViewById(R.id.versionValue);
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(getPackageName(), 0);
            version.setText(pi.versionName == null ? "1.0.0" : pi.versionName);
        } catch (PackageManager.NameNotFoundException ex) {
            version.setText("1.0.0");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        buildPermissionRows();
    }

    private void buildPermissionRows() {
        permissionsCard.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        addPermissionRow(inflater, Manifest.permission.READ_CONTACTS,
                getString(R.string.tab_contacts), "Names and numbers for search &amp; caller ID");
        addPermissionRow(inflater, Manifest.permission.READ_CALL_LOG,
                getString(R.string.tab_recents), "Your call history for recents &amp; details");
        addPermissionRow(inflater, Manifest.permission.CALL_PHONE,
                getString(R.string.call), "Place calls directly from Aurora");
    }

    private void addPermissionRow(LayoutInflater inflater, final String permission,
                                  String title, String subtitle) {
        View row = inflater.inflate(R.layout.item_setting_row, permissionsCard, false);
        TextView titleView = (TextView) row.findViewById(R.id.rowTitle);
        TextView subtitleView = (TextView) row.findViewById(R.id.rowSubtitle);
        TextView value = (TextView) row.findViewById(R.id.rowValue);
        View chevron = row.findViewById(R.id.rowChevron);

        boolean granted = hasPerm(permission);
        titleView.setText(title);
        subtitleView.setText(android.text.Html.fromHtml(subtitle));
        subtitleView.setVisibility(View.VISIBLE);
        value.setText(granted ? "Granted" : "Grant");
        value.setTextColor(granted
                ? getColor(R.color.light_success) : colorAttr(R.attr.av_colorPrimary));
        chevron.setVisibility(View.VISIBLE);
        row.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!hasPerm(permission)) {
                    requestPermissions(new String[] { permission }, REQ_PERMS);
                }
            }
        });
        permissionsCard.addView(row);
    }

    private void simulateIncoming() {
        List<RecentSource> sources = null;
        String number = "";
        String name = "";

        List<com.aurora.phone.RecentItem> recents = CallLogStore.loadGrouped(this);
        if (recents != null && !recents.isEmpty()) {
            com.aurora.phone.RecentItem item = recents.get(0);
            number = item.number;
            name = item.name;
        } else {
            List<com.aurora.phone.RecentItem> sample = SampleData.recents();
            if (!sample.isEmpty()) {
                number = sample.get(0).number;
                name = sample.get(0).name;
            }
        }
        if (number.length() == 0) {
            toast("No number to simulate");
            return;
        }
        Intent intent = new Intent(this, CallScreenActivity.class);
        intent.putExtra(CallScreenActivity.EXTRA_NUMBER, number);
        intent.putExtra(CallScreenActivity.EXTRA_NAME, name);
        intent.putExtra(CallScreenActivity.EXTRA_MODE, CallScreenActivity.MODE_INCOMING);
        startActivity(intent);
    }

    // silence unused warning helper type
    private interface RecentSource {
    }
}
