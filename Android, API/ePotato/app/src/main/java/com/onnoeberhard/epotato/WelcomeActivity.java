package com.onnoeberhard.epotato;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Id;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Notice;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Profile;

import net.rimoto.intlphoneinput.IntlPhoneInput;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;

public class WelcomeActivity extends AppCompatActivity {

    static final String DO_PHONE = "do_phone";
    private static final int S_START = 0;
    private static final int S_LOGIN = 1;
    private static final int S_FORGOT_PASSWORD = 2;
    private static final int S_RECOVER_PASSWORD = 3;
    private static final int S_SIGNUP = 4;
    private static final int S_PHONE = 5;
    private static final int S_CONFIRM_PHONE = 6;
    private int step = S_START;
    private boolean doing_phone = false;

    private SharedPreferences sp;

    private boolean recovered_password = false;

    private String epid;
    private String pw;  //hashed
    private String _pw; //unhashed
    private Long id;
    private IntlPhoneInput phoneInput;
    private String phone;

    private ProgressBar loading;
    private String code;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        if (getIntent().getBooleanExtra(DO_PHONE, false)) {
            doing_phone = true;
            signup2(null);
        } else
            start();
    }

    private void start() {
        step = S_START;
        setContentView(R.layout.a_welcome);
        _pw = "";
    }

    public void login1(View view) {
        step = S_LOGIN;
        setContentView(R.layout.l_login);
        loading = (ProgressBar) findViewById(R.id.loading);
        ((EditText) findViewById(R.id.login_epid)).setText(epid);
        if (recovered_password) {
            findViewById(R.id.recovered).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.recovered)).setText(getString(R.string.enterSMSPw, phone));
            ((Button) findViewById(R.id.forgot_password)).setText(R.string.sendSMSAgain);
        }
        ((EditText) findViewById(R.id.login_password)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    login2(null);
                    return true;
                }
                return false;
            }
        });
    }

    public void login2(View view) {
        epid = ((EditText) findViewById(R.id.login_epid)).getText().toString();
        _pw = ((EditText) findViewById(R.id.login_password)).getText().toString();
        if (epid.length() == 0 || _pw.length() == 0)
            Toast.makeText(this, R.string.enterEPIDPW, Toast.LENGTH_SHORT).show();
        else {
            loading.setVisibility(View.VISIBLE);
            View v = getCurrentFocus();
            if (v != null)
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
            pw = LocalDatabaseHandler.sha256(_pw);
            new EndpointsHandler(this).login(new EndpointsHandler.APIRequest() {
                @Override
                public void onResult(Object result) {
                    loading.setVisibility(View.INVISIBLE);
                    if (result == null || ((Notice) result).getCode() == EndpointsHandler.NOTICE_NULL)
                        Toast.makeText(WelcomeActivity.this, R.string.noEPID, Toast.LENGTH_SHORT).show();
                    else if (((Notice) result).getCode() == EndpointsHandler.NOTICE_ERROR)
                        Toast.makeText(WelcomeActivity.this, R.string.wrongPW, Toast.LENGTH_SHORT).show();
                    else if (((Notice) result).getCode() == EndpointsHandler.NOTICE_OTHER) {
                        id = Long.parseLong(((Notice) result).getMessage());
                        step = S_RECOVER_PASSWORD;
                        setContentView(R.layout.l_recover_password);
                        loading = (ProgressBar) findViewById(R.id.loading);
                        ((EditText) findViewById(R.id.password2)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                            @Override
                            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                if (actionId == EditorInfo.IME_ACTION_DONE) {
                                    recover_password(null);
                                    return true;
                                }
                                return false;
                            }
                        });
                    } else {
                        loading.setVisibility(View.VISIBLE);
                        new EndpointsHandler(WelcomeActivity.this).getProfile(new EndpointsHandler.APIRequest() {
                            @Override
                            public void onResult(Object result) {
                                loading.setVisibility(View.INVISIBLE);
                                Profile p = (Profile) result;
                                if (p != null) {
                                    sp.edit().putLong(LocalDatabaseHandler.UID, p.getId())
                                            .putString(LocalDatabaseHandler.PASSWORD, pw)
                                            .putString(LocalDatabaseHandler.EPID, p.getEpid())
                                            .putString(LocalDatabaseHandler.PHONE, p.getPhone()).apply();
                                    startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                                    Bundle ab = new Bundle();
                                    ab.putString(FIIDService.RECOVER, Boolean.toString(false));
                                    FirebaseAnalytics.getInstance(WelcomeActivity.this).logEvent(FIIDService.LOGIN, ab);
                                    finish();
                                } else
                                    Toast.makeText(WelcomeActivity.this, R.string.errorToast, Toast.LENGTH_SHORT).show();
                            }
                        }, EndpointsHandler.ID, ((Notice) result).getMessage());
                    }
                }
            }, epid, pw);
        }
    }

    public void forgot_password(View view) {
        epid = ((EditText) findViewById(R.id.login_epid)).getText().toString();
        if (epid.length() == 0)
            Toast.makeText(this, R.string.enterEPID, Toast.LENGTH_SHORT).show();
        else {
            loading.setVisibility(View.VISIBLE);
            new EndpointsHandler(this).recoverPassword(new EndpointsHandler.APIRequest() {
                @Override
                public void onResult(Object result) {
                    loading.setVisibility(View.INVISIBLE);
                    if (result == null) {
                        Toast.makeText(WelcomeActivity.this, R.string.noEPID, Toast.LENGTH_SHORT).show();
                        Bundle ab = new Bundle();
                        ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                        ab.putString(FIIDService.RESULT, FIIDService.FAIL);
                        FirebaseAnalytics.getInstance(WelcomeActivity.this).logEvent(FIIDService.FORGOT_PASSWORD, ab);
                    } else if (!((Notice) result).getOk()) {
                        Toast.makeText(WelcomeActivity.this, R.string.noRecover, Toast.LENGTH_LONG).show();
                        Bundle ab = new Bundle();
                        ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                        ab.putString(FIIDService.RESULT, FIIDService.FAIL);
                        FirebaseAnalytics.getInstance(WelcomeActivity.this).logEvent(FIIDService.FORGOT_PASSWORD, ab);
                    } else {
                        Toast.makeText(WelcomeActivity.this, R.string.newPasswordSMS, Toast.LENGTH_LONG).show();
                        recovered_password = true;
                        login1(null);
                        Bundle ab = new Bundle();
                        ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                        ab.putString(FIIDService.RESULT, FIIDService.OK);
                        FirebaseAnalytics.getInstance(WelcomeActivity.this).logEvent(FIIDService.FORGOT_PASSWORD, ab);
                    }
                }
            }, epid);
        }
    }

    public void recover_password(View view) {
        String pw1 = ((TextView) findViewById(R.id.password)).getText().toString();
        String pw2 = ((TextView) findViewById(R.id.password2)).getText().toString();
        if (pw1.length() == 0 || pw2.length() == 0)
            Toast.makeText(this, R.string.confirmPassword, Toast.LENGTH_SHORT).show();
        else if (!pw2.equals(pw1))
            Toast.makeText(this, R.string.noPWMatch, Toast.LENGTH_SHORT).show();
        else if (pw1.length() < 4)
            Toast.makeText(this, R.string.unsecurePW, Toast.LENGTH_SHORT).show();
        else {
            loading.setVisibility(View.VISIBLE);
            View v = getCurrentFocus();
            if (v != null)
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
            String npw = LocalDatabaseHandler.sha256(pw1);
            new EndpointsHandler(this).changePassword(new EndpointsHandler.APIRequest() {
                @Override
                public void onResult(Object result) {
                    loading.setVisibility(View.INVISIBLE);
                    if (result != null && ((Notice) result).getOk()) {
                        loading.setVisibility(View.VISIBLE);
                        new EndpointsHandler(WelcomeActivity.this).getProfile(new EndpointsHandler.APIRequest() {
                            @Override
                            public void onResult(Object result) {
                                loading.setVisibility(View.INVISIBLE);
                                Profile p = (Profile) result;
                                if (p != null) {
                                    sp.edit().putLong(LocalDatabaseHandler.UID, id)
                                            .putString(LocalDatabaseHandler.PASSWORD, pw)
                                            .putString(LocalDatabaseHandler.EPID, p.getEpid())
                                            .putString(LocalDatabaseHandler.PHONE, p.getPhone()).apply();
                                    startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                                    Bundle ab = new Bundle();
                                    ab.putString(FIIDService.RECOVER, Boolean.toString(true));
                                    FirebaseAnalytics.getInstance(WelcomeActivity.this).logEvent(FIIDService.LOGIN, ab);
                                    finish();
                                } else
                                    Toast.makeText(WelcomeActivity.this, R.string.errorToast, Toast.LENGTH_SHORT).show();
                            }
                        }, EndpointsHandler.ID, id.toString());
                    } else
                        Toast.makeText(WelcomeActivity.this, R.string.errorToast, Toast.LENGTH_SHORT).show();
                }
            }, epid, npw, pw);
        }
    }

    public void signup1(View view) {
        step = S_SIGNUP;
        setContentView(R.layout.l_signup);
        loading = (ProgressBar) findViewById(R.id.loading);
        ((EditText) findViewById(R.id.signup_epid)).setText(epid);
        ((EditText) findViewById(R.id.signup_password)).setText(_pw);
        EditText password2 = (EditText) findViewById(R.id.signup_password2);
        password2.setText(_pw);
        password2.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    signup2(null);
                    return true;
                }
                return false;
            }
        });
    }

    public void signup2(View view) {
        if (doing_phone) {
            step = S_PHONE;
            setContentView(R.layout.l_phone_1);
            ((Button) findViewById(R.id.back)).setText(android.R.string.cancel);
            findViewById(R.id.skip).setVisibility(View.GONE);
            loading = (ProgressBar) findViewById(R.id.loading);
            phoneInput = (IntlPhoneInput) findViewById(R.id.phone);
            phoneInput.setNumber(phone);
            phoneInput.setEmptyDefault(Locale.getDefault().getCountry());
            phoneInput.setOnKeyboardDone(new IntlPhoneInput.IntlPhoneInputListener() {
                @Override
                public void done(View view, boolean isValid) {
                    signup3(null);
                }
            });
        } else {
            epid = ((EditText) findViewById(R.id.signup_epid)).getText().toString();
            _pw = ((EditText) findViewById(R.id.signup_password)).getText().toString();
            String pwc = ((EditText) findViewById(R.id.signup_password2)).getText().toString();
            if (epid.length() == 0 || _pw.length() == 0)
                Toast.makeText(this, R.string.enterNewUNPW, Toast.LENGTH_SHORT).show();
            else if (pwc.length() == 0)
                Toast.makeText(this, R.string.confirmPassword, Toast.LENGTH_SHORT).show();
            else if (!pwc.equals(_pw))
                Toast.makeText(this, R.string.noPWMatch, Toast.LENGTH_SHORT).show();
            else if (!epid.matches("[a-zA-Z0-9._-]{1,30}"))
                Toast.makeText(this, R.string.unRegEx, Toast.LENGTH_LONG).show();
            else if (_pw.length() < 4)
                Toast.makeText(this, R.string.unsecurePW, Toast.LENGTH_SHORT).show();
            else {
                loading.setVisibility(View.VISIBLE);
                View v = getCurrentFocus();
                if (v != null)
                    ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
                new EndpointsHandler(this).check(new EndpointsHandler.APIRequest() {
                    @Override
                    public void onResult(Object result) {
                        loading.setVisibility(View.INVISIBLE);
                        if (result != null) {
                            Toast.makeText(WelcomeActivity.this, R.string.epidTaken, Toast.LENGTH_SHORT).show();
                        } else {
                            step = S_PHONE;
                            setContentView(R.layout.l_phone_1);
                            loading = (ProgressBar) findViewById(R.id.loading);
                            phoneInput = (IntlPhoneInput) findViewById(R.id.phone);
                            phoneInput.setNumber(phone);
                            phoneInput.setEmptyDefault(Locale.getDefault().getCountry());
                            phoneInput.setOnKeyboardDone(new IntlPhoneInput.IntlPhoneInputListener() {
                                @Override
                                public void done(View view, boolean isValid) {
                                    signup3(null);
                                }
                            });
                        }
                    }
                }, EndpointsHandler.PROFILE, EndpointsHandler.PR_EPID, epid);
            }
        }
    }

    public void signup3(View view) {
        if (!phoneInput.isValid())
            Toast.makeText(this, R.string.enterValidPhone, Toast.LENGTH_SHORT).show();
        else {
            phone = phoneInput.getNumber();
            code = String.format(Locale.UK, "%04d", new SecureRandom().nextInt(10000));
            loading.setVisibility(View.VISIBLE);
            View v = getCurrentFocus();
            if (v != null)
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
            new EndpointsHandler(this).check(new EndpointsHandler.APIRequest() {
                @Override
                public void onResult(Object result) {
                    if (result != null) {
                        phone = "";
                        code = "";
                        loading.setVisibility(View.INVISIBLE);
                        Toast.makeText(WelcomeActivity.this, R.string.phoneInUse, Toast.LENGTH_SHORT).show();
                    } else {
                        new EndpointsHandler(WelcomeActivity.this).sms(new EndpointsHandler.APIRequest() {
                            @Override
                            public void onResult(Object result) {
                                loading.setVisibility(View.INVISIBLE);
                                if (((Notice) result).getOk()) {
                                    step = S_CONFIRM_PHONE;
                                    setContentView(R.layout.l_phone_2);
                                    ((TextView) findViewById(R.id.phoneNotice)).setText(getString(R.string.enterCodeX, phone));
                                    loading = (ProgressBar) findViewById(R.id.loading);
                                    ((EditText) findViewById(R.id.code)).setOnEditorActionListener(new TextView.OnEditorActionListener() {
                                        @Override
                                        public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                            if (actionId == EditorInfo.IME_ACTION_DONE) {
                                                signup4(null);
                                                return true;
                                            }
                                            return false;
                                        }
                                    });
                                } else
                                    Toast.makeText(WelcomeActivity.this, R.string.errorToast, Toast.LENGTH_SHORT).show();
                            }
                        }, phone, getString(R.string.validateSMS) + code);
                    }
                }
            }, EndpointsHandler.PROFILE, EndpointsHandler.PR_PHONE, phone);
        }
    }

    public void skip_phone(View view) {
        code = "";
        phone = EndpointsHandler.NULL;
        signup4(null);
    }

    public void send_again(View view) {
        code = String.format(Locale.UK, "%04d", new SecureRandom().nextInt(10000));
        loading.setVisibility(View.VISIBLE);
        View v = getCurrentFocus();
        if (v != null)
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
        new EndpointsHandler(this).sms(new EndpointsHandler.APIRequest() {
            @Override
            public void onResult(Object result) {
                loading.setVisibility(View.INVISIBLE);
                if (!((Notice) result).getOk())
                    Toast.makeText(WelcomeActivity.this, R.string.errorToast, Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(WelcomeActivity.this, getString(R.string.newSMSCode) + phone, Toast.LENGTH_SHORT).show();
            }
        }, phone, R.string.validateSMS + code);
    }

    public void signup4(View view) {
        if (code.equals("") || ((EditText) findViewById(R.id.code)).getText().toString().equals(code)) {
            pw = LocalDatabaseHandler.sha256(_pw);
            loading.setVisibility(View.VISIBLE);
            View v = getCurrentFocus();
            if (v != null)
                ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(), 0);
            if (doing_phone)
                new EndpointsHandler(this).update(
                        new EndpointsHandler.APIRequest() {
                            @Override
                            public void onResult(Object result) {
                                loading.setVisibility(View.INVISIBLE);
                                if (result != null) {
                                    sp.edit().putString(LocalDatabaseHandler.PHONE, phone).apply();
                                    Bundle ab = new Bundle();
                                    ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                                    ab.putString(FIIDService.RESULT, FIIDService.OK);
                                    FirebaseAnalytics.getInstance(WelcomeActivity.this).logEvent(FIIDService.CONNECT_PHONE, ab);
                                    finish();
                                } else {
                                    Toast.makeText(WelcomeActivity.this, R.string.errorToast, Toast.LENGTH_SHORT).show();
                                    startActivity(getIntent());
                                    finish();
                                }
                            }
                        }, EndpointsHandler.PROFILE, EndpointsHandler.ID, Long.toString(sp.getLong(LocalDatabaseHandler.UID, 0L)),
                        new ArrayList<>(Collections.singletonList(EndpointsHandler.PR_PHONE)), new ArrayList<>(Collections.singletonList(phone)));
            else
                new EndpointsHandler(this).signup(new EndpointsHandler.APIRequest() {
                    @Override
                    public void onResult(Object result) {
                        loading.setVisibility(View.INVISIBLE);
                        if (result != null && ((Id) result).getId() != 0L) {
                            sp.edit().putLong(LocalDatabaseHandler.UID, ((Id) result).getId())
                                    .putString(LocalDatabaseHandler.EPID, epid)
                                    .putString(LocalDatabaseHandler.PASSWORD, pw)
                                    .putString(LocalDatabaseHandler.PHONE, phone.equals(EndpointsHandler.NULL) ? null : phone).apply();
                            startActivity(new Intent(WelcomeActivity.this, MainActivity.class));
                            Bundle ab = new Bundle();
                            ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                            ab.putString(FIIDService.RESULT, FIIDService.OK);
                            ab.putString(FIIDService.SIGNUP_PHONE, Boolean.toString(!phone.equals(EndpointsHandler.NULL)));
                            FirebaseAnalytics.getInstance(WelcomeActivity.this).logEvent(FIIDService.SIGNUP, ab);
                            finish();
                        } else {
                            Toast.makeText(WelcomeActivity.this, R.string.errorToast, Toast.LENGTH_SHORT).show();
                            Bundle ab = new Bundle();
                            ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                            ab.putString(FIIDService.RESULT, FIIDService.FAIL);
                            ab.putString(FIIDService.SIGNUP_PHONE, Boolean.toString(!phone.equals(EndpointsHandler.NULL)));
                            FirebaseAnalytics.getInstance(WelcomeActivity.this).logEvent(FIIDService.SIGNUP, ab);
                        }
                    }
                }, epid, pw, phone);
        } else
            Toast.makeText(this, R.string.wrongCode, Toast.LENGTH_SHORT).show();
    }

    public void back(View view) {
        switch (step) {
            case S_LOGIN:
            case S_SIGNUP:
                start();
                break;
            case S_FORGOT_PASSWORD:
            case S_RECOVER_PASSWORD:
                login1(null);
                break;
            case S_PHONE:
                if (doing_phone) {
                    finish();
                    Bundle ab = new Bundle();
                    ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                    ab.putString(FIIDService.RESULT, FIIDService.CANCEL);
                    FirebaseAnalytics.getInstance(this).logEvent(FIIDService.CONNECT_PHONE, ab);
                } else
                    signup1(null);
                break;
            case S_CONFIRM_PHONE:
                signup2(null);
                break;
            default:
                super.onBackPressed();
        }
    }

    @Override
    public void onBackPressed() {
        back(null);
    }
}
