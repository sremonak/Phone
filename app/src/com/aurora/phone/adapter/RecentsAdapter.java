package com.aurora.phone.adapter;

import android.content.Context;
import android.provider.CallLog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.aurora.phone.CallEntry;
import com.aurora.phone.ContactEntry;
import com.aurora.phone.data.BlockedStore;
import com.aurora.phone.R;
import com.aurora.phone.RecentItem;
import com.aurora.phone.util.NumberGeo;
import com.aurora.phone.util.TimeUtils;
import com.aurora.phone.view.AvatarView;

/** Recents list: grouped per number with type, location and miss badges. */
public class RecentsAdapter extends BaseAdapter {

    public interface Listener {
        void onRecentClick(RecentItem item, View anchor);
        boolean onRecentLongClick(RecentItem item, View anchor);
        void onRecentDelete(RecentItem item);
    }

    private final LayoutInflater inflater;
    private final Context context;
    private List<RecentItem> all = new ArrayList<RecentItem>();
    private List<RecentItem> visible = new ArrayList<RecentItem>();
    private Map<String, ContactEntry> contactIndex;
    private String query = "";
    private boolean hideSpam = false;
    private int chipFilter = 0;
    private Listener listener;

    public static final int CHIP_ALL = 0;
    public static final int CHIP_MISSED = 1;
    public static final int CHIP_CONTACTS = 2;
    public static final int CHIP_SPAM = 3;

    public void setChipFilter(int chip) {
        this.chipFilter = chip;
        applyFilter(query);
    }

    public List<RecentItem> allItems() {
        return all;
    }

    public RecentsAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setHideSpam(boolean hideSpam) {
        this.hideSpam = hideSpam;
    }

    public void setContactIndex(Map<String, ContactEntry> index) {
        this.contactIndex = index;
    }

    public void setData(List<RecentItem> items) {
        this.all = items == null ? new ArrayList<RecentItem>() : items;
        applyFilter(query);
    }

    public void applyFilter(String q) {
        this.query = q == null ? "" : q.trim().toLowerCase(Locale.getDefault());
        visible.clear();
        String digits = query.length() > 0 ? NumberGeo.digits(query) : "";
        for (int i = 0; i < all.size(); i++) {
            RecentItem item = all.get(i);
            if (hideSpam && spammy(item)) {
                continue;
            }
            if (!passesChip(item)) {
                continue;
            }
            if (query.length() > 0) {
                String name = displayName(item).toLowerCase(Locale.getDefault());
                String number = item.number.toLowerCase(Locale.getDefault());
                boolean match = name.contains(query) || number.contains(query)
                        || (digits.length() > 0 && NumberGeo.digits(item.number).contains(digits));
                if (!match) {
                    continue;
                }
            }
            visible.add(item);
        }
        notifyDataSetChanged();
    }

    /** Home-tab filter chips (All / Missed / Contacts / Spam). */
    private boolean passesChip(RecentItem item) {
        if (chipFilter == CHIP_MISSED) {
            return item.missed > 0;
        }
        if (chipFilter == CHIP_CONTACTS) {
            if (item.name != null && item.name.length() > 0) {
                return true;
            }
            if (contactIndex != null) {
                return contactIndex.get(NumberGeo.groupKey(item.number)) != null;
            }
            return false;
        }
        if (chipFilter == CHIP_SPAM) {
            return spammy(item);
        }
        return true;
    }

    /** Rows hidden while "Block suspected spam" is enabled. */
    private boolean spammy(RecentItem item) {
        NumberGeo.Info geo = NumberGeo.analyze(item.number);
        if (geo.isPremium()) {
            return true;
        }
        return item.missed >= 3 && item.count >= 3;
    }

    public String resolveName(RecentItem item) {
        return displayName(item);
    }

    private String displayName(RecentItem item) {
        if (item.name != null && item.name.length() > 0) {
            return item.name;
        }
        if (contactIndex != null) {
            ContactEntry entry = contactIndex.get(NumberGeo.groupKey(item.number));
            if (entry != null) {
                return entry.name;
            }
        }
        return NumberGeo.pretty(item.number);
    }

    @Override
    public int getCount() {
        return visible.size();
    }

    @Override
    public Object getItem(int position) {
        return visible.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View view = convertView;
        Holder holder;
        if (view == null) {
            view = inflater.inflate(R.layout.item_recents, parent, false);
            holder = new Holder();
            holder.avatar = (AvatarView) view.findViewById(R.id.avatar);
            holder.name = (TextView) view.findViewById(R.id.itemName);
            holder.meta = (TextView) view.findViewById(R.id.itemMeta);
            holder.typeIcon = (ImageView) view.findViewById(R.id.itemTypeIcon);
            holder.time = (TextView) view.findViewById(R.id.itemTime);
            holder.badge = (TextView) view.findViewById(R.id.itemBadge);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        final RecentItem item = visible.get(position);
        String name = displayName(item);
        holder.avatar.setPerson(name);
        holder.name.setText(name);
        holder.name.setTextColor(item.type == 3 && item.missed > 0
                ? color(R.attr.av_colorDanger) : color(R.attr.av_colorText));

        NumberGeo.Info geo = NumberGeo.analyze(item.number);
        int icon;
        if (item.type == CallEntry.TYPE_OUTGOING) {
            icon = R.drawable.ic_call_made;
            holder.typeIcon.setColorFilter(color(R.attr.av_colorTextSecondary));
        } else if (item.type == CallEntry.TYPE_MISSED) {
            icon = R.drawable.ic_call_missed;
            holder.typeIcon.setColorFilter(color(R.attr.av_colorDanger));
        } else if (item.type == CallEntry.TYPE_VOICEMAIL) {
            icon = R.drawable.ic_message;
            holder.typeIcon.setColorFilter(color(R.attr.av_colorTextSecondary));
        } else {
            icon = R.drawable.ic_call_received;
            holder.typeIcon.setColorFilter(color(R.attr.av_colorTextSecondary));
        }
        holder.typeIcon.setImageResource(icon);

        String where = NumberGeo.shortLabel(geo);
        StringBuilder meta = new StringBuilder();
        if (item.type == CallEntry.TYPE_OUTGOING) {
            meta.append("Outgoing");
        } else if (item.type == CallEntry.TYPE_MISSED) {
            meta.append("Missed");
        } else if (item.type == CallEntry.TYPE_VOICEMAIL) {
            meta.append("Voicemail");
        } else {
            meta.append("Incoming");
        }
        if (item.count > 1) {
            meta.append(" · ").append(item.count).append(" calls");
        } else if (item.duration > 0) {
            meta.append(" · ").append(TimeUtils.duration(item.duration));
        }
        if (where.length() > 0) {
            meta.append(" · ").append(where);
        }
        String note = com.aurora.phone.Prefs.note(context, item.number);
        if (note.length() > 0) {
            meta.append(" · ").append(context.getString(com.aurora.phone.R.string.note_title));
        }
        holder.meta.setText(meta.toString());

        holder.time.setText(TimeUtils.relative(item.date));

        if (item.missed > 0) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(item.missed + " missed");
            holder.badge.setTextColor(color(R.attr.av_colorDanger));
        } else if (item.count > 1) {
            holder.badge.setVisibility(View.VISIBLE);
            holder.badge.setText(item.count + "×");
            holder.badge.setTextColor(color(R.attr.av_colorTextSecondary));
        } else {
            holder.badge.setVisibility(View.GONE);
        }

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onRecentClick(item, v);
                }
            }
        });
        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                return listener != null && listener.onRecentLongClick(item, v);
            }
        });
        attachSwipe(view, item);
        return view;
    }

    private float downX;
    private boolean swiping;
    private boolean swipeConsumed;
    private android.graphics.drawable.Drawable rowBackground;

    /** Swipe a row to the left past ~90px to delete it. */
    private void attachSwipe(final View row, final RecentItem item) {
        row.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(final View v, android.view.MotionEvent event) {
                float density = context.getResources().getDisplayMetrics().density;
                float threshold = 96f * density;
                switch (event.getActionMasked()) {
                    case android.view.MotionEvent.ACTION_DOWN:
                        downX = event.getX();
                        swiping = false;
                        swipeConsumed = false;
                        rowBackground = v.getBackground();
                        v.setTranslationX(0f);
                        return false;
                    case android.view.MotionEvent.ACTION_MOVE: {
                        float dx = event.getX() - downX;
                        if (dx < -8f * density) {
                            swiping = true;
                            v.getParent().requestDisallowInterceptTouchEvent(true);
                            v.setTranslationX(Math.max(dx, -threshold - 40f * density));
                            float progress = Math.min(1f, -dx / threshold);
                            v.setBackgroundColor(android.graphics.Color.argb(
                                    (int) (progress * 160), 255, 60, 70));
                            return true;
                        }
                        if (swiping) {
                            return true;
                        }
                        return false;
                    }
                    case android.view.MotionEvent.ACTION_UP: {
                        float dx = event.getX() - downX;
                        if (swiping && dx < -threshold && !swipeConsumed) {
                            swipeConsumed = true;
                            v.animate().translationX(-v.getWidth())
                                    .alpha(0f).setDuration(180)
                                    .withEndAction(new Runnable() {
                                        @Override
                                        public void run() {
                                            v.setTranslationX(0f);
                                            v.setAlpha(1f);
                                            v.setBackground(rowBackground);
                                            if (listener != null) {
                                                listener.onRecentDelete(item);
                                            }
                                        }
                                    }).start();
                            return true;
                        }
                        if (rowBackground != null) {
                            v.setBackground(rowBackground);
                        }
                        v.animate().translationX(0f).alpha(1f).setDuration(140).start();
                        if (swiping) {
                            // consumed a swipe gesture — swallow the click
                            return true;
                        }
                        return false;
                    }
                    case android.view.MotionEvent.ACTION_CANCEL:
                        if (rowBackground != null) {
                            v.setBackground(rowBackground);
                        }
                        v.animate().translationX(0f).alpha(1f).setDuration(140).start();
                        return swiping;
                    default:
                        return false;
                }
            }
        });
    }

    private int color(int attr) {
        android.util.TypedValue tv = new android.util.TypedValue();
        if (context.getTheme().resolveAttribute(attr, tv, true)) {
            return tv.data;
        }
        return 0xFF000000;
    }

    private static class Holder {
        AvatarView avatar;
        TextView name;
        TextView meta;
        ImageView typeIcon;
        TextView time;
        TextView badge;
    }
}
