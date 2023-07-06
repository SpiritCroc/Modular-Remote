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

public final class Preferences {

    /**
     * The recovery keys to recreate the fragments in portrait orientation
     */
    public static final String SAVED_FRAGMENTS = "saved_fragments";

    /**
     * The recovery keys to recreate the fragments in landscape orientation
     */
    public static final String SAVED_FRAGMENTS_LANDSCAPE = "saved_fragments_landscape";

    /**
     * The amount of widgets with the same appWidgetId.
     * Saved in order to prevent the removal of the widget when there are several instances of the
     * same widget (after copying it)
     */
    public static final String WIDGET_CONTAINER_AMOUNT_ = "widget_container_amount_";// += appWidgetId

    /**
     * Save connections in order to recreate them with the right settings (e.g. customized arrays),
     * and also suggest the connection for new fragments, even if the connection is not required
     */
    public static final String SAVED_CONNECTIONS = "saved_connections";

    /**
     * Amount of attempts to reconnect to a device to which connection was lost
     */
    public static final String RECONNECTION_ATTEMPTS = "pref_reconnection_attempts";

    /**
     * Time interval in milliseconds, after which a new reconnection attempt should be started, if
     * {@link #RECONNECTION_ATTEMPTS} > 1
     */
    public static final String RECONNECTION_INTERVAL = "pref_reconnection_interval";

    /**
     * How many pages of the viewPages will not get destroyed in each direction.
     * Necessary e.g. for keeping WebViews alive
     */
    public static final String OFFSCREEN_PAGE_LIMIT = "pref_offscreen_page_limit";

    /**
     * The time interval in milliseconds in which displays in clock mode should be updated
     */
    public static final String TIME_UPDATE_INTERVAL = "pref_time_update_interval";

    /**
     * Whether to use fullscreen
     */
    public static final String FULLSCREEN = "pref_fullscreen";

    /**
     * Whether to hide the navigation bar
     */
    public static final String HIDE_NAVIGATION_BAR = "pref_hide_navigation_bar";

    /**
     * Whether to hide the action bar
     */
    public static final String HIDE_ACTION_BAR = "pref_hide_action_bar";

    /**
     * Whether to hide the pager tab strip
     */
    public static final String HIDE_PAGER_TAB_STRIP = "pref_hide_pager_tab_strip";

    /**
     * The time until fullscreen will re-appear after showing system ui elements (navbar, actionbar)
     * in milliseconds
     */
    public static final String SYSTEM_UI_TIMEOUT = "pref_system_ui_timeout";

    /**
     * Whether and how to change the ringer mode while using the remote
     */
    public static final String CHANGE_RINGER_MODE = "pref_change_ringer_mode";

    /**
     * Amount of units in which the width of the screen should get divided in portrait mode
     */
    public static final String BLOCK_SIZE = "pref_block_size";

    /**
     * Amount of units in which the height of the screen should get divided in portrait mode
     */
    public static final String BLOCK_SIZE_HEIGHT = "pref_block_size_height";

    /**
     * Amount of units in which the width of the screen should get divided in landscape mode
     */
    public static final String BLOCK_SIZE_LANDSCAPE = "pref_block_size_landscape";

    /**
     * Amount of units in which the height of the screen should get divided in landscape mode
     */
    public static final String BLOCK_SIZE_HEIGHT_LANDSCAPE = "pref_block_size_height_landscape";

    /**
     * The default width of newly added fragments
     */
    public static final String FRAGMENT_DEFAULT_WIDTH = "pref_fragment_default_width";

    /**
     * The default height of newly added fragments
     */
    public static final String FRAGMENT_DEFAULT_HEIGHT = "pref_fragment_default_height";

    /**
     * ID of the last added page. Prevents to create pages with the same ID in order to have
     * distinct launcher shortcuts
     */
    public static final String LAST_PAGE_ID = "last_page_id";

    /**
     * The time in milliseconds after which a click should be interpreted as a single click if there
     * was no second click (double click)
     */
    public static final String DOUBLE_CLICK_TIMEOUT = "pref_double_click_timeout";

    /**
     * The time interval in milliseconds in which the connectivity should be checked in order to
     * recognize when connection to controlled device was lost
     */
    public static final String CHECK_CONNECTIVITY_INTERVAL = "pref_check_connectivity_interval";

    /**
     * The latest version of GreetingsDialog that the user has seen
     */
    public static final String SEEN_GREETING_VERSION = "seen_greeting_version";

    /**
     * How orientation changes are handled
     */
    public static final String ORIENTATION = "pref_orientation";

    /**
     * Value for {@link #ORIENTATION}. When set, portrait and landscape orientation both use the
     * fragments and block size settings from portrait orientation
     */
    public static final String ORIENTATION_SHARE_LAYOUT = "share";

    /**
     * Value for {@link #ORIENTATION}. When set, portrait and landscape orientation each use their
     * own fragments and block size settings
     */
    public static final String ORIENTATION_SEPARATE_LAYOUT = "separate";

    /**
     * Value for {@link #ORIENTATION}. When set, the remote will only show in portrait orientation
     */
    public static final String ORIENTATION_PORTRAIT_ONLY = "portrait";


    /**
     * Value for {@link #ORIENTATION}. When set, the remote will only show in landscape orientation
     */
    public static final String ORIENTATION_LANDSCAPE_ONLY = "landscape";

    /**
     * Whether to enable a media session for global actions
     */
    public static final String GLOBAL_ACTIONS_ENABLE = "pref_global_actions_enable";

    /**
     * Volume up global action
     */
    public static final String GLOBAL_ACTION_VOLUME_UP = "pref_global_action_volume_up";

    /**
     * Volume down global action
     */
    public static final String GLOBAL_ACTION_VOLUME_DOWN = "pref_global_action_volume_down";

    /**
     * Whether to show on secure lockscreen
     */
    public static final String SHOW_ON_SECURE_LOCKSCREEN = "pref_show_on_secure_lockscreen";
}
