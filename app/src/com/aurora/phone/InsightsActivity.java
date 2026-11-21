package com.aurora.phone;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.aurora.phone.data.CallLogStore;
import com.aurora.phone.util.NumberGeo;
import com.aurora.phone.view.WeekChartView;

/** Call statistics: totals,7-day chart and top callers (offline insights). */
public class InsightsActivity extends BaseActivity {

    private TextView statTotal;
    private TextView statMissed;
    private TextView statAvg;
    private TextView statPeople;
    private WeekChartView chart;
    private TextView chartEmpty;
    private LinearLayout topCallers;
    private TextView insightsEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_insights);

        statTotal = (TextView) findViewById(R.id.statTotal);
        statMissed = (TextView) findViewById(R.id.statMissed);
        statAvg = (TextView) findViewById(R.id.statAvg);
        statPeople = (TextView) findViewById(R.id.statPeople);
        chart = (WeekChartView) findViewById(R.id.chart);
        chartEmpty = (TextView) findViewById(R.id.chartEmpty);
        topCallers = (LinearLayout) findViewById(R.id.topCallers);
        insightsEmpty = (TextView) findViewById(R.id.insightsEmpty);

        findViewById(R.id.btnBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        load();
    }

    private void load() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                final List<CallEntry> raw = CallLogStore.loadRaw(InsightsActivity.this);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }
                        render(raw);
                    }
                });
            }
        }).start();
    }

    private void render(List<CallEntry> raw) {
        if (raw == null || raw.isEmpty()) {
            statTotal.setText("0");
            statMissed.setText("0");
            statAvg.setText("0s");
            statPeople.setText("0");
            chartEmpty.setVisibility(View.VISIBLE);
            chart.setVisibility(View.GONE);
            insightsEmpty.setVisibility(View.VISIBLE);
            topCallers.setVisibility(View.GONE);
            return;
        }

        int total = raw.size();
        int missed = 0;
        long durationSum = 0;
        int timedCalls = 0;
        Map<String, Integer> counts = new LinkedHashMap<String, Integer>();
        Map<String, String> names = new LinkedHashMap<String, String>();

        // last7 days buckets: index0 =6 days ago … index6 = today
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        calendar.add(Calendar.DAY_OF_YEAR, -6);
        long weekStart = calendar.getTimeInMillis();
        int[] week = new int[7];
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEE", Locale.US);
        String[] dayLabels = new String[7];
        Calendar tmp = (Calendar) calendar.clone();
        for (int i = 0; i < 7; i++) {
            dayLabels[i] = dayFormat.format(tmp.getTime());
            tmp.add(Calendar.DAY_OF_YEAR, 1);
        }

        for (int i = 0; i < raw.size(); i++) {
            CallEntry e = raw.get(i);
            if (e.type == CallEntry.TYPE_MISSED) {
                missed++;
            }
            if (e.type == CallEntry.TYPE_INCOMING || e.type == CallEntry.TYPE_OUTGOING) {
                durationSum += e.duration;
                timedCalls++;
            }
            String key = NumberGeo.groupKey(e.number);
            Integer c = counts.get(key);
            counts.put(key, Integer.valueOf(c == null ? 1 : c.intValue() + 1));
            if (e.name != null && e.name.length() > 0) {
                names.put(key, e.name);
            }
            if (e.date >= weekStart) {
                int index = (int) ((e.date - weekStart) / 86400000L);
                if (index >= 0 && index < 7) {
                    week[index]++;
                }
            }
        }

        statTotal.setText(String.valueOf(total));
        statMissed.setText(String.valueOf(missed));
        long avg = timedCalls > 0 ? durationSum / timedCalls : 0;
        statAvg.setText(avg >= 60
                ? String.format(Locale.US, "%dm %02ds", avg / 60, avg % 60)
                : avg + "s");
        statPeople.setText(String.valueOf(counts.size()));

        chart.setData(week, dayLabels);
        chartEmpty.setVisibility(chart.isEmpty() ? View.VISIBLE : View.GONE);
        chart.setVisibility(chart.isEmpty() ? View.GONE : View.VISIBLE);

        // top callers
        List<Map.Entry<String, Integer>> top =
                new ArrayList<Map.Entry<String, Integer>>(counts.entrySet());
        Collections.sort(top, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> a, Map.Entry<String, Integer> b) {
                return b.getValue().compareTo(a.getValue());
            }
        });
        topCallers.removeAllViews();
        int show = Math.min(3, top.size());
        if (show == 0) {
            insightsEmpty.setVisibility(View.VISIBLE);
            topCallers.setVisibility(View.GONE);
            return;
        }
        insightsEmpty.setVisibility(View.GONE);
        topCallers.setVisibility(View.VISIBLE);
        int max = top.get(0).getValue().intValue();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < show; i++) {
            Map.Entry<String, Integer> entry = top.get(i);
            View row = inflater.inflate(R.layout.item_top_caller, topCallers, false);
            TextView rank = (TextView) row.findViewById(R.id.rankBadge);
            TextView name = (TextView) row.findViewById(R.id.callerName);
            TextView count = (TextView) row.findViewById(R.id.callerCount);
            final View bar = row.findViewById(R.id.callerBar);

            String display = names.get(entry.getKey());
            if (display == null || display.length() == 0) {
                display = NumberGeo.pretty(entry.getKey());
            }
            rank.setText(String.valueOf(i + 1));
            name.setText(display);
            int calls = entry.getValue().intValue();
            count.setText(calls + (calls == 1 ? " call" : " calls"));
            android.view.ViewGroup.LayoutParams params = bar.getLayoutParams();
            params.width = 0;
            bar.setLayoutParams(params);
            final int targetWidth = Math.round(
                    (calls / (float) max) * dp(180));
            bar.post(new Runnable() {
                @Override
                public void run() {
                    android.view.ViewGroup.LayoutParams p = bar.getLayoutParams();
                    int parent = ((View) bar.getParent()).getWidth() - dp(32);
                    int w = Math.min(targetWidth, Math.max(parent, 0));
                    p.width = Math.max(w, dp(8));
                    bar.setLayoutParams(p);
                }
            });
            topCallers.addView(row);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
