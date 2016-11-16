package com.example.schwusch.iotap_app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.view.View;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends Activity {
    FloatingActionButton fab;
    TextView tvText;
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    OutputStream mmOutputStream;
    static InputStream mmInputStream;
    Intent mServiceIntent;
    ResponseReceiver receiver;

    public void logToTextView(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // UI stuff here in case of calling from another thread
                tvText.setText(text);
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register broadcast receiver for bluetooth error messages
        IntentFilter mStatusIntentFilter = new IntentFilter(Constants.BROADCAST_ACTION);
        mStatusIntentFilter.addDataScheme("http");
        receiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, mStatusIntentFilter);

        tvText = (TextView) findViewById(R.id.tvText);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try
                {
                    findBT();
                    openBT();
                }
                catch (Exception ex) {
                    tvText.setText("An error occured. Check debug logs.");
                    ex.printStackTrace();
                }
            }
        });
    }

    void findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBluetoothAdapter == null) {
            tvText.setText("No bluetooth adapter available");
        }

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }

        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("IOTAP")) {
                    mmDevice = device;
                    break;
                }
            }
        }
        tvText.setText("Bluetooth Device Found");
    }

    void openBT() throws IOException {
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"); //Standard SerialPortService ID
        mmSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
        mmSocket.connect();
        mmOutputStream = mmSocket.getOutputStream();
        mmInputStream = mmSocket.getInputStream();

        beginListenForData();

        tvText.setText("Bluetooth Opened");
    }

    void beginListenForData()
    {
        mServiceIntent = new Intent(this, DataCollectorService.class);
        startService(mServiceIntent);
    }

    // Broadcast receiver for receiving status updates from the DataCollectorService
    private class ResponseReceiver extends BroadcastReceiver
    {
        // Prevents instantiation?
        private ResponseReceiver() {
        }
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            logToTextView(intent.getStringExtra(Constants.EXTENDED_DATA_STATUS));
        }
    }
}
