package com.example.schwusch.iotap_app;

import android.content.Context;
import android.os.Vibrator;
import android.util.Log;

import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.List;

import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

class GestureDetector {
    private ArrayList<LinkedList<Integer>> movingWindow = new ArrayList<>();
    private ArrayList<LinkedList<Integer>> filteredMovingWindow = new ArrayList<>();
    private ArrayList<LinkedList<Integer>> deltaMovingWindow = new ArrayList<>();
    private ArrayList<Attribute> attrList = new ArrayList<>();
    private ArrayList<String> classVal = new ArrayList<>();
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
        initiateAttributeList();
        this.mainActivity = mainActivity;
    }

    void addSample(String sample) throws Exception {
        Log.d("SAMPLE", sample);
        Integer[] values = parseStringSample(sample);
        //for(int i = 0; i < values.length; i++) {
        //    Log.d("CUT", values[i] + "");
        //}
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

           // for(int i = 0; i < filteredMovingWindow.size(); i++) {
           //     Log.d("Filtered", filteredMovingWindow.get(i).getLast() + "");
           // }

            if (!record && isThresholdExceeded()) {
                record = true;
                Vibrator v = (Vibrator) mainActivity.getSystemService(Context.VIBRATOR_SERVICE);
                v.vibrate(50);

                for (LinkedList list : deltaMovingWindow) {
                    list.clear();
                }
                accMax = Integer.MIN_VALUE;
                accMin = Integer.MAX_VALUE;
                gyrMax = Integer.MIN_VALUE;
                gyrMin = Integer.MAX_VALUE;
            }

            if (record && recordCounter < Constants.MOVING_WINDOW_SIZE) {
                for (int i = 0; i < Constants.SENSOR_VALUES/2; i++) {
                    int gyrIndex = i + Constants.SENSOR_VALUES/2;
                    int deltaAcc = filteredMovingWindow.get(i).getLast() - filteredMovingWindow.get(i).get(filteredMovingWindow.get(i).size() - 2);
                    int deltaGyr = filteredMovingWindow.get(gyrIndex).getLast() - filteredMovingWindow.get(gyrIndex).get(filteredMovingWindow.get(gyrIndex).size() - 2);
                    accMax = Math.max(deltaAcc, accMax);
                    accMin = Math.min(deltaAcc, accMin);
                    gyrMax = Math.max(deltaGyr, gyrMax);
                    gyrMin = Math.min(deltaGyr, gyrMin);
                    deltaMovingWindow.get(i).add(deltaAcc);
                    deltaMovingWindow.get(i + Constants.SENSOR_VALUES/2).add(deltaGyr);
                }
                recordCounter++;

            } else if(record) {
                classify();
                record = false;
                recordCounter = 0;
            }
        }
    }

    private void classify() throws Exception {
        /*
        The double array must be as long as the training datastructure
        In this case it must be the number of sensors times the number of samples
        plus one, the class which is the first value...
         */
        double flattenedValues[] = plattenData();
        // Create a dataset, which holds the structure
        Instances data = new Instances("Jan-Olof", attrList, 0);
        // Create an instance to be classified
        Instance inst = new DenseInstance(1.0, flattenedValues);
        // Tell the instance which dataset it belongs to
        inst.setDataset(data);
        // Add the instance to the dataset
        data.add(inst);
        // Tell the dataset what attribute is the class
        data.setClassIndex(0);
        int classIndex = (int)cls.classifyInstance(inst);
        Log.d("CLASS", classIndex + "");
        mainActivity.runOnUiThread(() -> {
            try {
                mainActivity.snack("Gesture " + classVal.get(classIndex) + "!");
                mainActivity.sendActionToBluemix(classVal.get(classIndex));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private double[] plattenData() {
        double flattenedValues[] = new double[deltaMovingWindow.size() * deltaMovingWindow.get(0).size() + 1];
        int index = 1;
        for (int i = 0; i < deltaMovingWindow.get(0).size(); i++) {
            flattenedValues[index++] = deltaMovingWindow.get(0).get(i);
            flattenedValues[index++] = deltaMovingWindow.get(1).get(i);
            flattenedValues[index++] = deltaMovingWindow.get(2).get(i);
            flattenedValues[index++] = deltaMovingWindow.get(3).get(i);
            flattenedValues[index++] = deltaMovingWindow.get(4).get(i);
            flattenedValues[index++] = deltaMovingWindow.get(5).get(i);
        }

        return flattenedValues;
    }
    private double[] flattenData(double[][] data) {
        double flattenedValues[] = new double[data.length * data[0].length + 1];
       /* for (int i = 0; i < data.length; i++) {
            for (int j = 0; j < data[0].length; j++) {
                flattenedValues[(data[0].length * i) + j + 1] = data[i][j];
            }
        } */
        for (int i = 0; i < deltaMovingWindow.get(0).size(); i++) {
            for (int j = 0; j < deltaMovingWindow.size(); j++) {
                flattenedValues[i*j + j + 1] = deltaMovingWindow.get(j).get(i);
            }
        }

        return flattenedValues;
    }

    private double[][] normalizeAll() {
        double values[][] = new double[6][Constants.MOVING_WINDOW_SIZE];

        for (int i = 0; i < Constants.SENSOR_VALUES/2; i++) {
            values[i] = normalize(accMin, accMax, deltaMovingWindow.get(i));
            values[i + Constants.SENSOR_VALUES/2] = normalize(gyrMin, gyrMax, deltaMovingWindow.get(i + Constants.SENSOR_VALUES/2));
        }
        return values;
    }

    private double[] normalize(int offset, int max, List<Integer> data) {
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
            deltaMovingWindow.add(new LinkedList<>());
        }
    }

    void loadClassifier() throws Exception {
        ObjectInputStream ois = new ObjectInputStream(mainActivity.getResources().openRawResource(R.raw.classifier62));
        cls = (Classifier) ois.readObject();
    }

    private void initiateAttributeList() {
        /*
        The attribute list must resemble the training dataset structure.
        The first attribute is a list of the classes, the rest is
        the sensor values.
         */
        classVal.add("UP");
        classVal.add("DOWN");
        classVal.add("RIGHT");
        classVal.add("LEFT");
        classVal.add("QW");
        classVal.add("AQW");
        attrList.add(new Attribute("class", classVal));
        for (int i = 0; i < Constants.MOVING_WINDOW_SIZE; i++) {
            attrList.add(new Attribute("AccX" + (i + 1)));
            attrList.add(new Attribute("AccY" + (i + 1)));
            attrList.add(new Attribute("AccZ" + (i + 1)));
            attrList.add(new Attribute("GyrX" + (i + 1)));
            attrList.add(new Attribute("GyrY" + (i + 1)));
            attrList.add(new Attribute("GyrZ" + (i + 1)));
        }
    }
}
