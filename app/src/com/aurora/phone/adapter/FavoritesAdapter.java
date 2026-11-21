package com.aurora.phone.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import com.aurora.phone.ContactEntry;
import com.aurora.phone.R;
import com.aurora.phone.view.AvatarView;

/** Grid adapter for starred contacts. */
public class FavoritesAdapter extends BaseAdapter {

    public interface Listener {
        void onFavoriteClick(ContactEntry entry);
    }

    private final LayoutInflater inflater;
    private List<ContactEntry> items = new ArrayList<ContactEntry>();
    private Listener listener;

    public FavoritesAdapter(Context context) {
        this.inflater = LayoutInflater.from(context);
    }

    public void setListener(Listener listener) {
        this.listener = listener;
    }

    public void setData(List<ContactEntry> data) {
        this.items = data == null ? new ArrayList<ContactEntry>() : data;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public Object getItem(int position) {
        return items.get(position);
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
            view = inflater.inflate(R.layout.item_favorite, parent, false);
            holder = new Holder();
            holder.avatar = (AvatarView) view.findViewById(R.id.avatar);
            holder.name = (TextView) view.findViewById(R.id.itemName);
            view.setTag(holder);
        } else {
            holder = (Holder) view.getTag();
        }
        final ContactEntry entry = items.get(position);
        holder.avatar.setPerson(entry.name);
        holder.name.setText(entry.name);
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onFavoriteClick(entry);
                }
            }
        });
        return view;
    }

    private static class Holder {
        AvatarView avatar;
        TextView name;
    }
}
