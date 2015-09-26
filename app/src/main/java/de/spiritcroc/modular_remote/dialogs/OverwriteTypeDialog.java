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
import de.spiritcroc.modular_remote.ReceiverIpSelectorUser;
import de.spiritcroc.modular_remote.TcpConnectionManager;

public class OverwriteTypeDialog extends DialogFragment {
    private TcpConnectionManager.TcpConnection tcpConnection;
    private TcpConnectionManager.ReceiverType receiverType;
    private ReceiverIpSelectorUser waiter;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        String message = getString(R.string.dialog_overwrite_receiver_type_message,
                TcpConnectionManager.getReceiverTypeDisplayString(
                        getResources(), tcpConnection.getType()),
                TcpConnectionManager.getReceiverTypeDisplayString(getResources(), receiverType));

        return builder.setTitle(R.string.dialog_overwrite_receiver_type)
                .setMessage(message)
                .setNegativeButton(R.string.dialog_no, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //only dismiss this dialog
                    }
                }).setPositiveButton(R.string.dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        tcpConnection.setType(receiverType);
                        waiter.resumeDismiss();
                    }
                }).create();
    }

    public OverwriteTypeDialog setValues(TcpConnectionManager.TcpConnection tcpConnection,
                                         TcpConnectionManager.ReceiverType receiverType,
                                         ReceiverIpSelectorUser waiter) {
        this.tcpConnection = tcpConnection;
        this.receiverType = receiverType;
        this.waiter = waiter;
        return this;
    }
}
