package me.fuji8.sdl.knock;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import java.io.IOException;
import java.lang.ref.WeakReference;

import me.fuji8.sdl.knock.MainActivity.State;

class ClientAgent extends Agent {
    private final static String TAG = ClientAgent.class.getSimpleName();

    private final static int REQ_GET_DEVICE = 2222;

    ClientAgent(MainActivity activity, Handler handler) {
        super(activity, handler);
    }

    void connect() {
        Log.d(TAG, "connect");
        Intent intent = new Intent(activity, ScanActivity.class);
        activity.startActivityForResult(intent, REQ_GET_DEVICE);
    }

    @Override
    void onActivityResult(int reqCode, int resCode, Intent data) {
        Log.d(TAG, "onActivityResult: reqCode=" + reqCode + " resCode=" + resCode);
        if (reqCode == REQ_GET_DEVICE) {
            if (resCode != Activity.RESULT_OK) {
                activity.setState(MainActivity.State.Disconnected);
                return;
            }
            connect1((BluetoothDevice) data.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE));
        }
    }

    private void connect1(BluetoothDevice device) {
        Log.d(TAG, "connect1");
        activity.setProgress(true);
        new ClientTask(this).execute(device);
        activity.setState(State.Connecting, device.getName());
    }

    private static class ClientTask extends AsyncTask<BluetoothDevice, Void, BluetoothSocket> {
        private final WeakReference<ClientAgent> ref;

        ClientTask(ClientAgent client) {
            super();
            ref = new WeakReference<>(client);
        }

        @Override
        protected BluetoothSocket doInBackground(BluetoothDevice... params) {
            Log.d(TAG, "doInBackground");
            BluetoothSocket socket = null;
            try {
                socket = params[0].createRfcommSocketToServiceRecord(MainActivity.SPP_UUID);
                socket.connect();
                return socket;
            } catch (IOException e) {
                e.printStackTrace();
                if (socket != null) {
                    try {
                        socket.close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
                return null;
            }
        }

        @RequiresApi(api = Build.VERSION_CODES.KITKAT)
        @Override
        protected void onPostExecute(BluetoothSocket socket) {
            Log.d(TAG, "onPostExecute");
            ClientAgent client = ref.get();
            if (client != null) {
                client.connect2(socket);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private void connect2(BluetoothSocket socket) {
        Log.d(TAG, "connect2");
        activity.setProgress(false);
        if (socket == null) {
            Toast.makeText(activity, R.string.toast_connection_failed, Toast.LENGTH_SHORT).show();
            activity.setState(State.Disconnected);
            return;
        }
        runCommThread(socket);
    }
}
