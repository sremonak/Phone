package com.aurora.phone;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.aurora.phone.adapter.SearchAdapter;
import com.aurora.phone.data.CallLogStore;
import com.aurora.phone.data.ContactsStore;
import com.aurora.phone.data.SampleData;

/** Global search: contacts + recents with recent-query chips. */
public class SearchActivity extends BaseActivity implements SearchAdapter.Listener {

    private EditText searchInput;
    private LinearLayout recentBlock;
    private LinearLayout recentChips;
    private ListView searchList;
    private TextView noResults;

    private SearchAdapter adapter;
    private List<ContactEntry> contacts = new ArrayList<ContactEntry>();
    private List<RecentItem> recents = new ArrayList<RecentItem>();
    private String query = "";
    private boolean loaded = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        searchInput = (EditText) findViewById(R.id.searchInput);
        recentBlock = (LinearLayout) findViewById(R.id.recentBlock);
        recentChips = (LinearLayout) findViewById(R.id.recentChips);
        searchList = (ListView) findViewById(R.id.searchList);
        noResults = (TextView) findViewById(R.id.noResults);

        adapter = new SearchAdapter(this);
        adapter.setListener(this);
        searchList.setAdapter(adapter);

        findViewById(R.id.btnSearchClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        findViewById(R.id.btnClearSearches).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Prefs.clearRecentSearches(SearchActivity.this);
                renderRecentChips();
            }
        });

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                query = s.toString();
                submit();
            }
        });
        searchInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                    Prefs.pushRecentSearch(SearchActivity.this, query);
                    renderRecentChips();
                    InputMethodManager imm = (InputMethodManager)
                            getSystemService(Context.INPUT_METHOD_SERVICE);
                    if (imm != null) {
                        imm.hideSoftInputFromWindow(searchInput.getWindowToken(), 0);
                    }
                    return true;
                }
                return false;
            }
        });

        renderRecentChips();
        searchInput.requestFocus();
        InputMethodManager imm = (InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(searchInput, 0);
        }
        loadData();
    }

    private void renderRecentChips() {
        List<String> recent = Prefs.recentSearchOrdered(this);
        recentChips.removeAllViews();
        boolean has = !recent.isEmpty();
        recentBlock.setVisibility(has || query.length() == 0 ? View.VISIBLE : View.GONE);
        if (!has) {
            findViewById(R.id.btnClearSearches).setVisibility(View.GONE);
            return;
        }
        findViewById(R.id.btnClearSearches).setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < recent.size(); i++) {
            final String term = recent.get(i);
            View chip = inflater.inflate(R.layout.item_suggestion, recentChips, false);
            // suggestion layout has two text lines; reuse name line only
            TextView name = (TextView) chip.findViewById(R.id.suggName);
            TextView num = (TextView) chip.findViewById(R.id.suggNumber);
            name.setText(term);
            num.setVisibility(View.GONE);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    searchInput.setText(term);
                    searchInput.setSelection(term.length());
                }
            });
            recentChips.addView(chip);
        }
    }

    private void loadData() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<ContactEntry> c = ContactsStore.load(SearchActivity.this);
                List<RecentItem> r = CallLogStore.loadGrouped(SearchActivity.this);
                final boolean demo = c == null || r == null;
                if (c == null) {
                    c = SampleData.contacts();
                }
                if (r == null) {
                    r = SampleData.recents();
                }
                final List<ContactEntry> fc = c;
                final List<RecentItem> fr = r;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }
                        contacts = fc;
                        recents = fr;
                        adapter.setContactIndex(ContactsStore.index(fc));
                        loaded = true;
                        submit();
                    }
                });
            }
        }).start();
    }

    private void submit() {
        if (!loaded) {
            return;
        }
        adapter.submit(query, contacts, recents);
        boolean empty = query.length() > 0 && adapter.isEmptyResult();
        noResults.setVisibility(empty ? View.VISIBLE : View.GONE);
        searchList.setVisibility(empty ? View.GONE : View.VISIBLE);
        recentBlock.setVisibility(query.length() == 0 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSearchContact(ContactEntry entry) {
        Prefs.pushRecentSearch(this, entry.name);
        openDetail(entry.number, entry.name);
    }

    @Override
    public void onSearchRecent(RecentItem item) {
        String name = item.name != null && item.name.length() > 0
                ? item.name : NumberGeoName(item.number);
        openDetail(item.number, name);
    }

    private String NumberGeoName(String number) {
        ContactEntry hit = ContactsStore.index(contacts).get(
                com.aurora.phone.util.NumberGeo.groupKey(number));
        if (hit != null) {
            return hit.name;
        }
        return com.aurora.phone.util.NumberGeo.pretty(number);
    }

    private void openDetail(String number, String name) {
        android.content.Intent intent = new android.content.Intent(this,
                CallerDetailActivity.class);
        intent.putExtra(CallerDetailActivity.EXTRA_NUMBER, number);
        intent.putExtra(CallerDetailActivity.EXTRA_NAME, name);
        startActivity(intent);
    }
}
