package com.example.schwusch.iotap_app;

import android.app.IntentService;
import android.content.Intent;
import android.os.Handler;

import java.io.IOException;
import java.io.InputStream;

/**
 * Created by schwusch on 2016-11-15.
 */

public class DataCollectorService extends IntentService {
    public DataCollectorService(){
        super("DataCollectorService");
    }

    @Override
    protected void onHandleIntent(Intent workIntent) {
        boolean stopWorker = false;
        int readBufferPosition = 0;
        byte[] readBuffer = new byte[1024];
        final byte delimiter = 10;
        final Handler handler = new Handler();
        InputStream mmInputStream = MainActivity.mmInputStream;

        while(!Thread.currentThread().isInterrupted() && !stopWorker)
        {
            try {
                int bytesAvailable = mmInputStream.available();
                if(bytesAvailable > 0) {
                    byte[] packetBytes = new byte[bytesAvailable];
                    mmInputStream.read(packetBytes);
                    for(int i=0;i<bytesAvailable;i++) {
                        byte b = packetBytes[i];
                        if(b == delimiter) {
                            byte[] encodedBytes = new byte[readBufferPosition];
                            System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                            final String data = new String(encodedBytes, "US-ASCII");
                            readBufferPosition = 0;

                            handler.post(new Runnable() {
                                public void run() {

                                }
                            });
                        } else {
                            readBuffer[readBufferPosition++] = b;
                        }
                    }
                }
            } catch (IOException ex) {
                stopWorker = true;
            }
        }
    }
}
