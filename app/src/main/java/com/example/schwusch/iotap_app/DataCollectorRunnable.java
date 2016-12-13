package com.example.schwusch.iotap_app;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

import java.io.InputStream;
import java.util.Set;
import java.util.UUID;

public class DataCollectorRunnable implements Runnable {
    private MainActivity mainActivity;
    private BluetoothDevice mmDevice;
    private InputStream mmInputStream;
    private GestureDetector detector;

    DataCollectorRunnable(MainActivity mainActivity) throws Exception {
        this.mainActivity = mainActivity;
    }

    @Override
    public void run() {
        if (findBT() && openBT()) {
            try {
                collectData();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean findBT() {
        BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Check if adapter exists and is enabled
        if (mBluetoothAdapter != null && !mBluetoothAdapter.isEnabled()) {
            mainActivity.runOnUiThread(() -> mainActivity.btMessage("Bluetooth Disabled!", true));
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
                    mainActivity.runOnUiThread(
                            () -> mainActivity.btMessage(
                                    "No Paired Device named " + Constants.BT_DEVICE_NAME, true
                            )
                    );
                }
            }
        }

        return false;
    }

    private boolean openBT() {
        try {
            BluetoothSocket mmSocket = mmDevice.createRfcommSocketToServiceRecord(
                    UUID.fromString(Constants.BT_SERVICE_UUID));
            mmSocket.connect();
            mmInputStream = mmSocket.getInputStream();

        } catch (Exception e) {
            mainActivity.runOnUiThread(() -> mainActivity.btMessage("Can't Connect To " + Constants.BT_DEVICE_NAME + "!", true));
            return false;
        }
        mainActivity.runOnUiThread(() -> mainActivity.btMessage(null, false));
        return true;
    }

    private void collectData() throws Exception {
        this.detector = new GestureDetector(mainActivity);
        boolean stopWorker = false;
        int readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];
        final byte ENDLINE = 10;

        while (!Thread.currentThread().isInterrupted() && !stopWorker) {
            try {
                int bytesAvailable = mmInputStream.available();

                if (bytesAvailable > 0) {
                    byte[] packetBytes = new byte[bytesAvailable];
                    mmInputStream.read(packetBytes);

                    for (int i = 0; i < bytesAvailable; i++) {
                        byte b = packetBytes[i];
                        if (b == ENDLINE) {
                            byte[] encodedBytes = new byte[readBufferPosition];
                            System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                            readBufferPosition = 0;
                            detector.addSample(new String(encodedBytes, "US-ASCII"));

                        } else {
                            readBuffer[readBufferPosition++] = b;
                        }
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                stopWorker = true;
                mainActivity.runOnUiThread(() -> mainActivity.btMessage("Bluetooth Stream lost!", true));
            }
        }
    }
}
