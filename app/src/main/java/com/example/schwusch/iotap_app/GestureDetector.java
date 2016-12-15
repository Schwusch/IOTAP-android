package com.example.schwusch.iotap_app;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import java.io.ObjectInputStream;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.DenseInstance;

class GestureDetector {
    private ArrayList<LinkedList<Integer>> movingWindow = new ArrayList<>();
    private ArrayList<LinkedList<Integer>> filteredMovingWindow = new ArrayList<>();
    private Classifier cls;
    private MainActivity mainActivity;
    private boolean record = false;
    private int recordCounter = 0;
    private int accMax = Integer.MIN_VALUE;
    private int accMin = Integer.MAX_VALUE;
    private int gyrMax = Integer.MIN_VALUE;
    private int gyrMin = Integer.MAX_VALUE;

    GestureDetector(MainActivity mainActivity) throws Exception {
        initMovingWindows();
        this.mainActivity = mainActivity;
    }

    void addSample(String sample) throws Exception {
        Integer[] values = parseStringSample(sample);
        if (values != null) {
            // If window is full, dequeue one sample from head before queuing another
            if (movingWindow.get(0).size() == Constants.MOVING_WINDOW_SIZE) {
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
                for (int i = 0; i < Constants.SENSOR_VALUES/2; i++) {
                    accMax = Math.max(filteredMovingWindow.get(i).getLast(), accMax);
                    accMin = Math.min(filteredMovingWindow.get(i).getLast(), accMin);
                }

                for (int i = Constants.SENSOR_VALUES/2; i < Constants.SENSOR_VALUES; i++) {
                    gyrMax = Math.max(filteredMovingWindow.get(i).getLast(), gyrMax);
                    gyrMin = Math.min(filteredMovingWindow.get(i).getLast(), gyrMin);
                }
                recordCounter++;

            } else if(record) {
                classify();

                record = false;
                recordCounter = 0;
                accMax = Integer.MIN_VALUE;
                accMin = Integer.MAX_VALUE;
                gyrMax = Integer.MIN_VALUE;
                gyrMin = Integer.MAX_VALUE;
            }
        }
    }

    private void classify() throws Exception {
        double values[][] = normalizeAll();
        double flattenedValues[] = flattenData(values);
        //TODO: Figure out what dataset is needed for Instance
        DenseInstance instance = new DenseInstance(1.0, flattenedValues);
        //cls.classifyInstance(instance);
    }

    private double[] flattenData(double[][] data) {
        double flattenedValues[] = new double[data.length * data[0].length];
        for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                flattenedValues[(data[0].length * i) + j] = data[i][j];
            }
        }
        return flattenedValues;
    }

    private double[][] normalizeAll() {
        double values[][] = new double[6][Constants.MOVING_WINDOW_SIZE];

        for (int i = 0; i < Constants.SENSOR_VALUES/2; i++) {
            values[i] = normalize(accMin, accMax, filteredMovingWindow.get(i));
            values[i + Constants.SENSOR_VALUES/2] = normalize(gyrMin, gyrMax, filteredMovingWindow.get(i + Constants.SENSOR_VALUES/2));
        }
        return values;
    }

    double[] normalize(int offset, int max, List<Integer> data) {
        int span = max - offset;
        double[] returnVals = new double[data.size()];

        for(int i = 0; i < data.size(); i++) {
            returnVals[i] = (double) (data.get(i) - offset) / (double) span;
        }
        return returnVals;
    }

    private boolean isThresholdExceeded() {
        boolean exceeded = false;
        if(filteredMovingWindow.get(0).size() > 2) {
            for (int i = 0; i < 3; i++) {
                int delta = filteredMovingWindow.get(i).getLast() - filteredMovingWindow.get(i).get(filteredMovingWindow.get(i).size() - 2);
                if (delta > Constants.ACC_THRESHOLD || delta < -Constants.ACC_THRESHOLD) {
                    exceeded = true;
                    break;
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
        ObjectInputStream ois = new ObjectInputStream(mainActivity.getResources().openRawResource(R.raw.classifier));
        cls = (Classifier) ois.readObject();
    }
}
