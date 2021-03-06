/*
 * Copyright (C) 2016 SpiritCroc
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

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Message;
import android.text.TextUtils;

import androidx.preference.Preference;
import androidx.preference.SwitchPreference;

import de.spiritcroc.modular_remote.dialogs.GlobalActionDialog;

@TargetApi(21)
public class SettingsGlobalActionsFragment extends CustomPreferenceFragment
        implements Preference.OnPreferenceClickListener,
        SharedPreferences.OnSharedPreferenceChangeListener, GlobalActionDialog.ResultListener {

    private SwitchPreference enablePreference;
    private Preference volumeUpPreference;
    private Preference volumeDownPreference;

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        addPreferencesFromResource(R.xml.preferences_global_actions);

        enablePreference = (SwitchPreference) findPreference(Preferences.GLOBAL_ACTIONS_ENABLE);
        volumeUpPreference = findPreference(Preferences.GLOBAL_ACTION_VOLUME_UP);
        volumeDownPreference = findPreference(Preferences.GLOBAL_ACTION_VOLUME_DOWN);

        volumeUpPreference.setOnPreferenceClickListener(this);
        volumeDownPreference.setOnPreferenceClickListener(this);
    }

    private void init() {
        initGlobalActionsEnabled();
        setGlobalActionSummary(volumeUpPreference);
        setGlobalActionSummary(volumeDownPreference);
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
        if (Preferences.GLOBAL_ACTIONS_ENABLE.equals(key)) {
            setGlobalActionsEnabled(enablePreference.isChecked());
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        if (preference == volumeUpPreference) {
            new GlobalActionDialog()
                    .setValues(GlobalActionSetting.recoverFromRecreationKey(getPreferenceManager()
                            .getSharedPreferences().getString(
                                    Preferences.GLOBAL_ACTION_VOLUME_UP, ""),
                            TcpConnectionManager.getInstance(getActivity())),
                            getString(R.string.dialog_volume_up_command),
                            preference.getKey(), this)
                    .show(getFragmentManager(), "GlobalActionDialog");
        } else if (preference == volumeDownPreference) {
            new GlobalActionDialog()
                    .setValues(GlobalActionSetting.recoverFromRecreationKey(getPreferenceManager()
                            .getSharedPreferences().getString(
                                    Preferences.GLOBAL_ACTION_VOLUME_DOWN, ""),
                            TcpConnectionManager.getInstance(getActivity())),
                            getString(R.string.dialog_volume_down_command),
                            preference.getKey(), this)
                    .show(getFragmentManager(), "GlobalActionDialog");
        }
        return false;
    }

    private void initGlobalActionsEnabled() {
        ((BaseActivity) getActivity()).sendGlobalKeyServiceMsg(Message.obtain(null, GlobalKeyService.ACTION_REQUEST_VOL_CTRL_ACTIVE));
    }

    private void setGlobalActionsEnabled(boolean enabled) {
        // Keep old checked state for now; if switch successful, will be updated later
        //enablePreference.setChecked(!enabled);
        if (enabled) {
            GlobalActionHandler.enable((BaseActivity) getActivity());
        } else {
            GlobalActionHandler.disable((BaseActivity) getActivity());
        }
    }

    private void setGlobalActionSummary(Preference preference) {
        String summary = GlobalActionSetting.recoverFromRecreationKey(
                getPreferenceManager().getSharedPreferences().getString(preference.getKey(), ""),
                TcpConnectionManager.getInstance(getActivity()))
                .getSummary(getActivity());
        if (TextUtils.isEmpty(summary)) {
            summary = getString(R.string.dialog_select_command);
        }
        preference.setSummary(summary);
    }

    @Override
    public void onGlobalActionResult(GlobalActionSetting settings, String key) {
        getPreferenceManager().getSharedPreferences().edit()
                .putString(key, settings.getRecreationKey())
                .apply();
        setGlobalActionSummary(findPreference(key));
        if (enablePreference.isChecked()) {
            // Update values
            GlobalActionHandler.enable((BaseActivity) getActivity());
        }
    }

    @Override
    protected void onGlobalKeyMessage(Message message) {
        if (message.what == GlobalKeyService.RESPONSE_VOL_CTRL_ACTIVE) {
            enablePreference.setChecked((Boolean) message.obj);
        }
        super.onGlobalKeyMessage(message);
    }
}
