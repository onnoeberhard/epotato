package com.onnoeberhard.epotato;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.NativeExpressAdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import static com.onnoeberhard.epotato.SendPotatoActivity.REPLY;
import static com.onnoeberhard.epotato.SendPotatoActivity.TSS;
import static com.onnoeberhard.epotato.SendPotatoActivity.UIDS;

public class AdActivity extends AppCompatActivity {

    static boolean stay = true;
    private boolean a_ad = false;
    private boolean a_click = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        stay = true;
        Intent i = new Intent(this, SendPotatoActivity.class);
        i.putExtra(UIDS, getIntent().getStringArrayListExtra(UIDS));
        i.putExtra(TSS, getIntent().getStringArrayListExtra(TSS));
        i.putExtra(REPLY, getIntent().getBooleanExtra(REPLY, false));
        startActivity(i);
        if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(LocalDatabaseHandler.PREMIUM, false))
            close(null);
        else {
            setContentView(R.layout.a_ad);
            final Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
            setSupportActionBar(mainToolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setDisplayShowCustomEnabled(true);
                getSupportActionBar().setDisplayShowTitleEnabled(false);
                View titlebar = View.inflate(this, R.layout.v_titlebar, null);
                ((FontTextView) titlebar.findViewById(R.id.title)).setFont(FontTextView.BARIOL);
                ((FontTextView) titlebar.findViewById(R.id.title)).setTextSize(22);
                ((FontTextView) titlebar.findViewById(R.id.title)).setText(R.string.ad);
                getSupportActionBar().setCustomView(titlebar);
            }
            MobileAds.initialize(this, getString(R.string.adCode));
            NativeExpressAdView adView = (NativeExpressAdView) findViewById(R.id.adView);
            adView.loadAd(new AdRequest.Builder().build());
            adView.setAdListener(new AdListener() {
                @Override
                public void onAdOpened() {
                    super.onAdOpened();
                    a_click = true;
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!stay) close(null);
        else {
            a_ad = true;
            stay = false;
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (a_ad) {
            Bundle ab = new Bundle();
            ab.putLong(FirebaseAnalytics.Param.VALUE, a_click ? 1 : 0);
            ab.putString(FIIDService.RESULT, a_click ? FIIDService.OK : FIIDService.CANCEL);
            FirebaseAnalytics.getInstance(this).logEvent(FIIDService.AD, ab);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        stay = false;
    }

    public void close(View view) {
        finish();
    }
}
