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
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import java.util.Arrays;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.ReceiverIpSelectorUser;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.modules.CommandLineFragment;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;

public class AddCommandLineFragmentDialog extends CustomDialogFragment
        implements ReceiverIpSelectorUser {
    private Container container;
    private TcpConnectionManager.ReceiverType type;
    private CommandLineFragment fragment;
    private PageContainerFragment page;
    private String ip, hint;

    private Spinner receiverTypeSpinner;
    private String[] typeValues;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater()
                .inflate(R.layout.dialog_command_line_fragment, null);

        receiverTypeSpinner = (Spinner) view.findViewById(R.id.edit_receiver_type);
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

        final EditText editHint = (EditText) view.findViewById(R.id.edit_hint);

        String positiveButtonText;
        if (fragment != null) {// Edit fragment
            positiveButtonText = getString(R.string.dialog_ok);
            hint = fragment.getHint();
            editReceiverIp.setText(fragment.getIp());
            type = fragment.getType();

            editHint.setText(hint);

            for (int i = 0; i < typeValues.length; i++) {// Select type from spinner
                if (typeValues[i].equals(type.toString())) {
                    receiverTypeSpinner.setSelection(i);
                    break;
                }
            }
        } else {// Create new fragment
            positiveButtonText = getString(R.string.dialog_add);
            hint = "";
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
                                hint = Util.getUserInput(editHint, false);

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
                                            AddCommandLineFragmentDialog.this).show(getFragmentManager(),
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

    public AddCommandLineFragmentDialog setPage(PageContainerFragment page) {
        this.page = page;
        return this;
    }

    public AddCommandLineFragmentDialog setEditFragment(CommandLineFragment fragment) {
        this.fragment = fragment;
        return this;
    }

    public AddCommandLineFragmentDialog setContainer(@Nullable Container container) {
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
            fragment. setValues(ip, type, hint);
        } else {// Add new fragment
            Util.addFragmentToContainer(getActivity(), CommandLineFragment.newInstance(ip, type,
                            hint), page, container);
        }
        dismiss();
    }
}
