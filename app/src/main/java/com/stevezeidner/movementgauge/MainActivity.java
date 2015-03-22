package com.stevezeidner.movementgauge;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
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
    private boolean samplingServiceRunning = false;
    private boolean samplingServiceBound = false;
    private boolean samplingServiceActivated = false;
    private String sampleCounterText = null;
    private BroadcastReceiver receiver;

    private TextView tvValue;
    private GaugeView gaugeView;

    private static final String LOG_TAG = MainActivity.class.getSimpleName();
    private static final String SAMPLING_SERVICE_ACTIVATED_KEY = "samplingServiceActivated";
    private static final String STEPCOUNT_KEY = "sampleCounter";


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        tvValue = (TextView) findViewById(R.id.value);
        gaugeView = (GaugeView) findViewById(R.id.gauge);
        Log.d(LOG_TAG, "onCreate");

        // create the broadcast receiver to receive updates from the sampling service
        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Float value = intent.getFloatExtra(SamplingService.SAMPLE_MESSAGE, 0.0f);
                updateValue(value);
            }
        };

        if (savedInstanceState != null) {
            sampleCounterText = savedInstanceState.getString(STEPCOUNT_KEY);
            samplingServiceActivated = savedInstanceState.getBoolean(SAMPLING_SERVICE_ACTIVATED_KEY, false);
        } else {
            samplingServiceActivated = false;
        }
        Log.d(LOG_TAG, "onCreate; samplingServiceActivated: " + samplingServiceActivated);

        // bind to the sampling service
        bindSamplingService();

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

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

    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d(LOG_TAG, "onSaveInstanceState");

        outState.putBoolean(SAMPLING_SERVICE_ACTIVATED_KEY, samplingServiceActivated);

        if (sampleCounterText != null) {
            outState.putString(STEPCOUNT_KEY, sampleCounterText);
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        releaseSamplingService();
    }

    private void startSamplingService() {
        if (samplingServiceRunning) {
            // safety check
            stopSamplingService();
        }

        startService(new Intent(this, SamplingService.class));
        samplingServiceRunning = true;
    }

    private void stopSamplingService() {
        Log.d(LOG_TAG, "stopSamplingService");
        if (samplingServiceRunning) {
            stopSampling();
            samplingServiceRunning = false;
        }
    }

    private void bindSamplingService() {
        samplingServiceConnection = new SamplingServiceConnection();
        bindService(new Intent(this, SamplingService.class), samplingServiceConnection, Context.BIND_AUTO_CREATE);
        samplingServiceBound = true;
    }

    private void releaseSamplingService() {
        releaseCallbackOnService();
        unbindService(samplingServiceConnection);
        samplingServiceConnection = null;
        samplingServiceBound = false;
    }

    private void updateValue(float value) {
        tvValue.setText("" + round(value, 2));
        float adjustedValue = Math.abs(value) * 20.0f;
        gaugeView.setValue(adjustedValue);
    }


    private void setCallbackOnService() {

    }

    private void releaseCallbackOnService() {

    }

    private void updateSamplingServiceRunning() {

    }

    private void stopSampling() {

    }


    class SamplingServiceConnection implements ServiceConnection {
        public void onServiceConnected(ComponentName className,
                                       IBinder boundService) {
            Log.d(LOG_TAG, "onServiceConnected");
            setCallbackOnService();
            updateSamplingServiceRunning();
        }

        public void onServiceDisconnected(ComponentName className) {
            Log.d(LOG_TAG, "onServiceDisconnected");
        }
    }


    public static BigDecimal round(float d, int decimalPlace) {
        BigDecimal bd = new BigDecimal(Float.toString(d));
        bd = bd.setScale(decimalPlace, BigDecimal.ROUND_HALF_UP);
        return bd;
    }


}
