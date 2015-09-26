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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;

public class SettingsMoreSpaceFragment extends CustomPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private CheckBoxPreference fullscreenPreference;
    private CheckBoxPreference hideNavBarPreference;
    private EditTextPreference systemUiTimeoutPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_more_space);

        systemUiTimeoutPreference =
                (EditTextPreference) findPreference(Preferences.KEY_SYSTEM_UI_TIMEOUT);
        fullscreenPreference = (CheckBoxPreference) findPreference(Preferences.KEY_FULLSCREEN);
        hideNavBarPreference =
                (CheckBoxPreference) findPreference(Preferences.KEY_HIDE_NAVIGATION_BAR);
    }

    private void init() {
        setSystemUiTimeoutSummary();
        setSystemUiTimeoutEnabled();
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
        init();
    }

    @Override
    public void onPause() {
        super.onPause();
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (Preferences.KEY_SYSTEM_UI_TIMEOUT.equals(key)) {
            setSystemUiTimeoutSummary();
        } else if (Preferences.KEY_FULLSCREEN.equals(key)) {
            setSystemUiTimeoutEnabled();
        } else if (Preferences.KEY_HIDE_NAVIGATION_BAR.equals(key)) {
            setSystemUiTimeoutEnabled();
        }
    }

    private void setSystemUiTimeoutSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.KEY_SYSTEM_UI_TIMEOUT, systemUiTimeoutPreference.getText(), 3);
        systemUiTimeoutPreference.setSummary(getResources()
                .getQuantityString(R.plurals.pref_system_ui_timeout, value, value));
    }

    private void setSystemUiTimeoutEnabled() {
        systemUiTimeoutPreference.setEnabled(fullscreenPreference.isChecked() ||
                hideNavBarPreference.isChecked());
    }
}
