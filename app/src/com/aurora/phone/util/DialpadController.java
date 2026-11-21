package com.aurora.phone.util;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.aurora.phone.BaseActivity;
import com.aurora.phone.ContactEntry;
import com.aurora.phone.Prefs;
import com.aurora.phone.R;
import com.aurora.phone.data.ContactsStore;
import com.aurora.phone.data.SampleData;

/**
 * Shared keypad logic: T9 suggestions, live number intel, haptics and
 * speed dial (long-press keys2–9). Used by both DialpadActivity and the
 * embedded keypad tab in MainActivity.
 */
public class DialpadController {

    private final BaseActivity activity;
    private final View root;
    private final EditText dialNumber;
    private final TextView dialInfo;
    private final LinearLayout suggestions;
    private final HorizontalScrollView suggestionsScroll;
    private final ImageView backspace;

    private List<ContactEntry> contacts = new ArrayList<ContactEntry>();

    public DialpadController(BaseActivity act, View root) {
        this.activity = act;
        this.root = root;
        dialNumber = (EditText) root.findViewById(R.id.dialNumber);
        dialInfo = (TextView) root.findViewById(R.id.dialInfo);
        suggestions = (LinearLayout) root.findViewById(R.id.suggestions);
        suggestionsScroll = (HorizontalScrollView) root.findViewById(R.id.suggestionsScroll);
        backspace = (ImageView) root.findViewById(R.id.btnBackspace);

        root.findViewById(R.id.btnCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeCall();
            }
        });
        root.findViewById(R.id.btnAddContact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = currentNumber();
                if (number.length() == 0) {
                    activity.toast("Enter a number first");
                    return;
                }
                Actions.addToContacts(activity, number);
            }
        });
        backspace.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                deleteOne();
            }
        });
        backspace.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                dialNumber.setText("");
                return true;
            }
        });
        dialNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                updateState(s.toString());
            }
        });

        wireSpeedDialKeys(root);
        loadContacts();
        updateState(dialNumber.getText().toString());
    }

    /** XML onClick handler — activities delegate here. */
    public void onDialKey(View v) {
        Object tag = v.getTag();
        if (tag == null) {
            return;
        }
        String key = tag.toString();
        int start = dialNumber.getSelectionStart();
        int end = dialNumber.getSelectionEnd();
        Editable text = dialNumber.getText();
        if (start < 0) {
            start = text.length();
        }
        if (end < 0) {
            end = text.length();
        }
        text.replace(Math.min(start, end), Math.max(start, end), key);
        int pos = Math.min(start, end) + 1;
        dialNumber.setSelection(Math.min(pos, text.length()));
        clickTick();
    }

    public void refresh() {
        loadContacts();
    }

    public String currentNumber() {
        return dialNumber.getText().toString().trim();
    }

    public void setNumber(String number) {
        dialNumber.setText(number);
        dialNumber.setSelection(number.length());
    }

    private void clickTick() {
        if (!Prefs.haptics(activity)) {
            return;
        }
        android.os.Vibrator vibrator = (android.os.Vibrator)
                activity.getSystemService(android.content.Context.VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(28,
                    android.os.VibrationEffect.DEFAULT_AMPLITUDE));
        }
    }

    private void deleteOne() {
        Editable text = dialNumber.getText();
        int pos = dialNumber.getSelectionStart();
        if (pos < 0) {
            pos = text.length();
        }
        if (pos == 0) {
            return;
        }
        text.delete(pos - 1, pos);
        clickTick();
    }

    private void placeCall() {
        String number = currentNumber();
        if (number.length() == 0) {
            activity.toast("Enter a number first");
            return;
        }
        if (Actions.dial(activity, number)) {
            return;
        }
        android.content.Intent intent = new android.content.Intent(activity,
                com.aurora.phone.CallScreenActivity.class);
        intent.putExtra(com.aurora.phone.CallScreenActivity.EXTRA_NUMBER, number);
        intent.putExtra(com.aurora.phone.CallScreenActivity.EXTRA_MODE,
                com.aurora.phone.CallScreenActivity.MODE_PREVIEW);
        activity.startActivity(intent);
    }

    private void updateState(String value) {
        String number = value.trim();
        backspace.setVisibility(number.length() > 0 ? View.VISIBLE : View.INVISIBLE);

        if (number.length() >= 3) {
            NumberGeo.Info info = NumberGeo.analyze(number);
            StringBuilder sb = new StringBuilder();
            if (info.carrier.length() > 0) {
                sb.append(info.carrier);
            }
            String where = NumberGeo.shortLabel(info);
            if (where.length() > 0) {
                if (sb.length() > 0) {
                    sb.append(" · ");
                }
                sb.append(where);
            }
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(NumberGeo.typeLabel(info.type));
            dialInfo.setText(sb.toString());
            dialInfo.setVisibility(View.VISIBLE);
        } else {
            dialInfo.setVisibility(View.GONE);
        }

        renderSuggestions(number);
    }

    private void renderSuggestions(String number) {
        suggestions.removeAllViews();
        if (number.length() < 1 || contacts.isEmpty()) {
            suggestionsScroll.setVisibility(View.GONE);
            return;
        }
        String digits = NumberGeo.digits(number);
        String t9 = NumberGeo.t9(number);
        LayoutInflater inflater = LayoutInflater.from(activity);
        int shown = 0;
        for (int i = 0; i < contacts.size() && shown < 6; i++) {
            ContactEntry entry = contacts.get(i);
            boolean match = false;
            if (digits.length() > 0 && NumberGeo.digits(entry.number).contains(digits)) {
                match = true;
            }
            if (t9.length() > 0 && NumberGeo.t9(entry.name).startsWith(t9)) {
                match = true;
            }
            if (!match) {
                continue;
            }
            final ContactEntry hit = entry;
            View chip = inflater.inflate(R.layout.item_suggestion, suggestions, false);
            TextView name = (TextView) chip.findViewById(R.id.suggName);
            TextView num = (TextView) chip.findViewById(R.id.suggNumber);
            name.setText(entry.name);
            num.setText(NumberGeo.pretty(NumberGeo.normalize(entry.number)));
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setNumber(hit.number);
                }
            });
            suggestions.addView(chip);
            shown++;
        }
        suggestionsScroll.setVisibility(shown > 0 ? View.VISIBLE : View.GONE);
    }

    private void loadContacts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ContactEntry> loaded = ContactsStore.load(activity);
                if (loaded == null) {
                    loaded = SampleData.contacts();
                }
                final List<ContactEntry> result = loaded;
                activity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!activity.isUiAlive()) {
                            return;
                        }
                        contacts = result;
                        updateState(dialNumber.getText().toString());
                        refreshSpeedDialLabels();
                    }
                });
            }
        }).start();
    }

    // ---------------- speed dial ----------------

    private void wireSpeedDialKeys(View view) {
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                wireSpeedDialKeys(group.getChildAt(i));
            }
        }
        Object tag = view.getTag();
        if (tag == null) {
            return;
        }
        String t = tag.toString();
        if (t.length() != 1 || !Character.isDigit(t.charAt(0))) {
            return;
        }
        final char key = t.charAt(0);
        if (key < '2' || key > '9') {
            return;
        }
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                handleSpeedDialLongPress(key);
                return true;
            }
        });
    }

    private void handleSpeedDialLongPress(final char key) {
        final String current = currentNumber();
        final String existing = Prefs.speedDial(activity, key);
        final java.util.List<String> actions = new java.util.ArrayList<String>();
        final java.util.List<Integer> codes = new java.util.ArrayList<Integer>();

        if (current.length() > 0 && !current.equals(existing)) {
            actions.add(activity.getString(R.string.speed_dial_menu_set,
                    NumberGeo.pretty(NumberGeo.normalize(current))));
            codes.add(Integer.valueOf(1));
        }
        if (existing.length() > 0) {
            actions.add(activity.getString(R.string.speed_dial_menu_call,
                    NumberGeo.pretty(NumberGeo.normalize(existing))));
            codes.add(Integer.valueOf(2));
            actions.add(activity.getString(R.string.speed_dial_clear));
            codes.add(Integer.valueOf(3));
        }
        if (actions.isEmpty()) {
            activity.toast(activity.getString(R.string.speed_dial_none));
            return;
        }

        new AlertDialog.Builder(activity)
                .setTitle(activity.getString(R.string.speed_dial_title) + " \u00b7 " + key)
                .setItems(actions.toArray(new CharSequence[actions.size()]),
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                int code = codes.get(which).intValue();
                                if (code == 1) {
                                    Prefs.setSpeedDial(activity, key, current);
                                    activity.toast(activity.getString(
                                            R.string.speed_dial_set_toast, key));
                                    refreshSpeedDialLabels();
                                } else if (code == 2) {
                                    Actions.dial(activity, existing);
                                } else if (code == 3) {
                                    Prefs.setSpeedDial(activity, key, "");
                                    activity.toast(activity.getString(R.string.speed_dial_clear));
                                    refreshSpeedDialLabels();
                                }
                            }
                        })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    /** Replace key letters with the assigned contact name (premium touch). */
    private void refreshSpeedDialLabels() {
        for (char key = '2'; key <= '9'; key++) {
            String number = Prefs.speedDial(activity, key);
            View keyView = findKeyView(root, String.valueOf(key));
            if (keyView == null || !(keyView instanceof ViewGroup)) {
                continue;
            }
            ViewGroup group = (ViewGroup) keyView;
            if (group.getChildCount() < 2) {
                continue;
            }
            View second = group.getChildAt(1);
            if (!(second instanceof TextView)) {
                continue;
            }
            TextView label = (TextView) second;
            if (number.length() == 0) {
                label.setText(lettersFor(key));
                label.setTextColor(activity.getResources().getColor(
                        com.aurora.phone.R.color.label_fallback));
            } else {
                String name = lookupName(number);
                label.setText(name.length() > 0 ? name
                        : NumberGeo.pretty(NumberGeo.normalize(number)));
                android.util.TypedValue tv = new android.util.TypedValue();
                if (activity.getTheme().resolveAttribute(
                        com.aurora.phone.R.attr.av_colorPrimary, tv, true)) {
                    label.setTextColor(tv.data);
                }
            }
        }
    }

    private String lettersFor(char key) {
        switch (key) {
            case '2': return "ABC";
            case '3': return "DEF";
            case '4': return "GHI";
            case '5': return "JKL";
            case '6': return "MNO";
            case '7': return "PQRS";
            case '8': return "TUV";
            case '9': return "WXYZ";
            default: return "";
        }
    }

    private String lookupName(String number) {
        String key = NumberGeo.groupKey(number);
        for (int i = 0; i < contacts.size(); i++) {
            if (NumberGeo.groupKey(contacts.get(i).number).equals(key)) {
                return contacts.get(i).name;
            }
        }
        return "";
    }

    private View findKeyView(View view, String tag) {
        if (tag.equals(String.valueOf(view.getTag()))) {
            return view;
        }
        if (view instanceof ViewGroup) {
            ViewGroup group = (ViewGroup) view;
            for (int i = 0; i < group.getChildCount(); i++) {
                View found = findKeyView(group.getChildAt(i), tag);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }
}
