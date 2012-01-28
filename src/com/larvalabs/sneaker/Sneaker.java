package com.larvalabs.sneaker;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

/**
 * @author John Watkinson
 */
public class Sneaker extends FragmentActivity {

    private SummaryFragment fragment;

    private static final String PREF_WELCOME = "welcome";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Util.setProjectName("SplinterNet");
        Util.setDebugMode(false);
        Database.initialize(this);
        Database db = Database.getDB();
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.getBoolean(PREF_WELCOME, false)) {
            db.createWelcomeData(this);
            preferences.edit().putBoolean(PREF_WELCOME, true).commit();
        }
        // Turn on to automatically populate with test data
//        if (db.getNumberOfPosts() == 0) {
////            db.createTestData(this);
//            db.createDemoData(this);
//        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_layout);
    }

    @Override
    public void onResume() {
        super.onResume();
    }

//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        menu.add("TEST DATA");
//        menu.add("DEMO DATA");
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getTitle().equals("TEST DATA")) {
            Database.getDB().createTestData(this);
        } else {
            Database.getDB().createDemoData(this);
        }
        if (fragment != null) {
            fragment.reload();
        }
        return true;
    }

    public void reload() {
        if (fragment != null) {
            fragment.reload();
        }
    }

    public void setFragment(SummaryFragment fragment) {
        this.fragment = fragment;
    }
}
