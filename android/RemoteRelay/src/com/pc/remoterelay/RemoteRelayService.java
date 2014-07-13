package com.pc.remoterelay;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class RemoteRelayService extends Service {
	
	protected static final String TAG = "RemoteRelayService";
	
    // Randomly generated unique UUID for this application
	private static final UUID BT_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
	
	// Incoming intent actions
	public static final String ACTION_SETDEVICE = "com.pc.remoterelay.action.setdevice";
	public static final String ACTION_CONNECT = "com.pc.remoterelay.action.connect";
	public static final String ACTION_DISCONNECT = "com.pc.remoterelay.action.disconnect";
	public static final String ACTION_COMMAND = "com.pc.remoterelay.action.command";
	
	// Outgoing intent actions
	public static final String ACTION_STATE_CHANGE = "com.pc.remoterelay.action.statechange";
	public static final String ACTION_READ = "com.pc.remoterelay.action.read";
	
	// Intent keys
	public static final String KEY_DEVICE = "device";
	public static final String KEY_COMMAND = "command";
	public static final String KEY_MESSAGE = "message";
	public static final String KEY_STATE = "state";
	
    // Constants that indicate the current connection state
    private int currentState;
    public static final int STATE_NONE = 0;       // we're doing nothing
    public static final int STATE_CONNECTING = 1; // now initiating an outgoing connection
    public static final int STATE_CONNECTED = 2;  // now connected to a remote device
	
	// Bluetooth Information
    private BluetoothAdapter btAdapter;
	private BluetoothDevice btDevice;
	
	// Connection threads
	private ConnectThread connectThread;
	private ConnectedThread connectedThread;

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		currentState = STATE_NONE;
	}
	
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		handleCommand(intent);
		return START_STICKY;
	}
	
	/**
	 * Handle incoming commands
	 * @param intent
	 */
	private void handleCommand(Intent intent) {
		Log.i(TAG, "Got action: " + intent.getAction());
		switch (intent.getAction()) {
			case ACTION_SETDEVICE:
				btDevice = intent.getParcelableExtra(KEY_DEVICE);
				break;
			case ACTION_CONNECT:
				connectToDevice();
				break;
			case ACTION_DISCONNECT:
				disconnectFromDevice();
				break;
			case ACTION_COMMAND:
				sendCommandToDevice(intent.getStringExtra(KEY_COMMAND));
				break;
			default:
				Log.e(TAG, "Unexpected Action: " + intent.getAction());
		}
	}
	
	/**
	 * Broadcast message to ui elements
	 * @param message
	 */
	private void sendMessage(String message) {
		Intent responseIntent = new Intent(ACTION_READ);
		responseIntent.putExtra(KEY_MESSAGE, message);
		sendBroadcast(responseIntent);
	}
	
	/**
	 * Set connection state
	 * @param state
	 */
	private void setState(int state) {
		currentState = state;
		Intent stateIntent = new Intent(ACTION_STATE_CHANGE);
		stateIntent.putExtra(KEY_STATE, state);
		sendBroadcast(stateIntent);
	}
	
	/**
	 * Connect to currently active device
	 */
	private synchronized void connectToDevice() {
		if (this.btDevice == null) {
			setState(STATE_NONE);
			return;
		}
        // Cancel any thread attempting to make a connection
        if (currentState == STATE_CONNECTING) {
            if (connectThread != null) {connectThread.cancel(); connectThread = null;}
        }

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}

        // Start the thread to connect with the given device
        connectThread = new ConnectThread();
        connectThread.start();
        setState(STATE_CONNECTING);
	}
	
	/**
	 * Disconnect bluetooth
	 */
	private synchronized void disconnectFromDevice() {
        // Cancel the thread that completed the connection
        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}
        setState(STATE_NONE);
	}
	

    /**
     * Send command to connected device
     * @param command
     */
    public void sendCommandToDevice(String command) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (currentState != STATE_CONNECTED) return;
            r = connectedThread;
        }
        // Perform the write unsynchronized
        r.write(command.getBytes());
    }
	
    /**
     * Indicate that the connection attempt failed
     */
    private void connectionFailed() {
    	setState(STATE_NONE);
    }
    
    /**
     * Start the ConnectedThread to begin managing a Bluetooth connection
     * @param socket  The BluetoothSocket on which the connection was made
     * @param device  The BluetoothDevice that has been connected
     */
    public synchronized void connected(BluetoothSocket socket) {
        // Cancel the thread that completed the connection
        if (connectThread != null) {connectThread.cancel(); connectThread = null;}

        // Cancel any thread currently running a connection
        if (connectedThread != null) {connectedThread.cancel(); connectedThread = null;}


        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        // Send the name of the connected device back to the UI Activity
        setState(STATE_CONNECTED);
        
        // get current state of relay, which is returned on any request
        sendCommandToDevice("asdf");
        
    }
	
	
	/**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;

        public ConnectThread() {
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = btDevice.createRfcommSocketToServiceRecord(
                            BT_UUID);
            } catch (IOException e) {
                Log.e(TAG, "Socket create() failed", e);
            }
            mmSocket = tmp;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectThread");

            // Always cancel discovery because it will slow down a connection
            btAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
            	Log.e(TAG, "something failed");
            	Log.e(TAG, e.getMessage());
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    Log.e(TAG, "unable to close() socket during connection failure", e2);
                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (RemoteRelayService.this) {
                connectThread = null;
            }

            // Start the connected thread
            connected(mmSocket);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {
            Log.d(TAG, "create ConnectedThread");
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "BEGIN mConnectedThread");
            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (true) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);
                    byte[] msgBytes = new byte[bytes];
                    System.arraycopy(buffer, 0, msgBytes, 0, bytes);
                    
                    String msg = new String(msgBytes, "UTF-8");
                    sendMessage(msg);
                } catch (IOException e) {
                    Log.e(TAG, "disconnected", e);
                    connectionFailed();
                    break;
                }
            }
        }

        /**
         * Write to the connected OutStream.
         * @param buffer  The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}