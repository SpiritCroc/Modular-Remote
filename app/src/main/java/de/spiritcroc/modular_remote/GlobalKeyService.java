/*
 * Copyright (C) 2017 SpiritCroc
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

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.media.session.PlaybackState;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

import androidx.core.app.NotificationCompat;
import androidx.media.VolumeProviderCompat;

import java.util.ArrayList;
import java.util.HashMap;

@TargetApi(21)
public class GlobalKeyService extends Service {

    public static final int ACTION_REGISTER_CLIENT = 1;

    public static final int ACTION_UNREGISTER_CLIENT = 2;

    public static final int ACTION_ADD_ACTION = 11;

    public static final int ACTION_REMOVE_ALL = 12;

    public static final int ACTION_REQUEST_VOL_CTRL_ACTIVE = 20;


    public static final int EXTRA_ACTION_ID_VOLUME_UP = 1;

    public static final int EXTRA_ACTION_ID_VOLUME_DOWN = 2;


    public static final int RESPONSE_VOL_CTRL_ACTIVE = 20;


    private static final String NAME = "de.spiritcroc.modular_remote.GlobalKeyService";

    private static final String MEDIA_SESSION_TAG = NAME;

    private static final String LOG_TAG = GlobalKeyService.class.getSimpleName();

    private static final boolean DEBUG = false;


    private NotificationManager notificationManager;

    private Messenger messenger = new Messenger(new MessageHandler());

    private ArrayList<Messenger> clients = new ArrayList<>();

    private HashMap<Integer, GlobalActionSetting> actions = new HashMap<>();

    private static MediaSessionCompat mediaSession;

    private boolean volumeCtrlActive = false;

    // Arbitrary but unique ID
    private static final int NOTIFICATION_ID = R.string.notif_service_running_summary;

    private static final String NOTIFICATION_CHANNEL_ID = NAME;

    @Override
    public void onCreate() {
        notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
    }

    @Override
    public void onDestroy() {
        // Cancel the persistent notification.
        cancelNotification();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return messenger.getBinder();
    }

    private class MessageHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case ACTION_REGISTER_CLIENT:
                    clients.add(msg.replyTo);
                    break;
                case ACTION_UNREGISTER_CLIENT:
                    clients.remove(msg.replyTo);
                    break;
                case ACTION_ADD_ACTION:
                    addAction(msg.arg1, msg.obj);
                    break;
                case ACTION_REMOVE_ALL:
                    removeAllActions();
                    break;
                case ACTION_REQUEST_VOL_CTRL_ACTIVE:
                    messageVolCtrlActive();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    private void addAction(int id, Object recreationKey) {
        if (recreationKey instanceof String) {
            addAction(id, (String) recreationKey);
        } else {
            Log.e(LOG_TAG, "addAction: invalid recreationKey " + recreationKey);
        }
    }

    private void addAction(int id, String recreationKey) {
        if (actions.containsKey(id)) {
            removeAction(id);
        }
        GlobalActionSetting action = GlobalActionSetting.recoverFromRecreationKey(recreationKey, TcpConnectionManager.getInstance(this));
        actions.put(id, action);
        updateKeyListeners();
    }

    private void removeAction(int id) {
        GlobalActionSetting action = actions.remove(id);
        if (action != null) {
            action.closeConnection();
        }
        updateKeyListeners();
    }

    private void removeAllActions() {
        for (GlobalActionSetting action: actions.values()) {
            action.closeConnection();
        }
        actions.clear();
        updateKeyListeners();
    }


    private void updateKeyListeners() {
        boolean volumeCtrlActive =
                (actions.containsKey(EXTRA_ACTION_ID_VOLUME_UP) && !actions.get(EXTRA_ACTION_ID_VOLUME_UP).isVoid()) ||
                (actions.containsKey(EXTRA_ACTION_ID_VOLUME_DOWN) && !actions.get(EXTRA_ACTION_ID_VOLUME_DOWN).isVoid());
        setMediaSessionEnabled(volumeCtrlActive);
    }

    private void setMediaSessionEnabled(boolean enabled) {
        if (DEBUG) Log.v(LOG_TAG, "setMediaSessionEnabled " + enabled);
        if (enabled == volumeCtrlActive && enabled == getMediaServerState()) {
            if (DEBUG) Log.v(LOG_TAG, "setMediaSessionEnabled: no need to update");
            return;
        }
        volumeCtrlActive = enabled;
        if (volumeCtrlActive) {
            // Start media session
            if (mediaSession != null) {
                mediaSession.release();
            }
            mediaSession = new MediaSessionCompat(this, MEDIA_SESSION_TAG);
            mediaSession.setCallback(new MediaSessionCompat.Callback() {
                // todo?
            });
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            CustomVolumeProvider volumeProvider = new CustomVolumeProvider();
            mediaSession.setPlaybackToRemote(volumeProvider);

            mediaSession.setActive(true);

            // Set volume active
            PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING,
                            PlaybackState.PLAYBACK_POSITION_UNKNOWN, SystemClock.elapsedRealtime())
                    .build();

            mediaSession.setPlaybackState(state);

            showNotification();
        } else {
            // Stop media session
            if (mediaSession != null) {
                mediaSession.setActive(false);
                mediaSession.release();
            }

            cancelNotification();
        }

        messageVolCtrlActive();
    }

    private boolean getMediaServerState() {
        return mediaSession != null && mediaSession.isActive();
    }

    private class CustomVolumeProvider extends VolumeProviderCompat {// TODO inactivity detection?
        //private int failedCount = 0;
        //private boolean inactive = false;
        public CustomVolumeProvider() {
            super(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE, 100, 50);
            //inactive = checkInactivity();
        }
        @Override
        public void onAdjustVolume(int direction) {
            if (DEBUG) Log.d(LOG_TAG, "onAdjustVolume: " + direction);
            if (direction > 0) {
                sendCommand(getVolumeUpAction());
            } else if (direction < 0) {
                sendCommand(getVolumeDownAction());
            } else {
                super.onAdjustVolume(direction);
            }
        }

        @Override
        public void onSetVolumeTo(int volume) {
            if (DEBUG) Log.d(LOG_TAG, "onSetVolumeTo: " + volume);
            if (volume > getCurrentVolume()) {
                sendCommand(getVolumeUpAction());
            } else if (volume < getCurrentVolume()) {
                sendCommand(getVolumeDownAction());
            } else {
                super.onSetVolumeTo(volume);
            }
        }
        private void sendCommand(GlobalActionSetting setting) {
            setting.sendCommand(GlobalKeyService.this);
            /*
            boolean inactive = false;
            if (!setting.sendCommand(GlobalKeyService.this)) {
                failedCount++;
            }
            if (failedCount >= INACTIVITY_COUNT) {
                failedCount = 0;
                inactive = checkInactivity();
            }
            if (inactive != this.inactive) {
                this.inactive = inactive;
                if (!inactive) {
                    setVolumeActive(true);
                }
            }
            */
        }
        /*
        private boolean checkInactivity() {
            if (!volumeUpSetting.isConnected() && !volumeDownSetting.isConnected()) {
                setVolumeActive(false);
                return true;
            } else {
                return false;
            }
        }
        */
        private GlobalActionSetting getVolumeUpAction() {
            return actions.get(EXTRA_ACTION_ID_VOLUME_UP);
        }
        private GlobalActionSetting getVolumeDownAction() {
            return actions.get(EXTRA_ACTION_ID_VOLUME_DOWN);
        }
    }


    private void messageVolCtrlActive() {
        messageClients(Message.obtain(null, RESPONSE_VOL_CTRL_ACTIVE, getMediaServerState()));
    }


    private void messageClients(Message message) {
        for (int i = clients.size() - 1; i >= 0; i--) {
            try {
                clients.get(i).send(Message.obtain(message));
            } catch (RemoteException e) {
                // The client is dead.  Remove it from the list;
                // we are going through the list from back to front
                // so this is safe to do inside the loop.
                clients.remove(i);
            }
        }
    }


    private void showNotification() {
        PendingIntent p = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);

        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = getString(R.string.notification_channel_fg_name);
            String description = getString(R.string.notification_channel_fg_description);
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            channel.setImportance(NotificationManager.IMPORTANCE_MIN);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }


        Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setOngoing(true)
                .setSmallIcon(R.mipmap.ic_launcher)//todo icon
                .setContentTitle(getText(R.string.notif_service_running_title))
                .setContentText(getText(R.string.notif_service_running_summary))
                .setContentIntent(p)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .build();


        startForeground(R.string.notif_service_running_title, notification);//todo: http://stackoverflow.com/questions/3856767/android-keeping-a-background-service-alive-preventing-process-death ?
    }

    private void cancelNotification() {
        stopForeground(true);
    }
}
