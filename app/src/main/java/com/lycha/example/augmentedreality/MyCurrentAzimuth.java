package com.lycha.example.augmentedreality;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import static android.content.Context.SENSOR_SERVICE;

/**
 * Created by krzysztofjackowski on 24/09/15.
 */
public class MyCurrentAzimuth implements SensorEventListener {

    Context mContext;
    private SensorManager sensorManager;
    private Sensor sensor;
    private int azimuthFrom = 0;
    private int azimuthTo = 0;
    private OnAzimuthChangedListener mAzimuthListener;
    float[] inR = new float[16];
    float[] I = new float[16];
    float[] gravity = new float[3];
    float[] geomag = new float[3];
    float[] orientVals = new float[3];

    double azimuth = 0;
    double pitch = 0;
    double roll = 0;

    public MyCurrentAzimuth(OnAzimuthChangedListener azimuthListener, Context context) {
        mAzimuthListener = azimuthListener;
        mContext = context;
    }

    public void start() {
        sensorManager = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        sensorManager.registerListener(this, sensor,
                SensorManager.SENSOR_DELAY_UI);
       /* msensorManager = (SensorManager) mContext.getSystemService(Context.SENSOR_SERVICE);
        mAccelerometer = msensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mGra = msensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        mMag = msensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        msensorManager.registerListener(this, mAccelerometer,
                SensorManager.SENSOR_DELAY_UI);
        msensorManager.registerListener(this, mGra,
                SensorManager.SENSOR_DELAY_UI);
        msensorManager.registerListener(this, mMag,
                SensorManager.SENSOR_DELAY_UI);*/
        /*SensorManager sm = (SensorManager) mContext.getSystemService(SENSOR_SERVICE);

    // Register this class as a listener for the accelerometer sensor
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
    // ...and the orientation sensor
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);*/
    }

    public void stop() {
        sensorManager.unregisterListener(this);
        //msensorManager.unregisterListener(this);
    }

    public void setOnShakeListener(OnAzimuthChangedListener listener) {
        mAzimuthListener = listener;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        azimuthFrom = azimuthTo;

        final float[] orientation = new float[3];
        final float[] rMat = new float[9];
        SensorManager.getRotationMatrixFromVector(rMat, event.values);
        azimuthTo = (int) (Math.toDegrees(SensorManager.getOrientation(rMat, orientation)[0]) + 360) % 360;
        mAzimuthListener.onAzimuthChanged(azimuthFrom, azimuthTo);

        /*if ((gravityValues != null) && (magneticValues != null)
                && (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)) {

            float[] deviceRelativeAcceleration = new float[4];
            deviceRelativeAcceleration[0] = event.values[0];
            deviceRelativeAcceleration[1] = event.values[1];
            deviceRelativeAcceleration[2] = event.values[2];
            deviceRelativeAcceleration[3] = 0;

            // Change the device relative acceleration values to earth relative values
            // X axis -> East
            // Y axis -> North Pole
            // Z axis -> Sky

            float[] R = new float[16], I = new float[16], earthAcc = new float[16];

            SensorManager.getRotationMatrix(R, I, gravityValues, magneticValues);

            float[] inv = new float[16];

            android.opengl.Matrix.invertM(inv, 0, R, 0);
            android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, deviceRelativeAcceleration, 0);
            Log.d("Acceleration", "Values: (" + earthAcc[0] + ", " + earthAcc[1] + ", " + earthAcc[2] + ")");

        } else if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
            gravityValues = event.values;
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            magneticValues = event.values;
        }*/
        /*// If the sensor data is unreliable return
        if (event.accuracy == SensorManager.SENSOR_STATUS_UNRELIABLE)
            return;

        // Gets the value of the sensor that has been changed
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                gravity = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                geomag = event.values.clone();
                break;
        }

        // If gravity and geomag have values then find rotation matrix
        if (gravity != null && geomag != null) {

            // checks that the rotation matrix is found
            boolean success = SensorManager.getRotationMatrix(inR, I,
                    gravity, geomag);
            if (success) {
                SensorManager.getOrientation(inR, orientVals);
                azimuth = Math.toDegrees(orientVals[0]);
                pitch = Math.toDegrees(orientVals[1]);
                roll = Math.toDegrees(orientVals[2]);
                Log.d("Acceleration", "Values: (" + azimuth + ", " + pitch + ", " + roll + ")");

            }
        }
        azimuthTo = (int) (Math.toDegrees(SensorManager.getOrientation(inR, orientVals)[0]) + 360) % 360;
        mAzimuthListener.onAzimuthChanged(azimuthFrom, azimuthTo);*/


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
