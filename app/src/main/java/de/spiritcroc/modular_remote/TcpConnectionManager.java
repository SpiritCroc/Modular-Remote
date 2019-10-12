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

package de.spiritcroc.modular_remote;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;
import android.os.Handler;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;

public class TcpConnectionManager {
    private static final boolean DEBUG = BuildConfig.DEBUG;
    private static final String LOG_TAG = TcpConnectionManager.class.getSimpleName();

    public enum ReceiverType {UNSPECIFIED, PIONEER}

    public static final int SUBMENU_INPUT = -1;
    // Two listening modes as PIONEER features two different arrays
    // If they are the same, just return the same array for both
    public static final int SUBMENU_LISTENING_MODE_SET = -2;
    public static final int SUBMENU_LISTENING_MODE_GET = -3;
    public static final int MENU_TCP_RESPONSES = -4;
    public static final int MENU_START_REQUESTS = -5;
    public static final int MENU_INIT_REQUESTS = -6;
    public static final int MENU_REQUEST_INIT_RESPONSES = -7;
    public static final int MENU_REQUEST_CLEAR_DISPLAY_RESPONSES = -8;
    public static final int MENU_STOP_CLEAR_DISPLAY_RESPONSES = -9;
    public static final int MENU_CLEARABLE_DISPLAYS = -10;

    public static final int CUSTOMIZABLE_SUBMENU_FLAG_HIDE_ITEMS = 1;
    public static final int CUSTOMIZABLE_SUBMENU_FLAG_RECEIVE_NAMES = 2;

    private static final String COMMAND_CHAIN_READABLE_SEPARATOR = ": ";

    private static TcpConnectionManager instance;
    private SharedPreferences sharedPreferences;
    private Context applicationContext;

    private TcpConnectionManager(Context context) {
        tcpConnections = new ArrayList<>();
        this.applicationContext = context.getApplicationContext();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(applicationContext);

    }
    public static TcpConnectionManager getInstance(Context context) {
        if (instance == null)
            instance = new TcpConnectionManager(context);
        return instance;
    }

    private ArrayList<TcpConnection> tcpConnections;

    public void recreateConnectionsFromRecreationKey(String recreationKey) {
        String[] keys = Util.split(recreationKey, Util.RK_ARRAY_ATTRIBUTE_SEPARATOR, 0);
        for (String key: keys) {
            try {
                String[] args = Util.split(key, Util.RK_ATTRIBUTE_SEPARATOR, 0);
                String ip = args[0];
                ReceiverType type = TcpConnectionManager.ReceiverType.valueOf(args[1]);
                TcpConnection connection = requireConnection(ip, type);
                if (args.length <= 2) {
                    // No customized menus
                    continue;
                }
                String[] customizedMenuKeys = Util.split(args[2],
                        Util.RK_CUSTOMIZED_MENU_SEPARATOR, 0);
                for (int i = 0; i < customizedMenuKeys.length; i++) {
                    String[] menuArgs = Util.split(customizedMenuKeys[i],
                            Util.RK_CUSTOMIZED_MENU_ATTRIBUTE_SEPARATOR, 0);
                    int menu = Integer.parseInt(menuArgs[0]);
                    TcpConnection.CustomizedMenu cMenu = connection.requireCustomizedMenu(menu,
                            applicationContext);
                    if (menuArgs.length % 3 != 1) {
                        Log.w(LOG_TAG, "recreateConnectionsFromRecreationKey: illegal key: " + key);
                        Log.w(LOG_TAG, "menuArgs.length % 3 != 1");
                        if (DEBUG) Log.d(LOG_TAG, "menuArgs.length == " + menuArgs.length);
                    } else {
                        int pos = 0;
                        ArrayList<String> values = new ArrayList<>(Arrays.asList(cMenu.values));
                        for (int j = 1; j < menuArgs.length; j += 3, pos++) {
                            String name = menuArgs[j];
                            String value = menuArgs[j+1];
                            boolean hide = Boolean.parseBoolean(menuArgs[j+2]);
                            int index = values.indexOf(value);
                            if (index >= 0) {
                                cMenu.names[index] = name;
                                cMenu.hidden[index] = hide;
                            } else {
                                Log.w(LOG_TAG, "recreateConnectionsFromRecreationKey: Could not " +
                                        "find value " + value + " in menu " + menu);
                            }
                        }
                    }
                }
            } catch (Exception e) {
                Log.w(LOG_TAG, "recreateConnectionsFromRecreationKey: illegal key: " + key);
                Log.w(LOG_TAG, "Got exception: " + e);
            }
        }
    }
    public String getConnectionRecreationKey() {
        String key = "";
        for (int i = 0; i < tcpConnections.size(); i++) {
            if (!tcpConnections.get(i).shouldRemove) {
                key += tcpConnections.get(i).getRecreationKey() +
                        Util.RK_ARRAY_ATTRIBUTE_SEPARATOR;
            }
        }
        return key;
    }

    public TcpConnection requireConnection(TcpUpdateInterface listener) {
        TcpConnection connection = requireConnection(listener.getIp(), listener.getType());
        connection.addListener(listener);
        return connection;
    }
    private TcpConnection requireConnection(String ip, ReceiverType type) {
        TcpConnection connection = getTcpConnection(ip);
        if (connection == null) {
            connection = new TcpConnection(ip, type);
            if (DEBUG) Log.v(LOG_TAG, "requireConnection: Created connection to " + ip);
        } else if (!connection.updateType(type)) {
            Log.w(LOG_TAG, "ip " + ip + ": has already another ReceiverType set, " +
                    "cannot change to " + type);
        }
        return connection;
    }
    public void stopUpdate(TcpUpdateInterface listener) {
        TcpConnection connection = getTcpConnection(listener.getIp());
        if (connection != null) {
            connection.removeListener(listener);
        }
    }

    /**
     * Does not actually remove connection, as this might cause issues if it is still required.
     * Instead, it will just be removed from the recovery key, so it will only get recreated on
     * next start if it is required
     */
    public void removeConnection(TcpConnection connection) {
        connection.shouldRemove = true;
    }

    /**
     * Use this method for checking connection values, but not communicating (sending commands)!
     * If you want to send commands, use requireConnection
     */
    public TcpConnection getTcpConnection(String ip) {
        for (int i = 0; i < tcpConnections.size(); i++) {
            TcpConnection connection = tcpConnections.get(i);
            if (connection.tcpIp.equals(ip)) {
                return connection;
            }
        }
        return null;
    }

    public void refreshConnections() {
        for (int i = 0; i < tcpConnections.size(); i++) {
            tcpConnections.get(i).refresh();
        }
    }

    private synchronized void sendUpdateBroadcast(TcpConnection connection,
                                                  TcpInformation information){// Also buffers data
        if (information == null) {// No need for update
            return;
        }
        // If value == null, then it's only supposed to tell listeners to update (no content)
        if (information.isClassifiedResponse() && information.getStringValue() != null) {
            for (int i = 0; i < connection.informationBuffer.size(); i++) {// Buffer data
                if (connection.informationBuffer.get(i).getResponseClassifier()
                        .equals(information.getResponseClassifier())) {
                    connection.informationBuffer.remove(i);// Remove old buffer
                    break;
                }
            }
            connection.informationBuffer.add(information);
        }
        connection.updateListeners(information);
    }

    public interface TcpUpdateInterface {
        String getIp();
        ReceiverType getType();
        void update(TcpInformation information);
        void setConnectionValues(String ip, ReceiverType type);
    }

    public class TcpConnection implements Runnable {
        private volatile String tcpIp, ip;
        private volatile int port;
        private volatile ArrayList<TcpInformation> informationBuffer = new ArrayList<>();
        private volatile ReceiverType type;
        private volatile ArrayList<CustomizedMenu> customizedMenus = new ArrayList<>();
        private volatile boolean resetNow = false, connected = false, sentRequest = false;
        private volatile int reconnectionAttempts = 0;
        private Handler stopRequestHandler = new Handler();
        private Handler reconnectHandler = new Handler ();
        private volatile ArrayList<TcpUpdateInterface> listeners = new ArrayList<>();
        private Socket socket;
        private BufferedReader in;
        private BufferedWriter out;
        private Thread thread;
        private String[] startRequests, initRequests;
        private boolean shouldClearDisplays = false;
        private boolean shouldRemove = false;
        // 0 → start, -1 → wait, > 0 → current progress
        private int startRequestProgress = 0, initRequestsProgress = -1;

        private Runnable stopRequest = new Runnable() {
            @Override
            public void run() {
                stopRequestHandler.removeCallbacks(this);
                sentRequest = false;
            }
        };
        private Runnable checkConnection = new Runnable() {
            @Override
            public void run() {
                while (connected) {
                    int interval = Util.getPreferenceInt(sharedPreferences,
                            Preferences.CHECK_CONNECTIVITY_INTERVAL, 3000);
                    try {
                        connected = InetAddress.getByName(ip).isReachable(Math.max(interval, 100));
                        Thread.sleep(interval);
                    } catch (IOException e) {
                        connected = false;
                        if (DEBUG) Log.v(LOG_TAG, tcpIp + ": checkConnection: " + e);
                    } catch (InterruptedException e) {
                        if (DEBUG) Log.v(LOG_TAG, tcpIp + ": checkConnection: " + e);
                    }
                    if (!connected) {
                        if (DEBUG) Log.v(LOG_TAG, tcpIp + ": checkConnection: not connected");
                        close();
                    }
                }
            }
        };

        public TcpConnection(String tcpIp, ReceiverType type) {
            this.tcpIp = tcpIp;
            this.type = type;
            tcpConnections.add(this);

            Resources resources = applicationContext.getResources();
            startRequests = getAllPossibleCommands(resources, type, MENU_START_REQUESTS);
            initRequests = getAllPossibleCommands(resources, type, MENU_INIT_REQUESTS);

            thread = new Thread(this);
            thread.start();
        }

        public void addListener(TcpUpdateInterface listener) {
            if (!listeners.contains(listener)) {
                listeners.add(listener);
            }
        }
        public void removeListener(TcpUpdateInterface listener) {
            listeners.remove(listener);
        }
        public void updateListeners(TcpInformation information) {
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).update(information);
            }
        }

        /**
         * Changes type if previously unspecified
         * @return
         * True if type valid
         */
        public boolean updateType(ReceiverType type) {
            if (this.type.equals(ReceiverType.UNSPECIFIED)) {
                this.type = type;
            } else if (!type.equals(ReceiverType.UNSPECIFIED) && !this.type.equals(type)) {
                // Invalid type
                return false;
            }
            return true;
        }

        public void run() {
            resetNow = false;
            if (isHidden()) {
                // No connection required
                return;
            }
            ip = tcpIp;
            port = 23;
            if (tcpIp.contains(":")) {
                try {
                    int index = tcpIp.lastIndexOf(":");
                    ip = tcpIp.substring(0, index);
                    port = Integer.parseInt(tcpIp.substring(index + 1));
                } catch (Exception e) {
                    Log.i(LOG_TAG, tcpIp + ": illegal port");
                }
            }
            try {
                if (DEBUG) Log.d(LOG_TAG, "Try to connect to ip " + ip + " at port " + port);
                socket = new Socket(ip, port);
                connected = true;
                reconnectionAttempts = 0;
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
                sendUpdateBroadcast(this, new TcpInformation(
                        TcpInformation.InformationType.CONNECTIVITY_CHANGE, connected));
                new Thread(checkConnection).start();
                while (connected && !resetNow) {
                    if (!in.ready() && !sentRequest) {
                        // Request buffers if not already waiting for information
                        if (startRequestProgress < startRequests.length) {
                            sentRequest = true;
                            sendRawCommand(startRequests[startRequestProgress++]);
                            stopRequestHandler.postDelayed(stopRequest, 100);
                        } else if (initRequestsProgress >= 0 &&
                                initRequestsProgress < initRequests.length) {
                            sentRequest = true;
                            sendRawCommand(initRequests[initRequestsProgress++]);
                            stopRequestHandler.postDelayed(stopRequest, 100);
                        }
                    }
                    if (sentRequest) {
                        while (connected && !resetNow && sentRequest && !in.ready()) {
                            if (DEBUG) Log.v(LOG_TAG, tcpIp + ": relax");
                            relax();
                        }
                        if (!in.ready()) {
                            // Request failed, send a new request
                            if (DEBUG) Log.d(LOG_TAG, tcpIp + ": Last buffer request failed");
                            continue;
                        }
                    }
                    String input = "";
                    int buffer;
                    while (connected && !resetNow) {
                        buffer = in.read();

                        if (buffer == -1 || (char) buffer == '\r' || (char) buffer == '\n') {
                            break;
                        }
                        input += (char) buffer;
                    }

                    if (!input.equals("")) {
                        stopRequest.run();
                        if (DEBUG) Log.d(LOG_TAG, tcpIp + ": Received string : " + input);
                        sendUpdateBroadcast(this, classifyResponse(
                                applicationContext.getResources(), this, input));
                        sendUpdateBroadcast(this,
                                new TcpInformation(TcpInformation.InformationType.RAW, input));
                        specialTreatment(this, input);
                    }
                }
            }
            catch (IOException e) {
                if (DEBUG) Log.d(LOG_TAG, "Lost connection to " + tcpIp +
                        ": got exception: " + e);
            }
            if (DEBUG) {
                if (!connected) {
                    Log.d(LOG_TAG, "Lost connection to " + tcpIp);
                }
            }

            connected = false;
            sendUpdateBroadcast(this, new TcpInformation(
                    TcpInformation.InformationType.CONNECTIVITY_CHANGE, connected));

            reconnect.run();

            close();
        }
        private synchronized void close() {
            try {
                if (socket != null) {
                    socket.close();
                    if (DEBUG)  Log.v(LOG_TAG, "Successfully closed socket for " + tcpIp);
                }
            } catch (IOException e) {
                if (DEBUG) Log.d(LOG_TAG, "run: " + tcpIp + ": got exception: " + e +
                        " while trying to close the socket");
            }
        }
        private void relax() {
            try {
                Thread.sleep(50);
            }  catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        private Runnable reconnect = new Runnable() {
            @Override
            public void run() {
                reconnectHandler.removeCallbacks(this);
                if (!connected && reconnectionAttempts < Util.getPreferenceInt(sharedPreferences,
                        Preferences.RECONNECTION_ATTEMPTS, 2)) {
                    reconnectionAttempts++;
                    reset();
                    reconnectHandler.postDelayed(this, Util.getPreferenceInt(sharedPreferences,
                            Preferences.RECONNECTION_INTERVAL, 200));
                }
            }
        };

        /**
         * Reconnect if necessary
         */
        public void refresh() {
            if (!connected) {
                reset();
            }
        }
        public synchronized void reset() {
            informationBuffer.clear();
            startRequestProgress = 0;
            initRequestsProgress = -1;

            resetNow = true;
            thread = new Thread(this);
            thread.start();
        }

        /**
         * @return
         * buffered data
         */
        public TcpInformation getBufferedInformation(String informationClassifier) {
            if (!connected) {
                return new TcpInformation(TcpInformation.InformationType.NONE);
            }
            if (shouldClearDisplays) {
                String[] clearableDisplays = getCommandValueArrayFromResource(
                        applicationContext.getResources(), type, MENU_CLEARABLE_DISPLAYS);
                if (Arrays.asList(clearableDisplays).contains(informationClassifier)) {
                    return new TcpInformation(informationClassifier, "", null);
                }
            }
            for (int i = 0; i < informationBuffer.size(); i++) {
                if (informationBuffer.get(i).getResponseClassifier()
                        .equals(informationClassifier)) {
                    return informationBuffer.get(i);
                }
            }
            return new TcpInformation(TcpInformation.InformationType.NONE);
        }
        public boolean containsBufferedInformation(String rawResponse){
            for (int i = 0; i < informationBuffer.size(); i++) {
                if (informationBuffer.get(i).getRawResponse() != null &&
                        informationBuffer.get(i).getRawResponse()
                        .equals(rawResponse)) {
                    return true;
                }
            }
            return false;
        }

        public synchronized void sendRawCommand(String command) {
            if (Looper.getMainLooper().getThread() == Thread.currentThread()) {
                // Don't run on UI thread
                new Thread(new SendRawCommandRunnable(command)).start();
            } else {
                new SendRawCommandRunnable(command).run();
            }
        }

        private class SendRawCommandRunnable implements Runnable {
            String command;
            public SendRawCommandRunnable(String command) {
                this.command = command;
            }
            @Override
            public void run() {
                realSendRawCommand(command);
            }
        }

        private synchronized void realSendRawCommand(String command) {
            refresh();
            if (connected && socket != null && socket.isConnected() && out != null) {
                try {
                    out.write(command + "\r");
                    out.flush();
                    if (DEBUG) Log.d(LOG_TAG, tcpIp + ": Sent string: " + command);
                } catch (IOException e) {
                    if (DEBUG) Log.d(LOG_TAG, tcpIp + ": Could not send string: \"" + command +
                            "\"; got exception: " + e);
                }
            } else Log.i(LOG_TAG, tcpIp + ": Could not send string: \"" + command +
                    "\"; socket is not connected");
        }

        public String getIp() {
            return tcpIp;
        }
        public ReceiverType getType() {
            return type;
        }
        public void setType(ReceiverType type) {
            this.type = type;
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).setConnectionValues(tcpIp, type);
            }
        }
        public void setValues(String ip, ReceiverType type) {
            tcpIp = ip;
            this.type = type;
            for (int i = 0; i < listeners.size(); i++) {
                listeners.get(i).setConnectionValues(tcpIp, type);
            }
            reset();
        }
        public boolean isConnected() {
            return connected;
        }

        /**
         * @return
         * Whether this connection should not get displayed to users
         */
        public boolean isHidden() {
            return "".equals(tcpIp);
        }

        public class CustomizedMenu {
            private int menuValue;
            public String[] names;
            public String[] values;
            public boolean[] hidden;
            public CustomizedMenu(int menuValue, String[] names, String[] values) {
                this.menuValue = menuValue;
                this.names = names;
                this.values = values;
                getHidden();
            }
            public int getMenuValue() {
                return menuValue;
            }
            public boolean[] getHidden() {
                if (hidden == null) {
                    resetHidden();
                } else if (hidden.length < values.length) {
                    // Don't hide by default
                    boolean[] newHidden = new boolean[values.length];
                    System.arraycopy(hidden, 0, newHidden, 0, hidden.length);
                    for (int i = hidden.length; i < newHidden.length; i++) {
                        newHidden[i] = false;
                    }
                    hidden = newHidden;
                }
                return hidden;
            }
            public void resetHidden() {
                hidden = new boolean[values.length];
                for (int i = 0; i < hidden.length; i++) {
                    hidden[i] = false;
                }
            }
            public String getRecreationKey() {
                if (values.length != names.length) {
                    Log.w(LOG_TAG, "CustomizedMenu.getRecreationKey: " +
                            "values.length != names.length");
                    return null;
                }
                if (values.length != hidden.length) {
                    Log.w(LOG_TAG, "CustomizedMenu.getRecreationKey: " +
                            "values.length != hidden.length");
                    return null;
                }
                String key = menuValue + Util.RK_CUSTOMIZED_MENU_ATTRIBUTE_SEPARATOR;
                for (int i = 0; i < values.length; i++) {
                    key += names[i] + Util.RK_CUSTOMIZED_MENU_ATTRIBUTE_SEPARATOR +
                            values[i] + Util.RK_CUSTOMIZED_MENU_ATTRIBUTE_SEPARATOR +
                            hidden[i] + Util.RK_CUSTOMIZED_MENU_ATTRIBUTE_SEPARATOR;
                }
                return key;
            }
        }

        private CustomizedMenu getCustomizedMenu(int menu) {
            for (int i = 0; i < customizedMenus.size(); i++) {
                if (customizedMenus.get(i).menuValue == menu) {
                    return customizedMenus.get(i);
                }
            }
            return null;
        }
        private String[] getCustomizedMenuNames(int menu, boolean includeHidden) {
            CustomizedMenu customizedMenu = getCustomizedMenu(menu);
            if (customizedMenu == null) {
                return null;
            }
            if (includeHidden) {
                return customizedMenu.names;
            }
            ArrayList<String> shownNames = new ArrayList<>();
            for (int i = 0; i < customizedMenu.names.length; i++) {
                if (!customizedMenu.hidden[i]) {
                    shownNames.add(customizedMenu.names[i]);
                }
            }
            return shownNames.toArray(new String[shownNames.size()]);
        }
        private String[] getCustomizedMenuValues(int menu) {
            CustomizedMenu customizedMenu = getCustomizedMenu(menu);
            if (customizedMenu == null) {
                return null;
            }
            ArrayList<String> shownValues = new ArrayList<>();
            for (int i = 0; i < customizedMenu.values.length; i++) {
                if (!customizedMenu.hidden[i]) {
                    shownValues.add(customizedMenu.values[i]);
                }
            }
            return shownValues.toArray(new String[shownValues.size()]);
        }
        public CustomizedMenu requireCustomizedMenu(int menu, Context context) {
            CustomizedMenu customizedMenu = getCustomizedMenu(menu);
            if (customizedMenu == null) {
                String[] names = getCommandNameArrayFromResource(context.getResources(), getType(),
                        menu);
                String[] values = getCommandValueArrayFromResource(context.getResources(),
                        getType(), menu);
                customizedMenu = new CustomizedMenu(menu, names, values);
                customizedMenus.add(customizedMenu);
            }
            return customizedMenu;
        }
        public String getRecreationKey() {
            String key = tcpIp + Util.RK_ATTRIBUTE_SEPARATOR + type + Util.RK_ATTRIBUTE_SEPARATOR;
            for (CustomizedMenu menu: customizedMenus) {
                String tmp = menu.getRecreationKey();
                if (tmp != null) {
                    key += tmp + Util.RK_CUSTOMIZED_MENU_SEPARATOR;
                }
            }
            return key;
        }
    }

    private String enhancedResponse (Context context, TcpConnection connection, String classifier,
                                     String rawData){
        ReceiverType type = connection.type;
        String result = null;
        String logInfo = "enhancedResponse: type \'" + type + "\', classifier \'" + classifier +
                "\', data \'" + rawData + "\'\n";
        switch (type) {
            case PIONEER:
                if (classifier.equals("VOL···") || classifier.equals("ZV··") || classifier.equals("YV··")) {// Volume
                    int i = 0;
                    try {
                        i = Integer.parseInt(rawData);
                    } catch (Exception e) {
                        Log.w(LOG_TAG, logInfo + "Got exception while trying to read volume: " + e);
                    }
                    result = i == 0 ? "" : (((double) i) * 0.5 - 80.5) +
                            context.getString(R.string.response_volume_db);
                } else if (classifier.equals("BA··") || classifier.equals("TR··")) {// Bass/treble
                    int i = 0;
                    try {
                        i = Integer.parseInt(rawData);
                    } catch (Exception e) {
                        Log.w(LOG_TAG, logInfo + "Got exception while trying to read bass/treble: "
                                + e);
                    }
                    result = (i > 6 ? "+" : "") + ((i - 6) * -1) +//testMe
                            context.getString(R.string.response_volume_db);
                } else if (classifier.substring(0,3).equals("CLV")) {// Channel level
                    int i = 0;
                    try {
                        i = Integer.parseInt(rawData);
                    } catch (Exception e) {
                        Log.w(LOG_TAG, logInfo + "Got exception while trying to read " +
                                "channel level: " + e);
                    }
                    result = (i > 50 ? "+" : "") + ((i - 50) * 0.5) +//testMe
                            context.getString(R.string.response_volume_db);
                } else if (classifier.equals("FL…")) {// Display
                    String copy = rawData.substring(2), display = "";
                    while (copy.length() >= 2) {
                        display += getDisplayCharacter(ReceiverType.PIONEER, copy.substring(0, 2));
                        copy = copy.substring(2);
                    }
                    result = display;
                } else if (classifier.equals("RGB…")) {// Input name
                    String value = rawData.substring(0, 2);

                    TcpConnection.CustomizedMenu inputMenu3 = connection.requireCustomizedMenu(3,
                            context);
                    for (int i = 0; i < inputMenu3.values.length; i++) {
                        if (inputMenu3.values[i].equals(value)) {
                            inputMenu3.names[i] = rawData.substring(3);
                        }
                    }
                    TcpConnection.CustomizedMenu inputMenu6 = connection.requireCustomizedMenu(6,
                            context);
                    TcpConnection.CustomizedMenu inputMenu22 = connection.requireCustomizedMenu(22,
                            context);
                    TcpConnection.CustomizedMenu inputMenu23 = connection.requireCustomizedMenu(23,
                            context);

                    TcpInformation buffer = connection.getBufferedInformation("FN··");
                    if (buffer != null && buffer.isStringAvailable()) {
                        for (int i = 0; i < inputMenu6.values.length; i++) {
                            // Update buffered information
                            if (inputMenu6.values[i].equals(value)) {
                                if (buffer.getStringValue().equals(inputMenu6.names[i])) {
                                    buffer.overwriteStringValue(rawData.substring(3));
                                }
                                break;
                            }
                        }
                    }
                    buffer = connection.getBufferedInformation("Z2F··");
                    if (buffer != null && buffer.isStringAvailable()) {
                        for (int i = 0; i < inputMenu23.values.length; i++) {
                            // Update buffered information
                            if (inputMenu23.values[i].equals(value)) {
                                if (buffer.getStringValue().equals(inputMenu23.names[i])) {
                                    buffer.overwriteStringValue(rawData.substring(3));
                                }
                                break;
                            }
                        }
                    }
                    buffer = connection.getBufferedInformation("Z3F··");
                    if (buffer != null && buffer.isStringAvailable()) {
                        for (int i = 0; i < inputMenu23.values.length; i++) {
                            // Update buffered information
                            if (inputMenu23.values[i].equals(value)) {
                                if (buffer.getStringValue().equals(inputMenu23.names[i])) {
                                    buffer.overwriteStringValue(rawData.substring(3));
                                }
                                break;
                            }
                        }
                    }

                    for (int i = 0; i < inputMenu6.values.length; i++) {
                        if (inputMenu6.values[i].equals(value)) {
                            inputMenu6.names[i] = rawData.substring(3);
                        }
                    }
                    for (int i = 0; i < inputMenu22.values.length; i++) {
                        if (inputMenu22.values[i].equals(value)) {
                            inputMenu22.names[i] = rawData.substring(3);
                        }
                    }
                    for (int i = 0; i < inputMenu23.values.length; i++) {
                        if (inputMenu23.values[i].equals(value)) {
                            inputMenu23.names[i] = rawData.substring(3);
                        }
                    }
                    result = "";
                    // Tell listeners to update
                    sendUpdateBroadcast(connection,
                            new TcpInformation(TcpInformation.InformationType.UPDATE_MENU, 3));
                    sendUpdateBroadcast(connection,
                            new TcpInformation(TcpInformation.InformationType.UPDATE_MENU, 6));
                    sendUpdateBroadcast(connection,
                            new TcpInformation(TcpInformation.InformationType.UPDATE_MENU, 22));
                    sendUpdateBroadcast(connection,
                            new TcpInformation(TcpInformation.InformationType.UPDATE_MENU, 23));
                }
                break;
            default:
                Log.d(LOG_TAG, "enhancedResponse: unhandled Receiver (brand): " + type);
                return result;
        }
        return result;
    }

    /**
     * @return
     * False if it seems to be invalid
     */
    public static boolean translateRawSubmenu(ReceiverType type, String classifier,
                                              Util.StringReference raw) {
        switch (type) {
            case PIONEER:
                if (classifier.equals("···VL") || classifier.equals("··ZV") || classifier.equals("··YV")) {// Volume
                    try {
                        String volume = "" + (int) (Double.parseDouble(raw.value) * 2 + 161);
                        while (volume.length() < 3) {
                            volume = "0" + volume;
                        }
                        raw.value = volume + classifier.substring(3, 5);
                        return true;
                    } catch (Exception e) {
                        Log.w(LOG_TAG, "translateRawSubmenu: Got exception while trying to read " +
                                "volume: " + e);
                        return false;
                    }
                }
                break;
            default:
                if (DEBUG) Log.v(LOG_TAG, "translateRawSubmenu: unhandled Receiver (brand): " +
                        type);
        }
        raw.value = Util.createCommandChain(classifier, raw.value);
        return true;
    }
    public static void getMessageForRawSubmenu(Context context, ReceiverType type,
                                               String classifier, Util.StringReference title,
                                               Util.StringReference summary) {
        switch (type) {
            case PIONEER:
                if (classifier.equals("···VL") || classifier.equals("··ZV") || classifier.equals("··YV")) { //Volume
                    title.value = context.getString(R.string.dialog_title_enter_volume);
                    summary.value = context.getString(R.string.dialog_summary_enter_volume_db);
                }
                break;
        }
    }
    private String getDisplayCharacter(ReceiverType type, String data) {
        switch (type) {
            case PIONEER:
                if (data.length()!=2) {
                    Log.e(LOG_TAG, "getDisplayCharacter: passed String for PIONEER should have " +
                            "exactly 2 characters");
                    return "";  //wrong length
                }
                final String[][] translation  = {
                        {"00", " "},
                        {"01", "⟳x"},// ⤨ does not show
                        {"02", "⟳"},
                        {"03", "x"},// ⤨ does not show
                        {"04", "▲▼"},// ⧎ does not show
                        {"05", "DOL"},
                        {"06", "BY"},
                        {"07", "I"},
                        {"08", "II"},
                        {"09", "◀"},
                        {"0A", "▶"},
                        {"0B", "♡"},
                        {"0C", "."},
                        {"0D", ".0"},
                        {"0E", ".5"},
                        {"0F", "Ω"},
                        {"10", "0"},
                        {"11", "1"},
                        {"12", "2"},
                        {"13", "3"},
                        {"14", "4"},
                        {"15", "5"},
                        {"16", "6"},
                        {"17", "7"},
                        {"18", "8"},
                        {"19", "9"},
                        {"1A", "A"},
                        {"1B", "B"},
                        {"1C", "C"},
                        {"1D", "F"},
                        {"1E", "M"},
                        {"1F", " ̄"},
                        {"20", " "},
                        {"21", "!"},
                        {"22", "\""},
                        {"23", "#"},
                        {"24", "$"},
                        {"25", "%"},
                        {"26", "&"},
                        {"27", "\'"},
                        {"28", "("},
                        {"29", ")"},
                        {"2A", "*"},
                        {"2B", "+"},
                        {"2C", ","},
                        {"2D", "-"},
                        {"2E", "."},
                        {"2F", "/"},
                        {"30", "0"},
                        {"31", "1"},
                        {"32", "2"},
                        {"33", "3"},
                        {"34", "4"},
                        {"35", "5"},
                        {"36", "6"},
                        {"37", "7"},
                        {"38", "8"},
                        {"39", "9"},
                        {"3A", ":"},
                        {"3B", ";"},
                        {"3C", "<"},
                        {"3D", "="},
                        {"3E", ">"},
                        {"3F", "?"},
                        {"40", "@"},
                        {"41", "A"},
                        {"42", "B"},
                        {"43", "C"},
                        {"44", "D"},
                        {"45", "E"},
                        {"46", "F"},
                        {"47", "G"},
                        {"48", "H"},
                        {"49", "I"},
                        {"4A", "J"},
                        {"4B", "K"},
                        {"4C", "L"},
                        {"4D", "M"},
                        {"4E", "N"},
                        {"4F", "O"},
                        {"50", "P"},
                        {"51", "Q"},
                        {"52", "R"},
                        {"53", "S"},
                        {"54", "T"},
                        {"55", "U"},
                        {"56", "V"},
                        {"57", "W"},
                        {"58", "X"},
                        {"59", "Y"},
                        {"5A", "Z"},
                        {"5B", "["},
                        {"5C", "\\"},
                        {"5D", "]"},
                        {"5E", "^"},
                        {"5F", "_"},
                        {"60", "||"},
                        {"61", "a"},
                        {"62", "b"},
                        {"63", "c"},
                        {"64", "d"},
                        {"65", "e"},
                        {"66", "f"},
                        {"67", "g"},
                        {"68", "h"},
                        {"69", "i"},
                        {"6A", "j"},
                        {"6B", "k"},
                        {"6C", "l"},
                        {"6D", "m"},
                        {"6E", "n"},
                        {"6F", "o"},
                        {"70", "p"},
                        {"71", "q"},
                        {"72", "r"},
                        {"73", "s"},
                        {"74", "t"},
                        {"75", "u"},
                        {"76", "v"},
                        {"77", "w"},
                        {"78", "x"},
                        {"79", "y"},
                        {"7A", "z"},
                        {"7B", "{"},
                        {"7C", "|"},
                        {"7D", "}"},
                        {"7E", "~"},
                        {"7F", "■"},
                        {"80", "Œ"},
                        {"81", "œ"},
                        {"82", "IJ"},
                        {"83", "ij"},
                        {"84", "π"},
                        {"85", "±"},
                        {"86", " "},
                        {"87", " "},
                        {"88", " "},
                        {"89", " "},
                        {"8A", " "},
                        {"8B", " "},
                        {"8C", "←"},
                        {"8D", "↑"},
                        {"8E", "→"},
                        {"8F", "↓"},
                        {"90", "+"},
                        {"91", "♪"},
                        {"92", "\uD83D\uDCC1"},
                        {"93", " "},
                        {"94", " "},
                        {"95", " "},
                        {"96", " "},
                        {"97", " "},
                        {"98", " "},
                        {"99", " "},
                        {"9A", " "},
                        {"9B", " "},
                        {"9C", " "},
                        {"9D", " "},
                        {"9E", " "},
                        {"9F", " "},
                        {"A0", " "},
                        {"A1", "¡"},
                        {"A2", "¢"},
                        {"A3", "£"},
                        {"A4", "¤"},
                        {"A5", "¥"},
                        {"A6", "¦"},
                        {"A7", "§"},
                        {"A8", "̈"},
                        {"A9", "©"},
                        {"AA", "ª"},
                        {"AB", "«"},
                        {"AC", "¬"},
                        {"AD", "-"},
                        {"AE", "®"},
                        {"AF", "̄"},
                        {"B0", "°"},
                        {"B1", "±"},
                        {"B2", "²"},
                        {"B3", "³"},
                        {"B4", "́"},
                        {"B5", "μ"},
                        {"B6", "¶"},
                        {"B7", "·"},
                        {"B8", "̧"},
                        {"B9", "¹"},
                        {"BA", "º"},
                        {"BB", "»"},
                        {"BC", "¼"},
                        {"BD", "½"},
                        {"BE", "¾"},
                        {"BF", "¿"},
                        {"C0", "À"},
                        {"C1", "Á"},
                        {"C2", "Â"},
                        {"C3", "Ã"},
                        {"C4", "Ä"},
                        {"C5", "Å"},
                        {"C6", "Æ"},
                        {"C7", "Ç"},
                        {"C8", "È"},
                        {"C9", "É"},
                        {"CA", "Ê"},
                        {"CB", "Ë"},
                        {"CC", "Ì"},
                        {"CD", "Í"},
                        {"CE", "Î"},
                        {"CF", "ï"},
                        {"D0", "Ð"},
                        {"D1", "Ñ"},
                        {"D2", "Ò"},
                        {"D3", "Ó"},
                        {"D4", "Ô"},
                        {"D5", "Õ"},
                        {"D6", "Ö"},
                        {"D7", "×"},
                        {"D8", "Ø"},
                        {"D9", "Ù"},
                        {"DA", "Ú"},
                        {"DB", "Û"},
                        {"DC", "Ü"},
                        {"DD", "Ý"},
                        {"DE", "Þ"},
                        {"DF", "ß"},
                        {"E0", "à"},
                        {"E1", "á"},
                        {"E2", "â"},
                        {"E3", "ã"},
                        {"E4", "ä"},
                        {"E5", "å"},
                        {"E6", "æ"},
                        {"E7", "ç"},
                        {"E8", "è"},
                        {"E9", "é"},
                        {"EA", "ê"},
                        {"EB", "ë"},
                        {"EC", "ì"},
                        {"ED", "í"},
                        {"EE", "î"},
                        {"EF", "ï"},
                        {"F0", "ð"},
                        {"F1", "ñ"},
                        {"F2", "ò"},
                        {"F3", "ó"},
                        {"F4", "ô"},
                        {"F5", "õ"},
                        {"F6", "ö"},
                        {"F7", "÷"},
                        {"F8", "ø"},
                        {"F9", "ù"},
                        {"FA", "ú"},
                        {"FB", "û"},
                        {"FC", "ü"},
                        {"FD", "ý"},
                        {"FE", "þ"},
                        {"FF", "ÿ"}
                };
                int i = 0;
                while (!data.equals(translation[i][0])) {
                    if (translation[i][0].equals("FF")) {// Reached last character
                        Log.i(LOG_TAG, "getDisplayCharacter: Could not find character with code: " +
                                data);
                        return " ";     // Unknown character
                    }
                    i++;
                }
                return translation[i][1];
            default:
                Log.w(LOG_TAG, "getDisplayCharacter: unhandled Receiver (brand)" +
                        data.substring(3, 5));
                return  "";
        }
    }

    private boolean commandCouldBeSubCommand(String subCommand, String command) {
        int i = 0, j = 0;
        boolean diffLength = false;
        for (; i < subCommand.length() && j < command.length(); i++) {
            if (subCommand.charAt(i) == command.charAt(j) || command.charAt(j) == '·') {
                j++;
            } else if (command.charAt(j) == '…') {
                diffLength = true;
            } else {
                return false;
            }
        }
        return (subCommand.length() == command.length()) ||
                (diffLength && subCommand.length() > command.length());
    }
    public static String[] getAllPossibleCommands(Resources resources, ReceiverType receiverType,
                                                  int menu) {
        ArrayList<String> commands = new ArrayList<>();
        getAllPossibleCommands(resources, receiverType, menu, null, commands);
        return commands.toArray(new String[commands.size()]);
    }
    private static void getAllPossibleCommands(Resources resources, ReceiverType receiverType,
                                               int menu, @Nullable String bossCommand,
                                               ArrayList<String> commands) {
        String[] commandValues = getCommandValueArrayFromResource(resources, receiverType, menu);
        int[] commandHasSubmenu = getCommandHasSubmenuArrayFromResource(resources, receiverType,
                menu);
        if (commandValues.length == 0 || commandHasSubmenu.length == 0) {
            return;
        }
        if (commandValues.length != commandHasSubmenu.length) {
            Log.e(LOG_TAG, "getAllPossibleCommands: Corrupt resource for receiver type (brand) " +
                    receiverType + "; commandValues.length != commandHasSubmenu.length");
            return;
        }
        for (int i = 0; i < commandValues.length; i++) {
            int submenu = commandHasSubmenu[i];
            String commandValue;
            if (bossCommand == null) {
                commandValue = commandValues[i];
            } else {
                commandValue = Util.createCommandChain(bossCommand, commandValues[i]);
            }

            if (submenu <= 0) {
                commands.add(commandValue);
            } else {
                getAllPossibleCommands(resources, receiverType, submenu, commandValue, commands);
            }
        }
    }

    public String getCommandNameFromResource(Resources resources,
                                             @Nullable TcpConnection connection,
                                             ReceiverType receiverType, String command,
                                             @Nullable ArrayList<Integer> commandSearchPath) {
        return getCommandNameFromResource(resources, connection, receiverType, command, 0,
                commandSearchPath);
    }
    public String getCommandNameFromResource(Resources resources,
                                             @Nullable TcpConnection connection,
                                             ReceiverType receiverType, String command, int menu,
                                             @Nullable ArrayList<Integer> commandSearchPath) {
        if (commandSearchPath != null) {
            commandSearchPath.clear();
        }
        return getCommandNameFromResource(resources, connection, receiverType, command, null, menu,
                commandSearchPath, null);
    }
    /**
     * @param commandSearchPath
     * Will save the submenu path to the command
     * @return
     * Command name, or raw command if command name not found
     */
    private String getCommandNameFromResource(Resources resources,
                                              @Nullable TcpConnection connection,
                                              ReceiverType receiverType, @NonNull String command,
                                              @Nullable String bossCommand, int menu,
                                              @Nullable ArrayList<Integer> commandSearchPath,
                                              @Nullable Util.StringReference baseCommandValue) {
        String[] commandNames;
        if (connection != null) {
            commandNames = getCommandNameArray(resources, connection, menu, true);
        } else {
            commandNames = getCommandNameArrayFromResource(resources, receiverType, menu);
        }
        String[] commandValues = getCommandValueArrayFromResource(resources, receiverType, menu);
        int[] commandHasSubMenu =
                getCommandHasSubmenuArrayFromResource(resources, receiverType, menu);
        if (commandNames.length == 0 || commandValues.length == 0) {
            // Error message already in getCommandNameArrayFromResource() or
            // getCommandValueArrayFromResource()
            return command;
        }
        if (commandNames.length != commandValues.length) {
            Log.e(LOG_TAG, "getCommandNameFromResource: Corrupt resource for receiver type " +
                    "(brand) " + receiverType + "; commandNames.length != commandValues.length " +
                    "menu " + menu + ")");
            return command;
        } else if (commandNames.length != commandHasSubMenu.length) {
            Log.e(LOG_TAG, "getCommandNameFromResource: Corrupt resource for receiver type " +
                    "(brand) " + receiverType +
                    "; commandNames.length != commandHasSubMenu.length (menu " + menu + ")");
            return command;
        }
        for (int i = 0; i < commandValues.length; i++) {
            if (commandSearchPath != null) {
                commandSearchPath.add(i);
            }
            int submenu = commandHasSubMenu[i];
            String commandValue = commandValues[i];
            if (bossCommand != null) {
                commandValue = Util.createCommandChain(bossCommand, commandValue);
            }
            if (commandValue.equals(command)) {
                if (baseCommandValue != null) {
                    baseCommandValue.value = commandValue;
                }
                return commandNames[i];
            } else if (submenu != 0 && commandCouldBeSubCommand(command, commandValue)) {
                if (submenu == -1) {
                    if (baseCommandValue != null) {
                        baseCommandValue.value = commandValue;
                    }
                    if (commandSearchPath != null) {
                        commandSearchPath.add(submenu);
                    }
                    String rawValue = commandValue;
                    rawValue = rawValue.replaceAll("…", "");
                    rawValue = rawValue.replaceAll("·", "");
                    int index = command.indexOf(rawValue);
                    if (index >= 0) {
                        rawValue = command.substring(0, index) +
                                command.substring(index + rawValue.length());
                        return commandNames[i] + COMMAND_CHAIN_READABLE_SEPARATOR + rawValue;
                    } else {
                        Log.e(LOG_TAG, "Could not read rawValue from command " + command +
                                " and rawValue " + rawValue);
                        return commandNames[i];
                    }
                } else {
                    if (baseCommandValue != null) {
                        baseCommandValue.value = commandValue;
                    }
                    String sub = getCommandNameFromResource(resources, connection, receiverType,
                            command, commandValue, submenu, commandSearchPath, null);
                    return sub.equals(command) ? sub :
                            commandNames[i] + COMMAND_CHAIN_READABLE_SEPARATOR + sub;
                }
            }
            if (commandSearchPath != null) {
                commandSearchPath.remove(commandSearchPath.size() - 1);
            }
        }
        if (DEBUG) Log.d(LOG_TAG, "getCommandNameFromResource: Could not find command \"" +
                command + "\" for receiver (brand) " + receiverType);
        return command;
    }
    private TcpInformation classifyResponse(Resources resources, TcpConnection connection,
                                            String responseValue) {
        ArrayList<Integer> path = new ArrayList<>();
        Util.StringReference classifier = new Util.StringReference();
        String command = getCommandNameFromResource(resources, connection, connection.getType(),
                responseValue, null, MENU_TCP_RESPONSES, path, classifier);
        if (classifier.value == null) {
            return null;
        }
        String searchingFor = COMMAND_CHAIN_READABLE_SEPARATOR;
        int index = command.indexOf(searchingFor);
        while (index >= 0) {
            // Remove unneeded stuff (already given by classifier)
            command = command.substring(index + searchingFor.length());
            index = command.indexOf(searchingFor);
        }
        if (path.get(path.size() - 1) == -1) {
            String enhanced = enhancedResponse(applicationContext, connection, classifier.value,
                    command);
            if (enhanced != null) {
                return new TcpInformation(classifier.value, enhanced, responseValue);
            }
        }
        return new TcpInformation(classifier.value, command, responseValue);
    }

    private void specialTreatment(TcpConnection connection, String responseValue) {
        Resources resources = applicationContext.getResources();

        // Maybe clear displays
        if (connection.shouldClearDisplays) {
            String[] stopDisplayClearResponses = getCommandValueArrayFromResource(resources,
                    connection.getType(), MENU_STOP_CLEAR_DISPLAY_RESPONSES);
            if (Arrays.asList(stopDisplayClearResponses).contains(responseValue)) {
                // Stop clearing displays
                connection.shouldClearDisplays =  false;
                String[] clearableDisplays = getCommandValueArrayFromResource(resources,
                        connection.getType(), MENU_CLEARABLE_DISPLAYS);
                for (String display : clearableDisplays) {
                    connection.updateListeners(connection.getBufferedInformation(display));
                }
            }
        } else {
            String[] requestDisplayClearResponses = getCommandValueArrayFromResource(resources,
                    connection.getType(), MENU_REQUEST_CLEAR_DISPLAY_RESPONSES);
            if (Arrays.asList(requestDisplayClearResponses).contains(responseValue)) {
                // Start clearing displays
                connection.shouldClearDisplays =  true;
                String[] clearableDisplays = getCommandValueArrayFromResource(resources,
                        connection.getType(), MENU_CLEARABLE_DISPLAYS);
                for (String display : clearableDisplays) {
                    connection.updateListeners(new TcpInformation(display, "", null));
                }
            }
        }

        // Maybe init
        String[] requestInitResponses = getCommandValueArrayFromResource(resources,
                connection.getType(), MENU_REQUEST_INIT_RESPONSES);
        if (Arrays.asList(requestInitResponses).contains(responseValue)) {
            connection.initRequestsProgress = 0;
        }
    }
    private static int possibleCommandArrayCustomizations(Resources resources,
                                                          ReceiverType receiverType, int menu){
        int[] menus, values;
        switch (receiverType) {
            case PIONEER:
                switch (menu) {
                    case SUBMENU_LISTENING_MODE_SET:
                        menu = 4;
                        break;
                    case SUBMENU_LISTENING_MODE_GET:
                        menu = 5;
                        break;
                    case SUBMENU_INPUT:
                        menu = 6;
                        break;
                }
                menus = resources.getIntArray(R.array.pioneer_tcp_customizable_submenu_numbers);
                values = resources.getIntArray(R.array.pioneer_tcp_customizable_submenu_values);
                break;
            default:
                return 0;
        }

        if (menus.length != values.length) {
            Log.w(LOG_TAG, "possibleCommandArrayCustomizations: menus.length != values.length");
            return 0;
        }
        for (int i = 0; i < menus.length; i++) {
            if (menus[i] == menu) {
                return values[i];
            }
        }
        return 0;
    }

    /**
     * @param includeHidden
     * Whether to include hidden commands.
     * True if you only want the customized names.
     *
     * If you don't want customized names, use getCommandNameArrayFromResource
     */
    public static String[] getCommandNameArray(Resources resources, TcpConnection connection,
                                               int menu, boolean includeHidden) {
        if (possibleCommandArrayCustomizations(resources, connection.getType(), menu) != 0) {
            String[] customizedArray = connection.getCustomizedMenuNames(menu, includeHidden);
            if (customizedArray != null) {
                return customizedArray;
            }
        }
        return getCommandNameArrayFromResource(resources, connection.getType(), menu);
    }

    /**
     * Never includes hidden commands
     * If you want to include hidden commands, use getCommandValueArrayFromResource
     */
    public static String[] getCommandValueArray(Resources resources, TcpConnection connection,
                                                int menu) {
        if (possibleCommandArrayCustomizations(resources, connection.getType(), menu) != 0) {
            String[] customizedArray = connection.getCustomizedMenuValues(menu);
            if (customizedArray != null) {
                return customizedArray;
            }
        }
        return getCommandValueArrayFromResource(resources, connection.getType(), menu);
    }

    /**
     * Never includes hidden commands
     * If you want to include hidden commands, use getCommandHasSubmenuArrayFromResource
     */
    public static int[] getCommandHasSubmenuArray(Resources resources, TcpConnection connection,
                                                  int menu) {
        if (possibleCommandArrayCustomizations(resources, connection.getType(), menu) != 0) {
            TcpConnection.CustomizedMenu customizedMenu = connection.getCustomizedMenu(menu);
            if (customizedMenu != null) {
                String[] originalValues = getCommandValueArrayFromResource(resources,
                        connection.getType(), menu);
                String[] connectionValues = connection.getCustomizedMenuValues(menu);
                int[] original = getCommandHasSubmenuArrayFromResource(resources,
                        connection.getType(), menu);
                if (connectionValues == null) {
                    return original;
                }
                int[] connectionHasSubmenu = new int[connectionValues.length];
                int pos = 0;
                for (int i = 0; i < connectionValues.length; i++) {
                    if (originalValues[i].equals(connectionValues[pos])) {
                        connectionHasSubmenu[pos++] = original[i];
                    }
                }
                return connectionHasSubmenu;
            }
        }
        return getCommandHasSubmenuArrayFromResource(resources, connection.getType(), menu);
    }
    public static String[] getCommandNameArrayFromResource(Resources resources,
                                                           ReceiverType receiverType, int menu) {
        switch (receiverType) {
            case PIONEER:
                switch (menu) {
                    case 0:
                        return resources.getStringArray(R.array.pioneer_tcp_commands);
                    case 1:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_1);
                    case 2:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_2);
                    case 3:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_3);
                    case 4:
                    case SUBMENU_LISTENING_MODE_SET:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_4);
                    case 5:
                    case SUBMENU_LISTENING_MODE_GET:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_5);
                    case SUBMENU_INPUT:
                    case 6:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_6);
                    case 7:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_7);
                    case 8:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_8);
                    case 9:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_9);
                    case 10:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_10);
                    case 11:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_11);
                    case 12:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_12);
                    case 13:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_13);
                    case 14:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_14);
                    case 15:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_15);
                    case 16:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_16);
                    case 17:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_17);
                    case 18:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_18);
                    //case 19:
                    //    return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_19);
                    case 20:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_20);
                    case 21:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_21);
                    case 22:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_22);
                    case 23:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_submenu_23);
                    case MENU_TCP_RESPONSES:
                        return resources.getStringArray(R.array.pioneer_tcp_responses);
                }
            default:
                Log.i(LOG_TAG, "getCommandNameArrayFromResource: could not get command name " +
                        "array from resource for receiver (brand) " + receiverType + " and menu " +
                        menu);
                return new String[0];
        }
    }
    public static String[] getCommandValueArrayFromResource(Resources resources,
                                                            ReceiverType receiverType, int menu) {
        switch (receiverType) {
            case PIONEER:
                switch (menu) {
                    case 0:
                        return resources.getStringArray(R.array.pioneer_tcp_commands_values);
                    case 1:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_1_values);
                    case 2:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_2_values);
                    case 3:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_3_values);
                    case 4:
                    case SUBMENU_LISTENING_MODE_SET:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_4_values);
                    case 5:
                    case SUBMENU_LISTENING_MODE_GET:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_5_values);
                    case 6:
                    case SUBMENU_INPUT:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_6_values);
                    case 7:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_7_values);
                    case 8:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_8_values);
                    case 9:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_9_values);
                    case 10:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_10_values);
                    case 11:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_11_values);
                    case 12:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_12_values);
                    case 13:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_13_values);
                    case 14:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_14_values);
                    case 15:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_15_values);
                    case 16:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_16_values);
                    case 17:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_17_values);
                    case 18:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_18_values);
                    //case 19:
                    //    return resources.getStringArray(
                    //            R.array.pioneer_tcp_commands_submenu_19_values);
                    case 20:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_20_values);
                    case 21:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_21_values);
                    case 22:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_22_values);
                    case 23:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_commands_submenu_23_values);
                    case MENU_TCP_RESPONSES:
                        return resources.getStringArray(R.array.pioneer_tcp_responses_values);
                    case MENU_START_REQUESTS:
                        return resources.getStringArray(R.array.pioneer_tcp_start_requests);
                    case MENU_INIT_REQUESTS:
                        return resources.getStringArray(R.array.pioneer_tcp_init_requests);
                    case MENU_REQUEST_INIT_RESPONSES:
                        return resources.getStringArray(R.array.pioneer_tcp_should_init_responses);
                    case MENU_REQUEST_CLEAR_DISPLAY_RESPONSES:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_should_clear_display_responses);
                    case MENU_STOP_CLEAR_DISPLAY_RESPONSES:
                        return resources.getStringArray(
                                R.array.pioneer_tcp_should_stop_clear_display_responses);
                    case MENU_CLEARABLE_DISPLAYS:
                        return resources.getStringArray(R.array.pioneer_tcp_clearable_displays);
                }
            case UNSPECIFIED:
                return new String[0];
            default:
                Log.i(LOG_TAG, "getCommandValueArrayFromResource: could not get command value " +
                        "array from resource for receiver (brand) " + receiverType + " and menu " +
                        menu);
                return new String[0];
        }
    }
    public static int[] getCommandHasSubmenuArrayFromResource(Resources resources,
                                                              ReceiverType receiverType, int menu) {
        switch (receiverType) {
            case PIONEER:
                switch (menu) {
                    case 0:
                        return resources.getIntArray(R.array.pioneer_tcp_commands_has_submenu);
                    case 1:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_1_has_submenu);
                    case 2:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_2_has_submenu);
                    case 3:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_3_has_submenu);
                    case 4:
                    case SUBMENU_LISTENING_MODE_SET:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_4_has_submenu);
                    case 5:
                    case SUBMENU_LISTENING_MODE_GET:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_5_has_submenu);
                    case 6:
                    case SUBMENU_INPUT:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_6_has_submenu);
                    case 7:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_7_has_submenu);
                    case 8:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_8_has_submenu);
                    case 9:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_9_has_submenu);
                    case 10:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_10_has_submenu);
                    case 11:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_11_has_submenu);
                    case 12:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_12_has_submenu);
                    case 13:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_13_has_submenu);
                    case 14:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_14_has_submenu);
                    case 15:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_15_has_submenu);
                    case 16:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_16_has_submenu);
                    case 17:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_17_has_submenu);
                    case 18:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_18_has_submenu);
                    //case 19:
                    //    return resources.getIntArray(
                    //            R.array.pioneer_tcp_commands_submenu_19_has_submenu);
                    case 20:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_20_has_submenu);
                    case 21:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_21_has_submenu);
                    case 22:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_22_has_submenu);
                    case 23:
                        return resources.getIntArray(
                                R.array.pioneer_tcp_commands_submenu_23_has_submenu);
                    case MENU_TCP_RESPONSES:
                        return resources.getIntArray(R.array.pioneer_tcp_responses_has_submenu);
                    case MENU_START_REQUESTS:
                        return resources.getIntArray(R.array.pioneer_tcp_start_request_submenus);
                    case MENU_INIT_REQUESTS:
                        return resources.getIntArray(R.array.pioneer_tcp_init_request_submenus);
                }
            case UNSPECIFIED:
                return new int[0];
            default:
                Log.i(LOG_TAG, "getCommandHasSubmenuArrayFromResource: could not get command " +
                        "has_submenu array from resource for receiver (brand) " + receiverType +
                        " and menu " + menu);
                return new int[0];
        }
    }
    public static String[] getDropdownMenuNames(Resources resources, ReceiverType receiverType) {
        switch (receiverType) {
            case PIONEER:
                return resources.getStringArray(R.array.pioneer_tcp_dropdown_menu_names);
            default:
                Log.i(LOG_TAG, "getDropdownMenuNames: could not get array from resource for " +
                        "receiver (brand) " + receiverType);
                return new String[0];
        }
    }
    public static String[] getDropdownMenuCommandValues(Resources resources,
                                                         ReceiverType receiverType) {
        switch (receiverType) {
            case PIONEER:
                return resources.getStringArray(R.array.pioneer_tcp_dropdown_menu_command_values);
            default:
                Log.i(LOG_TAG, "getDropdownMenuCommandValues: could not get array from resource " +
                        "for receiver (brand) " + receiverType);
                return new String[0];
        }
    }
    public static String[] getDropdownMenuResponseValues(Resources resources,
                                                          ReceiverType receiverType) {
        switch (receiverType) {
            case PIONEER:
                return resources.getStringArray(R.array.pioneer_tcp_dropdown_menu_response_values);
            default:
                Log.i(LOG_TAG, "getDropdownMenuResponseValues: could not get array from resource " +
                        "for receiver (brand) " + receiverType);
                return new String[0];
        }
    }
    public static int[] getDropdownMenuSubmenus(Resources resources, ReceiverType receiverType) {
        switch (receiverType) {
            case PIONEER:
                return resources.getIntArray(R.array.pioneer_tcp_dropdown_menu_submenus);
            default:
                Log.i(LOG_TAG, "getDropdownMenuSubmenus: could not get array from resource for " +
                        "receiver (brand) " + receiverType);
                return new int[0];
        }
    }
    public static String[] getCustomizableSubmenuNames(Resources resources,
                                                        ReceiverType receiverType) {
        switch (receiverType) {
            case PIONEER:
                return resources.getStringArray(R.array.pioneer_tcp_customizable_submenu_names);
            default:
                Log.i(LOG_TAG, "getCustomizableSubmenuNames: could not get array from resource " +
                        "for receiver (brand) " + receiverType);
                return new String[0];
        }
    }
    public static int[] getCustomizableSubmenus(Resources resources, ReceiverType receiverType) {
        switch (receiverType){
            case PIONEER:
                return resources.getIntArray(R.array.pioneer_tcp_customizable_submenu_numbers);
            default:
                Log.i(LOG_TAG, "getCustomizableSubmenus: could not get array from resource for " +
                        "receiver (brand) " + receiverType);
                return new int[0];
        }
    }
    public static int[] getCustomizableSubmenuValues(Resources resources,
                                                      ReceiverType receiverType) {
        switch (receiverType) {
            case PIONEER:
                return resources.getIntArray(R.array.pioneer_tcp_customizable_submenu_values);
            default:
                Log.i(LOG_TAG, "getCustomizableSubmenuValues: could not get array from resource " +
                        "for receiver (brand) " + receiverType);
                return new int[0];
        }
    }

    public String[] getConnectionSuggestions() {
        ArrayList<String> result = new ArrayList<>();
        for (int i = 0; i < tcpConnections.size(); i++) {
            if (!tcpConnections.get(i).isHidden() && !tcpConnections.get(i).shouldRemove) {
                result.add(tcpConnections.get(i).getIp());
            }
        }

        return result.toArray(new String[result.size()]);
    }
    public static String getReceiverTypeDisplayString(Resources resources,
                                                      ReceiverType receiverType) {
        String[] typeArray = resources.getStringArray(R.array.receiver_type_array),
                typeValueArray = resources.getStringArray(R.array.receiver_type_array_values);
        int index = Arrays.asList(typeValueArray).indexOf(receiverType.toString());
        if (index >= 0 && index < typeArray.length) {
            return typeArray[index];
        } else {
            return "???";
        }
    }
}
