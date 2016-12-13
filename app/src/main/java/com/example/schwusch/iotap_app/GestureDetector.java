package com.example.schwusch.iotap_app;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Queue;

import weka.classifiers.Classifier;
import weka.core.SerializationHelper;

class GestureDetector {
    private ArrayList<LinkedList<Integer>> movingWindow = new ArrayList<>();
    private ArrayList<LinkedList<Integer>> filteredMovingWindow = new ArrayList<>();
    private Classifier cls;
    Activity mainActivity;

    GestureDetector(Activity mainActivity) throws Exception {
        initMovingWindows();
        loadClassifier(mainActivity);
        this.mainActivity = mainActivity;
    }

    void addSample(String sample) {
        Integer[] values = parseStringSample(sample);
        if (values != null) {
            // If window is full, dequeue one sample from head before queuing another
            if (movingWindow.get(0).size() > Constants.MOVING_WINDOW_SIZE - 1) {
                movingWindow.forEach(Queue::poll);
                filteredMovingWindow.forEach(Queue::poll);
            }

            // Save sample to moving window tail
            for (int i = 0; i < Constants.SENSOR_VALUES; i++) {
                movingWindow.get(i).add(values[i]);
                filteredMovingWindow.get(i).add(
                        filterFIR(movingWindow.get(i))
                );
            }
        }
    }

    private Integer[] parseStringSample(String sample) {
        String[] parts = sample.split(",");
        if (parts.length != Constants.SENSOR_VALUES + 1) {
            Log.d("SAMPLE_ERROR", "Number of values:" + parts.length);
            return null;
        } else {
            return (Integer[]) Arrays.stream(Arrays.copyOfRange(parts, 1, parts.length)).
                    map(Integer::parseInt).toArray();
        }
    }

    private Integer filterFIR(LinkedList<Integer> data) {
        LinkedList<Integer> lastValues = (LinkedList<Integer>)
                data.subList(data.size() - Constants.MOVING_AVERAGE_LENGTH, data.size());

        return lastValues.stream().reduce(0, (a, b) -> a + b) / lastValues.size();
    }

    private void initMovingWindows() {
        for (int i = 0; i < Constants.SENSOR_VALUES; i++) {
            movingWindow.add(new LinkedList<>());
            filteredMovingWindow.add(new LinkedList<>());
        }
    }

    private void loadClassifier(Activity mainActivity) throws Exception {
        ObjectInputStream ois = new ObjectInputStream(
                mainActivity.getResources().openRawResource(R.raw.classifier));
        cls = (Classifier) ois.readObject();
    }
}
