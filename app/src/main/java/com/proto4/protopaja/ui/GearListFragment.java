package com.proto4.protopaja.ui;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.proto4.protopaja.DaliGear;
import com.proto4.protopaja.R;

import java.util.ArrayList;

/**
 * Created by user on 12.08.17.
 */


public class GearListFragment extends Fragment {

    private static final String TAG = GearListFragment.class.getSimpleName();

    private ArrayList<GearListItem> listItems;
    private ArrayList<GearListItem> selectedItems;
    private RecyclerView recyclerView;

    private GearListItem expandedGroup;

    private boolean isSelecting;

    private Listener listener;


    public static final int ITEM_ACTION_OPEN = 1;

    public static final int ACTION_GROUP_SELECTED = 5;
    public static final int ACTION_EXPANDED_GROUP_SELECTED = 6;
    public static final int ACTION_SELECTION_START = 7;
    public static final int ACTION_SELECTION_END = 8;


    public GearListFragment() {

    }

    public static GearListFragment newInstance() {
        GearListFragment fragment = new GearListFragment();
        fragment.listItems = new ArrayList<>();
        fragment.selectedItems = new ArrayList<>();
        fragment.isSelecting = false;
        return fragment;
    }

    public void addItem(DaliGear gear) {
        listItems.add(new GearListItem(gear));
    }

    public void clear() {
        listItems.clear();
        selectedItems.clear();
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void clearSelection() {
        Log.d(TAG, "clearSelection()");
        if (listener != null) {
            listener.onGearListFragmentAction(ACTION_SELECTION_END, null);
        }
        isSelecting = false;
        selectedItems.clear();
        GearListItem item;
        for (int i = 0; i < listItems.size(); i++) {
            item = listItems.get(i);
            item.setCheckBoxVisible(false);
            item.setChecked(false);
            recyclerView.getAdapter().notifyItemChanged(i);
        }
    }

    public ArrayList<GearListItem> getSelectedItems() {
        Log.d(TAG, "getSelectedItems() returning " + selectedItems.size() + " items");
        return selectedItems;
    }

    public void setListener(Listener _listener) {
        listener = _listener;
    }

    public void update() {
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void expandGroup(GearListItem group) {
        Log.d(TAG, "expandGroup()");
        ArrayList<GearListItem> toRemove = new ArrayList<>();
        int groupPosition = -1;
        for (int i = 0; i < listItems.size(); i++) {
            GearListItem item = listItems.get(i);
            if (!item.isGroup())
                toRemove.add(item);
            else if (item == group)
                groupPosition = i;
        }
        if (groupPosition == -1) {
            Log.w(TAG, "unable to expand group");
            return;
        }
        if (expandedGroup != group) {
            ArrayList<DaliGear> gears = group.getGear().getGroup();
            for (int i = 0; i < gears.size(); i++) {
                DaliGear g = gears.get(i);
                listItems.add(++groupPosition, new GearListItem(g));
                Log.d(TAG, "added list item on expand");
            }
            expandedGroup = group;
        } else {
            Log.d(TAG, "group was already expanded");
            expandedGroup = null;
        }

        for (int i = 0; i < toRemove.size(); i++) {
            listItems.remove(toRemove.get(i));
        }
        update();
    }

    private void startSelection(int position) {
        Log.d(TAG, "startSelection(position=" + position + ")");
        if (listener != null)
            listener.onGearListFragmentAction(ACTION_SELECTION_START, null);
        GearListItem item;
        isSelecting = true;
        for (int i = 0; i < listItems.size(); i++) {
            item = listItems.get(i);
            if (!item.isGroup()) {
                item.setCheckBoxVisible(true);
                if (i == position)
                    selectedItems.add(item);    // add long clicked item to selectedItems
                item.setChecked(i == position); // check long clicked item
                recyclerView.getAdapter().notifyItemChanged(i);
            }
        }
    }

    private void select(int position) {
        Log.d(TAG, "select(" + position + ")");
        GearListItem item = listItems.get(position);
        item.setChecked(!item.isChecked());   // toggle select
        recyclerView.getAdapter().notifyItemChanged(position);
        if (item.isChecked())
            selectedItems.add(item);
        else
            selectedItems.remove(item);
        if (selectedItems.isEmpty())
            clearSelection();

        // DEBUG
        String si = "";
        for (GearListItem i : selectedItems) {
            si += i.getGear().getName() + "\n";
        }
        Log.d(TAG, "selectedItems (" + selectedItems.size() + "):\n" + si);
    }

    public void removeSelectedItems() {
        for (int i = 0; i < selectedItems.size(); i++) {
            listItems.remove(selectedItems.get(i));
        }
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void removeExpandedGroup() {
        if (expandedGroup == null) return;
        listItems.remove(expandedGroup);
        expandedGroup = null;
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public byte getExpandedGroupId() {
        if (expandedGroup == null)
            return (byte)255;
        return expandedGroup.getGear().getId();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView");
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Log.d(TAG, "onActivityCreated");
        Activity activity = getActivity();
        recyclerView = activity.findViewById(R.id.fragment_recycler_view);
        if (recyclerView == null)
            Log.w(TAG, "recyclerView==null");
        if (listItems == null) {
            Log.d(TAG, "listItems was null");
            listItems = new ArrayList<>();
        }
        recyclerView.setAdapter(new GearListAdapter(activity, listItems));
        recyclerView.setLayoutManager(new LinearLayoutManager(activity));

        // set on item click listener for recycler list items
        ItemClickSupport.addTo(recyclerView).setOnItemClickListener(new ItemClickSupport.OnItemClickListener() {
            @Override
            public void onItemClicked(final RecyclerView recyclerView, final int position, View v) {
                Log.d(TAG, "on list item clicked");
                if (position < 0 || position > listItems.size()) {
                    Log.w(TAG, "list index out of bounds (" + position + ")");
                    return;
                }
                final GearListItem item = listItems.get(position);
                if (isSelecting) {
                    if (item.isGroup()) {
                        if (listener != null)
                            listener.onGearListFragmentAction((item == expandedGroup) ?
                                    ACTION_EXPANDED_GROUP_SELECTED : ACTION_GROUP_SELECTED, item);
                    } else {
                        select(position);
                    }
                    return;
                }
                if (listener != null)
                    listener.onGearListFragmentAction(ITEM_ACTION_OPEN, item);

            }
        });

        ItemClickSupport.addTo(recyclerView).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(final RecyclerView recyclerView, int position, View v) {
                Log.d(TAG, "on list item long clicked");

                final GearListItem item = listItems.get(position);
                if (isSelecting) // doesn't react to long click
                    return false;
                if (item.isGroup()) {
                    Log.d(TAG, "long clicked group");
                    expandGroup(item);
                } else {
                    Log.d(TAG, "long clicked gear");
                    startSelection(position);
                }
                return true;
            }
        });
    }

    public interface Listener {
        void onGearListFragmentAction(int which, GearListItem item);
    }
}