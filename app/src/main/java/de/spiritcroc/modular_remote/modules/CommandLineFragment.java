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
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.TcpInformation;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.dialogs.AddCommandLineFragmentDialog;

public class CommandLineFragment extends ModuleFragment
        implements TcpConnectionManager.TcpUpdateInterface {
    private static final String LOG_TAG = DisplayFragment.class.getSimpleName();

    private static final String ARG_IP = "ip";
    private static final String ARG_TYPE = "type";
    private static final String ARG_HINT = "hint";

    private String ip, hint;
    private TcpConnectionManager.ReceiverType type;
    private MenuItem menuResetItem;
    private EditText editText;
    private TcpConnectionManager tcpConnectionManager;
    private TcpConnectionManager.TcpConnection connection;
    private boolean menuEnabled = false;
    private boolean created = false;

    public static CommandLineFragment newInstance(String ip, TcpConnectionManager.ReceiverType type,
                                              String hint) {
        CommandLineFragment fragment = new CommandLineFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IP, ip);
        args.putString(ARG_TYPE, type.toString());
        args.putString(ARG_HINT, hint);
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
        if (args != null) {
            ip = args.getString(ARG_IP);
            type = TcpConnectionManager.ReceiverType.valueOf(args.getString(ARG_TYPE));
            hint = args.getString(ARG_HINT);
        } else {
            Log.e(LOG_TAG, "onCreate: getArguments() == null");
            ip = hint = "";
            type = TcpConnectionManager.ReceiverType.UNSPECIFIED;
        }
        tcpConnectionManager =
                TcpConnectionManager.getInstance(getActivity().getApplicationContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_command_line, container, false);
        editText = (EditText) view.findViewById(R.id.enter_command);

        editText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_GO:
                        connection.sendRawCommand(editText.getText().toString());
                        editText.setText("");
                        return true;
                    default:
                        return false;
                }
            }
        });

        setDragView(editText);
        setValues(ip, type, hint);
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
        if (menuEnabled && menuResetItem != null &&
                menuResetItem.getTitle().equals(item.getTitle())) {
            connection.reset();
            return true;// Connection only needs to reset once
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void setMenuEnabled(boolean menuEnabled) {
        this.menuEnabled = menuEnabled;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        tcpConnectionManager.stopUpdate(this);
    }

    @Override
    public String getReadableName() {
        return Util.getACString(R.string.fragment_command_line) + " " + hint;
    }

    @Override
    public String getRecreationKey() {
        return fixRecreationKey(COMMAND_LINE_FRAGMENT + SEP + pos.getRecreationKey() + SEP +
                ip + SEP + type.toString() + SEP + hint + SEP);
    }

    public static CommandLineFragment recoverFromRecreationKey(String key) {
        try {
            String[] args = Util.split(key, SEP, 0);
            String ip = args[2];
            TcpConnectionManager.ReceiverType type =
                    TcpConnectionManager.ReceiverType.valueOf(args[3]);
            String hint = args[4];
            CommandLineFragment fragment = newInstance(ip, type, hint);
            fragment.recoverPos(args[1]);
            return fragment;
        } catch (Exception e) {
            Log.e(LOG_TAG, "recoverFromRecreationKey: illegal key: " + key);
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public ModuleFragment copy(){
        CommandLineFragment fragment = newInstance(ip, type, hint);
        fragment.setPosMeasures(pos.width, pos.height);
        fragment.parent = parent;
        return fragment;
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

    @Override
    public void update(final TcpInformation information) {
        // Nothing to do
    }

    @Override
    public boolean isConnected() {
        return connection == null || connection.isConnected();
    }

    public String getHint() {
        return hint;
    }

    public void setValues(String ip, TcpConnectionManager.ReceiverType type, String hint) {
        if (connection != null) {
            tcpConnectionManager.stopUpdate(this);
        }
        this.ip = ip;
        this.type = type;
        this.hint = hint;

        connection = tcpConnectionManager.requireConnection(this);

        editText.setHint(hint);
    }

    public TcpConnectionManager.TcpConnection getConnection() {
        return connection;
    }

    @Override
    protected void editActionEdit() {
        new AddCommandLineFragmentDialog().setEditFragment(this)
                .show(getFragmentManager(), "AddCommandLineFragmentDialog");
    }

    @Override
    public void onStartDragMode() {
        super.onStartDragMode();
        // Prevent open keyboard when in drag mode
        if (editText != null) {
            editText.setInputType(EditorInfo.TYPE_NULL);
        }
    }

    @Override
    public void onStopDragMode() {
        super.onStopDragMode();
        editText.setInputType(EditorInfo.TYPE_CLASS_TEXT);
    }
}
