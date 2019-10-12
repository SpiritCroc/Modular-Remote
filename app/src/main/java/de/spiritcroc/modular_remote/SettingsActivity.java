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

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.preference.Preference;
import androidx.preference.PreferenceScreen;

public class SettingsActivity extends BaseActivity {

    private CustomPreferenceFragment preferenceFragment;

    private static final String EXTRA_PREFERENCE_FRAGMENT =
            "de.spiritcroc.modular_remote.extra.preference_fragment";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String preferenceKey = getIntent().getStringExtra(EXTRA_PREFERENCE_FRAGMENT);
        preferenceFragment = getNewPreferenceFragment(preferenceKey);
        getSupportFragmentManager().beginTransaction().replace(android.R.id.content, preferenceFragment)
                .commit();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(preferenceFragment.getPreferenceScreen().getTitle());
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public boolean onPreferenceClick(Preference preference) {
        if (preference instanceof PreferenceScreen) {
            String fragmentClass = preference.getFragment();
            if (fragmentClass != null) {
                startActivity(new Intent(this, SettingsActivity.class)
                        .putExtra(EXTRA_PREFERENCE_FRAGMENT, fragmentClass));
                return true;
            }
        }
        return false;
    }

    private CustomPreferenceFragment getNewPreferenceFragment(String preferenceFragment) {
        if (preferenceFragment != null) {
            try {
                return (CustomPreferenceFragment) Class.forName(preferenceFragment).newInstance();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException |
                    ClassCastException e) {
                e.printStackTrace();
            }
        }
        return new SettingsFragment();
    }

    @Override
    protected void onGlobalKeyMessage(Message message) {
        preferenceFragment.onGlobalKeyMessage(message);
        super.onGlobalKeyMessage(message);
    }
}
