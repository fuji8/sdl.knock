package me.fuji8.sdl.knock;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;

import com.google.android.material.snackbar.Snackbar;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.NavDestination;
import androidx.navigation.Navigation;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import me.fuji8.sdl.knock.databinding.ActivityMainBinding;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.UUID;

import me.fuji8.sdl.knock.message.ChatMessage;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private AppBarConfiguration appBarConfiguration;
    private ActivityMainBinding binding;


    private final static String TAG = MainActivity.class.getSimpleName();
    public final static UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private TextView status;
    private ProgressBar progress;


    public static BluetoothAdapter adapter;

    public enum State {
        Initializing,
        Disconnected,
        Connecting,
        Connected,
        Waiting
    }
    private State state = State.Initializing;

    private int messageSeq = 0;
    private Agent agent;
    private BluetoothInitializer initializer;

    // sensor
    private SensorManager manager;
    private Sensor gyroscope;
    private Sensor accelerometer;

    private String currentFragment = null;

    private Long adminDetectedTime = 0l;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        status = findViewById(R.id.main_status);
        progress = findViewById(R.id.main_progress);

        setSupportActionBar(binding.toolbar);

        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        appBarConfiguration = new AppBarConfiguration.Builder(navController.getGraph()).build();
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);

        navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                Log.e(TAG, "onDestinationChanged: "+destination.getLabel());
                currentFragment = destination.getLabel().toString();
            }
        );

        binding.fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });

        // sensor
        manager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (manager == null) {
            Toast.makeText(this, R.string.toast_no_sensor_manager, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        accelerometer= manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer == null) {
            Toast.makeText(this, R.string.toast_no_accelerometer, Toast.LENGTH_LONG).show();
        }

        setState(State.Initializing);

        initializer = new BluetoothInitializer(this) {
            @Override
            protected void onReady(BluetoothAdapter adapter) {
                MainActivity.this.adapter = adapter;
                setState(State.Disconnected);
            }
        };
        initializer.initialize();
    }

    private static class CommHandler extends Handler {
        WeakReference<MainActivity> ref;

        CommHandler(MainActivity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.d(TAG, "handleMessage");
            MainActivity activity = ref.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case Agent.MSG_STARTED:
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    activity.setState(State.Connected, ScanActivity.caption(device));
                    break;
                case Agent.MSG_FINISHED:
                    Toast.makeText(activity, R.string.toast_connection_closed, Toast.LENGTH_SHORT).show();
                    activity.setState(State.Disconnected);
                    break;
                case Agent.MSG_RECEIVED:
                    activity.showMessage((ChatMessage) msg.obj);
                    break;
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (state == State.Connected && agent != null) {
            agent.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        menu.findItem(R.id.menu_main_connect).setVisible(state == State.Disconnected);
        menu.findItem(R.id.menu_main_disconnect).setVisible(state == State.Connected);
        menu.findItem(R.id.menu_main_accept_connection).setVisible(state == State.Disconnected);
        menu.findItem(R.id.menu_main_stop_listening).setVisible(state == State.Waiting);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
            case R.id.menu_main_connect:
                agent = new ClientAgent(this, new CommHandler(this));
                ((ClientAgent) agent).connect();
                return true;
            case R.id.menu_main_disconnect:
                disconnect();
                return true;
            case R.id.menu_main_accept_connection:
                agent = new ServerAgent(this, new CommHandler(this));
                ((ServerAgent) agent).start(adapter);
                return true;
            case R.id.menu_main_stop_listening:
                ((ServerAgent) agent).stop();
                return true;
            case R.id.menu_main_clear_connection:
                return true;
            case R.id.menu_main_about:
                new AlertDialog.Builder(this)
                        .setTitle(R.string.about_dialog_title)
                        .setMessage(R.string.about_dialog_content)
                        .setPositiveButton(android.R.string.ok, null)
                        .show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_content_main);
        return NavigationUI.navigateUp(navController, appBarConfiguration)
                || super.onSupportNavigateUp();
    }


    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        Log.d(TAG, "onActivityResult: reqCode=" + reqCode + " resCode=" + resCode);
        initializer.onActivityResult(reqCode, resCode, data); // delegate
        if (agent != null) {
            agent.onActivityResult(reqCode, resCode, data); // delegate
        }
    }

    public void setState(State state) {
        setState(state, null);
    }

    public void setState(State state, String arg) {
        this.state = state;
        switch (state) {
            case Initializing:
            case Disconnected:
                status.setText(R.string.main_status_disconnected);
                break;
            case Connecting:
                status.setText(getString(R.string.main_status_connecting_to, arg));
                break;
            case Connected:
                status.setText(getString(R.string.main_status_connected_to, arg));
                break;
            case Waiting:
                status.setText(R.string.main_status_listening_for_incoming_connection);
                break;
        }
        invalidateOptionsMenu();
    }

    public void setProgress(boolean isConnecting) {
        progress.setIndeterminate(isConnecting);
    }

    public void showMessage(ChatMessage message) {
        if (message.sound == 1) {
            return;
        }
        if (currentFragment.equals("Admin Fragment")) {
            //if (message.time - 3e7 < adminDetectedTime && adminDetectedTime < message.time + 3e7) {
                AdminFragment.chatLogAdapter.add(message);
                AdminFragment.chatLogAdapter.notifyDataSetChanged();
            // }
        }
    }

    private void disconnect() {
        Log.d(TAG, "disconnect");
        agent.close();
        agent = null;
        setState(State.Disconnected);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        manager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST);
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        manager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Log.d(TAG, Long.toString(adminDetectedTime));
        if (this.state != State.Connected) {
            return;
        }

        if (currentFragment.equals("User Fragment")){
            float x = event.values[0];
            if(15 < x && x < 30) {
                String content = "shake";
                messageSeq++;
                long time = System.nanoTime();
                ChatMessage message = new ChatMessage(messageSeq, time, content, adapter.getName(), 0);
                agent.send(message);
            }
       } else if (currentFragment.equals("Admin Fragment")) {
            float z = event.values[2];
            if (9.95 < z && z < 10) {
                adminDetectedTime = System.nanoTime();
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "onAccuracyChanged: accuracy=" + accuracy);
    }
}