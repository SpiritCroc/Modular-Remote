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

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import java.util.ArrayList;
import java.util.Arrays;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.HorizontalContainerFragment;
import de.spiritcroc.modular_remote.modules.ModuleFragment;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;
import de.spiritcroc.modular_remote.modules.ScrollContainerFragment;

public class AddFragmentDialog extends DialogFragment {
    private static final String LOG_TAG = AddFragmentDialog.class.getSimpleName();

    private int selection = 0;
    private PageContainerFragment page;
    private Container container;
    private String[] fragmentValues;
    private CustomDialogFragment.OnDismissListener updateListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final FragmentActivity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        Resources resources = getResources();
        String[] fragments = resources.getStringArray(R.array.fragment_selection_array);
        fragmentValues = resources.getStringArray(R.array.fragment_selection_values_array);

        if (container != null) {
            ArrayList<String> fragmentValueList = new ArrayList<>();
            fragmentValueList.addAll(Arrays.asList(fragmentValues));
            int pageIndex = fragmentValueList.indexOf(ModuleFragment.PAGE_CONTAINER_FRAGMENT);
            if (pageIndex >= 0) {
                // You can't add pages to containers
                ArrayList<String> fragmentList = new ArrayList<>();
                fragmentList.addAll(Arrays.asList(fragments));
                fragmentList.remove(pageIndex);
                fragmentValueList.remove(pageIndex);
                fragments = fragmentList.toArray(new String[fragmentList.size()]);
                fragmentValues = fragmentValueList.toArray(new String[fragmentValueList.size()]);
            }
        }

        return builder.setTitle(R.string.dialog_add_fragment)
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        switch (fragmentValues[selection]) {
                            case ModuleFragment.PAGE_CONTAINER_FRAGMENT:
                                new AddPageDialog().show(getFragmentManager(), "AddPageDialog");
                                break;
                            case ModuleFragment.BUTTON_FRAGMENT:
                                new AddDisplayFragmentDialog().setPage(page).setButtonMode(true)
                                        .setContainer(container)
                                        .setOnDismissListener(updateListener)
                                        .show(getFragmentManager(), "AddButtonFragmentDialog");
                                break;
                            case ModuleFragment.TOGGLE_FRAGMENT:
                                new AddToggleFragmentDialog().setPage(page).setContainer(container)
                                        .setOnDismissListener(updateListener)
                                        .show(getFragmentManager(), "AddToggleFragmentDialog");
                                break;
                            case ModuleFragment.SPINNER_FRAGMENT:
                                new AddSpinnerFragmentDialog().setPage(page).setContainer(container)
                                        .setOnDismissListener(updateListener)
                                        .show(getFragmentManager(), "AddSpinnerFragmentDialog");
                                break;
                            case ModuleFragment.DISPLAY_FRAGMENT:
                                new AddDisplayFragmentDialog().setPage(page)
                                        .setContainer(container)
                                        .setOnDismissListener(updateListener)
                                        .show(getFragmentManager(), "AddDisplayFragmentDialog");
                                break;
                            case ModuleFragment.COMMAND_LINE_FRAGMENT:
                                new AddCommandLineFragmentDialog().setPage(page)
                                        .setContainer(container)
                                        .setOnDismissListener(updateListener)
                                        .show(getFragmentManager(), "AddCommandLineFragmentDialog");
                                break;
                            case ModuleFragment.WEB_VIEW_FRAGMENT:
                                new AddWebViewFragmentDialog().setPage(page).setContainer(container)
                                        .setOnDismissListener(updateListener)
                                        .show(getFragmentManager(), "AddWebViewFragmentDialog");
                                break;
                            case ModuleFragment.WIDGET_CONTAINER_FRAGMENT:
                                Util.addWidgetToContainer(getActivity(), page, container, null);
                                if (updateListener != null) {
                                    updateListener.onDismiss();
                                }
                                break;
                            case ModuleFragment.SCROLL_CONTAINER_FRAGMENT:
                                Util.addFragmentToContainer(activity, ScrollContainerFragment
                                        .newInstance(), page, container);
                                if (updateListener != null) {
                                    updateListener.onDismiss();
                                }
                                break;
                            case ModuleFragment.HORIZONTAL_CONTAINER:
                                Util.addFragmentToContainer(activity, HorizontalContainerFragment
                                        .newInstance(), page, container);
                                if (updateListener != null) {
                                    updateListener.onDismiss();
                                }
                                break;
                            default:
                                Log.w(LOG_TAG, "Unknown fragment: " + fragmentValues[selection]);
                                break;
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                })
                .setSingleChoiceItems(fragments, selection, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        selection = which;
                    }
                })
                .create();
    }

    public AddFragmentDialog setPage(PageContainerFragment page) {
        this.page = page;
        return this;
    }

    public AddFragmentDialog setContainer(@Nullable Container container) {
        this.container = container;
        return this;
    }

    public AddFragmentDialog setContentUpdateListener(CustomDialogFragment.OnDismissListener
                                                              updateListener) {
        this.updateListener = updateListener;
        return this;
    }
}
