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

import android.os.Handler;
import android.util.Log;

import java.util.ArrayList;
import java.util.Calendar;

public class TimeSingleton {
    private static final String LOG_TAG = TimeSingleton.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static TimeSingleton instance;
    private int updateInterval;
    private ArrayList<TimeListener> timeListeners;
    private Handler timeHandler;
    private String time;

    private TimeSingleton(int updateInterval) {
        setUpdateInterval(updateInterval);
        timeHandler = new Handler();
        timeListeners = new ArrayList<>();
    }

    public static TimeSingleton getInstance(int updateInterval) {
        if (instance == null) {
            instance = new TimeSingleton(updateInterval);
        } else {
            instance.setUpdateInterval(updateInterval);
        }
        return instance;
    }
    public static TimeSingleton getInstance() {// Do not overwrite updateInterval
        if (instance == null) {
            instance = new TimeSingleton(500);// Default
        }
        return instance;
    }
    private void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }

    private void setTime(String time) {
        for (int i = 0; i < timeListeners.size(); i++) {
            timeListeners.get(i).setTime(time);
        }
    }

    public void registerListener(TimeListener listener) {
        if (!timeListeners.contains(listener)) {
            if (timeListeners.isEmpty()) {
                onResume();
            }
            timeListeners.add(listener);
            listener.setTime(time);
        }
    }
    public void unregisterListener(TimeListener listener) {
        listener.setTime("");
        timeListeners.remove(listener);
        if (timeListeners.isEmpty()) {
            onPause();
        }
    }

    private void onResume() {
        timeHandler.postDelayed(timeUpdateTask, updateInterval);
    }
    private void onPause() {
        timeHandler.removeCallbacks(timeUpdateTask);
    }

    private Runnable timeUpdateTask = new Runnable() {
        @Override
        public void run() {
            if (updateInterval > 0) {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);
                time = hour + ":";
                if (minute < 10) {
                    time += "0";
                }
                time += minute + ":";
                if (second < 10) {
                    time += "0";
                }
                time += second + "";
                if (DEBUG) Log.v(LOG_TAG, "Update time for " + timeListeners.size() + " listeners");
                setTime(time);
                timeHandler.postDelayed(this, updateInterval);
            } else {
                setTime("");
            }
        }
    };

    public interface TimeListener {
        void setTime(String time);
    }
}
