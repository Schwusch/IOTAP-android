package com.example.schwusch.iotap_app;

import android.app.IntentService;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

/**
 * Created by Jonathan Böcker on 2016-11-15.
 *
 */

    public class DataCollectorService extends IntentService {
    BluetoothAdapter mBluetoothAdapter;
    BluetoothSocket mmSocket;
    BluetoothDevice mmDevice;
    InputStream mmInputStream;

    public DataCollectorService(){
        super("DataCollectorService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        if (findBT() && openBT())
            collectData();
    }

    private boolean findBT() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Check if adapter exists and is enabled
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            Intent localIntent = new Intent(Constants.IOTAP_GUI)
                    .putExtra(Constants.EXTENDED_DATA_STATUS, "Bluetooth Not Enabled!");
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        } else if (mBluetoothAdapter != null && mBluetoothAdapter.isEnabled()) {
            Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
            if (pairedDevices.size() > 0) {
                for (BluetoothDevice device : pairedDevices) {
                    if (device.getName().equals(Constants.BT_DEVICE_NAME)) {
                        mmDevice = device;
                        // Correct paired device is found
                        return true;
                    }
                }
                if (mmDevice == null) {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(
                            new Intent(Constants.IOTAP_GUI).putExtra(
                                    Constants.EXTENDED_DATA_STATUS,
                                    "No paired bluetooth device named " +
                                            Constants.BT_DEVICE_NAME +
                                            " on this phone."
                            )
                    );
                }
            }
        }

        return false;
    }

    private boolean openBT() {
        try {
            mmSocket = mmDevice.createRfcommSocketToServiceRecord(
                    UUID.fromString(Constants.BT_SERVICE_UUID)
            );
            mmSocket.connect();
            mmInputStream = mmSocket.getInputStream();

        } catch (Exception e){
            e.printStackTrace();
            Intent localIntent = new Intent(Constants.IOTAP_GUI)
                    .putExtra(Constants.EXTENDED_DATA_STATUS, "Bluetooth Error...");
            LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

            return false;
        }

        Intent localIntent = new Intent(Constants.IOTAP_GUI)
                .putExtra(Constants.EXTENDED_DATA_STATUS, "Bluetooth Opened!");
        LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);

        return true;
    }

    private void collectData(){
        boolean stopWorker = false;
        int readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];
        final byte ENDLINE = 10;

        // Vibrate to indicate things went well :P
        long[] pattern = {0,75,150,75};
        ((Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE)).vibrate(pattern, -1);

        while(!Thread.currentThread().isInterrupted() && !stopWorker)
        {
            try {
                int bytesAvailable = mmInputStream.available();

                if(bytesAvailable > 0) {
                    byte[] packetBytes = new byte[bytesAvailable];
                    mmInputStream.read(packetBytes);

                    for(int i=0;i<bytesAvailable;i++) {
                        byte b = packetBytes[i];
                        if(b == ENDLINE) {
                            byte[] encodedBytes = new byte[readBufferPosition];
                            System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                            final String data = new String(encodedBytes, "US-ASCII");
                            readBufferPosition = 0;
                            // Post received data to Gesture Detector
                            LocalBroadcastManager.getInstance(this).sendBroadcast(
                                    new Intent(Constants.IOTAP_GD)
                                            .putExtra(Constants.EXTENDED_DATA_STATUS, data)
                            );

                        } else {
                            readBuffer[readBufferPosition++] = b;
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                stopWorker = true;
                //Notifying main activity that connection error occured.
                Intent localIntent = new Intent(Constants.IOTAP_GUI)
                        .putExtra(Constants.EXTENDED_DATA_STATUS, "Connection Error");
                LocalBroadcastManager.getInstance(this).sendBroadcast(localIntent);
            }
        }
    }
}
