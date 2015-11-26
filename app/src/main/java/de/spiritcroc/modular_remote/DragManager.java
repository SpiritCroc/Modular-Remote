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

import android.util.Log;

import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.ModuleFragment;

public class DragManager {
    private static final String LOG_TAG = DragManager.class.getSimpleName();
    private static final boolean DEBUG = false;

    private static ModuleFragment dragFragment;

    private DragManager(){}

    public static boolean startDrag(ModuleFragment fragment) {
        // Make sure that parent containers don't replace dragged fragment
        if (dragFragment == null ||
                !(fragment instanceof Container)) {
            if (DEBUG) Log.v(LOG_TAG, "Start drag: " + fragment);
            dragFragment = fragment;
            fragment.onStartDrag();
            return true;
        } else {
            if (DEBUG) Log.d(LOG_TAG, "Don't start drag for " + fragment + ": already dragging " +
                    dragFragment);
            return false;
        }
    }
    public static ModuleFragment stopDrag() {
        ModuleFragment fragment = dragFragment;
        if (DEBUG) Log.v(LOG_TAG, "Stop drag: " + fragment);
        dragFragment = null;
        if (fragment != null) {
            fragment.onStopDrag();
        }
        return fragment;
    }

    private static float dragStartX, dragStartY;
    public static void setLongPressPos(float dragStartX, float dragStartY) {
        DragManager.dragStartX = dragStartX;
        DragManager.dragStartY = dragStartY;
    }
    public static float getDragStartX() {
        return dragStartX;
    }
    public static float getDragStartY() {
        return dragStartY;
    }
    public static void cancelLongPress() {
        dragStartX = -1;
    }
    public static boolean isLongPressPossible() {
        return dragStartX != -1;
    }
}
