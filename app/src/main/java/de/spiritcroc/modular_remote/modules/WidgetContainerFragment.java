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
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.Preferences;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.Util;

public class WidgetContainerFragment extends ModuleFragment {
    private static final String LOG_TAG = WidgetContainerFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static final String ARG_APP_WIDGET_ID = "app_widget_id";

    private LinearLayout widgetContainer;
    private View widget;
    private ImageView previewView;
    private int appWidgetId = -1;
    private boolean created = false;

    public static WidgetContainerFragment newInstance(int appWidgetId) {
        WidgetContainerFragment fragment = new WidgetContainerFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_APP_WIDGET_ID, appWidgetId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (created) {
            // Prevent overwriting attributes that are already set
            return;
        } else {
            created = true;
        }

        if (getArguments() != null) {
            appWidgetId = getArguments().getInt(ARG_APP_WIDGET_ID);
        } else {
            Log.e(LOG_TAG, "onCreate: getArguments()==null");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_widget_container, container, false);

        widgetContainer = (LinearLayout) view.findViewById(R.id.widget_container);
        previewView = (ImageView) view.findViewById(R.id.widget_preview);
        addWidget();
        setDragView(widgetContainer);
        updatePosition(view);
        resize(view);

        maybeStartDrag(view);

        return view;
    }

    @Override
    public void setMenuEnabled(boolean menuEnabled) {}// Has no menu
    @Override
    public void onRemove() {
        // removeWidget information if this is the last container with this appWidgetId
        Activity activity = getActivity();
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(activity);
        String widgetAmountKey = getWidgetAmountPrefKey();
        int amount = 0;
        if (sharedPreferences.contains(widgetAmountKey)) {
            amount = sharedPreferences.getInt(widgetAmountKey, 1) - 1;
            if (amount <= 1){
                sharedPreferences.edit().remove(widgetAmountKey).apply();
                if (DEBUG) Log.v(LOG_TAG, "Removed amount pref for " + appWidgetId);
            } else {
                sharedPreferences.edit().putInt(widgetAmountKey, amount).apply();
                if (DEBUG) Log.v(LOG_TAG, "New amount for " + appWidgetId + ": " + amount);
            }
        }
        if (amount <= 0) {
            // Removed the last one
            if (activity instanceof MainActivity) {
                if (DEBUG) Log.v(LOG_TAG, "Remove " + appWidgetId);
                ((MainActivity) activity).removeWidget(appWidgetId);
            } else {
                Log.w(LOG_TAG, "onRemove: !(activity instanceof MainActivity)");
            }
        }
    }

    private String getWidgetAmountPrefKey() {
        return Preferences.KEY_WIDGET_CONTAINER_AMOUNT_ + appWidgetId;
    }

    @Override
    public String getReadableName() {
        return Util.getACString(R.string.fragment_widget) + " " + widget.getContentDescription();
    }
    @Override
    public String getRecreationKey() {
        return fixRecreationKey(WIDGET_CONTAINER_FRAGMENT + SEP + pos.getRecreationKey() + SEP +
                appWidgetId + SEP);
    }
    public static WidgetContainerFragment recoverFromRecreationKey(String key) {
        try {
            String[] args = Util.split(key, SEP, 0);
            int appWidgetId = Integer.parseInt(args[2]);
            WidgetContainerFragment fragment = newInstance(appWidgetId);
            fragment.recoverPos(args[1]);
            return fragment;
        } catch (Exception e) {
            Log.e(LOG_TAG, "recoverFromRecreationKey: illegal key: " + key);
            Log.e(LOG_TAG, "Got exception: " + e);
            return null;
        }
    }
    @Override
    public ModuleFragment copy() {
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());

        // Update widget amount to make sure that widget information is only removed on last
        // WidgetContainerFragment removal with that appWidgetId
        String widgetAmountKey = getWidgetAmountPrefKey();
        int amount = sharedPreferences.getInt(widgetAmountKey, 1) + 1;
        sharedPreferences.edit().putInt(widgetAmountKey, amount).apply();
        if (DEBUG) Log.v(LOG_TAG, "New amount for " + appWidgetId + ": " + amount);

        ModuleFragment fragment = newInstance(appWidgetId);
        fragment.parent = parent;
        return fragment;
    }
    private void addWidget() {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            widget = ((MainActivity) activity).createWidget(appWidgetId);
            widgetContainer.addView(widget);
        }
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void onStartDragMode() {
        super.onStartDragMode();
        // Replace widget with an image of its content to prevent the widget from stealing focus
        if (previewView != null) {
            previewView.setVisibility(View.VISIBLE);
            Bitmap bitmap = Bitmap.createBitmap(
                    widget.getMeasuredWidth(), widget.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            widget.draw(canvas);
            previewView.setImageBitmap(bitmap);
            widget.setVisibility(View.GONE);
        }

    }
    public void onStopDragMode() {
        super.onStopDragMode();
        previewView.setVisibility(View.GONE);
        widget.setVisibility(View.VISIBLE);
    }

    @Override
    protected void prepareEditMenu(Menu menu) {
        super.prepareEditMenu(menu);
        menu.findItem(R.id.action_edit).setVisible(false);
    }

    @Override
    protected void editActionEdit() {
        // No settings
    }
}
