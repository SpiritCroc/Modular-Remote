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
import android.content.SharedPreferences;
import android.os.Message;
import android.util.Log;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

public abstract class CustomPreferenceFragment extends PreferenceFragmentCompat {
    private static final String LOG_TAG = CustomPreferenceFragment.class.getSimpleName();

    @Override
    public boolean onPreferenceTreeClick(Preference preference) {
        Activity activity = getActivity();
        if (activity instanceof SettingsActivity) {
            return ((SettingsActivity) activity).onPreferenceClick(preference) ||
                    super.onPreferenceTreeClick(preference);
        } else {
            Log.w(LOG_TAG, "activity not instanceof SettingsActivity");
            return super.onPreferenceTreeClick(preference);
        }
    }

    protected void onGlobalKeyMessage(Message message) {}

    // Util methods

    protected int correctInteger(SharedPreferences sharedPreferences, String key, String value,
                                 int defaultValue){
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(key, String.valueOf(defaultValue));
            editor.apply();
            return defaultValue;
        }
    }
}
