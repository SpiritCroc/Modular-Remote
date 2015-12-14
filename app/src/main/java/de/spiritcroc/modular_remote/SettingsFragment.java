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
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceCategory;

import java.util.Arrays;

public class SettingsFragment extends CustomPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String KEY_APP_APPEARANCE = "pref_category_app_appearance";
    private static final String KEY_MORE_SPACE_SETTINGS = "pref_more_space";

    private ListPreference ringerModePreference;
    private EditTextPreference blockSizePreference;
    private EditTextPreference blochSizeHeightPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);

        ringerModePreference = (ListPreference) findPreference(Preferences.KEY_CHANGE_RINGER_MODE);
        blockSizePreference = (EditTextPreference) findPreference(Preferences.KEY_BLOCK_SIZE);
        blochSizeHeightPreference =
                (EditTextPreference) findPreference(Preferences.KEY_BLOCK_SIZE_HEIGHT);

        PreferenceCategory appAppearancePreference =
                (PreferenceCategory) findPreference(KEY_APP_APPEARANCE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Hide fullscreen features for pre-KitKat devices
            Preference moreSpacePreference = findPreference(KEY_MORE_SPACE_SETTINGS);
            appAppearancePreference.removePreference(moreSpacePreference);
        } else {
            // Hide pref to hide pager tab strip, as it is also included in preferences_more_space
            Preference hidePagerTabStrip = findPreference(Preferences.KEY_HIDE_PAGER_TAB_STRIP);
            appAppearancePreference.removePreference(hidePagerTabStrip);
        }

        setRingerModeSummary();
    }

    private void init() {
        setRingerModeSummary();
        setBlockSizeSummary();
        setBlochSizeHeightSummary();
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
        if (Preferences.KEY_CHANGE_RINGER_MODE.equals(key)) {
            setRingerModeSummary();
        } else if (Preferences.KEY_BLOCK_SIZE.equals(key)) {
            setBlockSizeSummary();
        } else if (Preferences.KEY_BLOCK_SIZE_HEIGHT.equals(key)) {
            setBlochSizeHeightSummary();
        }

    }

    private void setRingerModeSummary() {
        String value = ringerModePreference.getValue();
        Resources resources = getResources();
        String[] values = resources.getStringArray(R.array.pref_change_ringer_mode_array_values);
        int index = Arrays.asList(values).indexOf(value);
        if (index < 0) {
            ringerModePreference.setSummary("");
        } else {
            String[] summaries =
                    resources.getStringArray(R.array.pref_change_ringer_mode_array_summaries);
            ringerModePreference.setSummary(summaries[index]);
        }
    }

    private void setBlockSizeSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.KEY_BLOCK_SIZE, blockSizePreference.getText(), 4);
        blockSizePreference.setSummary(getResources().getQuantityString(
                R.plurals.pref_block_size_summary, value, value));
    }

    private void setBlochSizeHeightSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.KEY_BLOCK_SIZE_HEIGHT, blochSizeHeightPreference.getText(), 6);
        blochSizeHeightPreference.setSummary(getResources().getQuantityString(
                R.plurals.pref_block_size_height_summary, value, value));
    }
}
