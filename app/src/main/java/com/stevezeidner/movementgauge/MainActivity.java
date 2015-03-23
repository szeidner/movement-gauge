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

import com.stevezeidner.movementgauge.network.PubNub;
import com.stevezeidner.movementgauge.service.SamplingService;
import com.stevezeidner.movementgauge.ui.view.GaugeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;

/**
 * Main Activity for the application that handles starting the data sampling service and
 * managing the main layout.
 */
public class MainActivity extends ActionBarActivity {
    private SamplingServiceConnection samplingServiceConnection = null;
    private Intent samplingServiceIntent = null;
    private ComponentName service = null;
    private boolean samplingServiceRunning = false;
    private boolean samplingServiceBound = false;
    private boolean samplingServiceActivated = false;
    private String sampleCounterText = null;
    private BroadcastReceiver receiver;

    private PubNub pubnub = null;

    private TextView tvValue, gaugeTitle;
    private GaugeView gaugeView;
    private SwitchCompat toggle;
    private Button reset;

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String SAMPLING_SERVICE_ACTIVATED_KEY = "samplingServiceActivated";
    private static final String STEPCOUNT_KEY = "sampleCounter";
    private static final String CUMULATIVE_PREFS_KEY = "Cumulative";

    private boolean cumulativeMode = false;

    private float cumulative;
    private float lastValue;
    private long lastPushedTime;

    private JSONArray queue;

    SharedPreferences sharedPref;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvValue = (TextView) findViewById(R.id.value);
        gaugeTitle = (TextView) findViewById(R.id.gauge_title);
        gaugeView = (GaugeView) findViewById(R.id.gauge);
        toggle = (SwitchCompat) findViewById(R.id.toggle);
        reset = (Button) findViewById(R.id.reset);
        Log.d(LOG_TAG, "onCreate");

        initBroadcastReceivers();
        initClickListeners();

        // restore state
        if (savedInstanceState != null) {
            sampleCounterText = savedInstanceState.getString(STEPCOUNT_KEY);
            samplingServiceActivated = savedInstanceState.getBoolean(SAMPLING_SERVICE_ACTIVATED_KEY, false);
        } else {
            samplingServiceActivated = false;
        }
        Log.d(LOG_TAG, "onCreate; samplingServiceActivated: " + samplingServiceActivated);

        // start pubnub service
        pubnub = new PubNub(
                "pub-c-3031c78d-7e71-43ac-8e87-9657a7bbc7b6",
                "sub-c-40913ce6-d10d-11e4-9f3d-0619f8945a4f",
                "sec-c-OTRlZjUyNGQtNDRiNi00N2ZiLWE2Y2EtYTI1NDAzMTAwNGU0",
                true,
                "accelerometer"
        );

        sharedPref = getPreferences(Context.MODE_PRIVATE);

        queue = new JSONArray();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // bind to the sampling service
        bindSamplingService();

        startSamplingService();

        lastPushedTime = System.currentTimeMillis();
    }

    @Override
    protected void onStart() {
        super.onStart();
        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter(SamplingService.SAMPLE_RESULT));
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(LOG_TAG, "onSaveInstanceState");

        outState.putBoolean(SAMPLING_SERVICE_ACTIVATED_KEY, samplingServiceActivated);

        if (sampleCounterText != null) {
            outState.putString(STEPCOUNT_KEY, sampleCounterText);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(LOG_TAG, "onPause");
        stopSamplingService();
        releaseSamplingService();

        writeCumulative(cumulative);
    }

    /**
     * Create the broadcast receiver to receive updates from the sampling service
     */
    private void initBroadcastReceivers() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                float value = intent.getFloatExtra(SamplingService.SAMPLE_VALUE, 0.0f);
                ;
                long timestamp = intent.getLongExtra(SamplingService.TIMESTAMP_VALUE, 0);
                cumulative = intent.getFloatExtra(SamplingService.CUMULATIVE_VALUE, 0.0f);
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

        if (queue.length() >= 100 || (deltaTime >= 10 && queue.length() > 0)) {
            pubnub.Publish(queue);
            queue = new JSONArray();
            lastPushedTime = currentTime;
        }
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
        samplingServiceIntent.putExtra(SamplingService.CUMULATIVE_STARTUP_VALUE, readCumulative());
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
        samplingServiceBound = true;
    }

    /**
     * Unbind sampling service
     */
    private void releaseSamplingService() {
        unbindService(samplingServiceConnection);
        samplingServiceConnection = null;
        samplingServiceBound = false;
    }

    /**
     * Update UI with the fresh values
     *
     * @param value Float of movement value
     */
    private void updateValue(float value) {
        tvValue.setText("" + round(value, 2));
        gaugeView.setValue(value);
    }

    /**
     * Write the cumulative value to shared prefs
     *
     * @param value float value to write to shared prefs
     */
    private void writeCumulative(float value) {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putFloat(CUMULATIVE_PREFS_KEY, value);
        editor.commit();
    }

    /**
     * Read cumulative value from shared preferences
     *
     * @return float of cumulative value from stored shared prefs
     */
    private float readCumulative() {
        SharedPreferences sharedPref = getPreferences(Context.MODE_PRIVATE);
        return sharedPref.getFloat(CUMULATIVE_PREFS_KEY, 0);
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

    /**
     * Round to certain number of decimals
     *
     * @param d
     * @param decimalPlace
     * @return
     */
    public static float round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd.floatValue();
    }

}
