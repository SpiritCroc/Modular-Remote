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
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.Util;

public class SelectCommandDialog extends DialogFragment {
    private static final String LOG_TAG = SelectCommandDialog.class.getSimpleName();
    private static final boolean DEBUG = false;

    private CommandInterface parentDialog;
    private TcpConnectionManager.TcpConnection connection;
    private TcpConnectionManager.ReceiverType type;
    private String[] commandValues;
    private int[] hasSubmenu;
    private int selection = 0, preSelection;
    private int submenu = 0;
    private String bossCommand = null;// Opposite of subCommand ;)
    private ArrayList<Integer> commandSearchPath = new ArrayList<>();
    private int commandId;
    private boolean responseMode;

    public SelectCommandDialog setResponseMode(boolean responseMode) {
        this.responseMode = responseMode;
        if (responseMode && submenu == 0) {
            submenu = TcpConnectionManager.MENU_TCP_RESPONSES;
        } else if (!responseMode && submenu == TcpConnectionManager.MENU_TCP_RESPONSES) {
            submenu = 0;
        }
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        String[] commandNames;
        if (connection == null) {
            commandNames = TcpConnectionManager.getCommandNameArrayFromResource(getResources(),
                    type, submenu);
            commandValues = TcpConnectionManager.getCommandValueArrayFromResource(getResources(),
                    type, submenu);
            hasSubmenu = TcpConnectionManager.getCommandHasSubmenuArrayFromResource(getResources(),
                    type, submenu);
        } else {
            commandNames = TcpConnectionManager.getCommandNameArray(getResources(), connection,
                    submenu, false);
            commandValues = TcpConnectionManager.getCommandValueArray(getResources(), connection,
                    submenu);
            hasSubmenu = TcpConnectionManager.getCommandHasSubmenuArray(getResources(), connection,
                    submenu);
        }
        if (commandSearchPath != null && !commandSearchPath.isEmpty()) {
            preSelection = selection = commandSearchPath.get(0);
        }

        builder.setTitle(responseMode ? R.string.dialog_select_response :
                R.string.dialog_select_command)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                }).setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String command = commandValues[selection];
                if (DEBUG) Log.v(LOG_TAG, "Select command " + command);
                if (bossCommand != null) {
                    command = Util.createCommandChain(bossCommand, command);
                    if (DEBUG) Log.v(LOG_TAG, "Chain command to " + command);
                }
                if (hasSubmenu[selection] == 0) {
                    parentDialog.setCommand(commandId, command);
                } else if (hasSubmenu[selection] == -1){
                    new EnterRawCommandDialog().setValues(parentDialog, commandId)
                            .setSubmenu(command, type)
                            .show(getFragmentManager(), "EnterRawCommandDialog");
                } else {
                    ArrayList<Integer> subCommandSearchPath = null;
                    if (commandSearchPath != null && !commandSearchPath.isEmpty()  &&
                            preSelection == selection) {
                        subCommandSearchPath = (ArrayList<Integer>) commandSearchPath.clone();
                        subCommandSearchPath.remove(0);
                    }
                    new SelectCommandDialog().setValues(parentDialog, connection, type,
                            subCommandSearchPath, commandId)
                            .setSubmenu(command, hasSubmenu[selection])
                            .setResponseMode(responseMode)
                            .show(getFragmentManager(), "SelectCommandDialog" +
                                    hasSubmenu[selection]);
                }
            }
        }).setNeutralButton(R.string.dialog_enter_raw,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        new EnterRawCommandDialog().setValues(parentDialog, commandId)
                                .setSubmenu(bossCommand)
                                .show(getFragmentManager(), "EnterRawCommandDialog");
                    }
                }).setSingleChoiceItems(commandNames, selection,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selection = which;
                    }
                });

        return builder.create();
    }

    public SelectCommandDialog setValues(CommandInterface parentDialog,
                                         @Nullable TcpConnectionManager.TcpConnection connection,
                                         TcpConnectionManager.ReceiverType type,
                                         ArrayList<Integer> commandSearchPath, int commandId) {
        this.parentDialog = parentDialog;
        this.connection = connection;
        this.type = type;
        this.commandSearchPath = commandSearchPath;
        this.commandId = commandId;
        return this;
    }
    private SelectCommandDialog setSubmenu(String bossCommand, int submenu) {
        this.submenu = submenu;
        this.bossCommand = bossCommand;
        return this;
    }
}
