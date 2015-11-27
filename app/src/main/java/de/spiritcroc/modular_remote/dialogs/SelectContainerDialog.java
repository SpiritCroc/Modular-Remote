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

package de.spiritcroc.modular_remote.dialogs;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.Spannable;
import android.widget.Toast;

import java.util.ArrayList;

import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.ModuleFragment;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;

public class SelectContainerDialog extends CustomDialogFragment {
    public enum Mode {ADD_FRAGMENT, MOVE_FRAGMENT, COPY_FRAGMENT, MOVE_FRAGMENTS, COPY_FRAGMENTS}

    private PageContainerFragment page;
    private ModuleFragment addFragment;
    private ArrayList<ModuleFragment> addFragments;
    private boolean addWidget = false;
    private double addWidgetWidth, addWidgetHeight;
    private int selection = 0;
    private Container[] containers;
    private Activity activity;
    private Mode mode = Mode.ADD_FRAGMENT;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        if ((mode == Mode.MOVE_FRAGMENT || mode == Mode.COPY_FRAGMENT ||
                mode == Mode.MOVE_FRAGMENTS || mode == Mode.COPY_FRAGMENTS) &&
                getActivity() instanceof MainActivity) {
            containers = ((MainActivity) getActivity())
                    .getAllContainers();
        } else {
            containers = page.getAllContainers();
        }
        if (containers.length == 1) {// No selection needed if only one container available
            addElementToContainer(0);
            dismiss();
            return builder.create();
        }
        Spannable[] containerList = new Spannable[containers.length];
        for (int i = 0; i < containers.length; i++) {
            int depth = containers[i].getDepth()-1;
            String indention = "";
            for (int j = 0; j < depth; j++) {
                indention += " - ";
            }
            containerList[i] = containers[i].getContentReadableName(indention);
        }

        return builder.setTitle(R.string.dialog_select_container)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        addElementToContainer(selection);
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //only close dialog
                    }
                }).setSingleChoiceItems(containerList, selection,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                selection = which;
                            }
                        }).create();
    }
    private void addElementToContainer(int index){
        switch (mode){
            case ADD_FRAGMENT:
            case COPY_FRAGMENT:
                if (addWidget) {
                    Util.addWidgetToContainer(getActivity(), addWidgetWidth, addWidgetHeight,
                            page, containers[index], null);
                } else {
                    Util.addFragmentToContainer(activity, addFragment, page, containers[index]);
                }
                break;
            case MOVE_FRAGMENT:
                addFragment.getParent().removeFragment(addFragment, false);
                containers[index].addFragment(addFragment);
                break;
            case COPY_FRAGMENTS:
                for (int i = 0; i < addFragments.size(); i++) {
                    ModuleFragment addFragment = addFragments.get(i);
                    Util.addFragmentToContainer(activity, addFragment, page, containers[index]);
                }
                break;
            case MOVE_FRAGMENTS:
                for (int i = 0; i < addFragments.size(); i++) {
                    ModuleFragment addFragment = addFragments.get(i);
                    addFragment.getParent().removeFragment(addFragment, false);
                    containers[index].addFragment(addFragment);
                }
                break;
        }
    }

    public SelectContainerDialog setValues(PageContainerFragment page, ModuleFragment addFragment) {
        this.page = page;
        this.addFragment = addFragment;
        return this;
    }
    public SelectContainerDialog setValues(PageContainerFragment page,
                                           ArrayList<ModuleFragment> addFragments) {
        this.page = page;
        this.addFragments = addFragments;
        return this;
    }
    public SelectContainerDialog addWidget(PageContainerFragment page, double width,
                                           double height) {
        this.page = page;
        addWidget = true;
        addWidgetWidth = width;
        addWidgetHeight = height;
        return this;
    }

    public SelectContainerDialog setMode(Mode mode) {
        this.mode = mode;
        return this;
    }

    @Override
    public void onDismiss(DialogInterface dialogInterface) {
        super.onDismiss(dialogInterface);
    }
    @Override
    public void onDestroy() {
        super.onDestroy();
    }
}
