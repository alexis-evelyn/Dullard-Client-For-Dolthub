package me.alexisevelyn.dolthub;

import android.os.Bundle;

import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;

public class ScrollingActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scrolling);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        CollapsingToolbarLayout toolBarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolBarLayout.setTitle(getTitle());

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_scrolling, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        } else if (id == R.id.action_version_test) {
            Cli cli = new Cli();
            String version = cli.getVersion(getApplicationContext());
            View view = getWindow().getDecorView().findViewById(android.R.id.content);

            Snackbar.make(view, version, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            return true;
        } else if (id == R.id.action_clone_repo_test) {
            Cli cli = new Cli();
            cli.cloneRepo(getApplicationContext());
            View view = getWindow().getDecorView().findViewById(android.R.id.content);

//            Snackbar.make(view, version, Snackbar.LENGTH_LONG)
//                    .setAction("Action", null).show();

            return true;
        } else if (id == R.id.action_read_rows_test) {
            Cli cli = new Cli();
            String rows = cli.readRows(getApplicationContext());
            View view = getWindow().getDecorView().findViewById(android.R.id.content);

            Snackbar.make(view, rows, Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();

            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}