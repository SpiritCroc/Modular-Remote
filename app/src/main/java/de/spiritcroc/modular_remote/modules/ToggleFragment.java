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

package de.spiritcroc.modular_remote.modules;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ToggleButton;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.TcpInformation;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.dialogs.AddToggleFragmentDialog;

public class ToggleFragment extends ModuleFragment
        implements TcpConnectionManager.TcpUpdateInterface, View.OnClickListener {
    private static final boolean DEBUG = false;
    private static final String LOG_TAG = ToggleFragment.class.getSimpleName();

    private static final String ARG_IP = "ip";
    private static final String ARG_TYPE = "type";
    private static final String ARG_DEFAULT_STATE = "state";
    private static final String ARG_TOGGLE_ON_COMMAND = "command1";
    private static final String ARG_TOGGLE_OFF_COMMAND = "command2";
    private static final String ARG_TOGGLE_ON_RESPONSE = "response1";
    private static final String ARG_TOGGLE_OFF_RESPONSE = "response2";
    private static final String ARG_TOGGLE_ON_LABEL = "label1";
    private static final String ARG_TOGGLE_OFF_LABEL = "label2";

    public static final int DEFAULT_STATE_OFF = 0;
    public static final int DEFAULT_STATE_ON = 1;
    public static final int DEFAULT_STATE_SAVE_ON = 2;
    public static final int DEFAULT_STATE_SAVE_OFF = 3;

    private String ip;
    private TcpConnectionManager.ReceiverType type;
    private TcpConnectionManager tcpConnectionManager;
    private TcpConnectionManager.TcpConnection connection;
    private boolean menuEnabled = false;
    private MenuItem menuResetItem;
    private ToggleButton toggleButton;
    private String toggleOnCommand, toggleOffCommand, toggleOnResponse, toggleOffResponse,
            toggleOnLabel, toggleOffLabel;
    private int defaultState;
    private boolean receivedInformation = false;
    private boolean created = false;

    public static ToggleFragment newInstance(String ip, TcpConnectionManager.ReceiverType type,
                                             String toggleOnCommand, String toggleOffCommand,
                                             String toggleOnResponse, String toggleOffResponse,
                                             String toggleOnLabel, String toggleOffLabel,
                                             int defaultState) {
        ToggleFragment fragment = new ToggleFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IP, ip);
        args.putString(ARG_TYPE, type.toString());
        args.putInt(ARG_DEFAULT_STATE, defaultState);
        args.putString(ARG_TOGGLE_ON_COMMAND, toggleOnCommand);
        args.putString(ARG_TOGGLE_OFF_COMMAND, toggleOffCommand);
        args.putString(ARG_TOGGLE_ON_RESPONSE, toggleOnResponse);
        args.putString(ARG_TOGGLE_OFF_RESPONSE, toggleOffResponse);
        args.putString(ARG_TOGGLE_ON_LABEL, toggleOnLabel);
        args.putString(ARG_TOGGLE_OFF_LABEL, toggleOffLabel);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        if (created) {
            // Prevent overwriting attributes that are already set
            return;
        } else {
            created = true;
        }

        Bundle args = getArguments();
        if (getArguments() != null) {
            ip = args.getString(ARG_IP);
            type = TcpConnectionManager.ReceiverType.valueOf(args.getString(ARG_TYPE));
            defaultState = args.getInt(ARG_DEFAULT_STATE);
            toggleOnCommand = args.getString(ARG_TOGGLE_ON_COMMAND);
            toggleOffCommand = args.getString(ARG_TOGGLE_OFF_COMMAND);
            toggleOnResponse = args.getString(ARG_TOGGLE_ON_RESPONSE);
            toggleOffResponse = args.getString(ARG_TOGGLE_OFF_RESPONSE);
            toggleOnLabel = args.getString(ARG_TOGGLE_ON_LABEL);
            toggleOffLabel = args.getString(ARG_TOGGLE_OFF_LABEL);
        } else {
            Log.e(LOG_TAG, "onCreate: getArguments()==null");
            ip = toggleOnCommand = toggleOffCommand = toggleOnResponse = toggleOffResponse =
                    toggleOnLabel = toggleOffLabel = "";
            type = TcpConnectionManager.ReceiverType.UNSPECIFIED;
            defaultState = DEFAULT_STATE_ON;
        }

        tcpConnectionManager =
                TcpConnectionManager.getInstance(getActivity().getApplicationContext());

        connection = tcpConnectionManager.requireConnection(this);
    }
    @Override
    public void onDestroy(){
        super.onDestroy();
        tcpConnectionManager.stopUpdate(this);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_toggle, container, false);

        toggleButton = (ToggleButton) view.findViewById(R.id.toggle);
        setDragView(toggleButton);
        toggleButton.setOnClickListener(this);
        toggleButton.setChecked(defaultState == DEFAULT_STATE_ON ||
                defaultState == DEFAULT_STATE_SAVE_ON);
        setValues(ip, type, toggleOnCommand, toggleOffCommand, toggleOnResponse, toggleOffResponse,
                toggleOnLabel, toggleOffLabel, defaultState);
        updatePosition(view);
        resize(view);

        maybeStartDrag(view);

        return view;
    }
    @Override
    public void onResume () {
        super.onResume();
        updateFromBuffer();
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menuEnabled && !connection.isConnected()) {
            String menuResetEntry = getString(R.string.action_reconnect_receiver) + " " + ip;
            boolean first = true;
            for (int i = 0; i < menu.size(); i++) {
                if (menu.getItem(i).getTitle().equals(menuResetEntry)) {
                    first = false;
                }
            }
            if (first) {
                menuResetItem = menu.add(Menu.NONE, Menu.NONE, MENU_ORDER, menuResetEntry);
            }
        }
        super.onPrepareOptionsMenu(menu);
    }
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (menuEnabled && menuResetItem != null &&
                menuResetItem.getTitle().equals(item.getTitle())) {
            connection.reset();
            return true;// Connection only needs to reset once
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void setMenuEnabled(boolean menuEnabled) {
        this.menuEnabled = menuEnabled;
    }

    @Override
    public void onClick(View view) {
        if (view == toggleButton) {
            if (toggleButton.isChecked()) {
                if (receivedInformation && !toggleOnResponse.equals("")) {
                    // Wait for response
                    toggleButton.setChecked(false);
                }
                connection.sendRawCommand(toggleOnCommand);
            } else {
                if (receivedInformation && !toggleOffResponse.equals("")) {
                    // Wait for response
                    toggleButton.setChecked(true);
                }
                connection.sendRawCommand(toggleOffCommand);
            }
        }
    }

    @Override
    public String getReadableName() {
        return Util.getACString(R.string.fragment_toggle) + " " + (toggleOnLabel.equals(toggleOffLabel) ?
                toggleOnLabel : (toggleOnLabel + "/" + toggleOffLabel));
    }
    @Override
    public String getRecreationKey() {
        return fixRecreationKey(TOGGLE_FRAGMENT + SEP + pos.getRecreationKey() + SEP + ip + SEP +
                connection.getType().toString() + SEP +
                (defaultState == DEFAULT_STATE_SAVE_ON || defaultState == DEFAULT_STATE_SAVE_OFF ?
                        (toggleButton.isChecked() ?
                                DEFAULT_STATE_SAVE_ON : DEFAULT_STATE_SAVE_OFF) :
                        defaultState) + SEP +
                toggleOnCommand + SEP + toggleOffCommand + SEP +
                toggleOnResponse + SEP + toggleOffResponse + SEP +
                toggleOnLabel + SEP + toggleOffLabel + SEP);
    }
    public static ToggleFragment recoverFromRecreationKey(String key) {
        try {
            String[] args = Util.split(key, SEP, 0);
            String ip = args[2];
            TcpConnectionManager.ReceiverType type =
                    TcpConnectionManager.ReceiverType.valueOf(args[3]);
            int defaultState = Integer.parseInt(args[4]);
            String toggleOnCommand = args[5];
            String toggleOffCommand = args[6];
            String toggleOnResponse = args[7];
            String toggleOffResponse = args[8];
            String toggleOnLabel = args[9];
            String toggleOffLabel = args[10];
            ToggleFragment fragment = newInstance(ip, type, toggleOnCommand, toggleOffCommand,
                    toggleOnResponse, toggleOffResponse, toggleOnLabel, toggleOffLabel,
                    defaultState);
            fragment.recoverPos(args[1]);
            return fragment;
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "recoverFromRecreationKey: illegal key: " + key);
            Log.e(LOG_TAG, "Got exception: " + e);
            return null;
        }
    }
    @Override
    public ModuleFragment copy() {
        return newInstance(ip,  type, toggleOnCommand, toggleOffCommand, toggleOnResponse,
                toggleOffResponse, toggleOnLabel, toggleOffLabel, defaultState);
    }

    public int getDefaultState() {
        return defaultState;
    }
    public String getToggleOnCommand() {
        return toggleOnCommand;
    }
    public String getToggleOffCommand() {
        return toggleOffCommand;
    }
    public String getToggleOnResponse() {
        return toggleOnResponse;
    }
    public String getToggleOffResponse() {
        return toggleOffResponse;
    }
    public String getToggleOnLabel() {
        return toggleOnLabel;
    }
    public String getToggleOffLabel() {
        return toggleOffLabel;
    }
    @Override
    public String getIp() {
        return ip;
    }
    @Override
    public TcpConnectionManager.ReceiverType getType() {
        return type;
    }
    @Override
    public void setConnectionValues(String ip, TcpConnectionManager.ReceiverType type) {
        this.ip = ip;
        this.type = type;
    }
    public void setValues(String ip, TcpConnectionManager.ReceiverType type,
                          String toggleOnCommand, String toggleOffCommand,
                          String toggleOnResponse, String toggleOffResponse,
                          String toggleOnLabel, String toggleOffLabel, int defaultState) {
        this.ip = ip;
        this.type = type;
        this.toggleOnCommand = toggleOnCommand;
        this.toggleOffCommand = toggleOffCommand;
        this.toggleOnResponse = toggleOnResponse;
        this.toggleOffResponse = toggleOffResponse;
        this.toggleOnLabel = toggleOnLabel;
        this.toggleOffLabel = toggleOffLabel;
        this.defaultState = defaultState;

        if (toggleButton != null) {
            toggleButton.setTextOn(toggleOnLabel);
            toggleButton.setTextOff(toggleOffLabel);
            toggleButton.setText(toggleButton.isChecked() ? toggleOnLabel : toggleOffLabel);
        }

        receivedInformation = false;

        connection = tcpConnectionManager.requireConnection(this);

        updateFromBuffer();
    }

    private void updateFromBuffer() {
        if (connection.containsBufferedInformation(toggleOnResponse)) {
            receivedInformation = true;
            toggleButton.setChecked(true);
            if (DEBUG) Log.v(LOG_TAG, "Set to buffer true");
        } else if (connection.containsBufferedInformation(toggleOffResponse)) {
            receivedInformation = true;
            toggleButton.setChecked(false);
            if (DEBUG) Log.v(LOG_TAG, "Set to buffer false");
        } else if (DEBUG) Log.v(LOG_TAG, "No buffer found");
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void update (final TcpInformation information){
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI(information);
            }
        });
    }
    private void updateUI(TcpInformation information){
        if (information != null &&
                information.getType().equals(TcpInformation.InformationType.RAW) &&
                information.isStringAvailable()) {
            String response = information.getStringValue();
            if (response.equals(toggleOnResponse)) {
                receivedInformation = true;
                if (response.equals(toggleOffResponse)) {
                    // Same responses for on + off state
                    toggleButton.toggle();
                } else {
                    toggleButton.setChecked(true);
                }
            } else if (response.equals(toggleOffResponse)) {
                receivedInformation = true;
                toggleButton.setChecked(false);
            }
        }
    }

    public TcpConnectionManager.TcpConnection getConnection() {
        return connection;
    }

    @Override
    protected void editActionEdit() {
        new AddToggleFragmentDialog()
                .setEditFragment(this, connection)
                .show(getFragmentManager(), "AddToggleFragmentDialog");
    }
}
