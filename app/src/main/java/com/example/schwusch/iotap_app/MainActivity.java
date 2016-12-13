package com.example.schwusch.iotap_app;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    FloatingActionButton fab;
    TextView tvBluetooth, tvServer;
    ProgressBar spinner;
    CoordinatorLayout coordinatorLayout;
    Thread collector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvBluetooth = (TextView) findViewById(R.id.tvBluetooth);
        tvBluetooth.setTextColor(Color.RED);
        tvServer = (TextView) findViewById(R.id.tvServer);
        tvServer.setTextColor(Color.RED);

        spinner = (ProgressBar)findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> beginListenForData());
    }

    void beginListenForData() {
        spinner.setVisibility(View.VISIBLE);
        if (collector == null || !collector.isAlive()) {
            try {
                collector = new Thread(new DataCollectorRunnable(this));
                collector.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    void btMessage(String message, boolean fail){
        spinner.setVisibility(View.GONE);
        if(fail) {
            tvBluetooth.setTextColor(Color.RED);
        } else {
            tvBluetooth.setTextColor(Color.GREEN);
        }
        if (message != null) snack(message);
    }

    void snack(String text) {
        Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_LONG).show();
    }
}
