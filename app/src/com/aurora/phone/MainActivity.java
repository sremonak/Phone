package com.aurora.phone;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.CallLog;
import android.view.View;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.aurora.phone.adapter.ContactsAdapter;
import com.aurora.phone.adapter.FavoritesAdapter;
import com.aurora.phone.adapter.RecentsAdapter;
import com.aurora.phone.data.BlockedStore;
import com.aurora.phone.data.CallLogStore;
import com.aurora.phone.data.ContactsStore;
import com.aurora.phone.data.SampleData;
import com.aurora.phone.util.Actions;
import com.aurora.phone.util.NumberGeo;

/** Launcher screen: Favorites / Recents / Contacts with search and dialpad. */
public class MainActivity extends BaseActivity
        implements RecentsAdapter.Listener, ContactsAdapter.Listener,
        FavoritesAdapter.Listener {

    private static final int REQ_PERMS = 41;
    private static final int REQ_DIAL = 42;

    private static final int TAB_FAVORITES = 0;
    private static final int TAB_RECENTS = 1;
    private static final int TAB_CONTACTS = 2;

    private ListView listRecents;
    private ListView listContacts;
    private GridView gridFavorites;
    private LinearLayout emptyState;
    private TextView emptyText;
    private TextView demoChip;
    private LinearLayout searchRow;
    private EditText searchInput;
    private TextView tabFavorites;
    private TextView tabRecents;
    private TextView tabContacts;

    private RecentsAdapter recentsAdapter;
    private ContactsAdapter contactsAdapter;
    private FavoritesAdapter favoritesAdapter;

    private List<ContactEntry> contactsData = new ArrayList<ContactEntry>();
    private boolean demoMode = false;
    private int currentTab = TAB_RECENTS;
    private String query = "";
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listRecents = (ListView) findViewById(R.id.listRecents);
        listContacts = (ListView) findViewById(R.id.listContacts);
        gridFavorites = (GridView) findViewById(R.id.gridFavorites);
        emptyState = (LinearLayout) findViewById(R.id.emptyState);
        emptyText = (TextView) findViewById(R.id.emptyText);
        demoChip = (TextView) findViewById(R.id.demoChip);
        searchRow = (LinearLayout) findViewById(R.id.searchRow);
        searchInput = (EditText) findViewById(R.id.searchInput);
        tabFavorites = (TextView) findViewById(R.id.tabFavorites);
        tabRecents = (TextView) findViewById(R.id.tabRecents);
        tabContacts = (TextView) findViewById(R.id.tabContacts);

        recentsAdapter = new RecentsAdapter(this);
        recentsAdapter.setListener(this);
        contactsAdapter = new ContactsAdapter(this);
        contactsAdapter.setListener(this);
        favoritesAdapter = new FavoritesAdapter(this);
        favoritesAdapter.setListener(this);

        listRecents.setAdapter(recentsAdapter);
        listContacts.setAdapter(contactsAdapter);
        listContacts.setFastScrollEnabled(true);
        gridFavorites.setAdapter(favoritesAdapter);

        tabFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_FAVORITES);
            }
        });
        tabRecents.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_RECENTS);
            }
        });
        tabContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_CONTACTS);
            }
        });

        findViewById(R.id.btnKeypad).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, DialpadActivity.class));
            }
        });
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        findViewById(R.id.btnSearch).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchRow.setVisibility(View.VISIBLE);
                searchInput.requestFocus();
                android.view.inputmethod.InputMethodManager imm =
                        (android.view.inputmethod.InputMethodManager)
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(searchInput, 0);
                }
            }
        });
        findViewById(R.id.btnSearchClose).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchInput.setText("");
                searchRow.setVisibility(View.GONE);
            }
        });
        searchInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
                query = s.toString();
                recentsAdapter.applyFilter(query);
                contactsAdapter.applyFilter(query);
                updateEmptyState();
            }
        });
        demoChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        selectTab(TAB_RECENTS);
        ensurePermissions();
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
    }

    private void ensurePermissions() {
        List<String> missing = new ArrayList<String>();
        String[] needed = new String[] {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.CALL_PHONE
        };
        for (int i = 0; i < needed.length; i++) {
            if (!hasPerm(needed[i])) {
                missing.add(needed[i]);
            }
        }
        if (!missing.isEmpty()) {
            requestPermissions(missing.toArray(new String[missing.size()]), REQ_PERMS);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                           int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERMS) {
            reload();
        }
    }

    private void reload() {
        if (loading) {
            return;
        }
        loading = true;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                List<ContactEntry> contacts = ContactsStore.load(MainActivity.this);
                List<RecentItem> recents = CallLogStore.loadGrouped(MainActivity.this);
                final boolean usedContactsDemo = contacts == null;
                final boolean usedRecentsDemo = recents == null;
                if (contacts == null) {
                    contacts = SampleData.contacts();
                }
                if (recents == null) {
                    recents = SampleData.recents();
                }
                final List<ContactEntry> finalContacts = contacts;
                final List<RecentItem> finalRecents = recents;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        loading = false;
                        if (!isUiAlive()) {
                            return;
                        }
                        contactsData = finalContacts;
                        Map<String, ContactEntry> index = ContactsStore.index(finalContacts);
                        recentsAdapter.setContactIndex(index);
                        recentsAdapter.setHideSpam(Prefs.blockSpam(MainActivity.this));
                        recentsAdapter.setData(finalRecents);
                        contactsAdapter.setData(finalContacts);

                        List<ContactEntry> favorites = new ArrayList<ContactEntry>();
                        for (int i = 0; i < finalContacts.size(); i++) {
                            ContactEntry entry = finalContacts.get(i);
                            if (entry.starred) {
                                favorites.add(entry);
                            }
                        }
                        favoritesAdapter.setData(favorites);

                        demoMode = usedContactsDemo || usedRecentsDemo;
                        demoChip.setVisibility(demoMode ? View.VISIBLE : View.GONE);
                        updateEmptyState();
                    }
                });
                } catch (RuntimeException ex) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loading = false;
                            demoChip.setVisibility(View.GONE);
                        }
                    });
                }
            }
        }).start();
    }

    private void selectTab(int tab) {
        currentTab = tab;
        tabFavorites.setSelected(tab == TAB_FAVORITES);
        tabRecents.setSelected(tab == TAB_RECENTS);
        tabContacts.setSelected(tab == TAB_CONTACTS);
        tabFavorites.setTextColor(tab == TAB_FAVORITES
                ? 0xFFFFFFFF : colorAttr(android.R.attr.textColorSecondary));
        tabRecents.setTextColor(tab == TAB_RECENTS
                ? 0xFFFFFFFF : colorAttr(android.R.attr.textColorSecondary));
        tabContacts.setTextColor(tab == TAB_CONTACTS
                ? 0xFFFFFFFF : colorAttr(android.R.attr.textColorSecondary));

        listRecents.setVisibility(tab == TAB_RECENTS ? View.VISIBLE : View.GONE);
        listContacts.setVisibility(tab == TAB_CONTACTS ? View.VISIBLE : View.GONE);
        gridFavorites.setVisibility(tab == TAB_FAVORITES ? View.VISIBLE : View.GONE);
        updateEmptyState();
    }

    private void updateEmptyState() {
        int count;
        int message;
        if (currentTab == TAB_RECENTS) {
            count = recentsAdapter.getCount();
            message = query.length() > 0 ? R.string.no_results : R.string.empty_recents;
        } else if (currentTab == TAB_CONTACTS) {
            count = contactsAdapter.getCount();
            message = query.length() > 0 ? R.string.no_results : R.string.empty_contacts;
        } else {
            count = favoritesAdapter.getCount();
            message = query.length() > 0 ? R.string.no_results : R.string.empty_favorites;
        }
        if (count == 0) {
            emptyText.setText(message);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            emptyState.setVisibility(View.GONE);
        }
    }

    // ---------- RecentsAdapter.Listener ----------

    @Override
    public void onRecentClick(RecentItem item, View anchor) {
        openDetail(item.number, resolveRecentName(item));
    }

    @Override
    public boolean onRecentLongClick(final RecentItem item, View anchor) {
        showNumberMenu(anchor, item.number, resolveRecentName(item));
        return true;
    }

    private String resolveRecentName(RecentItem item) {
        return recentsAdapter.resolveName(item);
    }

    // ---------- ContactsAdapter.Listener ----------

    @Override
    public void onContactClick(ContactEntry entry) {
        openDetail(entry.number, entry.name);
    }

    @Override
    public void onContactCall(ContactEntry entry, View anchor) {
        placeCall(entry.number);
    }

    // ---------- FavoritesAdapter.Listener ----------

    @Override
    public void onFavoriteClick(ContactEntry entry) {
        openDetail(entry.number, entry.name);
    }

    // ---------- shared helpers ----------

    private void openDetail(String number, String name) {
        Intent intent = new Intent(this, CallerDetailActivity.class);
        intent.putExtra(CallerDetailActivity.EXTRA_NUMBER, number);
        intent.putExtra(CallerDetailActivity.EXTRA_NAME, name);
        startActivity(intent);
    }

    private void placeCall(String number) {
        if (Actions.dial(this, number)) {
            return;
        }
        // No permission (or no dialer): show the in-app call preview instead.
        Intent intent = new Intent(this, CallScreenActivity.class);
        intent.putExtra(CallScreenActivity.EXTRA_NUMBER, number);
        intent.putExtra(CallScreenActivity.EXTRA_MODE, CallScreenActivity.MODE_PREVIEW);
        startActivity(intent);
    }

    /** Shared popup for number rows: call, message, details, block, copy, share. */
    private void showNumberMenu(final View anchor, final String number, final String name) {
        final PopupMenu popup = new PopupMenu(this, anchor);
        popup.getMenuInflater().inflate(R.menu.menu_number_actions, popup.getMenu());
        boolean blocked = BlockedStore.isBlocked(this, number);
        popup.getMenu().findItem(R.id.action_block)
                .setTitle(blocked ? R.string.unblock : R.string.block);
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(android.view.MenuItem menuItem) {
                int id = menuItem.getItemId();
                if (id == R.id.action_call) {
                    placeCall(number);
                    return true;
                }
                if (id == R.id.action_message) {
                    Actions.sms(MainActivity.this, number);
                    return true;
                }
                if (id == R.id.action_details) {
                    openDetail(number, name);
                    return true;
                }
                if (id == R.id.action_block) {
                    boolean now = !BlockedStore.isBlocked(MainActivity.this, number);
                    BlockedStore.setBlocked(MainActivity.this, number, now);
                    toast(now ? getString(R.string.blocked_toast)
                            : getString(R.string.unblocked_toast));
                    return true;
                }
                if (id == R.id.action_copy) {
                    Actions.copy(MainActivity.this, number);
                    return true;
                }
                if (id == R.id.action_share) {
                    String label = name == null || name.length() == 0 ? number : name;
                    Actions.share(MainActivity.this,
                            label + " — " + number + " (Aurora Phone)");
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }
}
