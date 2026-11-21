package com.aurora.phone;

import android.os.Bundle;
import android.view.View;

import com.aurora.phone.util.DialpadController;

/** Classic keypad screen (also reachable via intents); logic lives in DialpadController. */
public class DialpadActivity extends BaseActivity {

    private DialpadController controller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialpad);

        controller = new DialpadController(this, findViewById(R.id.dialpadRoot));

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        String preset = getIntent().getStringExtra("number");
        if (preset != null) {
            controller.setNumber(preset);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        controller.refresh();
    }

    /** XML onClick handler for every key. */
    public void onDialKey(View v) {
        controller.onDialKey(v);
    }
}
