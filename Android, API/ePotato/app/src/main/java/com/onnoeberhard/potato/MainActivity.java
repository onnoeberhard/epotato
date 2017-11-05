package com.onnoeberhard.potato;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.remoteconfig.FirebaseRemoteConfig;
import com.onnoeberhard.epotato.backend.potatoAPI.model.Notice;
import com.onnoeberhard.potato.inappbilling.IabHelper;
import com.onnoeberhard.potato.inappbilling.IabResult;
import com.onnoeberhard.potato.inappbilling.Inventory;

import java.io.IOException;
import java.util.ArrayList;

import me.leolin.shortcutbadger.ShortcutBadger;

import static com.onnoeberhard.potato.SendPotatoActivity.TSS;
import static com.onnoeberhard.potato.SendPotatoActivity.UIDS;

public class MainActivity extends AppCompatActivity {

    public static final String POTATO_RECEIVED = "EPOTATO_RECEIVED";
    public static final String POTATO_SENT = "EPOTATO_SENT";
    public static final String POTATO_FEED = "EPOTATO_FEED";
    public static boolean isVisible = false;
    private LocalDatabaseHandler ldb;
    private SharedPreferences sp;
    private TabLayout tabLayout;
    private IabHelper iabHelper;

    private BroadcastReceiver potato_receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            updateTabBadges();
            Toast.makeText(MainActivity.this, R.string.newPotato, Toast.LENGTH_SHORT).show();
        }
    };

    static void logout(Activity a) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(a);
        sp.edit().putLong(LocalDatabaseHandler.OLD_UID, sp.getLong(LocalDatabaseHandler.UID, 0L)).remove(LocalDatabaseHandler.UID).remove(LocalDatabaseHandler.EPID).remove(LocalDatabaseHandler.PASSWORD).remove(LocalDatabaseHandler.FIID).remove(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB).remove(LocalDatabaseHandler.FOLLOWERS).apply();
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(a));
        dispatcher.cancelAll();
        try {
            FirebaseInstanceId.getInstance().deleteInstanceId();
        } catch (IOException e) {
            e.printStackTrace();
        }
        gotoWelcome(a);
    }

    private static void gotoWelcome(Activity a) {
        a.startActivity(new Intent(a, WelcomeActivity.class));
        a.finish();
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_main);
        ldb = new LocalDatabaseHandler(this);
        sp = PreferenceManager.getDefaultSharedPreferences(this);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancelAll();
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(0);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(1);
        ((NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE)).cancel(2);
        if (!sp.contains(LocalDatabaseHandler.UID))
            gotoWelcome(this);
        else if (sp.contains(LocalDatabaseHandler.OLD_UID) && sp.getLong(LocalDatabaseHandler.OLD_UID, 0L) != sp.getLong(LocalDatabaseHandler.UID, 0L)) {
            new AlertDialog.Builder(this).setTitle(R.string.newUser)
                    .setMessage(R.string.newUserNotice)
                    .setPositiveButton(R.string.cont, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            new LocalDatabaseHandler(MainActivity.this).dropEverything();
                            sp.edit().remove(LocalDatabaseHandler.OLD_UID).remove(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB).apply();
                            gotoMain();
                            Bundle ab = new Bundle();
                            ab.putLong(FirebaseAnalytics.Param.VALUE, 1);
                            ab.putString(FIIDService.RESULT, FIIDService.OK);
                            FirebaseAnalytics.getInstance(MainActivity.this).logEvent(FIIDService.LOGIN_NEW_USER, ab);
                            dialog.dismiss();
                        }
                    })
                    .setNegativeButton(R.string.logout, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            sp.edit().remove(LocalDatabaseHandler.UID).remove(LocalDatabaseHandler.EPID).remove(LocalDatabaseHandler.PASSWORD).remove(LocalDatabaseHandler.FIID).apply();
                            gotoWelcome(MainActivity.this);
                            Bundle ab = new Bundle();
                            ab.putLong(FirebaseAnalytics.Param.VALUE, 0);
                            ab.putString(FIIDService.RESULT, FIIDService.CANCEL);
                            FirebaseAnalytics.getInstance(MainActivity.this).logEvent(FIIDService.LOGIN_NEW_USER, ab);
                            dialog.dismiss();
                        }
                    }).show();
        } else
            gotoMain();
    }

    private void checkGooglePlayServices() {
        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS)
            googleApiAvailability.makeGooglePlayServicesAvailable(this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        ContactsAdapter.contactsPermissionRequestCallback(requestCode, grantResults, this, null, null);
    }

    private void gotoMain() {
        if (getIntent().getStringArrayListExtra("sendto") != null) {
            Intent i = new Intent(this, AdActivity.class);
            i.putExtra(UIDS, getIntent().getStringArrayListExtra("sendto"));
            i.putExtra(TSS, new ArrayList<String>() {{
                for (int i = 0; i < getIntent().getStringArrayListExtra("sendto").size(); i++)
                    add(Boolean.toString(false));
            }});
            startActivity(i);
        }
        checkGooglePlayServices();
        if (!sp.contains(LocalDatabaseHandler.FIRST_PHONE_CONTACTS_JOB))
            ContactsAdapter.addPhoneContacts(true, this, null);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(new GooglePlayDriver(this));
        dispatcher.mustSchedule(dispatcher.newJobBuilder()
                .setService(MainService.class)
                .setTag(MainService.TAG)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(60 * 60, 60 * 60 + 10 * 60))
                .setReplaceCurrent(false)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setConstraints(Constraint.ON_ANY_NETWORK)
                .build());
        final Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowCustomEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
            getSupportActionBar().setCustomView(View.inflate(this, R.layout.v_titlebar, null));
        }
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setDefaults(R.xml.remote_config_defaults);
        final boolean feed = remoteConfig.getBoolean("feed");
        final HomeFragment receivedFragment = new HomeFragment();
        final Bundle receivedArgs = new Bundle();
        receivedArgs.putInt(HomeFragment.HOME_TYPE, HomeFragment.HOME_TYPE_RECEIVE);
        receivedFragment.setArguments(receivedArgs);
        final HomeFragment feedFragment = new HomeFragment();
        if (feed) {
            Bundle feedArgs = new Bundle();
            feedArgs.putInt(HomeFragment.HOME_TYPE, HomeFragment.HOME_TYPE_FEED);
            feedFragment.setArguments(feedArgs);
        }
        final HomeFragment sentFragment = new HomeFragment();
        Bundle sentArgs = new Bundle();
        sentArgs.putInt(HomeFragment.HOME_TYPE, HomeFragment.HOME_TYPE_SENT);
        sentFragment.setArguments(sentArgs);
        final ViewPager viewPager = (ViewPager) findViewById(R.id.mainViewPager);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return receivedFragment;
                    case 1:
                        return feed ? feedFragment : sentFragment;
                    case 2:
                        return sentFragment;
                }
                return null;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return getString(R.string.received);
                    case 1:
                        return feed ? getString(R.string.feed) : getString(R.string.sent);
                    case 2:
                        return getString(R.string.sent);
                }
                return null;
            }

            @Override
            public int getCount() {
                return feed ? 3 : 2;
            }
        });
        viewPager.setOffscreenPageLimit(viewPager.getAdapter().getCount() - 1);
        tabLayout = (TabLayout) findViewById(R.id.mainTabs);
        tabLayout.setupWithViewPager(viewPager);
        for (int i = 0; i < tabLayout.getTabCount(); i++)
            tabLayout.getTabAt(i).setCustomView(new MyTabView(this, tabLayout.getTabAt(i)));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                if (tab.getCustomView() != null)
                    ((MyTabView) tab.getCustomView()).select();
                viewPager.setCurrentItem(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                if (tab.getCustomView() != null)
                    ((MyTabView) tab.getCustomView()).unselect();
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
            }
        });
        updateTabBadges();
        if (getIntent().getBooleanExtra("feed", false))
            viewPager.setCurrentItem(1);
        new EndpointsHandler(this).login(new EndpointsHandler.APIRequest() {
            @Override
            public void onResult(Object result) {
                if (result != null && (((Notice) result).getCode() == EndpointsHandler.NOTICE_ERROR || ((Notice) result).getCode() == EndpointsHandler.NOTICE_NULL))
                    logout(MainActivity.this);
                else
                    FIIDService.updateFIEPID(MainActivity.this, null);
            }
        }, Long.toString(sp.getLong(LocalDatabaseHandler.UID, 0L)), sp.getString(LocalDatabaseHandler.PASSWORD, null));
        iabHelper = new IabHelper(this, getString(R.string.billingKey));
        iabHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
            @Override
            public void onIabSetupFinished(IabResult result) {
                if (result.isSuccess()) {
                    try {
                        iabHelper.queryInventoryAsync(new IabHelper.QueryInventoryFinishedListener() {
                            @Override
                            public void onQueryInventoryFinished(IabResult result, Inventory inv) {
                                if (result.isSuccess())
                                    sp.edit().putBoolean(LocalDatabaseHandler.PREMIUM, inv.hasPurchase(LocalDatabaseHandler.PREMIUM)).apply();
                            }
                        });
                    } catch (IabHelper.IabAsyncInProgressException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    public void sendPotato(View view) {
        startActivity(new Intent(this, AdActivity.class));
    }

    public void updateTabBadges() {
        if (tabLayout != null) {
            ((MyTabView) tabLayout.getTabAt(0).getCustomView()).setBadgeNumber(ldb.getAll(LocalDatabaseHandler.NEW_POTATOES).size());
            ((MyTabView) tabLayout.getTabAt(1).getCustomView()).setBadgeNumber(ldb.getAll(LocalDatabaseHandler.NEW_FEED_POTATOES).size() > 0 ? 1 : 0);
        }
        ShortcutBadger.applyCount(this, ldb.getAll(LocalDatabaseHandler.NEW_POTATOES).size());
    }

    public void tabBadgesMinusOne() {
        if (tabLayout != null)
            ((MyTabView) tabLayout.getTabAt(0).getCustomView()).setBadgeNumber(((MyTabView) tabLayout.getTabAt(0).getCustomView()).getBadgeNumber() - 1);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.home, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                startActivity(new Intent(this, SettingsActivity.class));
                break;
            case R.id.contacts:
                startActivity(new Intent(this, ContactsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
        try {
            unregisterReceiver(potato_receiver);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkGooglePlayServices();
        isVisible = true;
        registerReceiver(potato_receiver, new IntentFilter(POTATO_RECEIVED));
        updateTabBadges();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    static class MyTabView extends RelativeLayout {
        ImageView badge;
        int badgeNumber = 0;

        MyTabView(Context context, TabLayout.Tab tab) {
            super(context);
            inflate(context, R.layout.v_tab, this);
            TextView title = (TextView) findViewById(R.id.title);
            title.setText(tab.getText());
            setAlpha(tab.isSelected() ? 1f : 0.5f);
            badge = (ImageView) findViewById(R.id.badge);
        }

        void select() {
            setAlpha(1f);
        }

        void unselect() {
            setAlpha(0.5f);
        }

        int getBadgeNumber() {
            return badgeNumber;
        }

        void setBadgeNumber(int n) {
            badgeNumber = n;
            badge.setVisibility(n > 0 ? VISIBLE : GONE);
            if (n > 0)
                badge.setImageResource(n == 1 ? R.drawable.badge_1 : n == 2 ? R.drawable.badge_2 : n == 3 ? R.drawable.badge_3
                        : n == 4 ? R.drawable.badge_4 : n == 5 ? R.drawable.badge_5 : n == 6 ? R.drawable.badge_6 : n == 7 ? R.drawable.badge_7
                        : n == 8 ? R.drawable.badge_8 : n == 9 ? R.drawable.badge_9 : R.drawable.badge_9p);
        }
    }
}
