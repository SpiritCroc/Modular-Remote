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
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;

import java.util.ArrayList;

import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.dslv.DragSortListView;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;

public class SortPagesDialog extends DialogFragment {
    private static final String LOG_TAG = SortPagesDialog.class.getSimpleName();

    private ArrayList<String> sortPageNames = new ArrayList<>();
    private ArrayList<Integer> sortOrder = new ArrayList<>();
    private ArrayAdapter arrayAdapter;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final Activity activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_sort, null);

        if (activity instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) activity;

            DragSortListView dslv = (DragSortListView) view.findViewById(R.id.drag_sort_list);

            PageContainerFragment[] initPages = mainActivity.getPages();

            for (PageContainerFragment page: initPages) {
                sortPageNames.add(page.getName());
                sortOrder.add(sortOrder.size());
            }

            arrayAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1,
                    sortPageNames);
            dslv.setAdapter(arrayAdapter);
            dslv.setDropListener(new DragSortListView.DropListener() {
                @Override
                public void drop(int from, int to) {
                    sortPageNames.add(to, sortPageNames.remove(from));
                    sortOrder.add(to, sortOrder.remove(from));
                    arrayAdapter.notifyDataSetChanged();
                }
            });
        } else {
            Log.e(LOG_TAG, "!(activity instanceof MainActivity)");
        }

        return builder.setTitle(R.string.dialog_sort_pages)
                .setPositiveButton(R.string.dialog_apply, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (activity instanceof MainActivity) {
                            ((MainActivity) activity).orderPages(sortOrder);
                        } else {
                            Log.e(LOG_TAG, "!(activity instanceof MainActivity)");
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
}
