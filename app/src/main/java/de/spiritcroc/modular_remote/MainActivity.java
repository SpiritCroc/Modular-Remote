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

import android.app.KeyguardManager;
import android.app.NotificationManager;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.viewpager.widget.PagerTabStrip;
import androidx.viewpager.widget.ViewPager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.spiritcroc.modular_remote.dialogs.AboutDialog;
import de.spiritcroc.modular_remote.dialogs.AddFragmentDialog;
import de.spiritcroc.modular_remote.dialogs.GreetingDialog;
import de.spiritcroc.modular_remote.dialogs.OverlapWarningDialog;
import de.spiritcroc.modular_remote.dialogs.SelectConnectionDialog;
import de.spiritcroc.modular_remote.dialogs.SelectFragmentsDialog;
import de.spiritcroc.modular_remote.dialogs.SetupGridSizeDialog;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.ModuleFragment;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;
import de.spiritcroc.modular_remote.modules.WidgetContainerFragment;

public class MainActivity extends BaseActivity implements ViewPager.OnPageChangeListener {
    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG;

    // For launcher shortcut intents
    public static final String EXTRA_SELECT_PAGE_ID =
            "de.spiritcroc.modular_remote.extra.SELECT_PAGE_ID";
    private static final String EXTRA_RESTARTED_FROM_EDIT_MODE =
            "de.spiritcroc.modular_remote.extra.RESTARTED_FROM_EDIT_MODE";
    // Force orientation when using launcher shortcuts
    public static final String EXTRA_FORCE_ORIENTATION =
            "de.spiritcroc.modular_remote.extra.FORCE_ORIENTATION";
    public static final int FORCE_ORIENTATION_PORTRAIT = 1;
    public static final int FORCE_ORIENTATION_LANDSCAPE = 2;

    private CustomFragmentPagerAdapter fragmentPagerAdapter;
    private static CustomViewPager viewPager;
    private PagerTabStrip pagerTabStrip;
    private SharedPreferences sharedPreferences;
    private TcpConnectionManager tcpConnectionManager;
    private ArrayList<PageContainerFragment> pages;
    private static final String separator = Util.RK_CONTAINER_BRACKET + "0" +
            Util.RK_CONTAINER_BRACKET;
    private boolean neverDestroyPages, fullscreen, hideNavigationBar, hideActionBar;
    private View decorView;
    private Handler hideSystemUIHandler = new Handler();
    private AppWidgetManager appWidgetManager;
    private AppWidgetHost appWidgetHost;
    private Container addWidgetContainer;
    private DialogFragment addWidgetListener;
    private int previousRingerMode;
    private boolean changedRingerMode = false;
    private int volumeButtonSettingsShortcutCount = 0;
    private Handler resetVolumeButtonSettingsShortcutHandler = new Handler();
    private MenuItem editModeMenuItem, connectionMgrMenuItem;
    private boolean discardSavedInstance = false;
    private int forceOrientation = -1;

    private ActionMode actionMode;
    private boolean editModeContainerDrag = true;// If false: editModeContainerScroll
    private Toast editModeContainerDragMoveToast = null;
    private ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.menu_main_edit, menu);
            for (int i = 0; i < pages.size(); i++) {
                pages.get(i).onStartDragMode();
            }
            editModeContainerDrag = true;
            MenuItem editModeItem = menu.findItem(R.id.action_container_edit_mode);
            editModeItem.setTitle(R.string.action_container_edit_mode_scroll);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_fragment_settings:
                    new SelectFragmentsDialog().setPage(pages.get(viewPager.getCurrentItem()))
                            .show(getSupportFragmentManager(), "SelectFragmentDialog");
                    return true;
                case R.id.action_add_fragment:
                    new AddFragmentDialog().setPage(pages.get(viewPager.getCurrentItem()))
                            .show(getSupportFragmentManager(), "AddFragmentDialog");
                    return true;
                case R.id.action_container_edit_mode:
                    cancelEditContainerModeToast();
                    if (editModeContainerDrag) {
                        editModeContainerDrag = false;
                        item.setTitle(R.string.action_container_edit_mode_drag);
                        item.setIcon(R.drawable.ic_action_container_drag_24dp);
                        editModeContainerDragMoveToast = Toast.makeText(MainActivity.this,
                                R.string.toast_container_edit_scroll, Toast.LENGTH_SHORT);
                        editModeContainerDragMoveToast.show();
                    } else {
                        editModeContainerDrag = true;
                        item.setTitle(R.string.action_container_edit_mode_scroll);
                        item.setIcon(R.drawable.ic_action_container_scroll_24dp);
                        editModeContainerDragMoveToast = Toast.makeText(MainActivity.this,
                                R.string.toast_container_edit_drag, Toast.LENGTH_SHORT);
                        editModeContainerDragMoveToast.show();
                    }
                    for (int i = 0; i < pages.size(); i++) {
                        pages.get(i).setContainerDragEnabled(editModeContainerDrag);
                    }
                    return true;
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            actionMode = null;
            for (int i = 0; i < pages.size(); i++) {
                pages.get(i).onStopDragMode();
            }
            for (int i = 0; i < pages.size(); i++) {
                if (Util.hasContainerOverlappingFragments(pages.get(i))) {
                    new OverlapWarningDialog().setCallback(MainActivity.this)
                            .show(getSupportFragmentManager(), "OverlapWarningDialog");
                    return;
                }
            }
            cancelEditContainerModeToast();
            exitEditMode();
        }
    };

    public void enterEditMode() {
        actionMode = startActionMode(actionModeCallback);
    }
    public void exitEditMode() {
        saveSettings();
    }

    //constants for AppWidgetHost:
    private static final int APPWIDGET_HOST_ID = 1024;
    private static final int REQUEST_PICK_APPWIDGET = 9;
    private static final int REQUEST_CREATE_APPWIDGET = 5;

    private static int shortcutPage = -1;//-1 == no shortcut, change if launched from shortcut

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setTitle(R.string.title_activity_main);

        Util.init(this);

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        pages = new ArrayList<>();

        String savedFragments = sharedPreferences.getString(getSavedFragmentsKey(), "");
        if (DEBUG) Log.v(LOG_TAG, "savedFragments: " + savedFragments);

        pagerTabStrip = (PagerTabStrip) findViewById(R.id.pager_tab_strip);
        viewPager = (CustomViewPager) findViewById(R.id.view_pager);

        forceOrientation = getIntent().getIntExtra(EXTRA_FORCE_ORIENTATION, -1);

        restoreContentFromRecreationKey(savedFragments);
        pages.get(0).setMenuEnabled(true);

        fragmentPagerAdapter = new CustomFragmentPagerAdapter(this, getSupportFragmentManager());
        viewPager.setAdapter(fragmentPagerAdapter);

        viewPager.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft, int oldTop, int oldRight, int oldBottom) {
                boolean newY = Util.newY(viewPager);
                boolean newX = Util.newX(viewPager);
                if (newY || newX) {
                    viewPager.post(new Runnable() {
                        @Override
                        public void run() {
                            resizeContent();
                        }
                    });
                }
            }
        });

        fullscreen = sharedPreferences.getBoolean(Preferences.FULLSCREEN, false);
        hideNavigationBar = sharedPreferences.getBoolean(Preferences.HIDE_NAVIGATION_BAR, false);
        hideActionBar = sharedPreferences.getBoolean(Preferences.HIDE_ACTION_BAR, false);

        decorView = getWindow().getDecorView();
        decorView.setOnSystemUiVisibilityChangeListener(
                new View.OnSystemUiVisibilityChangeListener() {
                    @Override
                    public void onSystemUiVisibilityChange(int visibility) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                            boolean visible = (visibility & View.SYSTEM_UI_FLAG_HIDE_NAVIGATION) == 0;
                            if (visible) {
                                delayedHide();
                            }
                        }
                    }
                });

        appWidgetManager = AppWidgetManager.getInstance(this);
        appWidgetHost = new AppWidgetHost(this, APPWIDGET_HOST_ID);

        tcpConnectionManager = TcpConnectionManager.getInstance(getApplicationContext());
        tcpConnectionManager.recreateConnectionsFromRecreationKey(sharedPreferences.getString(
                Preferences.SAVED_CONNECTIONS, ""));

        if (sharedPreferences.getInt(Preferences.SEEN_GREETING_VERSION, 0) <
                GreetingDialog.VERSION) {
            new GreetingDialog().show(getSupportFragmentManager(), "GreetingDialog");
        } else if (SetupGridSizeDialog.shouldShow(sharedPreferences)) {
            new SetupGridSizeDialog()
                    .show(getSupportFragmentManager(), "SetupGridSizeDialog");
        }

        if (DEBUG) {
            getSupportFragmentManager().addOnBackStackChangedListener(
                    new FragmentManager.OnBackStackChangedListener() {
                        @Override
                        public void onBackStackChanged() {
                            Log.v(LOG_TAG, "Back stack count: " +
                                    getFragmentManager().getBackStackEntryCount());
                        }
                    });
        }

        if (getIntent().getBooleanExtra(EXTRA_RESTARTED_FROM_EDIT_MODE, false)) {
            enterEditMode();
        }

        if (sharedPreferences.getBoolean(Preferences.GLOBAL_ACTIONS_ENABLE, false)) {
            if (Build.VERSION.SDK_INT < 21) {
                sharedPreferences.edit().remove(Preferences.GLOBAL_ACTIONS_ENABLE).apply();
            } else {
                GlobalActionHandler.enable(this);
            }
        }
    }
    private Runnable hideSystemUIRunnable = new Runnable() {
        @Override
        public void run() {
            hideSystemUI();
        }
    };
    private void delayedHide() {
        hideSystemUIHandler.removeCallbacks(hideSystemUIRunnable);
        hideSystemUIHandler.postDelayed(hideSystemUIRunnable, Util.getPreferenceInt(
                sharedPreferences, Preferences.SYSTEM_UI_TIMEOUT, 3) * 1000);
    }
    public void hideSystemUI() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT &&
                fullscreen || hideNavigationBar) {
            decorView.setSystemUiVisibility(getHideSystemUIFlags());
        }
    }
    private void resizeContent() {
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).resize(true);
        }

    }
    private int getHideSystemUIFlags() {
        int flags;
        if (Build.VERSION.SDK_INT >= 19) {
            flags = View.SYSTEM_UI_FLAG_IMMERSIVE;
        } else {
            flags = 0;
        }
        if (Build.VERSION.SDK_INT >= 16) {
            if (fullscreen) {
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_FULLSCREEN;
            }
            if (hideNavigationBar) {
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
            }
            if (hideActionBar) {
                flags |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE;
            }
        }
        return flags;
    }
    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        super.onWindowFocusChanged(hasFocus);

        if (hasFocus) {
            hideSystemUI();
        } else {
            hideSystemUIHandler.removeCallbacks(hideSystemUIRunnable);
        }
    }
    @Override
    protected void onPause() {
        super.onPause();

        viewPager.removeOnPageChangeListener(this);

        saveConnections();
        try {
            appWidgetHost.stopListening();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Failed to unlisten to app widget host", e);
        }

        if (changedRingerMode) {
            changedRingerMode = false;
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            setRingerMode(audioManager, previousRingerMode);
        }
    }
    @Override
    protected void onResume() {
        super.onResume();
        appWidgetHost.startListening();

        tcpConnectionManager.refreshConnections();

        viewPager.addOnPageChangeListener(this);
        onPageSelected(viewPager.getCurrentItem());

        int tmp = Util.getPreferenceInt(sharedPreferences, Preferences.OFFSCREEN_PAGE_LIMIT, 2);
        if (tmp < 0) {
            neverDestroyPages = true;
            viewPager.setOffscreenPageLimit(pages.size());
        } else {
            neverDestroyPages = false;
            viewPager.setOffscreenPageLimit(tmp);
        }

        fullscreen = sharedPreferences.getBoolean(Preferences.FULLSCREEN, false);
        hideNavigationBar = sharedPreferences
                .getBoolean(Preferences.HIDE_NAVIGATION_BAR, false);
        hideActionBar = sharedPreferences.getBoolean(Preferences.HIDE_ACTION_BAR, false);


        int pagerTabStripVisibility =
                sharedPreferences.getBoolean(Preferences.HIDE_PAGER_TAB_STRIP, false) ?
                        View.GONE : View.VISIBLE;
        if (pagerTabStrip.getVisibility() != pagerTabStripVisibility) {
            pagerTabStrip.setVisibility(pagerTabStripVisibility);
        }

        String ringerMode = sharedPreferences
                .getString(Preferences.CHANGE_RINGER_MODE,
                        getString(R.string.pref_ringer_mode_keep_value));
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        previousRingerMode = audioManager.getRingerMode();
        if (getString(R.string.pref_ringer_mode_mute_value).equals(ringerMode)) {
            setRingerMode(audioManager, AudioManager.RINGER_MODE_SILENT);
            changedRingerMode = true;
        } else if (getString(R.string.pref_ringer_mode_vibrate_value).equals(ringerMode)) {
            setRingerMode(audioManager, AudioManager.RINGER_MODE_VIBRATE);
            changedRingerMode = true;
        } else if (getString(R.string.pref_ringer_mode_vibrate_if_not_muted_value)
                .equals(ringerMode) && previousRingerMode != AudioManager.RINGER_MODE_SILENT) {
            setRingerMode(audioManager, AudioManager.RINGER_MODE_VIBRATE);
            changedRingerMode = true;
        }

        if (forceOrientation == FORCE_ORIENTATION_PORTRAIT) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if (forceOrientation == FORCE_ORIENTATION_LANDSCAPE) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            String orientation = sharedPreferences.getString(Preferences.ORIENTATION,
                    Preferences.ORIENTATION_SHARE_LAYOUT);
            if (Preferences.ORIENTATION_PORTRAIT_ONLY.equals(orientation)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else if (Preferences.ORIENTATION_LANDSCAPE_ONLY.equals(orientation)) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER);
            }
        }

        resizeContent();

        setLockedModeVisibilities();

        if (sharedPreferences.getBoolean(Preferences.SHOW_ON_SECURE_LOCKSCREEN, false)) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        }
    }
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        getMenuInflater().inflate(R.menu.menu_main, menu);
        editModeMenuItem = menu.findItem(R.id.action_edit_page_content);
        connectionMgrMenuItem = menu.findItem(R.id.action_connection_manager);
        setLockedModeVisibilities();
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class)
                        .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                return true;
            case R.id.action_about:
                new AboutDialog().show(getSupportFragmentManager(), "AboutDialog");
                return true;
            case R.id.action_edit_page_content:
                enterEditMode();
                return true;
            case R.id.action_connection_manager:
                new SelectConnectionDialog().show(getSupportFragmentManager(), "SelectConnectionDialog");
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        // Most our fragments can't handle saved instances right now...
        //if (discardSavedInstance) {
            outState.clear();
        //}
    }

    @Override
    public void onConfigurationChanged(Configuration configuration) {
        super.onConfigurationChanged(configuration);

        if (!Preferences.ORIENTATION_SHARE_LAYOUT.equals(sharedPreferences.getString(
                Preferences.ORIENTATION, Preferences.ORIENTATION_SHARE_LAYOUT))) {
            // Restart for new setup; don't recreate old fragments
            discardSavedInstance = true;
            restart(null);
        }
    }

    // Hardware key listener
    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
        switch (volumeButtonSettingsShortcutCount) {
            case 0:
            case 1:
            case 4:
            case 6:
                resetVolumeButtonSettingsShortcutHandler.removeCallbacks(resetVolumeButtonSettingsShortcut);
                if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
                    volumeButtonSettingsShortcutCount++;
                    resetVolumeButtonSettingsShortcutHandler.postDelayed(resetVolumeButtonSettingsShortcut, 1000);
                } else {
                    volumeButtonSettingsShortcutCount = 0;
                }
                break;
            case 2:
            case 3:
            case 5:
                resetVolumeButtonSettingsShortcutHandler.removeCallbacks(resetVolumeButtonSettingsShortcut);
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    volumeButtonSettingsShortcutCount++;
                    resetVolumeButtonSettingsShortcutHandler.postDelayed(resetVolumeButtonSettingsShortcut, 1000);
                } else {
                    volumeButtonSettingsShortcutCount = 0;
                }
                break;
            case 7:
                resetVolumeButtonSettingsShortcutHandler
                        .removeCallbacks(resetVolumeButtonSettingsShortcut);
                if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
                    Toast.makeText(getApplicationContext(),
                            R.string.toast_volume_button_settings_shortcut, Toast.LENGTH_LONG)
                            .show();
                    startActivity(new Intent(this, SettingsActivity.class)
                            .addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT));
                }
                volumeButtonSettingsShortcutCount = 0;
                break;
            default:
                volumeButtonSettingsShortcutCount = 0;
                break;
        }
        return pages.get(viewPager.getCurrentItem()).onKeyDown(keyCode) ||
                super.onKeyDown(keyCode, event);
    }
    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                return pages.get(viewPager.getCurrentItem()).onKeyUp(keyCode) ||
                        super.onKeyDown(keyCode, event);
        }

        return super.onKeyUp(keyCode, event);
    }

    public void addPage(PageContainerFragment fragment) {
        if (neverDestroyPages) {
            viewPager.setOffscreenPageLimit(pages.size());
        }
        if (actionMode != null) {
            fragment.onStartDragMode();
        }
        pages.add(fragment);
        notifyDataSetChanged();
        viewPager.setCurrentItem(pages.size() - 1, true);
    }
    public void notifyDataSetChanged() {
        if (fragmentPagerAdapter != null) {
            fragmentPagerAdapter.notifyDataSetChanged();
        }
    }
    // For appropriate page display after page removal, a complete activity restart is required
    // (recreate() is not enough)
    public void removePage(PageContainerFragment page, boolean callOnRemove) {
        int pos = pages.indexOf(page);
        if (pos != 0) {
            pos--;
        }
        pages.remove(page);
        fragmentPagerAdapter.notifyDataSetChanged();
        if (callOnRemove) {
            page.onRemove();
        }
        restart(pos);
    }

    private void restart(Intent intent) {
        if (intent == null) {
            intent = getIntent();
        }
        finish();
        overridePendingTransition(0, 0);
        startActivity(intent);
        overridePendingTransition(0, 0);
    }
    private void restart(int selectPagePos) {
        Intent intent = getIntent();
        if (selectPagePos >= 0 && pages.size() > selectPagePos) {
            intent.putExtra(EXTRA_SELECT_PAGE_ID, pages.get(selectPagePos).getPageId());
            // Don't toast in case the page can not be selected
            intent.putExtra(EXTRA_RESTARTED_FROM_EDIT_MODE, true);
        }
        saveFragments();
        restart(intent);
    }

    public void scrollToPage(PageContainerFragment page) {
        int pos = pages.indexOf(page);
        if (pos >= 0 && pos != viewPager.getCurrentItem()) {
            viewPager.setCurrentItem(pos, true);
        }
    }

    public void saveSettings() {
        saveFragments();
        saveConnections();
    }
    private String getSavedFragmentsKey() {
        String orientationPref = sharedPreferences.getString(Preferences.ORIENTATION,
                Preferences.ORIENTATION_SHARE_LAYOUT);
        boolean landscape = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE &&
                !Preferences.ORIENTATION_SHARE_LAYOUT.equals(orientationPref);
        if (landscape) {
            return Preferences.SAVED_FRAGMENTS_LANDSCAPE;
        } else {
            return Preferences.SAVED_FRAGMENTS;
        }
    }
    private void saveFragments() {
        String key = ModuleFragment.ROOT + separator + Util.RECREATION_KEY_VERSION + separator;
        for (int i = 0; i < pages.size(); i++) {
            ModuleFragment fragment = pages.get(i);
            key += fragment.getRecreationKey() + separator;
        }
        if (DEBUG) Log.v(LOG_TAG, "saveFragments: " + key);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getSavedFragmentsKey(), key);
        editor.apply();
    }
    private void saveConnections() {
        String key = tcpConnectionManager.getConnectionRecreationKey();
        if (DEBUG) Log.v(LOG_TAG, "saveConnections: " + key);
        sharedPreferences.edit().putString(Preferences.SAVED_CONNECTIONS, key).apply();
    }

    private void restoreContentFromRecreationKey(String key) {
        String[] args = Util.split(key, separator, 0);
        int recreationVersion;
        try {
            recreationVersion = Integer.parseInt(args[1]);
            if (DEBUG) Log.v(LOG_TAG, "RecreationVersion: " + recreationVersion);
        } catch (Exception e) {
            Log.w(LOG_TAG, "Got exception while trying to get recreationVersion for key " + key);
            Log.w(LOG_TAG, "Got exception: " + e);
            // Try using current version instead
            recreationVersion = Util.RECREATION_KEY_VERSION;
        }
        for(int i = 2; i < args.length; i++) {
            String[] subArgs = Util.split(args[i], Util.RK_ATTRIBUTE_SEPARATOR, 1);
            if (ModuleFragment.PAGE_CONTAINER_FRAGMENT.equals(subArgs[0])) {
                addPage(PageContainerFragment.recoverFromRecreationKey(args[i]));
            } else {
                Log.w(LOG_TAG, "restoreContentFromRecreationKey:" +
                        " Only allowed to add PageContainers to MainActivity, you tried to add: " +
                        subArgs[0]);
            }
        }

        if (pages.size() == 0) {// Not allowed to have no fragments
            addPage(PageContainerFragment.newInstance(getString(R.string.page_name_default),
                    new Display.StaticTextSettings(getString(R.string.app_name)),
                    false, null, null, null, null));
        }

        // Select page if shortcut used and page is on current orientation:
        Intent intent = getIntent();
        long pageId = intent.getLongExtra(EXTRA_SELECT_PAGE_ID, -1);
        boolean landscape = getResources().getConfiguration().orientation ==
                Configuration.ORIENTATION_LANDSCAPE;
        if (pageId != -1 &&
                (forceOrientation == -1 ||
                        (landscape && forceOrientation == FORCE_ORIENTATION_LANDSCAPE) ||
                        (!landscape && forceOrientation == FORCE_ORIENTATION_PORTRAIT))) {
            for (int i = 0; i < pages.size(); i++) {
                if (pages.get(i).getPageId() == pageId) {
                    shortcutPage = i;
                }
            }
            if (shortcutPage == -1) {// Could not find page
                if (!intent.getBooleanExtra(EXTRA_RESTARTED_FROM_EDIT_MODE, false))
                    Toast.makeText(this, R.string.error_page_not_available, Toast.LENGTH_LONG)
                            .show();
            }
        }
    }
    public static String[] getPageNamesFromRecreationKey(String key, ArrayList<Long> pageIDs) {
        ArrayList<String> pageNames = new ArrayList<>();
        String[] args = Util.split(key, separator, 0);
        for (int i = 1; i < args.length; i++) {
            String[] subArgs = Util.split(args[i], Util.RK_ATTRIBUTE_SEPARATOR, 1);
            if (ModuleFragment.PAGE_CONTAINER_FRAGMENT.equals(subArgs[0])) {
                pageNames.add(PageContainerFragment.getPageNameFromRecreationKey(args[i]));
                pageIDs.add(PageContainerFragment.getPageIdFromRecreationKey(args[i]));
            } else {
                Log.e(LOG_TAG, "getPageNamesFromRecreationKey: Only allowed to add " +
                        "PageContainers to MainActivity, you tried to add: " + subArgs[0]);
            }
        }
        return pageNames.toArray(new String[pageNames.size()]);
    }

    public static class CustomFragmentPagerAdapter
            extends androidx.fragment.app.FragmentPagerAdapter {
        MainActivity activity;

        public CustomFragmentPagerAdapter (MainActivity activity, FragmentManager fragmentManager) {
            super(fragmentManager);
            this.activity = activity;
        }
        @Override
        public int getCount() {
            return activity.pages.size();
        }
        @Override
        public Fragment getItem(int position) {
            return activity.pages.get(position);
        }
        @Override
        public CharSequence getPageTitle (int position) {
            return activity.pages.get(position).getName();
        }
        @Override
        public void finishUpdate(ViewGroup container) {
            super.finishUpdate(container);

            if (shortcutPage >= 0 && shortcutPage < getCount()) {
                viewPager.setCurrentItem(shortcutPage);
                shortcutPage = -1;//use shortcut only once
            }
        }
    }

    public void addWidget(Container container) {
        addWidgetContainer = container;
        int appWidgetId = appWidgetHost.allocateAppWidgetId();
        Intent pickIntent = new Intent(AppWidgetManager.ACTION_APPWIDGET_PICK)
                .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        startActivityForResult(pickIntent, MainActivity.REQUEST_PICK_APPWIDGET);
    }
    public void setAddWidgetListener(DialogFragment addWidgetListener) {
        this.addWidgetListener = addWidgetListener;
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_PICK_APPWIDGET) {
                configureWidget(data);
            } else if (requestCode == REQUEST_CREATE_APPWIDGET) {
                createWidgetContainerFragment(data);
            }
        } else if (resultCode == RESULT_CANCELED && data != null) {
            int appWidgetId = data.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
            if (appWidgetId != -1) {
                appWidgetHost.deleteAppWidgetId(appWidgetId);
            }
        }
    }
    private void configureWidget(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        AppWidgetProviderInfo appWidgetProviderInfo = appWidgetManager.getAppWidgetInfo(appWidgetId);
        if (appWidgetProviderInfo.configure != null) {
            Intent intent = new Intent(AppWidgetManager.ACTION_APPWIDGET_CONFIGURE);
            intent.setComponent(appWidgetProviderInfo.configure);
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
            startActivityForResult(intent, REQUEST_CREATE_APPWIDGET);
        }
        else {
            createWidgetContainerFragment(data);
        }
    }
    private void createWidgetContainerFragment(Intent data) {
        Bundle extras = data.getExtras();
        int appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, -1);
        if (addWidgetContainer != null) {
            addWidgetContainer.addFragment(WidgetContainerFragment.newInstance(appWidgetId), false);
            if (addWidgetListener != null) {
                addWidgetListener.dismiss();
                addWidgetListener = null;
            }
        }
    }
    public View createWidget(int appWidgetId) {
        AppWidgetProviderInfo appWidgetProviderInfo =
                appWidgetManager.getAppWidgetInfo(appWidgetId);
        AppWidgetHostView hostView = appWidgetHost
                .createView(this, appWidgetId, appWidgetProviderInfo);
        hostView.setAppWidget(appWidgetId, appWidgetProviderInfo);
        return hostView;
    }
    public void removeWidget(int appWidgetId) {
        appWidgetHost.deleteAppWidgetId(appWidgetId);
    }
    public boolean isPageRemovalAllowed() {
        return pages.size() > 1;
    }

    public Container[] getAllContainers() {
        ArrayList<Container> list = new ArrayList<>();
        for (int i = 0; i < pages.size(); i++) {
            Container[] addition = pages.get(i).getAllContainers();
            list.addAll(Arrays.asList(addition));
        }
        return list.toArray(new Container[list.size()]);
    }

    public PageContainerFragment[] getPages() {
        return pages.toArray(new PageContainerFragment[pages.size()]);
    }

    public void orderPages(List<Integer> order) {
        ArrayList<PageContainerFragment> pages = new ArrayList<>();
        for (int i = 0; i < this.pages.size(); i++) {
            int index = order.indexOf(i);
            if (index < 0 || index >= this.pages.size()) {
                Log.e(LOG_TAG, "orderPages: illegal index " + index + " for page " + i);
                return;
            }
            pages.add(this.pages.get(index));
        }
        if (pages.equals(this.pages)) {
            return;
        }
        PageContainerFragment currentPage = this.pages.get(viewPager.getCurrentItem());
        this.pages = pages;
        notifyDataSetChanged();
        restart(pages.indexOf(currentPage));
    }

    // Required for the correct fragment size, as configured in the block unit preference
    public View getViewContainer() {
        return viewPager;
    }

    //ViewPager.OnPageChangeListener stuff
    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {}
    @Override
    public void onPageSelected(int position) {
        if (DEBUG) Log.v(LOG_TAG, "Select page " + position);
        setActionBarTitle(pages.get(position).getActionBarTitle());
        for (int i = 0; i < pages.size(); i++) {
            pages.get(i).setMenuEnabled(i == position);
        }
    }
    @Override
    public void onPageScrollStateChanged(int state) {}

    private Runnable resetVolumeButtonSettingsShortcut = new Runnable() {
        @Override
        public void run() {
            volumeButtonSettingsShortcutCount = 0;
        }
    };

    public void setActionBarTitle(PageContainerFragment page, String title) {
        try {
            if (page == pages.get(viewPager.getCurrentItem())) {
                setActionBarTitle(title);
            }
        } catch (Exception e) {
            // Catch case onResume, when time wants to update the actionbar
            // for not properly initialized activity
        }
    }
    private void setActionBarTitle(String title) {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(title);
        }
    }

    private void setLockedModeVisibilities() {
        // Only show some menu items if device unlocked
        KeyguardManager keyguardManager =
                (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        boolean unlocked = !keyguardManager.inKeyguardRestrictedInputMode();
        if (editModeMenuItem != null) {
            editModeMenuItem.setVisible(unlocked);
        }
        if (connectionMgrMenuItem != null) {
            connectionMgrMenuItem.setVisible(unlocked &&
                    tcpConnectionManager.getConnectionSuggestions().length != 0);
        }
    }

    public void setViewPagerEnabled(boolean enabled) {
        viewPager.setScrollEnabled(enabled);
    }

    private void cancelEditContainerModeToast() {
        if (editModeContainerDragMoveToast != null) {
            editModeContainerDragMoveToast.cancel();
            editModeContainerDragMoveToast = null;
        }
    }

    private void setRingerMode(AudioManager audioManager, int ringerMode) {
        //https://stackoverflow.com/questions/39151453/in-android-7-api-level-24-my-app-is-not-allowed-to-mute-phone-set-ringer-mode/39152607#39152607
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                && !notificationManager.isNotificationPolicyAccessGranted()) {
            // Permission will be requested when changing the setting again
            Log.w(LOG_TAG, "setRingerMode: discard change because of missing permission");
        } else {
            audioManager.setRingerMode(ringerMode);
        }

    }
}
