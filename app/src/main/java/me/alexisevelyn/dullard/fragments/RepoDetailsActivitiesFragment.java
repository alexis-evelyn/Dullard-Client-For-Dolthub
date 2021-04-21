package me.alexisevelyn.dullard.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;

import me.alexisevelyn.dullard.R;

public class RepoDetailsActivitiesFragment extends Fragment {
    private static final String tagName = "DullardRepoDetailsActivitiesFragment";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_repo_details_activities, container, false);
    }
}
