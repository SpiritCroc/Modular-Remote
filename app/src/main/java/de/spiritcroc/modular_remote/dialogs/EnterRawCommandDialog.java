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
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.Util;

public class EnterRawCommandDialog extends DialogFragment {
    private CommandInterface parentDialog;
    private EditText editCommand;
    private int commandId;
    private String bossCommand = null;
    private TcpConnectionManager.ReceiverType type;
    private boolean responseMode = false, requiresUserInput = false;

    public EnterRawCommandDialog setResponseMode(boolean responseMode) {
        this.responseMode = responseMode;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.dialog_enter_raw_command,
                null);
        editCommand = (EditText) view.findViewById(R.id.edit_command);

        String title = responseMode ? getString(R.string.dialog_enter_response) :
                getString(R.string.dialog_enter_command);
        if (bossCommand != null) {
            if (requiresUserInput) {
                Util.StringReference titleRef = new Util.StringReference(),
                        summary = new Util.StringReference();
                TcpConnectionManager.getMessageForRawSubmenu(getActivity(), type, bossCommand, titleRef,
                        summary);
                if (titleRef.value != null) {
                    title = titleRef.value;
                } else {
                    title = getString(R.string.dialog_enter_raw_subcommand);
                }
                if (summary.value != null) {
                    editCommand.setHint(summary.value);
                }
            } else {
                title = getString(R.string.dialog_enter_raw_subcommand);
            }
        }

        builder.setView(Util.scrollView(view))
                .setTitle(title)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                }).setPositiveButton(R.string.dialog_ok, null);

        final AlertDialog alertDialog = builder.create();

        // Listeners added to button this way so they don't lead to dialog.dismiss if illegal input
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Util.StringReference command = new Util.StringReference();
                                command.value = Util.getUserInput(editCommand, false);
                                if (bossCommand == null) {
                                    parentDialog.setCommand(commandId, command.value);
                                    dismiss();
                                } else {
                                    if (requiresUserInput) {
                                        if (TcpConnectionManager.translateRawSubmenu(type, bossCommand,
                                                command)) {
                                            parentDialog.setCommand(commandId, command.value);
                                            dismiss();
                                        } else {
                                            Toast.makeText(getActivity(), R.string.toast_illegal_input,
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    } else {
                                        parentDialog.setCommand(commandId, Util.createCommandChain(
                                                bossCommand, command.value));
                                        dismiss();
                                    }
                                }
                            }
                        });
            }
        });

        return alertDialog;
    }

    public EnterRawCommandDialog setValues(CommandInterface parentDialog, int commandId) {
        this.parentDialog = parentDialog;
        this.commandId = commandId;
        return this;
    }

    public EnterRawCommandDialog setSubmenu(String bossCommand,
                                            TcpConnectionManager.ReceiverType type) {
        requiresUserInput = true;
        this.bossCommand = bossCommand;
        this.type = type;
        return this;
    }
    public EnterRawCommandDialog setSubmenu(String bossCommand) {
        requiresUserInput = false;
        this.bossCommand = bossCommand;
        return this;
    }
}
