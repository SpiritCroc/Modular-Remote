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

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;

public class RemoveConnectionDialog extends DialogFragment {
    private final static String LOG_TAG = RemoveConnectionDialog.class.getSimpleName();

    private TcpConnectionManager.TcpConnection connection;

    public RemoveConnectionDialog setConnection(TcpConnectionManager.TcpConnection connection) {
        this.connection = connection;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        String ip;
        if (connection == null) {
            Log.e(LOG_TAG, "connection == null");
            dismiss();
            ip = "";
        } else {
            ip = connection.getIp();
        }

        return builder.setTitle(R.string.dialog_remove_connection)
                .setMessage(getString(R.string.dialog_remove_connection_message, ip))
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only dismiss
                    }
                }).setPositiveButton(R.string.dialog_remove, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (connection != null) {
                            TcpConnectionManager.getInstance(getActivity().getApplicationContext())
                                    .removeConnection(connection);
                        }
                    }
                }).create();
    }
}
