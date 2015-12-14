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

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import de.spiritcroc.modular_remote.Display;
import de.spiritcroc.modular_remote.Preferences;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.TcpInformation;
import de.spiritcroc.modular_remote.TimeSingleton;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.dialogs.AddDisplayFragmentDialog;

public class DisplayFragment extends ModuleFragment implements Display, TimeSingleton.TimeListener,
        View.OnClickListener, View.OnLongClickListener,
        TcpConnectionManager.TcpUpdateInterface {
    private static final String LOG_TAG = DisplayFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String ARG_BUTTON_MODE = "button";
    private static final String ARG_IP = "ip";
    private static final String ARG_TYPE = "type";
    private static final String ARG_CLICK_COMMAND = "click1";
    private static final String ARG_DOUBLE_CLICK_COMMAND = "click2";
    private static final String ARG_LONG_CLICK_COMMAND = "click3";
    private static final String ARG_HORIZONTAL_TEXT_GRAVITY = "gravity";
    // Contains mode specific extras
    private static final String ARG_MODE_SETTINGS = "settings";

    private int horizontalTextGravity;
    private boolean buttonMode;
    private String ip, clickCommand, doubleClickCommand, longClickCommand;
    private TcpConnectionManager.ReceiverType type;
    private MenuItem menuResetItem;
    private TextView textView;
    private ModeSettings modeSettings;
    private TcpConnectionManager tcpConnectionManager;
    private TcpConnectionManager.TcpConnection connection;
    private boolean menuEnabled = false;
    private LinearLayout baseLayout;
    private int clickCount = 0;
    private SharedPreferences sharedPreferences;
    private Handler cancelDoubleClickHandler = new Handler();
    private Runnable cancelDoubleClick = new Runnable() {
        @Override
        public void run() {
            if (clickCount == 1) {
                connection.sendRawCommand(clickCommand);
            }
            clickCount = 0;
        }
    };
    private boolean created = false;

    public static DisplayFragment newInstance(String ip, TcpConnectionManager.ReceiverType type,
                                              ModeSettings modeSettings, String clickCommand,
                                              String doubleClickCommand, String longClickCommand,
                                              int horizontalTextGravity, boolean buttonMode) {
        DisplayFragment fragment = new DisplayFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_HORIZONTAL_TEXT_GRAVITY, horizontalTextGravity);
        args.putBoolean(ARG_BUTTON_MODE, buttonMode);
        args.putString(ARG_IP, ip);
        args.putString(ARG_TYPE, type.toString());
        args.putString(ARG_CLICK_COMMAND, clickCommand);
        args.putString(ARG_DOUBLE_CLICK_COMMAND, doubleClickCommand);
        args.putString(ARG_LONG_CLICK_COMMAND, longClickCommand);
        args.putString(ARG_MODE_SETTINGS, modeSettings.getRecreationKey());
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

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        Bundle args = getArguments();
        if (args != null) {
            horizontalTextGravity = args.getInt(ARG_HORIZONTAL_TEXT_GRAVITY);
            buttonMode = args.getBoolean(ARG_BUTTON_MODE);
            ip = args.getString(ARG_IP);
            type = TcpConnectionManager.ReceiverType.valueOf(args.getString(ARG_TYPE));
            clickCommand = args.getString(ARG_CLICK_COMMAND);
            doubleClickCommand = args.getString(ARG_DOUBLE_CLICK_COMMAND);
            longClickCommand = args.getString(ARG_LONG_CLICK_COMMAND);
            String modeSettingsKey = args.getString(ARG_MODE_SETTINGS);
            if (modeSettingsKey != null) {
                try {
                    modeSettings = ModeSettings.recoverFromRecreationKey(modeSettingsKey);
                } catch (Exception e) {
                    Log.e(LOG_TAG, "onCreate: Got exception while trying to recover " +
                            "mode settings for key " + modeSettingsKey);
                    e.printStackTrace();
                }
            } else  {
                Log.e(LOG_TAG, "onCreate: modeSettingsKey == null");
            }

        } else {
            Log.e(LOG_TAG, "onCreate: getArguments() == null");
            ip = "";
            type = TcpConnectionManager.ReceiverType.UNSPECIFIED;
        }
        tcpConnectionManager = TcpConnectionManager.getInstance(getActivity().getApplicationContext());
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view;
        if (buttonMode) {
            view = inflater.inflate(R.layout.fragment_button, container, false);
            textView = (Button) view.findViewById(R.id.button);
        } else {
            view = inflater.inflate(R.layout.fragment_display, container, false);
            textView = (TextView) view.findViewById(R.id.display);
        }

        baseLayout = (LinearLayout) view.findViewById(R.id.base_layout);
        setDragView(textView);
        textView.setOnClickListener(this);
        textView.setOnLongClickListener(this);
        setValues(ip, type, modeSettings, clickCommand, doubleClickCommand, longClickCommand,
                horizontalTextGravity);
        updatePosition(view);
        resize(view);

        maybeStartDrag(view);

        return view;
    }
    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (modeSettings instanceof TcpDisplaySettings) {
            if (menuEnabled && !connection.isConnected()) {
                String menuResetEntry = getString(R.string.action_reconnect_receiver)+ " " + ip;
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
        } else if (modeSettings instanceof StaticTextSettings) {
            menuResetItem = null;
        } else if (modeSettings instanceof ClockSettings) {
            menuResetItem = null;
        } else {
            menuResetItem = null;
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
    public void onResume() {
        super.onResume();
        if (modeSettings instanceof TcpDisplaySettings) {
            update(connection.getBufferedInformation(
                    ((TcpDisplaySettings) modeSettings).informationType));
        }
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        tcpConnectionManager.stopUpdate(this);
    }
    @Override
    public void onClick(View view) {
        clickCount ++;
        if (clickCount == 2 && !doubleClickCommand.equals("")) {
            connection.sendRawCommand(doubleClickCommand);
        } else if (!clickCommand.equals("")) {
            if (doubleClickCommand.equals("")) {
                connection.sendRawCommand(clickCommand);
            } else {
                cancelDoubleClickHandler.removeCallbacks(cancelDoubleClick);
                try {
                    cancelDoubleClickHandler.postDelayed(cancelDoubleClick, Util.getPreferenceInt(
                            sharedPreferences, Preferences.KEY_DOUBLE_CLICK_TIMEOUT, 500));
                } catch (Exception e) {
                    Log.e(LOG_TAG, "onClick: Got exception: " + e);
                }
            }
        }
    }
    @Override
    public boolean onLongClick(View view) {
        if (longClickCommand.equals("")) {
            return false;
        } else {
            connection.sendRawCommand(longClickCommand);
            return true;
        }
    }
    @Override
    public void setTime(String time) {
        if (modeSettings instanceof ClockSettings) {
            textView.setText(time);
        } else {
            Log.i(LOG_TAG, "Called setTime despite being " + modeSettings.mode);
        }
    }

    @Override
    public String getReadableName() {
        String extra;
        if (modeSettings instanceof TcpDisplaySettings) {
            if (((TcpDisplaySettings) modeSettings).informationType.equals(
                    TcpInformation.InformationType.CONNECTIVITY_CHANGE.toString())) {
                extra = " " + Util.getACString(R.string.response_connectivity) + " " + ip;
            } else {
                String[] informationTypeNames = TcpConnectionManager.getCommandNameArrayFromResource(
                        getResources(), type, TcpConnectionManager.MENU_TCP_RESPONSES);
                String[] informationTypeValues = TcpConnectionManager.getCommandValueArrayFromResource(
                        getResources(), type, TcpConnectionManager.MENU_TCP_RESPONSES);
                String readableType = ((TcpDisplaySettings) modeSettings).informationType;
                if (informationTypeNames.length == informationTypeValues.length) {
                    for (int i = 0; i < informationTypeValues.length; i++) {
                        if (informationTypeValues[i].equals(
                                ((TcpDisplaySettings) modeSettings).informationType)) {
                            readableType = informationTypeNames[i];
                        }
                    }
                } else {
                    Log.w(LOG_TAG, "getReadableName: informationTypeNames.length != " +
                            "informationTypeValues.length");
                }
                extra = " " + readableType + " " + ip;
            }
        } else if (modeSettings instanceof StaticTextSettings) {
            extra = " " + ((StaticTextSettings) modeSettings).text;
        } else if (modeSettings instanceof  ClockSettings) {
            extra = " " +
                    Util.getACString(R.string.fragment_display_mode_clock);
        } else {
            extra = "";
        }
        return Util.getACString(buttonMode ? R.string.fragment_button : R.string.fragment_display) + extra;

    }
    @Override
    public String getRecreationKey() {
        return fixRecreationKey((buttonMode ? BUTTON_FRAGMENT : DISPLAY_FRAGMENT) + SEP +
                pos.getRecreationKey() + SEP +
                ip + SEP + type.toString() + SEP + clickCommand + SEP +
                doubleClickCommand + SEP + longClickCommand + SEP +
                horizontalTextGravity + SEP + modeSettings.getRecreationKey() + SEP);
    }
    public static DisplayFragment recoverFromRecreationKey(String key, boolean buttonMode) {
        try {
            String[] args = Util.split(key, SEP, 0);
            String ip = args[2];
            TcpConnectionManager.ReceiverType type =
                    TcpConnectionManager.ReceiverType.valueOf(args[3]);
            String clickCommand = args[4];
            String doubleClickCommand = args[5];
            String longClickCommand = args[6];
            int horizontalTextGravity = Integer.parseInt(args[7]);
            ModeSettings modeSettings = ModeSettings.recoverFromRecreationKey(args[8]);
            DisplayFragment fragment = newInstance(ip, type, modeSettings, clickCommand,
                    doubleClickCommand, longClickCommand, horizontalTextGravity, buttonMode);
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
        DisplayFragment fragment =
                newInstance(ip, type, modeSettings.copy(), clickCommand, doubleClickCommand,
                longClickCommand, horizontalTextGravity, buttonMode);
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
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateUI(information);
            }
        });
    }
    @Override
    public boolean isConnected() {
        return connection == null || connection.isConnected();
    }

    private void updateUI(TcpInformation information) {
        if (modeSettings instanceof TcpDisplaySettings) {
            if (information == null) {
                return;
            }
            if (((TcpDisplaySettings) modeSettings).informationType.equals("") &&
                    information.getType().equals(TcpInformation.InformationType.RAW)) {
                if (information.isStringAvailable()) {
                    textView.setText(information.getStringValue());
                }
            } else if (information.getType().equals(
                    TcpInformation.InformationType.CONNECTIVITY_CHANGE) &&
                    information.isBooleanAvailable()) {
                if (((TcpDisplaySettings) modeSettings).informationType.equals(
                        TcpInformation.InformationType.CONNECTIVITY_CHANGE.toString())) {
                    // This display shows the connectivity
                    textView.setText(information.getBooleanValue() ?
                            R.string.response_connected : R.string.response_not_connected);
                } else if (!information.getBooleanValue()) {
                    // Not connected, so clear content
                    textView.setText("");
                }
            } else if (information.isClassifiedResponse() &&
                    information.getResponseClassifier().equals(
                            ((TcpDisplaySettings) modeSettings).informationType)) {
                textView.setText(information.getStringValue());
            }
        }
    }

    public String getClickCommand() {
        return clickCommand;
    }
    public String getDoubleClickCommand() {
        return doubleClickCommand;
    }
    public String getLongClickCommand() {
        return longClickCommand;
    }
    public ModeSettings getModeSettings() {
        return modeSettings;
    }
    public int getHorizontalTextGravity() {
        return horizontalTextGravity;
    }
    public void setValues(String ip, TcpConnectionManager.ReceiverType type,
                          ModeSettings modeSettings, String clickCommand, String doubleClickCommand,
                          String longClickCommand, int horizontalTextGravity) {
        if (connection != null) {
            tcpConnectionManager.stopUpdate(this);
        }
        if (this.modeSettings instanceof ClockSettings) {
            TimeSingleton.getInstance().unregisterListener(this);
        }
        this.ip = ip;
        this.type = type;
        this.modeSettings = modeSettings;
        this.clickCommand = clickCommand;
        this.doubleClickCommand = doubleClickCommand;
        this.longClickCommand = longClickCommand;

        this.horizontalTextGravity = horizontalTextGravity;
        textView.setGravity(Gravity.CENTER_VERTICAL | horizontalTextGravity);

        if (modeSettings instanceof TcpDisplaySettings ||
                !clickCommand.equals("") || !doubleClickCommand.equals("") ||
                !longClickCommand.equals("")) {
            connection = tcpConnectionManager.requireConnection(this);
        } else {
            connection = null;
        }

        if (modeSettings instanceof TcpDisplaySettings) {
            textView.setText("");
            if (((TcpDisplaySettings) modeSettings).informationType.equals(
                    TcpInformation.InformationType.CONNECTIVITY_CHANGE.toString())) {
                // This display shows the connectivity
                textView.setText(connection.isConnected() ?
                        R.string.response_connected : R.string.response_not_connected);
            } else {
                update(connection.getBufferedInformation(
                        ((TcpDisplaySettings) modeSettings).informationType));
            }
        } else if (modeSettings instanceof StaticTextSettings) {
            if (DEBUG && ((StaticTextSettings) modeSettings).text.equals("--debug")) {
                textView.setText(Util.getDebugText());
                baseLayout.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
            } else {
                textView.setText(((StaticTextSettings) modeSettings).text);
            }
        } else if (modeSettings instanceof ClockSettings) {
            TimeSingleton.getInstance(Util.getPreferenceInt(sharedPreferences,
                    Preferences.KEY_TIME_UPDATE_INTERVAL, 500)).registerListener(this);
        }
    }

    public TcpConnectionManager.TcpConnection getConnection() {
        return connection;
    }

    @Override
    protected void editActionEdit() {
        new AddDisplayFragmentDialog()
                .setEditFragment(this, connection)
                .show(getFragmentManager(), "AddDisplayFragmentDialog");
    }
}
