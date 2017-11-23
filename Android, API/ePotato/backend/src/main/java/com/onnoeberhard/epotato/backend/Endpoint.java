package com.onnoeberhard.epotato.backend;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiNamespace;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.repackaged.org.apache.commons.codec.binary.Base64;
import com.googlecode.objectify.Key;
import com.twilio.sdk.TwilioRestClient;
import com.twilio.sdk.TwilioRestException;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.TimeZone;

import javax.inject.Named;

import static com.googlecode.objectify.ObjectifyService.begin;
import static com.googlecode.objectify.ObjectifyService.ofy;
import static com.googlecode.objectify.ObjectifyService.register;
import static com.onnoeberhard.epotato.backend.Credentials.FCM_AUTH_KEY;
import static com.onnoeberhard.epotato.backend.Credentials.TWILIO_NUMBER;
import static com.onnoeberhard.epotato.backend.Credentials.TWILIO_PASSWORD;
import static com.onnoeberhard.epotato.backend.Credentials.TWILIO_USERNAME;
import static com.onnoeberhard.epotato.backend.Notice.ERROR;
import static com.onnoeberhard.epotato.backend.Notice.OTHER;
import static com.onnoeberhard.epotato.backend.Potato.TYPE_FEED_POTATO;
import static com.onnoeberhard.epotato.backend.Potato.TYPE_POTATO;

@Api(
        name = "potatoAPI",
        version = "v1",
        namespace = @ApiNamespace(
                ownerDomain = "backend.epotato.onnoeberhard.com",
                ownerName = "backend.epotato.onnoeberhard.com"
        ),
        description = "API for ePotato"
)
public class Endpoint {

    static final String ID = "id";
    static final String PR_CONTACTS = "contacts";
    static final String PR_EPID = "epid";
    static final String PR_FIIDS = "fiids";
    static final String PR_FIIDS_IOS = "fiids_ios";
    static final String PR_FOLLOWERS = "followers";
    static final String PR_FOLLOWING = "following";
    static final String PR_PASSWORD = "password";
    static final String PR_PHONE = "phone";
    static final String PR_POTATOES = "potatoes";
    static final String PR_RAND = "rand";
    static final String PR_STRANGERS = "strangers";
    static final String PO_DT = "dt";
    static final String PO_FORM = "form";
    static final String PO_N = "n";
    static final String PO_MESSAGE = "message";
    static final String PO_PID = "pid";
    static final String PO_UID = "uid";
    private static final String PROFILE = "profile";
    private static final String POTATO = "potato";
    private static final Map<String, Class> kinds;

    private static final String NULL = "*NULL*";

    static {
        Map<String, Class> kmap = new HashMap<>();
        kmap.put(PROFILE, Profile.class);
        kmap.put(POTATO, Potato.class);
        kinds = Collections.unmodifiableMap(kmap);
    }

    public Endpoint() {
        register(Profile.class);
        register(Potato.class);
    }

    private static String sha256(String input) {
        String result = input;
        if (input != null) {
            try {
                MessageDigest md = MessageDigest.getInstance("SHA-256");
                byte[] bytes = md.digest(input.getBytes());
                StringBuilder sb = new StringBuilder();
                for (byte aByte : bytes)
                    sb.append(Integer.toString((aByte & 0xff) + 0x100, 16).substring(1));
                result = sb.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
        }
        return result;
    }

    static String getTimestamp() {
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK).format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
    }

    static ArrayList<String> deserialize(String in) {
        ArrayList<String> a = new ArrayList<>();
        if (in != null && !in.equals("null") && in.length() > 0)
            try {
                a = (ArrayList<String>) new ObjectInputStream(new ByteArrayInputStream(Hex.decodeHex(in.toCharArray()))).readObject();
            } catch (IOException | ClassNotFoundException | DecoderException e) {
                e.printStackTrace();
            }
        return a;
    }

    @ApiMethod(name = "serialize", path = "serialize")
    public Notice serialize(@Named("input") ArrayList<String> input) {
        String s = "";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(input);
            s = new String(Hex.encodeHex(out.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Notice(s);
    }

    @ApiMethod(name = "check")
    public Id check(@Named("_kind") String _kind, @Named("idKey") String idKey, @Named("idValue") String idValue) {
        Entity result = get(_kind, idKey, idValue);
        return result != null ? result.toId() : null;
    }

    @ApiMethod(name = "checkAllOne", path = "checkAllOne")
    public ArrayList<Id> checkAllOne(@Named("kind") String kind, @Named("property") final String property, @Named("values") final ArrayList<String> values) {
        return checkAll(kind, new ArrayList<String>() {{
            for (int i = 0; i < values.size(); i++) add(property);
        }}, values);
    }

    @ApiMethod(name = "checkAll", path = "checkAll")
    public ArrayList<Id> checkAll(@Named("kind") String kind, @Named("properties") ArrayList<String> properties, @Named("values") ArrayList<String> values) {
        ArrayList<Id> result = new ArrayList<>();
        if (properties.size() != values.size()) {
            String[] p = new String[values.size()];
            Arrays.fill(p, properties.get(0));
            properties = new ArrayList<>(Arrays.asList(p));
        }
        for (int i = 0; i < properties.size(); i++)
            result.add(check(kind, properties.get(i), values.get(i)));
        return result;
    }

    private <T extends Entity> T get(String _kind, String idKey, String idValue) {
        Class<T> type = kinds.get(_kind);
        begin();
        if (idKey.equals(ID))
            return type.cast(ofy().load().type(kinds.get(_kind)).id(Long.parseLong(idValue)).now());
        else
            return type.cast(idKey.equals(NULL) ? ofy().load().type(kinds.get(_kind)) : ofy().load().type(kinds.get(_kind)).filter(idKey, idValue).first().now());
    }

    private <T extends Entity> ArrayList<T> getAll(String _kind, String idKey, String idValue) {
        Class<T> type = kinds.get(_kind);
        begin();
        ArrayList<T> array = new ArrayList<>();
        List<T> list = (idKey.equals(NULL) ? ofy().load().type(kinds.get(_kind)) : ofy().load().type(kinds.get(_kind)).filter(idKey, idValue)).list();
        for (T item : list)
            array.add(type.cast(item));
        return array;
    }

    private <T extends Entity> ArrayList<T> getAll(String kind) {
        return getAll(kind, NULL, NULL);
    }

    @ApiMethod(name = "getProfile")
    public Profile getProfile(@Named("idKey") String idKey, @Named("idValue") String idValue) {
        return get(PROFILE, idKey, idValue);
    }

    private Profile getProfile(@Named("id") String id) {
        Profile p = getProfile(PR_EPID, id);
        p = p == null ? getProfile(PR_PHONE, id) : p;
        try {
            p = p == null ? getProfile(ID, id) : p;
        } catch (NumberFormatException e) {
            p = null;
        }
        return p;
    }

    @ApiMethod(name = "getProfiles")
    public ArrayList<Profile> getProfiles(@Named("idKey") String idKey, @Named("idValue") String idValue) {
        return getAll(PROFILE, idKey, idValue);
    }

    @ApiMethod(name = "getPotatoes", path = "getPotatoes")
    public ArrayList<Potato> getPotatoes(@Named("uid") final String uid) {
        try {
            final Calendar ctoday = Calendar.getInstance();
            ctoday.setTime(new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK).parse(getTimestamp()));
            final Profile profile = getProfile(uid);
            ArrayList<Potato> potatoes = new ArrayList<>();
            for (String pid : profile.getPotatoes()) {
                Potato p = Endpoint.this.get(POTATO, PO_PID, pid);
                if (p != null) {
                    potatoes.add(p);
                    Calendar cthen = Calendar.getInstance();
                    cthen.setTime(new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK).parse(p.getDt()));
                    int n = p.getN() - 1;
                    if (n == 0 || cthen.get(Calendar.YEAR) == ctoday.get(Calendar.YEAR) && ctoday.get(Calendar.DAY_OF_YEAR) - cthen.get(Calendar.DAY_OF_YEAR) > 7 ||
                            cthen.get(Calendar.YEAR) < ctoday.get(Calendar.YEAR) && Math.abs(ctoday.get(Calendar.DAY_OF_YEAR) - cthen.get(Calendar.DAY_OF_YEAR) + cthen.getActualMaximum(Calendar.DAY_OF_YEAR)) > 7) {
                        begin();
                        ofy().delete().entity(p).now();
                    } else
                        Endpoint.this.update(POTATO, PO_PID, p.getPid(), new ArrayList<>(Collections.singletonList(PO_N)), new ArrayList<>(Collections.singletonList(Integer.toString(n))));
                }
            }
            Endpoint.this.update(PROFILE, ID, uid, new ArrayList<>(Collections.singletonList(PR_POTATOES)), new ArrayList<>(Collections.singletonList(serialize(new ArrayList<String>()).getMessage())));
            ArrayList<Potato> myps = getAll(POTATO, PO_UID, uid);
            for (Potato p : myps) {
                Calendar cthen = Calendar.getInstance();
                cthen.setTime(new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK).parse(p.getDt()));
                if (p.getN() == 0 || cthen.get(Calendar.YEAR) == ctoday.get(Calendar.YEAR) && ctoday.get(Calendar.DAY_OF_YEAR) - cthen.get(Calendar.DAY_OF_YEAR) > 7 ||
                        cthen.get(Calendar.YEAR) < ctoday.get(Calendar.YEAR) && Math.abs(ctoday.get(Calendar.DAY_OF_YEAR) - cthen.get(Calendar.DAY_OF_YEAR) + cthen.getActualMaximum(Calendar.DAY_OF_YEAR)) > 7) {
                    begin();
                    ofy().delete().entity(p).now();
                }
            }
            return potatoes;
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiMethod(name = "cleanPotatoes")
    public Notice cleanPotatoes() {
        try {
            ArrayList<Potato> potatoes = getAll(POTATO);
            ArrayList<Potato> obsolete = new ArrayList<>();
            final Calendar ctoday = Calendar.getInstance();
            ctoday.setTime(new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK).parse(getTimestamp()));
            for (Potato p : potatoes) {
                Calendar cthen = Calendar.getInstance();
                cthen.setTime(new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK).parse(p.getDt()));
                if (cthen.get(Calendar.YEAR) == ctoday.get(Calendar.YEAR) && ctoday.get(Calendar.DAY_OF_YEAR) - cthen.get(Calendar.DAY_OF_YEAR) > 7 ||
                        cthen.get(Calendar.YEAR) < ctoday.get(Calendar.YEAR) && Math.abs(ctoday.get(Calendar.DAY_OF_YEAR) - cthen.get(Calendar.DAY_OF_YEAR) + cthen.getActualMaximum(Calendar.DAY_OF_YEAR)) > 7) {
                    obsolete.add(p);
                }
            }
            begin();
            ofy().delete().entities(obsolete).now();
            return new Notice(true);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiMethod(name = "insert", path = "insert")
    public Id insert(@Named("kind") String kind, @Named("properties") ArrayList<String> properties, @Named("values") ArrayList<String> values) {
        begin();
        if (kind.equals(PROFILE)) {
            Profile profile = new Profile(properties, values);
            ofy().save().entity(profile).now();
            return profile.toId();
        } else if (kind.equals(POTATO)) {
            Potato potato = new Potato(properties, values);
            ofy().save().entity(potato).now();
            return potato.toId();
        }
        return null;
    }

    @ApiMethod(name = "update", path = "update")
    public Id update(@Named("_kind") String _kind, @Named("idKey") String idKey, @Named("idValue") String idValue, @Named("properties") ArrayList<String> properties, @Named("values") ArrayList<String> values) {
        begin();
        Entity e = get(_kind, idKey, idValue);
        if (e == null)
            return null;
        if (_kind.equals(PROFILE)) {
            Profile profile = (Profile) e;
            profile.updateProperties(properties, values);
            ofy().save().entity(profile).now();
            return profile.toId();
        } else if (_kind.equals(POTATO)) {
            Potato potato = (Potato) e;
            potato.updateProperties(properties, values);
            ofy().save().entity(potato).now();
            return potato.toId();
        }
        return null;
    }

    @ApiMethod(name = "sms")
    public Notice sms(@Named("number") String number, @Named("body") String body) {
        Notice n = new Notice(false);
        try {
            List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair("To", number));
            params.add(new BasicNameValuePair("From", TWILIO_NUMBER));
            params.add(new BasicNameValuePair("Body", body));
            new TwilioRestClient(TWILIO_USERNAME, TWILIO_PASSWORD).getAccount().getMessageFactory().create(params);
            n.setOk(true);
        } catch (TwilioRestException e) {
            e.printStackTrace();
            n.setMessage(e.toString());
        }
        return n;
    }

    @ApiMethod(name = "signup")
    public Id signup(@Named("epid") String epid, @Named("password") String password, @Named("phone") String phone) {
        byte[] s = new byte[36];
        new SecureRandom().nextBytes(s);
        String salt = sha256(Base64.encodeBase64String(s));
        String pw = sha256(password + salt) + salt;
        return phone.equals(NULL) ? insert(PROFILE, new ArrayList<>(Arrays.asList(PR_EPID, PR_PASSWORD)), new ArrayList<>(Arrays.asList(epid, pw))) :
                insert(PROFILE, new ArrayList<>(Arrays.asList(PR_EPID, PR_PASSWORD, PR_PHONE)), new ArrayList<>(Arrays.asList(epid, pw, phone)));
    }

    @ApiMethod(name = "login")
    public Notice login(@Named("epid") String epid, @Named("password") String password) {
        Profile p = getProfile(epid);
        if (p != null) {
            if (sha256(password + p.getPassword().substring(64, 128)).equals(p.getPassword().substring(0, 64))) {
                if (p.getPassword().length() != 128) {
                    p.setPassword(p.getPassword().substring(0, 128));
                    begin();
                    ofy().save().entity(p).now();
                }
                Notice n = new Notice(true);
                n.setMessage(p.getId().toString());
                return n;
            } else if (p.getPassword().length() == 256 && sha256(password + p.getPassword().substring(192)).equals(p.getPassword().substring(128, 192))) {
                Notice n = new Notice(OTHER);
                n.setMessage(p.getId().toString());
                return n;
            } else
                return new Notice(false);
        }
        return new Notice(Notice.NULL);
    }

    @ApiMethod(name = "recoverPassword")
    public Notice recoverPassword(@Named("epid") String epid) {
        Profile p = getProfile(epid);
        if (p == null)
            return null;
        else if (p.getPhone() == null || p.getPhone().equals(""))
            return new Notice(false);
        else {
            byte[] n = new byte[6];
            new SecureRandom().nextBytes(n);
            String npw = Base64.encodeBase64String(n);
            byte[] s = new byte[36];
            new SecureRandom().nextBytes(s);
            String salt = sha256(Base64.encodeBase64String(s));
            String password = p.getPassword().substring(0, 128) + sha256(sha256(npw) + salt) + salt;
            p.setPassword(password);
            begin();
            ofy().save().entity(p).now();
            sms(p.getPhone(), "New ePotato-Password for " + (p.getEpid().length() < 20 ? p.getEpid() : "") + ": " + npw);
            return new Notice(true);
        }
    }

    @ApiMethod(name = "changePassword")
    public Notice changePassword(@Named("epid") String epid, @Named("npw") String npw, @Named("opw") String opw) {
        Notice login = login(epid, opw);
        if (login != null && login.getCode() != ERROR) {
            byte[] s = new byte[36];
            new SecureRandom().nextBytes(s);
            String salt = sha256(Base64.encodeBase64String(s));
            String pw = sha256(npw + salt) + salt;
            Profile p = getProfile(epid);
            p.setPassword(pw);
            begin();
            ofy().save().entity(p).now();
            return new Notice(true);
        }
        return login;
    }

    @ApiMethod(name = "deleteAccount")
    public Notice deleteAccount(@Named("epid") String epid, @Named("password") String password) {
        Notice login = login(epid, password);
        if (login != null && login.getCode() != ERROR) {
            begin();
            ofy().delete().entity(getProfile(epid)).now();
            return new Notice(true);
        }
        return login;
    }

    @ApiMethod(name = "sendFCM", path = "sendFCM")
    public Notice sendFCM(@Named("newPotato") boolean newPotato, @Named("uids") ArrayList<String> uids, @Named("x") String[] x, @Named("y") String[] y) {
        try {
            boolean error = false;
            int response = 0;
            for (int i = 0; i < 3; i++) {
                ArrayList<String> rids = new ArrayList<>();
                for (String uid : uids) {
                    ArrayList<String> fiids = i == 0 ? getProfile(ID, uid).getFiids() : getProfile(ID, uid).getFiidsIos();
                    if (fiids != null && fiids.size() > 0) {
                        rids.addAll(fiids);
                        error = false;
                    } else if (i == 0)
                        error = true;
                }
                if (rids.size() > 0) {
                    URL url = new URL("https://fcm.googleapis.com/fcm/send");
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setDoOutput(true);
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Authorization", "key=" + FCM_AUTH_KEY);
                    conn.setRequestProperty("Content-Type", "application/json");
                    JSONObject json = new JSONObject();
                    JSONArray registration_ids = new JSONArray();
                    registration_ids.addAll(rids);
                    json.put("registration_ids", registration_ids);
                    if (i == 1 && newPotato) {
                        JSONObject notification = new JSONObject();
                        notification.put("body", "New Potato!");
                        notification.put("sound", "");
                        json.put("notification", notification);
                    }
                    JSONObject data = new JSONObject();
                    if (x.length == y.length)
                        for (int ii = 0; ii < y.length; ii++)
                            data.put(x[ii], y[ii]);
                    json.put("data", data);
                    json.put("content_available", true);
                    json.put("priority", "high");
                    DataOutputStream writer = new DataOutputStream(conn.getOutputStream());
                    writer.write(json.toString().getBytes("UTF-8"));
                    if (response == 0 || response == 200)
                        response = conn.getResponseCode();
                    writer.close();
                    conn.disconnect();
                }
            }
            return error ? new Notice(ERROR) : new Notice(response);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @ApiMethod(name = "sendPotato", path = "sendPotato")
    public Notice sendPotato(@Named("_addressees_uids") ArrayList<String> _addressees_uids, @Named("addressees_ts") ArrayList<Boolean> addressees_ts, @Named("addressor") String addressor, @Named("message") String message, @Named("form") int form, @Named("pid") final String pid) {
        Notice result = new Notice(ERROR);
        final String ID_FOLLOWERS = "-4";
        ArrayList<String> normalUids = new ArrayList<>();
        ArrayList<String> tsUids = new ArrayList<>();
        if (addressees_ts.size() == _addressees_uids.size()) {
            for (int i = 0; i < _addressees_uids.size(); i++)
                (addressees_ts.get(i) ? tsUids : normalUids).add(_addressees_uids.get(i));
            ArrayList<String> normalUidsPositive = new ArrayList<>(normalUids);
            normalUidsPositive.remove(ID_FOLLOWERS);
            if (normalUidsPositive.size() > 0)
                insert(POTATO, new ArrayList<>(Arrays.asList(PO_MESSAGE, PO_FORM, PO_UID, PO_PID, PO_N)),
                        new ArrayList<>(Arrays.asList(message, Integer.toString(form), addressor, pid, Integer.toString(normalUidsPositive.size()))));
            for (String uid : normalUidsPositive) {
                Profile p = getProfile(uid);
                p.setPotatoes(new ArrayList<String>(p.getPotatoes()) {{
                    add(pid);
                }});
                begin();
                ofy().save().entity(p).now();
            }
            Notice fcm_f = null;
            if (normalUids.contains(ID_FOLLOWERS)) {
                Profile p = getProfile(addressor);
                if (p != null) {
                    ArrayList<String> fuids = p.getFollowers();
                    fcm_f = sendFCM(false, fuids, new String[]{"type", "uid", "form", "message", "pid"}, new String[]{Integer.toString(TYPE_FEED_POTATO), addressor, Integer.toString(form), message, pid});
                }
                normalUids.remove(ID_FOLLOWERS);
            }
            boolean ns = normalUids.size() > 0, ts = tsUids.size() > 0;
            Notice fcm_n = ns ? sendFCM(true, normalUids, new String[]{"type", "uid", "form", "message", "ts", "pid"}, new String[]{Integer.toString(TYPE_POTATO), addressor, Integer.toString(form), message, Boolean.toString(false), pid}) : null;
            Notice fcm_t = ts ? sendFCM(true, tsUids, new String[]{"type", "uid", "form", "message", "ts", "pid"}, new String[]{Integer.toString(TYPE_POTATO), addressor, Integer.toString(form), message, Boolean.toString(true), pid}) : null;
            result = ns && !ts ? fcm_n : ts && !ns ? fcm_t : ns && fcm_n == fcm_t ? fcm_n : ns ? new Notice(OTHER) : fcm_f;
        }
        return result;
    }

    @ApiMethod(name = "getTS", path = "getTS")
    public ArrayList<Profile> getTS(@Named("_n") int _n, @Named("contacts") ArrayList<String> contacts) {
        ArrayList<Profile> result = new ArrayList<>();
        begin();
        int n = _n;
        if ((Long) DatastoreServiceFactory.getDatastoreService().prepare(new Query("__Stat_Total__")).asSingleEntity().getProperty("count") > 1000)
            for (int i = 0; i < n; i++) {
                Profile p = ofy().load().type(Profile.class).filter("rand >=", new Random().nextDouble()).first().now();
                p = p == null ? ofy().load().type(Profile.class).first().now() : p;
                if (!contacts.contains(p.getId().toString()) && !result.contains(p) && p.getStrangers())
                    result.add(new Profile(new ArrayList<>(Arrays.asList(ID, PR_EPID)), new ArrayList<>(Arrays.asList(p.getId().toString(), p.getEpid()))));
                else if (n < 10 * _n) n++;
            }
        else {
            List<Key<Profile>> keys = ofy().load().type(Profile.class).keys().list();
            for (int i = 0; i < n; i++) {
                Profile p = ofy().load().now(keys.get(new Random().nextInt(keys.size())));
                if (!contacts.contains(p.getId().toString()) && !result.contains(p) && p.getStrangers())
                    result.add(new Profile(new ArrayList<>(Arrays.asList(ID, PR_EPID)), new ArrayList<>(Arrays.asList(p.getId().toString(), p.getEpid()))));
                else if (n < 10 * _n) n++;
            }
        }
        return result;
    }

    @ApiMethod(name = "newContact")
    public Notice newContact(@Named("uid") String uid, @Named("contactUid") final String contactUid) {
        begin();
        Profile p = getProfile(uid);
        if (p != null && (p.getContacts() == null || !p.getContacts().contains(contactUid))) {
            p.setContacts(new ArrayList<String>(p.getContacts() == null ? new ArrayList<String>() : p.getContacts()) {{
                add(contactUid);
            }});
            ofy().save().entity(p).now();
            follow(uid, contactUid);
        }
        return new Notice(p != null);
    }

    @ApiMethod(name = "deleteContact")
    public Notice deleteContact(@Named("uid") String uid, @Named("contactUid") final String contactUid) {
        begin();
        Profile p = getProfile(uid);
        if (p != null && p.getContacts() != null && p.getContacts().contains(contactUid)) {
            p.setContacts(new ArrayList<String>(p.getContacts()) {{
                remove(contactUid);
            }});
            ofy().save().entity(p).now();
            unfollow(uid, contactUid);
        }
        return new Notice(p != null);
    }

    @ApiMethod(name = "getContacts", path = "getContacts")
    public ArrayList<Profile> getContacts(@Named("_uid") String _uid, @Named("phoneNumbers") ArrayList<String> phoneNumbers) {
        ArrayList<Profile> result = null;
        final Profile profile = getProfile(_uid);
        if (profile != null) {
            result = new ArrayList<>();
            ArrayList<String> c = new ArrayList<>();
            if (phoneNumbers != null)
                for (String number : phoneNumbers) {
                    Profile p = getProfile(number);
                    if (p != null && !c.contains(p.getId().toString()))
                        c.add(p.getId().toString());
                }
            for (final Id id : checkAllOne(PROFILE, ID, profile.getContacts()))
                if (id != null && !c.contains(id.getId().toString()))
                    c.add(id.getId().toString());
            c.remove(_uid);
            profile.setContacts(c);
            begin();
            ofy().save().entity(profile).now();
            for (String id : c)
                if (!profile.getFollowing().contains(id))
                    follow(_uid, id);
            for (String id : profile.getContacts()) {
                Profile p = getProfile(id);
                if (p != null)
                    result.add(new Profile(new ArrayList<>(Arrays.asList(ID, PR_EPID, PR_PHONE)), new ArrayList<>(Arrays.asList(p.getId().toString(), p.getEpid(), p.getPhone()))));
            }
        }
        return result;
    }

    @ApiMethod(name = "getFollowing")
    public ArrayList<Profile> getFollowing(@Named("uid") String uid) {
        ArrayList<Profile> result = null;
        Profile p = getProfile(uid);
        if (p != null) {
            result = new ArrayList<>();
            ArrayList<String> following = new ArrayList<>();
            for (final Id id : checkAllOne(PROFILE, ID, p.getFollowing()))
                if (id != null && !following.contains(id.getId().toString()))
                    following.add(id.getId().toString());
            ArrayList<String> followers = new ArrayList<>();
            for (final Id id : checkAllOne(PROFILE, ID, p.getFollowers()))
                if (id != null && !followers.contains(id.getId().toString()))
                    followers.add(id.getId().toString());
            following.remove(uid);
            followers.remove(uid);
            p.setFollowing(following);
            p.setFollowers(followers);
            begin();
            ofy().save().entity(p).now();
            for (String _uid : p.getFollowing()) {
                final Profile _p = getProfile(_uid);
                if (_p != null)
                    result.add(new Profile(new ArrayList<>(Arrays.asList(ID, PR_EPID)), new ArrayList<>(Arrays.asList(_p.getId().toString(), _p.getEpid()))));
            }
        }
        return result;
    }

    @ApiMethod(name = "contactSuggestions")
    public ArrayList<Profile> contactSuggestions(@Named("uid") String uid) {
        ArrayList<Profile> result = null;
        Profile p = getProfile(uid);
        if (p != null) {
            begin();
            result = new ArrayList<>();
            for (String _uid : p.getContacts()) {
                Profile _p = getProfile(_uid);
                if (_p != null)
                    for (final String id : _p.getContacts())
                        if (!p.getContacts().contains(id) && !id.equals(uid))
                            result.add(new Profile(new ArrayList<>(Arrays.asList(ID, PR_EPID)), new ArrayList<>(Arrays.asList(getProfile(id).getId().toString(), getProfile(id).getEpid()))));
            }
            for (final Profile _p : new ArrayList<>(ofy().load().type(Profile.class).filter("contacts", uid).list()))
                result.add(new Profile(new ArrayList<>(Arrays.asList(ID, PR_EPID)), new ArrayList<>(Arrays.asList(_p.getId().toString(), _p.getEpid())), false));
        }
        return result;
    }

    @ApiMethod(name = "followSuggestions")
    public ArrayList<Profile> followSuggestions(@Named("uid") String uid) {
        ArrayList<Profile> result = null;
        Profile p = getProfile(uid);
        if (p != null) {
            begin();
            result = new ArrayList<>();
            for (String _uid : p.getFollowing()) {
                Profile _p = getProfile(_uid);
                if (_p != null)
                    for (final String id : _p.getFollowing())
                        if (!p.getFollowing().contains(id) && !id.equals(uid))
                            result.add(new Profile(new ArrayList<>(Arrays.asList(ID, PR_EPID)), new ArrayList<>(Arrays.asList(getProfile(id).getId().toString(), getProfile(id).getEpid())), false));
            }
        }
        return result;
    }

    @ApiMethod(name = "follow")
    public Notice follow(@Named("_uid") final String _uid, @Named("newId") final String newId) {
        Profile p = getProfile(_uid);
        if (p != null) {
            Profile _p = getProfile(newId);
            if (_p != null) {
                begin();
                if (!p.getFollowing().contains(newId))
                    p.setFollowing(new ArrayList<String>(p.getFollowing()) {{
                        add(newId);
                    }});
                if (!_p.getFollowers().contains(newId))
                    _p.setFollowers(new ArrayList<String>(_p.getFollowers()) {{
                        add(_uid);
                    }});
                ofy().save().entities(p, _p).now();
                return new Notice(true);
            }
            return new Notice(false);
        }
        return null;
    }

    @ApiMethod(name = "unfollow")
    public Notice unfollow(@Named("_uid") final String _uid, @Named("oldId") final String oldId) {
        Profile p = getProfile(_uid);
        if (p != null) {
            p.setFollowing(new ArrayList<String>(p.getFollowing()) {{
                remove(oldId);
            }});
            ofy().save().entity(p).now();
            Profile _p = getProfile(oldId);
            if (_p != null) {
                _p.setFollowers(new ArrayList<String>(_p.getFollowers()) {{
                    remove(_uid);
                }});
                ofy().save().entity(_p).now();
                return new Notice(true);
            }
            return new Notice(false);
        }
        return null;
    }

    @ApiMethod(name = "kevinBacon")
    public Notice kevinBacon() {
        // Get Network
        ArrayList<Profile> profiles = getAll(PROFILE);
        String result = getTimestamp() + ":";
        for (Profile p : profiles)
            result += p.getId().toString() + "." + p.getFollowing() + ";";
        // Get table of users:
        result += ";\n\n\n{\n";
        for (Profile p : profiles)
            result += "\t\"" + p.getEpid() + "\": \"" + p.getId().toString() + "\",\n";
        return new Notice(result + "}");
    }

    @ApiMethod(name = "test")
    public Notice test() {
        return new Notice("Hello, World!");
    }

}
