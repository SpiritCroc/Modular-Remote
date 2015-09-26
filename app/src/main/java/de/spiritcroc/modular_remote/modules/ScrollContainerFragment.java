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
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Arrays;

import de.spiritcroc.modular_remote.CustomScrollView;
import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.Util;

public class ScrollContainerFragment extends ModuleFragment implements Container {
    private static final String ARG_WIDTH = "width";
    private static final String ARG_HEIGHT = "height";
    private static final String LOG_TAG = ScrollContainerFragment.class.getSimpleName();

    private double width, height;// If height == -1, then match_parent
    private CustomScrollView scrollView;
    private LinearLayout baseLayout, containerLayout;
    private String recreationKey;
    private ArrayList<ModuleFragment> fragments = new ArrayList<>();;
    private Container parent;
    private boolean menuEnabled = false;

    public static ScrollContainerFragment newInstance(double width, double height) {
        ScrollContainerFragment fragment = new ScrollContainerFragment();
        Bundle args = new Bundle();
        args.putDouble(ARG_WIDTH, width);
        args.putDouble(ARG_HEIGHT, height);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getArguments() != null) {
            width = getArguments().getDouble(ARG_WIDTH);
            height = getArguments().getDouble(ARG_HEIGHT);
        } else {
            Log.e(LOG_TAG, "onCreate: getArguments()==null");
            width = height = 1;
        }
    }
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_scroll_container, container, false);

        scrollView = (CustomScrollView) view.findViewById(R.id.scroll_view);
        scrollView.setWrapFragment(this);
        baseLayout = (LinearLayout) view.findViewById(R.id.base_layout);
        containerLayout = (LinearLayout) view.findViewById(R.id.container);
        containerLayout.setId(Util.generateViewId());
        setValues(width, height);

        fragments.clear();
        restoreContentFromRecreationKey();

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
        String key = SCROLL_CONTAINER_FRAGMENT +SEP + width + SEP + height + SEP + separator;
        for (int i = 0; i < fragments.size(); i++) {
            ModuleFragment fragment = fragments.get(i);
            key += fragment.getRecreationKey() + separator;
        }
        return fixRecreationKey(key);
    }
    public static ScrollContainerFragment recoverFromRecreationKey(String key) {
        try {
            String[] args = Util.split(key, SEP, 0);
            double width = Double.parseDouble(args[1]);
            double height = Double.parseDouble(args[2]);
            ScrollContainerFragment fragment = newInstance(width, height);
            fragment.setRecreationKey(key);
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
    private void restoreContentFromRecreationKey() {
        if (recreationKey != null) {
            Util.restoreContentFromRecreationKey(this, recreationKey, menuEnabled);
        }
    }
    @Override
    public ModuleFragment copy() {
        ScrollContainerFragment fragment = newInstance(width, height);
        fragment.setRecreationKey(getRecreationKey());
        return fragment;
    }
    @Override
    public Container getParent() {
        return parent;
    }
    @Override
    public void setParent(Container parent) {
        this.parent = parent;
    }
    @Override
    public int getDepth() {
        return parent.getDepth() + 1;
    }

    @Override
    public void addFragment(ModuleFragment fragment) {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            fragmentManager.beginTransaction().add(containerLayout.getId(), fragment).commit();
            fragments.add(fragment);
            fragment.setMenuEnabled(menuEnabled);
            fragment.setParent(this);
        } else {
            Log.e(LOG_TAG, "Can't add " + fragment);
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

    public double getArgWidth() {
        return width;
    }
    public double getArgHeight() {
        return height;
    }
    public void setValues(double width, double height) {
        if (width <= 0) {
            width = 1;
        }
        if (height != -1 && height <= 0) {//if height == -1 set to match parent
            height = 1;
        }
        this.width = width;
        this.height = height;
        resize(false);
    }
    @Override
    public void resize() {
        resize(true);
    }
    private void resize(boolean resizeContent) {
        Activity activity = getActivity();
        if (activity instanceof MainActivity) {
            View containerView = ((MainActivity) activity).getViewContainer();
            Util.resizeLayoutWidth(containerView, baseLayout, width);
            Util.resizeLayoutHeight(containerView, baseLayout, height);
        } else {
            Log.w(LOG_TAG, "Can't resize: !(activity instanceof MainActivity)");
        }
        if (resizeContent) {
            for (ModuleFragment fragment : fragments) {
                fragment.resize();
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
    public boolean isConnected() {
        for (int i = 0; i < fragments.size(); i++) {
            if (fragments.get(i).isConnected()) {
                return true;
            }
        }
        return false;
    }
}
