package com.stevezeidner.movementgauge.service;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.IInterface;
import android.os.Parcel;
import android.os.RemoteException;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

public class SamplingService extends Service implements SensorEventListener {
    static final String LOG_TAG = SamplingService.class.getSimpleName();
    static final boolean DEBUG_GENERAL = true;
    static final int SAMPLECTR_MOD = 1000;

    private int sampleCounter;
    private int rate;
    private SensorManager sensorManager;
    private PrintWriter captureFile;
    private Sensor accelSensor;
    private Sensor gyroSensor;
    private boolean samplingStarted = false;

    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d(LOG_TAG, "onStartCommand");

        // in case the activity-level service management fails
        stopSampling();

        // set the sample rate
        rate = SensorManager.SENSOR_DELAY_FASTEST;

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

        // close out the file we are capturing to
        if (captureFile != null) {
            captureFile.close();
            captureFile = null;
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
        List<Sensor> sensors = sensorManager.getSensorList(Sensor.TYPE_ACCELEROMETER);
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

//        captureFile = null;
//        File captureFileName = new File(
//                Environment.getExternalStorageDirectory(),
//                "capture.csv");
//        try {
//            captureFile = new PrintWriter(new FileWriter(captureFileName, false));
//        } catch (IOException ex) {
//            Log.e(LOG_TAG, ex.getMessage(), ex);
//        }
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
        String sensorName = "n/a";
        if (sensorEvent.sensor == accelSensor) {
            sensorName = "accel";
        } else if (sensorEvent.sensor == gyroSensor) {
            sensorName = "gyro";
        }

        Log.i(LOG_TAG, sensorName + ": (" + sensorEvent.timestamp + "), " + values[0] + ", " + values[1] + ", " + values[2]);

        updateSampleCounter();
    }

    private final IBinder serviceBinder = new IBinder() {
        @Override
        public String getInterfaceDescriptor() throws RemoteException {
            return null;
        }

        @Override
        public boolean pingBinder() {
            return false;
        }

        @Override
        public boolean isBinderAlive() {
            return false;
        }

        @Override
        public IInterface queryLocalInterface(String descriptor) {
            return null;
        }

        @Override
        public void dump(FileDescriptor fd, String[] args) throws RemoteException {

        }

        @Override
        public void dumpAsync(FileDescriptor fd, String[] args) throws RemoteException {

        }

        @Override
        public boolean transact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
            return false;
        }

        @Override
        public void linkToDeath(DeathRecipient recipient, int flags) throws RemoteException {

        }

        @Override
        public boolean unlinkToDeath(DeathRecipient recipient, int flags) {
            return false;
        }
    };


}
