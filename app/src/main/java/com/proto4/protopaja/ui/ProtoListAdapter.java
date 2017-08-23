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

public class ProtoListAdapter extends RecyclerView.Adapter<ProtoListAdapter.ViewHolder> {

    private static final String TAG = ProtoListAdapter.class.getSimpleName();

    private Context context;
    private ArrayList<ProtoListItem> listItems;

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

    public ProtoListAdapter(Context _context, ArrayList<ProtoListItem> _listItems) {
        listItems = _listItems;
        context = _context;
    }

    @Override
    public ProtoListAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_list_row, parent, false);
        return new ProtoListAdapter.ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ProtoListAdapter.ViewHolder holder, int position) {

        Log.d(TAG, "onBindViewHolder()");

        ProtoListItem item  = listItems.get(position);

        int titleColorId = R.color.title_default;

        holder.title.setText(item.getName());

        switch (item.getType()) {
            case ProtoListItem.TYPE_GEAR:
                titleColorId = R.color.title_gear;
                holder.checkBox.setVisibility(item.isCheckBoxVisible() ? View.VISIBLE : View.GONE);
                holder.checkBox.setChecked(item.isChecked());
                holder.brIcon.setVisibility(item.isCheckBoxVisible() ? View.GONE : View.VISIBLE);
                if (holder.brIcon.getVisibility() == View.VISIBLE) {
                    int br = 255 - (int)(item.getValue()*255);
                    int filter = br << 24;
                    holder.brIcon.setColorFilter(filter);
                    Log.d(TAG, "brightness=" + br);
                }
                break;
            case ProtoListItem.TYPE_GROUP:
                holder.title.setText(item.getName());
                titleColorId = R.color.title_group;
                holder.checkBox.setVisibility(View.GONE);
                holder.brIcon.setVisibility(View.GONE);
                break;
            case ProtoListItem.TYPE_DEVICE:
                titleColorId = R.color.title_bt_device;
                holder.checkBox.setVisibility(View.GONE);
                holder.brIcon.setVisibility(View.GONE);
                break;
        }

        holder.boundaries.setBackgroundColor(context.getResources().getColor(titleColorId, null));

        if (position == listItems.size() - 1) {
            Log.d(TAG, "set last params " + item.getName());
            holder.itemView.setLayoutParams(lastLayoutParams);
        } else {
            Log.d(TAG, "set normal params " + item.getName());
            holder.itemView.setLayoutParams(normalLayoutParams);
        }
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }
}