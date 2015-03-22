package com.stevezeidner.movementgauge;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.widget.TextView;

import com.stevezeidner.movementgauge.service.SamplingService;

/**
 * Main Activity for the application that handles starting the data sampling service and
 * managing the main layout.
 */
public class MainActivity extends ActionBarActivity {
    private SamplingServiceConnection samplingServiceConnection = null;
    private boolean samplingServiceRunning = false;
    private boolean samplingServiceActivated = false;
    private TextView sampleCounterTV;
    private String sampleCounterText = null;

    static final String LOG_TAG = MainActivity.class.getSimpleName();
    static final String SAMPLING_SERVICE_ACTIVATED_KEY = "samplingServiceActivated";
    static final String STEPCOUNT_KEY = "sampleCounter";

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Log.d(LOG_TAG, "onCreate");

        if (savedInstanceState != null) {
            sampleCounterText = savedInstanceState.getString(STEPCOUNT_KEY);
            samplingServiceActivated = savedInstanceState.getBoolean(SAMPLING_SERVICE_ACTIVATED_KEY, false);
        } else {
            samplingServiceActivated = false;
        }

        Log.d(LOG_TAG, "onCreate; samplingServiceActivated: " + samplingServiceActivated);

        bindSamplingService();

        SensorManager sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

        //startSamplingService();
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
    }

    private void releaseSamplingService() {
        releaseCallbackOnService();
        unbindService(samplingServiceConnection);
        samplingServiceConnection = null;
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

    ;


}
