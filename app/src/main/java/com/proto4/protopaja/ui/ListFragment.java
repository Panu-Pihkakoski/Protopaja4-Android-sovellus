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
 * Created by user on 1.08.17.
 */

public class ListFragment extends Fragment {

    private static final String TAG = ListFragment.class.getSimpleName();

    private ArrayList<RecyclerListItem> listItems;
    private ArrayList<RecyclerListItem> selectedItems;
    private RecyclerView recyclerView;

    private RecyclerListItem expandedGroup;

    private boolean isSelecting;

    private ListFragmentListener listener;


    public static final int ITEM_ACTION_OPEN = 1;
    public static final int ITEM_ACTION_CONNECT = 2;
    public static final int ITEM_ACTION_SELECT = 3;
    public static final int ITEM_ACTION_EXPAND = 4;

    public static final int ACTION_GROUP_SELECTED = 5;
    public static final int ACTION_EXPANDED_GROUP_SELECTED = 6;
    public static final int ACTION_SELECTION_START = 7;
    public static final int ACTION_SELECTION_END = 8;


    public ListFragment() {

    }

    public static ListFragment newInstance(Context context) {
        ListFragment fragment = new ListFragment();
        fragment.listItems = new ArrayList<>();
        fragment.selectedItems = new ArrayList<>();
        fragment.isSelecting = false;
        return fragment;
    }

    public void addItem(RecyclerListItem item) {
        Log.d(TAG, "adding item to list (" + item.getTitle() + ")");
        listItems.add(item);
        Log.d(TAG, "listItems.size()==" + listItems.size());
        //recyclerView.getAdapter().notifyItemInserted(listItems.size()-1);
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void addItem(String title, int type, byte id) {
        addItem(new RecyclerListItem(title, type, id));
    }

    public void addItem(String title, String extra, int type, byte id) {
        addItem(new RecyclerListItem(title, extra, type, id));
    }

    public void clear() {
        if (listItems == null) {
            Log.d(TAG, "tried to clear null list");
            listItems = new ArrayList<>();
            return;
        }
        if (listItems.isEmpty())
            Log.d(TAG, "listItems was empty");
        else listItems.clear();
        if (selectedItems == null) {
            Log.d(TAG, "tried to clear null list (selectedItems)");
            selectedItems = new ArrayList<>();
            return;
        }
        if (selectedItems.isEmpty())
            Log.d(TAG, "selectedItems was empty");
        else selectedItems.clear();
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void clearSelection() {
        Log.d(TAG, "clearSelection()");
        if (listener != null) {
            listener.onListFragmentAction(ACTION_SELECTION_END, null);
        }
        isSelecting = false;
        selectedItems.clear();
        RecyclerListItem item;
        for (int i = 0; i < listItems.size(); i++) {
            item = listItems.get(i);
            item.showCheckBox(false);
            item.setChecked(false);
            recyclerView.getAdapter().notifyItemChanged(i);
        }
    }

    public RecyclerListItem getItemById(byte id) {
        for (RecyclerListItem item : listItems) {
            if (item.getId() == id)
                return item;
        }
        Log.w(TAG, "getItemById(): item not found");
        return null;
    }

    public ArrayList<RecyclerListItem> getSelectedItems() {
        Log.d(TAG, "getSelectedItems() returning " + selectedItems.size() + " items");
        return selectedItems;
    }

    public void setListener(ListFragmentListener _listener) {
        listener = _listener;
    }

    public void renameItem(byte itemId, String newName) {
        for (int i = 0; i < listItems.size(); i++) {
            if (listItems.get(i).getId() == itemId) {
                Log.d(TAG, "Renaming item with id " + itemId);
                listItems.get(i).setTitle(newName);
                recyclerView.getAdapter().notifyItemChanged(i);
                break;
            }
        }
    }

    public void update() {
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void expandGroup(RecyclerListItem group, ArrayList<DaliGear> gears) {

        ArrayList<RecyclerListItem> toRemove = new ArrayList<>();
        int groupPosition = -1;
        for (int i = 0; i < listItems.size(); i++) {
            RecyclerListItem item = listItems.get(i);
            if (item.getType() == RecyclerListItem.TYPE_GEAR)
                toRemove.add(item);
            else if (item == group)
                groupPosition = i;
        }
        if (groupPosition == -1) {
            Log.w(TAG, "unable to expand group");
            return;
        }
        if (expandedGroup != group) {
            for (int i = 0; i < gears.size(); i++) {
                DaliGear g = gears.get(i);
                listItems.add(++groupPosition,
                        new RecyclerListItem(g.getName(), RecyclerListItem.TYPE_GEAR, g.getId()));
            }
            expandedGroup = group;
        } else {
            expandedGroup = null;
        }

        for (int i = 0; i < toRemove.size(); i++) {
            listItems.remove(toRemove.get(i));
        }

    }

    private void toggleShowGroupMembers(int position) {
        Log.d(TAG, "toggleShowGroupMembers(position=" + position + ")");
        RecyclerListItem item = listItems.get(position);
        item.showExtraText(!item.showExtraText());
        recyclerView.getAdapter().notifyItemChanged(position);
    }

    private void startSelection(int position) {
        Log.d(TAG, "startSelection(position=" + position + ")");
        if (listener != null)
            listener.onListFragmentAction(ACTION_SELECTION_START, null);
        RecyclerListItem item;
        isSelecting = true;
        for (int i = 0; i < listItems.size(); i++) {
            item = listItems.get(i);
            if (item.getType() == RecyclerListItem.TYPE_GEAR) {
                item.showCheckBox(true);
                if (i == position)
                    selectedItems.add(item);    // add long clicked item to selectedItems
                item.setChecked(i == position); // check long clicked item
                recyclerView.getAdapter().notifyItemChanged(i);
            }
        }
        Log.d(TAG, "selectedItems.size()==" + selectedItems.size());
    }

    private void select(int position) {
        Log.d(TAG, "select(" + position + ")");
        RecyclerListItem item = listItems.get(position);
        item.setChecked(!item.isChecked());   // toggle select
        recyclerView.getAdapter().notifyItemChanged(position);
        if (item.isChecked())
            selectedItems.add(item);
        else
            selectedItems.remove(item);
        if (selectedItems.isEmpty())
            clearSelection();
        String si = "";
        for (RecyclerListItem i : selectedItems) {
            si += i.getTitle() + "\n";
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
        return expandedGroup.getId();
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
        recyclerView.setAdapter(new RecyclerListAdapter(activity, listItems));
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
                final RecyclerListItem item = listItems.get(position);
                if (isSelecting) {
                    if (item.getType() == RecyclerListItem.TYPE_GEAR)
                        select(position);
                    else if (item.getType() == RecyclerListItem.TYPE_GROUP) {
                        if (listener != null)
                            listener.onListFragmentAction((item == expandedGroup) ?
                                    ACTION_EXPANDED_GROUP_SELECTED : ACTION_GROUP_SELECTED, item);
                    }
                    return;
                }
                if (item.getType() == RecyclerListItem.TYPE_BT_DEVICE) { // connect to address in item extra
                    if (listener != null)
                        listener.onListFragmentAction(ITEM_ACTION_CONNECT, item);
                } else if (item.getType() == RecyclerListItem.TYPE_GEAR ||
                                item.getType() == RecyclerListItem.TYPE_GROUP) { // open gear fragment
                    if (listener != null)
                        listener.onListFragmentAction(ITEM_ACTION_OPEN, item);
                }/* else if (item.getAction() == ITEM_ACTION_EXPAND) {

                }*/
            }
        });

        ItemClickSupport.addTo(recyclerView).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(final RecyclerView recyclerView, int position, View v) {
                Log.d(TAG, "on list item long clicked");

                final RecyclerListItem item = listItems.get(position);
                if (item.getType() == RecyclerListItem.TYPE_BT_DEVICE || isSelecting) // doesn't react to long click
                    return false;
                if (item.getType() == RecyclerListItem.TYPE_GEAR) { // start selecting items
                    startSelection(position);
                } else if (item.getType() == RecyclerListItem.TYPE_GROUP) {
                    //toggleShowGroupMembers(position);
                    if (listener != null) {
                        listener.onListFragmentAction(ITEM_ACTION_EXPAND, item);
                    }
                }
                return true;
            }
        });
    }

    public interface ListFragmentListener {
        void onListFragmentAction(int which, RecyclerListItem item);
    }
}
