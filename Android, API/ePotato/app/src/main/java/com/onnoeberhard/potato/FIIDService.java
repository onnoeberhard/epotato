package com.onnoeberhard.potato;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Potato;
import com.onnoeberhard.epotato.backend.potatoAPI.model.PotatoCollection;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Profile;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FIIDService extends FirebaseInstanceIdService {

    static final String POTATO_RECEIVED = "potato_received";
    static final String FROM_CONTACT = "from_contact";
    static final String FEED = "feed";
    static final String NEW_PHONE_USER = "new_phone_user";

    static final String SENT = "send";
    static final String CHANGE_FORM = "change_form";
    static final String SAVE = "save";
    static final String CONTACTS_PERMISSION = "contacts_permission";
    static final String PEOPLE_RELOAD = "people_reload";
    static final String DIRECT = "direct";
    static final String PEOPLE = "people";
    static final String FORM = "form";
    static final String TEMPORARY = "temporary";
    static final String TS = "ts";
    static final String PHONES = "phone";
    static final String PHONES_INVITE = "phones_invite";
    static final String PHONES_SKIP = "phones_skip";
    static final String PHONES_IMAGE = "phones_image";
    static final String NULL = "null";
    static final String REPLY = "replying";
    static final String SEND_BUTTON = "SendButton";
    static final String HOME_FRAGMENT = "HomeFragment";

    static final String AD = "ad";

    static final String RESULT = "result";

    static final String OK = "OK";
    static final String ALLOW = "ALLOW";
    static final String DENY = "DENY";
    static final String CANCEL = "CANCEL";
    static final String FAIL = "FAIL";

    static final String FROM = "from";

    static final String CHANGE_EPID = "change_epid";
    static final String CONNECT_PHONE = "connect_phone";
    static final String CHANGE_PASSWORD = "change_password";
    static final String LOGOUT = "log_out";
    static final String DELETE_ACCOUNT = "delete_account";
    static final String LEAVE_REVIEW = "leave_review";
    static final String REMOVE_ADS = "remove_ads";

    static final String ADD_TO_CONTACTS = "add_to_contacts";
    static final String SEND_AGAIN = "send_again";
    static final String UNFOLLOW = "unfollow";
    static final String DELETE_POTATO = "delete_potato";

    static final String ADD_EPID = "add_epid";
    static final String SEND_PEOPLE = "SendPeople";
    static final String CONTACTS_ACT = "ContactsActivity";
    static final String TEMP = "temp";

    static final String FOLLOW = "follow";

    static final String DELETE_CONTACT = "delete_contact";
    static final String RENAME_CONTACT = "rename_contact";
    static final String INVITE_CONTACT = "invite_contact";

    static final String LOGIN = "log_in";
    static final String RECOVER = "recover";
    static final String FORGOT_PASSWORD = "forgot_password";
    static final String LOGIN_NEW_USER = "log_in_new_user";
    static final String SIGNUP = "signup";
    static final String SIGNUP_PHONE = "phone";

    static void updateFIEPID(final Context c, final FIEPIDRequest request) {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        new EndpointsHandler(c).getProfile(new EndpointsHandler.APIRequest() {
            @Override
            public void onResult(Object result) {
                if (result != null) {
                    Profile p = (Profile) result;
                    sp.edit().putString(LocalDatabaseHandler.EPID, p.getEpid())
                            .putString(LocalDatabaseHandler.PHONE, p.getPhone() == null || p.getPhone().equals(EndpointsHandler.NULL) ? null : p.getPhone())
                            .putInt(LocalDatabaseHandler.FOLLOWERS, p.getFollowers() != null ? p.getFollowers().size() : 0)
                            .putBoolean(LocalDatabaseHandler.TOTAL_STRANGERS, p.getStrangers()).apply();
                    String fiid = FirebaseInstanceId.getInstance().getToken();
                    if (p.getFiids() == null || !p.getFiids().contains(fiid)) {
                        final ArrayList<String> fiids = p.getFiids() == null ? new ArrayList<String>() : (ArrayList<String>) p.getFiids();
                        fiids.remove(sp.getString(LocalDatabaseHandler.FIID, ""));
                        sp.edit().putString(LocalDatabaseHandler.FIID, fiid).apply();
                        fiids.add(fiid);
                        new EndpointsHandler(c).update(null, EndpointsHandler.PROFILE, EndpointsHandler.ID, p.getId().toString(),
                                new ArrayList<>(Collections.singletonList(EndpointsHandler.PR_FIIDS)), new ArrayList<>(Collections.singletonList(LocalDatabaseHandler.serialize(fiids))));
                    }
                    if (p.getPotatoes() != null && p.getPotatoes().size() > 0) {
                        new EndpointsHandler(c).getPotatoes(new EndpointsHandler.APIRequest() {
                            @Override
                            public void onResult(Object result) {
                                if (result != null && ((PotatoCollection) result).getItems() != null) {
                                    List<Potato> ps = ((PotatoCollection) result).getItems();
                                    final LocalDatabaseHandler ldb = new LocalDatabaseHandler(c);
                                    ArrayList<String> pids = ldb.getAll(LocalDatabaseHandler.RECEIVED_POTATOES, LocalDatabaseHandler.ID);
                                    for (Potato p : ps) {
                                        if (!pids.contains(p.getPid())) {
                                            String message = p.getMessage();
                                            try {
                                                String iv = message.substring(message.length() - 16);
                                                message = new CryptLib().decryptSimple(message.substring(0, message.length() - 16), c.getString(R.string.key), iv);
                                            } catch (Exception e) {
                                                e.printStackTrace();
                                            }
                                            ldb.insert(LocalDatabaseHandler.RECEIVED_POTATOES,
                                                    LocalDatabaseHandler.ID, p.getPid(),
                                                    LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                                                    LocalDatabaseHandler.UID, p.getUid(),
                                                    LocalDatabaseHandler.RECEIVED_POTATOES_TS, Boolean.toString(false),
                                                    LocalDatabaseHandler.POTATO_TEXT, message,
                                                    LocalDatabaseHandler.POTATO_FORM, Integer.toString(p.getForm()));
                                            if (ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, p.getUid(), LocalDatabaseHandler.ID) == null &&
                                                    ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, p.getUid(), LocalDatabaseHandler.ID) == null) {
                                                new EndpointsHandler(c).getProfile(new EndpointsHandler.APIRequest() {
                                                    @Override
                                                    public void onResult(Object result) {
                                                        if (result != null) {
                                                            Profile p = (Profile) result;
                                                            ldb.insert(LocalDatabaseHandler.TEMP_CONTACTS,
                                                                    LocalDatabaseHandler.UID, p.getId().toString(),
                                                                    LocalDatabaseHandler.EPID, p.getEpid());
                                                            if (ContextCompat.checkSelfPermission(c, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED)
                                                                MainService.loadContacts(c, null, null);
                                                        }
                                                    }
                                                }, EndpointsHandler.ID, p.getUid());
                                            }
                                        }
                                    }
                                }
                                if (request != null) request.onResult();
                            }
                        }, p.getId().toString());
                    } else if (request != null) request.onResult();
                } else if (request != null) request.onResult();
                final FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
                remoteConfig.fetch().addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        remoteConfig.activateFetched();
                    }
                });
                analytics(c);
            }
        }, EndpointsHandler.ID, Long.toString(sp.getLong(LocalDatabaseHandler.UID, 0L)));
    }

    static void analytics(Context c) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        LocalDatabaseHandler ldb = new LocalDatabaseHandler(c);
        FirebaseAnalytics analytics = FirebaseAnalytics.getInstance(c);
        analytics.setUserProperty(LocalDatabaseHandler.CONTACTS, Integer.toString(ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.ID).size()));
        analytics.setUserProperty(LocalDatabaseHandler.CREEPS, Boolean.toString(sp.getBoolean(LocalDatabaseHandler.CREEPS, true)));
        analytics.setUserProperty(LocalDatabaseHandler.FEED_NOTIFICATIONS, Boolean.toString(sp.getBoolean(LocalDatabaseHandler.FEED_NOTIFICATIONS, true)));
        analytics.setUserProperty(LocalDatabaseHandler.FOLLOWERS, Integer.toString(sp.getInt(LocalDatabaseHandler.FOLLOWERS, 0)));
        analytics.setUserProperty(LocalDatabaseHandler.FOLLOWING, Integer.toString(ldb.getAll(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.ID).size()));
        ArrayList<String> contacts = ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.CONTACTS_SCORE);
        //noinspection SuspiciousMethodCalls
        contacts.removeAll(Collections.singleton(null));
        Collections.sort(contacts);
        analytics.setUserProperty(LocalDatabaseHandler.HI_SCORE, contacts.size() > 0 ? contacts.get(0) : "0");
        analytics.setUserProperty(LocalDatabaseHandler.PHONE, Boolean.toString(sp.getString(LocalDatabaseHandler.PHONE, null) != null));
        analytics.setUserProperty(LocalDatabaseHandler.TOTAL_STRANGERS, Boolean.toString(sp.getBoolean(LocalDatabaseHandler.TOTAL_STRANGERS, true)));
    }

    @Override
    public void onTokenRefresh() {
        updateFIEPID(this, null);
    }

    interface FIEPIDRequest {
        void onResult();
    }
}
