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
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;

import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.TcpInformation;

public class HideSubmenuItemsDialog extends DialogFragment {
    private TcpConnectionManager.TcpConnection connection;
    private int submenu;
    private String submenuName;

    public HideSubmenuItemsDialog setValues(TcpConnectionManager.TcpConnection connection,
                                            int submenu, String submenuName) {
        this.connection = connection;
        this.submenu = submenu;
        this.submenuName = submenuName;
        return this;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        final TcpConnectionManager.TcpConnection.CustomizedMenu customizedMenu =
                connection.requireCustomizedMenu(submenu, activity);
        final boolean[] selection = customizedMenu.getHidden();

        return builder.setTitle(getString(R.string.dialog_hide) + submenuName)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close dialog
                    }
                })
                .setNeutralButton(R.string.dialog_reset, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        customizedMenu.resetHidden();
                        connection.updateListeners(new TcpInformation(
                                TcpInformation.InformationType.UPDATE_MENU,
                                customizedMenu.getMenuValue()));

                    }
                })
                .setPositiveButton(R.string.dialog_hide_selected,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                customizedMenu.hidden = selection;
                                connection.updateListeners(new TcpInformation(
                                        TcpInformation.InformationType.UPDATE_MENU,
                                        customizedMenu.getMenuValue()));
                            }
                        })
                .setMultiChoiceItems(customizedMenu.names, selection,
                        new DialogInterface.OnMultiChoiceClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which,
                                                boolean isChecked) {
                                selection[which] = isChecked;
                            }
                        }).create();
    }
}
