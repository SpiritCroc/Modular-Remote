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

import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.ArrayList;
import java.util.Arrays;

import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.TcpInformation;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.dialogs.AddSpinnerFragmentDialog;

public class SpinnerFragment extends ModuleFragment implements AdapterView.OnItemSelectedListener,
        TcpConnectionManager.TcpUpdateInterface {
    private static final String ARG_IP = "ip";
    private static final String ARG_TYPE = "type";
    private static final String ARG_MENU = "menu";
    private static final String LOG_TAG = SpinnerFragment.class.getSimpleName();

    private String ip;
    private TcpConnectionManager.ReceiverType type;
    private int menu;
    private TcpConnectionManager tcpConnectionManager;
    private TcpConnectionManager.TcpConnection connection;
    private boolean menuEnabled = false, spinnerActive = false;
    private MenuItem menuResetItem;
    private Spinner spinner;
    private String responseClassifier, command, submenuReadable;
    private String[] spinnerItemNames, spinnerItemValues;
    private ArrayAdapter<String> adapter;
    private boolean created = false;

    public static SpinnerFragment newInstance(String ip, TcpConnectionManager.ReceiverType type,
                                              int menu) {
        SpinnerFragment fragment = new SpinnerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IP, ip);
        args.putString(ARG_TYPE, type.toString());
        args.putInt(ARG_MENU, menu);
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

        if (getArguments() != null) {
            ip = getArguments().getString(ARG_IP);
            type = TcpConnectionManager.ReceiverType.valueOf(getArguments().getString(ARG_TYPE));
            menu = getArguments().getInt(ARG_MENU);
        } else {
            Log.e(LOG_TAG, "onCreate: getArguments()==null");
            ip = "";
            type = TcpConnectionManager.ReceiverType.UNSPECIFIED;
            menu = 0;
        }

        tcpConnectionManager = TcpConnectionManager.getInstance(getActivity().getApplicationContext());

        connection = tcpConnectionManager.requireConnection(this);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        tcpConnectionManager.stopUpdate(this);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_spinner, container, false);

        spinner = (Spinner) view.findViewById(R.id.spinner);
        setDragView(spinner);
        spinner.setOnItemSelectedListener(this);
        //Workaround, at least prevents actionBar from showing because of opening of spinner
        spinner.setOnSystemUiVisibilityChangeListener(new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    boolean visible = (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                    if (visible && getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).hideSystemUI();
                    }
                }
            }
        });
        setValues(ip, type, menu);
        updatePosition(view);
        resize(view);

        maybeStartDrag(view);

        return view;
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (menuEnabled && !connection.isConnected()) {
            String menuResetEntry = getString(R.string.action_reconnect_receiver, ip);
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
        if (menuEnabled && menuResetItem != null && menuResetItem.getTitle().equals(item.getTitle())) {
            connection.reset();
            return true;//connection only needs to reset once
        }
        return super.onOptionsItemSelected(item);
    }
    @Override
    public void setMenuEnabled(boolean menuEnabled) {
        this.menuEnabled = menuEnabled;
    }
    @Override
    public void onItemSelected (AdapterView<?> parent, View view, int pos, long id) {
        if (command == null) {
            // Spinner not configured to control a device
            return;
        }
        TcpInformation information = connection.getBufferedInformation(responseClassifier);
        if (information != null) {
            spinner.setSelection(adapter.getPosition(information.getStringValue()));  //jump back to previous position so no wrong information is displayed
        }
        if (spinnerActive) {
            if (information == null || !information.isStringAvailable() ||
                    !information.getStringValue().equals(spinnerItemNames[pos])) {
                connection.sendRawCommand(Util.createCommandChain(command, spinnerItemValues[pos]));
            }
        } else {
            spinnerActive = true;
        }
    }
    @Override
    public void onNothingSelected(AdapterView<?> parent) {
        spinnerActive = false;//next itemSelection will not be made by user→don't send command
    }
    @Override
    public String getReadableName() {
        return Util.getACString(R.string.fragment_spinner) +
                (submenuReadable == null ? "" : (" " + submenuReadable));
    }
    @Override
    public String getRecreationKey() {
        return fixRecreationKey(SPINNER_FRAGMENT + SEP + pos.getRecreationKey() + SEP + ip + SEP +
                connection.getType().toString() + SEP + menu + SEP);
    }
    public static SpinnerFragment recoverFromRecreationKey(String key) {
        try {
            String[] args = Util.split(key, SEP, 0);
            String ip = args[2];
            TcpConnectionManager.ReceiverType type =
                    TcpConnectionManager.ReceiverType.valueOf(args[3]);
            int menu = Integer.parseInt(args[4]);
            SpinnerFragment fragment = newInstance(ip, type, menu);
            fragment.recoverPos(args[1]);
            return fragment;
        } catch (Exception e){
            Log.e(LOG_TAG, "recoverFromRecreationKey: illegal key: " + key);
            Log.e(LOG_TAG, "Got exception: " + e);
            return null;
        }
    }
    @Override
    public ModuleFragment copy() {
        ModuleFragment fragment =  newInstance(ip, type, menu);
        fragment.parent = parent;
        return fragment;
    }
    public int getMenu() {
        return menu;
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
    public void setValues(String ip, TcpConnectionManager.ReceiverType type, int menu) {
        this.ip = ip;
        this.type = type;
        this.menu = menu;

        connection = tcpConnectionManager.requireConnection(this);

        int[] spinnerMenus = TcpConnectionManager.getDropdownMenuSubmenus(getResources(), type);
        String[] spinnerCommands =
                TcpConnectionManager.getDropdownMenuCommandValues(getResources(), type);
        String[] spinnerResponses =
                TcpConnectionManager.getDropdownMenuResponseValues(getResources(), type);
        String[] spinnerReadables = TcpConnectionManager.getDropdownMenuNames(getResources(), type);
        if (spinnerMenus.length != spinnerCommands.length ||
                spinnerMenus.length != spinnerResponses.length ||
                spinnerMenus.length  != spinnerReadables.length) {
            Log.e(LOG_TAG, "setValues: Array lengths don't match");
            return;
        }
        for (int i = 0; i < spinnerMenus.length; i++) {
            if (spinnerMenus[i] == menu) {
                responseClassifier = spinnerResponses[i];
                command = spinnerCommands[i];
                submenuReadable = spinnerReadables[i];
                break;
            }
        }

        setSpinnerContent();
    }
    private void setSpinnerContent() {
        ArrayList<String> list = new ArrayList<>();
        spinnerItemNames = TcpConnectionManager.getCommandNameArray(getResources(), connection,
                menu, false);
        spinnerItemValues = TcpConnectionManager.getCommandValueArray(getResources(), connection,
                menu);
        list.addAll(Arrays.asList(spinnerItemNames));
        if (Util.SCREENSHOT) {
            if (list.isEmpty()) {
                spinnerItemValues = new String[]{""};
            } else {
                list.remove(0);
            }
            list.add(0, getString(R.string.fragment_spinner));
        }
        adapter = new ArrayAdapter<>(getActivity(), R.layout.support_simple_spinner_dropdown_item,
                list);
        spinner.setAdapter(adapter);
        updateUI(connection.getBufferedInformation(responseClassifier));
    }
    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void update(final TcpInformation information) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI(information);
            }
        });
    }
    private void updateUI(TcpInformation information) {
        if (information == null) {
            return;
        }
        if (information.getType().equals(TcpInformation.InformationType.UPDATE_MENU) &&
                information.isIntAvailable() && information.getIntValue() == menu) {
            // This means we should update the content of our spinner
            setSpinnerContent();
        } else if (information.isClassifiedResponse() &&
                information.getResponseClassifier().equals(responseClassifier) &&
                information.getStringValue() != null) {
            int index = Arrays.asList(spinnerItemNames).indexOf(information.getStringValue());
            if (index >= 0) {
                spinner.setSelection(index);
            }
        }
    }

    @Override
    protected void editActionEdit() {
        new AddSpinnerFragmentDialog()
                .setEditFragment(this)
                .show(getFragmentManager(), "AddSpinnerFragmentDialog");
    }
}
