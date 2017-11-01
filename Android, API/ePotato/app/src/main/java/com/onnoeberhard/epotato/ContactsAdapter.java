package com.onnoeberhard.epotato;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.LongSparseArray;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

class ContactsAdapter extends RecyclerView.Adapter {

    static final int CONTACTS_PERMISSION = 0;
    private static final String RANKING = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    static int CONTACTS_ACIVITY = 0;
    static int SEND_PEOPLE = 1;
    static int FOLLOWING = 2;
    static int ID_NULL = -1;
    static int ID_STRANGER = -2;
    static int ID_PHONES = -3;
    static int ID_FOLLOWERS = -4;
    ActionMode actionMode;
    View loading, content;
    SparseArray<CheckItem> checkItems = new SparseArray<>();
    private int mode = CONTACTS_ACIVITY;
    private AppCompatActivity activity;
    private LocalDatabaseHandler ldb;
    private SharedPreferences sp;
    private ArrayList<ContactsItem> items = new ArrayList<>();
    private int selection = 0;
    private int form;
    private String message;
    private LongSparseArray<Integer[]> entanglements = new LongSparseArray<>();
    private Profile _profile;
    private boolean addToContactsNow = false;

    ContactsAdapter(int mode, AppCompatActivity a, View loading, View content) {
        this(mode, a, 1, "", loading, content);
    }

    ContactsAdapter(int mode, AppCompatActivity a, int f, String m, View loading, View content) {
        super();
        this.mode = mode;
        activity = a;
        form = f;
        message = m;
        this.loading = loading;
        this.content = content;
        ldb = new LocalDatabaseHandler(activity);
        sp = PreferenceManager.getDefaultSharedPreferences(activity);
        populate();
        if (!sp.contains(LocalDatabaseHandler.PERMISSION_CONTACTS))
            addPhoneContacts(true, activity, this);
    }

    static void addPhoneContacts(boolean showNotice, final Activity a, final ContactsAdapter ca) {
        if (ContextCompat.checkSelfPermission(a, android.Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(a, android.Manifest.permission.READ_CONTACTS) && showNotice) {
                new AlertDialog.Builder(a).setTitle(R.string.readContacts)
                        .setMessage(R.string.contactsPermissionNotice)
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        })
                        .setOnCancelListener(new DialogInterface.OnCancelListener() {
                            @Override
                            public void onCancel(DialogInterface dialog) {
                                addPhoneContacts(false, a, ca);
                            }
                        })
                        .setOnDismissListener(new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialog) {
                                addPhoneContacts(false, a, ca);
                            }
                        }).show();
            } else
                ActivityCompat.requestPermissions(a, new String[]{android.Manifest.permission.READ_CONTACTS}, CONTACTS_PERMISSION);
        } else {
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(a);
            if (!sp.contains(LocalDatabaseHandler.PERMISSION_CONTACTS) && sp.contains(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB))
                sp.edit().putBoolean(LocalDatabaseHandler.PERMISSION_CONTACTS, true).apply();
            else if (!sp.contains(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB) && sp.getInt(LocalDatabaseHandler.CONTACTS_PERMISSION_TRY, 0) > 3)
                sp.edit().putBoolean(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB, true).apply();
            else if (!sp.contains(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB))
                sp.edit().putInt(LocalDatabaseHandler.CONTACTS_PERMISSION_TRY, sp.getInt(LocalDatabaseHandler.CONTACTS_PERMISSION_TRY, 0) + 1).apply();
            MainService.loadContacts(a, ca, null);
        }
    }

    static void contactsPermissionRequestCallback(int requestCode, int[] grantResults, Activity a, Menu m, ContactsAdapter ca) {
        if (requestCode == CONTACTS_PERMISSION) {
            boolean permission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(a);
            if (m != null) {
                m.findItem(R.id.checkcontacts).setVisible(!permission);
                m.findItem(R.id.reload).setVisible(permission);
            }
            if (permission)
                addPhoneContacts(true, a, ca);
            if (sp.contains(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB))
                sp.edit().putBoolean(LocalDatabaseHandler.PERMISSION_CONTACTS, permission).apply();
        }
    }

    void populate() {
        items.clear();
        if (mode == CONTACTS_ACIVITY) {
            ArrayList<Map<String, String>> contacts = ldb.getAll(LocalDatabaseHandler.CONTACTS);
            items.add(new CaptionItem("1", activity.getString(R.string.allContacts)));
            if (contacts.size() > 0) {
                for (Map<String, String> contact : contacts) {
                    String text = contact.get(LocalDatabaseHandler.CONTACT_NAME);
                    String subtext = contact.get(LocalDatabaseHandler.EPID);
                    if (text == null) {
                        text = subtext;
                        subtext = "";
                    }
                    items.add(new PersonItem("3", Long.parseLong(contact.get(LocalDatabaseHandler.UID)), text, subtext, contact.get(LocalDatabaseHandler.PHONE) == null, this));
                }
            } else
                items.add(new TextItem("2", activity.getString(R.string.noContacts)));
            items.add(new ButtonItem("4", ButtonItem.ADD, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addepid();
                }
            }, activity));
            Map<Map<String, String>, Integer> suggested = new HashMap<>();
            for (Map<String, String> sugContact : ldb.getAll(LocalDatabaseHandler.SUGGESTED_CONTACTS))
                suggested.put(sugContact, Integer.parseInt(sugContact.get(LocalDatabaseHandler.CONTACTS_SCORE)));
            if (suggested.size() > 0) {
                items.add(new CaptionItem("5", activity.getString(R.string.suggestedContacts)));
                items.add(new TextItem("6", activity.getString(R.string.clickAddContacts)));
                suggested = LocalDatabaseHandler.sortByValue(suggested);
                for (int i = 0; i < (suggested.size() > 10 ? 10 : suggested.size()); i++) {
                    Map<String, String> contact = (Map<String, String>) suggested.keySet().toArray()[i];
                    int x = Integer.MAX_VALUE - Integer.parseInt(contact.get(LocalDatabaseHandler.CONTACTS_SCORE));
                    items.add(new SuggestionItem(this, "7" + x, contact.get(LocalDatabaseHandler.UID), contact.get(LocalDatabaseHandler.EPID)));
                }
            }
            if (sp.getBoolean(LocalDatabaseHandler.PERMISSION_CONTACTS, true)) {
                ArrayList<Map<String, String>> phone_contacts = ldb.getAll(LocalDatabaseHandler.PHONE_CONTACTS);
                if (phone_contacts.size() > 0) {
                    items.add(new CaptionItem("8", activity.getString(R.string.phoneContacts)));
                    items.add(new TextItem("9", activity.getString(R.string.clickSendInvite)));
                    for (Map<String, String> contact : phone_contacts)
                        items.add(new PhoneItem(this, "A", contact.get(LocalDatabaseHandler.CONTACT_NAME)));
                }
            }
        } else if (mode == SEND_PEOPLE) {
            items.add(new PotatoItem(form, message, (SendPeopleActivity) activity));
            FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
            remoteConfig.setDefaults(R.xml.remote_config_defaults);
            if (sp.getBoolean(LocalDatabaseHandler.TOTAL_STRANGERS, true) && remoteConfig.getBoolean("ts"))
                items.add(new CheckItem(this, "1", activity.getString(R.string.sendTS), ID_STRANGER, false, false, true, false, false));
            if (remoteConfig.getBoolean("feed"))
                items.add(new CheckItem(this, "2", activity.getString(R.string.sendFollowers), ID_FOLLOWERS, false, false, false, false, false));
            items.add(new ButtonItem("B", ButtonItem.ADD, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    addepid();
                }
            }, activity));
            ArrayList<Map<String, String>> contacts = ldb.getAll(LocalDatabaseHandler.CONTACTS);
            Map<Map<String, String>, Integer> frequents = new HashMap<>();
            if (contacts.size() > 0) {
                items.add(new CaptionItem("6", activity.getString(R.string.allContacts)));
                items.add(new CheckItem(this, "7", activity.getString(R.string.sendAllContacts), true, false, true, true, false));
                for (Map<String, String> contact : contacts) {
                    String name = contact.get(LocalDatabaseHandler.CONTACT_NAME);
                    name = name == null ? contact.get(LocalDatabaseHandler.EPID) : name;
                    items.add(new CheckItem(this, "8", name, Long.parseLong(contact.get(LocalDatabaseHandler.UID))));
                    String _score = contact.get(LocalDatabaseHandler.CONTACTS_SCORE);
                    int score = _score == null ? 0 : Integer.parseInt(_score);
                    if (score > 2)
                        frequents.put(contact, score);
                }
            }
            if (frequents.size() > 0) {
                items.add(new CaptionItem("3", activity.getString(R.string.frequentContacts)));
                items.add(new CheckItem(this, "4", activity.getString(R.string.sendAllFrequents), true, false, true, true, false));
                frequents = LocalDatabaseHandler.reverseSortByValue(frequents);
                for (int i = 0; i < (frequents.size() > 3 ? 3 : frequents.size()); i++) {
                    Map<String, String> contact = (Map<String, String>) frequents.keySet().toArray()[i];
                    String name = contact.get(LocalDatabaseHandler.CONTACT_NAME);
                    name = name == null ? contact.get(LocalDatabaseHandler.EPID) : name;
                    int x = Integer.MAX_VALUE - Integer.parseInt(contact.get(LocalDatabaseHandler.CONTACTS_SCORE));
                    items.add(new CheckItem(this, "5" + x, name, Long.parseLong(contact.get(LocalDatabaseHandler.UID))));
                }
            }
            if (sp.getBoolean(LocalDatabaseHandler.PERMISSION_CONTACTS, true)) {
                ArrayList<Map<String, String>> phone_contacts = ldb.getAll(LocalDatabaseHandler.PHONE_CONTACTS);
                if (phone_contacts.size() > 0) {
                    items.add(new CaptionItem("C", activity.getString(R.string.phoneContacts)));
                    for (Map<String, String> contact : phone_contacts)
                        items.add(new CheckItem(this, "D", contact.get(LocalDatabaseHandler.CONTACT_NAME), ID_PHONES));
//                    if (sp.getBoolean(LocalDatabaseHandler.TOTAL_STRANGERS, true) && remoteConfig.getBoolean("ts")) {
//                        items.add(new SeperatorItem("E"));
//                        items.add(new CheckItem(this, "F", activity.getString(R.string.sendOtherTS), ID_STRANGER, false, false, false, false, false));
//                    }
                }
            }
        } else {
            TextItem ti = new TextItem("1", activity.getResources().getQuantityString(R.plurals.nOfFollowers, sp.getInt(LocalDatabaseHandler.FOLLOWERS, 0), sp.getInt(LocalDatabaseHandler.FOLLOWERS, 0)));
            ti.offset = true;
            items.add(ti);
            ArrayList<Map<String, String>> contacts = ldb.getAll(LocalDatabaseHandler.FOLLOWING);
            ArrayList<String> cs = ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID);
            for (Map<String, String> contact : new ArrayList<>(contacts))
                if (cs.contains(contact.get(LocalDatabaseHandler.UID)))
                    contacts.remove(contact);
            if (contacts.size() > 0) {
                items.add(new CaptionItem("2", activity.getString(R.string.following)));
                for (Map<String, String> contact : contacts)
                    items.add(new FollowingItem(this, "3", contact.get(LocalDatabaseHandler.UID), contact.get(LocalDatabaseHandler.EPID)));
            }
            items.add(new ButtonItem("4", ButtonItem.FOLLOW, new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    follow();
                }
            }, activity));
            Map<Map<String, String>, Integer> suggested = new HashMap<>();
            for (Map<String, String> sugContact : ldb.getAll(LocalDatabaseHandler.SUGGESTED_FOLLOWING))
                suggested.put(sugContact, Integer.parseInt(sugContact.get(LocalDatabaseHandler.CONTACTS_SCORE)));
            if (suggested.size() > 0) {
                items.add(new CaptionItem("5", activity.getString(R.string.whoToFollow)));
                items.add(new TextItem("6", activity.getString(R.string.clickToFollow)));
                suggested = LocalDatabaseHandler.sortByValue(suggested);
                for (int i = 0; i < (suggested.size() > 10 ? 10 : suggested.size()); i++) {
                    Map<String, String> contact = (Map<String, String>) suggested.keySet().toArray()[i];
                    int x = Integer.MAX_VALUE - Integer.parseInt(contact.get(LocalDatabaseHandler.CONTACTS_SCORE));
                    items.add(new SuggestionItem(this, "7" + x, contact.get(LocalDatabaseHandler.UID), contact.get(LocalDatabaseHandler.EPID)));
                }
            }
        }
        Collections.sort(items);
        notifyDataSetChanged();
    }

    void addepid() {
        AlertDialog.Builder adb = new AlertDialog.Builder(activity);
        View view = View.inflate(activity, R.layout.d_enterepid, null);
        final EditText et = (EditText) view.findViewById(R.id.et);
        final View loading = view.findViewById(R.id.loading);
        final FontTextView tv = (FontTextView) view.findViewById(R.id.tv);
        final View display = view.findViewById(R.id.display);
        final CheckBox cb = (CheckBox) view.findViewById(R.id.cb);
        final AlertDialog dlg = adb.setTitle(R.string.addContact).setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (mode == CONTACTS_ACIVITY) {
                            ldb.inup(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, _profile.getId().toString(),
                                    LocalDatabaseHandler.EPID, _profile.getEpid());
                            if (ldb.getAll(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID).contains(_profile.getId().toString()))
                                ldb.delete(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, _profile.getId().toString());
                            new EndpointsHandler(activity).newContact(null, _profile.getId().toString());
                            ArrayList<String> suggested = ldb.getAll(LocalDatabaseHandler.SUGGESTED_CONTACTS, LocalDatabaseHandler.UID);
                            if (suggested.contains(_profile.getId().toString())) {
                                ldb.delete(LocalDatabaseHandler.SUGGESTED_CONTACTS, LocalDatabaseHandler.UID, _profile.getId().toString());
                                for (ContactsItem item : new ArrayList<>(items))
                                    if (item.rank.startsWith("2") ||
                                            item.rank.startsWith("7") && item instanceof SuggestionItem && ((SuggestionItem) item).uid.equals(_profile.getId().toString()) ||
                                            suggested.size() == 1 && (item.rank.startsWith("5") || item.rank.startsWith("6")))
                                        items.remove(item);
                            }
                            items.add(new PersonItem("3", _profile.getId(), _profile.getEpid(), "", true, ContactsAdapter.this));
                            Bundle ab = new Bundle();
                            ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                            ab.putString(FIIDService.RESULT, FIIDService.OK);
                            ab.putString(FIIDService.FROM, FIIDService.CONTACTS_ACT);
                            ab.putString(FIIDService.TEMP, Boolean.toString(false));
                            FirebaseAnalytics.getInstance(activity).logEvent(FIIDService.ADD_EPID, ab);
                        } else if (mode == SEND_PEOPLE) {
                            ldb.inup(cb.isChecked() || addToContactsNow ? LocalDatabaseHandler.CONTACTS : LocalDatabaseHandler.TEMP_CONTACTS,
                                    LocalDatabaseHandler.UID, _profile.getId().toString(),
                                    LocalDatabaseHandler.EPID, _profile.getEpid());
                            if ((cb.isChecked() || addToContactsNow) && ldb.getAll(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID).contains(_profile.getId().toString()))
                                ldb.delete(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, _profile.getId().toString());
                            if (cb.isChecked() || addToContactsNow)
                                new EndpointsHandler(activity).newContact(null, _profile.getId().toString());
                            if (addToContactsNow) {
                                ArrayList<ContactsItem> _items = new ArrayList<>(items);
                                for (ContactsItem ci : _items)
                                    if (ci instanceof CheckItem && ((CheckItem) ci).id == _profile.getId())
                                        items.remove(ci);
                            }
                            CheckItem item = new CheckItem(ContactsAdapter.this, cb.isChecked() || addToContactsNow ? "8" : "A", _profile.getEpid(), _profile.getId());
                            items.add(item);
                            boolean add = false, sep = false, cap = false, all = false;
                            for (ContactsItem ci : items) {
                                if (ci.rank.startsWith("A"))
                                    add = true;
                                else if (ci.rank.startsWith("9"))
                                    sep = true;
                                else if (ci.rank.startsWith("6"))
                                    cap = true;
                                else if (ci.rank.startsWith("7"))
                                    all = true;
                            }
                            if (add && !sep)
                                items.add(new SeperatorItem("9"));
                            if (cb.isChecked()) {
                                if (!cap)
                                    items.add(new CaptionItem("6", activity.getString(R.string.allContacts)));
                                if (!all)
                                    items.add(new CheckItem(ContactsAdapter.this, "7", activity.getString(R.string.sendAllContacts), true, false, true, true, false));
                            }
                            item.setChecked(true, true, true);
                            Bundle ab = new Bundle();
                            ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                            ab.putString(FIIDService.RESULT, FIIDService.OK);
                            ab.putString(FIIDService.FROM, FIIDService.SEND_PEOPLE);
                            ab.putString(FIIDService.TEMP, Boolean.toString(!cb.isChecked() && !addToContactsNow));
                            FirebaseAnalytics.getInstance(activity).logEvent(FIIDService.ADD_EPID, ab);
                        }
                        Collections.sort(items);
                        notifyDataSetChanged();
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Bundle ab = new Bundle();
                        ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                        ab.putString(FIIDService.RESULT, FIIDService.CANCEL);
                        ab.putString(FIIDService.FROM, mode == CONTACTS_ACIVITY ? FIIDService.CONTACTS_ACT : FIIDService.SEND_PEOPLE);
                        FirebaseAnalytics.getInstance(activity).logEvent(FIIDService.ADD_EPID, ab);
                    }
                })
                .create();
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    display.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.VISIBLE);
                    addToContactsNow = false;
                    new EndpointsHandler(activity).getProfile(new EndpointsHandler.APIRequest() {
                        @Override
                        public void onResult(Object result) {
                            boolean again = false, contact = false;
                            _profile = (Profile) result;
                            if (_profile != null && sp.getLong(LocalDatabaseHandler.UID, 0L) == _profile.getId())
                                _profile = null;
                            if (mode == CONTACTS_ACIVITY)
                                again = contact = _profile != null && ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID).contains(_profile.getId().toString());
                            else if (mode == SEND_PEOPLE) {
                                if (_profile != null)
                                    for (ContactsItem ci : items)
                                        if (ci instanceof CheckItem && ((CheckItem) ci).id == _profile.getId()) {
                                            again = true;
                                            break;
                                        }
                                contact = again && ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID).contains(_profile.getId().toString());
                                cb.setVisibility(_profile == null || again ? View.GONE : View.VISIBLE);
                            }
                            loading.setVisibility(View.INVISIBLE);
                            tv.setText(_profile == null ? activity.getString(R.string.noMatchFound) : again ? contact ? activity.getString(R.string.alreadyContact) : activity.getString(R.string.alreadyAdded) : _profile.getEpid());
                            tv.setStyle(_profile == null || again ? FontTextView.NORMAL : FontTextView.BOLD);
                            addToContactsNow = again && !contact;
                            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(_profile != null && !again || addToContactsNow);
                            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setText(addToContactsNow ? activity.getString(R.string.addToContacts) : activity.getString(android.R.string.ok));
                        }
                    }, EndpointsHandler.PR_EPID, s.toString());
                }
            }
        });
        if (dlg.getWindow() != null)
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    void follow() {
        AlertDialog.Builder adb = new AlertDialog.Builder(activity);
        View view = View.inflate(activity, R.layout.d_enterepid, null);
        final EditText et = (EditText) view.findViewById(R.id.et);
        final View loading = view.findViewById(R.id.loading);
        final FontTextView tv = (FontTextView) view.findViewById(R.id.tv);
        final View display = view.findViewById(R.id.display);
        final AlertDialog dlg = adb.setTitle(R.string.follow).setView(view)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        ldb.inup(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, _profile.getId().toString(),
                                LocalDatabaseHandler.EPID, _profile.getEpid());
                        new EndpointsHandler(activity).follow(null, _profile.getId().toString());
                        for (ContactsItem item : new ArrayList<>(items))
                            if (item.rank.startsWith("2")) {
                                items.remove(item);
                                break;
                            }
                        items.add(new FollowingItem(ContactsAdapter.this, "3", _profile.getId().toString(), _profile.getEpid()));
                        items.add(new CaptionItem("2", activity.getString(R.string.following)));
                        ArrayList<String> suggested = ldb.getAll(LocalDatabaseHandler.SUGGESTED_FOLLOWING, LocalDatabaseHandler.UID);
                        if (suggested.contains(_profile.getId().toString())) {
                            ldb.delete(LocalDatabaseHandler.SUGGESTED_FOLLOWING, LocalDatabaseHandler.UID, _profile.getId().toString());
                            for (ContactsItem item : new ArrayList<>(items))
                                if (item.rank.startsWith("7") && item instanceof SuggestionItem && ((SuggestionItem) item).uid.equals(_profile.getId().toString()) ||
                                        suggested.size() == 1 && (item.rank.startsWith("5") || item.rank.startsWith("6")))
                                    items.remove(item);
                        }
                        Collections.sort(items);
                        notifyDataSetChanged();
                        Bundle ab = new Bundle();
                        ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                        ab.putString(FIIDService.RESULT, FIIDService.OK);
                        FirebaseAnalytics.getInstance(activity).logEvent(FIIDService.FOLLOW, ab);
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        Bundle ab = new Bundle();
                        ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                        ab.putString(FIIDService.RESULT, FIIDService.CANCEL);
                        FirebaseAnalytics.getInstance(activity).logEvent(FIIDService.FOLLOW, ab);
                    }
                }).create();
        et.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (s.length() > 0) {
                    display.setVisibility(View.VISIBLE);
                    loading.setVisibility(View.VISIBLE);
                    new EndpointsHandler(activity).getProfile(new EndpointsHandler.APIRequest() {
                        @Override
                        public void onResult(Object result) {
                            _profile = (Profile) result;
                            if (_profile != null && sp.getLong(LocalDatabaseHandler.UID, 0L) == _profile.getId())
                                _profile = null;
                            boolean contact = _profile != null && (ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID).contains(_profile.getId().toString()) || ldb.getAll(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID).contains(_profile.getId().toString()));
                            loading.setVisibility(View.INVISIBLE);
                            tv.setText(_profile == null ? activity.getString(R.string.noMatchFound) : contact ? activity.getString(R.string.alreadyFollowing) : _profile.getEpid());
                            tv.setStyle(_profile == null || contact ? FontTextView.NORMAL : FontTextView.BOLD);
                            dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(_profile != null && !contact);
                        }
                    }, EndpointsHandler.PR_EPID, s.toString());
                }
            }
        });
        if (dlg.getWindow() != null)
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dlg.show();
        dlg.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
    }

    void sendPotato() {
        ArrayList<String> uids = new ArrayList<>();
        for (ContactsItem ci : items)
            if (ci.selected)
                uids.add(Long.toString(((PersonItem) ci).id));
        Intent i = new Intent(activity, AdActivity.class);
        i.putExtra(SendPotatoActivity.UIDS, uids);
        ArrayList<String> tss = new ArrayList<>();
        for (String ignored : uids)
            tss.add(Boolean.toString(false));
        i.putExtra(SendPotatoActivity.TSS, tss);
        activity.startActivity(i);
        activity.finish();
    }

    void invite(String name) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.invitation));
        sendIntent.setType("text/plain");
        activity.startActivity(Intent.createChooser(sendIntent, activity.getString(R.string.inviteX, name)));
        FirebaseAnalytics.getInstance(activity).logEvent(FIIDService.INVITE_CONTACT, null);
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return viewType == ContactsItem.TYPE_PERSON ? PersonItem.freshViewHolder(parent)
                : viewType == ContactsItem.TYPE_BUTTON ? ButtonItem.freshViewHolder(parent)
                : viewType == ContactsItem.TYPE_CAPTION ? CaptionItem.freshViewHolder(parent)
                : viewType == ContactsItem.TYPE_TEXT ? TextItem.freshViewHolder(parent)
                : viewType == ContactsItem.TYPE_SUGGESTION ? SuggestionItem.freshViewHolder(parent)
                : viewType == ContactsItem.TYPE_PHONE ? PhoneItem.freshViewHolder(parent)
                : viewType == ContactsItem.TYPE_FOLLOWING ? FollowingItem.freshViewHolder(parent)
                : viewType == ContactsItem.TYPE_POTATO ? PotatoItem.freshViewHolder(parent)
                : viewType == ContactsItem.TYPE_CHECK ? CheckItem.freshViewHolder(parent)
                : viewType == ContactsItem.TYPE_SEPERATOR ? SeperatorItem.freshViewHolder(parent)
                : null;
    }

    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        if (position > 0 && items.get(position - 1).type == ContactsItem.TYPE_PERSON) {
            ((PersonItem) items.get(position - 1)).seperator = items.get(position).type == ContactsItem.TYPE_PERSON || items.get(position).type == ContactsItem.TYPE_BUTTON;
            if (items.get(position - 1).holder != null)
                items.get(position - 1).bindViewHolder(items.get(position - 1).holder);
        } else if (position > 0 && items.get(position - 1).type == ContactsItem.TYPE_SUGGESTION) {
            ((SuggestionItem) items.get(position - 1)).seperator = items.get(position).type == ContactsItem.TYPE_SUGGESTION || items.get(position).type == ContactsItem.TYPE_BUTTON;
            if (items.get(position - 1).holder != null)
                items.get(position - 1).bindViewHolder(items.get(position - 1).holder);
        } else if (position > 0 && items.get(position - 1).type == ContactsItem.TYPE_FOLLOWING) {
            ((FollowingItem) items.get(position - 1)).seperator = items.get(position).type == ContactsItem.TYPE_FOLLOWING || items.get(position).type == ContactsItem.TYPE_BUTTON;
            if (items.get(position - 1).holder != null)
                items.get(position - 1).bindViewHolder(items.get(position - 1).holder);
        } else if (position > 0 && items.get(position - 1).type == ContactsItem.TYPE_CHECK) {
            ((CheckItem) items.get(position - 1)).seperator = items.get(position).type == ContactsItem.TYPE_CHECK || items.get(position).type == ContactsItem.TYPE_BUTTON;
            if (items.get(position - 1).holder != null)
                items.get(position - 1).bindViewHolder(items.get(position - 1).holder);
        }
        items.get(position).bindViewHolder(holder);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private static class ContactsItem implements Comparable<ContactsItem> {

        static final int TYPE_PERSON = 0;
        static final int TYPE_PHONE = 1;
        static final int TYPE_BUTTON = 2;
        static final int TYPE_CAPTION = 3;
        static final int TYPE_TEXT = 4;
        static final int TYPE_SUGGESTION = 5;
        static final int TYPE_FOLLOWING = 6;
        static final int TYPE_POTATO = 7;
        static final int TYPE_CHECK = 8;
        static final int TYPE_SEPERATOR = 9;

        String rank;
        int type;
        boolean selected = false;

        RecyclerView.ViewHolder holder;

        ContactsItem() {
        }

        @Override
        public int compareTo(@NonNull ContactsItem ci) {
            return rank.compareTo(ci.rank);
        }

        void setRank(String _rank) {
            rank = _rank;
        }

        void bindViewHolder(RecyclerView.ViewHolder holder) {
            this.holder = holder;
        }
    }

    private static class PersonItem extends ContactsItem {

        ContactsAdapter adapter;

        String text;
        String subtext;
        boolean seperator = true;
        boolean editable = false;
        long id = ID_NULL;

        PersonItem(String rank, long _id, String _text, String _subtext, boolean _editable, ContactsAdapter _adapter) {
            super();
            type = TYPE_PERSON;
            id = _id;
            setRank(rank + _text);
            text = _text;
            subtext = _subtext;
            editable = _editable;
            adapter = _adapter;
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_person, parent, false);
            return new ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((ViewHolder) holder).bind(this);
        }

        void sendPotato() {
            Intent i = new Intent(adapter.activity, AdActivity.class);
            i.putExtra(SendPotatoActivity.UIDS, new ArrayList<>(Collections.singletonList(Long.toString(id))));
            i.putExtra(SendPotatoActivity.TSS, new ArrayList<>(Collections.singletonList(Boolean.toString(false))));
            adapter.activity.startActivity(i);
            adapter.activity.finish();
        }

        void delete() {
            new AlertDialog.Builder(adapter.activity).setTitle(R.string.deleteContact).setMessage(R.string.deleteContactNotice)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new EndpointsHandler(adapter.activity).deleteContact(null, Long.toString(id));
                            adapter.ldb.inup(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, Long.toString(id),
                                    LocalDatabaseHandler.EPID, adapter.ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, Long.toString(id), LocalDatabaseHandler.EPID));
                            adapter.ldb.delete(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, Long.toString(id));
                            adapter.items.remove(PersonItem.this);
                            Collections.sort(adapter.items);
                            adapter.notifyDataSetChanged();
                            FirebaseAnalytics.getInstance(adapter.activity).logEvent(FIIDService.DELETE_CONTACT, null);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }).show();
        }

        void rename() {
            View v = View.inflate(adapter.activity, R.layout.d_contacts_rename, null);
            final EditText nameet = (EditText) v.findViewById(R.id.name);
            final boolean hasname = !subtext.equals("");
            nameet.setText(hasname ? text : "");
            new AlertDialog.Builder(adapter.activity).setTitle(R.string.changeName).setView(v)
                    .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            subtext = hasname ? subtext : text;
                            text = nameet.getText().toString();
                            adapter.ldb.inup(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, Long.toString(id),
                                    LocalDatabaseHandler.CONTACT_NAME, text);
                            Collections.sort(adapter.items);
                            adapter.notifyDataSetChanged();
                            Bundle ab = new Bundle();
                            ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                            ab.putString(FIIDService.RESULT, FIIDService.OK);
                            FirebaseAnalytics.getInstance(adapter.activity).logEvent(FIIDService.RENAME_CONTACT, ab);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        @Override
                        public void onCancel(DialogInterface dialog) {
                            Bundle ab = new Bundle();
                            ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                            ab.putString(FIIDService.RESULT, FIIDService.CANCEL);
                            FirebaseAnalytics.getInstance(adapter.activity).logEvent(FIIDService.RENAME_CONTACT, ab);
                        }
                    }).show();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            PersonItem pi;

            FontTextView tv;
            FontTextView stv;
            View sv;
            View container;

            Drawable bg;
            View.OnClickListener onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (pi.adapter.selection > 0) {
                        toggleSelection();
                    } else if (pi.adapter.activity != null) {
                        new AlertDialog.Builder(pi.adapter.activity)
                                .setItems(pi.editable ? new String[]{pi.adapter.activity.getString(R.string.sendEP), pi.adapter.activity.getString(R.string.changeName), pi.adapter.activity.getString(R.string.deleteContact)} : new String[]{pi.adapter.activity.getString(R.string.sendEP)}, new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        switch (which) {
                                            case 0:
                                                pi.sendPotato();
                                                break;
                                            case 1:
                                                pi.rename();
                                                break;
                                            case 2:
                                                pi.delete();
                                                break;
                                        }
                                        dialog.dismiss();
                                    }
                                }).show();
                    }
                }
            };
            View.OnLongClickListener onLongClick = new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    toggleSelection();
                    return true;
                }
            };

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void bind(PersonItem pi) {
                this.pi = pi;
                tv = (FontTextView) view.findViewById(R.id.text);
                stv = (FontTextView) view.findViewById(R.id.subtext);
                sv = view.findViewById(R.id.seperator);
                container = view.findViewById(R.id.container);
                tv.setText(pi.text);
                stv.setText(pi.subtext);
                stv.setVisibility(pi.subtext.equals("") ? View.GONE : View.VISIBLE);
                sv.setVisibility(pi.seperator ? View.VISIBLE : View.GONE);
                container.setOnClickListener(onClick);
                container.setOnLongClickListener(onLongClick);
                bg = container.getBackground();
            }

            void toggleSelection() {
                pi.selected = !pi.selected;
                pi.adapter.selection += pi.selected ? 1 : -1;
                if (pi.adapter.mode == CONTACTS_ACIVITY) {
                    if (pi.adapter.actionMode == null) {
                        pi.adapter.actionMode = pi.adapter.activity.startSupportActionMode((ContactsActivity) pi.adapter.activity);
                        pi.adapter.actionMode.setTitle(Integer.toString(pi.adapter.selection));
                    } else {
                        if (pi.adapter.selection == 0)
                            pi.adapter.actionMode.finish();
                        else
                            pi.adapter.actionMode.setTitle(Integer.toString(pi.adapter.selection));
                    }
                }
                if (pi.selected)
                    container.setBackgroundColor(Color.parseColor("#10000000"));
                else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
                    container.setBackground(bg);
                else
                    container.setBackgroundDrawable(bg);
            }
        }
    }

    private static class ButtonItem extends ContactsItem {

        static final int ADD = 1;
        static final int FOLLOW = 2;

        String text;
        View.OnClickListener onclick;

        ButtonItem(String rank, int _type, View.OnClickListener _onclick, Context c) {
            super();
            type = TYPE_BUTTON;
            setRank(rank);
            text = _type == ADD ? c.getString(R.string.plusContact) : c.getString(R.string.plusFollow);
            onclick = _onclick;
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_button, parent, false);
            return new ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((ViewHolder) holder).bind(this);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            ButtonItem bi;

            FontTextView tv;

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void bind(ButtonItem bi) {
                this.bi = bi;
                tv = (FontTextView) view.findViewById(R.id.button);
                tv.setText(bi.text);
                view.setOnClickListener(bi.onclick);
            }
        }
    }

    private static class CaptionItem extends ContactsItem {

        String text;

        CaptionItem(String rank, String _text) {
            super();
            type = TYPE_CAPTION;
            setRank(rank + _text);
            text = _text;
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_caption, parent, false);
            return new ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((ViewHolder) holder).bind(this);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            CaptionItem ci;

            TextView tv;

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void bind(CaptionItem ci) {
                this.ci = ci;
                tv = (TextView) view.findViewById(R.id.tv);
                tv.setText(ci.text);
            }
        }
    }

    private static class TextItem extends ContactsItem {

        String text;
        boolean offset = false;

        TextItem(String rank, String _text) {
            super();
            type = TYPE_TEXT;
            setRank(rank + _text);
            text = _text;
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_text, parent, false);
            return new ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((ViewHolder) holder).bind(this);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            TextItem ti;

            TextView tv;

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void bind(TextItem ti) {
                this.ti = ti;
                tv = (TextView) view.findViewById(R.id.tv);
                tv.setText(ti.text);
                view.findViewById(R.id.offset).setVisibility(ti.offset ? View.VISIBLE : View.GONE);
            }
        }
    }

    private static class SuggestionItem extends ContactsItem {

        ContactsAdapter adapter;

        boolean seperator = true;

        String uid;
        String text;

        SuggestionItem(ContactsAdapter adapter, String rank, String _uid, String _text) {
            super();
            this.adapter = adapter;
            type = TYPE_SUGGESTION;
            setRank(rank + _text);
            uid = _uid;
            text = _text;
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_simple, parent, false);
            return new ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((ViewHolder) holder).bind(this);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            SuggestionItem si;

            View container;
            TextView tv;
            View.OnClickListener onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (si.adapter.mode == CONTACTS_ACIVITY) {
                        si.adapter.ldb.inup(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, si.uid,
                                LocalDatabaseHandler.EPID, si.text);
                        si.adapter.ldb.delete(LocalDatabaseHandler.SUGGESTED_CONTACTS, LocalDatabaseHandler.UID, si.uid);
                        int ssize = si.adapter.ldb.getAll(LocalDatabaseHandler.SUGGESTED_CONTACTS, LocalDatabaseHandler.ID).size();
                        for (ContactsItem item : new ArrayList<>(si.adapter.items))
                            if (item.rank.startsWith("2") ||
                                    item.rank.startsWith("7") && item instanceof SuggestionItem && ((SuggestionItem) item).uid.equals(si.uid) ||
                                    ssize == 0 && (item.rank.startsWith("5") || item.rank.startsWith("6")))
                                si.adapter.items.remove(item);
                        si.adapter.items.add(new PersonItem("3", Long.parseLong(si.uid), si.text, "", true, si.adapter));
                        new EndpointsHandler(si.adapter.activity).newContact(null, si.uid);
                    } else {
                        si.adapter.ldb.inup(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, si.uid,
                                LocalDatabaseHandler.EPID, si.text);
                        si.adapter.ldb.delete(LocalDatabaseHandler.SUGGESTED_FOLLOWING, LocalDatabaseHandler.UID, si.uid);
                        int ssize = si.adapter.ldb.getAll(LocalDatabaseHandler.SUGGESTED_FOLLOWING, LocalDatabaseHandler.ID).size();
                        for (ContactsItem item : new ArrayList<>(si.adapter.items))
                            if (item.rank.startsWith("2") ||
                                    item.rank.startsWith("7") && item instanceof SuggestionItem && ((SuggestionItem) item).uid.equals(si.uid) ||
                                    ssize == 0 && (item.rank.startsWith("5") || item.rank.startsWith("6")))
                                si.adapter.items.remove(item);
                        si.adapter.items.add(new CaptionItem("2", si.adapter.activity.getString(R.string.following)));
                        si.adapter.items.add(new FollowingItem(si.adapter, "3", si.uid, si.text));
                        new EndpointsHandler(si.adapter.activity).follow(null, si.uid);
                    }
                    Collections.sort(si.adapter.items);
                    si.adapter.notifyDataSetChanged();
                }
            };

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void bind(SuggestionItem si) {
                this.si = si;
                tv = (TextView) view.findViewById(R.id.text);
                tv.setText(si.text);
                container = view.findViewById(R.id.container);
                container.setOnClickListener(onClick);
                view.findViewById(R.id.seperator).setVisibility(si.seperator ? View.VISIBLE : View.GONE);
            }
        }
    }

    private static class PhoneItem extends ContactsItem {

        ContactsAdapter adapter;

        String text;

        PhoneItem(ContactsAdapter adapter, String rank, String _text) {
            super();
            this.adapter = adapter;
            type = TYPE_PHONE;
            setRank(rank + _text);
            text = _text;
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_simple, parent, false);
            return new ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((ViewHolder) holder).bind(this);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            PhoneItem pi;

            View container;
            TextView tv;
            View.OnClickListener onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    pi.adapter.invite(pi.text);
                }
            };

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void bind(PhoneItem pi) {
                this.pi = pi;
                tv = (TextView) view.findViewById(R.id.text);
                tv.setText(pi.text);
                container = view.findViewById(R.id.container);
                container.setOnClickListener(onClick);
            }
        }
    }

    private static class FollowingItem extends ContactsItem {

        ContactsAdapter adapter;

        boolean seperator = true;

        String uid;
        String text;

        FollowingItem(ContactsAdapter adapter, String rank, String _uid, String _text) {
            super();
            this.adapter = adapter;
            type = TYPE_FOLLOWING;
            setRank(rank + _text);
            uid = _uid;
            text = _text;
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_simple, parent, false);
            return new ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((ViewHolder) holder).bind(this);
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            FollowingItem fi;

            View container;
            TextView tv;
            View.OnClickListener onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(fi.adapter.activity).setItems(new String[]{fi.adapter.activity.getString(R.string.unfollow)}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new EndpointsHandler(fi.adapter.activity).unfollow(null, fi.uid);
                            fi.adapter.ldb.inup(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, fi.uid, LocalDatabaseHandler.EPID, fi.text);
                            fi.adapter.ldb.delete(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, fi.uid);
                            fi.adapter.items.remove(fi);
                            boolean fitems = false;
                            for (ContactsItem item : fi.adapter.items)
                                if (item.rank.startsWith("3")) {
                                    fitems = true;
                                    break;
                                }
                            if (!fitems)
                                for (ContactsItem item : new ArrayList<>(fi.adapter.items))
                                    if (item.rank.startsWith("2") || item.rank.startsWith("4"))
                                        fi.adapter.items.remove(item);
                            Collections.sort(fi.adapter.items);
                            fi.adapter.notifyDataSetChanged();
                            FirebaseAnalytics.getInstance(fi.adapter.activity).logEvent(FIIDService.UNFOLLOW, null);
                            dialog.dismiss();
                        }
                    }).show();
                }
            };

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void bind(FollowingItem fi) {
                this.fi = fi;
                tv = (TextView) view.findViewById(R.id.text);
                tv.setText(fi.text);
                container = view.findViewById(R.id.container);
                container.setOnClickListener(onClick);
                view.findViewById(R.id.seperator).setVisibility(fi.seperator ? View.VISIBLE : View.GONE);
            }
        }
    }

    private static class PotatoItem extends ContactsItem {

        int form;
        String text;
        SendPeopleActivity activity;

        PotatoItem(int _form, String _text, SendPeopleActivity act) {
            super();
            type = TYPE_POTATO;
            setRank("0");
            form = _form;
            text = _text;
            activity = act;
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_potato, parent, false);
            return new PotatoItem.ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((PotatoItem.ViewHolder) holder).bind(this);
            activity.pv = ((ViewHolder) holder).potato;
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            PotatoItem pi;

            PotatoView potato;

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void bind(PotatoItem pi) {
                this.pi = pi;
                if (potato == null || !potato.getText().equals(pi.text) || potato.getForm() != pi.form) {
                    potato = (PotatoView) view.findViewById(R.id.potato);
                    potato.setup(pi.form, pi.text, null);
                }
            }
        }
    }

    static class CheckItem extends ContactsItem {

        ContactsAdapter adapter;

        String text;
        boolean bold;
        boolean checked;
        boolean seperator;
        boolean isall;
        boolean hasall;

        int position;

        long id = ID_NULL;

        CheckItem(ContactsAdapter _adapter, String rank, String _text, long _id) {
            this(_adapter, rank, _text, _id, false, false, true, false, true);
        }

        CheckItem(ContactsAdapter _adapter, String rank, String _text, boolean _bold, boolean _checked, boolean _seperator, boolean _isall, boolean _hasall) {
            this(_adapter, rank, _text, ID_NULL, _bold, _checked, _seperator, _isall, _hasall);
        }

        CheckItem(ContactsAdapter _adapter, String rank, String _text, long _id, boolean _bold, boolean _checked, boolean _seperator, boolean _isall, boolean _hasall) {
            super();
            adapter = _adapter;
            position = adapter.items.size();
            type = TYPE_CHECK;
            id = _id;
            if (id > 0)
                adapter.entanglements.put(id, adapter.entanglements.get(id) != null ? new Integer[]{adapter.entanglements.get(id)[0], position} : new Integer[]{position});
            setRank(rank + _text);
            text = _text;
            bold = _bold;
            checked = _checked;
            seperator = _seperator;
            isall = _isall;
            hasall = _hasall;
            adapter.checkItems.put(position, this);
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_check, parent, false);
            return new CheckItem.ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((CheckItem.ViewHolder) holder).bind(this);
        }

        void setChecked(boolean _checked, boolean dothething, boolean dotheotherthing) {
            checked = _checked;
            if (holder != null && ((CheckItem.ViewHolder) holder).ci.position == position)
                ((CheckItem.ViewHolder) holder).setChecked(checked);
            if (dothething) {
                if (isall) {
                    for (int i = 0; i < adapter.checkItems.size(); i++)
                        if (RANKING.indexOf(adapter.checkItems.get(adapter.checkItems.keyAt(i)).rank.charAt(0)) == RANKING.indexOf(rank.charAt(0)) + 1)
                            adapter.checkItems.get(adapter.checkItems.keyAt(i)).setChecked(_checked, false, true);
                } else if (hasall) {
                    CheckItem all = null;
                    boolean ischecked = checked;
                    for (int i = 0; i < adapter.checkItems.size(); i++) {
                        if (RANKING.indexOf(adapter.checkItems.get(adapter.checkItems.keyAt(i)).rank.charAt(0)) == RANKING.indexOf(rank.charAt(0)) - 1)
                            all = adapter.checkItems.get(adapter.checkItems.keyAt(i));
                        else if (RANKING.indexOf(adapter.checkItems.get(adapter.checkItems.keyAt(i)).rank.charAt(0)) == RANKING.indexOf(rank.charAt(0)))
                            ischecked &= adapter.checkItems.get(adapter.checkItems.keyAt(i)).checked;
                    }
                    if (all != null)
                        all.setChecked(ischecked, false, true);
                }
            }
            if (dotheotherthing && id > 0 && adapter.entanglements.get(id).length > 1)
                adapter.checkItems.get(adapter.entanglements.get(id)[0] == position ? adapter.entanglements.get(id)[1] : adapter.entanglements.get(id)[0]).setChecked(checked, true, false);
            if (rank.startsWith("1")) {
                ArrayList<ContactsItem> cis = new ArrayList<>(adapter.items);
                for (ContactsItem ci : cis)
                    if (ci.rank.startsWith("E") || ci.rank.startsWith("F"))
                        adapter.items.remove(ci);
                if (checked && adapter.sp.getBoolean(LocalDatabaseHandler.PERMISSION_CONTACTS, true) && adapter.ldb.getAll(LocalDatabaseHandler.PHONE_CONTACTS).size() > 0) {
                    adapter.items.add(new SeperatorItem("E"));
                    adapter.items.add(new CheckItem(adapter, "F", adapter.activity.getString(R.string.sendOtherTS), ID_STRANGER, false, false, false, false, false));
                }
            }
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            CheckItem ci;

            FontTextView tv;
            CheckBox cb;
            View sv;
            View container;

            boolean checked;
            View.OnClickListener onClick = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ci.adapter.checkItems.get(ci.position).setChecked(!checked, true, true);
                }
            };

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void setChecked(boolean _checked) {
                checked = _checked;
                if (cb != null)
                    cb.setChecked(checked);
            }

            void bind(CheckItem ci) {
                this.ci = ci;
                checked = ci.adapter.checkItems.get(ci.position) != null ? ci.adapter.checkItems.get(ci.position).checked : ci.checked;
                tv = (FontTextView) view.findViewById(R.id.text);
                cb = (CheckBox) view.findViewById(R.id.box);
                sv = view.findViewById(R.id.seperator);
                container = view.findViewById(R.id.container);
                tv.setText(ci.text);
                tv.setStyle(ci.bold ? FontTextView.BOLD : FontTextView.NORMAL);
                cb.setChecked(checked);
                cb.setOnClickListener(onClick);
                sv.setVisibility(ci.seperator ? View.VISIBLE : View.GONE);
                container.setOnClickListener(onClick);
            }
        }

    }

    private static class SeperatorItem extends ContactsItem {

        SeperatorItem(String rank) {
            super();
            type = TYPE_SEPERATOR;
            setRank(rank);
        }

        static RecyclerView.ViewHolder freshViewHolder(ViewGroup parent) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.v_people_seperator, parent, false);
            return new SeperatorItem.ViewHolder(v);
        }

        @Override
        void bindViewHolder(RecyclerView.ViewHolder holder) {
            super.bindViewHolder(holder);
            ((SeperatorItem.ViewHolder) holder).bind();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            View view;

            ViewHolder(View itemView) {
                super(itemView);
                view = itemView;
            }

            void bind() {
            }

        }

    }
}
