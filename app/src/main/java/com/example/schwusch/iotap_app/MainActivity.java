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
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

public class MainActivity extends Activity {
    FloatingActionButton fab;
    TextView tvBluetooth, tvServer;
    Intent mServiceIntent;
    ResponseReceiver receiver;
    ProgressBar spinner;
    CoordinatorLayout coordinatorLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register broadcast receiver
        IntentFilter mStatusIntentFilter = new IntentFilter();
        mStatusIntentFilter.addAction(Constants.IOTAP_GUI);
        mStatusIntentFilter.addAction(Constants.IOTAP_BT_FAIL);
        mStatusIntentFilter.addAction(Constants.IOTAP_BT_SUCCESS);
        mStatusIntentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        receiver = new ResponseReceiver();
        this.registerReceiver(receiver, mStatusIntentFilter);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, mStatusIntentFilter);

        tvBluetooth = (TextView) findViewById(R.id.tvBluetooth);
        tvBluetooth.setTextColor(Color.RED);
        tvServer = (TextView) findViewById(R.id.tvServer);
        tvServer.setTextColor(Color.RED);

        spinner = (ProgressBar)findViewById(R.id.progressBar1);
        spinner.setVisibility(View.GONE);

        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinatorLayout);

        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> beginListenForData());
        startService(new Intent(this, GestureDetectorService.class));
    }

    void beginListenForData() {
        spinner.setVisibility(View.VISIBLE);
        if (mServiceIntent != null)
            stopService(mServiceIntent);
        mServiceIntent = new Intent(this, DataCollectorService.class);
        startService(mServiceIntent);
    }

    void snack(String text) {
        Snackbar.make(coordinatorLayout, text, Snackbar.LENGTH_LONG).show();
    }

    // Broadcast receiver for receiving status updates from the DataCollectorService
    public class ResponseReceiver extends BroadcastReceiver {
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            if (Constants.IOTAP_GUI.equals(intent.getAction())) {
                snack(intent.getStringExtra(Constants.EXTENDED_DATA_STATUS));

            } else if (Constants.IOTAP_BT_SUCCESS.equals(intent.getAction())) {
                spinner.setVisibility(View.GONE);
                tvBluetooth.setTextColor(Color.GREEN);
                snack(intent.getStringExtra(Constants.EXTENDED_DATA_STATUS));

            } else if(Constants.IOTAP_BT_FAIL.equals(intent.getAction())) {
                spinner.setVisibility(View.GONE);
                tvBluetooth.setTextColor(Color.RED);
                snack(intent.getStringExtra(Constants.EXTENDED_DATA_STATUS));

            } else if(ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                NetworkInfo currentNetworkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);

                if (currentNetworkInfo.isConnected()) {
                    tvServer.setTextColor(Color.GREEN);
                } else {
                    tvServer.setTextColor(Color.RED);
                }
            }
        }
    }
}
