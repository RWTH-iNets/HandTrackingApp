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
        SCREEN_ON_OFF(3);

        private final int value;

        private LogEventTypes(int value)
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
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, getCurrentSessionTime(), LogEventTypes.SCREEN_ON_OFF, 0);
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
        mSensorManager.registerListener(this, mRotationVectorSensor, SensorManager.SENSOR_DELAY_FASTEST);

        // Initialize log session
        mCurrentLogSessionID = getDBHelper().insertLogSession(mConfiguration);
        mCurrentLogSessionStartTime = System.nanoTime();
        getDBHelper().insertLogEntry(mCurrentLogSessionID, getCurrentSessionTime(), LogEventTypes.LOG_STARTED);

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
        getDBHelper().insertLogEntry(mCurrentLogSessionID, getCurrentSessionTime(), LogEventTypes.LOG_STOPPED);

        unregisterReceiver(mBroadcastReceiver);
        mSensorManager.unregisterListener(this);

        stopForeground(true);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        getDBHelper().insertLogEntry(mCurrentLogSessionID, getCurrentSessionTime(), LogEventTypes.ROTATION_VECTOR, sensorEvent.values[0], sensorEvent.values[1], sensorEvent.values[2], sensorEvent.values[3]);
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
