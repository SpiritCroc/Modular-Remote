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

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceCategory;

import java.util.Arrays;

public class SettingsFragment extends CustomPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String KEY_APP_BEHAVIOUR = "pref_app_behaviour";
    private static final String KEY_GLOBAL_ACTIONS_SETTINGS = "pref_global_actions";
    private static final String KEY_APP_APPEARANCE = "pref_category_app_appearance";
    private static final String KEY_MORE_SPACE_SETTINGS = "pref_more_space";

    private ListPreference ringerModePreference;
    private ListPreference orientationPreference;
    private EditTextPreference blockSizePreference;
    private EditTextPreference blockSizeHeightPreference;
    private EditTextPreference blockSizeLandscapePreference;
    private EditTextPreference blockSizeHeightLandscapePreference;
    private EditTextPreference fragmentDefaultWidthPreference;
    private EditTextPreference fragmentDefaultHeightPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences);

        ringerModePreference = (ListPreference) findPreference(Preferences.CHANGE_RINGER_MODE);
        orientationPreference = (ListPreference) findPreference(Preferences.ORIENTATION);
        blockSizePreference = (EditTextPreference) findPreference(Preferences.BLOCK_SIZE);
        blockSizeLandscapePreference =
                (EditTextPreference) findPreference(Preferences.BLOCK_SIZE_LANDSCAPE);
        blockSizeHeightLandscapePreference =
                (EditTextPreference) findPreference(Preferences.BLOCK_SIZE_HEIGHT_LANDSCAPE);
        blockSizeHeightPreference =
                (EditTextPreference) findPreference(Preferences.BLOCK_SIZE_HEIGHT);
        fragmentDefaultWidthPreference =
                (EditTextPreference) findPreference(Preferences.FRAGMENT_DEFAULT_WIDTH);
        fragmentDefaultHeightPreference =
                (EditTextPreference) findPreference(Preferences.FRAGMENT_DEFAULT_HEIGHT);

        PreferenceCategory appAppearancePreference =
                (PreferenceCategory) findPreference(KEY_APP_APPEARANCE);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.KITKAT) {
            // Hide fullscreen features for pre-KitKat devices
            Preference moreSpacePreference = findPreference(KEY_MORE_SPACE_SETTINGS);
            appAppearancePreference.removePreference(moreSpacePreference);
        } else {
            // Hide pref to hide pager tab strip, as it is also included in preferences_more_space
            Preference hidePagerTabStrip = findPreference(Preferences.HIDE_PAGER_TAB_STRIP);
            appAppearancePreference.removePreference(hidePagerTabStrip);
        }
        if (Build.VERSION.SDK_INT < 21) {
            PreferenceCategory appBehaviourPreference =
                    (PreferenceCategory) findPreference(KEY_APP_BEHAVIOUR);
            Preference globalActionsPreference = findPreference(KEY_GLOBAL_ACTIONS_SETTINGS);
            appBehaviourPreference.removePreference(globalActionsPreference);
        }
    }

    private void init() {
        setRingerModeSummary(true);
        setOrientationSummary();
        setBlockSizeSummary();
        setBlockSizeHeightSummary();
        setBlockSizeLandscapeSummary();
        setBlockSizeHeightLandscapeSummary();
        setFragmentDefaultWidthPreference();
        setFragmentDefaultHeightPreference();
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
        if (Preferences.CHANGE_RINGER_MODE.equals(key)) {
            setRingerModeSummary(false);
        } else if (Preferences.ORIENTATION.equals(key)) {
            setOrientationSummary();
        } else if (Preferences.BLOCK_SIZE.equals(key)) {
            setBlockSizeSummary();
        } else if (Preferences.BLOCK_SIZE_HEIGHT.equals(key)) {
            setBlockSizeHeightSummary();
        } else if (Preferences.BLOCK_SIZE_LANDSCAPE.equals(key)) {
            setBlockSizeLandscapeSummary();
        } else if (Preferences.BLOCK_SIZE_HEIGHT_LANDSCAPE.equals(key)) {
            setBlockSizeHeightLandscapeSummary();
        } else if (Preferences.FRAGMENT_DEFAULT_WIDTH.equals(key)) {
            setFragmentDefaultWidthPreference();
        } else if (Preferences.FRAGMENT_DEFAULT_HEIGHT.equals(key)) {
            setFragmentDefaultHeightPreference();
        }

    }

    private void setRingerModeSummary(boolean init) {
        String value = ringerModePreference.getValue();

        // Make sure we have the required permission for changing ringer mode
        final String VALUE_OFF = getString(R.string.pref_ringer_mode_keep_value);
        if (!VALUE_OFF.equals(value)) {
            NotificationManager notificationManager = (NotificationManager) getActivity()
                    .getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    && !notificationManager.isNotificationPolicyAccessGranted()) {
                if (init) {
                    // Pretend that the setting is off so we don't need the permission
                    ringerModePreference.setValue(VALUE_OFF);
                    value = VALUE_OFF;
                } else {
                    // Request the permission
                    startActivity(new Intent(android.provider.Settings
                            .ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS));
                }
            }
        }

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

    private void setOrientationSummary() {
        String value = orientationPreference.getValue();
        Resources resources = getResources();
        String[] values = resources.getStringArray(R.array.pref_orientation_array_values);
        int index = Arrays.asList(values).indexOf(value);
        if (index < 0) {
            orientationPreference.setSummary("");
        } else {
            String[] summaries =
                    resources.getStringArray(R.array.pref_orientation_array);
            orientationPreference.setSummary(summaries[index]);
        }
        // Enable/disable dependent preferences
        boolean enablePortrait, enableLandscape;
        switch (value) {
            case Preferences.ORIENTATION_PORTRAIT_ONLY:
            case Preferences.ORIENTATION_SHARE_LAYOUT:
                enablePortrait = true;
                enableLandscape = false;
                break;
            case Preferences.ORIENTATION_LANDSCAPE_ONLY:
                enablePortrait = false;
                enableLandscape = true;
                break;
            default:
                enablePortrait = true;
                enableLandscape = true;
                break;

        }
        blockSizePreference.setEnabled(enablePortrait);
        blockSizeHeightPreference.setEnabled(enablePortrait);
        blockSizeLandscapePreference.setEnabled(enableLandscape);
        blockSizeHeightLandscapePreference.setEnabled(enableLandscape);
    }

    private void setBlockSizeSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.BLOCK_SIZE, blockSizePreference.getText(), 4);
        blockSizePreference.setSummary(getResources().getQuantityString(
                R.plurals.pref_block_size_summary, value, value));
    }

    private void setBlockSizeHeightSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.BLOCK_SIZE_HEIGHT, blockSizeHeightPreference.getText(), 6);
        blockSizeHeightPreference.setSummary(getResources().getQuantityString(
                R.plurals.pref_block_size_height_summary, value, value));
    }

    private void setBlockSizeLandscapeSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.BLOCK_SIZE, blockSizeLandscapePreference.getText(), 6);
        blockSizeLandscapePreference.setSummary(getResources().getQuantityString(
                R.plurals.pref_block_size_summary, value, value));
    }

    private void setBlockSizeHeightLandscapeSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.BLOCK_SIZE_HEIGHT, blockSizeHeightLandscapePreference.getText(), 4);
        blockSizeHeightLandscapePreference.setSummary(getResources().getQuantityString(
                R.plurals.pref_block_size_height_summary, value, value));
    }

    private void setFragmentDefaultWidthPreference() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.FRAGMENT_DEFAULT_WIDTH,
                fragmentDefaultWidthPreference.getText(), 3);
        fragmentDefaultWidthPreference.setSummary(
                getString(R.string.pref_fragment_default_width_summary, value));
    }

    private void setFragmentDefaultHeightPreference() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.FRAGMENT_DEFAULT_HEIGHT,
                fragmentDefaultHeightPreference.getText(), 2);
        fragmentDefaultHeightPreference.setSummary(
                getString(R.string.pref_fragment_default_height_summary, value));
    }
}
