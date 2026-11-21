package com.aurora.phone;

import android.content.Intent;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.aurora.phone.data.ContactsStore;
import com.aurora.phone.data.SampleData;
import com.aurora.phone.util.Actions;
import com.aurora.phone.util.NumberGeo;

/** Classic keypad with T9 suggestions and live number intel. */
public class DialpadActivity extends BaseActivity {

    private EditText dialNumber;
    private TextView dialInfo;
    private LinearLayout suggestions;
    private HorizontalScrollView suggestionsScroll;
    private ImageView backspace;

    private List<ContactEntry> contacts = new ArrayList<ContactEntry>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialpad);

        dialNumber = (EditText) findViewById(R.id.dialNumber);
        dialInfo = (TextView) findViewById(R.id.dialInfo);
        suggestions = (LinearLayout) findViewById(R.id.suggestions);
        suggestionsScroll = (HorizontalScrollView) findViewById(R.id.suggestionsScroll);
        backspace = (ImageView) findViewById(R.id.btnBackspace);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeCall();
            }
        });
        findViewById(R.id.btnAddContact).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String number = currentNumber();
                if (number.length() == 0) {
                    toast("Enter a number first");
                    return;
                }
                Actions.addToContacts(DialpadActivity.this, number);
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

        String preset = getIntent().getStringExtra("number");
        if (preset != null) {
            dialNumber.setText(preset);
            dialNumber.setSelection(preset.length());
        }
        loadContacts();
        updateState(dialNumber.getText().toString());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadContacts();
    }

    private void loadContacts() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ContactEntry> loaded = ContactsStore.load(DialpadActivity.this);
                if (loaded == null) {
                    loaded = SampleData.contacts();
                }
                final List<ContactEntry> result = loaded;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        contacts = result;
                        updateState(dialNumber.getText().toString());
                    }
                });
            }
        }).start();
    }

    /** XML onClick handler for every key. */
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

    private void clickTick() {
        if (!Prefs.haptics(this)) {
            return;
        }
        Vibrator vibrator = (Vibrator) getSystemService(VIBRATOR_SERVICE);
        if (vibrator != null) {
            vibrator.vibrate(VibrationEffect.createOneShot(28,
                    VibrationEffect.DEFAULT_AMPLITUDE));
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

    private String currentNumber() {
        return dialNumber.getText().toString().trim();
    }

    private void placeCall() {
        String number = currentNumber();
        if (number.length() == 0) {
            toast("Enter a number first");
            return;
        }
        if (Actions.dial(this, number)) {
            return;
        }
        Intent intent = new Intent(this, CallScreenActivity.class);
        intent.putExtra(CallScreenActivity.EXTRA_NUMBER, number);
        intent.putExtra(CallScreenActivity.EXTRA_MODE, CallScreenActivity.MODE_PREVIEW);
        startActivity(intent);
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
        LayoutInflater inflater = LayoutInflater.from(this);
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
            if (number.startsWith("+") && NumberGeo.digits(entry.number)
                    .contains(digits)) {
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
                    dialNumber.setText(hit.number);
                    dialNumber.setSelection(hit.number.length());
                }
            });
            suggestions.addView(chip);
            shown++;
        }
        suggestionsScroll.setVisibility(shown > 0 ? View.VISIBLE : View.GONE);
    }
}
