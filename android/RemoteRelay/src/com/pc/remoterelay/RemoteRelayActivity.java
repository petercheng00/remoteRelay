package com.pc.remoterelay;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class RemoteRelayActivity extends Activity {
	
	
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
	private Boolean isRelayOn;
	
	/**
	 * Handles bluetooth actions
	 */
	private BluetoothAdapter btAdapter;
	
	private BluetoothDevice btDevice;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_remote_relay);
		// keep screen on for easier debugging. Perhaps remove later
		getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setupStatus();
		setupButtons();
		setupBluetooth();
	}
	
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
					btDevice = pairedDevices.get(index);
				}
				btStatus.setText(btDevice.getName());
			}
		});
		builder.create().show();
	}
	
	private void scanForDevices() {
		
	}
	
	private void disconnect() {
		connectButton.setEnabled(true);
		disconnectButton.setEnabled(false);
		onoffButton.setEnabled(false);
	}
		
	private void connect() {
		connectButton.setEnabled(false);
		disconnectButton.setEnabled(false);
		onoffButton.setEnabled(false);
	}
	
	private void onConnected() {
		// get onoff state
		connectButton.setEnabled(false);
		disconnectButton.setEnabled(true);
		onoffButton.setEnabled(true);
	}
	
	private void toggleRelay() {
	}
	
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
	
	
	private class BTHandler extends Handler
    {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BluetoothService.MESSAGE_STATE_CHANGE:
                switch (msg.arg1) {
                case BluetoothService.STATE_CONNECTED:
                	btStatus.setTextColor(Color.GREEN);
                    break;
                case BluetoothService.STATE_CONNECTING:
                	btStatus.setTextColor(Color.YELLOW);
                    break;
                case BluetoothService.STATE_NONE:
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
            }
        }
    };  
}
