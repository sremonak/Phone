package com.aurora.phone;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.CallLog;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.List;
import java.util.Map;

import com.aurora.phone.data.BlockedStore;
import com.aurora.phone.data.ContactsStore;
import com.aurora.phone.data.SampleData;
import com.aurora.phone.util.Actions;
import com.aurora.phone.util.NumberGeo;
import com.aurora.phone.util.RiskAnalyzer;
import com.aurora.phone.util.TimeUtils;
import com.aurora.phone.view.AvatarView;

/**
 * Full-screen caller ID: incoming-call simulation (answer / decline,
 * running timer) and a preview mode used when the call permission is missing.
 */
public class CallScreenActivity extends BaseActivity {

    public static final String EXTRA_NUMBER = "number";
    public static final String EXTRA_NAME = "name";
    public static final String EXTRA_MODE = "mode";

    public static final String MODE_INCOMING = "incoming";
    public static final String MODE_PREVIEW = "preview";

    private static final int REQ_DIAL = 71;

    private String number = "";
    private String displayName = "";
    private String mode = MODE_INCOMING;

    private TextView callStatus;
    private TextView callName;
    private TextView callNumber;
    private TextView callTimer;
    private TextView chipLocation;
    private TextView chipCarrier;
    private TextView chipRisk;
    private AvatarView callAvatar;
    private LinearLayout incomingRow;
    private LinearLayout activeCallRow;
    private LinearLayout controlsRow;
    private LinearLayout previewRow;

    private boolean connected = false;
    private long connectedAt = 0L;
    private final Handler timerHandler = new Handler(Looper.getMainLooper());
    private boolean muted = false;
    private boolean speaker = false;

    private final Runnable ticker = new Runnable() {
        @Override
        public void run() {
            if (connected && isUiAlive()) {
                long seconds = (System.currentTimeMillis() - connectedAt) / 1000L;
                callTimer.setText(TimeUtils.timer(seconds));
                timerHandler.postDelayed(this, 1000L);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call_screen);

        number = safeExtra(EXTRA_NUMBER);
        displayName = safeExtra(EXTRA_NAME);
        mode = safeExtra(EXTRA_MODE);
        if (mode.length() == 0) {
            mode = MODE_INCOMING;
        }

        callStatus = (TextView) findViewById(R.id.callStatus);
        callName = (TextView) findViewById(R.id.callName);
        callNumber = (TextView) findViewById(R.id.callNumber);
        callTimer = (TextView) findViewById(R.id.callTimer);
        chipLocation = (TextView) findViewById(R.id.chipLocation);
        chipCarrier = (TextView) findViewById(R.id.chipCarrier);
        chipRisk = (TextView) findViewById(R.id.chipRisk);
        callAvatar = (AvatarView) findViewById(R.id.callAvatar);
        incomingRow = (LinearLayout) findViewById(R.id.incomingRow);
        activeCallRow = (LinearLayout) findViewById(R.id.activeCallRow);
        controlsRow = (LinearLayout) findViewById(R.id.controlsRow);
        previewRow = (LinearLayout) findViewById(R.id.previewRow);

        String pretty = NumberGeo.pretty(NumberGeo.normalize(number));
        callNumber.setText(pretty);

        startAmbientAnimations();

        findViewById(R.id.btnDecline).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnAnswer).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connect();
            }
        });
        findViewById(R.id.btnEndCall).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                endCall();
            }
        });
        findViewById(R.id.btnQuickMsg).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Actions.sms(CallScreenActivity.this, number);
            }
        });
        findViewById(R.id.btnMute).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                muted = !muted;
                ImageView icon = (ImageView) findViewById(R.id.muteIcon);
                icon.setImageResource(muted ? R.drawable.ic_mic_off : R.drawable.ic_mic);
                icon.setAlpha(muted ? 1.0f : 0.75f);
            }
        });
        findViewById(R.id.btnSpeaker).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                speaker = !speaker;
                ImageView icon = (ImageView) findViewById(R.id.speakerIcon);
                icon.setAlpha(speaker ? 1.0f : 0.75f);
                icon.setColorFilter(speaker
                        ? getColor(R.color.dark_accent) : getColor(R.color.white));
            }
        });
        findViewById(R.id.btnCallNow).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                placeCall();
            }
        });

        if (MODE_PREVIEW.equals(mode)) {
            callStatus.setText("Caller preview");
            incomingRow.setVisibility(View.GONE);
            previewRow.setVisibility(View.VISIBLE);
        } else {
            callStatus.setText(getString(R.string.incoming_call));
            incomingRow.setVisibility(View.VISIBLE);
            previewRow.setVisibility(View.GONE);
            findViewById(R.id.btnAnswer).animate().alpha(1f).start();
        }

        loadIntel();
    }

    @Override
    protected void onDestroy() {
        timerHandler.removeCallbacks(ticker);
        super.onDestroy();
    }

    private String safeExtra(String key) {
        String value = getIntent().getStringExtra(key);
        return value == null ? "" : value;
    }

    private void loadIntel() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                NumberGeo.Info geo = NumberGeo.analyze(number);

                List<ContactEntry> contacts = ContactsStore.load(CallScreenActivity.this);
                if (contacts == null) {
                    contacts = SampleData.contacts();
                }
                Map<String, ContactEntry> index = ContactsStore.index(contacts);
                ContactEntry match = index.get(NumberGeo.groupKey(number));
                final String resolvedName = match != null ? match.name
                        : (displayName.length() > 0 ? displayName
                                : NumberGeo.pretty(NumberGeo.normalize(number)));
                final NumberGeo.Info finalGeo = geo;
                final boolean blocked = BlockedStore.isBlocked(CallScreenActivity.this, number);
                final RiskAnalyzer.Risk risk = RiskAnalyzer.analyze(geo, 0, 0, blocked,
                        match != null);

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }
                        displayName = resolvedName;
                        callName.setText(resolvedName);
                        callAvatar.setPerson(resolvedName);

                        if (finalGeo.region.length() > 0) {
                            chipLocation.setText(finalGeo.region);
                            chipLocation.setVisibility(View.VISIBLE);
                        } else if (finalGeo.country.length() > 0) {
                            chipLocation.setText(finalGeo.country);
                            chipLocation.setVisibility(View.VISIBLE);
                        }
                        if (finalGeo.carrier.length() > 0) {
                            chipCarrier.setText(finalGeo.carrier);
                            chipCarrier.setVisibility(View.VISIBLE);
                        }
                        if (risk.level >= RiskAnalyzer.LEVEL_WATCH) {
                            chipRisk.setText(risk.title);
                            chipRisk.setVisibility(View.VISIBLE);
                        } else if (blocked) {
                            chipRisk.setText("Blocked");
                            chipRisk.setVisibility(View.VISIBLE);
                        }
                    }
                });
            }
        }).start();
    }

    private void connect() {
        if (connected) {
            return;
        }
        connected = true;
        connectedAt = System.currentTimeMillis();
        callStatus.setText(getString(R.string.answer).replace("Answer", "Connected"));
        callStatus.setText("Connected");
        incomingRow.setVisibility(View.GONE);
        controlsRow.setVisibility(View.VISIBLE);
        activeCallRow.setVisibility(View.VISIBLE);
        callTimer.setVisibility(View.VISIBLE);
        callTimer.setText(TimeUtils.timer(0));
        timerHandler.postDelayed(ticker, 1000L);
    }

    private void endCall() {
        timerHandler.removeCallbacks(ticker);
        connected = false;
        callStatus.setText(getString(R.string.call_ended));
        controlsRow.setVisibility(View.GONE);
        activeCallRow.setVisibility(View.GONE);
        callTimer.setVisibility(View.GONE);
        timerHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isUiAlive()) {
                    finish();
                }
            }
        }, 900L);
    }

    private void placeCall() {
        if (Actions.dial(this, number)) {
            finish();
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
            finish();
        }
    }

    /** Pulsing rings around the avatar (incoming) + slow drifting aurora orbs. */
    private void startAmbientAnimations() {
        View ringOne = findViewById(R.id.pulseRingOne);
        View ringTwo = findViewById(R.id.pulseRingTwo);
        View orbOne = findViewById(R.id.orbOne);
        View orbTwo = findViewById(R.id.orbTwo);

        if (orbOne != null) {
            orbOne.animate().translationX(-26f).translationY(18f)
                    .setDuration(4200).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (isUiAlive() && findViewById(R.id.orbOne) != null) {
                                findViewById(R.id.orbOne).animate()
                                        .translationX(0f).translationY(0f)
                                        .setDuration(4200).withEndAction(this).start();
                            }
                        }
                    }).start();
        }
        if (orbTwo != null) {
            orbTwo.animate().translationX(22f).translationY(-16f)
                    .setDuration(5000).withEndAction(new Runnable() {
                        @Override
                        public void run() {
                            if (isUiAlive() && findViewById(R.id.orbTwo) != null) {
                                findViewById(R.id.orbTwo).animate()
                                        .translationX(0f).translationY(0f)
                                        .setDuration(5000).withEndAction(this).start();
                            }
                        }
                    }).start();
        }

        if (mode.equals(MODE_INCOMING) && ringOne != null && ringTwo != null) {
            pulse(ringOne, 0L, 1600);
            pulse(ringTwo, 400L, 1600);
        }
    }

    private void pulse(final View ring, long startDelay, final long duration) {
        ring.setScaleX(0.6f);
        ring.setScaleY(0.6f);
        ring.setAlpha(0.9f);
        ring.animate().scaleX(1.35f).scaleY(1.35f).alpha(0f)
                .setStartDelay(startDelay).setDuration(duration)
                .withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (isUiAlive() && mode.equals(MODE_INCOMING)) {
                            ring.setScaleX(0.6f);
                            ring.setScaleY(0.6f);
                            ring.setAlpha(0.9f);
                            pulse(ring, 0L, duration);
                        }
                    }
                }).start();
    }
}
