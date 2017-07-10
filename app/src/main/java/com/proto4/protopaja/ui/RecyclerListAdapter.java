package com.proto4.protopaja.ui;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

        public ViewHolder(View view) {
            super(view);
            title = (TextView) view.findViewById(R.id.list_row_title);
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
