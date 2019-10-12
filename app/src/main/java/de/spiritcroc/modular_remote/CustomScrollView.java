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

import androidx.annotation.NonNull;
import androidx.core.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ScrollView;

import de.spiritcroc.modular_remote.modules.ScrollContainerFragment;

/**
 * Enables reasonable support for ScrollContainerFragments within ScrollContainerFragments
 */

public class CustomScrollView extends ScrollView {
    private float pointerY;

    private ScrollContainerFragment wrapFragment;

    public CustomScrollView(Context context, AttributeSet attrs) {
        super (context, attrs);
    }

    @Override
    public boolean onInterceptTouchEvent(@NonNull MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);

        boolean result;

        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (!canScroll()) {
                    // If it can't scroll, don't intercept touch events
                    result = false;
                } else if ((ev.getY() > pointerY && !canScrollUp()) ||
                        (ev.getY() < pointerY && !canScrollDown())) {
                    result = false;
                } else {
                    if (wrapFragment != null && !wrapFragment.hasChildWithScrollY()) {
                        // We're the smallest scroll view with this direction in this hierarchy,
                        // so disallow the parents
                        View view = this;
                        while (view.getParent() != null && view.getParent() instanceof View) {
                            if (view.getParent() instanceof CustomScrollView) {
                                // Only steal touch event from views with the same direction
                                view.getParent().requestDisallowInterceptTouchEvent(true);
                            }
                            view = (View) view.getParent();
                        }
                    }
                    result = super.onInterceptTouchEvent(ev);
                }
                break;
            default:
                result = super.onInterceptTouchEvent(ev);
        }

        pointerY = ev.getY();

        return result;
    }

    public void setWrapFragment(ScrollContainerFragment wrapFragment) {
        this.wrapFragment = wrapFragment;
    }

    private boolean canScrollUp() {
        return getScrollY() != 0;
    }
    private boolean canScrollDown() {
        return getMeasuredHeight() + getScrollY() != getChildAt(0).getMeasuredHeight();
    }
    public boolean canScroll() {
        return getMeasuredHeight() < getChildAt(0).getMeasuredHeight();
    }
}
