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
import android.widget.ImageView;
import android.widget.EditText;
import android.widget.TextView;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.graphics.drawable.GradientDrawable;

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

        buildAccentRow();
        configTools();
        buildPatternRow();

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
        addPermissionRow(inflater, Manifest.permission.WRITE_CALL_LOG,
                getString(R.string.tab_recents), "Delete or clear entries from history");
    }

    // ---------- accent color ----------

    private void buildAccentRow() {
        LinearLayout row = (LinearLayout) findViewById(R.id.accentRow);
        row.removeAllViews();
        final int[] colors = new int[] {
                getColor(R.color.acc_violet),
                getColor(R.color.acc_blue),
                getColor(R.color.acc_teal),
                getColor(R.color.acc_rose),
                getColor(R.color.acc_amber),
                getColor(R.color.acc_green)
        };
        int selected = Prefs.accent(this);
        int margin = dp(6);
        for (int i = 0; i < colors.length; i++) {
            final int index = i;
            View swatch = new View(this);
            GradientDrawable shape = new GradientDrawable();
            shape.setShape(GradientDrawable.OVAL);
            shape.setSize(dp(40), dp(40));
            shape.setColor(colors[i]);
            if (i == selected) {
                shape.setStroke(dp(3), colorAttr(R.attr.av_colorText));
            } else {
                shape.setStroke(dp(2), 0x33000000);
            }
            swatch.setBackground(shape);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(40), dp(40));
            params.setMarginEnd(margin);
            swatch.setLayoutParams(params);
            swatch.setContentDescription(getString(R.string.accent_title));
            swatch.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Prefs.setAccent(SettingsActivity.this, index);
                    recreate();
                }
            });
            row.addView(swatch);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    // ---------- tools ----------

    private void configTools() {
        configRow(R.id.rowInsights, R.drawable.ic_chart, R.string.insights_open,
                R.string.insights_subtitle, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        startActivity(new Intent(SettingsActivity.this,
                                com.aurora.phone.InsightsActivity.class));
                    }
                });
        configRow(R.id.rowExport, R.drawable.ic_download, R.string.export_csv,
                R.string.export_csv_sub, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        exportHistory();
                    }
                });
        configRow(R.id.rowClearHistory, R.drawable.ic_delete, R.string.clear_history,
                0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        confirmClearHistory();
                    }
                });
        configRow(R.id.rowSpeedDial, R.drawable.ic_bolt, R.string.speed_dial_manager,
                R.string.speed_dial_manager_hint, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showSpeedDialDialog();
                    }
                });
        configRow(R.id.rowBlockedList, R.drawable.ic_block, R.string.blocked_list,
                0, new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        showBlockedList();
                    }
                });
    }

    private void configRow(int rowId, int icon, int title, int subtitle,
                           View.OnClickListener click) {
        View row = findViewById(rowId);
        TextView titleView = (TextView) row.findViewById(R.id.rowTitle);
        TextView subtitleView = (TextView) row.findViewById(R.id.rowSubtitle);
        ImageView iconView = (ImageView) row.findViewById(R.id.rowIcon);
        View chevron = row.findViewById(R.id.rowChevron);
        titleView.setText(title);
        iconView.setImageResource(icon);
        iconView.setVisibility(View.VISIBLE);
        chevron.setVisibility(View.VISIBLE);
        if (subtitle != 0) {
            subtitleView.setText(subtitle);
            subtitleView.setVisibility(View.VISIBLE);
        }
        row.setOnClickListener(click);
    }

    private void exportHistory() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                String result = null;
                try {
                    result = com.aurora.phone.util.Exporter.exportCsv(
                            SettingsActivity.this);
                } catch (Exception ignored) {
                }
                final String location = result;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }
                        if (location == null) {
                            toast(getString(R.string.export_fail));
                        } else {
                            toast(getString(R.string.export_done, location));
                        }
                    }
                });
            }
        }).start();
    }

    private void confirmClearHistory() {
        if (!hasPerm(Manifest.permission.WRITE_CALL_LOG)) {
            requestPermissions(new String[] { Manifest.permission.WRITE_CALL_LOG }, REQ_PERMS);
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.clear_history)
                .setMessage(R.string.clear_history_msg)
                .setPositiveButton(R.string.clear_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        boolean ok = CallLogStore.clearAll(SettingsActivity.this);
                        toast(getString(ok ? R.string.clear_history_done
                                : R.string.clear_history_need_perm));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showSpeedDialDialog() {
        StringBuilder sb = new StringBuilder();
        boolean any = false;
        for (char key = '2'; key <= '9'; key++) {
            String number = Prefs.speedDial(this, key);
            if (number.length() > 0) {
                any = true;
                sb.append(key).append(" → ")
                        .append(com.aurora.phone.util.NumberGeo.pretty(
                                com.aurora.phone.util.NumberGeo.normalize(number)))
                        .append('\n');
            }
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.speed_dial_manager)
                .setMessage(any ? sb.toString() : getString(R.string.speed_dial_none))
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void showBlockedList() {
        final java.util.List<String> numbers = new java.util.ArrayList<String>(
                com.aurora.phone.data.BlockedStore.all(this));
        final java.util.List<String> patterns = new java.util.ArrayList<String>(
                com.aurora.phone.data.BlockedStore.patterns(this));
        final java.util.List<String> rows = new java.util.ArrayList<String>();
        for (int i = 0; i < numbers.size(); i++) {
            rows.add(com.aurora.phone.util.NumberGeo.pretty(
                    com.aurora.phone.util.NumberGeo.normalize(numbers.get(i))));
        }
        for (int i = 0; i < patterns.size(); i++) {
            rows.add(patterns.get(i));
        }
        if (rows.isEmpty()) {
            toast(getString(R.string.blocked_empty));
            return;
        }
        final String[] asArray = rows.toArray(new String[rows.size()]);
        new AlertDialog.Builder(this)
                .setTitle(R.string.blocked_list)
                .setItems(asArray, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (which < numbers.size()) {
                            com.aurora.phone.data.BlockedStore.setBlocked(
                                    SettingsActivity.this, numbers.get(which), false);
                            toast(getString(R.string.unblocked_toast));
                        } else {
                            String pattern = patterns.get(which - numbers.size());
                            com.aurora.phone.data.BlockedStore.removePattern(
                                    SettingsActivity.this, pattern);
                            buildPatternRow();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    // ---------- block patterns ----------

    private void buildPatternRow() {
        final EditText input = (EditText) findViewById(R.id.patternInput);
        TextView add = (TextView) findViewById(R.id.patternAdd);
        TextView list = (TextView) findViewById(R.id.patternList);
        java.util.Set<String> patterns = com.aurora.phone.data.BlockedStore.patterns(this);
        if (patterns.isEmpty()) {
            list.setVisibility(View.GONE);
        } else {
            list.setVisibility(View.VISIBLE);
            list.setText(android.text.TextUtils.join("\n  ", patterns));
        }
        add.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String pattern = input.getText().toString().trim();
                if (!pattern.contains("*")) {
                    toast(getString(R.string.block_pattern_hint));
                    return;
                }
                com.aurora.phone.data.BlockedStore.addPattern(
                        SettingsActivity.this, pattern);
                input.setText("");
                toast(getString(R.string.block_pattern_added));
                buildPatternRow();
            }
        });
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
