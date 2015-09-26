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
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.Util;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;
import de.spiritcroc.modular_remote.modules.WidgetContainerFragment;

public class AddWidgetContainerDialog extends CustomDialogFragment {
    private Container container;
    private WidgetContainerFragment fragment;
    private PageContainerFragment page;
    private EditText editWidth, editHeight;
    private TextView widthTextView, heightTextView;
    private CheckBox editWrapWidth, editWrapHeight;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.dialog_widget_container,
                null);
        editWidth = (EditText) view.findViewById(R.id.edit_width);
        editHeight = (EditText) view.findViewById(R.id.edit_height);
        editWrapWidth = (CheckBox) view.findViewById(R.id.edit_wrap_width);
        editWrapHeight = (CheckBox) view.findViewById(R.id.edit_wrap_height);
        widthTextView = (TextView) view.findViewById(R.id.width_text_view);
        heightTextView = (TextView) view.findViewById(R.id.height_text_view);

        editWrapWidth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editWrapWidth.isChecked()) {
                    editWidth.setVisibility(View.GONE);
                    widthTextView.setVisibility(View.GONE);
                } else {
                    editWidth.setVisibility(View.VISIBLE);
                    widthTextView.setVisibility(View.VISIBLE);
                }
            }
        });
        editWrapHeight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editWrapHeight.isChecked()) {
                    editHeight.setVisibility(View.GONE);
                    heightTextView.setVisibility(View.GONE);
                } else {
                    editHeight.setVisibility(View.VISIBLE);
                    heightTextView.setVisibility(View.VISIBLE);
                }
            }
        });

        view.findViewById(R.id.view_wrap_width).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editWrapWidth.toggle();
                if (editWrapWidth.isChecked()) {
                    editWidth.setVisibility(View.GONE);
                    widthTextView.setVisibility(View.GONE);
                } else {
                    editWidth.setVisibility(View.VISIBLE);
                    widthTextView.setVisibility(View.VISIBLE);
                }
            }
        });
        view.findViewById(R.id.view_wrap_height).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editWrapHeight.toggle();
                if (editWrapHeight.isChecked()) {
                    editHeight.setVisibility(View.GONE);
                    heightTextView.setVisibility(View.GONE);
                } else {
                    editHeight.setVisibility(View.VISIBLE);
                    heightTextView.setVisibility(View.VISIBLE);
                }
            }
        });

        String positiveButtonText;
        if (fragment != null) {// Edit fragment
            positiveButtonText = getString(R.string.dialog_ok);
            double width = fragment.getArgWidth();
            double height = fragment.getArgHeight();
            if (width == -1) {
                editWrapWidth.setChecked(true);
                editWidth.setVisibility(View.GONE);
                widthTextView.setVisibility(View.GONE);
            } else {
                editWidth.setText(String.valueOf(width));
            }
            if (height == -1) {
                editWrapHeight.setChecked(true);
                editHeight.setVisibility(View.GONE);
                heightTextView.setVisibility(View.GONE);
            } else {
                editHeight.setText(String.valueOf(height));
            }
        } else {// Create new fragment
            positiveButtonText = getString(R.string.dialog_add);
        }

        final AlertDialog alertDialog = builder.setTitle(R.string.dialog_configure_fragment)
                .setView(Util.scrollView(view))
                .setPositiveButton(positiveButtonText, null)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                }).create();

        // Listeners added to button this way so they don't lead to dialog.dismiss if illegal input
        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                double width, height;
                                if (editWrapWidth.isChecked()) {
                                    width = -1;
                                } else {
                                    width = Util.getSizeInput(editWidth);
                                    if (width == -1) {
                                        width = -2;
                                    }
                                }
                                if (editWrapHeight.isChecked()) {
                                    height = -1;
                                } else {
                                    height = Util.getSizeInput(editHeight);
                                    if (height == -1) {
                                        height = -2;
                                    }
                                }
                                if (width == -2 || height == -2) {
                                    return;
                                }

                                if (fragment != null) {// Edit fragment
                                    fragment.setValues(width, height);
                                    dismiss();
                                } else {// Create new widget
                                    if (container == null) {
                                        dismiss();
                                        // If container != null, a parent dialog already selected a
                                        // container and is waiting for the widget to be added
                                        // In this case, this dialog will be dismissed on success
                                    }
                                    Util.addWidgetToContainer(getActivity(), width, height, page,
                                            container, AddWidgetContainerDialog.this);
                                }
                            }
                        });
            }
        });

        return alertDialog;
    }

    public AddWidgetContainerDialog setPage(PageContainerFragment page) {
        this.page = page;
        return this;
    }
    public AddWidgetContainerDialog setEditFragment(WidgetContainerFragment fragment) {
        this.fragment = fragment;
        return this;
    }
    public AddWidgetContainerDialog setContainer(@Nullable Container container) {
        this.container = container;
        return this;
    }
}
