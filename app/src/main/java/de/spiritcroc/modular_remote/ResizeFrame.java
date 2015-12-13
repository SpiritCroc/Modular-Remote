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

/*
 * Code inspired and graphics used from AppWidgetResizeFrame of the AOSP launcher
 * Copyright (c) 2005-2008, The Android Open Source Project
 * Licensed under the Apache License, Version 2.0
 */


package de.spiritcroc.modular_remote;

import android.content.Context;
import android.graphics.Rect;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import de.spiritcroc.modular_remote.modules.ModuleFragment;

public class ResizeFrame extends FrameLayout {
    private static final String LOG_TAG = ResizeFrame.class.getSimpleName();

    private final float DIMMED_HANDLE_ALPHA = 0f;

    private ModuleFragment fragment;

    private final ImageView leftHandle;
    private final ImageView rightHandle;
    private final ImageView topHandle;
    private final ImageView bottomHandle;

    private final int handleMargin;

    private enum DragMode {NONE, LEFT, TOP, RIGHT, BOTTOM}
    private DragMode dragMode = DragMode.NONE;

    public ResizeFrame(Context context, ModuleFragment fragment) {
        super(context);

        this.fragment = fragment;

        setBackgroundResource(R.drawable.widget_resize_shadow);
        setForeground(getResources().getDrawable(R.drawable.widget_resize_frame));
        setPadding(0, 0, 0, 0);

        handleMargin = getResources().getDimensionPixelSize(R.dimen.resize_handle_margin);

        LayoutParams lp;
        leftHandle = new ImageView(context);
        leftHandle.setImageResource(R.drawable.ic_widget_resize_handle);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.LEFT | Gravity.CENTER_VERTICAL);
        lp.leftMargin = handleMargin;
        addView(leftHandle, lp);

        rightHandle = new ImageView(context);
        rightHandle.setImageResource(R.drawable.ic_widget_resize_handle);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        lp.rightMargin = handleMargin;
        addView(rightHandle, lp);

        topHandle = new ImageView(context);
        topHandle.setImageResource(R.drawable.ic_widget_resize_handle);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.TOP);
        lp.topMargin = handleMargin;
        addView(topHandle, lp);

        bottomHandle = new ImageView(context);
        bottomHandle.setImageResource(R.drawable.ic_widget_resize_handle);
        lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
                Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        lp.bottomMargin = handleMargin;
        addView(bottomHandle, lp);
    }

    public void snapToFragment() {
        if (!(getLayoutParams() instanceof RelativeLayout.LayoutParams)) {
            Log.w(LOG_TAG, "!(getLayoutParams() instanceof FrameLayout.LayoutParams): " +
                    getLayoutParams());
            return;
        }
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();

        int offset = getOffset();

        // Allow offscreen
        lp.rightMargin = -offset;
        lp.bottomMargin = -offset;

        View cView = Util.getPrimeContainer(fragment).getView();
        int newWidth = Util.getWidthFromBlockUnits(cView, fragment.getPosWidth(), false) +
                2 * offset;
        int newHeight = Util.getHeightFromBlockUnits(cView, fragment.getPosHeight(), false) +
                2 * offset;

        int newX = Util.getWidthFromBlockUnits(cView, fragment.getPosX(), true) - offset;
        int newY = Util.getHeightFromBlockUnits(cView, fragment.getPosY(), true) - offset;

        lp.width = newWidth;
        lp.height = newHeight;
        lp.leftMargin = newX;
        lp.topMargin = newY;
        leftHandle.setAlpha(1.0f);
        rightHandle.setAlpha(1.0f);
        topHandle.setAlpha(1.0f);
        bottomHandle.setAlpha(1.0f);
        requestLayout();
    }

    public boolean handleTouchEvent(TouchOverlay overlay, MotionEvent event) {
        int[] location = new int[2];

        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        int allowOffsetX = lp.width/6;
        int allowOffsetY = lp.height/6;

        leftHandle.getLocationOnScreen(location);
        Rect left = new Rect(location[0] - allowOffsetX, location[1] - allowOffsetY,
                location[0] + leftHandle.getMeasuredWidth() + allowOffsetX,
                location[1] + leftHandle.getMeasuredHeight() + allowOffsetY);

        topHandle.getLocationOnScreen(location);
        Rect top = new Rect(location[0] - allowOffsetX, location[1] - allowOffsetY,
                location[0] + topHandle.getMeasuredWidth() + allowOffsetX,
                location[1] + topHandle.getMeasuredHeight() + allowOffsetY);

        rightHandle.getLocationOnScreen(location);
        Rect right = new Rect(location[0] - allowOffsetX, location[1] - allowOffsetY,
                location[0] + rightHandle.getMeasuredWidth() + allowOffsetX,
                location[1] + rightHandle.getMeasuredHeight() + allowOffsetY);

        bottomHandle.getLocationOnScreen(location);
        Rect bottom = new Rect(location[0] - allowOffsetX, location[1] - allowOffsetY,
                location[0] + bottomHandle.getMeasuredWidth() + allowOffsetX,
                location[1] + bottomHandle.getMeasuredHeight() + allowOffsetY);

        overlay.getLocationOnScreen(location);
        int x = location[0] + (int) event.getX();
        int y = location[1] + (int) event.getY();

        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                dragMode = DragMode.NONE;
                snapToFragment();
                return true;
            case MotionEvent.ACTION_DOWN:
                if (dragMode == DragMode.NONE) {
                    if (left.contains(x, y)) {
                        dragMode = DragMode.LEFT;
                    } else if (top.contains(x, y)) {
                        dragMode = DragMode.TOP;
                    } else if (right.contains(x, y)) {
                        dragMode = DragMode.RIGHT;
                    } else if (bottom.contains(x, y)) {
                        dragMode = DragMode.BOTTOM;
                    } else {
                        dragMode = DragMode.NONE;
                        post(new Runnable() {
                            @Override
                            public void run() {
                                fragment.finishResize();
                            }
                        });
                        return false;
                    }
                    leftHandle.setAlpha(dragMode == DragMode.LEFT ? 1.0f : DIMMED_HANDLE_ALPHA);
                    rightHandle.setAlpha(dragMode == DragMode.RIGHT ? 1.0f :DIMMED_HANDLE_ALPHA);
                    topHandle.setAlpha(dragMode == DragMode.TOP ? 1.0f : DIMMED_HANDLE_ALPHA);
                    bottomHandle.setAlpha(dragMode == DragMode.BOTTOM ? 1.0f : DIMMED_HANDLE_ALPHA);
                }
            case MotionEvent.ACTION_MOVE:
                if (dragMode != DragMode.NONE) {
                    dragResize(x, y);
                }
                return true;
        }
        Log.i(LOG_TAG, "Unhandled action " + event.getAction());
        snapToFragment();
        return true;
    }

    private void dragResize(int x, int y) {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        int[] location = new int[2];
        ((View) getParent()).getLocationOnScreen(location);
        int diff;
        int offset = getOffset();
        int minSize = 2 * offset;
        // Extra offset to make sure that the touch doesn't leave this frame while dragging
        switch (dragMode) {
            case LEFT:
                diff = x - lp.leftMargin - location[0] - offset;
                if (lp.width - diff > minSize) {
                    lp.leftMargin += diff;
                    lp.width -= diff;
                }
                break;
            case TOP:
                diff = y - lp.topMargin - location[1] - offset;
                if (lp.height - diff > minSize) {
                    lp.topMargin += diff;
                    lp.height -= diff;
                }
                break;
            case RIGHT:
                diff = x - lp.leftMargin - location[0] + offset - lp.width;
                if (lp.width + diff > minSize) {
                    lp.width += diff;
                }
                break;
            case BOTTOM:
                diff = y - lp.topMargin - location[1] + offset - lp.height;
                if (lp.height + diff > minSize) {
                    lp.height += diff;
                }
                break;
            default:
            case NONE:
                return;
        }
        requestLayout();
        resizeFragment();
    }

    private void resizeFragment() {
        RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) getLayoutParams();
        int offset = getOffset();
        View cView = Util.getPrimeContainer(fragment).getView();
        int width = Util.blockRound(cView, lp.width - 2 * offset, false);
        int height = Util.blockRound(cView, lp.height - 2 * offset, true);
        int diff;
        // Calculate measure difference
        switch (dragMode) {
            case LEFT:
            case RIGHT:
                diff = width - fragment.getPosWidth();
                break;
            case TOP:
            case BOTTOM:
                diff = height - fragment.getPosHeight();
                break;
            default:
                return;
        }
        // Resize
        switch (dragMode) {
            case LEFT:
                if (diff > 0) {
                    if (diff > fragment.getPosX()) {
                        // No negative x
                        diff = fragment.getPosX();
                    }
                } else if (diff < 0) {
                    if (width <= 0) {
                        // New width is 1
                        diff = 1 - fragment.getPosWidth();
                    }
                }
                if (diff != 0) {
                    fragment.setPosition(-diff, 0, true);
                    fragment.setPosWidth(fragment.getPosWidth() + diff);
                }
                break;
            case TOP:
                if (diff > 0) {
                    if (diff > fragment.getPosY()) {
                        // No negative y
                        diff = fragment.getPosY();
                    }
                } else if (diff < 0) {
                    if (height <= 0) {
                        // New height is 1
                        diff = 1 - fragment.getPosHeight();
                    }
                }
                if (diff != 0) {
                    fragment.setPosition(0, -diff, true);
                    fragment.setPosHeight(fragment.getPosHeight() + diff);
                }
                break;
            case RIGHT:
                if (diff < 0 && width <= 0) {
                    // New width is 1
                    diff = 1 - fragment.getPosWidth();
                }
                if (diff != 0) {
                    fragment.setPosWidth(fragment.getPosWidth() + diff);
                }
                break;
            case BOTTOM:
                if (diff < 0 && height <= 0) {
                    // New height is 1
                    diff = 1 - fragment.getPosHeight();
                }
                if (diff != 0) {
                    fragment.setPosHeight(fragment.getPosHeight() + diff);
                }
                break;
        }
    }

    private int getOffset() {
        return leftHandle.getDrawable().getIntrinsicWidth()/2 + handleMargin;
    }
}
