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
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;

import de.spiritcroc.modular_remote.Display;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.ReceiverIpSelectorUser;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.TcpInformation;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.DisplayFragment;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;

public class AddDisplayFragmentDialog extends CustomDialogFragment implements ReceiverIpSelectorUser,
        CommandInterface {
    private boolean buttonMode = false;
    private TcpConnectionManager.ReceiverType type = TcpConnectionManager.ReceiverType.UNSPECIFIED;
    private TcpConnectionManager.TcpConnection connection;
    private DisplayFragment fragment;
    private AddPageDialog pageDialog;// PageContainerFragments can also be used as display
    private PageContainerFragment page;// The page for the DisplayFragment
    private Container container;
    private EditText editStaticText;
    private String ip, command = "", command2 = "", command3 = "", informationType = "";
    private Display.ViewMode mode;
    private Spinner receiverTypeSpinner, informationTypeSpinner,
            horizontalTextGravitySpinner, addStaticTextSpinner;
    private LinearLayout tcpDisplayLayout, staticDisplayLayout, clockDisplayLayout, tcpLayout;
    private String[] typeValues, informationTypeValues, displayModeValues, addStaticTextValues;
    private int[] horizontalTextGravityValues;
    private String staticText;
    private ArrayList<Integer> commandSearchPath = new ArrayList<>();
    private ArrayList<Integer> command2SearchPath = new ArrayList<>();
    private ArrayList<Integer> command3SearchPath = new ArrayList<>();
    private Button selectCommand, selectCommand2, selectCommand3;
    private TcpConnectionManager tcpConnectionManager;

    public AddDisplayFragmentDialog setButtonMode(boolean buttonMode) {
        this.buttonMode = buttonMode;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater()
                .inflate(R.layout.dialog_display_fragment, null);

        tcpConnectionManager = TcpConnectionManager
                .getInstance(getActivity().getApplicationContext());

        tcpDisplayLayout = (LinearLayout) view.findViewById(R.id.edit_tcp_display_layout);
        staticDisplayLayout =
                (LinearLayout) view.findViewById(R.id.edit_static_text_display_layout);
        clockDisplayLayout = (LinearLayout) view.findViewById(R.id.edit_clock_layout);
        tcpLayout = (LinearLayout) view.findViewById(R.id.tcp_device_layout);

        receiverTypeSpinner = (Spinner) view.findViewById(R.id.edit_receiver_type);
        informationTypeSpinner = (Spinner) view.findViewById(R.id.edit_information_type);
        Spinner displayModeSpinner = (Spinner) view.findViewById(R.id.edit_display_mode);
        horizontalTextGravitySpinner = (Spinner) view.findViewById(R.id.horizontal_gravity_spinner);
        final AutoCompleteTextView editReceiverIp =
                (AutoCompleteTextView) view.findViewById(R.id.edit_receiver_ip);
        Util.suggestPreviousIps(this, editReceiverIp);

        editStaticText = (EditText) view.findViewById(R.id.edit_static_text);
        selectCommand = (Button) view.findViewById(R.id.select_command);
        selectCommand.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.equals(TcpConnectionManager.ReceiverType.UNSPECIFIED)) {
                    new EnterRawCommandDialog().setValues(AddDisplayFragmentDialog.this, 1)
                            .show(getFragmentManager(), "EnterRawCommandDialog");
                } else {
                    new SelectCommandDialog().setValues(AddDisplayFragmentDialog.this, connection,
                            type, commandSearchPath, 1)
                            .show(getFragmentManager(), "SelectCommandDialog");
                }
            }
        });
        selectCommand2 = (Button) view.findViewById(R.id.select_command_2);
        selectCommand2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.equals(TcpConnectionManager.ReceiverType.UNSPECIFIED)) {
                    new EnterRawCommandDialog().setValues(AddDisplayFragmentDialog.this, 2)
                            .show(getFragmentManager(), "EnterRawCommandDialog");
                } else {
                    new SelectCommandDialog().setValues(AddDisplayFragmentDialog.this, connection,
                            type, command2SearchPath, 2)
                            .show(getFragmentManager(), "SelectCommandDialog");
                }
            }
        });
        selectCommand3 = (Button) view.findViewById(R.id.select_command_3);
        selectCommand3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (type.equals(TcpConnectionManager.ReceiverType.UNSPECIFIED)) {
                    new EnterRawCommandDialog().setValues(AddDisplayFragmentDialog.this, 3)
                            .show(getFragmentManager(), "EnterRawCommandDialog");
                } else {
                    new SelectCommandDialog().setValues(AddDisplayFragmentDialog.this, connection,
                            type, command3SearchPath, 3)
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
                setInformationTypeSpinnerAdapter();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
        setInformationTypeSpinnerAdapter();
        informationTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                informationType = informationTypeValues[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        displayModeValues = getResources().getStringArray(R.array.fragment_display_mode_values);
        displayModeSpinner.setAdapter(ArrayAdapter.createFromResource(activity,
                R.array.fragment_display_modes, R.layout.support_simple_spinner_dropdown_item));
        displayModeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mode = Display.ViewMode.valueOf(displayModeValues[position]);
                setDisplayMode();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        horizontalTextGravityValues =
                getResources().getIntArray(R.array.horizontal_text_gravity_values);
        horizontalTextGravitySpinner.setAdapter(ArrayAdapter.createFromResource(activity,
                R.array.horizontal_text_gravities, R.layout.support_simple_spinner_dropdown_item));

        addStaticTextSpinner = (Spinner) view.findViewById(R.id.add_static_text_spinner);
        addStaticTextValues = getResources().getStringArray(R.array.button_label_spinner_chars);
        addStaticTextSpinner.setAdapter(ArrayAdapter.createFromResource(activity,
                R.array.button_label_spinner_chars, R.layout.support_simple_spinner_dropdown_item));
        addStaticTextSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    editStaticText.setText(editStaticText.getText() +
                            addStaticTextValues[position]);
                    addStaticTextSpinner.setSelection(0);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        String positiveButtonText;
        int title = R.string.dialog_configure_fragment;
        Display.ModeSettings modeSettings;
        if (fragment != null) {
            modeSettings = fragment.getModeSettings();
            editReceiverIp.setText(fragment.getIp());
            type = fragment.getType();
            command = fragment.getClickCommand();
            command2 = fragment.getDoubleClickCommand();
            command3 = fragment.getLongClickCommand();
            // Select horizontalTextGravity from spinner
            int horizontalTextGravity = fragment.getHorizontalTextGravity();
            for (int i = 0; i < horizontalTextGravityValues.length; i++) {
                if (horizontalTextGravityValues[i] == horizontalTextGravity) {
                    horizontalTextGravitySpinner.setSelection(i);
                    break;
                }
            }
        } else if (pageDialog != null) {
            title =  R.string.dialog_configure_action_bar_display;
            modeSettings = pageDialog.getDisplaySettings();
            view.findViewById(R.id.text_gravity_layout).setVisibility(View.GONE);
            view.findViewById(R.id.edit_buttons_layout).setVisibility(View.GONE);
        } else {
            modeSettings = null;
        }
        if (modeSettings != null) {// Edit display
            positiveButtonText = getString(R.string.dialog_ok);
            setInformationTypeSpinnerAdapter();
            mode = modeSettings.mode;
            setDisplayMode();

            // Select displayMode from spinner
            for (int i = 0; i < displayModeValues.length; i++) {
                if (displayModeValues[i].equals(mode.toString())) {
                    displayModeSpinner.setSelection(i);
                    break;
                }
            }

            switch (mode){
                case TCP_DISPLAY:
                    informationType = ((Display.TcpDisplaySettings)modeSettings).informationType;
                    if (pageDialog != null) {
                        editReceiverIp.setText(((Display.TcpDisplaySettings)modeSettings).ip);
                        type = ((Display.TcpDisplaySettings)modeSettings).receiverType;
                    }
                    // Select informationType from spinner
                    for (int i = 0; i < informationTypeValues.length; i++) {
                        if (informationTypeValues[i].equals(informationType)) {
                            informationTypeSpinner.setSelection(i);
                            break;
                        }
                    }
                    break;
                case STATIC_TEXT:
                    editStaticText.setText(((Display.StaticTextSettings)modeSettings).text);
                    break;
                case CLOCK:
                    break;
            }

            selectCommand.setText(tcpConnectionManager.getCommandNameFromResource(getResources(),
                    connection, type, command, commandSearchPath));
            selectCommand2.setText(tcpConnectionManager.getCommandNameFromResource(getResources(),
                    connection, type, command2, command2SearchPath));
            selectCommand3.setText(tcpConnectionManager.getCommandNameFromResource(getResources(),
                    connection, type, command3, command3SearchPath));

            // Select type from spinner
            for (int i = 0; i < typeValues.length; i++) {
                if (typeValues[i].equals(type.toString())) {
                    receiverTypeSpinner.setSelection(i);
                    break;
                }
            }

        } else {// Create new fragment
            positiveButtonText = getString(R.string.dialog_add);

            if (buttonMode) {
                // Select default displayMode for buttons from spinner
                for (int i = 0; i < displayModeValues.length; i++) {
                    if (displayModeValues[i].equals(Display.ViewMode.STATIC_TEXT.toString())) {
                        displayModeSpinner.setSelection(i);
                        break;
                    }
                }
            }
        }

        builder.setTitle(title)
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
                                if (pageDialog == null && ip == null) {
                                    return;
                                }
                                if (mode == Display.ViewMode.STATIC_TEXT) {
                                    staticText = Util.getUserInput(editStaticText, false);
                                    if (staticText == null) {
                                        return;
                                    }
                                } else if (pageDialog != null &&
                                        mode == Display.ViewMode.TCP_DISPLAY && ip == null) {
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
                                            AddDisplayFragmentDialog.this)
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
        String[] commandNameArray = TcpConnectionManager.getCommandNameArrayFromResource(
                getResources(), type, TcpConnectionManager.MENU_TCP_RESPONSES);
        String[] entries = new String[commandNameArray.length+2];
        System.arraycopy(commandNameArray, 0, entries, 0, commandNameArray.length);
        entries[entries.length-2] = getString(R.string.response_raw);
        entries[entries.length-1] = getString(R.string.response_connectivity);
        String[] commandValueArray = TcpConnectionManager.getCommandValueArrayFromResource(
                getResources(), type, TcpConnectionManager.MENU_TCP_RESPONSES);
        String[] newInformationTypeValues = new String[commandValueArray.length+2];
        System.arraycopy(commandValueArray, 0, newInformationTypeValues, 0,
                commandValueArray.length);
        newInformationTypeValues[newInformationTypeValues.length-2] = "";
        newInformationTypeValues[newInformationTypeValues.length-1] =
                TcpInformation.InformationType.CONNECTIVITY_CHANGE.toString();
        if (informationTypeValues != null &&
                newInformationTypeValues.length == informationTypeValues.length) {
            boolean stop = true;
            for (int i = 0; i < newInformationTypeValues.length; i++) {
                if (!newInformationTypeValues[i].equals(informationTypeValues[i])) {
                    stop = false;
                    break;
                }
            }
            if (stop) {// Nothing changed
                return;
            }
        }
        informationTypeValues = newInformationTypeValues;
        for (String s: entries) {
            adapter.add(s);
        }
        informationTypeSpinner.setAdapter(adapter);
    }
    private void setDisplayMode() {
        tcpDisplayLayout.setVisibility(mode.equals(Display.ViewMode.TCP_DISPLAY) ?
                View.VISIBLE : View.GONE);
        staticDisplayLayout.setVisibility(mode.equals(Display.ViewMode.STATIC_TEXT) ?
                View.VISIBLE : View.GONE);
        clockDisplayLayout.setVisibility(mode.equals(Display.ViewMode.CLOCK) ?
                View.VISIBLE : View.GONE);
        if (pageDialog != null) {
            tcpLayout.setVisibility(tcpDisplayLayout.getVisibility());
        }
    }

    public AddDisplayFragmentDialog setPage(PageContainerFragment page) {
        this.page = page;
        return this;
    }
    public AddDisplayFragmentDialog setContainer(@Nullable Container container) {
        this.container = container;
        return this;
    }
    public AddDisplayFragmentDialog setEditFragment(DisplayFragment fragment,
                                                    TcpConnectionManager.TcpConnection connection) {
        this.fragment = fragment;
        this.connection = connection;
        return this;
    }
    public AddDisplayFragmentDialog setEditFragment(AddPageDialog pageDialog,
                                                    TcpConnectionManager.TcpConnection connection) {
        this.pageDialog = pageDialog;
        this.connection = connection;
        return this;
    }
    @Override
    public void setCommand(int id, String command) {// Use from SelectCommandDialog
        if (id == 1) {
            this.command = command;
            selectCommand.setText(tcpConnectionManager.getCommandNameFromResource(getResources(),
                    connection, type, command, commandSearchPath));
        } else if (id == 2) {
            this.command2 = command;
            selectCommand2.setText(tcpConnectionManager.getCommandNameFromResource(getResources(),
                    connection, type, command, command2SearchPath));
        } else if (id == 3) {
            this.command3 = command;
            selectCommand3.setText(tcpConnectionManager.getCommandNameFromResource(getResources(),
                    connection, type, command, command3SearchPath));
        }
    }

    @Override
    public void setReceiverType(TcpConnectionManager.ReceiverType receiverType) {
        receiverTypeSpinner.setSelection(Arrays.asList(typeValues)
                .indexOf(receiverType.toString()));
    }
    @Override
    public void resumeDismiss() {
        Display.ModeSettings modeSettings;
        switch (mode) {
            case TCP_DISPLAY:
                modeSettings = new Display.TcpDisplaySettings(ip, type, informationType);
                break;
            case STATIC_TEXT:
                modeSettings =
                        new Display.StaticTextSettings(staticText);
                break;
            case CLOCK:
                modeSettings = new Display.ClockSettings();
                break;
            default:
                modeSettings = null;
                break;
        }
        int horizontalTextGravity = horizontalTextGravityValues[
                horizontalTextGravitySpinner.getSelectedItemPosition()];
        if (fragment != null) {// Edit fragment
            fragment.setValues(ip, type, modeSettings, command, command2, command3,
                    horizontalTextGravity);
        } else if (pageDialog != null) {// Edit pageContainer
            pageDialog.setDisplaySettings(modeSettings);
        } else {// Add new fragment
            Util.addFragmentToContainer(getActivity(),
                    DisplayFragment.newInstance(ip, type, modeSettings, command, command2, command3,
                            horizontalTextGravity, buttonMode),
                    page, container);
        }
        dismiss();
    }
}
