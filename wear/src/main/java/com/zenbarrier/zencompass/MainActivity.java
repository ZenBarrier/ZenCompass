package com.zenbarrier.zencompass;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity implements SensorEventListener{

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("hh:mm a", Locale.US);

    private WearableDrawerLayout mContainerView;
    private TextView mTextRotation;
    private TextView mClockView;
    private ImageView mCompassImage;
    private SensorManager mSensorManager;
    private Sensor mCompass;
    private Sensor mAccelerometer;

    private float[] mMagneticData;
    private float[] mAccelerometerData;
    private float[] mIdentityMatrix;
    private float[] mRotationMatrix;
    private float[] mOrientationMatrix;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (WearableDrawerLayout) findViewById(R.id.container);
        mTextRotation = (TextView) findViewById(R.id.textView_rotation);
        mClockView = (TextView) findViewById(R.id.clock);
        mCompassImage = (ImageView) findViewById(R.id.imageView_compass);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        mMagneticData = new float[3];
        mAccelerometerData = new float[3];
        mIdentityMatrix = new float[9];
        mRotationMatrix = new float[9];
        mOrientationMatrix = new float[3];
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        mSensorManager.unregisterListener(this);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        super.onExitAmbient();
    }

    private void updateDisplay() {
        if (isAmbient()) {
            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black, null));
            mClockView.setVisibility(View.VISIBLE);
            mTextRotation.setVisibility(View.GONE);
            mCompassImage.setImageResource(R.drawable.ic_ambient_compass);
            mCompassImage.setRotation(0);

            mClockView.setText(AMBIENT_DATE_FORMAT.format(new Date()));
        } else {
            mContainerView.setBackground(null);
            mCompassImage.setImageResource(R.drawable.ic_compass_background_rotate);
            mClockView.setVisibility(View.GONE);
            mTextRotation.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        switch (event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
                System.arraycopy(event.values, 0, mAccelerometerData, 0, 3);
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                System.arraycopy(event.values, 0, mMagneticData, 0, 3);
                break;
        }
        SensorManager.getRotationMatrix(mRotationMatrix, mIdentityMatrix, mAccelerometerData, mMagneticData);
        SensorManager.getOrientation(mRotationMatrix, mOrientationMatrix);
        float rotationRadian = mOrientationMatrix[0];
        double mRotationDegrees = Math.toDegrees(rotationRadian);
        mCompassImage.setRotation((float) -mRotationDegrees);
        mTextRotation.setText(String.valueOf((int)((mRotationDegrees >= 0 ? 0 : 360)+mRotationDegrees)));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
