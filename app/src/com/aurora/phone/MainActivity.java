package com.aurora.phone;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.EditText;
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
import com.aurora.phone.util.DialpadController;
import com.aurora.phone.util.Notifier;
import com.aurora.phone.util.NumberGeo;
import com.aurora.phone.view.AvatarView;

/**
 * Home screen in the style of modern dialers: a search pill on top,
 * Home (favorites strip + filter chips + recents), an embedded Keypad tab
 * and a Contacts tab, with a bottom navigation bar.
 */
public class MainActivity extends BaseActivity
        implements RecentsAdapter.Listener, ContactsAdapter.Listener,
        FavoritesAdapter.Listener {

    private static final int REQ_PERMS = 41;
    private static final int TAB_HOME = 0;
    private static final int TAB_KEYPAD = 1;
    private static final int TAB_CONTACTS = 2;

    private static final int CHIP_ALL = 0;
    private static final int CHIP_MISSED = 1;
    private static final int CHIP_CONTACTS = 2;
    private static final int CHIP_SPAM = 3;

    private View viewHome;
    private View viewKeypad;
    private View viewContacts;
    private LinearLayout navHome;
    private LinearLayout navKeypad;
    private LinearLayout navContacts;
    private LinearLayout favStrip;
    private View favScroll;
    private LinearLayout chipRow;
    private ListView listRecents;
    private ListView listContacts;
    private View skeleton;
    private LinearLayout emptyState;
    private TextView emptyText;
    private TextView demoChip;
    private TextView contactsEmpty;

    private RecentsAdapter recentsAdapter;
    private ContactsAdapter contactsAdapter;
    private DialpadController dialpad;

    private List<ContactEntry> contactsData = new ArrayList<ContactEntry>();
    private boolean demoMode = false;
    private int currentTab = TAB_HOME;
    private int currentChip = CHIP_ALL;
    private boolean loading = false;
    private boolean firstRender = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewHome = findViewById(R.id.viewHome);
        viewKeypad = findViewById(R.id.viewKeypad);
        viewContacts = findViewById(R.id.viewContacts);
        navHome = (LinearLayout) findViewById(R.id.navHome);
        navKeypad = (LinearLayout) findViewById(R.id.navKeypad);
        navContacts = (LinearLayout) findViewById(R.id.navContacts);
        favStrip = (LinearLayout) findViewById(R.id.favStrip);
        favScroll = findViewById(R.id.favScroll);
        chipRow = (LinearLayout) findViewById(R.id.chipRow);
        listRecents = (ListView) findViewById(R.id.listRecents);
        listContacts = (ListView) findViewById(R.id.listContacts);
        skeleton = findViewById(R.id.skeleton);
        emptyState = (LinearLayout) findViewById(R.id.emptyState);
        emptyText = (TextView) findViewById(R.id.emptyText);
        demoChip = (TextView) findViewById(R.id.demoChip);
        contactsEmpty = (TextView) findViewById(R.id.contactsEmpty);

        recentsAdapter = new RecentsAdapter(this);
        recentsAdapter.setListener(this);
        contactsAdapter = new ContactsAdapter(this);
        contactsAdapter.setListener(this);
        listRecents.setAdapter(recentsAdapter);
        listContacts.setAdapter(contactsAdapter);
        listContacts.setFastScrollEnabled(true);

        dialpad = new DialpadController(this, findViewById(R.id.viewKeypad));

        buildChips();

        navHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_HOME);
            }
        });
        navKeypad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_KEYPAD);
            }
        });
        navContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                selectTab(TAB_CONTACTS);
            }
        });

        findViewById(R.id.searchPill).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SearchActivity.class));
            }
        });
        findViewById(R.id.btnStats).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, InsightsActivity.class));
            }
        });
        findViewById(R.id.btnSettings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });
        demoChip.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
            }
        });

        skeleton.setVisibility(View.VISIBLE);
        selectTab(TAB_HOME);
        ensurePermissions();
        reload();
    }

    @Override
    protected void onResume() {
        super.onResume();
        reload();
        dialpad.refresh();
    }

    /** XML onClick handler for keypad keys inside the embedded dialpad. */
    public void onDialKey(View v) {
        dialpad.onDialKey(v);
    }

    // ---------- bottom nav ----------

    private void selectTab(int tab) {
        currentTab = tab;
        viewHome.setVisibility(tab == TAB_HOME ? View.VISIBLE : View.GONE);
        viewKeypad.setVisibility(tab == TAB_KEYPAD ? View.VISIBLE : View.GONE);
        viewContacts.setVisibility(tab == TAB_CONTACTS ? View.VISIBLE : View.GONE);
        navHome.setActivated(tab == TAB_HOME);
        navKeypad.setActivated(tab == TAB_KEYPAD);
        navContacts.setActivated(tab == TAB_CONTACTS);
        updateEmptyState();
    }

    // ---------- filter chips ----------

    private void buildChips() {
        int[] labels = {R.string.filter_all, R.string.filter_missed,
                R.string.filter_contacts, R.string.filter_spam};
        chipRow.removeAllViews();
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            TextView chip = new TextView(this);
            chip.setText(labels[i]);
            chip.setTextSize(14f);
            chip.setPadding(dp(18), dp(9), dp(18), dp(9));
            chip.setBackgroundResource(R.drawable.chip_filter);
            chip.setActivated(i == currentChip);
            chip.setTextColor(chipColor(i == currentChip));
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            params.setMarginEnd(dp(8));
            chip.setLayoutParams(params);
            chip.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setChip(index);
                }
            });
            chipRow.addView(chip);
        }
    }

    private void setChip(int index) {
        currentChip = index;
        for (int i = 0; i < chipRow.getChildCount(); i++) {
            View child = chipRow.getChildAt(i);
            boolean selected = i == index;
            child.setActivated(selected);
            if (child instanceof TextView) {
                ((TextView) child).setTextColor(chipColor(selected));
            }
        }
        recentsAdapter.setChipFilter(index);
        updateEmptyState();
    }

    private int chipColor(boolean selected) {
        if (selected) {
            return 0xFFFFFFFF;
        }
        return colorAttr(android.R.attr.textColorSecondary);
    }

    // ---------- permissions ----------

    private void ensurePermissions() {
        List<String> missing = new ArrayList<String>();
        String[] needed = new String[] {
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.READ_CALL_LOG,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.WRITE_CALL_LOG
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

    // ---------- data ----------

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

                    // missed-call notification (real call log only)
                    if (!usedRecentsDemo && CallLogStore.canRead(MainActivity.this)) {
                        long since = Prefs.lastMissedSeen(MainActivity.this);
                        final int missedCount = CallLogStore.countMissedSince(
                                MainActivity.this, since);
                        Prefs.setLastMissedSeen(MainActivity.this,
                                System.currentTimeMillis());
                        if (missedCount > 0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (isUiAlive()) {
                                        Notifier.showMissed(MainActivity.this, missedCount);
                                    }
                                }
                            });
                        }
                    }

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loading = false;
                            if (!isUiAlive()) {
                                return;
                            }
                            contactsData = finalContacts;
                            Map<String, ContactEntry> index =
                                    ContactsStore.index(finalContacts);
                            recentsAdapter.setContactIndex(index);
                            recentsAdapter.setHideSpam(Prefs.blockSpam(MainActivity.this));
                            recentsAdapter.setData(finalRecents);
                            contactsAdapter.setData(finalContacts);

                            buildFavorites(finalContacts);

                            demoMode = usedContactsDemo || usedRecentsDemo;
                            demoChip.setVisibility(demoMode ? View.VISIBLE : View.GONE);
                            skeleton.setVisibility(View.GONE);
                            if (firstRender) {
                                firstRender = false;
                                Animation slide = AnimationUtils.loadAnimation(
                                        MainActivity.this, android.R.anim.fade_in);
                                slide.setDuration(280);
                                listRecents.startAnimation(slide);
                            }
                            updateEmptyState();
                        }
                    });
                } catch (RuntimeException ex) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            loading = false;
                            skeleton.setVisibility(View.GONE);
                            demoChip.setVisibility(View.GONE);
                            updateEmptyState();
                        }
                    });
                }
            }
        }).start();
    }

    private void buildFavorites(List<ContactEntry> contacts) {
        favStrip.removeAllViews();
        List<ContactEntry> favorites = new ArrayList<ContactEntry>();
        for (int i = 0; i < contacts.size(); i++) {
            if (contacts.get(i).starred) {
                favorites.add(contacts.get(i));
            }
        }
        if (favorites.isEmpty()) {
            favScroll.setVisibility(View.GONE);
            return;
        }
        favScroll.setVisibility(View.VISIBLE);
        LayoutInflater inflater = LayoutInflater.from(this);
        for (int i = 0; i < Math.min(favorites.size(), 12); i++) {
            final ContactEntry entry = favorites.get(i);
            View item = inflater.inflate(R.layout.item_favorite, favStrip, false);
            item.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));
            AvatarView avatar = (AvatarView) item.findViewById(R.id.avatar);
            TextView name = (TextView) item.findViewById(R.id.itemName);
            avatar.setPerson(entry.name);
            avatar.setLayoutParams(new android.view.ViewGroup.LayoutParams(dp(56), dp(56)));
            name.setText(entry.name);
            item.setPadding(dp(10), dp(6), dp(10), dp(10));
            item.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    openDetail(entry.number, entry.name);
                }
            });
            favStrip.addView(item);
        }
    }

    private void updateEmptyState() {
        if (currentTab == TAB_HOME) {
            int count = recentsAdapter.getCount();
            if (count == 0) {
                emptyText.setText(currentChip == CHIP_ALL
                        ? R.string.empty_recents : R.string.no_results);
                emptyState.setVisibility(View.VISIBLE);
            } else {
                emptyState.setVisibility(View.GONE);
            }
        } else if (currentTab == TAB_CONTACTS) {
            boolean empty = contactsAdapter.getCount() == 0;
            contactsEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            listContacts.setVisibility(empty ? View.GONE : View.VISIBLE);
        }
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
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

    @Override
    public void onRecentDelete(final RecentItem item) {
        final String number = item.number;
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean ok = !demoMode
                        && CallLogStore.deleteNumber(MainActivity.this, number);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!isUiAlive()) {
                            return;
                        }
                        if (ok) {
                            toast(getString(R.string.deleted_toast));
                            reload();
                        } else if (demoMode) {
                            // demo list: drop the row in memory only
                            List<RecentItem> updated = new ArrayList<RecentItem>();
                            List<RecentItem> current = recentsAdapter.allItems();
                            for (int i = 0; i < current.size(); i++) {
                                if (current.get(i) != item) {
                                    updated.add(current.get(i));
                                }
                            }
                            recentsAdapter.setData(updated);
                            updateEmptyState();
                            toast(getString(R.string.deleted_toast));
                        } else {
                            toast(getString(R.string.delete_denied));
                        }
                    }
                });
            }
        }).start();
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

    // ---------- FavoritesAdapter.Listener (unused; favorites strip is inline) ----------

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
                            label + " \u2014 " + number + " (Aurora Phone)");
                    return true;
                }
                return false;
            }
        });
        popup.show();
    }
}
