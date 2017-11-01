package com.onnoeberhard.epotato;

import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.remoteconfig.FirebaseRemoteConfig;

public class ContactsActivity extends AppCompatActivity implements ActionMode.Callback {

    private Menu menu;

    private ContactFragment contactFragment;
    private ContactFragment followingFragment;
    private ViewPager viewPager;
    private boolean contactsPermission = true;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.a_contacts);
        final Toolbar mainToolbar = (Toolbar) findViewById(R.id.mainToolbar);
        setSupportActionBar(mainToolbar);
        contactFragment = new ContactFragment();
        Bundle receivedArgs = new Bundle();
        receivedArgs.putInt(ContactFragment.TYPE, ContactFragment.TYPE_CONTACTS);
        contactFragment.setArguments(receivedArgs);
        FirebaseRemoteConfig remoteConfig = FirebaseRemoteConfig.getInstance();
        remoteConfig.setDefaults(R.xml.remote_config_defaults);
        final boolean feed = remoteConfig.getBoolean("feed");
        if (feed) {
            followingFragment = new ContactFragment();
            Bundle feedArgs = new Bundle();
            feedArgs.putInt(ContactFragment.TYPE, ContactFragment.TYPE_FOLLOWING);
            followingFragment.setArguments(feedArgs);
        }
        viewPager = (ViewPager) findViewById(R.id.viewPager);
        viewPager.setAdapter(new FragmentPagerAdapter(getSupportFragmentManager()) {
            @Override
            public Fragment getItem(int position) {
                switch (position) {
                    case 0:
                        return contactFragment;
                    case 1:
                        return followingFragment;
                }
                return null;
            }

            @Override
            public CharSequence getPageTitle(int position) {
                switch (position) {
                    case 0:
                        return getString(R.string.contacts);
                    case 1:
                        return getString(R.string.following);
                }
                return null;
            }

            @Override
            public int getCount() {
                return feed ? 2 : 1;
            }
        });
        TabLayout tabLayout = (TabLayout) findViewById(R.id.mainTabs);
        if (feed) {
            tabLayout.setupWithViewPager(viewPager);
            for (int i = 0; i < tabLayout.getTabCount(); i++)
                tabLayout.getTabAt(i).setCustomView(new MainActivity.MyTabView(this, tabLayout.getTabAt(i)));
            tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
                @Override
                public void onTabSelected(TabLayout.Tab tab) {
                    if (tab.getCustomView() != null)
                        ((MainActivity.MyTabView) tab.getCustomView()).select();
                    viewPager.setCurrentItem(tab.getPosition());
                }

                @Override
                public void onTabUnselected(TabLayout.Tab tab) {
                    if (tab.getCustomView() != null)
                        ((MainActivity.MyTabView) tab.getCustomView()).unselect();
                }

                @Override
                public void onTabReselected(TabLayout.Tab tab) {
                }
            });
        } else tabLayout.setVisibility(View.GONE);
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
            ((FontTextView) titlebar.findViewById(R.id.title)).setText(R.string.people);
            getSupportActionBar().setCustomView(titlebar);
        }
        viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                if (menu != null) {
                    menu.findItem(R.id.checkcontacts).setVisible(position == 0 && !contactsPermission);
                    menu.findItem(R.id.reload).setVisible(position != 0 || contactsPermission);
                }
            }

            @Override
            public void onPageSelected(int i) {
            }

            @Override
            public void onPageScrollStateChanged(int i) {
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.menu = menu;
        getMenuInflater().inflate(R.menu.contacts, menu);
        contactsPermission = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED;
        menu.findItem(R.id.checkcontacts).setVisible(!contactsPermission);
        menu.findItem(R.id.reload).setVisible(contactsPermission);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.addepid:
                if (viewPager.getCurrentItem() == 0 && contactFragment.adapter != null)
                    contactFragment.adapter.addepid();
                else if (viewPager.getCurrentItem() == 1 && followingFragment.adapter != null)
                    followingFragment.adapter.follow();
                break;
            case R.id.checkcontacts:
            case R.id.reload:
                if (viewPager.getCurrentItem() == 0 && contactFragment.adapter != null)
                    ContactsAdapter.addPhoneContacts(true, this, contactFragment.adapter);
                else if (viewPager.getCurrentItem() == 1 && followingFragment.adapter != null)
                    ContactsAdapter.addPhoneContacts(true, this, followingFragment.adapter);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        ContactsAdapter.contactsPermissionRequestCallback(requestCode, grantResults, this, menu, contactFragment.adapter);
    }

    @Override
    public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
        actionMode.getMenuInflater().inflate(R.menu.contacts_actionmode, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
        switch (menuItem.getItemId()) {
            case R.id.send:
                contactFragment.adapter.sendPotato();
                break;
        }
        return false;
    }

    @Override
    public void onDestroyActionMode(ActionMode actionMode) {
        if (contactFragment.adapter != null)
            contactFragment.adapter.actionMode = null;
    }

    public static class ContactFragment extends Fragment {

        static String TYPE = "type";
        static int TYPE_CONTACTS = 0;
        static int TYPE_FOLLOWING = 1;

        int type;

        View loading;
        RecyclerView recycler;
        ContactsAdapter adapter;

        @Override
        public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
            View v = inflater.inflate(R.layout.f_contacts, container, false);
            type = getArguments().getInt(TYPE);
            loading = v.findViewById(R.id.loading);
            recycler = (RecyclerView) v.findViewById(R.id.contactsRecycler);
            adapter = new ContactsAdapter(type == TYPE_CONTACTS ? ContactsAdapter.CONTACTS_ACIVITY : ContactsAdapter.FOLLOWING, (AppCompatActivity) getActivity(), loading, recycler);
            recycler.setAdapter(adapter);
            recycler.setLayoutManager(new LinearLayoutManager(getActivity()));
            recycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                    super.onScrollStateChanged(recyclerView, newState);
                    if (newState == RecyclerView.SCROLL_STATE_IDLE)
                        adapter.notifyDataSetChanged();
                }
            });
            return v;
        }
    }
}
