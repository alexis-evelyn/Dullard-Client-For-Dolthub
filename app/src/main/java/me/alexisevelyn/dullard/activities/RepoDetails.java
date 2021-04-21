package me.alexisevelyn.dullard.activities;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TabHost;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import com.google.android.material.tabs.TabLayout;

import me.alexisevelyn.dullard.R;
import me.alexisevelyn.dullard.fragments.RepoDetailsActivitiesFragment;
import me.alexisevelyn.dullard.fragments.RepoDetailsCommitsFragment;
import me.alexisevelyn.dullard.fragments.RepoDetailsInfoFragment;
import me.alexisevelyn.dullard.fragments.RepoDetailsTablesFragment;
import me.alexisevelyn.dullard.utilities.HelperMethods;
import me.alexisevelyn.dullard.utilities.RepoDetailsHelper;

public class RepoDetails extends AppCompatActivity {
    private static final String tagName = "DullardRepoDetails";
    private RepoDetailsHelper helper;
    private TabLayout tabs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        HelperMethods.loadDayNightPreferences(getApplicationContext());
        setContentView(R.layout.activity_repo_details);

        // Initialize Default Fragment
        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        ft.replace(R.id.repo_details_tab_frame, new RepoDetailsInfoFragment());
        ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
        ft.commit();

        this.tabs = findViewById(R.id.repo_details_tabs);

        this.tabs.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                // called when tab selected
                Fragment fragment = null;
                switch (tab.getPosition()) {
                    case 0:
                        fragment = new RepoDetailsInfoFragment();
                        break;
                    case 1:
                        fragment = new RepoDetailsTablesFragment();
                        break;
                    case 2:
                        fragment = new RepoDetailsCommitsFragment();
                        break;
                    case 3:
                        fragment = new RepoDetailsActivitiesFragment();
                        break;
                }

                if (fragment == null)
                    return;

                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                ft.replace(R.id.repo_details_tab_frame, fragment);
                ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN);
                ft.commit();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // called when tab unselected
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                // called when a tab is reselected
            }
        });

        this.helper = new RepoDetailsHelper(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_repo_details, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        View view = getWindow().getDecorView().findViewById(android.R.id.content);

        if (id == R.id.action_settings) {
            HelperMethods.openSettings(view);

            return true;
        } else if (id == R.id.action_share_repo) {
            this.helper.shareRepo();
            return true;
        } else if (id == R.id.action_clone_repo) {
            this.helper.cloneRepo(view);
            return true;
        } else if (id == R.id.action_start_sql_server) {
            this.helper.startSQLServer(view);
            return true;
        } else if (id == R.id.action_stop_sql_server) {
            this.helper.stopSQLServer(view);
            return true;
        }

        return false;
    }
}