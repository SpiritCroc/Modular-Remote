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
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.ArrayList;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.Util;

public class ConfigureToggleDialog extends DialogFragment implements CommandInterface{
    private static final int COMMAND_INTERFACE_ID_COMMAND = 1;
    private static final int COMMAND_INTERFACE_ID_RESPONSE = 2;

    private ConfigureToggleInterface parentDialog;
    private TcpConnectionManager.TcpConnection connection;
    private TcpConnectionManager tcpConnectionManager;
    private EditText editLabel;
    private Button selectCommand, selectResponse;
    private Spinner addTextSpinner;
    private String[] addTextValues;
    private String label, command, response;
    private String title;
    private TcpConnectionManager.ReceiverType type;
    private ArrayList<Integer> commandSearchPath = new ArrayList<>();
    private ArrayList<Integer> responseSearchPath = new ArrayList<>();
    private int id;

    public ConfigureToggleDialog setValues(int id, TcpConnectionManager.TcpConnection connection,
                                           TcpConnectionManager.ReceiverType type,
                                           ConfigureToggleInterface parentDialog,
                                           String label, String command, String response,
                                           String title) {
        this.id = id;
        this.connection = connection;
        this.type = type;
        this.parentDialog = parentDialog;
        this.label = label;
        this.command = command;
        this.response = response;
        this.title = title;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater()
                .inflate(R.layout.dialog_configure_toggle, null);

        tcpConnectionManager =
                TcpConnectionManager.getInstance(getActivity().getApplicationContext());

        editLabel = (EditText) view.findViewById(R.id.edit_label);
        if (label != null) {
            editLabel.setText(label);
        }

        selectCommand = (Button) view.findViewById(R.id.select_command);
        selectCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.equals(TcpConnectionManager.ReceiverType.UNSPECIFIED)) {
                    new EnterRawCommandDialog().setValues(ConfigureToggleDialog.this,
                            COMMAND_INTERFACE_ID_COMMAND)
                            .show(getFragmentManager(), "EnterRawCommandDialog");
                } else {
                    new SelectCommandDialog().setValues(ConfigureToggleDialog.this,
                            connection, type, commandSearchPath,
                            COMMAND_INTERFACE_ID_COMMAND)
                            .show(getFragmentManager(), "SelectCommandDialog");
                }
            }
        });
        if (command != null) {
            setCommand(COMMAND_INTERFACE_ID_COMMAND, command);
        }

        selectResponse = (Button) view.findViewById(R.id.select_response);
        selectResponse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.equals(TcpConnectionManager.ReceiverType.UNSPECIFIED)) {
                    new EnterRawCommandDialog().setValues(ConfigureToggleDialog.this,
                            COMMAND_INTERFACE_ID_RESPONSE)
                            .setResponseMode(true)
                            .show(getFragmentManager(), "EnterRawCommandDialog");
                } else {
                    new SelectCommandDialog().setValues(ConfigureToggleDialog.this,
                            connection, type, responseSearchPath,
                            COMMAND_INTERFACE_ID_RESPONSE)
                            .setResponseMode(true)
                            .show(getFragmentManager(), "SelectCommandDialog");
                }
            }
        });
        if (response != null) {
            setCommand(COMMAND_INTERFACE_ID_RESPONSE, response);
        }

        addTextSpinner = (Spinner) view.findViewById(R.id.add_text_spinner);
        addTextValues = getResources().getStringArray(R.array.button_label_spinner_chars);
        addTextSpinner.setAdapter(ArrayAdapter.createFromResource(activity,
                R.array.button_label_spinner_chars, R.layout.support_simple_spinner_dropdown_item));
        addTextSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    String text = editLabel.getText() + addTextValues[position];
                    editLabel.setText(text);
                    addTextSpinner.setSelection(0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        final AlertDialog alertDialog = builder.setTitle(title)
                .setView(Util.scrollView(view))
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                })
                .create();

        // Listeners added to button this way so they don't lead to dialog.dismiss if illegal input
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                label = Util.getUserInput(editLabel, false);
                                if (label == null) {
                                    return;
                                }
                                parentDialog.setToggleValues(id, label, command, response);
                                dismiss();
                            }
                        });
            }
        });

        return alertDialog;
    }

    @Override
    public void setCommand(int id, String command){
        switch (id) {
            case COMMAND_INTERFACE_ID_COMMAND:
                this.command = command;
                selectCommand.setText(
                        tcpConnectionManager.getCommandNameFromResource(getResources(),
                        connection, type, command, commandSearchPath));
                break;
            case COMMAND_INTERFACE_ID_RESPONSE:
                response = command;
                selectResponse.setText(
                        tcpConnectionManager.getCommandNameFromResource(getResources(),
                        connection, type, command, TcpConnectionManager.MENU_TCP_RESPONSES,
                                responseSearchPath));
                break;
        }
    }

    public interface ConfigureToggleInterface {
        void setToggleValues(int id, String label, String command, String response);
    }
}
