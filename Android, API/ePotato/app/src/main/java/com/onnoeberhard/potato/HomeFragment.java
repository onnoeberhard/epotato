package com.onnoeberhard.potato;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Notice;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import javax.net.ssl.HttpsURLConnection;

import me.leolin.shortcutbadger.ShortcutBadger;

public class HomeFragment extends Fragment {

    public static final String HOME_TYPE = "type";
    public static final int HOME_TYPE_RECEIVE = 0;
    public static final int HOME_TYPE_FEED = 1;
    public static final int HOME_TYPE_SENT = 2;
    private int type;

    private LocalDatabaseHandler ldb;

    private MainActivity mainActivity;
    private SwipeRefreshLayout refresh;
    private RecyclerView recycler;
    private PotatoAdapter adapter;
    private PreCachingLayoutManager layoutManager;
    private boolean noRead = true;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateList();
        }
    };

    @Override
    public void onResume() {
        super.onResume();
        updateList();
        mainActivity.registerReceiver(receiver, new IntentFilter(type == HOME_TYPE_RECEIVE ? MainActivity.POTATO_RECEIVED : type == HOME_TYPE_FEED ? MainActivity.POTATO_FEED : MainActivity.POTATO_SENT));
    }

    @Override
    public void onPause() {
        super.onPause();
        mainActivity.unregisterReceiver(receiver);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mainActivity = (MainActivity) getActivity();
        ldb = new LocalDatabaseHandler(getActivity());
        type = getArguments().getInt(HOME_TYPE);
        View v = inflater.inflate(R.layout.f_home, container, false);
        refresh = (SwipeRefreshLayout) v.findViewById(R.id.swiperefresh);
        refresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refresh();
            }
        });
        refresh.setRefreshing(true);
        recycler = (RecyclerView) v.findViewById(R.id.homeRecycler);
        layoutManager = new PreCachingLayoutManager(mainActivity);
        ((TextView) v.findViewById(R.id.nors_text)).setText(type == HOME_TYPE_RECEIVE ? getString(R.string.noRPotatoes) : type == HOME_TYPE_FEED ? getString(R.string.noFPotatoes) : getString(R.string.noSPotatoes));
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        layoutManager.setExtraLayoutSpace(2 * getResources().getDisplayMetrics().heightPixels);
        recycler.setLayoutManager(layoutManager);
        recycler.setItemViewCacheSize(5);
        recycler.setDrawingCacheEnabled(true);
        adapter = new PotatoAdapter(mainActivity, v.findViewById(R.id.nors_view), type);
        recycler.setAdapter(adapter);
        if (type == HOME_TYPE_RECEIVE) {
            recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE)
                        markRead();
                }
            });
            recycler.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener() {
                @Override
                public void onChildViewAttachedToWindow(View view) {
                    if (noRead)
                        markRead();
                }

                @Override
                public void onChildViewDetachedFromWindow(View view) {
                }
            });
        }
        return v;
    }

    private void markRead() {
        noRead = false;
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = layoutManager.findFirstCompletelyVisibleItemPosition(); i <= layoutManager.findLastCompletelyVisibleItemPosition(); i++)
                    if (i >= 0 && adapter.items.get(i).ptype == PotatoAdapter.PotatoItem.TYPE_POTATO)
                        ldb.delete(LocalDatabaseHandler.NEW_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID, ((PotatoAdapter.Potato) adapter.items.get(i)).pid);
                ShortcutBadger.applyCount(mainActivity, ldb.getAll(LocalDatabaseHandler.NEW_POTATOES).size());
            }
        }, 250);
    }

    private void refresh() {
        FIIDService.updateFIEPID(mainActivity, new FIIDService.FIEPIDRequest() {
            @Override
            public void onResult() {
                updateList();
            }
        });
    }

    private void updateList() {
        if (recycler != null) {
            if (type == HOME_TYPE_RECEIVE) {
                noRead = true;
                markRead();
            }
            ((PotatoAdapter) recycler.getAdapter()).update();
            recycler.getAdapter().notifyDataSetChanged();
        } else
            refresh.setRefreshing(false);
        mainActivity.updateTabBadges();
        if (type == HOME_TYPE_FEED)
            ldb.truncate(LocalDatabaseHandler.NEW_FEED_POTATOES);
    }

    private class PreCachingLayoutManager extends LinearLayoutManager {
        private static final int DEFAULT_EXTRA_LAYOUT_SPACE = 600;
        private int extraLayoutSpace = -1;

        private PreCachingLayoutManager(Context context) {
            super(context);
        }

        private void setExtraLayoutSpace(int extraLayoutSpace) {
            this.extraLayoutSpace = extraLayoutSpace;
        }

        @Override
        protected int getExtraLayoutSpace(RecyclerView.State state) {
            return extraLayoutSpace >= 0 ? extraLayoutSpace : DEFAULT_EXTRA_LAYOUT_SPACE;
        }
    }

    private class PotatoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private final int VIEW_BIG = 0;
        private final int VIEW_SMALL = 1;
        private final int VIEW_LOAD = 2;

        MainActivity mainActivity;
        LocalDatabaseHandler ldb;

        View noitemsview;

        int type;

        int length = 5;

        ArrayList<PotatoItem> items = new ArrayList<>();

        PotatoAdapter(MainActivity activity, View _noitemsview, int _type) {
            mainActivity = activity;
            ldb = new LocalDatabaseHandler(mainActivity);
            noitemsview = _noitemsview;
            type = _type;
            update();
        }

        void update() {
            items.clear();
            ArrayList<Map<String, String>> potatoes = ldb.getAll(type == HOME_TYPE_RECEIVE ? LocalDatabaseHandler.RECEIVED_POTATOES : type == HOME_TYPE_FEED ? LocalDatabaseHandler.FEED_POTATOES : LocalDatabaseHandler.SENT_POTATOES);
            for (Map<String, String> potato : potatoes)
                items.add(new Potato(potato));
            Collections.sort(items);
            Collections.reverse(items);
            if (items.size() == 0) {
                noitemsview.setVisibility(View.VISIBLE);
                refresh.setRefreshing(false);
            } else {
                noitemsview.setVisibility(View.GONE);
                // System.out.println(((Potato) items.get(items.size() > length ? length - 2 : items.size() - 1)).text);
                items.get(items.size() > length ? length - 2 : items.size() - 1).last = true;
            }
            if (items.size() > length)
                items.add(new Load());
            Collections.sort(items);
            Collections.reverse(items);
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(viewType == VIEW_BIG ? R.layout.v_list_potato_big : viewType == VIEW_SMALL ? R.layout.v_list_potato_small : R.layout.v_list_potato_load, parent, false);
            return viewType == VIEW_BIG ? new BigViewHolder(v, viewType) : viewType == VIEW_SMALL ? new SmallViewHolder(v, viewType) : new LoadViewHolder(v);
        }

        @Override
        public void onBindViewHolder(final RecyclerView.ViewHolder holder, final int position) {
            items.get(position).bindViewHolder(holder);
        }

        @Override
        public int getItemViewType(int position) {
            return items.get(position).ptype == PotatoItem.TYPE_POTATO ? ((Potato) items.get(position)).nieuw ? VIEW_BIG : VIEW_SMALL : VIEW_LOAD;
        }

        @Override
        public int getItemCount() {
            return items.size() > length ? length : items.size();
        }

        class PotatoItem implements Comparable<PotatoItem> {

            static final int TYPE_POTATO = 1;
            static final int TYPE_LOAD = 2;
            String dt;
            int ptype = TYPE_POTATO;

            RecyclerView.ViewHolder holder;
            boolean last = false;

            @Override
            public int compareTo(@NonNull PotatoItem another) {
                return dt.compareTo(another.dt);
            }

            void bindViewHolder(RecyclerView.ViewHolder holder) {
                this.holder = holder;
                if (ptype == TYPE_POTATO)
                    ((PotatoViewHolder) holder).bind(this);
                else if (ptype == TYPE_LOAD)
                    ((LoadViewHolder) holder).bind(this);

            }
        }

        class Potato extends PotatoItem {
            boolean nieuw = false;
            String pid;
            String name = "";
            String text;
            String date;
            String contact_name;
            int form;
            boolean sent = false;
            int status = -1;

            ArrayList<String> uids;
            ArrayList<String> tss;
            ArrayList<String> names;
            String uid;
            boolean ts;

            Potato(Map<String, String> potato) {
                ptype = TYPE_POTATO;
                pid = potato.get(LocalDatabaseHandler.ID);
                if (type == HOME_TYPE_RECEIVE) {
                    uid = potato.get(LocalDatabaseHandler.UID);
                    name = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.CONTACT_NAME);
                    name = name == null ? ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID) : name;
                    name = name == null ? ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID) : name;
                    name = name == null ? "" : name;
                    contact_name = ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID);
                    ts = Boolean.parseBoolean(potato.get(LocalDatabaseHandler.RECEIVED_POTATOES_TS));
                } else if (type == HOME_TYPE_FEED) {
                    uid = potato.get(LocalDatabaseHandler.UID);
                    name = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.CONTACT_NAME);
                    name = name == null ? ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID) : name;
                    name = name == null ? ldb.get(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID) : name;
                    name = name == null ? ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID) : name;
                    name = name == null ? "" : name;
                } else {
                    status = Integer.parseInt(potato.get(LocalDatabaseHandler.SENT_POTATOES_CODE));
                    uids = LocalDatabaseHandler.deserialize(potato.get(LocalDatabaseHandler.SENT_POTATOES_UIDS));
                    tss = LocalDatabaseHandler.deserialize(potato.get(LocalDatabaseHandler.SENT_POTATOES_TSS));
                    names = LocalDatabaseHandler.deserialize(potato.get(LocalDatabaseHandler.SENT_POTATOES_NAMES));
                    sent = true;
                    for (String uid : uids) {
                        String _name = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.CONTACT_NAME);
                        _name = _name == null ? ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID) : _name;
                        _name = _name == null ? ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID) : _name;
                        _name = _name == null && uid.equals(Long.toString(ContactsAdapter.ID_FOLLOWERS)) ? getString(R.string.followers) : _name;
                        if (_name != null)
                            name += name.equals("") ? _name : ", " + _name;
                    }
                    if (uids.size() == 1)
                        contact_name = ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, uids.get(0), LocalDatabaseHandler.EPID);
                    for (String pname : LocalDatabaseHandler.deserialize(potato.get(LocalDatabaseHandler.SENT_POTATOES_NAMES))) {
                        if (pname != null)
                            name += name.equals("") ? pname : ", " + pname;
                    }
                }
                text = potato.get(LocalDatabaseHandler.POTATO_TEXT);
                try {
                    date = LocalDatabaseHandler.getNiceDate(potato.get(LocalDatabaseHandler.DT), mainActivity);
                } catch (ParseException e) {
                    e.printStackTrace();
                }
                dt = potato.get(LocalDatabaseHandler.DT);
                form = Integer.parseInt(potato.get(LocalDatabaseHandler.POTATO_FORM));
                nieuw = type == HOME_TYPE_RECEIVE && ldb.get(LocalDatabaseHandler.NEW_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID, pid, LocalDatabaseHandler.ID) != null;
            }
        }

        class Load extends PotatoItem {
            Load() {
                ptype = TYPE_LOAD;
                dt = items.get(length - 1).dt + "1";
            }
        }

        class LoadViewHolder extends RecyclerView.ViewHolder {

            TextView b;

            LoadViewHolder(View itemView) {
                super(itemView);
                b = (TextView) itemView.findViewById(R.id.button);
            }

            void bind(final PotatoItem p) {
                b.setText(R.string.loadMore);
                b.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        refresh.setRefreshing(true);
                        length += items.size() - length > 5 ? 5 : items.size() - length;
                        for (PotatoItem item : items)
                            item.last = false;
                        items.get(items.size() > length ? length - 2 : items.size() - 1).last = true;
                        if (items.size() > length)
                            p.dt = items.get(length).dt + "1";
                        else
                            items.remove(p);
                        recycler.setItemViewCacheSize(length);
                        Collections.sort(items);
                        Collections.reverse(items);
                        notifyDataSetChanged();
                    }
                });
            }
        }

        class BigViewHolder extends PotatoViewHolder {

            View menub;

            BigViewHolder(View itemView, int viewType) {
                super(itemView, viewType);
                menub = itemView.findViewById(R.id.menu);
            }

            @Override
            void bind(final PotatoAdapter.PotatoItem pi) {
                p = (Potato) pi;
                super.bind(p);
                ll.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        sendNew();
                    }
                });
                menub.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        PopupMenu popup = new PopupMenu(getActivity(), v);
                        popup.inflate(R.menu.new_potato);
                        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem menuItem) {
                                switch (menuItem.getItemId()) {
                                    case R.id.reply:
                                        sendNew();
                                        break;
                                    case R.id.addcontact:
                                        addToContacts();
                                        break;
                                    case R.id.markread:
                                        ldb.delete(LocalDatabaseHandler.NEW_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID, p.pid);
                                        p.nieuw = false;
                                        mainActivity.tabBadgesMinusOne();
                                        notifyDataSetChanged();
                                        ShortcutBadger.applyCount(mainActivity, ldb.getAll(LocalDatabaseHandler.NEW_POTATOES).size());
                                        break;
                                    case R.id.delete:
                                        delete();
                                        break;
                                }
                                return false;
                            }
                        });
                        popup.show();
                    }
                });
            }
        }

        class SmallViewHolder extends PotatoViewHolder {

            ImageView statusiv;

            SmallViewHolder(View itemView, int viewType) {
                super(itemView, viewType);
                statusiv = (ImageView) itemView.findViewById(R.id.status);
            }

            @Override
            void bind(final PotatoAdapter.PotatoItem pi) {
                p = (Potato) pi;
                super.bind(p);
                ll.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                        if (p.sent && p.status != LocalDatabaseHandler.SENT_POTATOES_CODE_NULL) {
                            builder.setTitle(getString(R.string.status) + (p.status == LocalDatabaseHandler.SENT_POTATOES_CODE_SENDING ? getString(R.string.sending)
                                    : p.status == LocalDatabaseHandler.SENT_POTATOES_CODE_SENT ? getString(R.string.sent)
                                    : p.status == LocalDatabaseHandler.SENT_POTATOES_CODE_RECEIVED ? getString(R.string.received) : getString(R.string.errorSending)));
                        }
                        if (p.sent) {
                            final boolean addToContacts = p.uids.size() == 1 && ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, p.uids.get(0), LocalDatabaseHandler.ID) == null && p.contact_name != null;
                            final boolean sendAgain = p.status == LocalDatabaseHandler.SENT_POTATOES_CODE_ERROR;
                            String[] options = addToContacts && sendAgain ? new String[]{getString(R.string.sendAgain), getString(R.string.addXToContacts, p.contact_name), getString(R.string.sendNewPotato), getString(R.string.delete)}
                                    : addToContacts ? new String[]{getString(R.string.addXToContacts, p.contact_name), getString(R.string.sendNewPotato), getString(R.string.delete)}
                                    : sendAgain ? new String[]{getString(R.string.sendAgain), getString(R.string.sendNewPotato), getString(R.string.delete)}
                                    : p.uids.size() > 0 ? new String[]{getString(R.string.sendNewPotato), getString(R.string.delete)}
                                    : new String[]{getString(R.string.delete)};
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0 && sendAgain)
                                        sendAgain();
                                    else if (addToContacts && (sendAgain && which == 1 || !sendAgain && which == 0))
                                        addToContacts();
                                    else if (which == 1 && (addToContacts ^ sendAgain) || which == 2 && addToContacts && sendAgain || which == 0 && p.uids.size() > 0)
                                        sendNew();
                                    else
                                        delete();
                                }
                            });
                        } else if (type == HOME_TYPE_FEED) {
                            final boolean following = ldb.get(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, p.uid, LocalDatabaseHandler.ID) != null && ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, p.uid, LocalDatabaseHandler.ID) == null;
                            String[] options = following ? new String[]{getString(R.string.reply), getString(R.string.unfollowX, p.name), getString(R.string.delete)}
                                    : new String[]{getString(R.string.reply), getString(R.string.delete)};
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0)
                                        sendNew();
                                    else if (which == 1 && following)
                                        unfollow();
                                    else
                                        delete();
                                }
                            });
                        } else {
                            final boolean addToContacts = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, p.uid, LocalDatabaseHandler.ID) == null && p.contact_name != null;
                            String[] options = addToContacts ? new String[]{getString(R.string.reply), getString(R.string.addXToContacts, p.contact_name), getString(R.string.delete)}
                                    : new String[]{getString(R.string.reply), getString(R.string.delete)};
                            builder.setItems(options, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    if (which == 0)
                                        sendNew();
                                    else if (which == 1 && addToContacts)
                                        addToContacts();
                                    else
                                        delete();
                                }
                            });
                        }
                        builder.show();
                    }
                });
                updateStatus();
            }

            void updateStatus() {
                statusiv.setVisibility(p != null && p.sent && p.status != LocalDatabaseHandler.SENT_POTATOES_CODE_NULL ? View.VISIBLE : View.GONE);
                if (p != null && p.sent && p.status != LocalDatabaseHandler.SENT_POTATOES_CODE_NULL) {
                    statusiv.setImageResource(p.status == LocalDatabaseHandler.SENT_POTATOES_CODE_SENDING ? R.drawable.ic_query_builder_black_24dp
                            : p.status == LocalDatabaseHandler.SENT_POTATOES_CODE_SENT ? R.drawable.ic_done_black_24dp
                            : p.status == LocalDatabaseHandler.SENT_POTATOES_CODE_RECEIVED ? R.drawable.ic_done_all_black_24dp
                            : R.drawable.ic_error_black_24dp);
                }
            }
        }
    }

    class PotatoViewHolder extends RecyclerView.ViewHolder {

        int viewType;

        LinearLayout ll;
        FontTextView nametv;
        TextView datetv;
        PotatoView potato;

        PotatoAdapter.Potato p;

        PotatoViewHolder(View itemView, int viewType) {
            super(itemView);
            this.viewType = viewType;
            ll = (LinearLayout) itemView.findViewById(R.id.ll);
            nametv = (FontTextView) itemView.findViewById(R.id.nametv);
            potato = (PotatoView) itemView.findViewById(R.id.potato);
            View container = itemView.findViewById(R.id.container);
            container.setVisibility(View.INVISIBLE);
            potato.setVisItem(container);
            datetv = (TextView) itemView.findViewById(R.id.datetv);
        }

        void bind(final PotatoAdapter.PotatoItem pi) {
            this.p = (PotatoAdapter.Potato) pi;
            nametv.setText(p.name);
            if (!potato.getText().equals(p.text) || potato.getForm() != p.form) {
                potato.hr = true;
                potato.setup(p.form, p.text, pi.last ? new PotatoView.Callback() {
                    @Override
                    public void callback() {
                        refresh.setRefreshing(false);
                    }
                } : null);
            } else if (pi.last)
                refresh.setRefreshing(false);
            datetv.setText((p.ts ? getString(R.string.ts) + "\n" : "") + p.date);
        }

        void sendNew() {
            ArrayList<String> uids = new ArrayList<>();
            ArrayList<String> tss = new ArrayList<>();
            if (type == HOME_TYPE_RECEIVE) {
                uids.add(p.uid);
                tss.add(Boolean.toString(p.ts));
            } else if (type == HOME_TYPE_FEED) {
                uids.add(p.uid);
            } else {
                uids = new ArrayList<>(p.uids);
                for (String uid : p.uids)
                    if (Long.parseLong(uid) < 0)
                        uids.remove(uid);
                tss.addAll(p.tss);
            }
            Intent i = new Intent(getActivity(), AdActivity.class);
            i.putExtra(SendPotatoActivity.UIDS, uids);
            i.putExtra(SendPotatoActivity.TSS, tss);
            i.putExtra(SendPotatoActivity.REPLY, true);
            startActivity(i);
        }

        void sendAgain() {
            ldb.update(LocalDatabaseHandler.SENT_POTATOES,
                    LocalDatabaseHandler.ID, p.pid,
                    LocalDatabaseHandler.SENT_POTATOES_CODE, Integer.toString(LocalDatabaseHandler.SENT_POTATOES_CODE_SENDING));
            new EndpointsHandler(getActivity()).sendPotato(new EndpointsHandler.APIRequest() {
                @Override
                public void onResult(Object result) {
                    Notice n = (Notice) result;
                    ldb.update(LocalDatabaseHandler.SENT_POTATOES,
                            LocalDatabaseHandler.ID, p.pid, LocalDatabaseHandler.SENT_POTATOES_CODE,
                            Integer.toString(n != null && n.getCode() == HttpsURLConnection.HTTP_OK ?
                                    LocalDatabaseHandler.SENT_POTATOES_CODE_SENT : LocalDatabaseHandler.SENT_POTATOES_CODE_ERROR));
                    getActivity().sendBroadcast(new Intent(MainActivity.POTATO_SENT));
                }
            }, p.uids, p.tss, potato.getText(), p.form, p.pid);
            getActivity().sendBroadcast(new Intent(MainActivity.POTATO_SENT));
            FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.SEND_AGAIN, null);
        }

        void addToContacts() {
            String uid;
            if (type == HOME_TYPE_RECEIVE)
                uid = p.uid;
            else
                uid = p.uids.get(0);
            if (p.contact_name != null) {
                ldb.inup(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid,
                        LocalDatabaseHandler.EPID, p.contact_name);
                Toast.makeText(mainActivity, R.string.addedToContacts, Toast.LENGTH_SHORT).show();
            } else
                Toast.makeText(mainActivity, getString(R.string.errorToast), Toast.LENGTH_SHORT).show();
            adapter.notifyDataSetChanged();
            FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.ADD_TO_CONTACTS, null);
        }

        void unfollow() {
            ldb.delete(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, p.uid);
            new EndpointsHandler(mainActivity).unfollow(null, p.uid);
            FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.UNFOLLOW, null);
        }

        void delete() {
            ldb.delete(type == HOME_TYPE_RECEIVE ? LocalDatabaseHandler.RECEIVED_POTATOES : type == HOME_TYPE_FEED ? LocalDatabaseHandler.FEED_POTATOES : LocalDatabaseHandler.SENT_POTATOES, LocalDatabaseHandler.ID, p.pid);
            if (type == HOME_TYPE_RECEIVE)
                ldb.delete(LocalDatabaseHandler.NEW_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID, p.pid);
            else if (type == HOME_TYPE_FEED)
                ldb.delete(LocalDatabaseHandler.NEW_FEED_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID, p.pid);
            mainActivity.updateTabBadges();
            adapter.items.remove(p);
            adapter.notifyDataSetChanged();
            FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.DELETE_POTATO, null);
        }
    }
}
