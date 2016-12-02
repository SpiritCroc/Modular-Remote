/*
 * Copyright (C) 2016 SpiritCroc
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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;

import de.spiritcroc.modular_remote.GlobalActionSetting;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.ReceiverIpSelectorUser;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.Util;

public class GlobalActionDialog extends DialogFragment implements ReceiverIpSelectorUser,
        CommandInterface{
    private GlobalActionSetting settings;
    private String title;
    private String key;
    private ResultListener listener;
    private TcpConnectionManager tcpConnectionManager;

    private Spinner receiverTypeSpinner;
    private Button selectCommand;

    private ArrayList<Integer> commandSearchPath = new ArrayList<>();
    private String[] typeValues;

    private String ip, command = "";
    private TcpConnectionManager.ReceiverType type = TcpConnectionManager.ReceiverType.UNSPECIFIED;

    public GlobalActionDialog setValues(GlobalActionSetting settings, String title, String key,
                                   ResultListener listener) {
        this.settings = settings;
        this.title = title;
        this.key = key;
        this.listener = listener;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater()
                .inflate(R.layout.dialog_global_action, null);

        tcpConnectionManager = TcpConnectionManager
                .getInstance(getActivity().getApplicationContext());

        receiverTypeSpinner = (Spinner) view.findViewById(R.id.edit_receiver_type);
        final AutoCompleteTextView editReceiverIp =
                (AutoCompleteTextView) view.findViewById(R.id.edit_receiver_ip);
        Util.suggestPreviousIps(this, editReceiverIp);

        selectCommand = (Button) view.findViewById(R.id.select_command);
        selectCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.equals(TcpConnectionManager.ReceiverType.UNSPECIFIED)) {
                    new EnterRawCommandDialog().setValues(GlobalActionDialog.this, 1)
                            .show(getFragmentManager(), "EnterRawCommandDialog");
                } else {
                    new SelectCommandDialog().setValues(GlobalActionDialog.this, null,
                            type, commandSearchPath, 1)
                            .show(getFragmentManager(), "SelectCommandDialog");
                }
            }
        });

        typeValues = getResources().getStringArray(R.array.receiver_type_array_values);
        receiverTypeSpinner.setAdapter(ArrayAdapter.createFromResource(activity,
                R.array.receiver_type_array, R.layout.support_simple_spinner_dropdown_item));
        receiverTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                type = TcpConnectionManager.ReceiverType.valueOf(typeValues[(int) id]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        editReceiverIp.setText(settings.getIp());
        type = settings.getType();
        command = settings.getClickCommand();
        // Select type from spinner
        for (int i = 0; i < typeValues.length; i++) {
            if (typeValues[i].equals(type.toString())) {
                receiverTypeSpinner.setSelection(i);
                break;
            }
        }
        selectCommand.setText(tcpConnectionManager.getCommandNameFromResource(getResources(),
                null, type, command, commandSearchPath));

        builder.setTitle(title)
                .setView(Util.scrollView(view))
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                });

        final AlertDialog alertDialog = builder.create();

        // Listeners added to button this way so they don't lead to dialog.dismiss if illegal input
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                // Prevent opening without editing ip
                editReceiverIp.dismissDropDown();

                alertDialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                ip = Util.getUserInput(editReceiverIp, false);
                                if (ip == null) {
                                    ip = "";
                                }

                                TcpConnectionManager.TcpConnection connectionCheck =
                                        TcpConnectionManager.getInstance(getActivity()
                                                .getApplicationContext()).getTcpConnection(ip);
                                if (connectionCheck != null && connectionCheck.getType() ==
                                        TcpConnectionManager.ReceiverType.UNSPECIFIED) {
                                    connectionCheck.setType(type);
                                    resumeDismiss();
                                } else if (connectionCheck != null &&
                                        connectionCheck.getType() != type) {
                                    new OverwriteTypeDialog().setValues(connectionCheck, type,
                                            GlobalActionDialog.this)
                                            .show(getFragmentManager(), "OverwriteTypeDialog");
                                } else {
                                    resumeDismiss();
                                }
                            }
                        });
            }
        });

        return alertDialog;
    }

    @Override
    public void setCommand(int id, String command) {// Use from SelectCommandDialog
        if (id == 1) {
            this.command = command;
            selectCommand.setText(tcpConnectionManager.getCommandNameFromResource(getResources(),
                    null, type, command, commandSearchPath));
        }
    }

    @Override
    public void setReceiverType(TcpConnectionManager.ReceiverType receiverType) {
        receiverTypeSpinner.setSelection(Arrays.asList(typeValues)
                .indexOf(receiverType.toString()));
    }

    @Override
    public void resumeDismiss() {
        listener.onGlobalActionResult(new GlobalActionSetting(ip, type, command,
                TcpConnectionManager.getInstance(getActivity())), key);
        dismiss();
    }

    public interface ResultListener {
        void onGlobalActionResult(GlobalActionSetting settings, String key);
    }
}
