package com.example.schwusch.iotap_app;

import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity implements MqttCallback {
    FloatingActionButton fab;
    TextView tvBluetooth, tvServer;
    ProgressBar spinner;
    Button btnUp, btnDown, btnLeft, btnRight, btnQw, btnAqw;
    CoordinatorLayout coordinatorLayout;
    Thread collector;
    DataCollectorRunnable dataCollect;
    MqttClient mqttClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main);

        tvBluetooth = (TextView) findViewById(R.id.tvBluetooth);
        tvBluetooth.setTextColor(Color.RED);
        tvServer = (TextView) findViewById(R.id.tvServer);
        tvServer.setTextColor(Color.RED);
        btnUp = (Button) findViewById(R.id.btnUp);
        btnDown = (Button) findViewById(R.id.btnDown);
        btnRight = (Button) findViewById(R.id.btnRight);
        btnLeft = (Button) findViewById(R.id.btnLeft);
        btnQw = (Button) findViewById(R.id.btnQw);
        btnAqw = (Button) findViewById(R.id.btnAqw);

        spinner = (ProgressBar) findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                beginListenForData();
                connectBluemix();
            }
        });
    }

    void beginListenForData() {
        spinner.setVisibility(View.VISIBLE);
        if (collector == null || !collector.isAlive()) {
            try {
                dataCollect = new DataCollectorRunnable(this);
                collector = new Thread(dataCollect);
                collector.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (collector != null && collector.isAlive()) {
            dataCollect.tryToSend();
        }
    }

    void btMessage(String message, boolean fail) {
        spinner.setVisibility(View.GONE);
        if (fail) {
            tvBluetooth.setTextColor(Color.RED);
        } else {
            tvBluetooth.setTextColor(Color.GREEN);
        }
        if (message != null) snack(message);
    }

    void snack(String text) {
        Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_SHORT).show();
        if (text.contains("UP")) {
            btnLeft.setVisibility(View.GONE);
            btnRight.setVisibility(View.GONE);
            btnDown.setVisibility(View.GONE);
            btnAqw.setVisibility(View.GONE);
            btnQw.setVisibility(View.GONE);
            btnUp.setVisibility(View.VISIBLE);
        } else if (text.contains("DOWN")) {
            btnLeft.setVisibility(View.GONE);
            btnRight.setVisibility(View.GONE);
            btnUp.setVisibility(View.GONE);
            btnAqw.setVisibility(View.GONE);
            btnQw.setVisibility(View.GONE);
            btnDown.setVisibility(View.VISIBLE);
        } else if (text.contains("RIGHT")) {
            btnLeft.setVisibility(View.GONE);
            btnDown.setVisibility(View.GONE);
            btnUp.setVisibility(View.GONE);
            btnAqw.setVisibility(View.GONE);
            btnQw.setVisibility(View.GONE);
            btnRight.setVisibility(View.VISIBLE);
        } else if (text.contains("LEFT")) {
            btnDown.setVisibility(View.GONE);
            btnRight.setVisibility(View.GONE);
            btnUp.setVisibility(View.GONE);
            btnAqw.setVisibility(View.GONE);
            btnQw.setVisibility(View.GONE);
            btnLeft.setVisibility(View.VISIBLE);
        } else if(text.contains("AQW")){
            btnLeft.setVisibility(View.GONE);
            btnRight.setVisibility(View.GONE);
            btnUp.setVisibility(View.GONE);
            btnDown.setVisibility(View.GONE);
            btnAqw.setVisibility(View.VISIBLE);
            btnQw.setVisibility(View.GONE);
        } else if(text.contains("QW")){
            btnLeft.setVisibility(View.GONE);
            btnRight.setVisibility(View.GONE);
            btnUp.setVisibility(View.GONE);
            btnDown.setVisibility(View.GONE);
            btnQw.setVisibility(View.VISIBLE);
            btnAqw.setVisibility(View.GONE);
        } else {                                //idle state
            btnLeft.setVisibility(View.GONE);
            btnRight.setVisibility(View.GONE);
            btnUp.setVisibility(View.GONE);
            btnDown.setVisibility(View.GONE);
            btnQw.setVisibility(View.GONE);
            btnAqw.setVisibility(View.GONE);
        }
    }

    void connectBluemix() {
        if (mqttClient == null || !mqttClient.isConnected()) {
            String broker = "tcp://ydlax8.messaging.internetofthings.ibmcloud.com:1883";
            String clientId = "d:ydlax8:Android:bocker";
            String password = "";
            MemoryPersistence persistence = new MemoryPersistence();
            try {
                mqttClient = new MqttClient(broker, clientId, persistence);
                MqttConnectOptions connOpts = new MqttConnectOptions();
                connOpts.setPassword(password.toCharArray());
                connOpts.setUserName("use-token-auth");
                connOpts.setCleanSession(true);
                mqttClient.connect(connOpts);
                mqttClient.setCallback(this);

                tvServer.setTextColor(Color.GREEN);
            } catch (MqttException me) {
                tvServer.setTextColor(Color.RED);
                me.printStackTrace();
            }
        }
    }

    void sendActionToBluemix(String message) {
        try {
            JSONObject json = new JSONObject();
            json.put("move", message);
            JSONObject contJson = new JSONObject();
            contJson.put("d", json);
            mqttClient.publish("iot-2/evt/eid/fmt/json", contJson.toString().getBytes(), 0, false);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void connectionLost(Throwable cause) {
        tvServer.setTextColor(Color.RED);
        cause.printStackTrace();
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        snack(message.toString());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {

    }
}
