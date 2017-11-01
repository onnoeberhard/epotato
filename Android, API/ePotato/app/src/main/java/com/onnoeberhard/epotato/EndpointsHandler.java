package com.onnoeberhard.epotato;

import android.content.Context;
import android.os.AsyncTask;
import android.preference.PreferenceManager;

import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.api.client.extensions.android.json.AndroidJsonFactory;
import com.onnoeberhard.epotato.backend.potatoAPI.PotatoAPI;

import java.io.IOException;
import java.util.ArrayList;

import static com.onnoeberhard.epotato.LocalDatabaseHandler.deserialize;
import static com.onnoeberhard.epotato.LocalDatabaseHandler.serialize;

@SuppressWarnings("unused")
class EndpointsHandler extends AsyncTask<String, Void, Object> {

    static final String ID = "id";

    static final String PROFILE = "profile";
    static final String PR_CONTACTS = "contacts";
    static final String PR_EPID = "epid";
    static final String PR_FIIDS = "fiids";
    static final String PR_FIIDS_IOS = "fiids_ios";
    static final String PR_FOLLOWERS = "followers";
    static final String PR_FOLLOWING = "following";
    static final String PR_PASSWORD = "password";
    static final String PR_PHONE = "phone";
    static final String PR_RAND = "rand";
    static final String PR_STRANGERS = "strangers";
    static final String PO_DT = "dt";
    static final String PO_FORM = "form";
    static final String PO_N = "n";
    static final String PO_MESSAGE = "message";
    static final String PO_PID = "pid";
    static final String PO_UID = "uid";
    static final int TYPE_POTATO = 1;
    static final int TYPE_FEED_POTATO = 2;
    private static final String CHECK = "check";
    private static final String GET_PROFILE = "getProfile";
    private static final String GET_POTATOES = "getPotatoes";
    private static final String UPDATE = "update";
    private static final String SMS = "sms";
    private static final String SIGNUP = "signup";
    private static final String LOGIN = "login";
    private static final String RECOVER_PASSWORD = "recoverPassword";
    private static final String CHANGE_PASSWORD = "changePassword";
    private static final String DELETE_ACCOUNT = "deleteAccount";
    private static final String SEND_POTATO = "sendPotato";
    private static final String GET_TS = "getTS";
    private static final String NEW_CONTACT = "newContact";
    private static final String DELETE_CONTACT = "deleteContact";
    private static final String GET_CONTACTS = "getContacts";
    private static final String CONTACT_SUGGESTIONS = "contact_suggestions";
    private static final String FOLLOW_SUGGESTIONS = "follow_suggestions";
    private static final String GET_FOLLOWING = "getFollowing";
    private static final String FOLLOW = "follow";
    private static final String UNFOLLOW = "unfollow";
    static int NOTICE_OK = 1;
    static int NOTICE_ERROR = 2;
    static int NOTICE_OTHER = 3;
    static int NOTICE_NULL = 4;
    static String NULL = "*NULL*";
    private static PotatoAPI api = null;
    private Context context;

    private LocalDatabaseHandler ldb;
    private APIRequest request;

    EndpointsHandler(Context context) {
        this.context = context;
        ldb = new LocalDatabaseHandler(context);
    }

    void check(APIRequest request, String kind, String key, String value) {
        this.request = request;
        execute(CHECK, kind, key, value);
    }

    void getProfile(APIRequest request, String key, String value) {
        this.request = request;
        execute(GET_PROFILE, key, value);
    }

    void getPotatoes(APIRequest request, String uid) {
        this.request = request;
        execute(GET_POTATOES, uid);
    }

    void update(APIRequest request, String kind, String property, String value, ArrayList<String> properties, ArrayList<String> values) {
        this.request = request;
        String p = serialize(properties);
        String v = serialize(values);
        execute(UPDATE, kind, property, value, p, v);
    }

    void sms(APIRequest request, String number, String body) {
        this.request = request;
        execute(SMS, number, body);
    }

    void signup(APIRequest request, String epid, String password, String phone) {
        this.request = request;
        execute(SIGNUP, epid, password, phone);
    }

    void login(APIRequest request, String epid, String password) {
        this.request = request;
        execute(LOGIN, epid, password);
    }

    void recoverPassword(APIRequest request, String epid) {
        this.request = request;
        execute(RECOVER_PASSWORD, epid);
    }

    void changePassword(APIRequest request, String epid, String npw, String opw) {
        this.request = request;
        execute(CHANGE_PASSWORD, epid, npw, opw);
    }

    void deleteAccount(APIRequest request, String epid, String pw) {
        this.request = request;
        execute(DELETE_ACCOUNT, epid, pw);
    }

    void sendPotato(APIRequest request, ArrayList<String> uids, ArrayList<String> ts, String message, int type, String pid) {
        this.request = request;
        try {
            String iv = CryptLib.generateRandomIV(16);
            message = new CryptLib().encryptSimple(message, context.getString(R.string.key), iv) + iv;
        } catch (Exception e) {
            e.printStackTrace();
        }
        execute(SEND_POTATO, message, Integer.toString(type), pid, serialize(uids), serialize(ts));
    }

    void getTS(APIRequest request, int n) {
        this.request = request;
        ArrayList<String> contacts = ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID);
        contacts.addAll(ldb.getAll(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID));
        contacts.add(0, Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L)));
        execute(GET_TS, Integer.toString(n), serialize(contacts));
    }

    void newContact(APIRequest request, String uid) {
        this.request = request;
        execute(NEW_CONTACT, uid);
    }

    void deleteContact(APIRequest request, String uid) {
        this.request = request;
        execute(DELETE_CONTACT, uid);
    }

    void getContacts(APIRequest request, ArrayList<String> numbers) {
        this.request = request;
        execute(GET_CONTACTS, serialize(numbers));
    }

    void contactSuggestions(APIRequest request) {
        this.request = request;
        execute(CONTACT_SUGGESTIONS);
    }

    void followSuggestions(APIRequest request) {
        this.request = request;
        execute(FOLLOW_SUGGESTIONS);
    }

    void getFollowing(APIRequest request) {
        this.request = request;
        execute(GET_FOLLOWING);
    }

    void follow(APIRequest request, String uid) {
        this.request = request;
        execute(FOLLOW, uid);
    }

    void unfollow(APIRequest request, String uid) {
        this.request = request;
        execute(UNFOLLOW, uid);
    }

    @Override
    protected Object doInBackground(String... params) {
        api = api == null ? new PotatoAPI.Builder(AndroidHttp.newCompatibleTransport(), new AndroidJsonFactory(), null)
                .setRootUrl(context.getString(R.string.api_url)).build() : api;
        try {
            switch (params[0]) {
                case CHECK:
                    return api.check(params[1], params[2], params[3]).execute();
                case GET_PROFILE:
                    return api.getProfile(params[1], params[2]).execute();
                case GET_POTATOES:
                    return api.getPotatoes(params[1]).execute();
                case UPDATE:
                    return api.update(params[1], params[2], params[3], deserialize(params[4]), deserialize(params[5])).execute();
                case SMS:
                    return api.sms(params[1], params[2]).execute();
                case SIGNUP:
                    return api.signup(params[1], params[2], params[3]).execute();
                case LOGIN:
                    return api.login(params[1], params[2]).execute();
                case RECOVER_PASSWORD:
                    return api.recoverPassword(params[1]).execute();
                case CHANGE_PASSWORD:
                    return api.changePassword(params[1], params[2], params[3]).execute();
                case DELETE_ACCOUNT:
                    return api.deleteAccount(params[1], params[2]).execute();
                case SEND_POTATO:
                    ArrayList<Boolean> ts = new ArrayList<>();
                    for (String _sts : deserialize(params[5]))
                        ts.add(Boolean.parseBoolean(_sts));
                    return api.sendPotato(deserialize(params[4]), ts, Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L)), Integer.parseInt(params[2]), params[1], params[3]).execute();
                case GET_TS:
                    return api.getTS(Integer.parseInt(params[1]), deserialize(params[2])).execute();
                case NEW_CONTACT:
                    return api.newContact(Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L)), params[1]).execute();
                case DELETE_CONTACT:
                    return api.deleteContact(Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L)), params[1]).execute();
                case GET_CONTACTS:
                    return api.getContacts(Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L)), deserialize(params[1])).execute();
                case CONTACT_SUGGESTIONS:
                    return api.contactSuggestions(Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L))).execute();
                case FOLLOW_SUGGESTIONS:
                    return api.followSuggestions(Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L))).execute();
                case GET_FOLLOWING:
                    return api.getFollowing(Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L))).execute();
                case FOLLOW:
                    return api.follow(Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L)), params[1]).execute();
                case UNFOLLOW:
                    return api.unfollow(Long.toString(PreferenceManager.getDefaultSharedPreferences(context).getLong(LocalDatabaseHandler.UID, 0L)), params[1]).execute();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object result) {
        if (request != null)
            request.onResult(result);
        if (ldb != null)
            ldb.close();
    }

    interface APIRequest {
        void onResult(Object result);
    }
}