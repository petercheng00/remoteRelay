package com.pc.remoterelay;

import java.util.ArrayList;
import java.util.List;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RemoteRelayActivity extends Activity {
	private static final String TAG = "RemoteRelayActivity";
	
	/**
	 * Unique identifier for notification 
	 */
	private static final int NOTIFICATION_ID = 1111;
	
	/**
	 * Unique broadcast code
	 */
	private static final int BROADCAST_CODE = 1212;
	
	/**
	 * Request code for asking Android to enable bluetooth
	 */
	private static final int REQUEST_ENABLE_BT = 1;

	/**
	 * Text for name of selected adapter. Color indicates connection status 
	 */
	private TextView btStatus;
	
	/**
	 * Buttons to connect/disconnect
	 */
	private Button connectButton;
	private Button disconnectButton;
	
	/**
	 * Button will get correct on/off state from arduino 
	 */
	private Button onoffButton;
	
	/**
	 * True if relay currently set to on
	 */
	private boolean isRelayOn;
	
	/**
	 * Handles bluetooth actions
	 */
	private BluetoothAdapter btAdapter;
	
	/**
	 * Service for bluetooth communication
	 */
	private BluetoothService btService;
	
	/**
	 * For setting up notification control
	 */
	private NotificationManager notificationManager;
	
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_remote_relay);
		// keep screen on for easier debugging. Perhaps remove later
		getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
		
		setupStatus();
		setupButtons();
		setupBluetooth();
		setupNotification();
	}
	
	/**
	 * Select a bluetooth device
	 */
	private void selectDevice() {
		final List<BluetoothDevice> pairedDevices = new ArrayList<BluetoothDevice>();
		pairedDevices.addAll(btAdapter.getBondedDevices());
		final CharSequence[] deviceNames = new CharSequence[pairedDevices.size()+1];
		for (int i = 0; i < pairedDevices.size(); ++i) {
			deviceNames[i] = pairedDevices.get(i).getName();
		}
		deviceNames[deviceNames.length-1] = "Scan for more devices";
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select a device");
		builder.setItems(deviceNames, new DialogInterface.OnClickListener(){			
			@Override
			public void onClick(DialogInterface dialog, int index) {
				if (index == deviceNames.length - 1) {
					scanForDevices();
				}
				else {
					btService.setDevice(pairedDevices.get(index));
				}
				btStatus.setText(btService.getDevice().getName());
			}
		});
		builder.create().show();
	}
	
	/**
	 * Scan for non-paired devices
	 */
	private void scanForDevices() {
		//todo!
	}
	
	/**
	 * Disconnect bluetooth
	 */
	private void disconnect() {
		btService.stop();
		connectButton.setEnabled(true);
		disconnectButton.setEnabled(false);
		onoffButton.setEnabled(false);
	}
	
	/**
	 * Update views when bluetooth is disconnected
	 */
	private void onDisconnected() {
		connectButton.setEnabled(true);
		disconnectButton.setEnabled(false);
		onoffButton.setEnabled(false);		
	}
		
	/**
	 * Connect to bluetooth
	 */
	private void connect() {
		btService.connect();
		connectButton.setEnabled(false);
		disconnectButton.setEnabled(false);
		onoffButton.setEnabled(false);
	}
	
	/**
	 * Update views when bluetooth is connected
	 */
	private void onConnected() {
		// get onoff state
		btService.write("getstate".getBytes());
		connectButton.setEnabled(false);
		disconnectButton.setEnabled(true);
		onoffButton.setEnabled(true);
	}
	
	/**
	 * Toggle relay state
	 */
	private void toggleRelay() {
		if (isRelayOn) {
			btService.write("0".getBytes());
		}
		else {
			btService.write("1".getBytes());
		}
	}
	
	/**
	 * Setup status view
	 */
	private void setupStatus() {
		btStatus = (TextView) this.findViewById(R.id.bt_name);
		btStatus.setText("No device selected");
		btStatus.setTextColor(Color.RED);
		btStatus.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				selectDevice();
			}
		});
	}
	
	/**
	 * Setup buttons
	 */
	private void setupButtons() {
		connectButton = (Button) this.findViewById(R.id.button_connect);
		disconnectButton = (Button) this.findViewById(R.id.button_disconnect);
		onoffButton = (Button) this.findViewById(R.id.button_onoff);
		disconnectButton.setEnabled(false);
		onoffButton.setEnabled(false);
		connectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				connect();
			}
		});
		disconnectButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				disconnect();
			}
		});
		onoffButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				toggleRelay();
			}
		});
	}
	
	/**
	 * Setup bluetooth
	 */
	private void setupBluetooth() {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        // If the adapter is null, then Bluetooth is not supported
        if (btAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (!btAdapter.isEnabled()) {
        	Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        	startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        btService = new BluetoothService(this, new BTHandler());
	}
	
	/**
	 * Setup notification controls
	 * 
	 * TODO: Notification is way bigger than it needs to be
	 * move to service
	 */
	private void setupNotification() {
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		builder.setAutoCancel(false);
		builder.setDefaults(Notification.DEFAULT_ALL);
		builder.setWhen(System.currentTimeMillis());
		builder.setContentTitle("Remote Relay");
		builder.setSmallIcon(R.drawable.ic_launcher);
		builder.setTicker("Remote Relay");
		builder.setPriority(NotificationCompat.PRIORITY_HIGH);
		builder.setOngoing(true);
		
		Intent intent = new Intent(this, RemoteRelayActivity.class);
		PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.setContentIntent(pendingIntent);

		Intent offIntent = new Intent(this, BluetoothService.class);
		offIntent.setAction(BluetoothService.OFF_CODE);
		PendingIntent offPendingIntent = PendingIntent.getBroadcast(this, BROADCAST_CODE, offIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.addAction(android.R.drawable.ic_media_pause, "Off", offPendingIntent);
		Intent onIntent = new Intent(this, BluetoothService.class);
		onIntent.setAction(BluetoothService.ON_CODE);
		PendingIntent onPendingIntent = PendingIntent.getBroadcast(this, BROADCAST_CODE, onIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.addAction(android.R.drawable.ic_media_play, "On", onPendingIntent);

		NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		manager.notify(TAG, NOTIFICATION_ID, builder.build());
	}
	
	
	/**
	 * Class to respond to bluetooth messages
	 * @author Peter
	 *
	 */
	private class BTHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                	onConnected();
                	btStatus.setTextColor(Color.GREEN);
                    break;
                case BluetoothService.STATE_CONNECTING:
                	btStatus.setTextColor(Color.YELLOW);
                    break;
                case BluetoothService.STATE_NONE:
                	onDisconnected();
                	btStatus.setTextColor(Color.RED);
                    break;
                }
                break;
            case BluetoothService.MESSAGE_DEVICE_NAME:
                // save the connected device's name
                String mConnectedDeviceName = msg.getData().getString(BluetoothService.DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case BluetoothService.MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(BluetoothService.TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            case BluetoothService.MESSAGE_READ:
            	byte[] rawdata = new byte[msg.arg1];
            	System.arraycopy(msg.obj, 0, rawdata, 0, msg.arg1);
            	String contents = new String(rawdata);
            	Log.e(TAG, "reading " + contents);
            	isRelayOn = (contents.equals("1"));
            	if (isRelayOn) {
            		onoffButton.setText("Turn Off");
            	}
            	else {
            		onoffButton.setText("Turn On");
            	}
            	break;
            }
        }
    };  
}
