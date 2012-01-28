package com.larvalabs.sneaker;

import android.app.Activity;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;

/**
 * @author John Watkinson
 */
public class DetailsActivity extends FragmentActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            // During initial setup, plug in the details fragment.
            //DetailsFragment details = DetailsFragment.newInstance(getIntent().getIntExtra(DetailsFragment.ARG_INDEX, 0), getIntent().getStringExtra(DetailsFragment.ARG_SEARCH));
            DetailsFragment details = new DetailsFragment();
            details.setArguments(getIntent().getExtras());
            getSupportFragmentManager().beginTransaction().add(android.R.id.content, details).commit();
        }
    }

}
