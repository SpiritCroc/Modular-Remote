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
import android.os.Bundle;
import android.util.Log;

import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.R;

public class OverlapWarningDialog extends DialogFragment {
    private static String LOG_TAG = OverlapWarningDialog.class.getSimpleName();

    private MainActivity callback;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(R.string.dialog_warning)
                .setMessage(R.string.dialog_overlapping_fragments_warning)
                .setPositiveButton(R.string.dialog_change, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback == null) {
                            Log.e(LOG_TAG, "callback == null");
                        } else {
                            callback.enterEditMode();
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_ignore, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (callback == null) {
                            Log.e(LOG_TAG, "callback == null");
                        } else {
                            callback.exitEditMode();
                        }
                    }
                });

        return builder.create();
    }

    public OverlapWarningDialog setCallback(MainActivity callback) {
        this.callback = callback;
        return this;
    }
}
