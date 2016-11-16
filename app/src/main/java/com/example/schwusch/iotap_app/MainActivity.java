package com.example.schwusch.iotap_app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.TextView;

public class MainActivity extends Activity {
    FloatingActionButton fab;
    TextView tvText;
    Intent mServiceIntent;
    ResponseReceiver receiver;

    public void logToTextView(final String text) {
        runOnUiThread(() -> {
            tvText.setText(text);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Register broadcast receiver for bluetooth error messages
        IntentFilter mStatusIntentFilter = new IntentFilter();
        mStatusIntentFilter.addAction(Constants.BROADCAST_ACTION);
        receiver = new ResponseReceiver();
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, mStatusIntentFilter);

        tvText = (TextView) findViewById(R.id.tvText);
        fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(view -> beginListenForData());
    }

    void beginListenForData() {
        if (mServiceIntent != null)
            stopService(mServiceIntent);
        mServiceIntent = new Intent(this, DataCollectorService.class);
        startService(mServiceIntent);
    }

    // Broadcast receiver for receiving status updates from the DataCollectorService
    public class ResponseReceiver extends BroadcastReceiver {
        // Called when the BroadcastReceiver gets an Intent it's registered to receive
        public void onReceive(Context context, Intent intent) {
            logToTextView(intent.getStringExtra(Constants.EXTENDED_DATA_STATUS));
        }
    }
}
