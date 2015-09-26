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
import android.text.SpannableString;
import android.view.View;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.dslv.DragSortListView;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.ModuleFragment;

public class SortFragmentsDialog extends CustomDialogFragment {

    private Container container;
    private ModuleFragment[] initFragments;
    private ArrayList<ModuleFragment> sortFragments = new ArrayList<>();
    private ArrayList<Spannable> sortFragmentNames = new ArrayList<>();
    private ArrayAdapter arrayAdapter;
    private boolean reAddedFragments = false;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final Activity activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_sort, null);

        DragSortListView dslv = (DragSortListView) view.findViewById(R.id.drag_sort_list);

        initFragments = container.getFragments();
        for (ModuleFragment fragment: initFragments) {
            sortFragments.add(fragment);
            sortFragmentNames.add(fragment instanceof Container ?
                    ((Container) fragment).getContentReadableName(null) :
                    new SpannableString(fragment.getReadableName()));
            container.removeFragment(fragment, false);
        }

        arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                sortFragmentNames);
        dslv.setAdapter(arrayAdapter);
        dslv.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                sortFragments.add(to, sortFragments.remove(from));
                sortFragmentNames.add(to, sortFragmentNames.remove(from));
                arrayAdapter.notifyDataSetChanged();
            }
        });

        return builder.setTitle(R.string.dialog_sort_fragments)
                .setPositiveButton(R.string.dialog_apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Re-add in new order
                        reAddedFragments = true;
                        for (int i = 0; i < sortFragments.size(); i++) {
                            container.addFragment(sortFragments.get(i));
                        }
                    }
                })
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Close dialog
                    }
                })
                .setView(view).create();
    }

    public SortFragmentsDialog setContainer(Container container) {
        this.container = container;
        return this;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        maybeResetContent();
    }

    @Override
    public void onDestroy() {
        maybeResetContent();
    }

    private void maybeResetContent() {
        if (!reAddedFragments) {
            reAddedFragments = true;
            // Re-add in old order
            for (ModuleFragment fragment: initFragments) {
                container.addFragment(fragment);
            }
        }
    }
}
