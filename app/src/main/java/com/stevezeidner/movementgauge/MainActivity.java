package com.stevezeidner.movementgauge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.stevezeidner.movementgauge.core.Constants;
import com.stevezeidner.movementgauge.core.Utility;
import com.stevezeidner.movementgauge.network.PubNub;
import com.stevezeidner.movementgauge.service.SamplingService;
import com.stevezeidner.movementgauge.ui.view.GaugeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main Activity for the application that handles starting the data sampling service and
 * managing the main layout.
 */
public class MainActivity extends ActionBarActivity {
    // service and receiver
    private SamplingServiceConnection samplingServiceConnection = null;
    private Intent samplingServiceIntent = null;
    private boolean samplingServiceRunning = false;
    private BroadcastReceiver receiver;

    // views
    private TextView tvValue, gaugeTitle;
    private GaugeView gaugeView;
    private SwitchCompat toggle;
    private Button reset;

    // log tag
    private static final String LOG_TAG = MainActivity.class.getSimpleName();

    // used to keep track of current state
    private boolean cumulativeMode = false;
    private float cumulative;
    private float lastValue;
    private long lastPushedTime;

    // network request parameters
    private JSONArray queue;
    private PubNub pubnub = null;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // set our content layout
        setContentView(R.layout.activity_main);

        // grab views
        tvValue = (TextView) findViewById(R.id.value);
        gaugeTitle = (TextView) findViewById(R.id.gauge_title);
        gaugeView = (GaugeView) findViewById(R.id.gauge);
        toggle = (SwitchCompat) findViewById(R.id.toggle);
        reset = (Button) findViewById(R.id.reset);

        // configure the broadcast receiver and any click listeners on view objects
        initBroadcastReceivers();
        initClickListeners();

        // start pubnub service
        pubnub = new PubNub(
                Constants.PUBNUB_PUB,
                Constants.PUBNUB_SUB,
                Constants.PUBNUB_SEC,
                true,
                Constants.PUBNUB_CHANNEL
        );

        // initialize queue
        queue = new JSONArray();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter(Constants.SAMPLE_RESULT));

        // subscribe to pubnub
        if (pubnub != null) {
            pubnub.Subscribe();
        }

        // bind to the sampling service
        bindSamplingService();

        // start sampling points
        startSamplingService();

        // reset the last pushed time to right now
        lastPushedTime = System.currentTimeMillis();
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

        // stop sampline points
        stopSamplingService();

        // release the service
        releaseSamplingService();

        // flush PubNub queue
        flushQueue();

        // unsubscribe from pubnub
        if (pubnub != null) {
            pubnub.Unsubscribe();
        }

        // write our cumulative value to shared preferences for retrieval later
        writeCumulative(cumulative);

        super.onStop();
    }

    /**
     * Create the broadcast receiver to receive updates from the sampling service
     */
    private void initBroadcastReceivers() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                float value = intent.getFloatExtra(Constants.SAMPLE_VALUE, 0.0f);
                ;
                long timestamp = intent.getLongExtra(Constants.TIMESTAMP_VALUE, 0);
                cumulative = intent.getFloatExtra(Constants.CUMULATIVE_VALUE, 0.0f);
                if (cumulativeMode) {
                    updateValue(cumulative);
                } else {
                    updateValue(value);
                }

                addToQueue(timestamp, value);
            }
        };
    }

    /**
     * Add value to a queue that gets flushed to pubnub every so often
     *
     * @param timestamp
     * @param value
     */
    private void addToQueue(long timestamp, float value) {

        if (value != lastValue) {
            // add data to queue if the value has changed
            JSONObject data = new JSONObject();
            try {
                data.put("time", timestamp);
                data.put("value", value);
            } catch (JSONException ignore) {
            }

            queue.put(data);
        }

        lastValue = value;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastPushedTime) / 1000.0f;

        // if the queue is growing large or it has been some reasonable amount of time since the last publish, then publish again
        if (queue.length() >= 100 ||
                (queue.length() > 0 && deltaTime >= Utility.randomBetween(Constants.LOW_SEND_TIME, Constants.HIGH_SEND_TIME))) {
            flushQueue();
            lastPushedTime = currentTime;
        }
    }

    private void flushQueue() {
        pubnub.Publish(queue);
        queue = new JSONArray();
    }

    /**
     * Create UI click listeners
     */
    private void initClickListeners() {
        toggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    setCumulative();
                } else {
                    setInstantaneous();
                }
            }
        });

        reset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cumulative = 0;
                writeCumulative(cumulative);
                startSamplingService();
            }
        });
    }

    /**
     * Set gauge mode to display cumulative motion
     */
    private void setCumulative() {
        cumulativeMode = true;
        gaugeTitle.setText(getResources().getString(R.string.cumulative_title));
        gaugeView.setFaceColor(getResources().getColor(R.color.face_cumulative));
        reset.setVisibility(View.VISIBLE);
    }

    /**
     * Set gauge mode to display realtime motion
     */
    private void setInstantaneous() {
        cumulativeMode = false;
        gaugeTitle.setText(getResources().getString(R.string.instantaneous_title));
        gaugeView.setFaceColor(getResources().getColor(R.color.face_instantaneous));
        reset.setVisibility(View.GONE);
    }

    /**
     * Start up the service to sample data from the sensors
     */
    private void startSamplingService() {
        if (samplingServiceRunning) {
            // safety check
            stopSamplingService();
        }
        samplingServiceIntent = new Intent(this, SamplingService.class);
        samplingServiceIntent.putExtra(Constants.CUMULATIVE_STARTUP_VALUE, readCumulative());
        startService(samplingServiceIntent);
        samplingServiceRunning = true;
    }

    /**
     * Stop the sample service
     */
    private void stopSamplingService() {
        Log.d(LOG_TAG, "stopSamplingService");
        if (samplingServiceRunning) {
            if (samplingServiceIntent != null) {
                stopService(samplingServiceIntent);
            }
            samplingServiceRunning = false;
        }
    }

    /**
     * Bind sampling service
     */
    private void bindSamplingService() {
        samplingServiceConnection = new SamplingServiceConnection();
        bindService(new Intent(this, SamplingService.class), samplingServiceConnection, Context.BIND_AUTO_CREATE);
    }

    /**
     * Unbind sampling service
     */
    private void releaseSamplingService() {
        unbindService(samplingServiceConnection);
        samplingServiceConnection = null;
    }

    /**
     * Update UI with the fresh values
     *
     * @param value Float of movement value
     */
    private void updateValue(float value) {
        tvValue.setText("" + Utility.round(value, 2));
        gaugeView.setValue(value);
    }

    /**
     * Write the cumulative value to shared prefs
     *
     * @param value float value to write to shared prefs
     */
    public void writeCumulative(float value) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(Constants.CUMULATIVE_PREFS_KEY, value);
        editor.commit();
    }

    /**
     * Read cumulative value from shared preferences
     *
     * @return float of cumulative value from stored shared prefs
     */
    public float readCumulative() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getFloat(Constants.CUMULATIVE_PREFS_KEY, 0);
    }

    /**
     * Connection to sampling service
     */
    class SamplingServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className,
                                       IBinder boundService) {
            Log.d(LOG_TAG, "onServiceConnected");
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(LOG_TAG, "onServiceDisconnected");
        }
    }


}
