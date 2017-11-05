package com.onnoeberhard.potato;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Profile;
import com.onnoeberhard.epotato.backend.potatoAPI.model.ProfileCollection;

import net.rimoto.intlphoneinput.IntlPhoneInput;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainService extends JobService {

    static final String TAG = "EP_MAIN_SERVICE";
    private static int status = 0;
    private static ContactRequest cr = null;

    static void loadContacts(final Context c, final ContactsAdapter ca, ContactRequest request) {
        cr = request;
        status = 0;
        final View loading = ca != null ? ca.loading : null;
        final View content = ca != null ? ca.content : null;
        if (loading != null && content != null) {
            content.setVisibility(View.GONE);
            loading.setVisibility(View.VISIBLE);
        }
        final LocalDatabaseHandler ldb = new LocalDatabaseHandler(c);
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(c);
        final Map<String, String> numbernames = new HashMap<>();
        if (ContextCompat.checkSelfPermission(c, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            Cursor phones = c.getContentResolver().query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
            IntlPhoneInput ipi = new IntlPhoneInput(c);
            while (phones != null && phones.moveToNext()) {
                String name = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String number;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN)
                    number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NORMALIZED_NUMBER));
                else {
                    number = phones.getString(phones.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    ipi.setNumber(number);
                    if (ipi.isValid())
                        number = ipi.getNumber();
                }
                if (number != null && !numbernames.containsKey(number))
                    numbernames.put(number, name);
            }
            if (phones != null)
                phones.close();
        }
        // Get current contacts list from datastore
        final ArrayList<String> contactIds = ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID);
        new EndpointsHandler(c).getContacts(new EndpointsHandler.APIRequest() {
            @Override
            public void onResult(Object result) {
                ArrayList<String> used_names = new ArrayList<>();
                if (result != null && ((ProfileCollection) result).getItems() != null) {
                    // Delete all expired contacts from ldb
                    for (String contactId : contactIds) {
                        boolean keep = false;
                        for (Profile p : ((ProfileCollection) result).getItems())
                            if (p.getId() != null && p.getId().toString().equals(contactId)) {
                                keep = true;
                                break;
                            }
                        if (!keep) {
                            ldb.inup(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, contactId,
                                    LocalDatabaseHandler.EPID, ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.ID, contactId, LocalDatabaseHandler.EPID));
                            ldb.delete(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, contactId);
                        }
                    }
                    // Delete phone number & name from existing linked contacts -> in case of new phone number: keep contact, but not linked
                    for (Map<String, String> contact : ldb.getAll(LocalDatabaseHandler.CONTACTS))
                        if (contact.get(LocalDatabaseHandler.CONTACT_PHONE) != null)
                            ldb.inup(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.ID, contact.get(LocalDatabaseHandler.ID),
                                    LocalDatabaseHandler.CONTACT_NAME, null,
                                    LocalDatabaseHandler.CONTACT_PHONE, null);
                    // Add all new contacts to ldb
                    boolean newNotification = false;
                    ArrayList<String> newContacts = new ArrayList<>();
                    ArrayList<String> newContactIds = new ArrayList<>();
                    for (Profile p : ((ProfileCollection) result).getItems()) {
                        if (!contactIds.contains(p.getId().toString()) && p.getPhone() != null && numbernames.keySet().contains(p.getPhone())) {
                            newContacts.add(numbernames.get(p.getPhone()));
                            newContactIds.add(p.getId().toString());
                            newNotification = newNotification || !ldb.getAll(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID).contains(p.getId().toString());
                        }
                        if (!contactIds.contains(p.getId().toString()))
                            ldb.inup(LocalDatabaseHandler.CONTACTS,
                                    LocalDatabaseHandler.UID, p.getId().toString(),
                                    LocalDatabaseHandler.EPID, p.getEpid());
                        if (p.getPhone() != null && numbernames.keySet().contains(p.getPhone())) {
                            ldb.inup(LocalDatabaseHandler.CONTACTS,
                                    LocalDatabaseHandler.UID, p.getId().toString(),
                                    LocalDatabaseHandler.EPID, p.getEpid(),
                                    LocalDatabaseHandler.CONTACT_NAME, numbernames.get(p.getPhone()),
                                    LocalDatabaseHandler.CONTACT_PHONE, p.getPhone());
                            used_names.add(numbernames.get(p.getPhone()));
                        }
                    }
                    if (newContacts.size() > 0) {
                        Bundle ab = new Bundle();
                        ab.putInt(FIIDService.RESULT, newContacts.size());
                        FirebaseAnalytics.getInstance(c).logEvent(FIIDService.NEW_PHONE_USER, ab);
                    }
                    // Send notification about new phone contacts
                    if (newNotification && ca == null && sp.contains(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB)) {
                        String names = newContacts.get(0);
                        for (int i = 0; i < newContacts.size() - 1; i++)
                            names += ", " + newContacts.get(i);
                        Intent resultIntent = new Intent(c, MainActivity.class);
                        resultIntent.putStringArrayListExtra("sendto", newContactIds);
                        PendingIntent resultPendingIntent = PendingIntent.getActivity(c, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                        ((NotificationManager) c.getSystemService(Context.NOTIFICATION_SERVICE)).notify(2, new NotificationCompat.Builder(c)
                                .setSmallIcon(R.drawable.noticon)
                                .setColor(ContextCompat.getColor(c, R.color.colorPrimary))
                                .setAutoCancel(true)
                                .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                                .setPriority(NotificationCompat.PRIORITY_HIGH)
                                .setContentTitle(newContacts.size() == 1 ? names + c.getString(R.string.xNowUses) : Integer.toString(newContacts.size()) + c.getString(R.string.yNowUse))
                                .setContentText(newContacts.size() == 1 ? c.getString(R.string.sayHello) : c.getString(R.string.sayHelloTo, names))
                                .setContentIntent(resultPendingIntent).build());
                    }
                    // Delete all contacts that are now in real contacts from phone contacts list
                    ldb.truncate(LocalDatabaseHandler.PHONE_CONTACTS);
                    for (Map.Entry<String, String> numbername : numbernames.entrySet())
                        if (!used_names.contains(numbername.getValue()) && !ldb.getAll(LocalDatabaseHandler.PHONE_CONTACTS, LocalDatabaseHandler.CONTACT_NAME).contains(numbername.getValue()))
                            ldb.insert(LocalDatabaseHandler.PHONE_CONTACTS,
                                    LocalDatabaseHandler.CONTACT_NAME, numbername.getValue(),
                                    LocalDatabaseHandler.CONTACT_PHONE, numbername.getKey());
                }
                doStatus(ca, loading, content);
                if (!sp.contains(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB))
                    sp.edit().putBoolean(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB, true).apply();
            }
        }, new ArrayList<>(numbernames.keySet()));
        // Update Followers, Following
        new EndpointsHandler(c).getFollowing(new EndpointsHandler.APIRequest() {
            @Override
            public void onResult(Object result) {
                if (result != null && ((ProfileCollection) result).getItems() != null && ((ProfileCollection) result).getItems().size() > 0) {
                    ldb.truncate(LocalDatabaseHandler.FOLLOWING);
                    for (Profile p : ((ProfileCollection) result).getItems())
                        ldb.inup(LocalDatabaseHandler.FOLLOWING,
                                LocalDatabaseHandler.UID, p.getId().toString(),
                                LocalDatabaseHandler.EPID, p.getEpid());
                }
                doStatus(ca, loading, content);
            }
        });
//        // Get & update suggested contacts    --commented because I don't really see the use in suggesting contacts _and_ followers.. (I also don't really understand the difference, so this helps distinguishing :)
//        new EndpointsHandler(c).contactSuggestions(new EndpointsHandler.APIRequest() {
//            @Override
//            public void onResult(Object result) {
//                if (result != null && ((ProfileCollection) result).getItems() != null && ((ProfileCollection) result).getItems().size() > 0) {
//                    ldb.truncate(LocalDatabaseHandler.SUGGESTED_CONTACTS);
//                    Map<Profile, Integer> suggestions = new HashMap<>();
//                    for (Profile p : ((ProfileCollection) result).getItems())
//                        if (p != null)
//                            if (suggestions.keySet().contains(p))
//                                suggestions.put(p, suggestions.get(p) + 1);
//                            else suggestions.put(p, 1);
//                    for (Map.Entry<Profile, Integer> entry : suggestions.entrySet())
//                        if (entry.getKey().getId() != sp.getLong(LocalDatabaseHandler.UID, 0L))
//                            ldb.insert(LocalDatabaseHandler.SUGGESTED_CONTACTS,
//                                    LocalDatabaseHandler.UID, entry.getKey().getId().toString(),
//                                    LocalDatabaseHandler.EPID, entry.getKey().getEpid(),
//                                    LocalDatabaseHandler.CONTACTS_SCORE, entry.getValue().toString());
//                }
//                doStatus(ca, loading, content);
//            }
//        });
        // Get & update suggested following
        new EndpointsHandler(c).followSuggestions(new EndpointsHandler.APIRequest() {
            @Override
            public void onResult(Object result) {
                if (result != null && ((ProfileCollection) result).getItems() != null && ((ProfileCollection) result).getItems().size() > 0) {
                    ldb.truncate(LocalDatabaseHandler.SUGGESTED_FOLLOWING);
                    Map<Profile, Integer> suggestions = new HashMap<>();
                    for (Profile p : ((ProfileCollection) result).getItems())
                        if (p != null)
                            if (suggestions.keySet().contains(p))
                                suggestions.put(p, suggestions.get(p) + 1);
                            else suggestions.put(p, 1);
                    for (Map.Entry<Profile, Integer> entry : suggestions.entrySet())
                        if (entry.getKey().getId() != sp.getLong(LocalDatabaseHandler.UID, 0L))
                            ldb.insert(LocalDatabaseHandler.SUGGESTED_FOLLOWING,
                                    LocalDatabaseHandler.UID, entry.getKey().getId().toString(),
                                    LocalDatabaseHandler.EPID, entry.getKey().getEpid(),
                                    LocalDatabaseHandler.CONTACTS_SCORE, entry.getValue().toString());
                }
                doStatus(ca, loading, content);
            }
        });
        // Delete unused temp contacts
        ArrayList<String> puids = ldb.getAll(LocalDatabaseHandler.RECEIVED_POTATOES, LocalDatabaseHandler.UID);
        for (String uids : ldb.getAll(LocalDatabaseHandler.SENT_POTATOES, LocalDatabaseHandler.SENT_POTATOES_UIDS))
            puids.addAll(LocalDatabaseHandler.deserialize(uids));
        ArrayList<String> contacts = ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID);
        for (Map<String, String> user : ldb.getAll(LocalDatabaseHandler.TEMP_CONTACTS))
            if (!puids.contains(user.get(LocalDatabaseHandler.UID)) || contacts.contains(user.get(LocalDatabaseHandler.UID)) || user.get(LocalDatabaseHandler.EPID) == null || user.get(LocalDatabaseHandler.EPID).equals(""))
                ldb.delete(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, user.get(LocalDatabaseHandler.UID));
        //Get all used but nonexisting contacts from datastore
        ArrayList<String> everybody = new ArrayList<>(contacts);
        everybody.addAll(ldb.getAll(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID));
        for (String puid : puids)
            if (!everybody.contains(puid))
                new EndpointsHandler(c).getProfile(new EndpointsHandler.APIRequest() {
                    @Override
                    public void onResult(Object result) {
                        if (result != null) {
                            Profile p = (Profile) result;
                            ldb.insert(LocalDatabaseHandler.TEMP_CONTACTS,
                                    LocalDatabaseHandler.UID, p.getId().toString(),
                                    LocalDatabaseHandler.EPID, p.getEpid());
                        }
                    }
                }, EndpointsHandler.ID, puid);
        FIIDService.updateFIEPID(c, null);
    }

    private static void doStatus(ContactsAdapter ca, View load, View content) {
        if (status == 2) {
            if (cr != null)
                cr.onResult();
            if (ca != null) {
                ca.populate();
                if (load != null && content != null) {
                    load.setVisibility(View.GONE);
                    content.setVisibility(View.VISIBLE);
                }
            }
        } else status++;
    }

    @Override
    public boolean onStartJob(JobParameters job) {
        loadContacts(this, null, null);
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters job) {
        return true;
    }

    interface ContactRequest {
        void onResult();
    }

}