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
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Spinner;

import java.util.Arrays;

import de.spiritcroc.modular_remote.R;
import de.spiritcroc.modular_remote.TcpConnectionManager;
import de.spiritcroc.modular_remote.TcpInformation;
import de.spiritcroc.modular_remote.Util;

public class EditConnectionDialog extends DialogFragment {
    private static final String LOG_TAG = EditConnectionDialog.class.getSimpleName();

    private TcpConnectionManager.TcpConnection connection;
    private TcpConnectionManager.ReceiverType type;
    private String[] typeValues;
    private LinearLayout dynamicButtonsLayout;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Activity activity = getActivity();
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        final View view = activity.getLayoutInflater().inflate(R.layout.dialog_edit_connection,
                null);

        Spinner receiverTypeSpinner = (Spinner) view.findViewById(R.id.edit_receiver_type);
        final EditText editReceiverIp = (EditText) view.findViewById(R.id.edit_receiver_ip);
        editReceiverIp.setText(connection.getIp());
        typeValues = getResources().getStringArray(R.array.receiver_type_array_values);
        receiverTypeSpinner.setAdapter(ArrayAdapter.createFromResource(activity,
                R.array.receiver_type_array, R.layout.support_simple_spinner_dropdown_item));
        receiverTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                type = TcpConnectionManager.ReceiverType.valueOf(typeValues[(int) id]);
                if (type.equals(connection.getType())) {
                    addTypeSpecificButtons();
                } else {
                    removeTypeSpecificButtons();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
        // Select type from spinner
        for (int i = 0; i < typeValues.length; i++) {
            if (typeValues[i].equals(connection.getType().toString())) {
                receiverTypeSpinner.setSelection(i);
                break;
            }
        }

        Button hideSubmenuItemsButton =
                (Button) view.findViewById(R.id.hide_submenu_elements_button);
        hideSubmenuItemsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new SelectEditSubmenuDialog().setConnection(connection).show(getFragmentManager(),
                        "SelectEditSubmenuDialog");
            }
        });

        dynamicButtonsLayout = (LinearLayout) view.findViewById(R.id.dynamic_buttons_layout);
        addTypeSpecificButtons();

        Button reconnectButton = (Button) view.findViewById(R.id.button_reconnect);
        reconnectButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connection.reset();
            }
        });

        final AlertDialog alertDialog = builder.setTitle(R.string.dialog_edit_connection)
                .setPositiveButton(R.string.dialog_ok, null)
                .setNegativeButton(R.string.dialog_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Only close
                    }
                })
                .setView(Util.scrollView(view))
                .create();

        alertDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(final DialogInterface dialog) {
                alertDialog.getButton(DialogInterface.BUTTON_POSITIVE)
                        .setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                String ip = Util.getUserInput(editReceiverIp, false);
                                if (ip == null) {
                                    return;
                                }
                                boolean changed = false;
                                if (!connection.getType().equals(type)) {
                                    changed = true;
                                }
                                if (!connection.getIp().equals(ip)) {
                                    changed = true;
                                    TcpConnectionManager.TcpConnection connectionCheck =
                                            TcpConnectionManager.getInstance(getActivity()
                                                    .getApplicationContext()).getTcpConnection(ip);
                                    if (connectionCheck != null) {
                                        // Don't allow multiple connections with same address
                                        editReceiverIp.setError(getString(
                                                R.string.error_connection_already_exists));
                                        return;
                                    }
                                }
                                if (changed) {
                                    connection.setValues(editReceiverIp.getText().toString(), type);
                                }
                                dismiss();
                            }
                        });
            }
        });

        return alertDialog;
    }

    public EditConnectionDialog setConnection(TcpConnectionManager.TcpConnection connection) {
        this.connection = connection;
        return this;
    }

    private void addTypeSpecificButtons() {
        removeTypeSpecificButtons();

        switch (connection.getType()) {
            case PIONEER: {
                final Button hideInputButton = new Button(getActivity());
                dynamicButtonsLayout.addView(hideInputButton,
                        new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.WRAP_CONTENT));
                hideInputButton.setText(R.string.dialog_detect_inputs);
                hideInputButton.setOnClickListener(new AutoHideUpdateOnClickListener() {
                    AutoHideDetector autoHideDetector;

                    @Override
                    public void onClick(View v) {
                        if (autoHideDetector == null) {
                            autoHideDetector = new AutoHideDetector("FN··",
                                    connection.requireCustomizedMenu(6, getActivity()), this);
                            autoHideDetector.execute();
                            hideInputButton.setText(R.string.dialog_detecting_inputs);
                        } else {
                            autoHideDetector.finish();
                        }
                    }

                    @Override
                    public void onFinish(boolean success) {
                        if (success) {
                            hideInputButton.setText(R.string.dialog_detect_inputs_finish);
                        } else {
                            hideInputButton.setText(R.string.dialog_detect_inputs);
                            autoHideDetector = null;
                        }
                    }
                });
                break;
            }
        }
    }

    private void removeTypeSpecificButtons() {
        dynamicButtonsLayout.removeAllViews();
    }

    private class AutoHideDetector extends AsyncTask<Void, Void, Void>
            implements TcpConnectionManager.TcpUpdateInterface {
        private TcpInformation firstInformation;
        private volatile boolean finish = false, waitForReceive = false, success = false;
        private TcpConnectionManager tcpConnectionManager;
        private String responseClassifier;
        private TcpConnectionManager.TcpConnection.CustomizedMenu customizedMenu;
        private AutoHideUpdateOnClickListener listener;
        private boolean[] hidden;

        private Handler stopWaitingHandler = new Handler();
        private Runnable stopWaiting = new Runnable() {
            @Override
            public void run() {
                waitForReceive = false;
            }
        };

        public AutoHideDetector(String responseClassifier,
                                TcpConnectionManager.TcpConnection.CustomizedMenu menu,
                                AutoHideUpdateOnClickListener listener) {
            this.responseClassifier = responseClassifier;
            customizedMenu = menu;
            this.listener = listener;
        }

        @Override
        protected void onPreExecute() {
            tcpConnectionManager = TcpConnectionManager.getInstance(
                    getActivity().getApplicationContext());
            tcpConnectionManager.requireConnection(this);
        }
        @Override
        protected  Void doInBackground(Void... nothing) {
            // Show all in order to receive correct classified responses
            for (int i = 0; i < customizedMenu.hidden.length; i++) {
                customizedMenu.hidden[i] = false;
            }
            // Hide all (locally)
            hidden = new boolean[customizedMenu.hidden.length];
            for (int i = 0; i < hidden.length; i++) {
                hidden[i] = true;
            }
            /* Don't get buffered information in case it could not be classified
            firstInformation = connection.getBufferedInformation(responseClassifier);
            */
            // Request all inputs
            while (!finish) {
                waitForReceive = true;
                stopWaitingHandler.removeCallbacks(stopWaiting);
                // Give up if no response after 5 seconds
                stopWaitingHandler.postDelayed(stopWaiting, 5000);
                connection.sendRawCommand("FU");
                while(waitForReceive && !finish) {}
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) {
            tcpConnectionManager.stopUpdate(this);
            customizedMenu.hidden = hidden;
            listener.onFinish(success);
            connection.updateListeners(new TcpInformation(
                    TcpInformation.InformationType.UPDATE_MENU,
                    customizedMenu.getMenuValue()));
            if (success) {
                // Go back to previous input
                connection.sendRawCommand("FD");
            }
        }

        public void update(TcpInformation information) {
            if (information.isClassifiedResponse() &&
                    information.getResponseClassifier().equals(responseClassifier)) {
                if (firstInformation == null || !firstInformation.isStringAvailable()) {
                    firstInformation = information;
                    waitForReceive = false;
                } else {
                    if (firstInformation.getStringValue().equals(information.getStringValue())) {
                        finish = success = true;
                    } else {
                        waitForReceive = false;
                    }
                    int index = Arrays.asList(customizedMenu.names).indexOf(
                            information.getStringValue());
                    if (index < 0) {
                        Log.w(LOG_TAG, "Could not find input " + information.getStringValue());
                        success = false;
                        finish = true;
                    } else {
                        hidden[index] = false;
                    }
                }
            }
        }
        public TcpConnectionManager.ReceiverType getType() {
            return connection.getType();
        }
        public String getIp() {
            return connection.getIp();
        }
        @Override
        public void setConnectionValues(String ip, TcpConnectionManager.ReceiverType type) {
            // Not required
        }
        public void finish() {
            finish = true;
        }
    }

    private interface AutoHideUpdateOnClickListener extends View.OnClickListener {
        void onFinish(boolean success);
    }
}
