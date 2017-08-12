package com.proto4.protopaja.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.proto4.protopaja.R;

import java.util.ArrayList;

/**
 * Created by user on 12.08.17.
 */

public class GearListAdapter extends RecyclerView.Adapter<GearListAdapter.ViewHolder> {

    private static final String TAG = GearListAdapter.class.getSimpleName();

    private Context context;
    private ArrayList<GearListItem> listItems;

    private final RelativeLayout.LayoutParams normalLayoutParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.WRAP_CONTENT
    );
    private final RelativeLayout.LayoutParams lastLayoutParams = new RelativeLayout.LayoutParams(
            RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT
    );

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView title;
        public CheckBox checkBox;
        public ImageView brIcon;
        public RelativeLayout boundaries;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.list_row_title);
            checkBox = view.findViewById(R.id.list_item_checkbox);
            checkBox.setChecked(false);
            checkBox.setVisibility(View.GONE);
            checkBox.setClickable(false);
            brIcon = view.findViewById(R.id.list_item_brightness_icon);
            brIcon.setVisibility(View.GONE);
            brIcon.setColorFilter(0xffa0a0f0);
            boundaries = view.findViewById(R.id.list_row_boundaries);
        }
    }

    public GearListAdapter(Context _context, ArrayList<GearListItem> _listItems) {
        listItems = _listItems;
        context = _context;
    }

    @Override
    public GearListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_list_row, parent, false);
        return new GearListAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(GearListAdapter.ViewHolder holder, int position) {
        GearListItem item  = listItems.get(position);
        holder.title.setText(item.getGear().getName());

        int colorId = item.isGroup() ? R.color.title_group : R.color.title_gear;
        int boundaryColor = context.getResources().getColor(colorId, null);

        holder.boundaries.setBackgroundColor(boundaryColor);
        if (item.isGroup()) {
            holder.checkBox.setVisibility(View.GONE);
            holder.brIcon.setVisibility(View.GONE);
        } else {
            holder.checkBox.setVisibility(item.isCheckBoxVisible() ? View.VISIBLE : View.GONE);
            holder.checkBox.setChecked(item.isChecked());
            holder.brIcon.setVisibility(item.isCheckBoxVisible() ? View.GONE : View.VISIBLE);
            if (holder.brIcon.getVisibility() == View.VISIBLE) {
                int br = (int)(item.getGear().getPowerRatio()*255);
                int brColor = 0xff000000 + (br << 16) + (br << 8) + br;
                holder.brIcon.setColorFilter(brColor);
                Log.d(TAG, "brightness=" + br);
            }
        }

        if (position == listItems.size() - 1) {
            Log.d(TAG, "set last params " + item.getGear().getName());
            holder.itemView.setLayoutParams(lastLayoutParams);
        } else {
            Log.d(TAG, "set normal params " + item.getGear().getName());
            holder.itemView.setLayoutParams(normalLayoutParams);
        }
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }
}