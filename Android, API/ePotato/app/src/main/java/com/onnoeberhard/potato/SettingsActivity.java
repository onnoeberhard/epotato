package com.onnoeberhard.potato;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Notice;
import com.onnoeberhard.potato.inappbilling.IabHelper;
import com.onnoeberhard.potato.inappbilling.IabResult;
import com.onnoeberhard.potato.inappbilling.Purchase;

import java.util.ArrayList;
import java.util.Collections;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.onnoeberhard.potato.EndpointsHandler.PROFILE;
import static com.onnoeberhard.potato.EndpointsHandler.PR_STRANGERS;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_settings);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.settings);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getFragmentManager().beginTransaction()
                .add(R.id.settingsFrame, new SettingsFragment())
                .commit();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        FIIDService.analytics(this);
    }

    public static class SettingsFragment extends PreferenceFragment {

        Preference phone;
        IabHelper iabHelper;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(getActivity());
            FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
            remoteConfig.setDefaults(R.xml.remote_config_defaults);
            if (!remoteConfig.getBoolean("feed"))
                ((PreferenceCategory) findPreference("general")).removePreference(findPreference("feed_notifications"));
            if (!remoteConfig.getBoolean("ts")) {
                ((PreferenceCategory) findPreference("general")).removePreference(findPreference("total_strangers"));
                findPreference("creeps").setSummary(null);
            }
            findPreference("total_strangers").setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    new EndpointsHandler(getActivity()).update(null, PROFILE, EndpointsHandler.ID, Long.toString(sp.getLong(LocalDatabaseHandler.UID, 0L)),
                            new ArrayList<>(Collections.singletonList(PR_STRANGERS)), new ArrayList<>(Collections.singletonList(newValue.toString())));
                    return true;
                }
            });
            final Preference changeepid = findPreference("changeepid");
            changeepid.setSummary(sp.getString(LocalDatabaseHandler.EPID, ""));
            changeepid.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    View v = View.inflate(getActivity(), R.layout.d_settings_changeepid, null);
                    final EditText epidet = (EditText) v.findViewById(R.id.epid);
                    final EditText pwet = (EditText) v.findViewById(R.id.pw);
                    final View loading = v.findViewById(R.id.loading);
                    final AlertDialog dlg = new AlertDialog.Builder(getActivity()).setTitle(R.string.changeEpid)
                            .setView(v)
                            .setPositiveButton(android.R.string.ok, null)
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Bundle ab = new Bundle();
                                    ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                                    ab.putString(FIIDService.RESULT, FIIDService.CANCEL);
                                    FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.CHANGE_EPID, ab);
                                }
                            })
                            .create();
                    dlg.show();
                    dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final String epid = epidet.getText().toString();
                            final String pw = pwet.getText().toString();
                            if (epid.length() == 0 || pw.length() == 0)
                                Toast.makeText(getActivity(), R.string.enterUNPW, Toast.LENGTH_SHORT).show();
                            else if (!epid.matches("[a-zA-Z0-9._-]{1,30}"))
                                Toast.makeText(getActivity(), R.string.unRegEx, Toast.LENGTH_LONG).show();
                            else {
                                loading.setVisibility(View.VISIBLE);
                                new EndpointsHandler(getActivity()).check(new EndpointsHandler.APIRequest() {
                                    @Override
                                    public void onResult(Object result) {
                                        if (result != null) {
                                            loading.setVisibility(View.GONE);
                                            Toast.makeText(getActivity(), R.string.epidTaken, Toast.LENGTH_SHORT).show();
                                        } else {
                                            new EndpointsHandler(getActivity()).login(new EndpointsHandler.APIRequest() {
                                                @Override
                                                public void onResult(Object result) {
                                                    Notice n = (Notice) result;
                                                    if (n != null && n.getOk()) {
                                                        new EndpointsHandler(getActivity()).update(
                                                                new EndpointsHandler.APIRequest() {
                                                                    @Override
                                                                    public void onResult(Object result) {
                                                                        FIIDService.updateFIEPID(getActivity(), null);
                                                                        changeepid.setSummary(epid);
                                                                        loading.setVisibility(View.GONE);
                                                                        Bundle ab = new Bundle();
                                                                        ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                                                                        ab.putString(FIIDService.RESULT, FIIDService.OK);
                                                                        FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.CHANGE_EPID, ab);
                                                                        dlg.dismiss();
                                                                    }
                                                                }, EndpointsHandler.PROFILE,
                                                                EndpointsHandler.ID, Long.toString(sp.getLong(LocalDatabaseHandler.UID, 0L)),
                                                                new ArrayList<>(Collections.singletonList(EndpointsHandler.PR_EPID)),
                                                                new ArrayList<>(Collections.singletonList(epid)));
                                                    } else {
                                                        loading.setVisibility(View.GONE);
                                                        if (n != null)
                                                            Toast.makeText(getActivity(), R.string.wrongPW, Toast.LENGTH_SHORT).show();
                                                        else
                                                            Toast.makeText(getActivity(), R.string.errorINOUT, Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            }, Long.toString(sp.getLong(LocalDatabaseHandler.UID, 0L)), LocalDatabaseHandler.sha256(pw));
                                        }
                                    }
                                }, EndpointsHandler.PROFILE, EndpointsHandler.PR_EPID, epid);
                            }

                        }
                    });
                    return false;
                }
            });
            phone = findPreference("phone");
            String number = sp.getString(LocalDatabaseHandler.PHONE, null);
            phone.setTitle(number == null ? getActivity().getString(R.string.conPhone) : getActivity().getString(R.string.changePhone));
            if (number != null)
                phone.setSummary(number);
            phone.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent i = new Intent(getActivity(), WelcomeActivity.class);
                    i.putExtra(WelcomeActivity.DO_PHONE, true);
                    startActivity(i);
                    return false;
                }
            });
            findPreference("changepw").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    final View v = View.inflate(getActivity(), R.layout.d_settings_changepw, null);
                    final View loading = v.findViewById(R.id.loading);
                    final AlertDialog dlg = new AlertDialog.Builder(getActivity()).setTitle(R.string.changePW)
                            .setView(v)
                            .setPositiveButton(android.R.string.ok, null)
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Bundle ab = new Bundle();
                                    ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                                    ab.putString(FIIDService.RESULT, FIIDService.CANCEL);
                                    FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.CHANGE_PASSWORD, ab);
                                }
                            })
                            .create();
                    dlg.show();
                    dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            final String pwo = ((EditText) v.findViewById(R.id.old_pw)).getText().toString();
                            final String pw1 = ((EditText) v.findViewById(R.id.pw)).getText().toString();
                            final String pw2 = ((EditText) v.findViewById(R.id.pw_confirm)).getText().toString();
                            if (pwo.length() == 0 || pw1.length() == 0 || pw2.length() == 0)
                                Toast.makeText(getActivity(), R.string.enterCurNewPW, Toast.LENGTH_SHORT).show();
                            else if (!pw2.equals(pw1))
                                Toast.makeText(getActivity(), R.string.noPWMatch, Toast.LENGTH_SHORT).show();
                            else if (pw1.length() < 4)
                                Toast.makeText(getActivity(), R.string.unsecurePW, Toast.LENGTH_SHORT).show();
                            else {
                                loading.setVisibility(View.VISIBLE);
                                new EndpointsHandler(getActivity()).changePassword(new EndpointsHandler.APIRequest() {
                                    @Override
                                    public void onResult(Object result) {
                                        loading.setVisibility(View.GONE);
                                        if (result != null) {
                                            if (((Notice) result).getOk()) {
                                                sp.edit().putString(LocalDatabaseHandler.PASSWORD, LocalDatabaseHandler.sha256(pw1)).apply();
                                                Bundle ab = new Bundle();
                                                ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                                                ab.putString(FIIDService.RESULT, FIIDService.OK);
                                                FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.CHANGE_PASSWORD, ab);
                                                dlg.dismiss();
                                            } else
                                                Toast.makeText(getActivity(), R.string.wrongPW, Toast.LENGTH_SHORT).show();
                                        } else
                                            Toast.makeText(getActivity(), R.string.errorToast, Toast.LENGTH_SHORT).show();
                                    }
                                }, Long.toString(sp.getLong(LocalDatabaseHandler.UID, 0L)), LocalDatabaseHandler.sha256(pw1), LocalDatabaseHandler.sha256(pwo));
                            }
                        }
                    });
                    return false;
                }
            });
            findPreference("logout").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    new AlertDialog.Builder(getActivity()).setTitle(R.string.logout)
                            .setMessage(R.string.logoutOk)
                            .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    Bundle ab = new Bundle();
                                    ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                                    ab.putString(FIIDService.RESULT, FIIDService.OK);
                                    FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.LOGOUT, ab);
                                    MainActivity.logout(getActivity());
                                    dialogInterface.dismiss();
                                }
                            })
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Bundle ab = new Bundle();
                                    ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                                    ab.putString(FIIDService.RESULT, FIIDService.CANCEL);
                                    FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.LOGOUT, ab);
                                }
                            })
                            .create().show();
                    return false;
                }
            });
            findPreference("deleteaccount").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    View v = View.inflate(getActivity(), R.layout.d_settings_deleteaccount, null);
                    final EditText pwet = (EditText) v.findViewById(R.id.pw);
                    final View loading = v.findViewById(R.id.loading);
                    final AlertDialog dlg = new AlertDialog.Builder(getActivity()).setTitle(R.string.delAccount)
                            .setView(v)
                            .setPositiveButton(android.R.string.ok, null)
                            .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.cancel();
                                }
                            })
                            .setOnCancelListener(new DialogInterface.OnCancelListener() {
                                @Override
                                public void onCancel(DialogInterface dialog) {
                                    Bundle ab = new Bundle();
                                    ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                                    ab.putString(FIIDService.RESULT, FIIDService.CANCEL);
                                    FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.DELETE_ACCOUNT, ab);
                                }
                            })
                            .create();
                    dlg.show();
                    dlg.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (pwet.length() == 0)
                                Toast.makeText(getActivity(), R.string.enterPW, Toast.LENGTH_SHORT).show();
                            else {
                                loading.setVisibility(View.VISIBLE);
                                new EndpointsHandler(getActivity()).deleteAccount(new EndpointsHandler.APIRequest() {
                                    @Override
                                    public void onResult(Object result) {
                                        loading.setVisibility(View.GONE);
                                        if (result != null) {
                                            if (((Notice) result).getOk()) {
                                                Bundle ab = new Bundle();
                                                ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                                                ab.putString(FIIDService.RESULT, FIIDService.OK);
                                                FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.DELETE_ACCOUNT, ab);
                                                MainActivity.logout(getActivity());
                                                dlg.dismiss();
                                            } else
                                                Toast.makeText(getActivity(), R.string.wrongPW, Toast.LENGTH_SHORT).show();
                                        } else
                                            Toast.makeText(getActivity(), R.string.errorToast, Toast.LENGTH_SHORT).show();
                                    }
                                }, Long.toString(sp.getLong(LocalDatabaseHandler.UID, 0L)), LocalDatabaseHandler.sha256(pwet.getText().toString()));
                            }
                        }
                    });
                    return false;
                }
            });
            Intent reviewIntent = new Intent(Intent.ACTION_VIEW);
            reviewIntent.setData(Uri.parse(getActivity().getString(R.string.marketURL)));
            findPreference("review").setIntent(reviewIntent);
            findPreference("review").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.LEAVE_REVIEW, null);
                    return false;
                }
            });
            if (sp.getBoolean(LocalDatabaseHandler.PREMIUM, false)) {
                findPreference("premium").setTitle(R.string.donateTitle);
                findPreference("premium").setSummary(R.string.thankSummary);
            } else
                findPreference("premium").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        iabHelper = new IabHelper(getActivity(), getActivity().getString(R.string.billingKey));
                        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
                            @Override
                            public void onIabSetupFinished(IabResult result) {
                                if (result.isSuccess()) {
                                    try {
                                        iabHelper.launchPurchaseFlow(getActivity(), sp.getBoolean(LocalDatabaseHandler.PREMIUM, false) ? LocalDatabaseHandler.DONATION : LocalDatabaseHandler.PREMIUM, 0, new IabHelper.OnIabPurchaseFinishedListener() {
                                            @Override
                                            public void onIabPurchaseFinished(IabResult result, Purchase info) {
                                                if (result.isSuccess() && info.getSku().equals(LocalDatabaseHandler.PREMIUM)) {
                                                    new AlertDialog.Builder(getActivity()).setTitle(R.string.restart)
                                                            .setMessage(R.string.thankPurchase)
                                                            .setPositiveButton(R.string.restart, new DialogInterface.OnClickListener() {
                                                                @Override
                                                                public void onClick(DialogInterface dialog, int which) {
                                                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                                                    intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                                                                    getActivity().startActivity(intent);
                                                                    getActivity().finish();
                                                                    Runtime.getRuntime().exit(0);
                                                                }
                                                            }).setCancelable(false).show();
                                                } else if (result.isSuccess() && info.getSku().equals(LocalDatabaseHandler.DONATION)) {
                                                    new AlertDialog.Builder(getActivity())
                                                            .setMessage(R.string.wowthanks)
                                                            .show();
                                                }
                                                Bundle ab = new Bundle();
                                                ab.putLong(FirebaseAnalytics.Param.VALUE, result.isSuccess() ? 1 : 0);
                                                ab.putString(FIIDService.RESULT, result.isSuccess() ? FIIDService.OK : FIIDService.CANCEL);
                                                FirebaseAnalytics.getInstance(getActivity()).logEvent(FIIDService.REMOVE_ADS, ab);
                                            }
                                        }, "");
                                    } catch (IabHelper.IabAsyncInProgressException e) {
                                        e.printStackTrace();
                                        Toast.makeText(getActivity(), R.string.errorToast, Toast.LENGTH_SHORT).show();
                                    }
                                } else
                                    Toast.makeText(getActivity(), R.string.errorToast, Toast.LENGTH_SHORT).show();
                            }
                        });
                        return true;
                    }
                });
            findPreference("about").setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    View v = View.inflate(getActivity(), R.layout.d_settings_about, null);
                    TextView tv = (TextView) v.findViewById(R.id.tv);
                    tv.setText(Html.fromHtml(getString(R.string.aboutMessage, getString(R.string.versionName))));
                    tv.setMovementMethod(LinkMovementMethod.getInstance());
                    new AlertDialog.Builder(getActivity()).setView(v).show();
                    return true;
                }
            });
        }

        @Override
        public void onResume() {
            super.onResume();
            String number = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(LocalDatabaseHandler.PHONE, null);
            if (phone != null && number != null) {
                phone.setTitle(getActivity().getString(R.string.changePhone));
                phone.setSummary(number);
            }
        }

        @Override
        public void onDestroy() {
            super.onDestroy();
            if (iabHelper != null) try {
                iabHelper.dispose();
            } catch (IabHelper.IabAsyncInProgressException e) {
                e.printStackTrace();
            }
        }
    }

}
