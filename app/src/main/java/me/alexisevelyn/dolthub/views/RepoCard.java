package me.alexisevelyn.dolthub.views;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.material.card.MaterialCardView;

import me.alexisevelyn.dolthub.R;
import me.alexisevelyn.dolthub.utilities.HelperMethods;

public class RepoCard extends MaterialCardView {
    private Context context;
    private String tagName = "RepoCard";

    // Do I need these?
    private String owner = null;
    private String repo = null;
    private String description = null;
    private String size = null;

    private long sizeRaw = 0L;
    private int stars = 0;
    private int forks = 0;
    private long timeStamp = 0L;

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
        TypedArray view_set_keys = context.obtainStyledAttributes(attributeSet, R.styleable.repo_card);

        CharSequence owner_text = view_set_keys.getString(R.styleable.repo_card_owner_text);
        CharSequence repo_text = view_set_keys.getString(R.styleable.repo_card_repo_text);
        CharSequence description_text = view_set_keys.getString(R.styleable.repo_card_description_text);
        CharSequence size = view_set_keys.getString(R.styleable.repo_card_size_long);
        boolean placeholderCard = view_set_keys.getBoolean(R.styleable.repo_card_placeholder_card, false);

        Log.e(tagName, "OWNER TEXT: " + owner_text);
        Log.e(tagName, "DESC TEXT: " + description_text);

        if (owner_text != null)
            this.setOwner(owner_text.toString());

        if (repo_text != null)
            this.setRepo(repo_text.toString());

        if (description_text != null)
            this.setDescription(description_text.toString());

        if (size != null)
            this.setSize(Long.parseLong(size.toString()));

        // TODO: Rewrite Once Actual Card Design Is Made
        if (placeholderCard) {
            View ownerView = findViewById(R.id.owner_view);
            View repoView = findViewById(R.id.repo_view);
            View sizeView = findViewById(R.id.size_view);
            View descriptionView = findViewById(R.id.description_view);

            ownerView.setVisibility(GONE);
            repoView.setVisibility(GONE);
            sizeView.setVisibility(GONE);

            // Doesn't seem to make a difference, the code works, just the size is the same in this state
            ViewGroup.LayoutParams descriptionLayoutParams = descriptionView.getLayoutParams();
            descriptionLayoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;

            descriptionView.setLayoutParams(descriptionLayoutParams);
        }

        view_set_keys.recycle();
    }

    public void setOwner(String owner) {
        this.owner = owner;

        TextView ownerView = findViewById(R.id.owner_view);
        ownerView.setText(this.owner);
    }

    public void setRepo(String repo) {
        this.repo = repo;

        TextView repoView = findViewById(R.id.repo_view);
        repoView.setText(this.repo);
    }

    public void setDescription(String description) {
        this.description = description;

        TextView descriptionView = findViewById(R.id.description_view);
        descriptionView.setText(this.description);
    }

    public void setSize(Long sizeRaw) {
        this.sizeRaw = sizeRaw;
        this.size = HelperMethods.humanReadableByteCountSI(this.sizeRaw); // TODO: Allow user to choose units

        TextView sizeButton = findViewById(R.id.size_view);
        sizeButton.setText(size);
    }

    public void setStars(int stars) {
        this.stars = stars;

        String starText = String.format(this.context.getString(R.string.star_fork_card_format), this.stars, this.context.getString(R.string.stars_icon));

        TextView starView = findViewById(R.id.stars_view);
        starView.setText(starText);
    }

    public void setForks(int forks) {
        this.forks = forks;

        String forkText = String.format(this.context.getString(R.string.star_fork_card_format), this.forks, this.context.getString(R.string.forks_icon));

        TextView forksView = findViewById(R.id.forks_view);
        forksView.setText(forkText);
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;

//        Button sizeButton = findViewById(R.id.size_button);
//        sizeButton.setText(size);
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
