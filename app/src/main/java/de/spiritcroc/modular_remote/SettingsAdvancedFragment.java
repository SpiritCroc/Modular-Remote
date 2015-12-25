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
import android.preference.EditTextPreference;

public class SettingsAdvancedFragment extends CustomPreferenceFragment
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private EditTextPreference offscreenPageLimitPreference;
    private EditTextPreference timeUpdateIntervalPreference;
    private EditTextPreference checkConnectivityIntervalPreference;
    private EditTextPreference doubleClickTimeoutPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences_advanced);

        offscreenPageLimitPreference =
                (EditTextPreference) findPreference(Preferences.OFFSCREEN_PAGE_LIMIT);
        timeUpdateIntervalPreference =
                (EditTextPreference) findPreference(Preferences.TIME_UPDATE_INTERVAL);
        checkConnectivityIntervalPreference =
                (EditTextPreference) findPreference(Preferences.CHECK_CONNECTIVITY_INTERVAL);
        doubleClickTimeoutPreference =
                (EditTextPreference) findPreference(Preferences.DOUBLE_CLICK_TIMEOUT);
    }

    private void init() {
        setOffscreenPageLimitSummary();
        setTimeUpdateIntervalSummary();
        setCheckConnectivityIntervalSummary();
        setDoubleClickTimeoutSummary();
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
        if (Preferences.OFFSCREEN_PAGE_LIMIT.equals(key)) {
            setOffscreenPageLimitSummary();
        } else if (Preferences.TIME_UPDATE_INTERVAL.equals(key)) {
            setTimeUpdateIntervalSummary();
        } else if (Preferences.CHECK_CONNECTIVITY_INTERVAL.equals(key)) {
            setCheckConnectivityIntervalSummary();
        } else if (Preferences.DOUBLE_CLICK_TIMEOUT.equals(key)) {
            setDoubleClickTimeoutSummary();
        }
    }

    private void setOffscreenPageLimitSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.OFFSCREEN_PAGE_LIMIT, offscreenPageLimitPreference.getText(), 2);
        if (value < 0) {
            offscreenPageLimitPreference
                    .setSummary(getString(R.string.pref_offscreen_page_limit_never));
        } else {
            offscreenPageLimitPreference.setSummary(getResources().getQuantityString(
                    R.plurals.pref_offscreen_page_limit, value, value));
        }
    }

    private void setTimeUpdateIntervalSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.TIME_UPDATE_INTERVAL, timeUpdateIntervalPreference.getText(), 500);
        timeUpdateIntervalPreference.setSummary(getResources()
                .getQuantityString(R.plurals.pref_time_update_interval, value, value));
    }

    private void setCheckConnectivityIntervalSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.CHECK_CONNECTIVITY_INTERVAL,
                checkConnectivityIntervalPreference.getText(), 3000);
        checkConnectivityIntervalPreference.setSummary(getResources()
                .getQuantityString(R.plurals.pref_check_connectivity_interval_summary, value,
                        value));
    }

    private void setDoubleClickTimeoutSummary() {
        int value = correctInteger(getPreferenceManager().getSharedPreferences(),
                Preferences.DOUBLE_CLICK_TIMEOUT, doubleClickTimeoutPreference.getText(), 500);
        doubleClickTimeoutPreference.setSummary(getResources()
                .getQuantityString(R.plurals.pref_double_click_timeout_summary, value, value));
        // Apply change
        TimeSingleton.getInstance(value);
    }
}
