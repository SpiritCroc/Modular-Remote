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
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewParent;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import java.util.Random;

import de.spiritcroc.modular_remote.DragManager;
import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.Preferences;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.ResizeFrame;
import de.spiritcroc.modular_remote.TouchOverlay;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.dialogs.AddFragmentDialog;
import de.spiritcroc.modular_remote.dialogs.RemoveContainerDialog;
import de.spiritcroc.modular_remote.dialogs.SelectContainerDialog;

public abstract class ModuleFragment extends Fragment implements View.OnTouchListener,
        View.OnDragListener {
    protected static final String LOG_TAG = ModuleFragment.class.getSimpleName();
    private static final boolean DEBUG = false;

    protected Container parent;
    protected Pos pos = new Pos();
    private boolean dragModeEnabled = false;

    private ResizeFrame resizeFrame;
    private TouchOverlay resizeOverlay;

    /**
     * Whether a container should get dragged or scrolled when touched
     * Only relevant for ModuleFragments instanceof Container
     */
    private boolean containerDragEnabled = true;

    public abstract String getReadableName();
    public abstract String getRecreationKey();
    public abstract void setMenuEnabled(boolean menuEnabled);

    /**
     * If module needs updating when re-adding with new depth, old depth is stored in this variable
     * When updating is done/no updating required, set to -1
     */
    protected int oldDepth = -1;

    /**
     * Some fragments (e.g. WidgetContainerFragment) might need to free some space on removal
     */
    public void onRemove() {}
    final public Container getParent() {
        return parent;
    }
    final public void setParent(Container parent) {
        this.parent = parent;
    }

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
    public final static String COMMAND_LINE_FRAGMENT = "command_line_fragment";

    protected final static int MENU_ORDER = 50;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(getActivity());
        pos.init(sharedPreferences);
    }

    /**
     * Resize if block unit size changed
     * @param updateContent
     * Whether containers should call the same method for their contained fragments
     */
    public void resize (boolean updateContent) {
        View v = getView();
        if (v != null) {
            resize(v);
        }
    }

    protected void resize(@NonNull View v) {
        updatePosition();
        if (v instanceof LinearLayout) {
            Activity activity = getActivity();
            if (activity instanceof MainActivity) {
                View containerView = ((MainActivity) activity).getViewContainer();
                Util.resizeLayoutWidth(containerView, (LinearLayout) v, pos.width);
                Util.resizeLayoutHeight(containerView, (LinearLayout) v, pos.height);
            } else {
                Log.w(LOG_TAG, "Can't resize: !(activity instanceof MainActivity)");
            }
        } else {
            Log.e(LOG_TAG, "Can't resize: !(getView() instanceof LinearLayout)");
        }
    }

    /**
     * Position attributes
     */
    protected static class Pos {
        private static String SEP = Util.RK_FRAGMENT_POS;
        protected int leftMargin, topMargin;
        protected int width, height;
        protected Pos () {
            leftMargin = 0;
            topMargin = 0;
            width = -1;
            height = -1;
        }
        protected void init(SharedPreferences sharedPreferences) {
            if (width <= 0) {
                width = Util.getPreferenceInt(sharedPreferences,
                        Preferences.FRAGMENT_DEFAULT_WIDTH, 3);
            }
            if (height <= 0) {
                height = Util.getPreferenceInt(sharedPreferences,
                        Preferences.FRAGMENT_DEFAULT_HEIGHT, 2);
            }
        }
        protected String getRecreationKey() {
            return fixRecreationKey(leftMargin + SEP + topMargin + SEP +
                    width + SEP + height + SEP);
        }
        protected void recoverFromRecreationKey(String key) {
            try {
                String[] args = Util.split(key, SEP, 0);
                leftMargin = Integer.parseInt(args[0]);
                topMargin = Integer.parseInt(args[1]);
                width = Integer.parseInt(args[2]);
                height = Integer.parseInt(args[3]);
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

    public void resetPosition() {
        setPosition(0, 0);
    }
    public void setPosition(int leftMargin, int topMargin) {
        setPosition(leftMargin, topMargin, false);
    }
    /**
     * @param relative
     * false if absolute margin, true if changing margin for that value
     */
    public void setPosition(int leftMargin, int topMargin, boolean relative) {
        if (relative) {
            pos.leftMargin += leftMargin;
            pos.topMargin += topMargin;
        } else {
            pos.leftMargin = leftMargin;
            pos.topMargin = topMargin;
        }
        updatePosition();
    }

    private void updatePosition() {
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

    public void setPosWidth(int width) {
        pos.width = width;
        resize(false);
    }
    public void setPosHeight(int height) {
        pos.height = height;
        resize(false);
    }
    public void setPosMeasures(int width, int height) {
        pos.width = width;
        pos.height = height;
        resize(false);
    }

    public int getPosX() {
        return pos.leftMargin;
    }
    public int getPosY() {
        return pos.topMargin;
    }
    public int getPosWidth() {
        return pos.width;
    }
    public int getPosHeight() {
        return pos.height;
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
    private Handler longPressHandler = new Handler();
    private Runnable longPress = new Runnable() {
        @Override
        public void run() {
            if (DragManager.isLongPressPossible()) {
                if (DEBUG) Log.d(LOG_TAG, "longPress");
                DragManager.stopDrag();
                cancelLongPress();
                showEditMenu();
            }
        }
    };
    private void cancelLongPress() {
        longPressHandler.removeCallbacks(longPress);
        DragManager.cancelLongPress();
    }
    private void showEditMenu() {
        PopupMenu popupMenu = new PopupMenu(getActivity(), getView());
        popupMenu.setOnMenuItemClickListener(editMenuItemClickListener);
        prepareEditMenu(popupMenu.getMenu());
        popupMenu.show();
    }
    protected void prepareEditMenu(Menu menu) {
        getActivity().getMenuInflater().inflate(R.menu.menu_fragment_edit, menu);
        menu.setGroupVisible(R.id.action_container, this instanceof Container);
        if (getActivity() instanceof MainActivity) {
            // Hide move action if only one container available
            int containerCount = ((MainActivity) getActivity()).getAllContainers().length;
            menu.findItem(R.id.action_move).setVisible(containerCount > 2 ||
                    (!(this instanceof Container) && containerCount > 1));
        }
    }
    private PopupMenu.OnMenuItemClickListener editMenuItemClickListener =
            new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    switch (item.getItemId()) {
                        case R.id.action_edit:
                            editActionEdit();
                            return true;
                        case R.id.action_resize:
                            editActionResize();
                            return true;
                        case R.id.action_move:
                            editActionMove();
                            return true;
                        case R.id.action_clone:
                            editActionClone();
                            return true;
                        case R.id.action_remove:
                            editActionRemove();
                            return true;
                        case R.id.action_add_fragment:
                            editActionAddFragment();
                            return true;
                        default:
                            return false;
                    }
                }
            };
    protected abstract void editActionEdit();
    private void editActionResize() {
        if (resizeFrame == null) {
            resizeFrame = new ResizeFrame(getActivity(), this);
            getParent().addResizeFrame(resizeFrame);
            resizeFrame.snapToFragment();
            ModuleFragment c = Util.getPrimeContainer(this);
            if (c instanceof PageContainerFragment) {
                resizeOverlay = new TouchOverlay(getActivity(), resizeFrame);
                ((PageContainerFragment) c).addResizeOverlay(resizeOverlay);
            } else {
                Log.w(LOG_TAG, "Could not add resizeOverlay");
            }
        }
    }
    public void finishResize() {
        if (resizeFrame != null) {
            getParent().removeResizeFrame(resizeFrame);
            resizeFrame = null;
            ModuleFragment c = Util.getPrimeContainer(this);
            if (c instanceof PageContainerFragment) {
                ((PageContainerFragment) c).removeResizeOverlay(resizeOverlay);
                resizeOverlay = null;
            } else {
                Log.w(LOG_TAG, "Could not remove resizeOverlay");
            }
        }
    }
    protected void editActionMove() {
        new SelectContainerDialog().setValues(Util.getPage(this), this)
                .setMode(SelectContainerDialog.Mode.MOVE_FRAGMENT)
                .show(getFragmentManager(), "SelectContainerDialog");
    }
    protected void editActionClone() {
        new SelectContainerDialog().setValues(Util.getPage(this), copy())
                .setMode(SelectContainerDialog.Mode.COPY_FRAGMENT)
                .show(getFragmentManager(), "SelectContainerDialog");
    }
    protected void editActionRemove() {
        if (this instanceof Container && ((Container) this).getAllFragments().length > 1) {
            new RemoveContainerDialog().setFragment(this)
                    .show(getFragmentManager(), "RemoveContainerDialog");
        } else {
            getParent().removeFragment(this, true);
        }
    }
    protected void editActionAddFragment() {
        if (this instanceof Container) {
            new AddFragmentDialog().setPage(Util.getPage(this))
                    .setContainer((Container) this)
                    .show(getFragmentManager(), "AddFragmentDialog");
        } else {
            Log.w(LOG_TAG, "editActionAddFragment called by non-container class " +
                    this.getClass().getSimpleName());
        }
    }
    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (dragModeEnabled && (!(this instanceof Container) || containerDragEnabled)) {
                    if (DragManager.startDrag(this)) {
                        finishResize();
                        if (DEBUG) Log.v(LOG_TAG, "start drag: " + getClass());
                        v.invalidate();
                        v.startDrag(null, new DragShadowBuilder(v), null, 0);
                        // Detect long presses
                        int[] location = new int[2];
                        v.getLocationOnScreen(location);
                        DragManager.setLongPressPos(location[0] + event.getX(),
                                location[1] + event.getY());
                        longPressHandler.postDelayed(longPress,
                                ViewConfiguration.getLongPressTimeout());
                    }
                    return true;
                }
                break;
            case MotionEvent.ACTION_UP:
                cancelLongPress();
                break;
        }
        return false;
    }
    @Override
    public boolean onDrag(View v, DragEvent event) {
        int action = event.getAction();

        switch (action) {
            case DragEvent.ACTION_DRAG_STARTED:
            case DragEvent.ACTION_DRAG_ENTERED:
            case DragEvent.ACTION_DRAG_EXITED:
                return true;// Ignore
            case DragEvent.ACTION_DRAG_LOCATION:
                // Abort long press if moved for more then one block
                if (DragManager.isLongPressPossible()) {
                    int[] location = new int[2];
                    v.getLocationOnScreen(location);
                    int diffX = (int) (location[0] + event.getX() - DragManager.getDragStartX());
                    int diffY = (int) (location[1] + event.getY() - DragManager.getDragStartY());
                    if (diffX < 0)
                        diffX *= -1;
                    if (diffY < 0)
                        diffY *= -1;
                    View primeContainer = Util.getPrimeContainer(this).getView();
                    if (Util.blockRound(primeContainer, diffX, false) != 0 ||
                            Util.blockRound(primeContainer, diffY, true) != 0) {
                        if (DEBUG) Log.d(LOG_TAG, "Drag module, cancel longPress");
                        cancelLongPress();
                    }
                }
                return true;
            case DragEvent.ACTION_DROP:
                final ModuleFragment insertFragment = DragManager.stopDrag();
                if (insertFragment != null) {
                    View view = insertFragment.getView();
                    if (view != null) {
                        float dropX = event.getX();
                        float dropY = event.getY();
                        if (DEBUG) {
                            Log.d(LOG_TAG, "drop x " + dropX);
                            Log.d(LOG_TAG, "drop y " + dropY);
                        }
                        View cView = Util.getPrimeContainer(this).getView();
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
                        int newX =
                                Util.blockRound(cView, dropX - view.getMeasuredWidth() / 2, false);
                        int newY =
                                Util.blockRound(cView, dropY - view.getMeasuredHeight() / 2, true);
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
                                    insertFragment.prepareDepthChange();
                                    container.addFragment(insertFragment, true);
                                }
                            }
                        }
                        insertFragment.getParent().onContentMoved();
                        return true;
                    } else {
                        new Exception("Drop operation failed; view is null").printStackTrace();
                    }
                } else {
                    if (DEBUG) Log.d(LOG_TAG, "Drop operation failed; fragment is null");
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
    public void prepareDepthChange() {
        // Depth can't change if there is no parent yet
        if (parent != null) {
            oldDepth = (this instanceof Container ? ((Container) this).getDepth() :
                    getParent().getDepth() + 1);
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
        finishResize();
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
