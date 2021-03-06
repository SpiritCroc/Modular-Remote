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
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.method.LinkMovementMethod;
import android.util.TypedValue;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.spiritcroc.modular_remote.Preferences;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.Util;

public class GreetingDialog extends DialogFragment {

    // Increment if content significantly changes and should be seen again
    public static int VERSION = 1;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        TextView view = new TextView(getActivity());
        int padding = getResources().getDimensionPixelSize(R.dimen.dialog_content_padding);
        view.setPadding(padding, padding, padding, padding);
        view.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        view.setText(R.string.dialog_greeting_message);
        view.setMovementMethod(LinkMovementMethod.getInstance());
        builder.setTitle(R.string.dialog_greeting)
                .setView(Util.scrollView(view))
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences preferences =
                                PreferenceManager.getDefaultSharedPreferences(getActivity());
                        preferences.edit().putInt(Preferences.SEEN_GREETING_VERSION, VERSION)
                                .apply();
                        if (SetupGridSizeDialog.shouldShow(preferences)) {
                            new SetupGridSizeDialog()
                                    .show(getFragmentManager(), "SetupGridSizeDialog");
                        }
                    }
                });

        return builder.create();
    }
}
