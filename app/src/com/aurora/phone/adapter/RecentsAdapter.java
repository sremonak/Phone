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
    }

    private final LayoutInflater inflater;
    private final Context context;
    private List<RecentItem> all = new ArrayList<RecentItem>();
    private List<RecentItem> visible = new ArrayList<RecentItem>();
    private Map<String, ContactEntry> contactIndex;
    private String query = "";
    private boolean hideSpam = false;
    private Listener listener;

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
        if (query.length() == 0) {
            for (int i = 0; i < all.size(); i++) {
                RecentItem item = all.get(i);
                if (hideSpam && spammy(item)) {
                    continue;
                }
                visible.add(item);
            }
        } else {
            String digits = NumberGeo.digits(query);
            for (int i = 0; i < all.size(); i++) {
                RecentItem item = all.get(i);
                if (hideSpam && spammy(item)) {
                    continue;
                }
                String name = displayName(item).toLowerCase(Locale.getDefault());
                String number = item.number.toLowerCase(Locale.getDefault());
                if (name.contains(query) || number.contains(query)
                        || (digits.length() > 0 && NumberGeo.digits(item.number).contains(digits))) {
                    visible.add(item);
                }
            }
        }
        notifyDataSetChanged();
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
        return view;
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
