package com.example.schwusch.iotap_app;

import android.app.IntentService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Vibrator;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;

import weka.classifiers.Classifier;

/**
 * Created by Jonathan Böcker on 2016-11-24.
 */

public class GestureDetectorService extends IntentService {
    private ArrayList<Queue<Integer>> movingWindow = new ArrayList<>();
    private ArrayList<Queue<Integer>> filteredMovingWindow = new ArrayList<>();
    private ResponseReceiver receiver;
    private int counter = Constants.OVERLAP;

    public GestureDetectorService() throws Exception {
        super("GD");
        initMovingWindows();
        InputStream ins = getResources().openRawResource(R.raw.classifier);
        ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
        int size = 0;
        // Read the entire resource into a local byte buffer.
        byte[] buffer = new byte[1024];
        while((size=ins.read(buffer,0,1024))>=0){
            outputStream.write(buffer,0,size);
        }
        ins.close();
        buffer=outputStream.toByteArray();

        FileOutputStream fos = new FileOutputStream("classifier.model");
        fos.write(buffer);
        fos.close();

        Classifier cls = (Classifier) weka.core.SerializationHelper.read("classifier.model");

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        loadClassifier();
        IntentFilter mStatusIntentFilter = new IntentFilter();
        mStatusIntentFilter.addAction(Constants.IOTAP_GUI);
        receiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, mStatusIntentFilter);

        while (!Thread.currentThread().isInterrupted()) {
        }
    }

    // Broadcast receiver for receiving status updates from the DataCollectorService
    public class ResponseReceiver extends BroadcastReceiver {
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            try {
                Integer[] ret =
                        parseStringSample(intent.getStringExtra(Constants.EXTENDED_DATA_STATUS));
                if (ret != null) {
                    // If window is full, dequeue one sample from head before queuing another
                    if (movingWindow.get(0).size() > Constants.MOVING_WINDOW_SIZE - 1) {
                        movingWindow.forEach(Queue::poll);
                        filteredMovingWindow.forEach(Queue::poll);
                    }

                    // Save sample to moving window tail
                    for (int i = 0; i < Constants.SENSOR_VALUES; i++) {
                        movingWindow.get(i).add(ret[i]);
                        filteredMovingWindow.get(i).add(
                                filterFIR(movingWindow.get(i))
                        );
                    }



                    // Count down to see if its time for classification
                    counter--;
                    if (counter == 0) {
                        recognizeGesture(filteredMovingWindow);
                        counter = Constants.OVERLAP;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private Integer filterFIR(Queue<Integer> data) {
        Integer[] lastValues = (Integer[]) Arrays.copyOfRange(
                data.toArray(),
                data.size() - Constants.MOVING_AVERAGE_LENGTH,
                data.size());

        return Arrays.stream(lastValues).reduce(0, (a, b) -> a + b) / lastValues.length;
    }

    private void recognizeGesture(ArrayList<Queue<Integer>> data) {
        // TODO: use filteredMovingWindow to detect gestures
    }

    private Integer[] parseStringSample(String sample) {
        String[] parts = sample.split(" ");
        if (parts.length != Constants.SENSOR_VALUES) {
            Log.d("SAMPLE_ERROR", "Number of values:" + parts.length);
            return null;
        } else {
            return (Integer[]) Arrays.stream(parts).map(Integer::parseInt).toArray();
        }
    }

    private void initMovingWindows() {
        for (int i = 0; i < Constants.SENSOR_VALUES; i++) {
            movingWindow.add(new LinkedList<>());
            filteredMovingWindow.add(new LinkedList<>());
        }
    }

    private void loadClassifier() {
        LocalBroadcastManager.getInstance(this).sendBroadcast(
                new Intent(Constants.IOTAP_GUI)
                        .putExtra(Constants.EXTENDED_DATA_STATUS, "Loading Classifier not implemented!")
        );
    }
}
