package de.spiritcroc.modular_remote;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.media.session.PlaybackState;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.v4.media.VolumeProviderCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;

@TargetApi(21)
public abstract class GlobalActionHandler {
    private static final String LOG_TAG = GlobalActionHandler.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String MEDIA_SESSION_TAG = "de.spiritcroc.modular_remote.GlobalAction";

    private static final int INACTIVITY_COUNT = 5;

    private static Context appContext;
    private static MediaSessionCompat mediaSession;

    private static GlobalActionSetting volumeUpSetting;
    private static GlobalActionSetting volumeDownSetting;
    private static boolean volumeCtrlEnabled = false;

    public static void init(Context context) {
        appContext = context.getApplicationContext();
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(appContext);
        if (volumeUpSetting != null) {
            closeConnections();
        }
        volumeUpSetting = GlobalActionSetting.recoverFromRecreationKey(
                sp.getString(Preferences.GLOBAL_ACTION_VOLUME_UP, ""),
                TcpConnectionManager.getInstance(appContext));
        volumeDownSetting = GlobalActionSetting.recoverFromRecreationKey(
                sp.getString(Preferences.GLOBAL_ACTION_VOLUME_DOWN, ""),
                TcpConnectionManager.getInstance(appContext));
        volumeCtrlEnabled = !volumeUpSetting.isVoid() || !volumeUpSetting.isVoid();
    }

    public static boolean isEnabled() {
        return mediaSession != null && mediaSession.isActive();
    }

    public static void enable(Activity activity) {
        init(activity);

        if (isEnabled()) {
            return;
        }

        if (mediaSession != null) {
            mediaSession.release();
        }

        mediaSession = new MediaSessionCompat(appContext, MEDIA_SESSION_TAG);
        mediaSession.setCallback(new MediaSessionCompat.Callback() {
            /*@Override
            public boolean onMediaButtonEvent(@NonNull Intent mediaButtonIntent) {
                return handleMediaButton(mediaButtonIntent)
                        || super.onMediaButtonEvent(mediaButtonIntent);
            }*/
        });
        CustomVolumeProvider volumeProvider = null;
        if (volumeCtrlEnabled) {
            mediaSession.setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS
                    | MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS);
            volumeProvider = new CustomVolumeProvider();
            mediaSession.setPlaybackToRemote(volumeProvider);
        }

        mediaSession.setActive(true);

        if (volumeProvider != null && !volumeProvider.inactive) {
            setVolumeActive(true);
        }
    }

    private static void setVolumeActive(boolean active) {
        PlaybackStateCompat state = new PlaybackStateCompat.Builder()
                .setState(active ?
                        PlaybackStateCompat.STATE_PLAYING : PlaybackStateCompat.STATE_STOPPED,
                        PlaybackState.PLAYBACK_POSITION_UNKNOWN, SystemClock.elapsedRealtime())
                .build();

        mediaSession.setPlaybackState(state);
    }

    public static void disable() {
        if (!isEnabled()) {
            return;
        }
        mediaSession.setActive(false);
        mediaSession.release();
        closeConnections();
    }

    /*private static boolean handleMediaButton(Intent mediaButtonIntent) {
        int keyEvent = mediaButtonIntent.getIntExtra(Intent.EXTRA_KEY_EVENT, -1);
        switch (keyEvent) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (DEBUG) Log.d(LOG_TAG, "handleMediaButton: volume up");
                return volumeUpSetting.sendCommand(appContext);
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (DEBUG) Log.d(LOG_TAG, "handleMediaButton: volume down");
                return volumeDownSetting.sendCommand(appContext);
            default:
                if (DEBUG) Log.d(LOG_TAG, "handleMediaButton: unknown: " + keyEvent);
                return false;
        }
    }*/

    private static void closeConnections() {
        volumeUpSetting.closeConnection();
        volumeDownSetting.closeConnection();
    }

    public static void updateSetting(GlobalActionSetting setting) {
        if (appContext == null) {
            Log.e(LOG_TAG, "updateSettings: missing context");
            return;
        }
        String key;
        if (setting == volumeUpSetting) {
            key = Preferences.GLOBAL_ACTION_VOLUME_UP;
        } else if (setting == volumeDownSetting) {
            key = Preferences.GLOBAL_ACTION_VOLUME_DOWN;
        } else {
            Log.w(LOG_TAG, "updateSetting: requested by unknown action " + setting);
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(appContext).edit()
                .putString(key, setting.getRecreationKey()).apply();
    }

    private static class CustomVolumeProvider extends VolumeProviderCompat {
        private int failedCount = 0;
        private boolean inactive = false;
        public CustomVolumeProvider() {
            super(VolumeProviderCompat.VOLUME_CONTROL_ABSOLUTE, 100, 50);
            inactive = checkInactivity();
        }
        @Override
        public void onAdjustVolume(int direction) {
            if (DEBUG) Log.d(LOG_TAG, "onAdjustVolume: " + direction);
            if (direction > 0) {
                sendCommand(volumeUpSetting);
            } else if (direction < 0) {
                sendCommand(volumeDownSetting);
            } else {
                super.onAdjustVolume(direction);
            }
        }

        @Override
        public void onSetVolumeTo(int volume) {
            if (DEBUG) Log.d(LOG_TAG, "onSetVolumeTo: " + volume);
            if (volume > getCurrentVolume()) {
                sendCommand(volumeUpSetting);
            } else if (volume < getCurrentVolume()) {
                sendCommand(volumeDownSetting);
            } else {
                super.onSetVolumeTo(volume);
            }
        }
        private void sendCommand(GlobalActionSetting setting) {
            boolean inactive = false;
            if (!setting.sendCommand(appContext)) {
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
        }
        private boolean checkInactivity() {
            if (!volumeUpSetting.isConnected() && !volumeDownSetting.isConnected()) {
                setVolumeActive(false);
                return true;
            } else {
                return false;
            }
        }
    }
}
