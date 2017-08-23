package com.proto4.protopaja.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.proto4.protopaja.R;

import java.util.ArrayList;

public class ListFragment extends Fragment {

    private static final String TAG = ListFragment.class.getSimpleName();

    private ArrayList<ProtoListItem> listItems;
    private ArrayList<ProtoListItem> selectedItems;
    private int selected;
    private RecyclerView recyclerView;

    private int expandedGroupId;

    private boolean isSelecting;

    private Listener listener;


    public static final int ITEM_ACTION_OPEN = 1;
    public static final int ITEM_ACTION_CONNECT = 2;

    public static final int ACTION_EXPAND_GROUP = 4;
    public static final int ACTION_GROUP_SELECTED = 5;
    public static final int ACTION_EXPANDED_GROUP_SELECTED = 6;
    public static final int ACTION_SELECTION_START = 7;
    public static final int ACTION_SELECTION_END = 8;


    public ListFragment() {

    }

    public static ListFragment newInstance() {
        ListFragment fragment = new ListFragment();
        fragment.listItems = new ArrayList<>();
        fragment.selectedItems = new ArrayList<>();
        fragment.selected = 0;
        fragment.isSelecting = false;
        return fragment;
    }

    public void addItem(String name, int type, int id) {
        Log.d(TAG, "adding item to list");
        listItems.add(new ProtoListItem(name, type, id));
    }

    public void addItem(ProtoListItem item) {
        Log.d(TAG, "adding item to list");
        listItems.add(item);
    }

    public void clear() {
        listItems.clear();
        selectedItems.clear();
        expandedGroupId = -1;
        //recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void clearSelection() {
        Log.d(TAG, "clearSelection()");
        if (listener != null) {
            listener.onListFragmentAction(ACTION_SELECTION_END, null);
        }
        isSelecting = false;
        selectedItems.clear();
        selected = 0;
        ProtoListItem item;
        for (int i = 0; i < listItems.size(); i++) {
            item = listItems.get(i);
            item.setCheckBoxVisible(false);
            item.setChecked(false);
            recyclerView.getAdapter().notifyItemChanged(i);
        }
    }

    public ArrayList<ProtoListItem> getSelectedItems() {
        Log.d(TAG, "getSelectedItems() returning " + selectedItems.size() + " items");
        return selectedItems;
    }

    public int getSelected() {
        return selected;
    }

    public void setListener(Listener _listener) {
        listener = _listener;
    }

    public void update() {
        Log.d(TAG, "update()");
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void expand(int groupId, ArrayList<ProtoListItem> items, boolean forceExpand) {
        Log.d(TAG, "expand()");

        ArrayList<ProtoListItem> toRemove = new ArrayList<>();
        int groupPosition = -1;
        for (int i = 0; i < listItems.size(); i++) {
            ProtoListItem item = listItems.get(i);
            if (item.getType() == ProtoListItem.TYPE_GEAR)
                toRemove.add(item);
            else if (item.getId() == groupId)
                groupPosition = i;
        }
        if (groupPosition == -1) {
            Log.d(TAG, "expand: group not found");
            groupPosition = 0;
        }
        if (expandedGroupId != groupId || forceExpand) {
            for (int i = 0; i < items.size(); i++) {
                listItems.add(++groupPosition, items.get(i));
                Log.d(TAG, "added list item on expand");
            }
            expandedGroupId = groupId;
        } else {
            Log.d(TAG, "group was already expanded");
            expandedGroupId = -1;
        }

        for (int i = 0; i < toRemove.size(); i++) {
            listItems.remove(toRemove.get(i));
        }

        update();
    }

    private void startSelection(int position) {
        Log.d(TAG, "startSelection(position=" + position + ")");
        if (listener != null)
            listener.onListFragmentAction(ACTION_SELECTION_START, null);
        ProtoListItem item;
        isSelecting = true;
        for (int i = 0; i < listItems.size(); i++) {
            item = listItems.get(i);
            if (!(item.getType() == ProtoListItem.TYPE_GROUP)) {
                item.setCheckBoxVisible(true);
                if (i == position) {
                    Log.d(TAG, "startSelection: item (id=" + item.getId() + ") checked");
                    selectedItems.add(item);    // add long clicked item to selectedItems
                    selected |= (1 << item.getId());
                }
                item.setChecked(i == position); // check long clicked item
                recyclerView.getAdapter().notifyItemChanged(i);
            }
        }
    }

    private void select(int position) {
        Log.d(TAG, "select(" + position + ")");
        ProtoListItem item = listItems.get(position);
        item.setChecked(!item.isChecked());   // toggle select
        recyclerView.getAdapter().notifyItemChanged(position);
        if (item.isChecked()) {
            Log.d(TAG, "select: item (id=" + item.getId() + ") checked");
            selectedItems.add(item);
            selected |= (1 << item.getId());
        } else {
            Log.d(TAG, "select: item (id=" + item.getId() + ") unchecked");
            selectedItems.remove(item);
            selected ^= (1 << item.getId());
        }
        if (selectedItems.isEmpty())
            clearSelection();

        // DEBUG
        String si = "";
        for (ProtoListItem i : selectedItems) {
            si += i.getName() + "\n";
        }
        Log.d(TAG, "selectedItems (" + selectedItems.size() + "):\n" + si);
        Log.d(TAG, "selected: " + selected);
    }

    public void removeSelectedItems() {
        for (int i = 0; i < selectedItems.size(); i++) {
            listItems.remove(selectedItems.get(i));
        }
        clearSelection();
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public void removeExpandedGroup() {
        Log.d(TAG, "removeExpandedGroup: expandedGroupId==" + expandedGroupId);
        if (expandedGroupId == -1 || expandedGroupId == 255) return;
        ProtoListItem expanded = getExpandedGroup();
        if (expanded == null) {
            Log.d(TAG, "removeExpandedGroup: expanded group not found");
            return;
        }
        listItems.remove(expanded);
        expandedGroupId = -1;
        recyclerView.getAdapter().notifyDataSetChanged();
    }

    public int getExpandedGroupId() {
        if (expandedGroupId == -1)
            return 255;
        return expandedGroupId;
    }

    public ProtoListItem getExpandedGroup() {
        return getItemById(expandedGroupId);
    }

    public ProtoListItem getItemById(int id) {
        int pos = 0;
        while (pos < listItems.size() && listItems.get(pos).getId() != id) {
            pos++;
        }
        if (pos == listItems.size()) return null;
        return listItems.get(pos);
    }

    public boolean isListingGears() {
        if (listItems.size() == 0) return true;
        return !(listItems.get(0).getType() == ProtoListItem.TYPE_DEVICE);
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
        recyclerView.setAdapter(new ProtoListAdapter(activity, listItems));
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
                final ProtoListItem item = listItems.get(position);

                if (item.getType() == ProtoListItem.TYPE_DEVICE) {
                    Log.d(TAG, "clicked on bt device");
                    if (listener != null)
                        listener.onListFragmentAction(ITEM_ACTION_CONNECT, item);
                    return;
                }

                if (isSelecting) {
                    if (item.getType() == ProtoListItem.TYPE_GROUP) {
                        if (listener != null)
                            listener.onListFragmentAction((item.getId() == expandedGroupId) ?
                                    ACTION_EXPANDED_GROUP_SELECTED : ACTION_GROUP_SELECTED, item);
                    } else {
                        select(position);
                    }
                    return;
                }
                if (listener != null)
                    listener.onListFragmentAction(ITEM_ACTION_OPEN, item);

            }
        });

        ItemClickSupport.addTo(recyclerView).setOnItemLongClickListener(new ItemClickSupport.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClicked(final RecyclerView recyclerView, int position, View v) {
                Log.d(TAG, "on list item long clicked");

                final ProtoListItem item = listItems.get(position);
                if (isSelecting || item.getType() == ProtoListItem.TYPE_DEVICE) // doesn't react to long click
                    return false;
                if (item.getType() == ProtoListItem.TYPE_GROUP) {
                    Log.d(TAG, "long clicked group");
                    if (listener != null)
                        listener.onListFragmentAction(ACTION_EXPAND_GROUP, item);
                    //expandGroup(item);
                } else {
                    Log.d(TAG, "long clicked gear");
                    startSelection(position);
                }
                return true;
            }
        });
    }

    public interface Listener {
        void onListFragmentAction(int which, ProtoListItem item);
    }
}