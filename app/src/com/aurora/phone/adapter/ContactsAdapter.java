package com.aurora.phone.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SectionIndexer;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.aurora.phone.ContactEntry;
import com.aurora.phone.R;
import com.aurora.phone.util.NumberGeo;
import com.aurora.phone.view.AvatarView;

/** Alphabetical contacts list with fast-scroll sections. */
public class ContactsAdapter extends BaseAdapter implements SectionIndexer {

    public interface Listener {
        void onContactClick(ContactEntry entry);
        void onContactCall(ContactEntry entry, View anchor);
    }

    private final LayoutInflater inflater;
    private final Context context;
    private List<ContactEntry> all = new ArrayList<ContactEntry>();
    private List<ContactEntry> visible = new ArrayList<ContactEntry>();
    private String[] sections = new String[0];
    private int[] positions = new int[0];
    private String query = "";
    private Listener listener;

    public ContactsAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<ContactEntry> items) {
        this.all = items == null ? new ArrayList<ContactEntry>() : items;
        applyFilter(query);
    }

    public void applyFilter(String q) {
        this.query = q == null ? "" : q.trim().toLowerCase(Locale.getDefault());
        visible.clear();
        if (query.length() == 0) {
            visible.addAll(all);
        } else {
            String digits = NumberGeo.digits(query);
            String t9 = NumberGeo.t9(query);
            for (int i = 0; i < all.size(); i++) {
                ContactEntry entry = all.get(i);
                String name = entry.name.toLowerCase(Locale.getDefault());
                boolean match = name.contains(query)
                        || NumberGeo.digits(entry.number).contains(digits)
                        || (t9.length() > 0 && NumberGeo.t9(entry.name).startsWith(t9));
                if (match) {
                    visible.add(entry);
                }
            }
        }
        rebuildSections();
        notifyDataSetChanged();
    }

    private void rebuildSections() {
        List<String> sectionList = new ArrayList<String>();
        List<Integer> positionList = new ArrayList<Integer>();
        String current = null;
        for (int i = 0; i < visible.size(); i++) {
            String letter = sectionFor(visible.get(i).name);
            if (!letter.equals(current)) {
                current = letter;
                sectionList.add(letter);
                positionList.add(Integer.valueOf(i));
            }
        }
        sections = sectionList.toArray(new String[sectionList.size()]);
        positions = new int[positionList.size()];
        for (int i = 0; i < positionList.size(); i++) {
            positions[i] = positionList.get(i).intValue();
        }
    }

    private static String sectionFor(String name) {
        if (name == null || name.trim().length() == 0) {
            return "#";
        }
        char c = Character.toUpperCase(name.trim().charAt(0));
        if (c < 'A' || c > 'Z') {
            return "#";
        }
        return String.valueOf(c);
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
            view = inflater.inflate(R.layout.item_contact, parent, false);
            holder = new Holder();
            holder.avatar = (AvatarView) view.findViewById(R.id.avatar);
            holder.name = (TextView) view.findViewById(R.id.itemName);
            holder.number = (TextView) view.findViewById(R.id.itemNumber);
            holder.star = (ImageView) view.findViewById(R.id.itemStar);
            holder.call = (ImageView) view.findViewById(R.id.itemCall);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }

        final ContactEntry entry = visible.get(position);
        holder.avatar.setPerson(entry.name);
        holder.name.setText(entry.name);
        holder.number.setText(NumberGeo.pretty(NumberGeo.normalize(entry.number)));
        holder.star.setVisibility(entry.starred ? View.VISIBLE : View.GONE);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onContactClick(entry);
                }
            }
        });
        holder.call.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onContactCall(entry, v);
                }
            }
        });
        return view;
    }

    @Override
    public Object[] getSections() {
        return sections;
    }

    @Override
    public int getPositionForSection(int sectionIndex) {
        if (sectionIndex >= 0 && sectionIndex < positions.length) {
            return positions[sectionIndex];
        }
        return 0;
    }

    @Override
    public int getSectionForPosition(int position) {
        for (int i = 0; i < positions.length; i++) {
            if (positions[i] <= position) {
                if (i + 1 >= positions.length || positions[i + 1] > position) {
                    return i;
                }
            } else {
                break;
            }
        }
        return 0;
    }

    private static class Holder {
        AvatarView avatar;
        TextView name;
        TextView number;
        ImageView star;
        ImageView call;
    }
}
