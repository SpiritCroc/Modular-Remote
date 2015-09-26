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

import android.app.Fragment;

import de.spiritcroc.modular_remote.Util;

public abstract class ModuleFragment extends Fragment {
    public abstract String getReadableName();
    public abstract String getRecreationKey();
    public abstract void setMenuEnabled(boolean menuEnabled);

    /**
     * Some fragments (e.g. WidgetContainerFragment) might need to free some space on removal
     */
    public void onRemove() {}
    public abstract Container getParent();
    public abstract void setParent(Container parent);

    /**
     * @return
     * For modules that have a TcpConnection: connection.isConnected()
     * For containers: is at least one connected
     * For modules with no connection: true
     */
    public abstract boolean isConnected();

    /**
     * @return
     * A copy of the calling fragment
     * For containers: copy includes copies of content (do this by giving own recreation key,
     * as layout probably not created at the time copy is called)
     */
    public abstract ModuleFragment copy();

    public final static String ROOT = "root";
    public final static String WEB_VIEW_FRAGMENT = "web_view_fragment";
    public final static String SCROLL_CONTAINER_FRAGMENT = "scroll_container_fragment";
    public final static String PAGE_CONTAINER_FRAGMENT = "page_container_fragment";
    public final static String WIDGET_CONTAINER_FRAGMENT = "widget_container_fragment";
    public final static String HORIZONTAL_CONTAINER = "horizontal_container";
    public final static String BUTTON_FRAGMENT = "button_fragment";
    public final static String DISPLAY_FRAGMENT = "display_fragment";
    public final static String SPINNER_FRAGMENT = "spinner_fragment";
    public final static String TOGGLE_FRAGMENT = "toggle_fragment";

    protected final static int MENU_ORDER = 50;

    /**
     * Resize if block unit size changed
     * Containers should call the same method for their contained fragments
     */
    public abstract void resize();

    // For shorter code:
    protected final static String SEP = Util.RK_ATTRIBUTE_SEPARATOR;
    protected static String fixRecreationKey(String key) {
        return Util.fixRecreationKey(key, SEP);
    }
}
