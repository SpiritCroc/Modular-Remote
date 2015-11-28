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

import android.support.annotation.Nullable;
import android.text.Spannable;

public interface Container {
    void addFragment(ModuleFragment fragment, boolean post);

    /**
     * @param callOnRemove
     * True if fragment will be completely removed, false if fragment will get added to a container
     * later
     */
    void removeFragment(ModuleFragment fragment, boolean callOnRemove);
    int getDepth();
    boolean scrollsX();
    boolean scrollsY();
    /**
     * @return
     * Scroll offset in x-direction
     */
    int getScrollX();
    /**
     * @return
     * Scroll offset in y-direction
     */
    int getScrollY();

    /**
     * If container needs some resizing because of moved content, it can be called in this method
     */
    void onContentMoved();

    /**
     * @return
     * Util.getAllContainers()
     */
    Container[] getAllContainers();

    /**
     * @return
     * Container that calls the method and all contained elements
     */
    ModuleFragment[] getAllFragments();

    /**
     * @return
     * Directly contained elements
     */
    ModuleFragment[] getFragments();

    /**
     * @return
     * Util.getContainerContentReadableName()
     */
    Spannable getContentReadableName(@Nullable String prefix);

    boolean isEmpty();
    int getFragmentCount();

    String getReadableName();
}
