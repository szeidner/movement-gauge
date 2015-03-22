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

public class SamplingService extends Service implements SensorEventListener {
    private SensorManager sensorManager;

    private Sensor accelSensor;
    private Sensor gyroSensor;

    private boolean samplingStarted = false;
    private int rate;
    private int sampleCounter;
    private LocalBroadcastManager broadcaster;

    private static final String LOG_TAG = SamplingService.class.getSimpleName();
    static final public String SAMPLE_RESULT = "com.stevezeidner.movementgauge.service.SamplingService.REQUEST_PROCESSED";
    static final public String SAMPLE_MESSAGE = "com.stevezeidner.movementgauge.service.SamplingService.MESSAGE";

    @Override
    public void onCreate() {
        super.onCreate();
        broadcaster = LocalBroadcastManager.getInstance(this);
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand");

        // in case the activity-level service management fails
        stopSampling();

        // set the sample rate
        rate = SensorManager.SENSOR_DELAY_NORMAL;

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
            // TODO: allow for sensor recalibration
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

        // get the gyroscope sensor (if it exists)
        sensors = sensorManager.getSensorList(Sensor.TYPE_GYROSCOPE);
        gyroSensor = sensors.size() == 0 ? null : sensors.get(0);

        if ((accelSensor != null) && (gyroSensor != null)) {
            // if we got both of the sensor, go ahead and register listeners
            Log.d(LOG_TAG, "Register listener");
            sensorManager.registerListener(this, accelSensor, rate);
            sensorManager.registerListener(this, gyroSensor, rate);
        } else {
            // if either of the sensors are missing, we can't get the data we need
            Log.e(LOG_TAG, "Sensor(s) missing: accelSensor: " + accelSensor + "; gyroSensor: " + gyroSensor);
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

        // break down the directions

        String sensorName = "n/a";
        if (sensorEvent.sensor == accelSensor) {
            sensorName = "accel";

            float x = values[0];
            float y = values[1];
            float z = values[2];

            float currentAccel = FloatMath.sqrt(x * x + y * y + z * z) * 10;
            sendResult(currentAccel);
        } else if (sensorEvent.sensor == gyroSensor) {
            sensorName = "gyro";
        }

        //Log.i(LOG_TAG, sensorName + ": (" + sensorEvent.timestamp + "), " + values[0] + ", " + values[1] + ", " + values[2]);

        updateSampleCounter();


    }

    public void sendResult(float message) {
        Intent intent = new Intent(SAMPLE_RESULT);
        intent.putExtra(SAMPLE_MESSAGE, message);
        broadcaster.sendBroadcast(intent);
    }

    public class SamplingBinder extends Binder {
        SamplingService getService() {
            return SamplingService.this;
        }
    }

    private final IBinder serviceBinder = new SamplingBinder();

}
