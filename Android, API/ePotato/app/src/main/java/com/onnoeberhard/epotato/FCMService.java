package com.onnoeberhard.epotato;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Profile;

import java.util.ArrayList;
import java.util.Map;

import me.leolin.shortcutbadger.ShortcutBadger;

public class FCMService extends FirebaseMessagingService {

    private LocalDatabaseHandler ldb;
    private SharedPreferences sp;

    @Override
    public void onMessageReceived(RemoteMessage msg) {
        ldb = new LocalDatabaseHandler(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        final Map<String, String> data = msg.getData();
        boolean a_fromContact = false;
        boolean a_feed = false;
        boolean a_success = false;
        if (data != null && data.containsKey("type") && data.get("type").equals(Integer.toString(EndpointsHandler.TYPE_POTATO))
                && (!Boolean.parseBoolean(data.get("ts")) || sp.getBoolean(LocalDatabaseHandler.TOTAL_STRANGERS, true))
                && (Boolean.parseBoolean(data.get("ts")) || !sp.getBoolean(LocalDatabaseHandler.CREEPS, false)
                || ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, data.get("uid"), LocalDatabaseHandler.ID) != null)
                && !ldb.getAll(LocalDatabaseHandler.RECEIVED_POTATOES, LocalDatabaseHandler.ID).contains(data.get("pid"))) {
            String message = data.get("message");
            try {
                String iv = message.substring(message.length() - 16);
                message = new CryptLib().decryptSimple(message.substring(0, message.length() - 16), getString(R.string.key), iv);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ldb.insert(LocalDatabaseHandler.RECEIVED_POTATOES,
                    LocalDatabaseHandler.ID, data.get("pid"),
                    LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                    LocalDatabaseHandler.UID, data.get("uid"),
                    LocalDatabaseHandler.RECEIVED_POTATOES_TS, data.get("ts"),
                    LocalDatabaseHandler.POTATO_TEXT, message,
                    LocalDatabaseHandler.POTATO_FORM, data.get("form"));
            ldb.insert(LocalDatabaseHandler.NEW_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID, data.get("pid"));
            if (ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, data.get("uid"), LocalDatabaseHandler.ID) == null &&
                    ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, data.get("uid"), LocalDatabaseHandler.ID) == null) {
                new EndpointsHandler(this).getProfile(new EndpointsHandler.APIRequest() {
                    @Override
                    public void onResult(Object result) {
                        if (result != null) {
                            Profile p = (Profile) result;
                            ldb.insert(LocalDatabaseHandler.TEMP_CONTACTS,
                                    LocalDatabaseHandler.UID, p.getId().toString(),
                                    LocalDatabaseHandler.EPID, p.getEpid());
                            if (ContextCompat.checkSelfPermission(FCMService.this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                MainService.loadContacts(FCMService.this, null, new MainService.ContactRequest() {
                                    @Override
                                    public void onResult() {
                                        updatePotatoes();
                                    }
                                });
                            }
                        } else {
                            new EndpointsHandler(FCMService.this).getProfile(new EndpointsHandler.APIRequest() {
                                @Override
                                public void onResult(Object result) {
                                    if (result != null) {
                                        Profile p = (Profile) result;
                                        ldb.insert(LocalDatabaseHandler.TEMP_CONTACTS,
                                                LocalDatabaseHandler.UID, p.getId().toString(),
                                                LocalDatabaseHandler.EPID, p.getEpid());
                                    }
                                    updatePotatoes();
                                }
                            }, EndpointsHandler.ID, data.get("uid"));
                        }
                    }
                }, EndpointsHandler.ID, data.get("uid"));
                a_fromContact = false;
            } else
                updatePotatoes();
            a_success = true;
        } else if (data != null && data.containsKey("type") && data.get("type").equals(Integer.toString(EndpointsHandler.TYPE_FEED_POTATO)) &&
                !ldb.getAll(LocalDatabaseHandler.FEED_POTATOES, LocalDatabaseHandler.ID).contains(data.get("pid"))) {
            String message = data.get("message");
            try {
                String iv = message.substring(message.length() - 16);
                message = new CryptLib().decryptSimple(message.substring(0, message.length() - 16), getString(R.string.key), iv);
            } catch (Exception e) {
                e.printStackTrace();
            }
            ldb.insert(LocalDatabaseHandler.FEED_POTATOES,
                    LocalDatabaseHandler.ID, data.get("pid"),
                    LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                    LocalDatabaseHandler.UID, data.get("uid"),
                    LocalDatabaseHandler.POTATO_TEXT, message,
                    LocalDatabaseHandler.POTATO_FORM, data.get("form"));
            ldb.insert(LocalDatabaseHandler.NEW_FEED_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID, data.get("pid"));
            a_fromContact = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, data.get("uid"), LocalDatabaseHandler.ID) != null;
            if (ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, data.get("uid"), LocalDatabaseHandler.ID) == null &&
                    ldb.get(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, data.get("uid"), LocalDatabaseHandler.ID) == null &&
                    ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, data.get("uid"), LocalDatabaseHandler.ID) == null)
                new EndpointsHandler(this).getProfile(new EndpointsHandler.APIRequest() {
                    @Override
                    public void onResult(Object result) {
                        if (result != null) {
                            Profile p = (Profile) result;
                            ldb.insert(LocalDatabaseHandler.TEMP_CONTACTS,
                                    LocalDatabaseHandler.UID, p.getId().toString(),
                                    LocalDatabaseHandler.EPID, p.getEpid());
                            if (ContextCompat.checkSelfPermission(FCMService.this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                                MainService.loadContacts(FCMService.this, null, new MainService.ContactRequest() {
                                    @Override
                                    public void onResult() {
                                        updateFeedPotatoes();
                                    }
                                });
                            }
                        } else {
                            new EndpointsHandler(FCMService.this).getProfile(new EndpointsHandler.APIRequest() {
                                @Override
                                public void onResult(Object result) {
                                    if (result != null) {
                                        Profile p = (Profile) result;
                                        ldb.insert(LocalDatabaseHandler.TEMP_CONTACTS,
                                                LocalDatabaseHandler.UID, p.getId().toString(),
                                                LocalDatabaseHandler.EPID, p.getEpid());
                                    }
                                    updateFeedPotatoes();
                                }
                            }, EndpointsHandler.ID, data.get("uid"));
                        }
                    }
                }, EndpointsHandler.ID, data.get("uid"));
            else
                updateFeedPotatoes();
            a_feed = true;
            a_success = true;
        }
        Bundle ab = new Bundle();
        ab.putLong(FirebaseAnalytics.Param.VALUE, a_success ? 1 : 0);
        ab.putString(FIIDService.RESULT, a_success ? FIIDService.OK : FIIDService.FAIL);
        ab.putString(FIIDService.FROM_CONTACT, Boolean.toString(a_fromContact));
        ab.putString(FIIDService.FEED, Boolean.toString(a_feed));
        FirebaseAnalytics.getInstance(this).logEvent(FIIDService.POTATO_RECEIVED, ab);
    }

    private void updatePotatoes() {
        sendBroadcast(new Intent(MainActivity.POTATO_RECEIVED));
        if (!MainActivity.isVisible) {
            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.noticon)
                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH);
            String title = "";
            String text = getString(R.string.potatoReceived);
            ArrayList<String> pids = ldb.getAll(LocalDatabaseHandler.NEW_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID);
            if (pids.size() > 1) {
                ArrayList<String> uids = new ArrayList<>();
                for (String pid : pids) {
                    String uid = ldb.get(LocalDatabaseHandler.RECEIVED_POTATOES, LocalDatabaseHandler.ID, pid, LocalDatabaseHandler.UID);
                    if (!uids.contains(uid))
                        uids.add(uid);
                }
                for (int i = 0; i < uids.size(); i++) {
                    String name = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uids.get(i), LocalDatabaseHandler.CONTACT_NAME);
                    name = name == null ? ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uids.get(i), LocalDatabaseHandler.EPID) : name;
                    name = name == null ? ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, uids.get(i), LocalDatabaseHandler.EPID) : name;
                    name = name == null ? "" : name;
                    title = !name.equals("") ? title.equals("") ? name : title + ", " + name : title;
                }
                title = title.equals("") ? getString(R.string.app_name) : title;
                text = getResources().getQuantityString(R.plurals.receivedPotatoes, ldb.getAll(LocalDatabaseHandler.NEW_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID).size(), ldb.getAll(LocalDatabaseHandler.NEW_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID).size());
            } else {
                String uid = ldb.get(LocalDatabaseHandler.RECEIVED_POTATOES, LocalDatabaseHandler.ID, ldb.getAll(LocalDatabaseHandler.NEW_POTATOES, LocalDatabaseHandler.NEW_POTATOES_PID).get(0), LocalDatabaseHandler.UID);
                String name = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.CONTACT_NAME);
                name = name == null ? ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID) : name;
                name = name == null ? ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.EPID) : name;
                title = name == null ? title : name;
            }
            mBuilder.setContentTitle(title).setContentText(text);
            Intent resultIntent = new Intent(this, MainActivity.class);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            mBuilder.setContentIntent(resultPendingIntent);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(0, mBuilder.build());
            ShortcutBadger.applyCount(this, pids.size());
        }
    }

    private void updateFeedPotatoes() {
        sendBroadcast(new Intent(MainActivity.POTATO_FEED));
        if (!MainActivity.isVisible && sp.getBoolean(LocalDatabaseHandler.FEED_NOTIFICATIONS, true)) {
            Intent resultIntent = new Intent(this, MainActivity.class);
            resultIntent.putExtra("feed", true);
            PendingIntent resultPendingIntent = PendingIntent.getActivity(this, 0, resultIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).notify(1, new NotificationCompat.Builder(this)
                    .setSmallIcon(R.drawable.noticon)
                    .setColor(ContextCompat.getColor(this, R.color.colorPrimary))
                    .setAutoCancel(true)
                    .setDefaults(Notification.DEFAULT_SOUND | Notification.DEFAULT_VIBRATE | Notification.DEFAULT_LIGHTS)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentTitle(getString(R.string.app_name))
                    .setContentText(getString(R.string.newFeedPotatoes))
                    .setContentIntent(resultPendingIntent).build());
        }
    }
}
