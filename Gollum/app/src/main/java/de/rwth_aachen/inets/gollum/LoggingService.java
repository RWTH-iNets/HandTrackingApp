package de.rwth_aachen.inets.gollum;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.JsonReader;
import android.util.Log;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

public class LoggingService extends Service implements SensorEventListener
{
    public static final String CONFIGURATION_TAG = "Configuration";

    public static final int NOTIFICATION_ID = 1;

    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;
    private LoggingServiceConfiguration mConfiguration;
    private long mCurrentLogSessionID;
    private long mCurrentLogSessionStartTime;

    public enum LogEventTypes
    {
        LOG_STARTED(0),
        LOG_STOPPED(1),
        ROTATION_VECTOR(2),
        SCREEN_ON_OFF(3),
        GAME_ROTATION_VECTOR(4),
        GYRO(5);

        private final int value;

        LogEventTypes(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }

        public static LogEventTypes fromInt(int value)
        {
            LogEventTypes[] values = values();
            for(LogEventTypes type : values)
            {
                if(type.getValue() == value)
                    return type;
            }

            return null;
        }
    };

    private LoggingServiceDBHelper getDBHelper()
    {
        return LoggingServiceDBHelper.getInstance(this);
    }

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            switch(intent.getAction())
            {
                case Intent.ACTION_SCREEN_ON:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, getCurrentSessionTime(), LogEventTypes.SCREEN_ON_OFF, 1);

                    if(mConfiguration.SamplingBehavior == LoggingServiceConfiguration.SamplingBehaviors.SCREEN_ON)
                    {
                        startLoggingSensors();
                    }
                    break;

                case Intent.ACTION_SCREEN_OFF:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, getCurrentSessionTime(), LogEventTypes.SCREEN_ON_OFF, 0);

                    if(mConfiguration.SamplingBehavior == LoggingServiceConfiguration.SamplingBehaviors.SCREEN_ON)
                    {
                        stopLoggingSensors();
                    }
                    break;
            }
        }
    };

    public LoggingService()
    {

    }

    @Override
    public void onCreate()
    {

    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        mConfiguration = (LoggingServiceConfiguration) intent.getSerializableExtra(CONFIGURATION_TAG);

        // Initialize BroadcastReceiver
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        registerReceiver(mBroadcastReceiver, filter);

        // Initialize RotationVector sensor
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

        mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
        if (mRotationVectorSensor == null){
            if (mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) != null){
                mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            }
            else{
                mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            }
        }

        if(mRotationVectorSensor == null) {
            //Ouups
        }

        // Initialize log session
        mCurrentLogSessionID = getDBHelper().insertLogSession(mConfiguration);
        mCurrentLogSessionStartTime = System.nanoTime();
        getDBHelper().insertLogEntry(mCurrentLogSessionID, getCurrentSessionTime(), LogEventTypes.LOG_STARTED);

        //this needs to happen after session created
        startLoggingSensors();

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.drawable.ic_graphic_eq_black_24dp);
        builder.setContentTitle(getText(R.string.service_notification_title));
        builder.setContentText(getText(R.string.service_notification_text));

        Intent touchIntent = new Intent(this, MainActivity.class);
        PendingIntent touchPendingIntent = PendingIntent.getActivity(this, 0, touchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        builder.setContentIntent(touchPendingIntent);

        startForeground(NOTIFICATION_ID, builder.build());

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        stopLoggingSensors();
        unregisterReceiver(mBroadcastReceiver);

        //this needs to come after listeners have been detached.
        getDBHelper().insertLogEntry(mCurrentLogSessionID, getCurrentSessionTime(), LogEventTypes.LOG_STOPPED);

        stopForeground(true);
    }

    private void startLoggingSensors()
    {
        mSensorManager.registerListener(this, mRotationVectorSensor, mConfiguration.SamplingInterval);
    }

    private void stopLoggingSensors()
    {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        int type = sensorEvent.sensor.getType();
        LogEventTypes log_event_type = LogEventTypes.ROTATION_VECTOR;

        if(type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            log_event_type = LogEventTypes.GAME_ROTATION_VECTOR;
        }
        if(type == Sensor.TYPE_GYROSCOPE) {
            log_event_type = LogEventTypes.GYRO;
        }

        //first three elements are last three elements of a unit quaternion. 4th element is optional
        //we want to use event time not the time when the data is stored => use sensorEvent.timestamp
        if(sensorEvent.values.length == 4) {
            getDBHelper().insertLogEntry(mCurrentLogSessionID, sensorEvent.timestamp,
                    log_event_type, sensorEvent.values[0], sensorEvent.values[1],
                    sensorEvent.values[2], sensorEvent.values[3]);
        } else {
            getDBHelper().insertLogEntry(mCurrentLogSessionID, sensorEvent.timestamp,
                    log_event_type, sensorEvent.values[0], sensorEvent.values[1],
                    sensorEvent.values[2], 0);
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }

    private long getCurrentSessionTime()
    {
        return System.nanoTime() - mCurrentLogSessionStartTime;
    }


}
