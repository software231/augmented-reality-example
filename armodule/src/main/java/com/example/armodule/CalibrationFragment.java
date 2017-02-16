package com.example.armodule;

import android.app.Activity;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.FillDirection;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;

import java.util.LinkedList;
import java.util.Locale;
/*
import com.androidplot.xy.BoundaryMode;
import com.androidplot.xy.FillDirection;
import com.androidplot.xy.LineAndPointFormatter;
import com.androidplot.xy.SimpleXYSeries;
import com.androidplot.xy.XYPlot;
import com.mamlambo.globals.Global;*/

// TODO: Auto-generated Javadoc

/**
 * The Class CalibrationFragment.
 */
public class CalibrationFragment extends Fragment implements
        SensorEventListener {

    /**
     * The m plot.
     */
    private XYPlot mPlot;

    /**
     * The m plot data y.
     */
    private SimpleXYSeries mPlotDataY = new SimpleXYSeries("Y axis");

    /**
     * The m plot data z.
     */
    private SimpleXYSeries mPlotDataZ = new SimpleXYSeries("Z axis");

    /**
     * The m header.
     */
    private TextView mHeader;

    /**
     * The m sensor manager.
     */
    private SensorManager mSensorManager;

    /**
     * The m last accelerometer.
     */
    private float[] mLastAccelerometer;

    /**
     * The m last compass.
     */
    private float[] mLastCompass;

    /**
     * The m orientation data y.
     */
    private LinkedList<Number> mOrientationDataY = new LinkedList<Number>();

    /**
     * The m orientation data z.
     */
    private LinkedList<Number> mOrientationDataZ = new LinkedList<Number>();

    /**
     * The m back button.
     */
    private Button mBackButton;

    /**
     * The m save button.
     */
    private Button mSaveButton;

    /**
     * The m fragment.
     */
    private Fragment mFragment;

    /**
     * The m axis error y.
     */
    private float mAxisErrorY;

    /**
     * The m axis error z.
     */
    private float mAxisErrorZ;

    /**
     * The m handler.
     */
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message m) {
            switch (m.what) {
                case 0:
                    mPlotDataZ.setModel(mOrientationDataZ,
                            SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
                    mPlotDataY.setModel(mOrientationDataY,
                            SimpleXYSeries.ArrayFormat.Y_VALS_ONLY);
                    mPlot.redraw();
                    break;
                case 1:
                    mBackButton.setEnabled(true);
                    mSaveButton.setEnabled(true);
                    mAxisErrorY = computeError(mOrientationDataY, 0f);
                    mAxisErrorZ = computeError(mOrientationDataZ, 90f);
                    if (Math.abs(mAxisErrorZ) > 5f) { // || Math.abs(mAxisErrorY) >
                        // 5f ) {
                        mAxisErrorY = mAxisErrorZ = 0f;
                        mHeader.setText("Error bigger than 5 deegress please repeat");
                        mSaveButton.setText("Repeat");
                        mSaveButton.setTag("Repeat");
                    } else {
                        mHeader.setText(String.format(Locale.getDefault(),
                                "Y axis error: %f, Z axis error: %f degrees",
                                mAxisErrorY, mAxisErrorZ));
                    }
                default:
                    break;
            }
        }
    };

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onActivityCreated(android.os.Bundle)
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onActivityCreated(savedInstanceState);
        setRetainInstance(true);
        registerSensors();
        initViewMembers();
        setUpPlot();
        mesureErrors();
    }

    /**
     * Inits the view members.
     */
    private void initViewMembers() {
        mFragment = this;
        mHeader = (TextView) getActivity().findViewById(R.id.header);
        mBackButton = (Button) getActivity().findViewById(R.id.back);
        mBackButton.setOnClickListener(new OnClickListener() {

            public void onClick(View arg0) {
                FragmentTransaction t = getFragmentManager().beginTransaction();
                t.remove(mFragment);
                t.commit();
                getFragmentManager().popBackStack();
            }
        });
        mSaveButton = (Button) getActivity().findViewById(R.id.save);
        mSaveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (v.getTag() != null && v.getTag().equals("Repeat")) {
                    v.setTag("Save");
                    ((TextView) v).setText("Save");
                    mesureErrors();
                } else {
                    SharedPreferences.Editor pref = PreferenceManager
                            .getDefaultSharedPreferences(getActivity()).edit();
                    pref.putFloat("y_axis_error", mAxisErrorY);
                    pref.putFloat("z_axis_error", mAxisErrorZ);
                    pref.commit();
                    Global.Y_AXIS_CORRECTION = mAxisErrorY;
                    Global.Z_AXIS_CORRECTION = mAxisErrorZ;
                    Toast.makeText(getActivity(), "Values saved",
                            Toast.LENGTH_SHORT).show();
                    getActivity().finish();
                }
            }
        });
    }

    /**
     * Mesure errors. Here we have counter that reads sensor reapetedly and
     * passes info to handler to redraw the plot. We read sensors in same way as
     * in overlay view
     */
    private void mesureErrors() {
        mBackButton.setEnabled(false);
        mSaveButton.setEnabled(false);
        mOrientationDataY.clear();
        mOrientationDataZ.clear();
        CountDownTimer timer = new CountDownTimer(10000, 200) {
            float rotation[] = new float[9];
            float identity[] = new float[9];

            @Override
            public void onTick(long millisUntilFinished) {
                if (mLastAccelerometer != null && mLastCompass != null) {
                    boolean gotRotation = SensorManager.getRotationMatrix(
                            rotation, identity, mLastAccelerometer,
                            mLastCompass);
                    if (gotRotation) {
                        float cameraRotation[] = new float[9];
                        SensorManager.remapCoordinateSystem(rotation,
                                SensorManager.AXIS_X, SensorManager.AXIS_Z,
                                cameraRotation);
                        float orientation[] = new float[3];
                        SensorManager.getOrientation(cameraRotation,
                                orientation);
                        mOrientationDataY.add(Math.toDegrees(orientation[1]));
                        mOrientationDataZ.add(Math.toDegrees(orientation[2]));
                        Log.i("SlopeView", String.format(Locale.getDefault(),
                                "X: %f Y: %f Z: %f",
                                Math.toDegrees(orientation[0]),
                                Math.toDegrees(orientation[1]),
                                Math.toDegrees(orientation[2])));
                        mHandler.sendEmptyMessage(0);
                    }
                }
            }

            @Override
            public void onFinish() {
                playDefaultNotificationSoundAndVibrate();
                mHandler.sendEmptyMessage(1);
            }
        };
        timer.start();
    }

    /**
     * Sets the up plot.
     */
    private void setUpPlot() {
        mPlot = (XYPlot) getActivity().findViewById(R.id.calibrationPlot);
        mPlot.setRangeBoundaries(-90, 90, BoundaryMode.FIXED);
        mPlot.setRangeLabel("Rotation in degrees");
        mPlot.setDomainLabel("Sample number");
        mPlot.setTitle("Z axis rotation");
        mPlot.setDomainBoundaries(0, 50, BoundaryMode.FIXED);
        mPlot.setDomainStepValue(5);
        mPlot.setTicksPerRangeLabel(2);
        LineAndPointFormatter f1 = new LineAndPointFormatter(Color.rgb(200, 10,
                10), Color.rgb(255, 0, 0), Color.rgb(200, 0, 0), null,
                FillDirection.RANGE_ORIGIN);
        f1.getFillPaint().setAlpha(200);
        mPlot.addSeries(mPlotDataZ, f1);
        LineAndPointFormatter f2 = new LineAndPointFormatter(Color.rgb(10, 200,
                10), Color.rgb(0, 255, 0), Color.rgb(0, 255, 0), null,
                FillDirection.RANGE_ORIGIN);
        f2.getFillPaint().setAlpha(200);
        mPlot.addSeries(mPlotDataY, f2);
    }

    /**
     * Register sensors.
     */
    private void registerSensors() {
        mSensorManager = (SensorManager) getActivity().getSystemService(
                Activity.SENSOR_SERVICE);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this,
                mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_NORMAL);
    }

    /**
     * Compute error.
     *
     * @param orientationData the orientation data
     * @param desiredAngle    the desired angle
     * @return the float
     */
    protected float computeError(LinkedList<Number> orientationData,
                                 float desiredAngle) {
        float result = 0f;
        for (Number number : orientationData) {
            result += number.floatValue();
        }
        return desiredAngle
                - Math.abs((result / (float) orientationData.size()));
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.support.v4.app.Fragment#onCreateView(android.view.LayoutInflater,
     * android.view.ViewGroup, android.os.Bundle)
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.calibration_fragment, null);
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.hardware.SensorEventListener#onAccuracyChanged(android.hardware
     * .Sensor, int)
     */
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * android.hardware.SensorEventListener#onSensorChanged(android.hardware
     * .SensorEvent)
     */
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mLastAccelerometer = event.values.clone();
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mLastCompass = event.values.clone();
            default:
                break;
        }
    }

    /**
     * Play default notification sound and vibrate.
     */
    private void playDefaultNotificationSoundAndVibrate() {
        new Thread(new Runnable() {
            public void run() {
                Vibrator v = (Vibrator) getActivity().getSystemService(
                        Activity.VIBRATOR_SERVICE);
                v.vibrate(300);
            }
        }).start();
        Uri notification = RingtoneManager
                .getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Ringtone r = RingtoneManager.getRingtone(getActivity(), notification);
        r.play();
    }

    /*
     * (non-Javadoc)
     *
     * @see android.support.v4.app.Fragment#onDestroy()
     */
    @Override
    public void onDestroy() {
        mSensorManager.unregisterListener(this);
        super.onDestroy();
    }

}