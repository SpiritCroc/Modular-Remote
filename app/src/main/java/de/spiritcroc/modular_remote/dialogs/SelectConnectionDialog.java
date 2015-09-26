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

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;

public class SelectConnectionDialog extends DialogFragment {
    private int selection = 0;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final String[] items = TcpConnectionManager.getInstance(getActivity()
                .getApplicationContext()).getConnectionSuggestions();

        return builder.setTitle(R.string.dialog_select_connection)
                .setSingleChoiceItems(items, selection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selection = which;
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                })
                .setPositiveButton(R.string.dialog_edit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new EditConnectionDialog().setConnection(TcpConnectionManager
                                .getInstance(getActivity().getApplicationContext())
                                .getTcpConnection(items[selection]))
                                .show(getFragmentManager(), "EditConnectionDialog");
                    }
                })
                .setNeutralButton(R.string.dialog_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new RemoveConnectionDialog().setConnection(TcpConnectionManager
                                .getInstance(getActivity().getApplicationContext())
                                .getTcpConnection(items[selection]))
                                .show(getFragmentManager(), "RemoveConnectionDialog");
                    }
                })
                .create();
    }
}
