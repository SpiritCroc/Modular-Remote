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
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.modules.Container;
import de.spiritcroc.modular_remote.modules.DisplayFragment;
import de.spiritcroc.modular_remote.modules.SpinnerFragment;
import de.spiritcroc.modular_remote.modules.ToggleFragment;
import de.spiritcroc.modular_remote.modules.WebViewFragment;
import de.spiritcroc.modular_remote.modules.ModuleFragment;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;

public class SelectFragmentsDialog extends DialogFragment {
    private static final String LOG_TAG = SelectFragmentsDialog.class.getSimpleName();

    private PageContainerFragment page;
    private ModuleFragment[] moduleFragments;
    private ModuleFragment[] previousFragments;
    private ArrayList<ModuleFragment> selectedModuleFragments = new ArrayList<>();
    private boolean[] selection;
    private boolean pageRemovalAllowed = false;

    private AlertDialog alertDialog;

    private View removeButton, addButton, sortButton, moveButton, editButton, cloneButton;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState){
        final Activity activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        View view = activity.getLayoutInflater().inflate(R.layout.dialog_select_fragment, null);

        view.findViewById(R.id.finish_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Only close dialog
                dismiss();
            }
        });
        removeButton = view.findViewById(R.id.remove_button);
        removeButton.setOnClickListener(removeListener);
        moveButton = view.findViewById(R.id.move_button);
        moveButton.setOnClickListener(moveListener);
        cloneButton = view.findViewById(R.id.clone_button);
        cloneButton.setOnClickListener(cloneListener);
        editButton = view.findViewById(R.id.edit_button);
        editButton.setOnClickListener(editListener);
        addButton = view.findViewById(R.id.add_button);
        addButton.setOnClickListener(addListener);
        sortButton = view.findViewById(R.id.sort_button);
        sortButton.setOnClickListener(sortListener);

        if (activity instanceof MainActivity) {
            pageRemovalAllowed = ((MainActivity) activity).isPageRemovalAllowed();
        }

        String[] fragmentNameList = getFragmentList();
        createSelection();

        alertDialog = builder.setTitle(R.string.dialog_select_fragment)
                .setView(view)
                .setMultiChoiceItems(fragmentNameList, selection,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                selection[which] = isChecked;
                                if (isChecked) {
                                    selectedModuleFragments.add(moduleFragments[which]);
                                } else {
                                    selectedModuleFragments.remove(moduleFragments[which]);
                                }
                                setViewVisibilities();
                                if (alertDialog != null && isChecked &&
                                        which == selection.length - 1) {
                                    // Selected last:
                                    // Make sure that the selection is not hidden behind buttons
                                    // made visible with setViewVisibilities
                                    final ListView listView = alertDialog.getListView();
                                    listView.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            listView.smoothScrollToPosition(selection.length - 1);
                                        }
                                    });
                                }
                            }
                        }).create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialog) {
                if (!selectedModuleFragments.isEmpty()) {
                    // Scroll to first selected item
                    alertDialog.getListView().smoothScrollToPositionFromTop(
                            Arrays.asList(moduleFragments).indexOf(selectedModuleFragments.get(0)),
                            0, 0);
                }
            }
        });

        return alertDialog;
    }
    private void createSelection() {
        if (moduleFragments == null) {
            // Do it later
            return;
        }
        if (selection == null) {
            selection = new boolean[moduleFragments.length];
            for (int i = 0; i < selection.length; i++) {
                selection[i] = false;
            }
        } else {
            if (previousFragments != null && !Arrays.equals(previousFragments, moduleFragments)) {
                // Update selection to select the same fragments
                boolean[] previousSelection = selection;
                selection = new boolean[moduleFragments.length];
                List<ModuleFragment> previousFragmentList = Arrays.asList(previousFragments);
                for (int i = 0; i < selection.length; i++) {
                    int previousIndex = previousFragmentList.indexOf(moduleFragments[i]);
                    selection[i] = previousIndex >= 0 && previousSelection[previousIndex];
                }
            }
            // Update selected moduleFragments
            selectedModuleFragments.clear();
            for (int i = 0; i < selection.length; i++) {
                if (selection[i]) {
                    selectedModuleFragments.add(moduleFragments[i]);
                }
            }
        }
        setViewVisibilities();
    }

    public SelectFragmentsDialog setPage(PageContainerFragment page){
        this.page = page;
        return this;
    }

    private void setViewVisibilities() {
        boolean containsPage = false;
        for (int i = 0; i < selectedModuleFragments.size(); i++) {
            if (selectedModuleFragments.get(i) instanceof PageContainerFragment) {
                containsPage = true;
            }
        }

        switch (selectedModuleFragments.size()) {
            case 0:
                removeButton.setVisibility(View.GONE);
                moveButton.setVisibility(View.GONE);
                cloneButton.setVisibility(View.GONE);
                editButton.setVisibility(View.GONE);
                addButton.setVisibility(View.GONE);
                sortButton.setVisibility(View.GONE);
                break;
            case 1:
                removeButton.setVisibility(pageRemovalAllowed || !containsPage ?
                        View.VISIBLE : View.GONE);
                moveButton.setVisibility(pageRemovalAllowed || !containsPage ?
                        View.VISIBLE : View.GONE);
                cloneButton.setVisibility(View.VISIBLE);
                editButton.setVisibility(View.VISIBLE);
                if (selectedModuleFragments.get(0) instanceof Container) {
                    addButton.setVisibility(View.VISIBLE);
                    sortButton.setVisibility(((Container) selectedModuleFragments.get(0))
                            .getFragmentCount() > 1 ? View.VISIBLE : View.GONE);
                } else {
                    addButton.setVisibility(View.GONE);
                    sortButton.setVisibility(View.GONE);
                }
                break;
            default:
                boolean parentAndChildSelected = parentAndChildSelected();
                removeButton.setVisibility(!parentAndChildSelected &&
                        (pageRemovalAllowed || !containsPage) ?
                        View.VISIBLE : View.GONE);
                moveButton.setVisibility(containsPage || parentAndChildSelected ?
                        View.GONE : View.VISIBLE);
                cloneButton.setVisibility(containsPage || parentAndChildSelected ?
                        View.GONE : View.VISIBLE);
                editButton.setVisibility(View.GONE);
                addButton.setVisibility(View.GONE);
                sortButton.setVisibility(View.GONE);
                break;
        }
    }

    private boolean parentAndChildSelected() {
        for (int i = 0; i < selectedModuleFragments.size(); i++) {
            if (parentSelected(selectedModuleFragments.get(i))) {
                return true;
            }
        }
        return false;
    }

    private String[] getFragmentList() {
        moduleFragments = page.getAllFragments();

        String[] fragmentList = new String[moduleFragments.length];
        for (int i = 0; i < moduleFragments.length; i++){
            fragmentList[i] = "";
            int depth;
            if (moduleFragments[i] instanceof PageContainerFragment)
                depth = ((PageContainerFragment) moduleFragments[i]).getDepth()-1;
            else
                depth = moduleFragments[i].getParent().getDepth();
            for (int j = 0; j < depth; j++)
                fragmentList[i] += "   ";
            fragmentList[i] += moduleFragments[i].getReadableName();
        }
        return fragmentList;
    }

    private View.OnClickListener editListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            ModuleFragment fragment = selectedModuleFragments.get(0);
            if (fragment instanceof WebViewFragment) {
                new AddWebViewFragmentDialog()
                        .setEditFragment((WebViewFragment) fragment)
                        .setOnDismissListener(contentUpdateListener)
                        .show(getFragmentManager(),
                                "EditWebViewFragmentDialog");
            } else if (fragment instanceof PageContainerFragment) {
                new AddPageDialog().setPage((PageContainerFragment) fragment,
                        ((PageContainerFragment)fragment).getConnection())
                        .setOnDismissListener(contentUpdateListener)
                        .show(getFragmentManager(), "ManagePagesDialog");
            } else if (fragment instanceof DisplayFragment) {
                new AddDisplayFragmentDialog()
                        .setEditFragment((DisplayFragment) fragment,
                                ((DisplayFragment) fragment).getConnection())
                        .setOnDismissListener(contentUpdateListener)
                        .show(getFragmentManager(), "AddDisplayFragmentDialog");
            } else if (fragment instanceof SpinnerFragment) {
                new AddSpinnerFragmentDialog()
                        .setEditFragment((SpinnerFragment) fragment)
                        .setOnDismissListener(contentUpdateListener)
                        .show(getFragmentManager(), "AddSpinnerFragmentDialog");
            } else if (fragment instanceof ToggleFragment) {
                new AddToggleFragmentDialog()
                        .setEditFragment((ToggleFragment) fragment,
                                ((ToggleFragment) fragment).getConnection())
                        .setOnDismissListener(contentUpdateListener)
                        .show(getFragmentManager(), "AddToggleFragmentDialog");
            } else {
                Toast.makeText(getActivity(), R.string.error_fragment_not_editable,
                        Toast.LENGTH_SHORT).show();
            }
        }
    };
    private View.OnClickListener moveListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (selectedModuleFragments.get(0) instanceof PageContainerFragment) {
                new SortPagesDialog().show(getFragmentManager(), "SortPagesDialog");
            } else {
                new SelectContainerDialog().setValues(page, selectedModuleFragments)
                        .setMode(SelectContainerDialog.Mode.MOVE_FRAGMENTS)
                        .setOnDismissListener(contentUpdateListener)
                        .show(getFragmentManager(), "SelectContainerDialog2");
            }
        }
    };
    private View.OnClickListener removeListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (selectedModuleFragments.get(0) instanceof PageContainerFragment) {
                if (pageRemovalAllowed) {
                    dismiss();
                    new RemoveContainerDialog().setFragment(selectedModuleFragments.get(0))
                            .show(getFragmentManager(), "RemoveContainerDialog");
                } else {
                    Toast.makeText(getActivity(),
                            R.string.
                                    error_last_page_removal_not_allowed,
                            Toast.LENGTH_SHORT).show();
                }
            } else {
                for (int i = 0; i < selectedModuleFragments.size(); i++) {
                    ModuleFragment fragment = selectedModuleFragments.get(i);
                    fragment.getParent().removeFragment(fragment, true);
                }
                updateContent();
            }
        }
    };
    private View.OnClickListener cloneListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            Activity activity = getActivity();
            if (!(activity instanceof MainActivity)) {
                dismiss();
                Log.e(LOG_TAG, "!(activity instanceof MainActivity)");
                return;
            }
            MainActivity mainActivity = (MainActivity) activity;
            if (selectedModuleFragments.get(0) instanceof PageContainerFragment) {
                dismiss();
                mainActivity.addPage((PageContainerFragment) selectedModuleFragments.get(0).copy());
            } else {
                ArrayList<ModuleFragment> fragmentCopies = new ArrayList<>();
                for (int i = 0; i < selectedModuleFragments.size(); i++) {
                    fragmentCopies.add(selectedModuleFragments.get(i).copy());
                }
                new SelectContainerDialog().setValues(page, fragmentCopies)
                        .setMode(SelectContainerDialog.Mode.COPY_FRAGMENTS)
                        .setOnDismissListener(contentUpdateListener)
                        .show(getFragmentManager(), "SelectContainerDialog");
            }
        }
    };
    private View.OnClickListener addListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new AddFragmentDialog().setPage(page)
                    .setContentUpdateListener(contentUpdateListener)
                    .setContainer((Container) selectedModuleFragments.get(0))
                    .show(getFragmentManager(), "AddFragmentDialog");
        }
    };
    private View.OnClickListener sortListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            new SortFragmentsDialog().setContainer((Container) selectedModuleFragments.get(0))
                    .setOnDismissListener(contentUpdateListener)
                    .show(getFragmentManager(), "SortFragmentsDialog");
        }
    };

    private CustomDialogFragment.OnDismissListener contentUpdateListener =
            new CustomDialogFragment.OnDismissListener() {
                @Override
                public void onDismiss() {
                    updateContent();
                }
            };

    private void updateContent() {
        // Dismiss and re-show to get an updated fragment list
        dismiss();
        new SelectFragmentsDialog().setPage(page)
                .setSelection(selection, moduleFragments)
                .show(getFragmentManager(), "SelectFragmentsDialog");
    }

    private SelectFragmentsDialog setSelection(boolean[] selection,
                                               ModuleFragment[] moduleFragments) {
        this.selection = selection;
        previousFragments = moduleFragments;
        createSelection();
        return this;
    }

    private boolean parentSelected(ModuleFragment fragment) {
        Container parent;
        while (true) {
            parent = fragment.getParent();
            if (parent instanceof ModuleFragment && selectedModuleFragments.contains(parent)) {
                return true;
            }
            if (parent instanceof ModuleFragment) {
                fragment = (ModuleFragment) parent;
            } else {
                break;
            }
        }
        return false;
    }
}
