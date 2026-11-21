package com.aurora.phone.adapter;

import android.content.Context;
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
import com.aurora.phone.R;
import com.aurora.phone.RecentItem;
import com.aurora.phone.util.NumberGeo;
import com.aurora.phone.util.TimeUtils;
import com.aurora.phone.view.AvatarView;

/** Global search results: section headers, contacts and recents in one list. */
public class SearchAdapter extends BaseAdapter {

    private static final int TYPE_CONTACT_HEADER = 0;
    private static final int TYPE_RECENT_HEADER = 1;
    private static final int TYPE_CONTACT = 2;
    private static final int TYPE_RECENT = 3;

    public interface Listener {
        void onSearchContact(ContactEntry entry);
        void onSearchRecent(RecentItem item);
    }

    private final LayoutInflater inflater;
    private final Context context;
    private Listener listener;

    private final List<Integer> types = new ArrayList<Integer>();
    private final List<Object> rows = new ArrayList<Object>();
    private Map<String, ContactEntry> contactIndex;

    public SearchAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setContactIndex(Map<String, ContactEntry> index) {
        this.contactIndex = index;
    }

    public void submit(String query, List<ContactEntry> contacts, List<RecentItem> recents) {
        types.clear();
        rows.clear();
        String q = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        String digits = NumberGeo.digits(q);

        List<ContactEntry> matchedContacts = new ArrayList<ContactEntry>();
        if (contacts != null) {
            for (int i = 0; i < contacts.size(); i++) {
                ContactEntry entry = contacts.get(i);
                if (q.length() == 0) {
                    continue;
                }
                String name = entry.name == null ? "" : entry.name.toLowerCase(Locale.getDefault());
                String number = entry.number == null ? "" : entry.number.toLowerCase(Locale.getDefault());
                if (name.contains(q) || number.contains(q)
                        || (digits.length() > 0 && NumberGeo.digits(entry.number).contains(digits))) {
                    matchedContacts.add(entry);
                }
            }
        }

        List<RecentItem> matchedRecents = new ArrayList<RecentItem>();
        if (recents != null && q.length() > 0) {
            for (int i = 0; i < recents.size(); i++) {
                RecentItem item = recents.get(i);
                String name = resolveName(item).toLowerCase(Locale.getDefault());
                String number = item.number.toLowerCase(Locale.getDefault());
                if (name.contains(q) || number.contains(q)
                        || (digits.length() > 0 && NumberGeo.digits(item.number).contains(digits))) {
                    matchedRecents.add(item);
                }
            }
        }

        if (!matchedContacts.isEmpty()) {
            types.add(Integer.valueOf(TYPE_CONTACT_HEADER));
            rows.add(context.getString(R.string.search_contacts_header));
            for (int i = 0; i < matchedContacts.size(); i++) {
                types.add(Integer.valueOf(TYPE_CONTACT));
                rows.add(matchedContacts.get(i));
            }
        }
        if (!matchedRecents.isEmpty()) {
            types.add(Integer.valueOf(TYPE_RECENT_HEADER));
            rows.add(context.getString(R.string.search_recents_header));
            for (int i = 0; i < matchedRecents.size(); i++) {
                types.add(Integer.valueOf(TYPE_RECENT));
                rows.add(matchedRecents.get(i));
            }
        }
        notifyDataSetChanged();
    }

    private String resolveName(RecentItem item) {
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

    public boolean isEmptyResult() {
        return rows.isEmpty();
    }

    @Override
    public int getCount() {
        return rows.size();
    }

    @Override
    public int getItemViewType(int position) {
        return types.get(position).intValue();
    }

    @Override
    public int getViewTypeCount() {
        return 4;
    }

    @Override
    public Object getItem(int position) {
        return rows.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean isEnabled(int position) {
        int type = getItemViewType(position);
        return type == TYPE_CONTACT || type == TYPE_RECENT;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        int type = getItemViewType(position);
        View view = convertView;
        if (type == TYPE_CONTACT_HEADER || type == TYPE_RECENT_HEADER) {
            if (view == null) {
                view = inflater.inflate(R.layout.item_section_header, parent, false);
            }
            TextView title = (TextView) view.findViewById(R.id.sectionTitle);
            title.setText((String) rows.get(position));
            return view;
        }
        if (type == TYPE_CONTACT) {
            if (view == null) {
                view = inflater.inflate(R.layout.item_contact, parent, false);
            }
            final ContactEntry entry = (ContactEntry) rows.get(position);
            AvatarView avatar = (AvatarView) view.findViewById(R.id.avatar);
            TextView name = (TextView) view.findViewById(R.id.itemName);
            avatar.setPerson(entry.name);
            name.setText(entry.name);
            // hide secondary line if present
            View subtitle = view.findViewById(R.id.itemMeta);
            if (subtitle != null) {
                subtitle.setVisibility(View.GONE);
            }
            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (listener != null) {
                        listener.onSearchContact(entry);
                    }
                }
            });
            return view;
        }

        final RecentItem item = (RecentItem) rows.get(position);
        if (view == null) {
            view = inflater.inflate(R.layout.item_recents, parent, false);
        }
        String display = resolveName(item);
        AvatarView avatar = (AvatarView) view.findViewById(R.id.avatar);
        TextView name = (TextView) view.findViewById(R.id.itemName);
        TextView meta = (TextView) view.findViewById(R.id.itemMeta);
        ImageView typeIcon = (ImageView) view.findViewById(R.id.itemTypeIcon);
        TextView time = (TextView) view.findViewById(R.id.itemTime);
        TextView badge = (TextView) view.findViewById(R.id.itemBadge);

        avatar.setPerson(display);
        name.setText(display);
        int icon;
        int tint;
        if (item.type == CallEntry.TYPE_OUTGOING) {
            icon = R.drawable.ic_call_made;
            tint = color(R.attr.av_colorTextSecondary);
        } else if (item.type == CallEntry.TYPE_MISSED) {
            icon = R.drawable.ic_call_missed;
            tint = color(R.attr.av_colorDanger);
        } else if (item.type == CallEntry.TYPE_VOICEMAIL) {
            icon = R.drawable.ic_message;
            tint = color(R.attr.av_colorTextSecondary);
        } else {
            icon = R.drawable.ic_call_received;
            tint = color(R.attr.av_colorTextSecondary);
        }
        typeIcon.setImageResource(icon);
        typeIcon.setColorFilter(tint);
        NumberGeo.Info geo = NumberGeo.analyze(item.number);
        String where = NumberGeo.shortLabel(geo);
        StringBuilder sb = new StringBuilder();
        if (item.count > 1) {
            sb.append(item.count).append(" calls");
        } else if (item.duration > 0) {
            sb.append(TimeUtils.duration(item.duration));
        }
        if (where.length() > 0) {
            if (sb.length() > 0) {
                sb.append(" · ");
            }
            sb.append(where);
        }
        meta.setText(sb.toString());
        time.setText(TimeUtils.relative(item.date));
        badge.setVisibility(View.GONE);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onSearchRecent(item);
                }
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
}
