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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RemoteRelayActivity extends Activity {
	private static final String TAG = "RemoteRelayActivity";
	
	private static final int NOTIFICATION_CODE = 1111;
		
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
	 * Respond to updates from the service
	 */
	private BroadcastReceiver broadcastReceiver;
	
	@Override
	protected void onCreate(Bundle state) {
		super.onCreate(state);
		setContentView(R.layout.activity_remote_relay);
		// keep screen on for easier debugging. Perhaps remove later
		getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		
		setupStatus();
		setupButtons();
		setupBluetooth();
		setupBroadcastReceiver();
		setupNotification();
	}

	@Override
	protected void onResume() {
		super.onResume();
		IntentFilter filter = new IntentFilter();
		filter.addAction(RemoteRelayService.ACTION_READ);
		filter.addAction(RemoteRelayService.ACTION_STATE_CHANGE);
		this.registerReceiver(broadcastReceiver, filter);
	}
	
	@Override
	protected void onPause() {
		this.unregisterReceiver(broadcastReceiver);
		super.onPause();
	}

	
	
	/**
	 * Set active device
	 * @param device
	 */
	private void setDevice(BluetoothDevice device) {
		Intent deviceIntent = new Intent(this, RemoteRelayService.class);
		deviceIntent.setAction(RemoteRelayService.ACTION_SETDEVICE);
		deviceIntent.putExtra(RemoteRelayService.KEY_DEVICE, device);
		this.startService(deviceIntent);
	}
	
	/**
	 * Disconnect bluetooth
	 */
	private void disconnect() {
		Intent disconnectIntent = new Intent(this, RemoteRelayService.class);
		disconnectIntent.setAction(RemoteRelayService.ACTION_DISCONNECT);
		this.startService(disconnectIntent);
	}
	
		
	/**
	 * Connect to bluetooth
	 */
	private void connect() {
		Intent connectIntent = new Intent(this, RemoteRelayService.class);
		connectIntent.setAction(RemoteRelayService.ACTION_CONNECT);
		this.startService(connectIntent);
	}

	/**
	 * Update views when bluetooth is disconnected
	 */
	private void onDisconnected() {
		connectButton.setEnabled(true);
		disconnectButton.setEnabled(false);
		onoffButton.setEnabled(false);
		btStatus.setTextColor(Color.RED);
	}
	
	/**
	 * Update views when bluetooth is connecting
	 */
	private void onConnecting() {
		connectButton.setEnabled(false);
		disconnectButton.setEnabled(true);
		onoffButton.setEnabled(false);
		btStatus.setTextColor(Color.YELLOW);
	}
	/**
	 * Update views when bluetooth is connected
	 */
	private void onConnected() {
		connectButton.setEnabled(false);
		disconnectButton.setEnabled(true);
		onoffButton.setEnabled(true);
		btStatus.setTextColor(Color.GREEN);
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
		deviceNames[deviceNames.length-1] = "Scan (not supported yet)";
		
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setTitle("Select a device");
		builder.setItems(deviceNames, new DialogInterface.OnClickListener(){			
			@Override
			public void onClick(DialogInterface dialog, int index) {
				if (index == deviceNames.length - 1) {
					// TODO: scan for devices
				}
				else {
					setDevice(pairedDevices.get(index));
					btStatus.setText(deviceNames[index]);
				}
			}
		});
		builder.create().show();
	}
	
	/**
	 * Toggle relay state
	 */
	private void toggleRelay() {
		Intent commandIntent = new Intent(this, RemoteRelayService.class);
		commandIntent.setAction(RemoteRelayService.ACTION_COMMAND);
		if (isRelayOn) {
			commandIntent.putExtra(RemoteRelayService.KEY_COMMAND, "0");
		}
		else {
			commandIntent.putExtra(RemoteRelayService.KEY_COMMAND, "1");
		}
		this.startService(commandIntent);
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
	}
	
	/**
	 * Handle state changes reported by service
	 * @param state
	 */
	private void handleStateChange(int state) {
		switch (state) {
			case RemoteRelayService.STATE_NONE:
				onDisconnected();
				break;
			case RemoteRelayService.STATE_CONNECTING:
				onConnecting();
				break;
			case RemoteRelayService.STATE_CONNECTED:
				onConnected();
				break;
			default:
				Log.e(TAG, "unexpected state change intent");
		}
	}
	
	/**
	 * Handle messages from service
	 * @param msg
	 */
	private void handleMessage(String msg) {
		if (msg.equals("1")) {
			isRelayOn = true;
			onoffButton.setText("Turn off");
		}
		else if (msg.equals("0")) {
			isRelayOn = false;
			onoffButton.setText("Turn on");
		}
		else {
			Log.e(TAG, "unexpected message from intent");
		}
	}

	/**
	 * Setup broadcast receiver to listen to service
	 */
	private void setupBroadcastReceiver() {
		broadcastReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				switch (intent.getAction()) {
				case RemoteRelayService.ACTION_STATE_CHANGE:
					handleStateChange(intent.getIntExtra(RemoteRelayService.KEY_STATE, -1));
					break;
				case RemoteRelayService.ACTION_READ:
					handleMessage(intent.getStringExtra(RemoteRelayService.KEY_MESSAGE));
					break;
				default:
					Log.e(TAG, "unexpected intent action received");
				}
			}
		};
	}
	
	/**
	 * Setup notification controls
	 * 
	 * TODO: Notification is way bigger than it needs to be
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
		
		Intent offIntent = new Intent(this, RemoteRelayService.class);
		offIntent.setAction(RemoteRelayService.ACTION_COMMAND);
		offIntent.putExtra(RemoteRelayService.KEY_COMMAND, "0");
		PendingIntent offPendingIntent = PendingIntent.getService(this, 123, offIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.addAction(android.R.drawable.ic_media_pause, "Off", offPendingIntent);
		
		Intent onIntent = new Intent(this, RemoteRelayService.class);
		onIntent.setAction(RemoteRelayService.ACTION_COMMAND);
		onIntent.putExtra(RemoteRelayService.KEY_COMMAND, "1");
		PendingIntent onPendingIntent = PendingIntent.getService(this, 321, onIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		builder.addAction(android.R.drawable.ic_media_play, "On", onPendingIntent);

		NotificationManager manager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		manager.notify(TAG, NOTIFICATION_CODE, builder.build());
	}
}
