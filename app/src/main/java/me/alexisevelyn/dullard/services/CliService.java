package me.alexisevelyn.dullard.services;

import android.content.Intent;

import androidx.annotation.NonNull;
import androidx.core.app.JobIntentService;

// Cause Apparently The Threads Get Stopped When The Calling Activity Is Exited
public class CliService extends JobIntentService {
    @Override
    protected void onHandleWork(@NonNull Intent intent) {

    }
}
