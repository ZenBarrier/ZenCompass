package com.zenbarrier.zencompass;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.drawer.WearableActionDrawer;
import android.support.wearable.view.drawer.WearableDrawerLayout;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends WearableActivity implements
        SensorEventListener,
        WearableActionDrawer.OnMenuItemClickListener{

    private static final String KEY_PREF_IS_MILITARY_TIME = "KEY_PREF_IS_MILITARY_TIME";
    private static final String KEY_PREF_IS_SHOWING_DEGREES = "KEY_PREF_IS_SHOWING_DEGREES";
    private static final String KEY_PREF_HAS_POINTER = "KEY_PREF_HAS_POINTER";
    private SimpleDateFormat mAmbientDateFormat;
    private static final long SHOW_DRAWER_TIME = 3000;

    private WearableDrawerLayout mContainerView;
    private TextView mTextRotation;
    private TextView mClockView;
    private ImageView mCompassImage;
    private ImageView mPointerImage;
    private SensorManager mSensorManager;
    private Sensor mCompass;
    private Sensor mAccelerometer;
    private WearableActionDrawer mActionDrawer;
    private Handler mDrawerHandler;
    private SharedPreferences mSharedPreferences;

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

        mDrawerHandler = new Handler();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        mTextRotation = (TextView) findViewById(R.id.textView_rotation);
        mClockView = (TextView) findViewById(R.id.clock);
        mCompassImage = (ImageView) findViewById(R.id.imageView_compass);
        mPointerImage = (ImageView) findViewById(R.id.imageView_pointer);
        mActionDrawer = (WearableActionDrawer) findViewById(R.id.bottom_action_drawer);
        mActionDrawer.setOnMenuItemClickListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mCompass = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        setMenuItems();

        mMagneticData = new float[3];
        mAccelerometerData = new float[3];
        mIdentityMatrix = new float[9];
        mRotationMatrix = new float[9];
        mOrientationMatrix = new float[3];
    }

    private void setMenuItems(){

        MenuItem clockMenuItem = mActionDrawer.getMenu().findItem(R.id.menu_clock_format);
        if(mSharedPreferences.getBoolean(KEY_PREF_IS_MILITARY_TIME, false)){
            clockMenuItem.setTitle(R.string.menu_ambient_clock_24);
            mAmbientDateFormat =
                    new SimpleDateFormat("HH:mm", Locale.US);
        }else{
            clockMenuItem.setTitle(R.string.menu_ambient_clock_12);
            mAmbientDateFormat =
                    new SimpleDateFormat("hh:mm a", Locale.US);
        }
        MenuItem degreeMenuItem = mActionDrawer.getMenu().findItem(R.id.menu_degree_show);
        if(mSharedPreferences.getBoolean(KEY_PREF_IS_SHOWING_DEGREES, false)){
            degreeMenuItem.setTitle(R.string.menu_hide_degrees);
            mTextRotation.setVisibility(View.VISIBLE);
        }else{
            degreeMenuItem.setTitle(R.string.menu_show_degrees);
            mTextRotation.setVisibility(View.GONE);
        }

        MenuItem pointerMenuItem = mActionDrawer.getMenu().findItem(R.id.menu_compass_pointer);
        if(mSharedPreferences.getBoolean(KEY_PREF_HAS_POINTER, false)){
            pointerMenuItem.setTitle(R.string.menu_hide_needle);
            mCompassImage.setImageResource(R.drawable.ic_compass_background_static);
            mPointerImage.setVisibility(View.VISIBLE);
            mCompassImage.setRotation(0);
        }else{
            mCompassImage.setImageResource(R.drawable.ic_compass_background_rotate);
            mPointerImage.setVisibility(View.GONE);
            pointerMenuItem.setTitle(R.string.menu_show_needle);
        }
    }

    public void showDrawerHint(View view) {
        mActionDrawer.peekDrawer();
        mDrawerHandler.removeCallbacksAndMessages(null);
        if (mActionDrawer.isClosed()) {
            mDrawerHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    if (mActionDrawer.isPeeking()) {
                        mActionDrawer.closeDrawer();
                    }
                }
            }, SHOW_DRAWER_TIME);
        }else if(mActionDrawer.isPeeking()){
            mActionDrawer.closeDrawer();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mSensorManager.registerListener(this, mCompass, SensorManager.SENSOR_DELAY_NORMAL);
        mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_NORMAL);

        showDrawerHint(null);
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
            mPointerImage.setVisibility(View.GONE);
            mCompassImage.setRotation(0);

            mClockView.setText(mAmbientDateFormat.format(new Date()));
        } else {
            mContainerView.setBackgroundColor(Color.DKGRAY);
            mClockView.setVisibility(View.GONE);
            if(mSharedPreferences.getBoolean(KEY_PREF_IS_SHOWING_DEGREES, false)) {
                mTextRotation.setVisibility(View.VISIBLE);
            }
            if(mSharedPreferences.getBoolean(KEY_PREF_HAS_POINTER, false)) {
                mPointerImage.setVisibility(View.VISIBLE);
                mCompassImage.setImageResource(R.drawable.ic_compass_background_static);
            }else{
                mCompassImage.setImageResource(R.drawable.ic_compass_background_rotate);
            }
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
        if(mSharedPreferences.getBoolean(KEY_PREF_HAS_POINTER, false)) {
            mPointerImage.setRotation((float) -mRotationDegrees);
        }else{
            mCompassImage.setRotation((float) -mRotationDegrees);
        }
        mTextRotation.setText(String.valueOf((int)((mRotationDegrees >= 0 ? 0 : 360)+mRotationDegrees)));
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        if(sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD && accuracy == SensorManager.SENSOR_STATUS_ACCURACY_LOW){
            findViewById(R.id.imageView_calibrate).setVisibility(View.VISIBLE);
        }else{
            findViewById(R.id.imageView_calibrate).setVisibility(View.GONE);
        }
    }

    @Override
    public boolean onMenuItemClick(MenuItem menuItem) {

        switch (menuItem.getItemId()){
            case R.id.menu_compass_pointer:
                boolean hasPointer = mSharedPreferences.getBoolean(KEY_PREF_HAS_POINTER, false);
                mSharedPreferences.edit().putBoolean(KEY_PREF_HAS_POINTER, !hasPointer).apply();
                setMenuItems();
                break;
            case R.id.menu_clock_format:
                boolean clockFormat = mSharedPreferences.getBoolean(KEY_PREF_IS_MILITARY_TIME, false);
                mSharedPreferences.edit().putBoolean(KEY_PREF_IS_MILITARY_TIME, !clockFormat).apply();
                setMenuItems();
                break;
            case R.id.menu_degree_show:
                boolean isShowingDegrees = mSharedPreferences.getBoolean(KEY_PREF_IS_SHOWING_DEGREES, false);
                mSharedPreferences.edit().putBoolean(KEY_PREF_IS_SHOWING_DEGREES, !isShowingDegrees).apply();
                setMenuItems();
                break;
            case R.id.menu_close:
                mActionDrawer.closeDrawer();
                break;
        }
        mActionDrawer.closeDrawer();

        return true;
    }
}
