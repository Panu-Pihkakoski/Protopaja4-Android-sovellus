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


public class RecyclerListAdapter extends RecyclerView.Adapter<RecyclerListAdapter.ViewHolder> {

    private static final String TAG = RecyclerListAdapter.class.getSimpleName();

    private Context context;
    private ArrayList<RecyclerListItem> listItems;

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
        public ImageView tempIcon, brIcon;

        public ViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.list_row_title);
            checkBox = (CheckBox) view.findViewById(R.id.list_item_checkbox);
            checkBox.setChecked(false);
            checkBox.setVisibility(View.GONE);
            tempIcon = view.findViewById(R.id.list_item_temp_icon);
            tempIcon.setVisibility(View.GONE);
            brIcon = view.findViewById(R.id.list_item_brightness_icon);
            brIcon.setVisibility(View.GONE);
            brIcon.setColorFilter(0xffa0a0f0);
        }
    }

    public RecyclerListAdapter(Context _context, ArrayList<RecyclerListItem> _listItems) {
        listItems = _listItems;
        context = _context;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.recycler_list_row, parent, false);
        return new ViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        RecyclerListItem item  = listItems.get(position);
        holder.title.setText(item.getTitle());
        int colorId;
        switch (item.getType()) {
            case RecyclerListItem.TYPE_BT_DEVICE:
                colorId = R.color.title_bt_device;
                break;
            case RecyclerListItem.TYPE_GEAR:
                colorId = R.color.title_gear;
                break;
            case RecyclerListItem.TYPE_GROUP:
                colorId = R.color.title_group;
                break;
            default:
                colorId = R.color.title_default;
                break;
        }
        int titleColor = context.getResources().getColor(colorId, null);
        holder.title.setTextColor(titleColor);
        if (item.getType() == RecyclerListItem.TYPE_GEAR) {
            holder.checkBox.setVisibility(item.showCheckBox() ? View.VISIBLE : View.GONE);
            holder.checkBox.setChecked(item.isChecked());
            holder.tempIcon.setVisibility(item.showCheckBox() ? View.GONE : item.showTempIcon() ? View.VISIBLE : View.GONE);
            holder.brIcon.setVisibility(item.showCheckBox() ? View.GONE : View.VISIBLE);
            holder.brIcon.setColorFilter(item.getBrightnessColor());
            Log.d(TAG, "brightness=" + item.getBrightnessColor());
        }

        if (position == listItems.size() - 1) {
            Log.d(TAG, "set last params " + item.getTitle());
            holder.itemView.setLayoutParams(lastLayoutParams);
        } else {
            Log.d(TAG, "set normal params " + item.getTitle());
            holder.itemView.setLayoutParams(normalLayoutParams);
        }
    }

    @Override
    public int getItemCount() {
        return listItems.size();
    }
}
