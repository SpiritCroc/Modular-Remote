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
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.Util;

public class AboutDialog extends DialogFragment {
    private static final String LOG_TAG = AboutDialog.class.getSimpleName();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        View view = activity.getLayoutInflater().inflate(R.layout.dialog_about, null);
        String text = getString(R.string.dialog_about_app_version) + ": ";
        try {
            text += activity.getPackageManager().getPackageInfo(activity.getPackageName(), 0)
                    .versionName;
        } catch (PackageManager.NameNotFoundException e) {
            text = "";
            Log.w(LOG_TAG, "Got exception " + e);
        }
        ((TextView) view.findViewById(R.id.version_view)).setText(text);
        builder.setView(Util.scrollView(view))
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                })
                .setNeutralButton(R.string.dialog_greeting, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new GreetingDialog().show(getFragmentManager(), "GreetingDialog");
                    }
                });

        return builder.create();
    }
}
