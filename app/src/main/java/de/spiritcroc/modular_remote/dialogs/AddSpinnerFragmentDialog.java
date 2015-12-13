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
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Spinner;

import java.util.Arrays;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.ReceiverIpSelectorUser;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;
import de.spiritcroc.modular_remote.modules.SpinnerFragment;

public class AddSpinnerFragmentDialog extends CustomDialogFragment
        implements ReceiverIpSelectorUser {
    private Container container;
    private TcpConnectionManager.ReceiverType type = TcpConnectionManager.ReceiverType.UNSPECIFIED;
    private SpinnerFragment fragment;
    private PageContainerFragment page;
    private String ip;
    private Spinner receiverTypeSpinner, informationTypeSpinner;
    private String[] typeValues;
    private int[] informationTypeValues;
    private int informationType;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.dialog_spinner_fragment,
                null);

        receiverTypeSpinner = (Spinner) view.findViewById(R.id.edit_receiver_type);
        informationTypeSpinner = (Spinner) view.findViewById(R.id.edit_information_type);
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
                setInformationTypeSpinnerAdapter();
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        setInformationTypeSpinnerAdapter();
        informationTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                informationType = informationTypeValues[position];
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String positiveButtonText;
        if (fragment != null) {// Edit fragment
            positiveButtonText = getString(R.string.dialog_ok);
            editReceiverIp.setText(fragment.getIp());
            type = fragment.getType();
            informationType = fragment.getMenu();
            setInformationTypeSpinnerAdapter();

            // Select type from spinner
            for (int i = 0; i < typeValues.length; i++) {
                if (typeValues[i].equals(type.toString())) {
                    receiverTypeSpinner.setSelection(i);
                    break;
                }
            }
            // Select informationType from spinner
            for (int i = 0; i < informationTypeValues.length; i++) {
                if (informationTypeValues[i] == informationType) {
                    informationTypeSpinner.setSelection(i);
                    break;
                }
            }
        } else {// Create new fragment
            positiveButtonText = getString(R.string.dialog_add);
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
                                if (ip == null) {
                                    return;
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
                                            AddSpinnerFragmentDialog.this)
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
    private void setInformationTypeSpinnerAdapter() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(),
                android.R.layout.simple_list_item_1, android.R.id.text1);

        int[] spinnerMenus = TcpConnectionManager.getDropdownMenuSubmenus(getResources(), type);
        String[] spinnerNames = TcpConnectionManager.getDropdownMenuNames(getResources(), type);

        if (informationTypeValues != null && spinnerMenus.length == informationTypeValues.length) {
            boolean stop = true;
            for (int i = 0; i < spinnerMenus.length; i++) {
                if (spinnerMenus[i] != informationTypeValues[i]) {
                    stop = false;
                    break;
                }
            }
            if (stop) {// Nothing changed
                return;
            }
        }
        informationTypeValues = spinnerMenus;
        for (String s: spinnerNames) {
            adapter.add(s);
        }
        informationTypeSpinner.setAdapter(adapter);
    }

    public AddSpinnerFragmentDialog setPage(PageContainerFragment page) {
        this.page = page;
        return this;
    }
    public AddSpinnerFragmentDialog setEditFragment(SpinnerFragment fragment) {
        this.fragment = fragment;
        return this;
    }
    public AddSpinnerFragmentDialog setContainer(@Nullable Container container) {
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
        if (fragment != null) {// Edit fragment
            fragment.setValues(ip, type, informationType);
        } else {// Add new fragment
            Util.addFragmentToContainer(getActivity(), SpinnerFragment.newInstance(ip, type,
                    informationType), page, container);
        }
        dismiss();
    }
}
