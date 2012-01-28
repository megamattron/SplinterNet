package com.larvalabs.sneaker;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.*;
import java.util.ArrayList;
import java.util.UUID;

/**
 * @author John Watkinson
 */
public class SyncActivity extends Activity {

    // Magic number with which to begin communications.
    public static final int MAGIC_NUMBER = 0xFABFAB01;

    public static final int DISCOVERABLE_MINUTES = 300;

    // Protocol codes
    public static final int CODE_AVAILABLE_KEYS = 1;
    public static final int CODE_REQUESTED_KEYS = 2;
    public static final int CODE_POSTS = 3;
    public static final int CODE_DISCONNECT = 4;

    // Message types sent via handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Message to Device List Activity
    private static final int REQUEST_CONNECT_DEVICE_SECURE = 1;
    private static final int REQUEST_ENABLE_BLUETOOTH = 3;

    private static final String NAME_SECURE = "SplinterNet";

    private static final UUID MY_UUID_SECURE = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");
    private Button listenButton;
    private Button connectButton;
    private TextView nameView;

    private enum AdapterState {
        OFF,
        READY,
        LISTEN,
        CONNECTING,
        CONNECTED
    }

    ;

    // Local Bluetooth adapter
    private BluetoothAdapter adapter = null;
    private AdapterState state;
    private Handler uiHandler = new Handler();
    private HandlerThread bgHandlerThread;
    private Handler bgHandler;

    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;

    private boolean sendingDone;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        bgHandlerThread = new HandlerThread("Background Sync Handler");
        bgHandlerThread.start();
        bgHandler = new Handler(bgHandlerThread.getLooper());

        // Set up the window layout
        setContentView(R.layout.sync_layout);

        final Button panicButton = (Button) findViewById(R.id.sync_panic);
        panicButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Util.handlePanic(SyncActivity.this);
            }
        });
        final Button listButton = (Button) findViewById(R.id.sync_list);
        listButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                finish();
            }
        });
        listenButton = (Button) findViewById(R.id.sync_listen);
        listenButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (state == AdapterState.LISTEN) {
                    stopServer();
                } else {
                    startServer();
                }

            }
        });
        connectButton = (Button) findViewById(R.id.sync_connect);
        connectButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                stopServer();
                startClient();
            }
        });
        nameView = (TextView) findViewById(R.id.sync_device_name);
        nameView.setVisibility(View.GONE);
        // Get local Bluetooth adapter
        adapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (adapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        // If BT is not on, request that it be enabled.
        if (!adapter.isEnabled()) {
            setState(AdapterState.OFF);
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BLUETOOTH);
            // Otherwise, setup the chat session
        } else {
            setState(AdapterState.READY);
            // Ready to go
            setDeviceName();
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_SECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data, true);
                }
                break;
            case REQUEST_ENABLE_BLUETOOTH:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setState(AdapterState.READY);
                    setDeviceName();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
        }
    }

    private void connectDevice(Intent data, boolean secure) {
        // Get the device MAC address
        String address = data.getExtras().getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        // Get the BluetoothDevice object
        BluetoothDevice device = adapter.getRemoteDevice(address);
        // Attempt to connect to the device
        connect(device);
    }

    public synchronized void setState(AdapterState state) {
        Util.debug("--> SETTING STATE TO: " + state);
        this.state = state;
        // todo - notify
    }

    public synchronized void connect(BluetoothDevice device) {

        // Cancel any thread attempting to make a connection
        if (state == AdapterState.CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Start the thread to connect with the given device
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(AdapterState.CONNECTING);
    }

    private void setDeviceName() {
        String name = adapter.getName();
        nameView.setText(name);
    }

    public void startServer() {
        // First -- do discoverable
        synchronized (this) {
            setState(AdapterState.LISTEN);
        }
        if (adapter.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, DISCOVERABLE_MINUTES);
            startActivity(discoverableIntent);
        }
        mAcceptThread = new AcceptThread();
        mAcceptThread.start();
        nameView.setVisibility(View.VISIBLE);
        listenButton.setBackgroundResource(R.drawable.sync_animation);
        AnimationDrawable frameAnimation = (AnimationDrawable) listenButton.getBackground();
        // Start the animation (looped playback by default).
        frameAnimation.start();
        connectButton.setVisibility(View.GONE);
        // Next, start listening thread
    }

    public void stopServer() {
        synchronized (this) {
            setState(AdapterState.READY);
        }
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
        }
        listenButton.setBackgroundResource(R.drawable.sync_listen_but2);
        connectButton.setVisibility(View.VISIBLE);
        nameView.setVisibility(View.GONE);
    }

    public void startClient() {
        Intent serverIntent = new Intent(this, DeviceListActivity.class);
        startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE);
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket serverSocket;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;

            Util.debug("Accept thread starting.");
            // Create a new listening server socket
            try {
                tmp = adapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, MY_UUID_SECURE);
            } catch (IOException e) {
                Util.error("Socket listen() failed", e);
            }
            serverSocket = tmp;
        }

        public void run() {
            setName("AcceptThread");

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (state != AdapterState.CONNECTED) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = serverSocket.accept();
                    Util.debug("Accept thread connected.");
                } catch (IOException e) {
                    Util.error("Socket accept() failed", e);
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    connected(socket, socket.getRemoteDevice(), true);
                }
            }
        }

        public void cancel() {
            try {
                serverSocket.close();
            } catch (IOException e) {
                Util.error("Socket close() of server failed", e);
            }
        }
    }

    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
//                Method m = device.getClass().getMethod("createRfcommSocket", new Class[] { int.class });
//                tmp = (BluetoothSocket) m.invoke(device, 1);
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID_SECURE);
            } catch (Exception e) {
                e.printStackTrace();
                Util.error("Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            adapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Util.error("unable to close() socket during connection failure", e2);
                }
                uiHandler.post(new Runnable() {
                    public void run() {
                        connectionFailed();
                    }
                });
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (SyncActivity.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice, false);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Util.error("close() of connect socket failed", e);
            }
        }
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        // Send a failure message back to the Activity
        Toast.makeText(this, "Connection failed.", Toast.LENGTH_LONG).show();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        // Send a failure message back to the Activity
        Toast.makeText(this, "Connection lost.", Toast.LENGTH_LONG).show();
    }

    private void syncComplete() {
        // Send a failure message back to the Activity
        Toast.makeText(this, "Sync complete.", Toast.LENGTH_LONG).show();
        finish();
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final DataInputStream mmInStream;
        private final DataOutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Util.error("temp sockets not created", e);
            }

            mmInStream = new DataInputStream(tmpIn);
            mmOutStream = new DataOutputStream(tmpOut);
//            mmInStream = new DataInputStream(new BufferedInputStream(tmpIn, 8192));
//            mmOutStream = new DataOutputStream(new BufferedOutputStream(tmpOut, 8192));
        }

        public DataOutputStream getOutputStream() {
            return mmOutStream;
        }

        public void run() {

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    int magic = mmInStream.readInt();
                    if (magic != MAGIC_NUMBER) {
                        throw new IOException("Protocol error, aborting.");
                    }
                    int code = mmInStream.readInt();
                    int n = 0;
                    if (code != CODE_DISCONNECT) {
                        n = mmInStream.readInt();
                    }
                    if (code == CODE_AVAILABLE_KEYS) {
                        Util.debug("* RECEIVED AVAILABLE KEYS");
                        final ArrayList<String> keys = new ArrayList<String>(n);
                        for (int i = 0; i < n; i++) {
                            keys.add(mmInStream.readUTF());
                        }
                        bgHandler.post(new Runnable() {
                            public void run() {
                                processKeys(keys);
                            }
                        });
                    } else if (code == CODE_REQUESTED_KEYS) {
                        Util.debug("* RECEIVED REQUESTED KEYS");
                        final ArrayList<String> keys = new ArrayList<String>(n);
                        for (int i = 0; i < n; i++) {
                            keys.add(mmInStream.readUTF());
                        }
                        bgHandler.post(new Runnable() {
                            public void run() {
                                sendPosts(keys);
                            }
                        });
                    } else if (code == CODE_POSTS) {
                        Util.debug("* RECEIVING POSTS");
                        Util.setChangeFlag(SyncActivity.this);
                        Database db = Database.getDB();
                        for (int i = 0; i < n; i++) {
                            Post post = new Post();
                            post.read(mmInStream);
                            Util.debug("Receiving post: " + post.getKey() + ": '" + post.getText() + "'.");
                            post.setScore(post.getScore() * Constants.POINTS_DECAY / Constants.POINTS_DENOMINATOR);
                            if (post.getStatus() == Status.STARRED) {
                                post.setScore(post.getScore() + Constants.POINTS_STAR);
                            }
                            if (post.getScore() > Constants.POINTS_MAX) {
                                post.setScore(Constants.POINTS_MAX);
                            }
                            post.setStatus(Status.UNREAD);
                            db.addPost(post);
                            setReceiveProgress(n, (i+1));
                            // todo - update progress
                        }
                        bgHandler.post(new Runnable() {
                            public void run() {
                                sendDisconnect();
                            }
                        });
                    } else if (code == CODE_DISCONNECT) {
                        boolean canClose;
                        synchronized (this) {
                            canClose = sendingDone;
                        }
                        if (canClose) {
                            cancel();
                        }
                    } else {
                        throw new IOException("Protocol error, invalid code.");
                    }
                } catch (IOException e) {
                    Util.error("disconnected", e);
                    uiHandler.post(new Runnable() {
                        public void run() {
                            hideDialog();
                            if (sendingDone) {
                                syncComplete();
                            } else {
                                connectionLost();
                            }
                        }
                    });
                    // Start the service over to restart listening mode
                    //BluetoothService.this.start();
                    break;
                }
            }
        }

        public void cancel() {
            hideDialog();
            try {
                mmSocket.close();
            } catch (IOException e) {
                Util.error("close() of connect socket failed", e);
            }
        }
    }


    public synchronized void connected(BluetoothSocket socket, final BluetoothDevice device, boolean server) {
        // Cancel the thread that completed the connection
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        // Cancel any thread currently running a connection
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        // Cancel the accept thread because we only want to connect to one device
        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        // Start the thread to manage the connection and perform transmissions
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        // todo - Share this back
        uiHandler.post(new Runnable() {
            public void run() {
                showDialog();
            }
        });
        bgHandler.post(new Runnable() {
            public void run() {
                sendKeys(device.getName());
            }
        });
        // Send the name of the connected device back to the UI Activity
//        Message msg = mHandler.obtainMessage(BluetoothSync.MESSAGE_DEVICE_NAME);
//        Bundle bundle = new Bundle();
//        bundle.putString(BluetoothSync.DEVICE_NAME, device.getName());
//        bundle.putBoolean(BluetoothSync.IS_SERVER, server);
//        msg.setData(bundle);
//        mHandler.sendMessage(msg);

        setState(AdapterState.CONNECTED);
    }

    public void sendKeys(String deviceName) {
        Util.debug("* SENDING AVAILABLE KEYS");
        try {
            final DataOutputStream outputStream = mConnectedThread.getOutputStream();
            final ArrayList<String> syncList = Database.getDB().getSyncList();
            outputStream.writeInt(MAGIC_NUMBER);
            outputStream.writeInt(CODE_AVAILABLE_KEYS);
            outputStream.writeInt(syncList.size());
            for (String key : syncList) {
                outputStream.writeUTF(key);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void processKeys(ArrayList<String> keys) {
        Util.debug("* SENDING REQUESTED KEYS");
        try {
            final DataOutputStream outputStream = mConnectedThread.getOutputStream();
            final ArrayList<String> filtered = Database.getDB().filterSyncList(keys);
            setReceiveProgress(filtered.size(), 0);
            outputStream.writeInt(MAGIC_NUMBER);
            outputStream.writeInt(CODE_REQUESTED_KEYS);
            outputStream.writeInt(filtered.size());
            for (String key : filtered) {
                outputStream.writeUTF(key);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void sendPosts(ArrayList<String> keys) {
        Util.debug("* SENDING POSTS");
        int n = keys.size();
        setSendProgress(n, 0);
        final DataOutputStream outputStream = mConnectedThread.getOutputStream();
        try {
            outputStream.writeInt(MAGIC_NUMBER);
            outputStream.writeInt(CODE_POSTS);
            outputStream.writeInt(keys.size());
            int count = 0;
            for (String key : keys) {
                Post post = Database.getDB().getPost(key);
                post.write(outputStream);
                count++;
                setSendProgress(n, count);
            }
            synchronized (this) {
                sendingDone = true;
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void sendDisconnect() {
        synchronized (this) {
            sendingDone = true;
        }
        Util.debug("* SENDING DISCONNECT");
        final DataOutputStream outputStream = mConnectedThread.getOutputStream();
        try {
            outputStream.writeInt(MAGIC_NUMBER);
            outputStream.writeInt(CODE_DISCONNECT);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    //// Dialog stuff

    private AlertDialog dialog;
    private ProgressBar sendBar, receiveBar;

    public void showDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.sync_progress, null);
        sendBar = (ProgressBar) layout.findViewById(R.id.sync_send_progress);
        receiveBar = (ProgressBar) layout.findViewById(R.id.sync_receive_progress);
        sendBar.setIndeterminate(false);
        receiveBar.setIndeterminate(false);
        sendBar.setMax(1);
        sendBar.setProgress(0);
        receiveBar.setMax(1);
        receiveBar.setProgress(0);
        builder.setView(layout);
        builder.setNegativeButton(R.string.sync_cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                if (mConnectedThread != null) {
                    mConnectedThread.cancel();
                }
            }
        });
        dialog = builder.show();
    }

    public void hideDialog() {
        uiHandler.post(new Runnable() {
            public void run() {
                if (dialog != null) {
                    dialog.hide();
                }
            }
        });
    }

    public void setSendProgress(final int max, final int progress) {
        uiHandler.post(new Runnable() {
            public void run() {
                sendBar.setMax(max);
                sendBar.setProgress(progress);
            }
        });
    }

    public void setReceiveProgress(final int max, final int progress) {
        uiHandler.post(new Runnable() {
            public void run() {
                receiveBar.setMax(max);
                receiveBar.setProgress(progress);
            }
        });
    }

}
