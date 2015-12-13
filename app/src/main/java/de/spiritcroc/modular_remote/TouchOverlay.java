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
import android.view.MotionEvent;
import android.widget.FrameLayout;

/**
 * An overlay to receive all touches and forward them to the resizeFrame
 */
public class TouchOverlay extends FrameLayout {
    ResizeFrame resizeFrame;

    public TouchOverlay(Context context, ResizeFrame resizeFrame) {
        super(context);
        this.resizeFrame = resizeFrame;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return resizeFrame.handleTouchEvent(this, event);
    }
}
