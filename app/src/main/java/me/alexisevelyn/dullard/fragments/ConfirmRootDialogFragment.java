package me.alexisevelyn.dullard.fragments;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import me.alexisevelyn.dullard.R;

// We Use A Custom Dialog To Ensure Overlaid Apps Do Not Interfere With The User's Interactions
public class ConfirmRootDialogFragment extends DialogFragment {
    private View.OnClickListener positiveButtonCallback = null;
    private View.OnClickListener negativeButtonCallback = null;

    public void setPositiveButtonCallback(View.OnClickListener positiveButtonCallback) {
        this.positiveButtonCallback = positiveButtonCallback;
    }

    public void setNegativeButtonCallback(View.OnClickListener negativeButtonCallback) {
        this.negativeButtonCallback = negativeButtonCallback;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Context context = getContext();
        FragmentActivity fragmentActivity = getActivity();

        // If It's Null, It's Probably Better The Dialog Crashes
        //   So We Can Prevent A Malicious Activity From Trying To Activate Root Privileges
        assert fragmentActivity != null;
        assert context != null;

        LayoutInflater inflater = fragmentActivity.getLayoutInflater();
        AlertDialog.Builder confirmRootDialogBuilder = new AlertDialog.Builder(context);

        View confirmationWarning = inflater.inflate(R.layout.fragment_root_confirmation, null);
        Button allowRootButton = confirmationWarning.findViewById(R.id.allow_root_confirmation);
        Button denyRootButton = confirmationWarning.findViewById(R.id.deny_root_confirmation);

        confirmationWarning.setFilterTouchesWhenObscured(true);
        allowRootButton.setFilterTouchesWhenObscured(true);
        denyRootButton.setFilterTouchesWhenObscured(true);

        allowRootButton.setOnClickListener(this.positiveButtonCallback);
        denyRootButton.setOnClickListener(this.negativeButtonCallback);

        confirmRootDialogBuilder.setView(confirmationWarning);
        return confirmRootDialogBuilder.create();
    }
}