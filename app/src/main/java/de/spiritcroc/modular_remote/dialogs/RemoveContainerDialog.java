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

import de.spiritcroc.modular_remote.MainActivity;
import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.modules.ModuleFragment;
import de.spiritcroc.modular_remote.modules.PageContainerFragment;

public class RemoveContainerDialog extends DialogFragment {
    private static final String LOG_TAG = RemoveContainerDialog.class.getSimpleName();

    private ModuleFragment fragment;

    public RemoveContainerDialog setFragment(ModuleFragment fragment) {
        this.fragment = fragment;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setMessage(getString(R.string.dialog_remove_container, fragment.getReadableName()))
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                })
                .setPositiveButton(R.string.dialog_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (fragment instanceof PageContainerFragment) {
                            Activity activity = getActivity();
                            if (activity instanceof MainActivity) {
                                ((MainActivity) activity)
                                        .removePage((PageContainerFragment) fragment, true);
                            } else {
                                Log.e(LOG_TAG, "!(getActivity() instanceof MainActivity)");
                            }
                        } else {
                            fragment.getParent().removeFragment(fragment, true);
                        }
                    }
                })
                .create();
    }
}
