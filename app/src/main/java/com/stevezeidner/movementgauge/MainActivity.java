package com.stevezeidner.movementgauge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.stevezeidner.movementgauge.service.SamplingService;
import com.stevezeidner.movementgauge.ui.view.GaugeView;

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

    private TextView tvValue, gaugeTitle;
    private GaugeView gaugeView;
    private SwitchCompat toggle;

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String SAMPLING_SERVICE_ACTIVATED_KEY = "samplingServiceActivated";
    private static final String STEPCOUNT_KEY = "sampleCounter";

    private boolean cumulativeMode = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvValue = (TextView) findViewById(R.id.value);
        gaugeTitle = (TextView) findViewById(R.id.gauge_title);
        gaugeView = (GaugeView) findViewById(R.id.gauge);
        toggle = (SwitchCompat) findViewById(R.id.toggle);
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

        // bind to the sampling service
        bindSamplingService();

        startSamplingService();
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
    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        releaseSamplingService();
    }

    /**
     * Create the broadcast receiver to receive updates from the sampling service
     */
    private void initBroadcastReceivers() {
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Float value;
                if (cumulativeMode) {
                    value = intent.getFloatExtra(SamplingService.CUMULATIVE_VALUE, 0.0f);
                } else {
                    value = intent.getFloatExtra(SamplingService.SAMPLE_VALUE, 0.0f);
                }
                updateValue(value);
            }
        };
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
    }

    /**
     * Set gauge mode to display cumulative motion
     */
    private void setCumulative() {
        cumulativeMode = true;
        gaugeTitle.setText(getResources().getString(R.string.cumulative_title));
        gaugeView.setFaceColor(getResources().getColor(R.color.face_cumulative));
    }

    /**
     * Set gauge mode to display realtime motion
     */
    private void setInstantaneous() {
        cumulativeMode = false;
        gaugeTitle.setText(getResources().getString(R.string.instantaneous_title));
        gaugeView.setFaceColor(getResources().getColor(R.color.face_instantaneous));
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
     * Unbind sampline service
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
