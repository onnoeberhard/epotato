package com.onnoeberhard.potato;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Notice;

import java.util.ArrayList;
import java.util.Random;

import javax.net.ssl.HttpsURLConnection;

public class SendPotatoActivity extends AppCompatActivity {

    static final String UIDS = "uids";
    static final String TSS = "tss";
    static final String REPLY = "reply";
    static final String MESSAGE = "message";
    static final String FORM = "form";
    static final int STORAGE_PERMISSION = 1;
    final static String RECEIVE_FINSIH = "EP_SEND_POTATO_ACTIVITY_RECEIVE_FINISH";
    private LocalDatabaseHandler ldb;
    private PotatoView potato;
    private ArrayList<String> uids;
    private ArrayList<String> tss;
    private ArrayList<Integer> nextPotatoes = new ArrayList<>(PotatoView.potatoForms);
    private boolean a_contiue = false;
    private boolean a_sent = false;
    private boolean a_change_form = false;
    private String a_save = "-";
    private boolean a_reply = false;
    private BroadcastReceiver finish_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_send_potato);
        ldb = new LocalDatabaseHandler(this);
        uids = getIntent().getStringArrayListExtra(UIDS);
        if (uids != null && uids.size() == 0) uids = null;
        tss = getIntent().getStringArrayListExtra(TSS);
        a_reply = getIntent().getBooleanExtra(REPLY, false);
        registerReceiver(finish_receiver, new IntentFilter(RECEIVE_FINSIH));
        final Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            View titlebar = View.inflate(this, R.layout.v_titlebar, null);
            titlebar.findViewById(R.id.up).setVisibility(View.VISIBLE);
            titlebar.findViewById(R.id.click).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
            FontTextView titletv = ((FontTextView) titlebar.findViewById(R.id.title));
            titletv.setFont(FontTextView.BARIOL);
            titletv.setTextSize(22);
            if (uids == null)
                titletv.setText(R.string.lessEnthusiasticNewPotato);
            else {
                String name = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uids.get(0), LocalDatabaseHandler.CONTACT_NAME);
                name = name == null ? ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uids.get(0), LocalDatabaseHandler.EPID) : name;
                name = name == null ? ldb.get(LocalDatabaseHandler.FOLLOWING, LocalDatabaseHandler.UID, uids.get(0), LocalDatabaseHandler.EPID) : name;
                name = name == null ? ldb.get(LocalDatabaseHandler.TEMP_CONTACTS, LocalDatabaseHandler.UID, uids.get(0), LocalDatabaseHandler.EPID) : name;
                titletv.setText(name == null || name.equals("") ? getString(R.string.lessEnthusiasticNewPotato) : getString(R.string.potatoFor) + name + (uids.size() > 1 ? ", ..." : ""));
            }
            getSupportActionBar().setCustomView(titlebar);
        }
        potato = (PotatoView) findViewById(R.id.potato);
        if (savedInstanceState != null)
            potato.setup(savedInstanceState.getInt(FORM), savedInstanceState.getString(MESSAGE), null);
        else
            enterText(null);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(FORM, potato.getForm());
        outState.putString(MESSAGE, potato.getText());
        super.onSaveInstanceState(outState);
    }

    public void enterText(View view) {
        AlertDialog.Builder adb = new AlertDialog.Builder(this);
        View v = View.inflate(this, R.layout.d_sendpotato, null);
        final EditText et = (EditText) v.findViewById(R.id.et);
        et.setText(potato.getText());
        et.setSelection(et.getText().length());
        final Dialog dlg = adb.setTitle(R.string.enterMessage).setView(v)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        potato.setText(et.getText().toString());
                        dialog.dismiss();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                }).create();
        et.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    potato.setText(et.getText().toString());
                    dlg.dismiss();
                    return true;
                }
                return false;
            }
        });
        if (dlg.getWindow() != null)
            dlg.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dlg.show();
    }

    public void sendPotato(View view) {
        if (uids == null) {
            Intent i = new Intent(this, SendPeopleActivity.class);
            i.putExtra(FORM, potato.getForm());
            i.putExtra(MESSAGE, potato.getText());
            i.putExtra(FIIDService.CHANGE_FORM, a_change_form);
            i.putExtra(FIIDService.SAVE, a_save);
            startActivity(i);
            a_contiue = true;
        } else {
            int form = potato.getForm();
            final String id = ldb.insert(LocalDatabaseHandler.SENT_POTATOES,
                    LocalDatabaseHandler.DT, LocalDatabaseHandler.getTimestamp(),
                    LocalDatabaseHandler.SENT_POTATOES_UIDS, LocalDatabaseHandler.serialize(uids),
                    LocalDatabaseHandler.SENT_POTATOES_TSS, LocalDatabaseHandler.serialize(tss),
                    LocalDatabaseHandler.POTATO_TEXT, potato.getText(),
                    LocalDatabaseHandler.POTATO_FORM, Integer.toString(form),
                    LocalDatabaseHandler.SENT_POTATOES_CODE, Integer.toString(LocalDatabaseHandler.SENT_POTATOES_CODE_SENDING));
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
            }, uids, tss, potato.getText(), form, id);
            for (String uid : uids) {
                String _score = ldb.get(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.CONTACTS_SCORE);
                int score = _score == null ? 0 : Integer.parseInt(_score);
                ldb.update(LocalDatabaseHandler.CONTACTS, LocalDatabaseHandler.UID, uid, LocalDatabaseHandler.CONTACTS_SCORE, Integer.toString(++score));
            }
            sendBroadcast(new Intent(MainActivity.POTATO_SENT));
            AdActivity.stay = true;
            a_sent = true;
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sendpotato, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.newpotato:
                if (nextPotatoes.size() == 1)
                    nextPotatoes = new ArrayList<>(PotatoView.potatoForms);
                nextPotatoes.remove(nextPotatoes.indexOf(potato.getForm()));
                potato.setup(nextPotatoes.get(new Random().nextInt(nextPotatoes.size())), potato.getText(), null);
                a_change_form = true;
                break;
            case R.id.savepotato:
                String save = potato.saveToGallery(true, this);
                if (!save.equals("-")) {
                    Toast.makeText(this, getString(R.string.savedToGal), Toast.LENGTH_SHORT).show();
                    a_save = save;
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION) {
            boolean permission = grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            String save = "-";
            if (permission) save = potato.saveToGallery(false, this);
            if (!save.equals("-")) {
                Toast.makeText(this, getString(R.string.savedToGal), Toast.LENGTH_SHORT).show();
                a_save = save;
            } else if (!permission) a_save = FIIDService.DENY;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(finish_receiver);
        if (!a_contiue) {
            Bundle ab = new Bundle();
            ab.putLong(FirebaseAnalytics.Param.VALUE, a_sent ? 1 : 0);
            ab.putString(FIIDService.RESULT, a_sent ? FIIDService.OK : FIIDService.CANCEL);
            ab.putString(FIIDService.DIRECT, Boolean.toString(uids != null));
            ab.putString(FIIDService.REPLY, Boolean.toString(a_reply));
            ab.putString(FIIDService.FROM, a_reply ? FIIDService.HOME_FRAGMENT : uids != null ? FIIDService.CONTACTS_ACT : FIIDService.SEND_BUTTON);
            ab.putString(FIIDService.CHANGE_FORM, Boolean.toString(a_change_form));
            ab.putString(FIIDService.SAVE, a_save);
            ab.putInt(FIIDService.PEOPLE, uids != null ? uids.size() : 0);
            ab.putInt(FIIDService.FORM, potato != null ? potato.getForm() : 0);
            FirebaseAnalytics.getInstance(this).logEvent(FIIDService.SENT, ab);
        }
    }
}
