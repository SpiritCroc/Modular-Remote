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

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Arrays;

import de.spiritcroc.modular_remote.Display;
import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.ReceiverIpSelectorUser;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;

public class AddPageDialog extends CustomDialogFragment implements ReceiverIpSelectorUser,
        CommandInterface {
    private static final int COMMAND_VOLUME_UP_ID = 1;
    private static final int COMMAND_VOLUME_DOWN_ID = 2;

    private TcpConnectionManager tcpConnectionManager;
    private TcpConnectionManager.TcpConnection connection;
    private Display.ModeSettings displaySettings;
    private PageContainerFragment fragment;
    private EditText editName;
    private AutoCompleteTextView editReceiverIp;
    private Spinner receiverTypeSpinner;
    private LinearLayout volumeButtonLayout;
    private Button selectCommandUp, selectCommandDown;
    private String name, ip, commandUp = "", commandDown = "";
    private boolean useHardwareVolume;
    private TcpConnectionManager.ReceiverType type;
    private FragmentActivity activity;
    private String[] typeValues;
    private ArrayList<Integer> commandUpSearchPath = new ArrayList<>();
    private ArrayList<Integer> commandDownSearchPath = new ArrayList<>();

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.dialog_add_page, null);

        tcpConnectionManager =
                TcpConnectionManager.getInstance(getActivity().getApplicationContext());

        editName = (EditText) view.findViewById(R.id.edit_name);
        receiverTypeSpinner = (Spinner) view.findViewById(R.id.edit_receiver_type);
        editReceiverIp = (AutoCompleteTextView) view.findViewById(R.id.edit_receiver_ip);
        volumeButtonLayout = (LinearLayout) view.findViewById(R.id.volume_button_layout);
        selectCommandUp = (Button) view.findViewById(R.id.select_command_up);
        selectCommandDown = (Button) view.findViewById(R.id.select_command_down);
        selectCommandUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.equals(TcpConnectionManager.ReceiverType.UNSPECIFIED)) {
                    new EnterRawCommandDialog().setValues(AddPageDialog.this, COMMAND_VOLUME_UP_ID)
                            .show(getFragmentManager(), "EnterRawCommandDialog");
                } else {
                    new SelectCommandDialog().setValues(AddPageDialog.this, connection,
                            type, commandUpSearchPath, COMMAND_VOLUME_UP_ID)
                            .show(getFragmentManager(), "SelectCommandDialog");
                }
            }
        });
        selectCommandDown.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.equals(TcpConnectionManager.ReceiverType.UNSPECIFIED)) {
                    new EnterRawCommandDialog().setValues(AddPageDialog.this, COMMAND_VOLUME_DOWN_ID)
                            .show(getFragmentManager(), "EnterRawCommandDialog");
                } else {
                    new SelectCommandDialog().setValues(AddPageDialog.this, connection,
                            type, commandDownSearchPath, COMMAND_VOLUME_DOWN_ID)
                            .show(getFragmentManager(), "SelectCommandDialog");
                }
            }
        });
        Util.suggestPreviousIps(this, editReceiverIp);
        final CheckBox editUseHardwareVolume =
                (CheckBox) view.findViewById(R.id.edit_use_hardware_volume);
        editUseHardwareVolume.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                volumeButtonLayout.setVisibility(((CheckBox) v).isChecked() ?
                        View.VISIBLE : View.GONE);
            }
        });

        view.findViewById(R.id.view_use_hardware_volume).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        editUseHardwareVolume.toggle();
                        volumeButtonLayout.setVisibility(editUseHardwareVolume.isChecked() ?
                                View.VISIBLE : View.GONE);
                    }
                }
        );

        view.findViewById(R.id.configure_action_bar_display).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        new AddDisplayFragmentDialog().setEditFragment(AddPageDialog.this,
                                fragment == null ? null : fragment.getConnection())
                                .show(getFragmentManager(), "AddDisplayFragmentDialog");
                    }
                }
        );
        if (fragment == null) {
            displaySettings = new Display.StaticTextSettings(getString(R.string.app_name));
        } else {
            displaySettings = fragment.getModeSettings();
        }

        typeValues = getResources().getStringArray(R.array.receiver_type_array_values);
        receiverTypeSpinner.setAdapter(ArrayAdapter.createFromResource(activity,
                R.array.receiver_type_array,
                R.layout.support_simple_spinner_dropdown_item));
        receiverTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                type = TcpConnectionManager.ReceiverType.valueOf(typeValues[(int) id]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        builder.setView(Util.scrollView(view))
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                });
        if (fragment == null) {
            builder.setTitle(R.string.dialog_add_page)
                    .setPositiveButton(R.string.dialog_add, null);
            volumeButtonLayout.setVisibility(View.GONE);
        } else {
            editName.setText(fragment.getName());
            editUseHardwareVolume.setChecked(fragment.isUseHardwareButtons());
            if (fragment.isUseHardwareButtons()) {
                editReceiverIp.setText(fragment.getIp());
                type = fragment.getType();
                // Select type from spinner
                for (int i = 0; i < typeValues.length; i++) {
                    if (typeValues[i].equals(type.toString())) {
                        receiverTypeSpinner.setSelection(i);
                        break;
                    }
                }
                commandUp = fragment.getVolumeUpCommand();
                commandDown = fragment.getVolumeDownCommand();
                selectCommandUp.setText(tcpConnectionManager.getCommandNameFromResource(
                        getResources(), connection, type, commandUp, commandUpSearchPath));
                selectCommandDown.setText(tcpConnectionManager.getCommandNameFromResource(
                        getResources(), connection, type, commandDown, commandDownSearchPath));
            } else {
                volumeButtonLayout.setVisibility(View.GONE);
            }
            builder.setTitle(R.string.dialog_edit_page)
                    .setPositiveButton(R.string.dialog_ok, null);
        }

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
                                name = Util.getUserInput(editName, true);
                                ip = Util.getUserInput(editReceiverIp, false);
                                if (name == null || ip == null) {
                                    return;
                                }
                                useHardwareVolume = editUseHardwareVolume.isChecked();

                                TcpConnectionManager.TcpConnection connectionCheck =
                                        TcpConnectionManager.getInstance(getActivity()
                                                .getApplicationContext()).getTcpConnection(ip);
                                if (useHardwareVolume && connectionCheck != null &&
                                        connectionCheck.getType() ==
                                                TcpConnectionManager.ReceiverType.UNSPECIFIED) {
                                    connectionCheck.setType(type);
                                    resumeDismiss();
                                } else if (useHardwareVolume && connectionCheck != null &&
                                        connectionCheck.getType() != type) {
                                    new OverwriteTypeDialog().setValues(connectionCheck, type,
                                            AddPageDialog.this).show(getFragmentManager(),
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
    public AddPageDialog setPage(PageContainerFragment fragment,
                                 TcpConnectionManager.TcpConnection connection) {
        this.fragment = fragment;
        this.connection = connection;
        return this;
    }

    @Override
    public void resumeDismiss() {
        if (fragment == null && activity instanceof MainActivity) {
            ((MainActivity) activity).addPage(PageContainerFragment.newInstance(name,
                    displaySettings, useHardwareVolume, ip, type, commandUp, commandDown)
                    .init(getActivity().getApplicationContext()));
        } else if (fragment == null) {
            Toast.makeText(activity, getString(R.string.error_could_not_add_page),
                    Toast.LENGTH_SHORT).show();
        } else {
            fragment.edit(name, displaySettings, useHardwareVolume, ip, type, commandUp,
                    commandDown);
        }
        dismiss();
    }
    @Override
    public void setReceiverType(TcpConnectionManager.ReceiverType type) {
        this.type = type;
        receiverTypeSpinner.setSelection(Arrays.asList(typeValues)
                .indexOf(type.toString()));
    }

    @Override
    public void setCommand(int id, String command) {// Use from SelectCommandDialog
        switch (id) {
            case COMMAND_VOLUME_UP_ID:
                commandUp = command;
                selectCommandUp.setText(
                        tcpConnectionManager.getCommandNameFromResource(getResources(),
                                connection, type, command, commandUpSearchPath));
                break;
            case COMMAND_VOLUME_DOWN_ID:
                commandDown = command;
                selectCommandDown.setText(
                        tcpConnectionManager.getCommandNameFromResource(getResources(),
                                connection, type, command, commandDownSearchPath));
                break;
        }
    }

    public void setDisplaySettings(Display.ModeSettings displaySettings) {
        this.displaySettings = displaySettings;
    }
    public Display.ModeSettings getDisplaySettings() {
        return displaySettings;
    }
}
