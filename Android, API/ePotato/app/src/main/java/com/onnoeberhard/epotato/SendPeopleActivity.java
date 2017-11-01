package com.onnoeberhard.epotato;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Notice;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Profile;
import com.onnoeberhard.epotato.backend.potatoAPI.model.ProfileCollection;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import javax.net.ssl.HttpsURLConnection;

import static com.onnoeberhard.epotato.ContactsAdapter.CONTACTS_PERMISSION;
import static com.onnoeberhard.epotato.ContactsAdapter.ID_NULL;
import static com.onnoeberhard.epotato.ContactsAdapter.ID_PHONES;
import static com.onnoeberhard.epotato.ContactsAdapter.ID_STRANGER;
import static com.onnoeberhard.epotato.SendPotatoActivity.STORAGE_PERMISSION;

public class SendPeopleActivity extends AppCompatActivity {

    PotatoView pv;
    private String message;
    private int form;
    private RecyclerView recycler;
    private ContactsAdapter adapter;
    private LocalDatabaseHandler ldb;
    private Menu menu;
    private boolean a_sent = false;
    private boolean a_change_form = false;
    private String a_save = "-";
    private boolean a_null = false;
    private boolean a_reload = false;
    private String a_contacts_permission = "-";
    private int a_invite = 0;
    private int a_skip = 0;
    private ArrayList<String> phones;
    private ArrayList<String> finalPhones = new ArrayList<>();
    private ArrayList<String> uids;
    private ArrayList<String> tss;
    private String id;
    private int i = 0;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ldb = new LocalDatabaseHandler(this);
        message = getIntent().getStringExtra(SendPotatoActivity.MESSAGE);
        form = getIntent().getIntExtra(SendPotatoActivity.FORM, 1);
        a_change_form = getIntent().getBooleanExtra(FIIDService.CHANGE_FORM, a_change_form);
        a_save = getIntent().getStringExtra(FIIDService.SAVE);
        setContentView(R.layout.a_send_people);
        final Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            View titlebar = View.inflate(this, R.layout.v_titlebar, null);
            ((FontTextView) titlebar.findViewById(R.id.title)).setFont(FontTextView.BARIOL);
            titlebar.findViewById(R.id.up).setVisibility(View.VISIBLE);
            titlebar.findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            ((FontTextView) titlebar.findViewById(R.id.title)).setTextSize(22);
            ((FontTextView) titlebar.findViewById(R.id.title)).setText(R.string.sendPTo);
            getSupportActionBar().setCustomView(titlebar);
        }
        View loading = findViewById(R.id.loading);
        recycler = (RecyclerView) findViewById(R.id.peopleRecycler);
        adapter = new ContactsAdapter(ContactsAdapter.SEND_PEOPLE, this, form, message, loading, recycler);
        recycler.setAdapter(adapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setRecycleChildrenOnDetach(true);
        recycler.setLayoutManager(llm);
        recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                adapter.notifyDataSetChanged();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (recycler != null)
            recycler.getAdapter().notifyDataSetChanged();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.sendpeople, menu);
        boolean permission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        menu.findItem(R.id.checkcontacts).setVisible(!permission);
        menu.findItem(R.id.reload).setVisible(permission);
        a_contacts_permission = permission ? FIIDService.OK : "-";
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.share:
                sendImage("", true);
                break;
            case R.id.savepotato:
                String save = pv.saveToGallery(true, this);
                if (!save.equals("-")) {
                    Toast.makeText(this, R.string.savedToGal, Toast.LENGTH_SHORT).show();
                    a_save = save;
                }
                break;
            case R.id.addepid:
                if (adapter != null)
                    adapter.addepid();
                break;
            case R.id.checkcontacts:
            case R.id.reload:
                ContactsAdapter.addPhoneContacts(true, this, adapter);
                a_reload = true;
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (a_sent || a_null) {
            Bundle ab = new Bundle();
            ab.putLong(FirebaseAnalytics.Param.VALUE, a_sent ? 1 : 0);
            ab.putString(FIIDService.RESULT, a_sent ? FIIDService.OK : FIIDService.CANCEL);
            ab.putString(FIIDService.DIRECT, Boolean.toString(false));
            ab.putString(FIIDService.FROM, FIIDService.SEND_BUTTON);
            ab.putString(FIIDService.CHANGE_FORM, Boolean.toString(a_change_form));
            ab.putString(FIIDService.SAVE, a_save);
            ab.putInt(FIIDService.PEOPLE, uids != null ? uids.size() : 0);
            ab.putInt(FIIDService.FORM, form);
            int a_ts = 0;
            if (tss != null)
                for (String ts : tss)
                    if (Boolean.parseBoolean(ts)) a_ts++;
            ab.putInt(FIIDService.TS, a_ts);
            ab.putString(FIIDService.PEOPLE_RELOAD, Boolean.toString(a_reload));
            ab.putString(FIIDService.CONTACTS_PERMISSION, a_contacts_permission);
            int a_temp = -a_ts;
            ArrayList<String> contacts = ldb.getAll(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID);
            if (uids != null)
                for (String uid : uids)
                    if (!contacts.contains(uid)) a_temp++;
            ab.putInt(FIIDService.TEMPORARY, a_temp);
            ab.putString(FIIDService.NULL, Boolean.toString(a_null));
            ab.putInt(FIIDService.PHONES, phones.size());
            ab.putInt(FIIDService.PHONES_IMAGE, finalPhones.size());
            ab.putInt(FIIDService.PHONES_INVITE, a_invite);
            ab.putInt(FIIDService.PHONES_SKIP, a_skip);
            FirebaseAnalytics.getInstance(this).logEvent(FIIDService.SENT, ab);
        }
        ldb.close();
    }

    public void sendPotato(View view) {
        uids = new ArrayList<>();
        tss = new ArrayList<>();
        phones = new ArrayList<>();
        for (int i = 0; i < adapter.checkItems.size(); i++) {
            ContactsAdapter.CheckItem item = adapter.checkItems.get(adapter.checkItems.keyAt(i));
            if (item.checked && (!uids.contains(Long.toString(item.id)) || item.id == ID_STRANGER) && item.id != ID_NULL && item.id != ID_PHONES)
                uids.add(Long.toString(item.id));
            else if (item.checked && item.id == ID_PHONES)
                phones.add(item.text);
        }
        if (uids.size() > 0 || phones.size() > 0)
            doPhones();
        else
            showNone();
    }

    private void showNone() {
        new AlertDialog.Builder(this).setMessage(R.string.noReceiver)
                .setPositiveButton(R.string.quit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        sendBroadcast(new Intent(SendPotatoActivity.RECEIVE_FINSIH));
                        dialog.dismiss();
                        a_null = true;
                        finish();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                }).show();
    }

    private void close() {
        if (uids.size() > 0 || finalPhones.size() > 0) {
            ArrayList<String> _uids = new ArrayList<>(uids);
            int strangers = 0;
            for (String uid : _uids)
                if (Long.parseLong(uid) == ID_STRANGER) {
                    uids.remove(uid);
                    strangers++;
                } else tss.add(Boolean.toString(false));
            id = ldb.insert(LocalDatabaseHandler.SENT_POTATOES,
                    LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                    LocalDatabaseHandler.SENT_POTATOES_UIDS, uids.size() > 0 ? LocalDatabaseHandler.serialize(uids) : null,
                    LocalDatabaseHandler.SENT_POTATOES_TSS, tss.size() > 0 ? LocalDatabaseHandler.serialize(tss) : null,
                    LocalDatabaseHandler.SENT_POTATOES_NAMES, phones.size() > 0 ? LocalDatabaseHandler.serialize(finalPhones) : null,
                    LocalDatabaseHandler.POTATO_TEXT, message,
                    LocalDatabaseHandler.POTATO_FORM, Integer.toString(form),
                    LocalDatabaseHandler.SENT_POTATOES_CODE,
                    _uids.size() > 0 ? Integer.toString(LocalDatabaseHandler.SENT_POTATOES_CODE_SENDING) : Integer.toString(LocalDatabaseHandler.SENT_POTATOES_CODE_NULL));
            if (strangers > 0) {
                new EndpointsHandler(this).getTS(new EndpointsHandler.APIRequest() {
                    @Override
                    public void onResult(Object result) {
                        if (result != null && ((ProfileCollection) result).getItems() != null && ((ProfileCollection) result).getItems().size() > 0) {
                            for (Profile p : ((ProfileCollection) result).getItems()) {
                                uids.add(0, p.getId().toString());
                                tss.add(0, Boolean.toString(true));
                                ldb.inup(LocalDatabaseHandler.TEMP_CONTACTS,
                                        LocalDatabaseHandler.UID, p.getId().toString(),
                                        LocalDatabaseHandler.EPID, p.getEpid());
                            }
                            ldb.update(LocalDatabaseHandler.SENT_POTATOES,
                                    LocalDatabaseHandler.ID, id,
                                    LocalDatabaseHandler.SENT_POTATOES_UIDS, LocalDatabaseHandler.serialize(uids),
                                    LocalDatabaseHandler.SENT_POTATOES_TSS, LocalDatabaseHandler.serialize(tss));
                        }
                        sendToUids();
                    }
                }, strangers);
            } else
                sendToUids();
            for (String uid : uids) {
                String _score = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.CONTACTS_SCORE);
                int score = _score == null ? 0 : Integer.parseInt(_score);
                ldb.update(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.CONTACTS_SCORE, Integer.toString(++score));
            }
            sendBroadcast(new Intent(MainActivity.POTATO_SENT));
            sendBroadcast(new Intent(SendPotatoActivity.RECEIVE_FINSIH));
            AdActivity.stay = true;
            a_sent = true;
            finish();
        } else {
            i = 0;
            showNone();
        }
    }

    private void sendToUids() {
        if (uids.size() > 0) {
            new EndpointsHandler(this).sendPotato(new EndpointsHandler.APIRequest() {
                @Override
                public void onResult(Object result) {
                    Notice n = (Notice) result;
                    ldb.inup(LocalDatabaseHandler.SENT_POTATOES,
                            LocalDatabaseHandler.ID, id, LocalDatabaseHandler.SENT_POTATOES_CODE,
                            Integer.toString(n != null && n.getCode() == HttpsURLConnection.HTTP_OK ?
                                    LocalDatabaseHandler.SENT_POTATOES_CODE_SENT : LocalDatabaseHandler.SENT_POTATOES_CODE_ERROR));
                    sendBroadcast(new Intent(MainActivity.POTATO_SENT));
                }
            }, uids, tss, message, form, id);
        }
    }

    private void doPhones() {
        if (phones.size() > i) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this).setMessage(phones.get(i) + getString(R.string.doesNotHave))
                    .setPositiveButton(R.string.sendImage, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            finalPhones.add(phones.get(i));
                            sendImage(phones.get(i), false);
                            i++;
                            doPhones();
                        }
                    })
                    .setNeutralButton(R.string.invite, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            adapter.invite(phones.get(i));
                            a_invite++;
                            dialog.dismiss();
                            i++;
                            doPhones();
                        }
                    })
                    .setNegativeButton(R.string.skip, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            a_skip++;
                            i++;
                            doPhones();
                        }
                    });
            builder.show();
        } else close();
    }

    private void sendImage(final String name, boolean general) {
        try {
            File dir = new File(getCacheDir(), "images");
            if (dir.exists() || dir.mkdirs()) {
                final String file = dir + "/image.png";
                final FileOutputStream out = new FileOutputStream(file);
                pv.bitmapWithWatermark().compress(Bitmap.CompressFormat.PNG, 100, out);
                out.close();
                Uri uri = FileProvider.getUriForFile(SendPeopleActivity.this, "com.onnoeberhard.epotato.fileprovider", new File(file));
                if (uri != null) {
                    Intent shareIntent = new Intent();
                    shareIntent.setAction(Intent.ACTION_SEND);
                    shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    shareIntent.setDataAndType(uri, getContentResolver().getType(uri));
                    shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
                    startActivity(Intent.createChooser(shareIntent, general ? getString(R.string.shareImage) : getString(R.string.shareWithX, name)));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        if (requestCode == CONTACTS_PERMISSION) {
            ContactsAdapter.contactsPermissionRequestCallback(requestCode, grantResults, this, menu, adapter);
            boolean permission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            a_contacts_permission = permission ? FIIDService.ALLOW : FIIDService.DENY;
        } else if (requestCode == STORAGE_PERMISSION) {
            boolean permission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (permission && pv != null) {
                String save = "-";
                if (permission) save = pv.saveToGallery(false, this);
                if (!save.equals("-"))
                    Toast.makeText(this, getString(R.string.savedToGal), Toast.LENGTH_SHORT).show();
                else a_save = save;
            } else if (!permission) a_save = FIIDService.DENY;
        }
    }
}
