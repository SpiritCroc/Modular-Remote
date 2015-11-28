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

import android.app.Activity;
import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Arrays;

import de.spiritcroc.modular_remote.Display;
import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.Preferences;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.TcpInformation;
import de.spiritcroc.modular_remote.TimeSingleton;
import de.spiritcroc.modular_remote.Util;

public class PageContainerFragment extends ModuleFragment implements Container,
        TcpConnectionManager.TcpUpdateInterface, Display, TimeSingleton.TimeListener {
    private static final String ARG_NAME = "name";
    private static final String ARG_ACTION_BAR_MODE_SETTINGS = "actionBarModeSettings";
    private static final String ARG_USE_HARDWARE_BUTTONS = "useHardwareButtons";
    private static final String ARG_IP = "ip";
    private static final String ARG_TYPE = "type";
    private static final String ARG_VOLUME_UP_COMMAND = "upCommand";
    private static final String ARG_VOLUME_DOWN_COMMAND = "downCommand";
    private static final String LOG_TAG = PageContainerFragment.class.getSimpleName();

    private ModeSettings actionBarModeSettings;
    private String actionBarTitle = "";
    private String name, volumeUpCommand = "", volumeDownCommand = "";
    private RelativeLayout baseViewGroup;
    private String recreationKey;
    private ArrayList<ModuleFragment> fragments;
    private MenuItem menuResetItem;
    // Prevents to return wrong recreation key when fragment was not even shown
    private boolean built = false;
    private boolean menuEnabled = false;
    private boolean useHardwareButtons;
    private TcpConnectionManager.TcpConnection connection, actionBarConnection;
    private String ip;
    private TcpConnectionManager.ReceiverType type;
    private long pageId = -1;//pageId == -1 → pageId not set yet
    private SharedPreferences sharedPreferences;
    private boolean created = false;

    public PageContainerFragment(){
        fragments = new ArrayList<>();
    }

    public static PageContainerFragment newInstance(String name,ModeSettings actionBarModeSettings,
                                                    boolean useHardwareButtons, String ip,
                                                    TcpConnectionManager.ReceiverType type,
                                                    String volumeUpCommand,
                                                    String volumeDownCommand) {
        PageContainerFragment fragment = new PageContainerFragment();
        Bundle args = new Bundle();
        args.putString(ARG_NAME, name);
        args.putString(ARG_ACTION_BAR_MODE_SETTINGS, actionBarModeSettings.getRecreationKey());
        args.putBoolean(ARG_USE_HARDWARE_BUTTONS, useHardwareButtons);
        if (useHardwareButtons) {
            args.putString(ARG_IP, ip);
            args.putString(ARG_TYPE, type.toString());
            args.putString(ARG_VOLUME_UP_COMMAND, volumeUpCommand);
            args.putString(ARG_VOLUME_DOWN_COMMAND, volumeDownCommand);
        }
        fragment.setArguments(args);
        return fragment;
    }

    public void edit(String name, ModeSettings actionBarModeSettings,
                     boolean useHardwareButtons, String ip, TcpConnectionManager.ReceiverType type,
                     String volumeUpCommand, String volumeDownCommand) {
        if (this.actionBarModeSettings instanceof ClockSettings) {
            TimeSingleton.getInstance().unregisterListener(this);
        } else if (this.actionBarModeSettings instanceof TcpDisplaySettings) {
            TcpConnectionManager.getInstance(getActivity().getApplicationContext())
                    .stopUpdate(actionBarUpdateListener);
        }
        if (this.useHardwareButtons) {
            TcpConnectionManager.getInstance(getActivity().getApplicationContext())
                    .stopUpdate(this);
        }
        this.name = name;
        this.actionBarModeSettings = actionBarModeSettings;
        this.useHardwareButtons = useHardwareButtons;
        if (useHardwareButtons) {
            this.ip = ip;
            this.type = type;
            this.volumeUpCommand = volumeUpCommand;
            this.volumeDownCommand = volumeDownCommand;
            if (getActivity() != null) {
                connection =
                        TcpConnectionManager.getInstance(getActivity().getApplicationContext())
                                .requireConnection(this);
            }
        } else if (getActivity() != null) {
            TcpConnectionManager.getInstance(getActivity().getApplicationContext())
                    .stopUpdate(this);
        }

        if (actionBarModeSettings instanceof TcpDisplaySettings) {
            setActionBarTitle("");
            if (getActivity() != null) {
                actionBarConnection =
                        TcpConnectionManager.getInstance(getActivity().getApplicationContext())
                                .requireConnection(actionBarUpdateListener);
            }
            if (((TcpDisplaySettings) actionBarModeSettings).informationType.equals(
                    TcpInformation.InformationType.CONNECTIVITY_CHANGE.toString())) {
                // Show the connectivity
                if (actionBarConnection != null) {
                    setActionBarTitle(Util.getACString(actionBarConnection.isConnected() ?
                            R.string.response_connected : R.string.response_not_connected));
                }
            } else {
                actionBarUpdateListener.update(actionBarConnection.getBufferedInformation(
                        ((TcpDisplaySettings) actionBarModeSettings).informationType));
            }
        } else if (actionBarModeSettings instanceof StaticTextSettings) {
            setActionBarTitle(((StaticTextSettings) actionBarModeSettings).text);
        } else if (actionBarModeSettings instanceof ClockSettings) {
            if (sharedPreferences != null) {
                TimeSingleton.getInstance(Util.getPreferenceInt(sharedPreferences,
                        Preferences.KEY_TIME_UPDATE_INTERVAL, 500)).registerListener(this);
            }
        }

        updateActivity();
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

        if (getArguments() != null) {
            name = getArguments().getString(ARG_NAME);
            String actionBarModeSettingsKey =
                    getArguments().getString(ARG_ACTION_BAR_MODE_SETTINGS);
            try {
                actionBarModeSettings =
                        ModeSettings.recoverFromRecreationKey(actionBarModeSettingsKey);
            } catch (Exception e) {
                Log.w(LOG_TAG, "onCreate: could not recover actionBarModeSettings with key " +
                        actionBarModeSettingsKey);
                e.printStackTrace();
            }
            useHardwareButtons = getArguments().getBoolean(ARG_USE_HARDWARE_BUTTONS);
            if (useHardwareButtons) {
                ip = getArguments().getString(ARG_IP);
                type = TcpConnectionManager.ReceiverType
                        .valueOf(getArguments().getString(ARG_TYPE));
                volumeUpCommand = getArguments().getString(ARG_VOLUME_UP_COMMAND);
                volumeDownCommand = getArguments().getString(ARG_VOLUME_DOWN_COMMAND);
            }
            updateActivity();
        }  else {
            Log.e(LOG_TAG, "onCreate: getArguments()==null");
            name = "";
        }

        edit(name, actionBarModeSettings, useHardwareButtons, ip, type, volumeUpCommand,
                volumeDownCommand);
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        View view = inflater.inflate(R.layout.fragment_page_container, container, false);

        baseViewGroup = (RelativeLayout) view.findViewById(R.id.base_view_group);
        baseViewGroup.setOnDragListener(this);
        baseViewGroup.setId(Util.generateViewId());

        fragments.clear();
        restoreContentFromRecreationKey();
        built = true;

        return view;
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (useHardwareButtons) {
            TcpConnectionManager.getInstance(getActivity().getApplicationContext())
                    .stopUpdate(this);
        }
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recreationKey = getRecreationKey();
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        if (connection == null) {
            super.onPrepareOptionsMenu(menu);
            return;
        }
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
        if (connection == null) {
            return super.onOptionsItemSelected(item);
        }
        if (menuEnabled && menuResetItem != null &&
                menuResetItem.getTitle().equals(item.getTitle())){
            connection.reset();
            return true;// Connection only needs to reset once
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setMenuEnabled(boolean menuEnabled) {
        this.menuEnabled = menuEnabled;
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).setMenuEnabled(menuEnabled);
        }
    }
    @Override
    public void onRemove() {
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).onRemove();
        }
    }

    @Override
    public String getReadableName() {
        return Util.getACString(R.string.fragment_page_container) + " " + name;
    }
    @Override
    public Spannable getContentReadableName(@Nullable String prefix) {
        return Util.getContainerContentReadableName(this, prefix);
    }
    @Override
    public String getRecreationKey() {
        if (built) {
            String separator = Util.getSeparator(this);
            String key = PAGE_CONTAINER_FRAGMENT + SEP + pageId + SEP + name + SEP +
                    actionBarModeSettings.getRecreationKey() + SEP +
                    useHardwareButtons + (useHardwareButtons ?
                    (SEP + ip + SEP + connection.getType().toString() + SEP +
                            volumeUpCommand + SEP + volumeDownCommand + SEP) : SEP) + separator;
            for (int i = 0; i < fragments.size(); i++) {
                ModuleFragment fragment = fragments.get(i);
                key += fragment.getRecreationKey() + separator;
            }
            return fixRecreationKey(key);
        } else {
            return recreationKey;
        }
    }
    @Override
    public void resize() {
        for (ModuleFragment fragment: fragments) {
            fragment.resize();
        }
    }
    @Override
    public double getArgWidth() {
        return -1;// Has no such setting
    }
    @Override
    public double getArgHeight() {
        return -1;// Has no such setting
    }
    public boolean onKeyDown (int keyCode) {
        if (useHardwareButtons && connection != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                    connection.sendRawCommand(volumeUpCommand);
                    return true;
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    connection.sendRawCommand(volumeDownCommand);
                    return true;
            }
        }
        return false;
    }
    public boolean onKeyUp(int keyCode) {
        if (useHardwareButtons && connection != null) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_VOLUME_UP:
                case KeyEvent.KEYCODE_VOLUME_DOWN:
                    return true;//don't beep
            }
        }
        return false;
    }
    public String getName() {
        return name;
    }
    public boolean isUseHardwareButtons() {
        return useHardwareButtons;
    }
    public String getVolumeUpCommand() {
        return volumeUpCommand;
    }
    public String getVolumeDownCommand() {
        return volumeDownCommand;
    }
    public TcpConnectionManager.TcpConnection getConnection() {
        return connection;
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
    public static PageContainerFragment recoverFromRecreationKey(String key) {
        try {
            String[] args = Util.split(key, SEP, 0);
            long pageId = Long.parseLong(args[1]);
            String name = args[2];
            ModeSettings actionBarModeSettings = ModeSettings.recoverFromRecreationKey(args[3]);
            boolean useHardwareButtons = Boolean.parseBoolean(args[4]);
            String ip = null;
            TcpConnectionManager.ReceiverType type = null;
            String volumeUpCommand = null, volumeDownCommand = null;
            if (useHardwareButtons) {
                ip = args[5];
                type = TcpConnectionManager.ReceiverType.valueOf(args[6]);
                volumeUpCommand = args[7];
                volumeDownCommand = args[8];
            }

            PageContainerFragment fragment = newInstance(name, actionBarModeSettings,
                    useHardwareButtons, ip, type, volumeUpCommand, volumeDownCommand);
            // Workaround to get page names directly after loading content of activity
            // (getName would return null);
            fragment.edit(name, actionBarModeSettings, useHardwareButtons, ip, type,
                    volumeUpCommand, volumeDownCommand);
            fragment.setRecreationKey(key);
            return fragment.setPageId(pageId);
        } catch (Exception e) {
            Log.e(LOG_TAG, "recoverFromRecreationKey: illegal key: " + key);
            Log.e(LOG_TAG, "Got exception: " + e);
            return null;
        }
    }
    public static String getPageNameFromRecreationKey(String key) {// Needed for launcher shortcuts
        return Util.split(key, SEP, 3)[2];
    }
    public static long getPageIdFromRecreationKey(String key) {// Needed for launcher shortcuts
        try {
            return Long.parseLong(Util.split(key, SEP, 0)[1]);
        } catch (Exception e) {
            return -1;
        }
    }
    private void setRecreationKey(String recreationKey) {
        this.recreationKey = recreationKey;
    }
    private void restoreContentFromRecreationKey() {
        if (recreationKey != null) {
            Util.restoreContentFromRecreationKey(this, recreationKey, menuEnabled);
        }
    }
    @Override
    public ModuleFragment copy(){
        PageContainerFragment fragment =
                newInstance(name, actionBarModeSettings.copy(), useHardwareButtons, ip, type,
                        volumeUpCommand, volumeDownCommand);
        fragment.init(getActivity(), true).setRecreationKey(getRecreationKey());
        return fragment;
    }
    @Override
    public Container getParent() {//has no official Container as parent
        return null;
    }
    @Override
    public void setParent(Container parent) {}
    public int getDepth() {
        return 1;
    }

    @Override
    public void addFragment(final ModuleFragment fragment, boolean post) {
        if (post) {
            baseViewGroup.post(new AddFragment(fragment));
        } else {
            new AddFragment(fragment).run();
        }
    }
    private class AddFragment implements Runnable {
        private ModuleFragment fragment;
        private AddFragment(ModuleFragment fragment){
            this.fragment = fragment;
        }
        @Override
        public void run() {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction().add(baseViewGroup.getId(), fragment)
                        .commit();
                fragments.add(fragment);
                fragment.setMenuEnabled(menuEnabled);
                fragment.setParent(PageContainerFragment.this);
                if (isDragModeEnabled()) {
                    fragment.onStartDragMode();
                }
                fragment.setContainerDragEnabled(isContainerDragEnabled());
            } else {
                Log.e(LOG_TAG, "Can't add " + fragment);
            }
        }
    }
    @Override
    public void removeFragment(ModuleFragment fragment, boolean callOnRemove) {
        fragments.remove(fragment);
        if (callOnRemove) {
            fragment.onRemove();
        }
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().remove(fragment).commit();
    }
    private void updateActivity() {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).notifyDataSetChanged();
        }
    }
    @Override
    public Container[] getAllContainers() {
        return Util.getAllContainers(this);
    }
    @Override
    public ModuleFragment[] getAllFragments() {
        ArrayList<ModuleFragment> list = new ArrayList<>();
        list.add(this);
        for (int i = 0; i < fragments.size(); i++) {
            if (fragments.get(i) instanceof Container) {
                ModuleFragment[] children = ((Container) fragments.get(i)).getAllFragments();
                list.addAll(Arrays.asList(children));
            } else {
                list.add(fragments.get(i));
            }
        }
        return list.toArray(new ModuleFragment[list.size()]);
    }
    @Override
    public ModuleFragment[] getFragments() {
        return fragments.toArray(new ModuleFragment[fragments.size()]);
    }
    @Override
    public boolean isEmpty() {
        return fragments.isEmpty();
    }
    @Override
    public int getFragmentCount() {
        return fragments.size();
    }

    @Override
    public boolean scrollsX() {
        // If page removal allowed, then more than one page exists, meaning that we can scroll
        return getActivity() instanceof MainActivity &&
                ((MainActivity) getActivity()).isPageRemovalAllowed();
    }
    @Override
    public boolean scrollsY() {
        return false;
    }
    @Override
    public int getScrollX() {
        return 0;
    }
    @Override
    public int getScrollY() {
        return 0;
    }

    @Override
    public boolean isConnected() {
        for (int i = 0; i < fragments.size(); i++) {
            if (fragments.get(i).isConnected()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Call this method when creating this page for the first time in order to generate an pageId
     */
    public PageContainerFragment init (Context context) {
        return init(context, false);
    }
    private PageContainerFragment init(Context context, boolean forceNewId) {
        if (pageId == -1 || forceNewId) {
            pageId = Util.getPageId(context);
        } else {
            Log.w(LOG_TAG, "init: pageId already set");
        }
        return this;
    }
    public long getPageId() {
        return pageId;
    }
    /**
     * Call this method when recreating this page in order to retain pageId
     */
    private PageContainerFragment setPageId(long pageId) {
        this.pageId = pageId;
        return this;
    }


    @Override
    public void update(final TcpInformation information) {} // Not required

    private void updateTcpActionBar(TcpInformation information) {
        if (actionBarModeSettings instanceof TcpDisplaySettings) {
            if (information == null) {
                return;
            }
            if (((TcpDisplaySettings) actionBarModeSettings).informationType.equals("") &&
                    information.getType().equals(TcpInformation.InformationType.RAW)) {
                if (information.isStringAvailable()) {
                    setActionBarTitle(information.getStringValue());
                }
            } else if (information.getType().equals(
                    TcpInformation.InformationType.CONNECTIVITY_CHANGE) &&
                    information.isBooleanAvailable()) {
                if (((TcpDisplaySettings) actionBarModeSettings).informationType.equals(
                        TcpInformation.InformationType.CONNECTIVITY_CHANGE.toString())) {
                    // This display shows the connectivity
                    setActionBarTitle(Util.getACString(information.getBooleanValue() ?
                            R.string.response_connected : R.string.response_not_connected));
                } else if (!information.getBooleanValue()) {
                    // Not connected, so clear content
                    setActionBarTitle("");
                }
            } else if (information.isClassifiedResponse() &&
                    information.getResponseClassifier().equals(
                            ((TcpDisplaySettings) actionBarModeSettings).informationType)) {
                setActionBarTitle(information.getStringValue());
            }
        }
    }

    @Override
    public ModeSettings getModeSettings() {
        return actionBarModeSettings;
    }

    @Override
    public void setTime(String time) {
        if (actionBarModeSettings instanceof ClockSettings) {
            setActionBarTitle(time);
        }
    }

    private void setActionBarTitle(String text) {
        actionBarTitle = text;
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            ((MainActivity) activity).setActionBarTitle(this, text);
        }
    }
    public String getActionBarTitle() {
        return actionBarTitle;
    }

    private TcpConnectionManager.TcpUpdateInterface actionBarUpdateListener =
            new TcpConnectionManager.TcpUpdateInterface() {
                @Override
                public String getIp() {
                    return ((TcpDisplaySettings)actionBarModeSettings).ip;
                }
                @Override
                public TcpConnectionManager.ReceiverType getType() {
                    return ((TcpDisplaySettings)actionBarModeSettings).receiverType;
                }
                @Override
                public void update(final TcpInformation information) {
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            updateTcpActionBar(information);
                        }
                    });
                }
                @Override
                public void setConnectionValues(String ip, TcpConnectionManager.ReceiverType type) {
                    ((TcpDisplaySettings)actionBarModeSettings).ip = ip;
                    ((TcpDisplaySettings)actionBarModeSettings).receiverType = type;
                }
            };


    @Override
    public void onStartDragMode() {
        super.onStartDragMode();
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).onStartDragMode();
        }
    }

    @Override
    public void onStopDragMode() {
        super.onStopDragMode();
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).onStopDragMode();
        }
    }

    @Override
    public void setContainerDragEnabled(boolean containerDragEnabled) {
        super.setContainerDragEnabled(containerDragEnabled);
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).setContainerDragEnabled(containerDragEnabled);
        }
    }
    public void onContentMoved() {
        // Nothing to do here
    }

    @Override
    protected int getDragModeBgColor() {
        return Color.TRANSPARENT;
    }

    @Override
    protected void prepareEditMenu(Menu menu) {
        // Don't call super: don't enable menu at all
        // No possibility to reach menu (longPress disabled)
    }
    @Override
    protected void editActionEdit() {
        // No possibility to reach menu (longPress disabled)
        /*
        new AddPageDialog().setPage(this, connection)
                .show(getFragmentManager(), "AddPageDialog");
        */
    }
    @Override
    protected void editActionMove() {
        // No possibility to reach menu (longPress disabled)
    }
    @Override
    protected void editActionClone() {
        // No possibility to reach menu (longPress disabled)
    }
    @Override
    protected void editActionRemove() {
        // No possibility to reach menu (longPress disabled)
        /*
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            if (((MainActivity) activity).isPageRemovalAllowed()) {
                new ConfirmRemovePageDialog().setPage(this)
                        .show(getFragmentManager(), "ConfirmRemovePageDialog");
            } else {
                Toast.makeText(activity,
                        R.string.error_last_page_removal_not_allowed,
                        Toast.LENGTH_SHORT).show();
            }
        } else {
            Log.e(LOG_TAG, "!(activity instanceof MainActivity)");
        }
        */
    }
}
