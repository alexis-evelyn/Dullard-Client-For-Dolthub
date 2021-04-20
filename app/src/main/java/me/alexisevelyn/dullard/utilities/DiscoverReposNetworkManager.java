package me.alexisevelyn.dullard.utilities;

import android.net.ConnectivityManager;
import android.net.Network;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

import me.alexisevelyn.dullard.R;

public class DiscoverReposNetworkManager extends ConnectivityManager.NetworkCallback {
    private final Runnable reposRunnable;
    private final AppCompatActivity context;

    public DiscoverReposNetworkManager(AppCompatActivity context, Runnable reposRunnable) {
        this.context = context;
        this.reposRunnable = reposRunnable;
    }

    @Override
    public void onAvailable(Network network) {
        // If Network Becomes Available And We Still Have The Placeholder Card, Update
        LinearLayout repoView = this.context.findViewById(R.id.repos);
        if (repoView.findViewById(R.id.placeholder_repo) != null) {
            Thread reposThread = new Thread(reposRunnable);
            reposThread.setName("Retrieving ReposList Network Came Online (DiscoverRepos)");
            reposThread.start();
        }
    }

    @Override
    public void onLost(Network network) {
        // We don't need to do anything when the network is lost right now.
        // Maybe we can add a message or something to alert the user later.
    }
}
