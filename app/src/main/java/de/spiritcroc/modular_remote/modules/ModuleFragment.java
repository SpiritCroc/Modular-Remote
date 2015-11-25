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
import android.app.Fragment;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.DragEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.RelativeLayout;

import java.util.Random;

import de.spiritcroc.modular_remote.DragManager;
import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.Util;

public abstract class ModuleFragment extends Fragment implements View.OnTouchListener,
        View.OnDragListener {
    protected static final String LOG_TAG = ModuleFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    protected Pos pos = new Pos();
    private boolean dragModeEnabled = false;

    /**
     * Whether a container should get dragged or scrolled when touched
     * Only relevant for ModuleFragments instanceof Container
     */
    private boolean containerDragEnabled = true;

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

    public abstract double getArgWidth();
    public abstract double getArgHeight();

    /**
     * Position attributes
     */
    protected static class Pos {
        private static String SEP = Util.RK_FRAGMENT_POS;
        protected int leftMargin, topMargin;
        protected Pos () {
            leftMargin = 0;
            topMargin = 0;
        }
        protected String getRecreationKey() {
            return fixRecreationKey(leftMargin + SEP + topMargin + SEP);
        }
        protected void recoverFromRecreationKey(String key) {
            try {
                String[] args = Util.split(key, SEP, 0);
                leftMargin = Integer.parseInt(args[0]);
                topMargin = Integer.parseInt(args[1]);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Pos: recoverFromRecreationKey: illegal key: " + key);
                e.printStackTrace();
            }
        }
    }

    protected void recoverPos(String posRecreationKey) {
        pos.recoverFromRecreationKey(posRecreationKey);
        updatePosition();
    }

    protected void setPosition(int leftMargin, int topMargin) {
        setPosition(leftMargin, topMargin, false);
    }
    /**
     * @param relative
     * false if absolute margin, true if changing margin for that value
     */
    protected void setPosition(int leftMargin, int topMargin, boolean relative) {
        if (relative) {
            pos.leftMargin += leftMargin;
            pos.topMargin += topMargin;
        } else {
            pos.leftMargin = leftMargin;
            pos.topMargin = topMargin;
        }
        updatePosition();
    }

    protected void updatePosition() {
        View view = getView();
        if (view == null) {
            if (DEBUG) Log.d(LOG_TAG, "updatePosition: view is null");
            return;
        }
        updatePosition(view);
    }
    protected void updatePosition(@NonNull View view) {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            View containerView = ((MainActivity) activity).getViewContainer();
            if (view.getLayoutParams() instanceof RelativeLayout.LayoutParams) {
                RelativeLayout.LayoutParams rlp =
                        (RelativeLayout.LayoutParams) view.getLayoutParams();
                rlp.leftMargin = pos.leftMargin*Util.getWidthFromBlockUnits(containerView, 1, true);
                rlp.topMargin = pos.topMargin*Util.getHeightFromBlockUnits(containerView, 1, true);
                ViewParent viewParent = view.getParent();
                if (viewParent != null) {
                    viewParent.requestLayout();
                }
            } else {
                Log.e(LOG_TAG, "setPosition: LayoutParams are " + view.getLayoutParams());
            }
        } else {
            Log.w(LOG_TAG, "Can't update position: !(activity instanceof MainActivity)");
        }
    }


    // Drag and Drop touch events

    /**
     * @param v
     * The view used for to view drags
     */
    protected void setDragView(View v) {
        v.setOnTouchListener(this);
        v.setOnDragListener(this);
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (dragModeEnabled && (!(this instanceof Container) || containerDragEnabled)) {
            if (DragManager.startDrag(this)) {
                if (DEBUG) Log.v(LOG_TAG, "start drag: " + getClass());
                v.invalidate();
                v.startDrag(null, new DragShadowBuilder(v), null, 0);
            }
            return true;
        } else {
            return false;
        }
    }
    @Override
    public boolean onDrag(View v, DragEvent event) {
        int action = event.getAction();

        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED:
            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_EXITED:
            case DragEvent.ACTION_DRAG_LOCATION:
                return true;// Ignore
            case DragEvent.ACTION_DROP:
                final ModuleFragment insertFragment = DragManager.stopDrag();
                if (insertFragment != null) {
                    View view = insertFragment.getView();
                    if (view != null) {
                        ViewGroup.LayoutParams lp = view.getLayoutParams();
                        if (lp instanceof RelativeLayout.LayoutParams) {
                            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) lp;
                            float dropX = event.getX();
                            float dropY = event.getY();
                            if (DEBUG) {
                                Log.d(LOG_TAG, "drop x " + dropX);
                                Log.d(LOG_TAG, "drop y " + dropY);
                            }
                            ModuleFragment primeContainer = this;
                            while (primeContainer.getParent() != null &&
                                    primeContainer.getParent() instanceof ModuleFragment) {
                                primeContainer = (ModuleFragment) primeContainer.getParent();
                            }
                            View cView = primeContainer.getView();
                            boolean dropOnItself = false;
                            if (this == insertFragment) {
                                dropOnItself = true;
                            } else {
                                Container c = getParent();
                                while (c instanceof ModuleFragment) {
                                    if (c == insertFragment) {
                                        dropOnItself = true;
                                        break;
                                    } else {
                                        c = ((ModuleFragment) c).getParent();
                                    }
                                }
                            }
                            // Add scroll values if necessary
                            Container c = insertFragment != this && this instanceof Container ?
                                    (Container) this : getParent();
                            while (c instanceof ModuleFragment) {
                                if (c.scrollsX()) {
                                    dropX += c.getScrollX();
                                }
                                if (c.scrollsY()) {
                                    dropY += c.getScrollY();
                                }
                                c = ((ModuleFragment) c).getParent();
                            }
                            int newX = Util.blockRound(cView, dropX - rlp.width / 2, false);
                            int newY = Util.blockRound(cView, dropY - rlp.height / 2, true);
                            if (dropOnItself) {
                                insertFragment.setPosition(newX, newY, true);
                            } else {
                                if (this instanceof Container){
                                    insertFragment.setPosition(newX, newY);
                                } else {
                                    // Overlapping fragments might be helpful while editing
                                    insertFragment.setPosition(
                                            pos.leftMargin + newX, pos.topMargin + newY);
                                }
                                if (insertFragment.getParent() != this) {
                                    // Move fragment from old to this container
                                    final Container container = this instanceof Container ?
                                            (Container) this : getParent();
                                    if (container == insertFragment) {
                                        Log.w(LOG_TAG, "Can't add fragment to itself");
                                    } else {
                                        Container oldC = insertFragment.getParent();
                                        oldC.removeFragment(insertFragment, false);
                                        oldC.onContentMoved();
                                        final int oldDepth = insertFragment instanceof Container ?
                                                ((Container) insertFragment).getDepth() : -1;
                                        v.post(new Runnable() {
                                            @Override
                                            public void run() {
                                                container.addFragment(insertFragment);
                                                if (insertFragment instanceof Container) {
                                                    ((Container) insertFragment)
                                                            .updateDepth(oldDepth);
                                                }
                                            }
                                        });
                                    }
                                }
                            }
                            insertFragment.getParent().onContentMoved();
                            return true;
                        } else {
                            new Exception("Drop operation failed; layout is " + lp)
                                    .printStackTrace();
                        }
                    } else {
                        new Exception("Drop operation failed; view is null").printStackTrace();
                    }
                } else {
                    new Exception("Drop operation failed; fragment is null").printStackTrace();
                }
                return true;
            case DragEvent.ACTION_DRAG_ENDED:
                DragManager.stopDrag();
                return true;
            default:
                Log.e(LOG_TAG, "Unknown action type received by OnDragListener.");
                return false;

        }
    }
    public void onStartDragMode() {
        dragModeEnabled = true;
        containerDragEnabled = true;
        if (getActivity() != null && getView() != null) {
            getView().setBackgroundColor(getDragModeBgColor());
        }
    }
    public void onStopDragMode() {
        dragModeEnabled = false;
        if (getActivity() != null && getView() != null) {
            getView().setBackgroundColor(Color.TRANSPARENT);
        }
    }
    public void onStartDrag(){
        setAlpha(0.2f);
    }
    public void onStopDrag(){
        setAlpha(1);
    }
    private void setAlpha(float alpha) {
        View view = getView();
        if (view != null) {
            view.setAlpha(alpha);
        }
    }
    public boolean isDragModeEnabled() {
        return dragModeEnabled;
    }
    public boolean isContainerDragEnabled() {
        return containerDragEnabled;
    }
    public void setContainerDragEnabled(boolean containerDragEnabled) {
        this.containerDragEnabled = containerDragEnabled;
    }
    protected int getDragModeBgColor() {
        Random random = new Random();
        int r, g, b;
        do {
            r = random.nextInt(256);
            g = random.nextInt(256);
            b = random.nextInt(256);
            // Color should not be too dark
        } while (r < 128 && g < 128 && b < 128);
        return Color.argb(127, r, g, b);
    }

    /**
     * Call this in onCreateView()
     */
    protected void maybeStartDrag(View v) {
        if (dragModeEnabled) {
            v.post(new Runnable() {
                @Override
                public void run() {
                    onStartDragMode();
                }
            });
        }
    }

    // For shorter code:
    protected final static String SEP = Util.RK_ATTRIBUTE_SEPARATOR;
    protected static String fixRecreationKey(String key) {
        return Util.fixRecreationKey(key, SEP);
    }

    private class DragShadowBuilder extends View.DragShadowBuilder {
        View view;

        public DragShadowBuilder(View view) {
            super(view);
            this.view = view;
        }
        @Override
        public void onDrawShadow(Canvas canvas) {
            canvas.drawRGB(0, 0, 0);//todo better shadow
            super.onDrawShadow(canvas);
        }
    }
}
