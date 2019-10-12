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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.ReceiverIpSelectorUser;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;
import de.spiritcroc.modular_remote.modules.ToggleFragment;

public class AddToggleFragmentDialog extends CustomDialogFragment implements ReceiverIpSelectorUser,
        ConfigureToggleDialog.ConfigureToggleInterface{
    private static final int CONFIGURE_TOGGLE_ON_ID = 1;
    private static final int CONFIGURE_TOGGLE_OFF_ID = 2;

    private Container container;
    private TcpConnectionManager.ReceiverType type;
    private TcpConnectionManager.TcpConnection connection;
    private ToggleFragment fragment;
    private PageContainerFragment page;
    private String ip;
    private Button configureOnStateButton, configureOffStateButton;
    private Spinner receiverTypeSpinner, defaultStateSpinner;
    private String[] typeValues;
    private int[] stateValues;
    private String label1, label2, command1, command2, response1, response2;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater()
                .inflate(R.layout.dialog_toggle_fragment, null);

        receiverTypeSpinner = (Spinner) view.findViewById(R.id.edit_receiver_type);
        defaultStateSpinner = (Spinner) view.findViewById(R.id.default_state_spinner);
        final AutoCompleteTextView editReceiverIp =
                (AutoCompleteTextView) view.findViewById(R.id.edit_receiver_ip);
        Util.suggestPreviousIps(this, editReceiverIp);

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

        stateValues = getResources().getIntArray(R.array.fragment_toggle_default_state_values);
        defaultStateSpinner.setAdapter(ArrayAdapter.createFromResource(activity,
                R.array.fragment_toggle_default_states,
                R.layout.support_simple_spinner_dropdown_item));

        configureOnStateButton = (Button) view.findViewById(R.id.configure_on_state_button);
        configureOnStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ConfigureToggleDialog().setValues(CONFIGURE_TOGGLE_ON_ID, connection, type,
                        AddToggleFragmentDialog.this, label1, command1, response1,
                        getString(R.string.dialog_configure_toggle_on_state_title))
                        .show(getFragmentManager(), "ConfigureToggleDialog");
            }
        });
        configureOffStateButton = (Button) view.findViewById(R.id.configure_off_state_button);
        configureOffStateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new ConfigureToggleDialog().setValues(CONFIGURE_TOGGLE_OFF_ID, connection, type,
                        AddToggleFragmentDialog.this, label2, command2, response2,
                        getString(R.string.dialog_configure_toggle_off_state_title))
                        .show(getFragmentManager(), "ConfigureToggleDialog");
            }
        });

        String positiveButtonText;
        if (fragment != null) {// Edit fragment
            positiveButtonText = getString(R.string.dialog_ok);
            label1 = fragment.getToggleOnLabel();
            label2 = fragment.getToggleOffLabel();
            command1 = fragment.getToggleOnCommand();
            command2 = fragment.getToggleOffCommand();
            response1 = fragment.getToggleOnResponse();
            response2 = fragment.getToggleOffResponse();
            editReceiverIp.setText(fragment.getIp());
            type = fragment.getType();

            configureOnStateButton.setText(label1);
            configureOffStateButton.setText(label2);

            for (int i = 0; i < typeValues.length; i++) {// Select type from spinner
                if (typeValues[i].equals(type.toString())) {
                    receiverTypeSpinner.setSelection(i);
                    break;
                }
            }

            for (int i = 0; i < stateValues.length; i++) {// Select default state from spinner
                if (stateValues[i] == fragment.getDefaultState()) {
                    defaultStateSpinner.setSelection(i);
                    break;
                }
            }
        } else {// Create new fragment
            positiveButtonText = getString(R.string.dialog_add);
            label1 = label2 = command1 = command2 = response1 = response2 = "";
        }

        builder.setTitle(R.string.dialog_configure_fragment)
                .setView(Util.scrollView(view))
                .setPositiveButton(positiveButtonText, null)
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

                                TcpConnectionManager.TcpConnection connectionCheck =
                                        TcpConnectionManager
                                                .getInstance(getActivity().getApplicationContext())
                                                .getTcpConnection(ip);
                                if (connectionCheck != null && connectionCheck.getType() ==
                                        TcpConnectionManager.ReceiverType.UNSPECIFIED) {
                                    connectionCheck.setType(type);
                                    resumeDismiss();
                                } else if (connectionCheck != null &&
                                        connectionCheck.getType() != type) {
                                    new OverwriteTypeDialog().setValues(connectionCheck, type,
                                            AddToggleFragmentDialog.this).show(getFragmentManager(),
                                            "OverwriteTypeDialog");
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
    public void setToggleValues(int id, String label, String command, String response) {
        switch (id) {
            case CONFIGURE_TOGGLE_ON_ID:
                label1 = label;
                command1 = command;
                response1 = response;
                configureOnStateButton.setText(label);
                break;
            case CONFIGURE_TOGGLE_OFF_ID:
                label2 = label;
                command2 = command;
                response2 = response;
                configureOffStateButton.setText(label);
                break;
        }
    }
    public AddToggleFragmentDialog setPage(PageContainerFragment page) {
        this.page = page;
        return this;
    }
    public AddToggleFragmentDialog setEditFragment(ToggleFragment fragment,
                                                   TcpConnectionManager.TcpConnection connection) {
        this.fragment = fragment;
        this.connection = connection;
        return this;
    }
    public AddToggleFragmentDialog setContainer(@Nullable Container container) {
        this.container = container;
        return this;
    }

    @Override
    public void setReceiverType(TcpConnectionManager.ReceiverType receiverType) {
        receiverTypeSpinner.setSelection(Arrays.asList(typeValues)
                .indexOf(receiverType.toString()));
    }
    @Override
    public void resumeDismiss() {
        if (fragment != null) { // Edit fragment
            fragment. setValues(ip, type, command1, command2, response1, response2,
                    label1, label2, stateValues[defaultStateSpinner.getSelectedItemPosition()]);
        } else {// Add new fragment
            Util.addFragmentToContainer(getActivity(), ToggleFragment.newInstance(ip, type,
                    command1, command2, response1, response2, label1, label2,
                    stateValues[defaultStateSpinner.getSelectedItemPosition()]),
                    page, container);
        }
        dismiss();
    }

}
