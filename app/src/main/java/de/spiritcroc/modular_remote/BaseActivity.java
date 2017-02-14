/*
 * Copyright (C) 2017 SpiritCroc
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

package de.spiritcroc.modular_remote;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String LOG_TAG = BaseActivity.class.getSimpleName();

    private static final boolean DEBUG = false;

    private Messenger mGlobalKeyService = null;

    private Messenger mGlobalKeyListener = new Messenger(new Handler() {
        @Override
        public void handleMessage(Message msg) {
            onGlobalKeyMessage(msg);
        }
    });

    private boolean mGlobalKeyServiceBound = false;

    protected ServiceConnection mGlobalKeyServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            mGlobalKeyService = new Messenger(iBinder);
            mGlobalKeyServiceBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mGlobalKeyService = null;
            mGlobalKeyServiceBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startService(new Intent(this, GlobalKeyService.class));
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, GlobalKeyService.class), mGlobalKeyServiceConnection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mGlobalKeyServiceBound) {
            unbindService(mGlobalKeyServiceConnection);
            mGlobalKeyServiceBound = false;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        Message msg = Message.obtain(null, GlobalKeyService.ACTION_REGISTER_CLIENT);
        msg.replyTo = mGlobalKeyListener;
        sendGlobalKeyServiceMsg(msg);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Message msg = Message.obtain(null, GlobalKeyService.ACTION_UNREGISTER_CLIENT);
        msg.replyTo = mGlobalKeyListener;
        sendGlobalKeyServiceMsg(msg);
    }

    public void sendGlobalKeyServiceMsg(Message message) {
        if (!mGlobalKeyServiceBound) {
            new RuntimeException("Discarding message, required service is not bound").printStackTrace();
            return;
        }
        try {
            mGlobalKeyService.send(message);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    protected void onGlobalKeyMessage(Message message) {
        if (DEBUG) Log.v(LOG_TAG, "Received message " + message);
    }
}
