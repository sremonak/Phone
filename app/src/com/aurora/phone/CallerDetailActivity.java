package com.aurora.phone;

import android.content.Intent;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.EditText;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aurora.phone.data.BlockedStore;
import com.aurora.phone.data.CallLogStore;
import com.aurora.phone.data.ContactsStore;
import com.aurora.phone.data.SampleData;
import com.aurora.phone.util.Actions;
import com.aurora.phone.util.NumberGeo;
import com.aurora.phone.util.RiskAnalyzer;
import com.aurora.phone.util.TimeUtils;
import com.aurora.phone.view.AvatarView;

/** The detailed caller-information screen. */
public class CallerDetailActivity extends BaseActivity {

    public static final String EXTRA_NUMBER = "number";
    public static final String EXTRA_NAME = "name";

    private static final int REQ_DIAL = 61;

    private String number = "";
    private String incomingName = "";
    private CallerInfo info = new CallerInfo();

    private AvatarView avatar;
    private TextView nameView;
    private TextView numberView;
    private TextView chipLocation;
    private TextView chipCarrier;
    private TextView chipType;
    private View riskBanner;
    private ImageView riskIcon;
    private TextView riskTitle;
    private TextView riskDesc;
    private LinearLayout infoCard;
    private LinearLayout historyCard;
    private TextView historyEmpty;
    private TextView blockLabel;
    private TextView noteText;
    private TextView btnEditNote;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_caller_detail);

        number = getIntent().getStringExtra(EXTRA_NUMBER);
        incomingName = getIntent().getStringExtra(EXTRA_NAME);
        if (number == null) {
            number = "";
        }
        if (incomingName == null) {
            incomingName = "";
        }

        avatar = (AvatarView) findViewById(R.id.detailAvatar);
        nameView = (TextView) findViewById(R.id.detailName);
        numberView = (TextView) findViewById(R.id.detailNumber);
        chipLocation = (TextView) findViewById(R.id.chipLocation);
        chipCarrier = (TextView) findViewById(R.id.chipCarrier);
        chipType = (TextView) findViewById(R.id.chipType);
        riskBanner = findViewById(R.id.riskBanner);
        riskIcon = (ImageView) findViewById(R.id.riskIcon);
        riskTitle = (TextView) findViewById(R.id.riskTitle);
        riskDesc = (TextView) findViewById(R.id.riskDesc);
        infoCard = (LinearLayout) findViewById(R.id.infoCard);
        historyCard = (LinearLayout) findViewById(R.id.historyCard);
        historyEmpty = (TextView) findViewById(R.id.historyEmpty);
        blockLabel = (TextView) findViewById(R.id.actionBlockLabel);
        noteText = (TextView) findViewById(R.id.noteText);
        btnEditNote = (TextView) findViewById(R.id.btnEditNote);
        refreshNote();
        btnEditNote.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editNote();
            }
        });

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnMore).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showMenu(v);
            }
        });

        findViewById(R.id.actionCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeCall();
            }
        });
        findViewById(R.id.actionMessage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Actions.sms(CallerDetailActivity.this, number);
            }
        });
        findViewById(R.id.actionBlock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleBlock();
            }
        });
        findViewById(R.id.actionAdd).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Actions.addToContacts(CallerDetailActivity.this, number);
            }
        });
        findViewById(R.id.actionShare).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String label = displayName();
                Actions.share(CallerDetailActivity.this,
                        label + " — " + number + " (Aurora Phone)");
            }
        });

        // instant header while the rest loads
        String display = incomingName.length() > 0 ? incomingName
                : NumberGeo.pretty(NumberGeo.normalize(number));
        avatar.setPerson(display);
        nameView.setText(display);
        numberView.setText(NumberGeo.pretty(NumberGeo.normalize(number)));
        blockLabel.setText(BlockedStore.isBlocked(this, number)
                ? R.string.unblock : R.string.block);

        buildInfoAsync();
    }

    private String displayName() {
        if (incomingName.length() > 0) {
            return incomingName;
        }
        if (info.displayName.length() > 0) {
            return info.displayName;
        }
        return NumberGeo.pretty(NumberGeo.normalize(number));
    }

    private void buildInfoAsync() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NumberGeo.Info geo = NumberGeo.analyze(number);

                List<ContactEntry> contacts = ContactsStore.load(CallerDetailActivity.this);
                boolean contactsDemo = contacts == null;
                if (contacts == null) {
                    contacts = SampleData.contacts();
                }
                Map<String, ContactEntry> index = ContactsStore.index(contacts);
                ContactEntry match = index.get(NumberGeo.groupKey(number));

                List<CallEntry> raw = CallLogStore.loadRaw(CallerDetailActivity.this);
                boolean callsDemo = raw == null;
                if (raw == null) {
                    raw = SampleData.history(number);
                }

                String key = NumberGeo.groupKey(number);
                List<CallEntry> history = new ArrayList<CallEntry>();
                long first = 0L;
                long last = 0L;
                int total = 0;
                int missed = 0;
                long duration = 0L;
                int shortCalls = 0;
                for (int i = 0; i < raw.size(); i++) {
                    CallEntry entry = raw.get(i);
                    if (!NumberGeo.groupKey(entry.number).equals(key)) {
                        continue;
                    }
                    history.add(entry);
                    total++;
                    if (entry.type == 3) {
                        missed++;
                    }
                    duration += entry.duration;
                    if (entry.duration > 0 && entry.duration < 8) {
                        shortCalls++;
                    }
                    if (first == 0L || entry.date < first) {
                        first = entry.date;
                    }
                    if (entry.date > last) {
                        last = entry.date;
                    }
                    if (history.size() >= 40) {
                        break;
                    }
                }

                final CallerInfo built = new CallerInfo();
                built.number = number;
                built.geo = geo;
                built.history = history;
                built.firstDate = first;
                built.lastDate = last;
                built.totalCalls = total;
                built.missedCalls = missed;
                built.totalDuration = duration;
                built.shortCalls = shortCalls;
                built.blocked = BlockedStore.isBlocked(CallerDetailActivity.this, number);
                built.inContacts = match != null;
                if (match != null) {
                    built.contactId = match.id;
                    built.starred = match.starred;
                    built.displayName = match.name;
                } else if (incomingName.length() > 0) {
                    built.displayName = incomingName;
                }
                built.risk = RiskAnalyzer.analyze(geo, total, shortCalls,
                        built.blocked, built.inContacts);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }
                        info = built;
                        render();
                    }
                });
            }
        }).start();
    }

    private void render() {
        String display = displayName();
        avatar.setPerson(display);
        nameView.setText(display);
        numberView.setText(NumberGeo.pretty(NumberGeo.normalize(number)));

        NumberGeo.Info geo = info.geo;
        if (geo.region.length() > 0) {
            chipLocation.setText(geo.region);
            chipLocation.setVisibility(View.VISIBLE);
        } else if (geo.country.length() > 0) {
            chipLocation.setText(geo.country);
            chipLocation.setVisibility(View.VISIBLE);
        } else {
            chipLocation.setVisibility(View.GONE);
        }
        if (geo.carrier.length() > 0) {
            chipCarrier.setText(geo.carrier);
            chipCarrier.setVisibility(View.VISIBLE);
        } else {
            chipCarrier.setVisibility(View.GONE);
        }
        chipType.setText(NumberGeo.typeLabel(geo.type));
        chipType.setVisibility(View.VISIBLE);

        renderRisk();
        renderInfoRows();
        renderHistory();

        blockLabel.setText(info.blocked ? R.string.unblock : R.string.block);
    }

    private void renderRisk() {
        if (!Prefs.showRisk(this)) {
            riskBanner.setVisibility(View.GONE);
            return;
        }
        riskBanner.setVisibility(View.VISIBLE);
        RiskAnalyzer.Risk risk = info.risk;
        riskTitle.setText(risk.title);
        riskDesc.setText(risk.desc);
        if (risk.level >= RiskAnalyzer.LEVEL_SPAM) {
            riskBanner.setBackgroundResource(R.drawable.bg_banner_danger);
            riskIcon.setImageResource(R.drawable.ic_warning);
            riskIcon.setColorFilter(getColor(R.color.light_danger));
        } else if (risk.level == RiskAnalyzer.LEVEL_WATCH) {
            riskBanner.setBackgroundResource(R.drawable.bg_banner_warn);
            riskIcon.setImageResource(R.drawable.ic_warning);
            riskIcon.setColorFilter(getColor(R.color.light_warning));
        } else {
            riskBanner.setBackgroundResource(R.drawable.bg_banner_safe);
            riskIcon.setImageResource(R.drawable.ic_shield);
            riskIcon.setColorFilter(getColor(R.color.light_success));
        }
    }

    private void renderInfoRows() {
        infoCard.removeAllViews();
        NumberGeo.Info geo = info.geo;
        String pretty = NumberGeo.pretty(NumberGeo.normalize(number));

        addRow(getString(R.string.number), pretty);
        addRow(getString(R.string.type), NumberGeo.typeLabel(geo.type));
        if (geo.carrier.length() > 0) {
            addRow(getString(R.string.carrier), geo.carrier);
        }
        String where = geo.region.length() > 0 ? geo.region
                : (geo.country.length() > 0 ? geo.country : getString(R.string.region_unknown));
        addRow(getString(R.string.location), where);
        if (geo.country.length() > 0 && geo.region.length() > 0) {
            addRow(getString(R.string.country), geo.country
                    + (geo.dialCode.length() > 0 ? " (" + geo.dialCode + ")" : ""));
        }
        if (info.firstDate > 0) {
            addRow(getString(R.string.first_call), TimeUtils.full(info.firstDate));
        }
        if (info.lastDate > 0) {
            addRow(getString(R.string.last_call), TimeUtils.full(info.lastDate));
        }
        addRow(getString(R.string.total_calls),
                String.valueOf(info.totalCalls) + (info.missedCalls > 0
                        ? "  (" + info.missedCalls + " missed)" : ""));
        addRow(getString(R.string.total_time), TimeUtils.duration(info.totalDuration / 1000L));
        addRow("Status", info.blocked ? "Blocked in Aurora"
                : (info.inContacts ? "In your contacts" : "Not in contacts"));
    }

    private void addRow(String label, String value) {
        View row = getLayoutInflater().inflate(R.layout.item_info_row, infoCard, false);
        TextView labelView = (TextView) row.findViewById(R.id.infoLabel);
        TextView valueView = (TextView) row.findViewById(R.id.infoValue);
        labelView.setText(label);
        valueView.setText(value);
        infoCard.addView(row);
    }

    private void renderHistory() {
        historyCard.removeAllViews();
        if (info.history.isEmpty()) {
            historyEmpty.setVisibility(View.VISIBLE);
            historyCard.setVisibility(View.GONE);
            return;
        }
        historyEmpty.setVisibility(View.GONE);
        historyCard.setVisibility(View.VISIBLE);
        int limit = Math.min(info.history.size(), 12);
        for (int i = 0; i < limit; i++) {
            CallEntry entry = info.history.get(i);
            View row = getLayoutInflater().inflate(R.layout.item_history, historyCard, false);
            ImageView icon = (ImageView) row.findViewById(R.id.histIcon);
            TextView type = (TextView) row.findViewById(R.id.histType);
            TextView date = (TextView) row.findViewById(R.id.histDate);
            TextView duration = (TextView) row.findViewById(R.id.histDuration);

            String typeLabel;
            if (entry.type == 1) {
                typeLabel = getString(R.string.incoming);
                icon.setImageResource(R.drawable.ic_call_received);
                icon.setColorFilter(getColor(R.color.light_success));
            } else if (entry.type == 2) {
                typeLabel = getString(R.string.outgoing);
                icon.setImageResource(R.drawable.ic_call_made);
                icon.setColorFilter(getColor(R.color.light_primary));
            } else if (entry.type == 4) {
                typeLabel = getString(R.string.voicemail);
                icon.setImageResource(R.drawable.ic_message);
                icon.setColorFilter(getColor(R.color.light_warning));
            } else {
                typeLabel = getString(R.string.missed);
                icon.setImageResource(R.drawable.ic_call_missed);
                icon.setColorFilter(getColor(R.color.light_danger));
            }
            type.setText(typeLabel);
            date.setText(TimeUtils.full(entry.date));
            duration.setText(entry.duration > 0
                    ? TimeUtils.duration(entry.duration) : "—");
            historyCard.addView(row);
        }
    }

    private void toggleBlock() {
        boolean now = !info.blocked;
        BlockedStore.setBlocked(this, number, now);
        info.blocked = now;
        blockLabel.setText(now ? R.string.unblock : R.string.block);
        toast(now ? getString(R.string.blocked_toast) : getString(R.string.unblocked_toast));
        renderRisk();
    }

    private void placeCall() {
        if (Actions.dial(this, number)) {
            return;
        }
        if (!Actions.hasDialPermission(this)) {
            Actions.requestDialPermission(this, REQ_DIAL);
            return;
        }
        toast(getString(R.string.no_phone_app));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_DIAL && Actions.hasDialPermission(this)) {
            Actions.dial(this, number);
        }
    }

    private void showMenu(View anchor) {
        final PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_number_actions, popup.getMenu());
        popup.getMenu().findItem(R.id.action_block)
                .setTitle(info.blocked ? R.string.unblock : R.string.block);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.action_call) {
                    placeCall();
                    return true;
                }
                if (id == R.id.action_message) {
                    Actions.sms(CallerDetailActivity.this, number);
                    return true;
                }
                if (id == R.id.action_block) {
                    toggleBlock();
                    return true;
                }
                if (id == R.id.action_copy) {
                    Actions.copy(CallerDetailActivity.this, number);
                    return true;
                }
                if (id == R.id.action_share) {
                    Actions.share(CallerDetailActivity.this,
                            displayName() + " — " + number + " (Aurora Phone)");
                    return true;
                }
                if (id == R.id.action_details) {
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }

    private void refreshNote() {
        String note = Prefs.note(this, number);
        if (note.length() == 0) {
            noteText.setText(R.string.note_empty);
            noteText.setTextColor(colorAttr(R.attr.av_colorTextSecondary));
            btnEditNote.setText(R.string.add_note);
        } else {
            noteText.setText(note);
            noteText.setTextColor(colorAttr(R.attr.av_colorText));
            btnEditNote.setText(R.string.edit_note);
        }
    }

    private void editNote() {
        final EditText input = new EditText(this);
        input.setHint(R.string.note_hint);
        input.setText(Prefs.note(this, number));
        input.setMinLines(2);
        input.setGravity(android.view.Gravity.TOP);
        input.setInputType(android.text.InputType.TYPE_CLASS_TEXT
                | android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        input.setSelection(input.getText().length());
        android.widget.FrameLayout wrapper = new android.widget.FrameLayout(this);
        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        wrapper.setPadding(pad, 0, pad, 0);
        wrapper.addView(input);
        new AlertDialog.Builder(this)
                .setTitle(R.string.note_title)
                .setView(wrapper)
                .setPositiveButton(R.string.note_saved, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.setNote(CallerDetailActivity.this, number,
                                input.getText().toString());
                        refreshNote();
                        toast(getString(R.string.note_saved));
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .setNeutralButton(R.string.clear_all, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Prefs.setNote(CallerDetailActivity.this, number, "");
                        refreshNote();
                    }
                })
                .show();
    }
}
