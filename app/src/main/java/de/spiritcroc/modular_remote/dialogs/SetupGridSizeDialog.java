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

package de.spiritcroc.modular_remote.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import de.spiritcroc.modular_remote.Preferences;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.Util;

public class SetupGridSizeDialog extends DialogFragment {
    private static final String LOG_TAG = SetupGridSizeDialog.class.getSimpleName();

    private int suggestX;
    private int suggestY;
    private EditText editWidth;
    private EditText editHeight;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_setup_grid_size, null);
        suggestX = Util.suggestBlockSize(activity, false);
        suggestY = Util.suggestBlockSize(activity, true);
        if (suggestX > suggestY) {
            // Swap, as we assume portrait settings
            int tmp = suggestX;
            suggestX = suggestY;
            suggestY = tmp;
        }
        editWidth = (EditText) view.findViewById(R.id.edit_portrait_width);
        editHeight = (EditText) view.findViewById(R.id.edit_portrait_height);
        editWidth.setHint(getString(R.string.dialog_setup_grid_size_suggest, suggestX));
        editHeight.setHint(getString(R.string.dialog_setup_grid_size_suggest, suggestY));
        builder.setTitle(R.string.dialog_setup_grid_size_title)
                .setView(Util.scrollView(view))
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int x;
                        try {
                            x = Integer.parseInt(editWidth.getText().toString());
                        } catch (Exception e) {
                            Log.i(LOG_TAG, "Invalid width: " + editWidth.getText() +
                                    "; use suggested width instead");
                            x = -1;
                        }
                        if (x <= 0) {
                            x = suggestX;
                        }
                        int y;
                        try {
                            y = Integer.parseInt(editHeight.getText().toString());
                        } catch (Exception e) {
                            Log.i(LOG_TAG, "Invalid height: " + editHeight.getText() +
                                    "; use suggested height instead");
                            y = -1;
                        }
                        if (y <= 0) {
                            y = suggestY;
                        }
                        PreferenceManager.getDefaultSharedPreferences(activity)
                                .edit()
                                .putString(Preferences.KEY_BLOCK_SIZE, ""  + x)
                                .putString(Preferences.KEY_BLOCK_SIZE_HEIGHT, "" + y)
                                .putString(Preferences.KEY_BLOCK_SIZE_LANDSCAPE, "" + y)
                                .putString(Preferences.KEY_BLOCK_SIZE_HEIGHT_LANDSCAPE, "" + x)
                                .apply();
                    }
                });

        return builder.create();
    }

    public static boolean shouldShow(SharedPreferences preferences) {
        return "".equals(preferences.getString(Preferences.KEY_BLOCK_SIZE, ""));
    }
}
