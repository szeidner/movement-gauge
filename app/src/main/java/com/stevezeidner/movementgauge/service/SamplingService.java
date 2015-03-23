package com.stevezeidner.movementgauge.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.content.LocalBroadcastManager;
import android.util.FloatMath;
import android.util.Log;

import java.util.List;

/**
 * Sample data from the accelerometer sensor and broadcast a normalized value
 */
public class SamplingService extends Service implements SensorEventListener {
    private SensorManager sensorManager;

    private Sensor accelSensor;

    private boolean samplingStarted = false;
    private int rate;
    private int sampleCounter;
    private LocalBroadcastManager broadcaster;

    private float cumulative;

    private static final String LOG_TAG = SamplingService.class.getSimpleName();
    static final public String SAMPLE_RESULT = "com.stevezeidner.movementgauge.service.SamplingService.REQUEST_PROCESSED";
    static final public String SAMPLE_VALUE = "com.stevezeidner.movementgauge.service.SamplingService.SAMPLE_VALUE";
    static final public String CUMULATIVE_VALUE = "com.stevezeidner.movementgauge.service.SamplingService.CUMULATIVE_VALUE";
    static final public String TIMESTAMP_VALUE = "com.stevezeidner.movementgauge.service.SamplingService.TIMESTAMP_VALUE";
    static final public String CUMULATIVE_STARTUP_VALUE = "com.stevezeidner.movementgauge.service.SamplingService.CUMULATIVE_STARTUP_VALUE";

    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand");

        // get the cumulative value from the intent
        cumulative = intent.getFloatExtra(CUMULATIVE_STARTUP_VALUE, 0.0f);;

        // in case the activity-level service management fails
        stopSampling();

        // set the sample rate to a suitable level
        rate = SensorManager.SENSOR_DELAY_UI;

        // get sensor manager and start sampling data
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        startSampling();

        Log.d(LOG_TAG, "onStartCommand ends");
        return START_NOT_STICKY;
    }

    public void onDestroy() {
        super.onDestroy();
        Log.d(LOG_TAG, "onDestroy");
        stopSampling();
    }

    public IBinder onBind(Intent intent) {
        return serviceBinder;
    }

    // SensorEventListener
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if (accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW ||
                accuracy == SensorManager.SENSOR_STATUS_NO_CONTACT ||
                accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE) {
            Log.w(LOG_TAG, "Sensor " + sensor.getName() + " accuracy is low, need to recalibrate");
        } else {
            Log.d(LOG_TAG, "Sensor " + sensor.getName() + " accuracy has changed, but is medium or high. Let's not do anything for the time being.");
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        processSample(sensorEvent);
    }


    private void stopSampling() {
        // if the sampling hasn't even started, nothing needs to happen here
        if (!samplingStarted) {
            return;
        }

        // go ahead and unregister the sensorManager if it's not already
        if (sensorManager != null) {
            Log.d(LOG_TAG, "Unregister listener.");
            sensorManager.unregisterListener(this);
        }

        // keep track of the state of the sampling
        samplingStarted = false;
    }

    private void startSampling() {
        // sampling has already started, so this is redundant
        if (samplingStarted) {
            return;
        }

        // reset sample counter
        sampleCounter = 0;

        // get the accelerometer sensor (if it exists)
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_LINEAR_ACCELERATION);
        accelSensor = sensors.size() == 0 ? null : sensors.get(0);

        if (accelSensor != null) {
            // if we got both of the sensor, go ahead and register listeners
            Log.d(LOG_TAG, "Register listener");
            sensorManager.registerListener(this, accelSensor, rate);
        } else {
            // if either of the sensors are missing, we can't get the data we need
            Log.e(LOG_TAG, "Sensor(s) missing: accelSensor: " + accelSensor);
        }

        samplingStarted = true;
    }


    private void updateSampleCounter() {
        ++sampleCounter;
    }

    private void processSample(SensorEvent sensorEvent) {
        float values[] = sensorEvent.values;
        if (values.length < 3) {
            return;
        }

        // get the axes
        float x = values[0];
        float y = values[1];
        float z = values[2];

        // normalize the 3D accelerometer data into just one value
        float normAccel = FloatMath.sqrt(x * x + y * y + z * z);
        float scaledAccel = FloatMath.floor(normAccel * 10);
        cumulative += FloatMath.floor(normAccel) * 0.01; // scale this back so we have more interesting numbers to look at

        // broadcast the calculated value
        sendResult(scaledAccel, cumulative, sensorEvent.timestamp);

        updateSampleCounter();
    }

    /**
     * Broadcast motion value for receiver to handle
     *
     * @param sample Float of the current sample value
     * @param cumulative Float of the cumulative values since app started
     */
    public void sendResult(float sample, float cumulative, long timestamp) {
        Intent intent = new Intent(SAMPLE_RESULT);
        intent.putExtra(TIMESTAMP_VALUE, timestamp);
        intent.putExtra(SAMPLE_VALUE, sample);
        intent.putExtra(CUMULATIVE_VALUE, cumulative);
        broadcaster.sendBroadcast(intent);
    }

    public class SamplingBinder extends Binder {
        SamplingService getService() {
            return SamplingService.this;
        }
    }

    private final IBinder serviceBinder = new SamplingBinder();

    public void setCumulative(float cumulative) {
        this.cumulative = cumulative;
    }
}
