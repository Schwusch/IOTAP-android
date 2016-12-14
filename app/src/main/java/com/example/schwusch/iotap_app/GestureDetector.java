package com.example.schwusch.iotap_app;

import android.app.Activity;
import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;

class GestureDetector {
    private ArrayList<LinkedList<Integer>> movingWindow = new ArrayList<>();
    private ArrayList<LinkedList<Integer>> filteredMovingWindow = new ArrayList<>();
    private Classifier cls;
    private MainActivity mainActivity;
    private boolean record = false;
    private int recordCounter = 0;

    GestureDetector(MainActivity mainActivity) throws Exception {
        initMovingWindows();
        this.mainActivity = mainActivity;
    }

    void addSample(String sample) {
        Integer[] values = parseStringSample(sample);
        if (values != null) {
            // If window is full, dequeue one sample from head before queuing another
            if (movingWindow.get(0).size() > Constants.MOVING_WINDOW_SIZE - 1) {
                for (int i = 0; i < movingWindow.size(); i++) {
                    movingWindow.get(i).poll();
                    filteredMovingWindow.get(i).poll();
                }
            }

            // Save sample to moving window tail
            for (int i = 0; i < Constants.SENSOR_VALUES; i++) {
                movingWindow.get(i).add(values[i]);
                filteredMovingWindow.get(i).add(
                        filterFIR(movingWindow.get(i))
                );
            }
            if (!record && isThresholdExceeded()) {
                record = true;
                Vibrator v = (Vibrator) mainActivity.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(100);
                mainActivity.runOnUiThread(() -> mainActivity.snack("Gesture Detected!"));
            }

            if (record && recordCounter < Constants.MOVING_WINDOW_SIZE + 1) {
                recordCounter++;
            } else if(record) {
                record = false;
                recordCounter = 0;
            }
        }
    }

    private boolean isThresholdExceeded() {
        boolean exceeded = false;
        if(filteredMovingWindow.get(0).size() > 2) {
            for (int i = 0; i < 3; i++) {
                int delta = filteredMovingWindow.get(i).getLast() - filteredMovingWindow.get(i).get(filteredMovingWindow.get(i).size() - 2);
                if (delta > Constants.ACC_THRESHOLD || delta < -Constants.ACC_THRESHOLD) {
                    exceeded = true;
                }
            }
        }
        return exceeded;
    }

    private Integer[] parseStringSample(String sample) {
        String[] parts = sample.split(",");
        if (parts.length != Constants.SENSOR_VALUES + 1) {
            Log.d("SAMPLE_ERROR", "Number of values:" + parts.length);
            return null;
        } else {
            Integer values[] = new Integer[Constants.SENSOR_VALUES];
            for (int i = 1; i < parts.length; i++) {
                values[i - 1] = Integer.parseInt(parts[i]);
            }
            return values;
        }
    }

    private Integer filterFIR(LinkedList<Integer> data) {
        List<Integer> lastValues;
        if(data.size() > Constants.MOVING_AVERAGE_LENGTH) {
            lastValues = data.subList(data.size() - Constants.MOVING_AVERAGE_LENGTH, data.size());
        } else {
            lastValues = data;
        }
        Integer sum = 0;
        for (Integer val: lastValues) {
            sum += val;
        }
        return sum / lastValues.size();
    }

    private void initMovingWindows() {
        for (int i = 0; i < Constants.SENSOR_VALUES; i++) {
            movingWindow.add(new LinkedList<>());
            filteredMovingWindow.add(new LinkedList<>());
        }
    }

    void loadClassifier() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                mainActivity.getResources().openRawResource(R.raw.classifier));
        cls = (Classifier) ois.readObject();
    }
}
