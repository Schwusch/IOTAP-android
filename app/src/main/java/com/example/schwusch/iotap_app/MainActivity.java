package com.example.schwusch.iotap_app;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {
    FloatingActionButton fab;
    BluetoothAdapter mBluetoothAdapter;
    ConnectThread mConnectThread;
    BluetoothDevice mDevice;
    ConnectedThread ct;
    TextView tvText;
    StringBuilder log = new StringBuilder();

    protected void logToTextView(final String text) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                log.append(text + "\n");
                tvText.setText(log.toString());
            }
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvText = (TextView) findViewById(R.id.tvText);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG).setAction("Action", null).show();
                String msg = "*";
                if (ct != null) ct.write(msg.getBytes());
            }
        });

        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        // Check if bluetooth is enabled
        if (!mBluetoothAdapter.isEnabled()) {
            // Ask to enable
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            logToTextView("Asking for bluetooth?");
            startActivityForResult(enableBtIntent, 1);
        } else {
            logToTextView("Bluetooth enabled :D");
        }

        // Find the "wristband" and connect to it
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        if (pairedDevices.size() > 0) {
            for (BluetoothDevice device : pairedDevices) {
                if (device.getName().equals("IOTAP")) {
                    logToTextView("Found IOTAP paired device in list.");
                    mDevice = device;
                    mConnectThread = new ConnectThread(mDevice);
                    mConnectThread.start();
                }
            }
        }

        if (mConnectThread == null) {
            logToTextView("No arduino was found...");
        }

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

//-------------------------- PRIVATE CLASSES--------------------------

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;
        private final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

        // Create a bluetooth socket
        public ConnectThread(BluetoothDevice device) {
            BluetoothSocket tmp = null;
            mmDevice = device;

            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
            } catch (IOException e) {
            }

            mmSocket = tmp;
        }

        // Connect to socket and start sending
        public void run() {
            mBluetoothAdapter.cancelDiscovery();
            try {
                logToTextView("Connecting to Arduino...");
                mmSocket.connect();
            } catch (IOException connectException) {
                logToTextView("Connecting failed!");
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                }
                return;
            }
            ct = new ConnectedThread(mmSocket);
            ct.start();
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

    private class ConnectedThread extends Thread {
        final byte delimiter = 10;
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        int readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];

        public ConnectedThread(BluetoothSocket socket) {
            Looper.prepare();
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
            }
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
            logToTextView("Connected to Arduino");
        }

        public void run() {
            boolean stopWorker = false;
            while(!Thread.currentThread().isInterrupted() && !stopWorker) {
                try {
                    int bytesAvailable = mmInStream.available();
                    if (bytesAvailable > 0) {
                        byte[] packetBytes = new byte[bytesAvailable];
                        mmInStream.read(packetBytes);
                        for (int i = 0; i < bytesAvailable; i++) {
                            byte b = packetBytes[i];
                            if (b == delimiter) {
                                byte[] encodedBytes = new byte[readBufferPosition];
                                System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                final String data = new String(encodedBytes, "US-ASCII");
                                readBufferPosition = 0;

                                logToTextView("Arduino says:" + data);
                            } else {
                                readBuffer[readBufferPosition++] = b;
                            }
                        }
                    }
                } catch (IOException ex) {
                    stopWorker = true;
                    logToTextView("IOException when receiving data!");
                }
            }
        }

        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                Log.d("DEBUG", "Message sent");
            } catch
                    (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }
}
