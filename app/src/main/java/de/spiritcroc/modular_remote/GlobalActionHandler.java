/*
 * Copyright (C) 2017 SpiritCroc
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
import android.os.Message;
import android.preference.PreferenceManager;

@TargetApi(21)
public abstract class GlobalActionHandler {
    private static final String LOG_TAG = GlobalActionHandler.class.getSimpleName();

    public static void enable(BaseActivity context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        Message volUpMsg = Message.obtain(null, GlobalKeyService.ACTION_ADD_ACTION, GlobalKeyService.EXTRA_ACTION_ID_VOLUME_UP, 0, sp.getString(Preferences.GLOBAL_ACTION_VOLUME_UP, ""));
        Message volDownMsg = Message.obtain(null, GlobalKeyService.ACTION_ADD_ACTION, GlobalKeyService.EXTRA_ACTION_ID_VOLUME_DOWN, 0, sp.getString(Preferences.GLOBAL_ACTION_VOLUME_DOWN, ""));
        context.sendGlobalKeyServiceMsg(volUpMsg);
        context.sendGlobalKeyServiceMsg(volDownMsg);
    }

    public static void disable(BaseActivity context) {
        Message msg = Message.obtain(null, GlobalKeyService.ACTION_REMOVE_ALL);
        context.sendGlobalKeyServiceMsg(msg);
    }

    /* todo (best to move all of this class to BaseActivity?)
    public static void updateSetting(Context context, GlobalActionSetting setting) {
        if (appContext == null) {
            Log.e(LOG_TAG, "updateSettings: missing context");
            return;
        }
        String key;
        if (setting == volumeUpSetting) {
            key = Preferences.GLOBAL_ACTION_VOLUME_UP;
        } else if (setting == volumeDownSetting) {
            key = Preferences.GLOBAL_ACTION_VOLUME_DOWN;
        } else {
            Log.w(LOG_TAG, "updateSetting: requested by unknown action " + setting);
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(appContext).edit()
                .putString(key, setting.getRecreationKey()).apply();
    }
    */
}
