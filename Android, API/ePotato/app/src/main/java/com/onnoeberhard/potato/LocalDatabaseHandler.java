package com.onnoeberhard.potato;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

class LocalDatabaseHandler extends SQLiteOpenHelper {

    static final String UID = "uid";
    static final String EPID = "epid";
    static final String PASSWORD = "password";
    static final String PHONE = "phone";
    static final String FIID = "fiid";
    static final String PERMISSION_CONTACTS = "permission_contacts";
    static final String FIRST_PHONE_CONTACTS_JOB = "first_phone_contacts_job";
    static final String CONTACTS_PERMISSION_TRY = "contacts_permission_try";
    static final String OLD_UID = "old_uid";
    static final String TOTAL_STRANGERS = "total_strangers";
    static final String CREEPS = "creeps";
    static final String FEED_NOTIFICATIONS = "feed_notifications";
    static final String FOLLOWERS = "followers";

    static final String PREMIUM = "premium";
    static final String DONATION = "don";
    static final String HI_SCORE = "hi_score";

    static final String ID = "id";
    static final String DT = "dt";
    static final String CONTACT_NAME = "name";
    static final String CONTACT_PHONE = "phone";
    static final String POTATO_TEXT = "text";
    static final String POTATO_FORM = "form";

    static final String CONTACTS = "contacts";
    static final String CONTACTS_SCORE = "score";
    static final String RECEIVED_POTATOES = "received_potatoes";
    static final String RECEIVED_POTATOES_TS = "ts";
    static final String SENT_POTATOES = "sent_potatoes";
    static final String SENT_POTATOES_UIDS = "uids";
    static final String SENT_POTATOES_TSS = "tss";
    static final String SENT_POTATOES_NAMES = "names";
    static final String SENT_POTATOES_CODE = "code";
    static final int SENT_POTATOES_CODE_NULL = 0;
    static final int SENT_POTATOES_CODE_SENDING = 1;
    static final int SENT_POTATOES_CODE_ERROR = 2;
    static final int SENT_POTATOES_CODE_SENT = 3;
    static final int SENT_POTATOES_CODE_RECEIVED = 4;
    static final String NEW_POTATOES = "new_potatoes";
    static final String NEW_POTATOES_PID = "pid";
    static final String PHONE_CONTACTS = "phone_contacts";
    static final String TEMP_CONTACTS = "temp_contacts";
    static final String SUGGESTED_CONTACTS = "suggested_contacts";
    static final String SUGGESTED_FOLLOWING = "suggested_following";
    static final String FEED_POTATOES = "feed_potatoes";
    static final String NEW_FEED_POTATOES = "new_feed_potatoes";
    static final String FOLLOWING = "following";

    private static final String EPOTATO = "epotato";
    private static final String[] TABLES = {CONTACTS, RECEIVED_POTATOES, SENT_POTATOES, NEW_POTATOES, PHONE_CONTACTS, TEMP_CONTACTS, SUGGESTED_CONTACTS, SUGGESTED_FOLLOWING, FEED_POTATOES, NEW_FEED_POTATOES, FOLLOWING};

    private static final String DELETE = "*DELETE*";

    LocalDatabaseHandler(Context context) {
        super(context, EPOTATO, null, 1);
    }

    private static String escape(String s) {
        return s != null ? s.replaceAll("\'", "\'\'") : null;
    }

    @SuppressWarnings("unused")
    @SafeVarargs
    static <T> T[] concat(T[] first, T[]... rest) {
        int totalLength = first.length;
        for (T[] array : rest)
            totalLength += array.length;
        T[] result = Arrays.copyOf(first, totalLength);
        int offset = first.length;
        for (T[] array : rest) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    static <K, V extends Comparable<? super V>> Map<K, V> reverseSortByValue(Map<K, V> map) {
        return sortByValue(map, true);
    }

    static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map) {
        return sortByValue(map, false);
    }

    private static <K, V extends Comparable<? super V>> Map<K, V> sortByValue(Map<K, V> map, boolean reverse) {
        List<Map.Entry<K, V>> list = new LinkedList<>(map.entrySet());
        Collections.sort(list, new Comparator<Map.Entry<K, V>>() {
            @Override
            public int compare(Map.Entry<K, V> o1, Map.Entry<K, V> o2) {
                return (o1.getValue()).compareTo(o2.getValue());
            }
        });
        if (reverse)
            Collections.reverse(list);
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : list)
            result.put(entry.getKey(), entry.getValue());
        return result;
    }

    static String serialize(ArrayList<String> in) {
        String s = "";
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new ObjectOutputStream(out).writeObject(in);
            s = new String(Hex.encodeHex(out.toByteArray()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
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

    static String sha256(String input) {
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
        return new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK).format(Calendar.getInstance().getTime());
    }

    static String getNiceDate(String timestamp, Context c) throws ParseException {
        String s_today = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK)).format(Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
        Date dtoday = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK)).parse(s_today);
        Date dthen = (new SimpleDateFormat("yyyyMMddHHmmss", Locale.UK)).parse(timestamp);
        Calendar ctoday = Calendar.getInstance();
        ctoday.setTime(dtoday);
        Calendar cthen = Calendar.getInstance();
        cthen.setTime(dthen);
        if (ctoday.get(Calendar.DAY_OF_YEAR) == cthen.get(Calendar.DAY_OF_YEAR) && ctoday.get(Calendar.YEAR) == cthen.get(Calendar.YEAR)) {
            DateFormat df = DateFormat.getTimeInstance(DateFormat.SHORT, Locale.getDefault());
            return df.format(dthen);
        } else if (c != null && ctoday.get(Calendar.DAY_OF_YEAR) == (cthen.get(Calendar.DAY_OF_YEAR) + 1) && ctoday.get(Calendar.YEAR) == cthen.get(Calendar.YEAR) ||
                ctoday.get(Calendar.DAY_OF_YEAR) == 1 && cthen.get(Calendar.DAY_OF_YEAR) == cthen.getActualMaximum(Calendar.DAY_OF_YEAR) && ctoday.get(Calendar.YEAR) == cthen.get(Calendar.YEAR) + 1) {
            return c.getString(R.string.yesterday);
        } else if (cthen.get(Calendar.DAY_OF_YEAR) < ctoday.get(Calendar.DAY_OF_YEAR) && ctoday.get(Calendar.DAY_OF_YEAR) - cthen.get(Calendar.DAY_OF_YEAR) < 7 ||
                cthen.get(Calendar.DAY_OF_YEAR) > ctoday.get(Calendar.DAY_OF_YEAR) && Math.abs(ctoday.get(Calendar.DAY_OF_YEAR) - cthen.get(Calendar.DAY_OF_YEAR) + cthen.getActualMaximum(Calendar.DAY_OF_YEAR)) < 7) {
            return cthen.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.LONG, Locale.getDefault());
        } else {
            DateFormat df = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());
            return df.format(dthen);
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + CONTACTS + "(" +
                ID + " TEXT, " +
                UID + " TEXT, " +
                EPID + " TEXT, " +
                CONTACT_NAME + " TEXT, " +
                CONTACT_PHONE + " TEXT, " +
                CONTACTS_SCORE + " TEXT)");
        db.execSQL("CREATE TABLE " + RECEIVED_POTATOES + "(" +
                ID + " TEXT, " +
                UID + " TEXT, " +
                RECEIVED_POTATOES_TS + " TEXT, " +
                DT + " TEXT, " +
                POTATO_TEXT + " TEXT, " +
                POTATO_FORM + " TEXT)");
        db.execSQL("CREATE TABLE " + SENT_POTATOES + "(" +
                ID + " TEXT, " +
                SENT_POTATOES_UIDS + " TEXT, " +
                SENT_POTATOES_TSS + " TEXT, " +
                SENT_POTATOES_NAMES + " TEXT, " +
                DT + " TEXT, " +
                SENT_POTATOES_CODE + " TEXT, " +
                POTATO_TEXT + " TEXT, " +
                POTATO_FORM + " TEXT)");
        db.execSQL("CREATE TABLE " + NEW_POTATOES + "(" +
                ID + " TEXT, " +
                NEW_POTATOES_PID + " TEXT)");
        db.execSQL("CREATE TABLE " + PHONE_CONTACTS + "(" +
                ID + " TEXT, " +
                CONTACT_NAME + " TEXT, " +
                CONTACT_PHONE + " TEXT)");
        db.execSQL("CREATE TABLE " + TEMP_CONTACTS + "(" +
                ID + " TEXT, " +
                UID + " TEXT, " +
                EPID + " TEXT)");
        db.execSQL("CREATE TABLE " + SUGGESTED_CONTACTS + "(" +
                ID + " TEXT, " +
                UID + " TEXT, " +
                EPID + " TEXT, " +
                CONTACTS_SCORE + " TEXT)");
        db.execSQL("CREATE TABLE " + SUGGESTED_FOLLOWING + "(" +
                ID + " TEXT, " +
                UID + " TEXT, " +
                EPID + " TEXT, " +
                CONTACTS_SCORE + " TEXT)");
        db.execSQL("CREATE TABLE " + FEED_POTATOES + "(" +
                ID + " TEXT, " +
                UID + " TEXT, " +
                DT + " TEXT, " +
                POTATO_TEXT + " TEXT, " +
                POTATO_FORM + " TEXT)");
        db.execSQL("CREATE TABLE " + NEW_FEED_POTATOES + "(" +
                ID + " TEXT, " +
                NEW_POTATOES_PID + " TEXT)");
        db.execSQL("CREATE TABLE " + FOLLOWING + "(" +
                ID + " TEXT, " +
                UID + " TEXT, " +
                EPID + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        for (String table : TABLES)
            db.execSQL("DROP TABLE IF EXISTS " + table);
        onCreate(db);
    }

    void dropEverything() {
        for (String table : TABLES)
            truncate(table);
    }

    String get(String table, String idKey, String idValue, String column) {
        String result = null;
        String countQuery = "SELECT * FROM `" + table + "` WHERE `" + idKey + "` = '" + idValue + "'";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.getCount() > 0 && cursor.moveToFirst() && cursor.getString(cursor.getColumnIndex(column)) != null && !cursor.getString(cursor.getColumnIndex(column)).equals("null"))
            result = cursor.getString(cursor.getColumnIndex(column));
        cursor.close();
        return result;
    }

    @SuppressWarnings("unused")
    Map<String, String> get(String table, String idKey, String idValue) {
        Map<String, String> result = new HashMap<>();
        String countQuery = "SELECT * FROM `" + table + "` WHERE `" + idKey + "` = '" + idValue + "'";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            for (String name : cursor.getColumnNames())
                result.put(name, cursor.getString(cursor.getColumnIndex(name)) != null && !cursor.getString(cursor.getColumnIndex(name)).equals("null") ? cursor.getString(cursor.getColumnIndex(name)) : null);
        }
        cursor.close();
        return result;
    }

    private ArrayList<String> getAll(String table, String column, String idKey, String idValue) {
        ArrayList<String> result = new ArrayList<>();
        String countQuery = "SELECT * FROM `" + table + "`";
        if (idKey != null)
            countQuery = "SELECT * FROM `" + table + "` WHERE `" + idKey + "` = '" + idValue + "'";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            result.add(cursor.getString(cursor.getColumnIndex(column)) != null && !cursor.getString(cursor.getColumnIndex(column)).equals("null") ? cursor.getString(cursor.getColumnIndex(column)) : null);
            while (cursor.moveToNext())
                result.add(cursor.getString(cursor.getColumnIndex(column)) != null && !cursor.getString(cursor.getColumnIndex(column)).equals("null") ? cursor.getString(cursor.getColumnIndex(column)) : null);
        }
        cursor.close();
        return result;
    }

    ArrayList<String> getAll(String table, String column) {
        return getAll(table, column, null, null);
    }

    private ArrayList<Map<String, String>> getAll(String table, String idKey, String idValue) {
        ArrayList<Map<String, String>> result = new ArrayList<>();
        String countQuery = "SELECT * FROM `" + table + "`";
        if (idKey != null)
            countQuery = "SELECT * FROM `" + table + "` WHERE `" + idKey + "` = '" + idValue + "'";
        SQLiteDatabase db = getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            Map<String, String> row0 = new HashMap<>();
            for (String name : cursor.getColumnNames())
                row0.put(name, cursor.getString(cursor.getColumnIndex(name)) != null && !cursor.getString(cursor.getColumnIndex(name)).equals("null") ? cursor.getString(cursor.getColumnIndex(name)) : null);
            result.add(row0);
            while (cursor.moveToNext()) {
                Map<String, String> row = new HashMap<>();
                for (String name : cursor.getColumnNames())
                    row.put(name, cursor.getString(cursor.getColumnIndex(name)) != null && !cursor.getString(cursor.getColumnIndex(name)).equals("null") ? cursor.getString(cursor.getColumnIndex(name)) : null);
                result.add(row);
            }
        }
        cursor.close();
        return result;
    }

    ArrayList<Map<String, String>> getAll(String table) {
        return getAll(table, null, null);
    }

    /**
     * pairs[0] = idKey
     * pairs[1] = idValue
     *
     * @return ID, if insert
     */
    @Nullable
    private String inup(boolean insert, boolean _update, String table, String... pairs) {
        String ret_id = null;
        SQLiteDatabase db = getWritableDatabase();
        for (int i = 0; i < pairs.length; i++)
            pairs[i] = escape(pairs[i]);
        boolean delete = pairs[0].equals(DELETE) || (pairs.length > 2 && pairs[2].equals(DELETE));
        boolean update = !delete && !pairs[0].equals("") && get(table, pairs[0], pairs[1], pairs[0]) != null && !insert;
        String sql = "";
        if (delete)
            sql = "DELETE FROM `" + table + (pairs[0].equals(DELETE) ? "`" : "` WHERE `" + pairs[0] + "` = '" + pairs[1] + "'");
        else if (update) {
            String set = "";
            for (int i = 2; i < pairs.length; i++) {
                if (i % 2 == 0) {
                    if (i != 2)
                        set += ", ";
                    set += "`" + pairs[i] + "`";
                } else
                    set += " = '" + pairs[i] + "'";
            }
            if (pairs.length > 2)
                sql = "UPDATE `" + table + "` SET " + set + " WHERE `" + pairs[0] + "` = '" + pairs[1] + "'";
            else
                sql = "";
        } else if (!_update) {
            String names = "`" + ID + "`";
            String values;
            int start = 0;
            if (!pairs[0].equals(ID)) {
                String B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/";
                String id;
                do {
                    id = "";
                    for (int i = 0; i < 8; i++)
                        id += B64.charAt((int) (Math.random() * 64));
                } while (getAll(table, ID).contains(id));
                values = "'" + id + "'";
                ret_id = id;
            } else {
                values = "'" + pairs[1] + "'";
                start = 2;
                ret_id = pairs[1];
            }
            for (int i = start; i < pairs.length; i++) {
                if (i % 2 == 0)
                    names += ", `" + pairs[i] + "`";
                else
                    values += ", '" + pairs[i] + "'";
            }
            sql = "INSERT INTO `" + table + "` (" + names + ")VALUES(" + values + ")";
        }
        if (!sql.equals("")) db.execSQL(sql);
        return ret_id;
    }

    String inup(String table, String... pairs) {
        return inup(false, false, table, pairs);
    }

    String insert(String table, String... pairs) {
        return inup(true, false, table, pairs);
    }

    void update(String table, String... pairs) {
        inup(false, true, table, pairs);
    }

    void delete(String table, String idKey, String idValue) {
        inup(false, false, table, idKey, idValue, DELETE);
    }

    void truncate(String table) {
        execSQL("DELETE FROM `" + table + "`");
        execSQL("VACUUM");
    }

    private void execSQL(String sql) {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL(sql);
    }
}