package me.alexisevelyn.dolthub.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.Button;

import com.google.android.material.card.MaterialCardView;

import me.alexisevelyn.dolthub.R;

public class RepoCard extends MaterialCardView {
    private Context context;
    private String tagName = "RepoCard";

    public RepoCard(Context context) {
        super(context);

        this.context = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.repo_card_view, this);
    }

    public RepoCard(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);

        this.context = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.repo_card_view, this);

        setAttributeSet(attributeSet);
    }

    // Apparently Never Called By Android, But The Int Is A Style (Or Theme?) ID - See: https://stackoverflow.com/a/30200015/6828099
    public RepoCard(Context context, AttributeSet attributeSet, int defStyleAttr) {
        super(context, attributeSet, defStyleAttr);

        this.context = context;

        LayoutInflater inflater = LayoutInflater.from(context);
        inflater.inflate(R.layout.repo_card_view, this);

        setAttributeSet(attributeSet);
    }

    // This sets the attributes from XML
    private void setAttributeSet(AttributeSet attributeSet) {
        TypedArray view_set_keys = context.obtainStyledAttributes(attributeSet, R.styleable.RepoCard);

        CharSequence owner_text = view_set_keys.getString(R.styleable.RepoCard_owner_text);
        CharSequence repo_text = view_set_keys.getString(R.styleable.RepoCard_repo_text);
        CharSequence description_text = view_set_keys.getString(R.styleable.RepoCard_description_text);
        CharSequence size_text = view_set_keys.getString(R.styleable.RepoCard_size_text);

        Log.e(tagName, "OWNER TEXT: " + owner_text);
        Log.e(tagName, "DESC TEXT: " + description_text);

        if (owner_text != null)
            this.setOwner(owner_text.toString());

        if (repo_text != null)
            this.setRepo(repo_text.toString());

        if (description_text != null)
            this.setDescription(description_text.toString());

        if (size_text != null)
            this.setSize(size_text.toString());

        view_set_keys.recycle();
    }

    public void setOwner(String owner) {
        Button ownerButton = findViewById(R.id.owner_button);
        ownerButton.setText(owner);
    }

    public void setRepo(String repo) {
        Button repoButton = findViewById(R.id.repo_button);
        repoButton.setText(repo);
    }

    public void setDescription(String description) {
        Button descriptionButton = findViewById(R.id.description_button);
        descriptionButton.setText(description);
    }

    public void setSize(String size) {
        Button sizeButton = findViewById(R.id.size_button);
        sizeButton.setText(size);
    }

    // Yes, I know passing this method to MaterialCardView is cheesing it,
    //   but why rewrite this when I don't have to?
    @Override
    public void setTag(int key, Object tag) {
        MaterialCardView repoCard = findViewById(R.id.repo_card);
        repoCard.setTag(key, tag);
    }

    // Yes, I know passing this method to MaterialCardView is cheesing it,
    //   but why rewrite this when I don't have to?
    @Override
    public void setOnClickListener(OnClickListener l) {
        MaterialCardView repoCard = findViewById(R.id.repo_card);
        repoCard.setOnClickListener(l);
    }
}
