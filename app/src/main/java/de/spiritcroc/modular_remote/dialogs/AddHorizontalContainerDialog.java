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
import de.spiritcroc.modular_remote.modules.HorizontalContainerFragment;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;

public class AddHorizontalContainerDialog extends CustomDialogFragment {
    private Container container;
    private HorizontalContainerFragment fragment;
    private PageContainerFragment page;
    private EditText editWidth, editHeight;
    private TextView widthTextView;
    private CheckBox editMatchWidth;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.dialog_horizontal_container,
                null);
        editWidth = (EditText) view.findViewById(R.id.edit_width);
        editHeight = (EditText) view.findViewById(R.id.edit_height);
        editMatchWidth = (CheckBox) view.findViewById(R.id.edit_match_width);
        widthTextView = (TextView) view.findViewById(R.id.width_text_view);

        editMatchWidth.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editMatchWidth.isChecked()) {
                    editWidth.setVisibility(View.GONE);
                    widthTextView.setVisibility(View.GONE);
                } else {
                    editWidth.setVisibility(View.VISIBLE);
                    widthTextView.setVisibility(View.VISIBLE);
                }
            }
        });
        view.findViewById(R.id.view_match_width).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                editMatchWidth.toggle();
                if (editMatchWidth.isChecked()) {
                    editWidth.setVisibility(View.GONE);
                    widthTextView.setVisibility(View.GONE);
                } else {
                    editWidth.setVisibility(View.VISIBLE);
                    widthTextView.setVisibility(View.VISIBLE);
                }
            }
        });

        String positiveButtonText;
        if (fragment != null) {// Edit fragment
            positiveButtonText = getString(R.string.dialog_ok);
            double width = fragment.getArgWidth();
            double height = fragment.getArgHeight();
            if (width == -1) {
                editMatchWidth.setChecked(true);
                editWidth.setVisibility(View.GONE);
                widthTextView.setVisibility(View.GONE);
            } else {
                editWidth.setText(String.valueOf(width));
            }
            editHeight.setText(String.valueOf(height));
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

                                if (editMatchWidth.isChecked()) {
                                    width = -1;
                                } else {
                                    width = Util.getSizeInput(editWidth);
                                    if (width == -1) {
                                        width = -2;
                                    }
                                }
                                height = Util.getSizeInput(editHeight);
                                if (width == -2 || height == -1) {
                                    return;
                                }

                                if (fragment != null) {// Edit fragment
                                    fragment.setValues(width, height);
                                } else {// Create new fragment
                                    Util.addFragmentToContainer(activity, HorizontalContainerFragment
                                            .newInstance(width, height), page, container);
                                }
                                dismiss();
                            }
                        });
            }
        });

        return alertDialog;
    }

    public AddHorizontalContainerDialog setPage(PageContainerFragment page) {
        this.page = page;
        return this;
    }
    public AddHorizontalContainerDialog setEditFragment(HorizontalContainerFragment fragment) {
        this.fragment = fragment;
        return this;
    }
    public AddHorizontalContainerDialog setContainer(@Nullable Container container) {
        this.container = container;
        return this;
    }
}
