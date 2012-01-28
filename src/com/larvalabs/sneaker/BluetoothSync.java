package com.larvalabs.sneaker;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.CodingErrorAction;
import java.util.ArrayList;

/**
 * Adapted from the Bluetooth demo app.
 *
 * @author John Watkinson
 */
public class BluetoothSync extends Activity {

    // Magic number with which to begin communications.
    public static final int MAGIC_NUMBER = 0xFABFAB01;
    // Protocol codes
    public static final int CODE_AVAILABLE_KEYS = 1;
    public static final int CODE_REQUESTED_KEYS = 2;
    public static final int CODE_POSTS = 3;

    // Debugging
    private static final String TAG = "BluetoothSync";
    private static final boolean D = true;

    // Message types sent from the BluetoothService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String IS_SERVER = "is_server";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BT = 3;

    // Name of the connected device
    private String connectedDeviceName = null;

    // Local Bluetooth adapter
    private BluetoothAdapter bluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothService service = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (D) {
            Log.e(TAG, "+++ ON CREATE +++");
        }

        // Set up the window layout
        setContentView(R.layout.sync_layout);

        final Button panicButton = (Button) findViewById(R.id.sync_panic);
        panicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Util.handlePanic(BluetoothSync.this);
            }
        });
        final Button listButton = (Button) findViewById(R.id.sync_list);
        listButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        final Button listenButton = (Button) findViewById(R.id.sync_listen);
        listenButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                ensureDiscoverable();
                // Start listening mode
                service.startServer();
            }
        });
        final Button connectButton = (Button) findViewById(R.id.sync_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent serverIntent = new Intent(BluetoothSync.this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
            }
        });
        // Get local Bluetooth adapter
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        setDeviceName();

        // If the adapter is null, then Bluetooth is not supported
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    private void setDeviceName() {
        String name = bluetoothAdapter.getName();
        final TextView nameView = (TextView) findViewById(R.id.sync_device_name);
        nameView.setText(name);
    }

    @Override
    public void onStart() {
        super.onStart();
        if (D) {
            Log.e(TAG, "++ ON START ++");
        }

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!bluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
            // Otherwise, setup the chat session
        } else {
            if (service == null) {
                setupChat();
            }
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if (D) {
            Log.e(TAG, "+ ON RESUME +");
        }

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (service != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (service.getState() == BluetoothService.STATE_NONE) {
                // Start the Bluetooth chat services
                service.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the BluetoothService to perform bluetooth connections
        service = new BluetoothService(this, handler);

    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if (D) {
            Log.e(TAG, "- ON PAUSE -");
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (D) {
            Log.e(TAG, "-- ON STOP --");
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (service != null) {
            service.stop();
        }
        if (D) {
            Log.e(TAG, "--- ON DESTROY ---");
        }
    }

    private void ensureDiscoverable() {
        if (D) {
            Log.d(TAG, "ensure discoverable");
        }
        if (bluetoothAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message containing posts.
     */
    private void sendMessage(int code, Post[] posts) {
        // Check that we're actually connected before trying anything
        if (service.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        service.write(code, posts);
    }

    /**
     * Sends a message containing strings
     * @param code
     * @param keys
     */
    private void sendMessage(int code, ArrayList<String> keys) {
        // Check that we're actually connected before trying anything
        if (service.getState() != BluetoothService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }
        service.write(code, keys);
    }

    private void setStatus(int resId) {
        // todo
//        final ActionBar actionBar = getActionBar();
//        actionBar.setSubtitle(resId);
    }

    private void setStatus(CharSequence subTitle) {
        // todo
//        final ActionBar actionBar = getActionBar();
//        actionBar.setSubtitle(subTitle);
    }

    // The Handler that gets information back from the BluetoothService
    private final Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MESSAGE_STATE_CHANGE:
                {
                    if (D) {
                        Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                    }
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            setStatus(getString(R.string.title_connected_to, connectedDeviceName));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            setStatus(R.string.title_connecting);
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                            setStatus(R.string.title_not_connected);
                            break;
                    }
                    break;
                }
                case MESSAGE_WRITE:
                {
                    // no-op
                    break;
                }
                case MESSAGE_READ:
                {
                    Util.debug("-----> RECEIVED message from bluetooth!");
                    int code = msg.arg1;
                    if (code == CODE_POSTS) {
                        Post[] posts = (Post[]) msg.obj;
                    // construct a string from the valid bytes in the buffer
                        receive(code, posts);
                    } else {
                        ArrayList<String> keys = (ArrayList<String>) msg.obj;
                        receive(code, keys);
                    }
                    break;
                }
                case MESSAGE_DEVICE_NAME:
                {
                    // save the connected device's name
                    connectedDeviceName = msg.getData().getString(DEVICE_NAME);
                    Toast.makeText(getApplicationContext(), "Connected to "
                            + connectedDeviceName, Toast.LENGTH_SHORT).show();
                    boolean server = msg.getData().getBoolean(IS_SERVER);
                    showDialog();
                    if (server) {
                        Util.debug("*** SERVER ***");
                        sendAvailable();
                    } else {
                        Util.debug("*** CLIENT ***");
                        // todo - test
                        sendAvailable();
                    }
                    break;
                }
                case MESSAGE_TOAST:
                {
                    Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST), Toast.LENGTH_SHORT).show();
                    break;
                }

            }
        }
    };

    private AlertDialog dialog;
    private ProgressBar sendBar, receiveBar;

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.sync_progress, null);
        sendBar = (ProgressBar) layout.findViewById(R.id.sync_send_progress);
        receiveBar = (ProgressBar) layout.findViewById(R.id.sync_receive_progress);
        builder.setView(layout);
        builder.setNegativeButton(R.string.sync_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                // todo - cancel sync
            }
        });
        dialog = builder.show();
    }

    private void sendAvailable() {
        final ArrayList<String> syncList = Database.getDB().getSyncList();
        sendMessage(CODE_AVAILABLE_KEYS, syncList);
    }

    private void sendRequested(ArrayList<String> keys) {
        final ArrayList<String> filtered = Database.getDB().filterSyncList(keys);
        // Set how many we expect to receive
        receiveBar.setMax(filtered.size());
        sendMessage(CODE_REQUESTED_KEYS, filtered);
    }

    private void sendPosts(ArrayList<String> keys) {
        sendBar.setMax(keys.size());
        ArrayList<Post> posts = new ArrayList<Post>();
        for (String key : keys) {
            Post post = Database.getDB().getPost(key);
            posts.add(post);
        }
        // todo - send posts one at a time, otherwise too much memory wasted.
        Post[] postArray = posts.toArray(new Post[0]);
        sendMessage(CODE_POSTS, postArray);
    }

    private void receive(int code, ArrayList<String> keys) {
        if (code == CODE_AVAILABLE_KEYS) {
            Util.debug("PROTOCOL: Received list of available keys.");
            sendRequested(keys);
        } else if (code == CODE_REQUESTED_KEYS) {
            Util.debug("PROTOCOL: Received list of requested keys.");
            sendPosts(keys);
        }
    }

    private void receive(int code, Post[] posts) {
        Util.debug("PROTOCOL: Received requested posts.");
        final Database db = Database.getDB();
        for (Post post : posts) {
            Util.debug("Receiving post: " + post.getKey() + ": '" + post.getText() + "'.");
            post.setScore(post.getScore() * Constants.POINTS_DECAY / Constants.POINTS_DENOMINATOR);
            db.addPost(post);
        }
        Toast.makeText(getApplicationContext(), "Sync complete.", Toast.LENGTH_SHORT).show();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (D) {
            Log.d(TAG, "onActivityResult " + resultCode);
        }
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupChat();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(address);
        // Attempt to connect to the device
        service.connect(device, secure);
    }

    // Menu disabled
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater inflater = getMenuInflater();
//        inflater.inflate(R.menu.bluetooth_menu, menu);
//        return true;
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        switch (item.getItemId()) {
            case R.id.secure_connect_scan:
                // Launch the DeviceListActivity to see devices and do scan
                serverIntent = new Intent(this, DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
                return true;
            case R.id.discoverable:
                // Ensure this device is discoverable by others
                ensureDiscoverable();
                // Start listening mode
                service.startServer();
                return true;
        }
        return false;
    }

}
