/*
 * Copyright (C) 2015 SpiritCroc
 * Email: spiritcroc@gmail.com
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.spiritcroc.modular_remote;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Inspired by the AOSP Browser
 */
public class ShortcutActivity extends Activity implements OnClickListener,
        AdapterView.OnItemClickListener {
    private String[] pageNames;
    private ArrayList<Long> pageIDs;
    private static final String LOG_TAG = ShortcutActivity.class.getSimpleName();

    // According to the AOSP browser code, there is no public string defining this intent so if Home
    // changes the value, I  have to update this string:
    private static final String INSTALL_SHORTCUT = "com.android.launcher.action.INSTALL_SHORTCUT";

    int firstLandscape = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setTitle(R.string.title_activity_shortcut);
        setContentView(R.layout.activity_shortcut);
        View cancel = findViewById(R.id.cancel);
        cancel.setOnClickListener(this);
        ListView pageListView = (ListView) findViewById(R.id.select_page_list);
        pageListView.setOnItemClickListener(this);
        pageIDs = new ArrayList<>();
        try {
            String[] pageNamesPortrait = MainActivity.getPageNamesFromRecreationKey(
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .getString(Preferences.SAVED_FRAGMENTS, ""), pageIDs);
            String[] pageNamesLandscape = MainActivity.getPageNamesFromRecreationKey(
                    PreferenceManager.getDefaultSharedPreferences(this)
                            .getString(Preferences.SAVED_FRAGMENTS_LANDSCAPE, ""), pageIDs);
            ArrayList<String> pageNameList = new ArrayList<>();
            pageNameList.addAll(Arrays.asList(pageNamesPortrait));
            firstLandscape = pageNameList.size();
            for (String name: pageNamesLandscape) {
                if (pageNameList.contains(name)) {
                    pageNameList.add(getString(R.string.redundant_page_name_landscape, name));
                } else {
                    pageNameList.add(name);
                }
            }
            pageNames = pageNameList.toArray(new String[pageNameList.size()]);
            pageListView.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                    pageNames));
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.error_could_not_load_pages,
                    Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "onCreate: Got exception while trying to get page names: " + e);
            finish();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.cancel:
                finish();
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView parent, View view, int position, long id) {
        try {
            Intent intent = new Intent(INSTALL_SHORTCUT);
            Intent shortcutIntent = new Intent(getApplicationContext(), MainActivity.class);
            shortcutIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            shortcutIntent.putExtra(MainActivity.EXTRA_SELECT_PAGE_ID, pageIDs.get(position));
            int orientation = (firstLandscape >= 0 && firstLandscape <= position) ?
                    MainActivity.FORCE_ORIENTATION_LANDSCAPE :
                    MainActivity.FORCE_ORIENTATION_PORTRAIT;
            shortcutIntent.putExtra(MainActivity.EXTRA_FORCE_ORIENTATION, orientation);
            intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
            intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, pageNames[position]);
            intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                    Intent.ShortcutIconResource.fromContext(getApplicationContext(),
                            R.mipmap.ic_launcher));
            setResult(RESULT_OK, intent);
        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), R.string.error_could_not_create_shortcut,
                    Toast.LENGTH_LONG).show();
            Log.e(LOG_TAG, "Got exception while trying to create launcher shortcut: " + e);
        }
        finish();
    }
}
