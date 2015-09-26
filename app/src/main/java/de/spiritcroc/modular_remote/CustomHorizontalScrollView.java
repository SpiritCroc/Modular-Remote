package de.spiritcroc.modular_remote;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.MotionEventCompat;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;

import de.spiritcroc.modular_remote.modules.HorizontalContainerFragment;

/**
 * Enables reasonable support for HorizontalContainerFragments within HorizontalContainerFragments
 */

public class CustomHorizontalScrollView extends HorizontalScrollView {
    private float pointerX;

    private HorizontalContainerFragment wrapFragment;

    public CustomHorizontalScrollView(Context context, AttributeSet attrs) {
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
                } else if ((ev.getX() > pointerX && !canScrollLeft()) ||
                        (ev.getX() < pointerX && !canScrollRight())) {
                    result = false;
                } else {
                    if (wrapFragment != null && !wrapFragment.hasChildWithScrollX()) {
                        // We're the smallest scroll view with this direction in this hierarchy,
                        // so disallow the parents
                        View view = this;
                        while (view.getParent() != null && view.getParent() instanceof View) {
                            if (view.getParent() instanceof CustomHorizontalScrollView) {
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

        pointerX = ev.getX();

        return result;
    }

    public void setWrapFragment(HorizontalContainerFragment wrapFragment) {
        this.wrapFragment = wrapFragment;
    }

    private boolean canScrollLeft() {
        return getScrollX() != 0;
    }
    private boolean canScrollRight() {
        return getMeasuredWidth() + getScrollX() != getChildAt(0).getMeasuredWidth();
    }
    public boolean canScroll() {
        return getMeasuredWidth() < getChildAt(0).getMeasuredWidth();
    }
}
