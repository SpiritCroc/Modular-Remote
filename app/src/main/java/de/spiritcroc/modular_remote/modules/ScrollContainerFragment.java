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

import android.app.FragmentManager;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import java.util.ArrayList;
import java.util.Arrays;

import de.spiritcroc.modular_remote.CustomScrollView;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.ResizeFrame;
import de.spiritcroc.modular_remote.Util;

public class ScrollContainerFragment extends ModuleFragment implements Container {
    private static final String LOG_TAG = ScrollContainerFragment.class.getSimpleName();

    private CustomScrollView scrollView;
    private RelativeLayout containerLayout;
    private String recreationKey;
    private ArrayList<ModuleFragment> fragments = new ArrayList<>();
    private boolean menuEnabled = false;

    public static ScrollContainerFragment newInstance() {
        ScrollContainerFragment fragment = new ScrollContainerFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scroll_container, container, false);

        scrollView = (CustomScrollView) view.findViewById(R.id.scroll_view);
        scrollView.setOnDragListener(this);
        scrollView.setWrapFragment(this);
        containerLayout = (RelativeLayout) view.findViewById(R.id.container);
        setDragView(scrollView);
        containerLayout.setId(Util.generateViewId());
        updatePosition(view);
        resize(view);

        fragments.clear();
        restoreContentFromRecreationKey();

        maybeStartDrag(view);

        return view;
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        recreationKey = getRecreationKey();
    }

    @Override
    public void setMenuEnabled(boolean menuEnabled) {
        this.menuEnabled = menuEnabled;
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).setMenuEnabled(menuEnabled);
        }
    }
    @Override
    public void onRemove() {
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).onRemove();
        }
    }

    @Override
    public String getReadableName() {
        return Util.getACString(R.string.fragment_scroll_container);
    }
    @Override
    public Spannable getContentReadableName(@Nullable String prefix) {
        return Util.getContainerContentReadableName(this, prefix);
    }
    @Override
    public String getRecreationKey() {
        String separator = Util.getSeparator(this);
        String key = SCROLL_CONTAINER_FRAGMENT + SEP + pos.getRecreationKey() + SEP + separator;
        for (int i = 0; i < fragments.size(); i++) {
            ModuleFragment fragment = fragments.get(i);
            key += fragment.getRecreationKey() + separator;
        }
        return fixRecreationKey(key);
    }
    public static ScrollContainerFragment recoverFromRecreationKey(String key) {
        try {
            String[] args = Util.split(key, SEP, 0);
            ScrollContainerFragment fragment = newInstance();
            fragment.setRecreationKey(key);
            fragment.recoverPos(args[1]);
            return fragment;
        } catch (Exception e) {
            Log.e(LOG_TAG, "recoverFromRecreationKey: illegal key: " + key);
            Log.e(LOG_TAG, "Got exception: " + e);
            return null;
        }
    }
    private void setRecreationKey(String recreationKey) {
        this.recreationKey = recreationKey;
    }
    private void updateDepth() {
        if (oldDepth != -1) {
            recreationKey = Util.updateRecreationKey(recreationKey, oldDepth, getDepth());
            oldDepth = -1;
        }
    }
    private void restoreContentFromRecreationKey() {
        if (recreationKey != null) {
            updateDepth();
            Util.restoreContentFromRecreationKey(this, recreationKey, menuEnabled);
        }
    }
    @Override
    public ModuleFragment copy() {
        ScrollContainerFragment fragment = newInstance();
        fragment.setRecreationKey(getRecreationKey());
        fragment.setPosMeasures(pos.width, pos.height);
        return fragment;
    }
    @Override
    public int getDepth() {
        return parent.getDepth() + 1;
    }

    @Override
    public void addFragment(final ModuleFragment fragment, boolean post) {
        if (post) {
            containerLayout.post(new AddFragment(fragment));
        } else {
            new AddFragment(fragment).run();
        }
    }
    private class AddFragment implements Runnable {
        private ModuleFragment fragment;
        private AddFragment(ModuleFragment fragment){
            this.fragment = fragment;
        }
        @Override
        public void run() {
            FragmentManager fragmentManager = getFragmentManager();
            if (fragmentManager != null) {
                fragmentManager.beginTransaction().add(containerLayout.getId(), fragment)
                        .commit();
                fragments.add(fragment);
                fragment.setMenuEnabled(menuEnabled);
                fragment.setParent(ScrollContainerFragment.this);
                if (isDragModeEnabled()) {
                    fragment.onStartDragMode();
                }
                fragment.setContainerDragEnabled(isContainerDragEnabled());
            } else {
                Log.e(LOG_TAG, "Can't add " + fragment);
            }
        }
    }
    @Override
    public void removeFragment(ModuleFragment fragment, boolean callOnRemove) {
        fragments.remove(fragment);
        if (callOnRemove) {
            fragment.onRemove();
        }
        FragmentManager fragmentManager = getFragmentManager();
        fragmentManager.beginTransaction().remove(fragment).commit();
    }

    @Override
    public void resize(boolean resizeContent) {
        super.resize(resizeContent);
        if (resizeContent) {
            for (ModuleFragment fragment : fragments) {
                fragment.resize(true);
            }
        }
    }

    @Override
    public Container[] getAllContainers() {
        return Util.getAllContainers(this);
    }
    @Override
    public ModuleFragment[] getAllFragments() {
        ArrayList<ModuleFragment> list = new ArrayList<>();
        list.add(this);
        for (int i = 0; i < fragments.size(); i++) {
            if (fragments.get(i) instanceof Container) {
                ModuleFragment[] children = ((Container) fragments.get(i)).getAllFragments();
                list.addAll(Arrays.asList(children));
            } else {
                list.add(fragments.get(i));
            }
        }
        return list.toArray(new ModuleFragment[list.size()]);
    }
    @Override
    public ModuleFragment[] getFragments() {
        return fragments.toArray(new ModuleFragment[fragments.size()]);
    }
    @Override
    public boolean isEmpty() {
        return fragments.isEmpty();
    }
    @Override
    public int getFragmentCount() {
        return fragments.size();
    }

    public boolean hasChildWithScrollY() {
        for (int i = 0; i < fragments.size(); i++) {
            if (fragments.get(i) instanceof Container &&
                    ((Container) fragments.get(i)).scrollsY()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean scrollsX() {
        return false;
    }
    @Override
    public boolean scrollsY() {
        return scrollView.canScroll();
    }
    @Override
    public int getScrollX() {
        return 0;
    }
    @Override
    public int getScrollY() {
        return scrollView.getScrollY();
    }

    @Override
    public void onStartDragMode() {
        super.onStartDragMode();
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).onStartDragMode();
        }
        requestContentSize();
    }
    @Override
    public void onStopDragMode() {
        super.onStopDragMode();
        scrollView.setBackgroundColor(Color.TRANSPARENT);
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).onStopDragMode();
        }
        requestContentSize();
    }
    @Override
    public void setContainerDragEnabled(boolean containerDragEnabled) {
        super.setContainerDragEnabled(containerDragEnabled);
        for (int i = 0; i < fragments.size(); i++) {
            fragments.get(i).setContainerDragEnabled(containerDragEnabled);
        }
    }

    /**
     * Set content size to wrap content, and more if drag mode is enabled to provide more space
     */
    private void requestContentSize() {
        if (containerLayout == null) {
            return;
        }
        final int scrollY = getScrollY();
        containerLayout.getLayoutParams().height = ViewGroup.LayoutParams.WRAP_CONTENT;
        containerLayout.requestLayout();
        if (isDragModeEnabled()) {
            containerLayout.post(new Runnable() {
                @Override
                public void run() {
                    containerLayout.getLayoutParams().height = containerLayout.getMeasuredHeight() +
                            scrollView.getMeasuredHeight()*2;
                    containerLayout.requestLayout();
                    // Scroll to previous position
                    containerLayout.post(new Runnable() {
                        @Override
                        public void run() {
                            scrollView.scrollTo(0, scrollY);
                        }
                    });
                }
            });
        }
    }
    public void onContentMoved() {
        requestContentSize();
    }

    @Override
    public boolean isConnected() {
        for (int i = 0; i < fragments.size(); i++) {
            if (fragments.get(i).isConnected()) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected void prepareEditMenu(Menu menu) {
        super.prepareEditMenu(menu);
        menu.findItem(R.id.action_edit).setVisible(false);
    }

    @Override
    protected void editActionEdit() {
        // No settings
    }

    @Override
    public void addResizeFrame(ResizeFrame resizeFrame) {
        containerLayout.addView(resizeFrame);
    }
    @Override
    public void removeResizeFrame(ResizeFrame resizeFrame) {
        containerLayout.removeView(resizeFrame);
    }
}
