package de.rwth_aachen.inets.gollum;

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
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Telephony;
import android.support.v7.app.NotificationCompat;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.rvalerio.fgchecker.AppChecker;

public class LoggingService extends Service implements SensorEventListener
{
    public static final String CONFIGURATION_TAG = "Configuration";

    public static final int NOTIFICATION_ID = 1;

    private SensorManager mSensorManager;
    private Sensor mRotationVectorSensor;
    private Sensor mGyroscopeSensor;
    private Sensor mAccelerometerSensor;
    private Sensor mMagnetometerSensor;
    private Sensor mProximitySensor;
    private Sensor mLightSensor;
    private Sensor mPressureSensor;
    private Sensor mAmbientTemperatureSensor;
    private Handler mTaskHandler = new Handler();
    private LoggingServiceConfiguration mConfiguration;
    private long mCurrentLogSessionID;

    public enum LogEventTypes
    {
        LOG_STARTED(0),
        LOG_STOPPED(1),
        ROTATION_VECTOR(2),
        SCREEN_ON_OFF(3),
        GAME_ROTATION_VECTOR(4),
        GYROSCOPE(5),
        ACCELEROMETER(6),
        MAGNETOMETER(7),
        PROXIMITY(8),
        LIGHT(9),
        PRESSURE(10),
        AMBIENT_TEMPERATURE(11),
        TRAFFIC_STATS(12),
        FOREGROUND_APPLICATION(13),
        POWER_CONNECTED(14),
        DAYDREAM_ACTIVE(15),
        PHONE_CALL(16),
        SMS_RECEIVED(17);

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

    public enum PhoneCallEvents
    {
        INCOMING_CALL(0),
        INCOMING_CALL_ATTENDED(1),
        INCOMING_CALL_MISSED(2),
        OUTGOING_CALL_PLACED(3),
        CALL_ENDED(4);

        private final int value;

        PhoneCallEvents(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }

        public static PhoneCallEvents fromInt(int value)
        {
            PhoneCallEvents[] values = values();
            for(PhoneCallEvents type : values)
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

    private String mLastCallNumber = null;
    private String mLastCallState = null;

    private final BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver()
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            switch(intent.getAction())
            {
                // Screen on/off?
                case Intent.ACTION_SCREEN_ON:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.SCREEN_ON_OFF, 1);

                    if(mConfiguration.SamplingBehavior == LoggingServiceConfiguration.SamplingBehaviors.SCREEN_ON)
                    {
                        startLogging();
                    }
                    break;
                case Intent.ACTION_SCREEN_OFF:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.SCREEN_ON_OFF, 0);

                    if(mConfiguration.SamplingBehavior == LoggingServiceConfiguration.SamplingBehaviors.SCREEN_ON)
                    {
                        stopLogging();
                    }
                    break;

                // Daydream on/off?
                case Intent.ACTION_DREAMING_STARTED:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.DAYDREAM_ACTIVE, 1);
                    break;
                case Intent.ACTION_DREAMING_STOPPED:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.DAYDREAM_ACTIVE, 0);
                    break;

                // Power cord plugged in?
                case Intent.ACTION_POWER_CONNECTED:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.POWER_CONNECTED, 1);
                    break;
                case Intent.ACTION_POWER_DISCONNECTED:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.POWER_CONNECTED, 0);
                    break;

                // Phone state
                case Intent.ACTION_NEW_OUTGOING_CALL: {
                    final Bundle extras = intent.getExtras();
                    if (extras != null) {
                        mLastCallNumber = extras.getString(Intent.EXTRA_PHONE_NUMBER);
                        getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.PHONE_CALL, PhoneCallEvents.OUTGOING_CALL_PLACED.getValue(), mLastCallNumber);
                    }
                    break;
                }
                case TelephonyManager.ACTION_PHONE_STATE_CHANGED: {
                    // See https://stackoverflow.com/questions/8547519/how-to-detect-incoming-call-status-in-android?lq=1
                    final Bundle extras = intent.getExtras();
                    if (extras != null) {
                        final String state = extras.getString(TelephonyManager.EXTRA_STATE);
                        if (state != null) {
                            if (state.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                                mLastCallNumber = extras.getString(TelephonyManager.EXTRA_INCOMING_NUMBER);
                                getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.PHONE_CALL, PhoneCallEvents.INCOMING_CALL.getValue(), mLastCallNumber);
                            }
                            if (mLastCallState != null) {
                                if (state.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                                    if (mLastCallState.equals(TelephonyManager.EXTRA_STATE_RINGING) && mLastCallNumber != null) {
                                        // Call has been attended
                                        getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.PHONE_CALL, PhoneCallEvents.INCOMING_CALL_ATTENDED.getValue(), mLastCallNumber);
                                    }
                                } else if (state.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                                    if (mLastCallState.equals(TelephonyManager.EXTRA_STATE_RINGING) && mLastCallNumber != null) {
                                        // Call has been missed
                                        getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.PHONE_CALL, PhoneCallEvents.INCOMING_CALL_MISSED.getValue(), mLastCallNumber);
                                    } else if (mLastCallState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                                        // Call has ended
                                        getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.PHONE_CALL, PhoneCallEvents.CALL_ENDED.getValue(), mLastCallNumber);
                                    }

                                    mLastCallNumber = null;
                                }
                            }

                            mLastCallState = state;
                        }
                    }
                    break;
                }

                // SMS
                case Telephony.Sms.Intents.SMS_RECEIVED_ACTION:
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.SMS_RECEIVED);
                    break;
            }
        }
    };

    public LoggingService()
    {

    }

    private long mLastMobileRx;
    private long mLastMobileTx;
    private long mLastTotalRx;
    private long mLastTotalTx;

    private Runnable mNetworkTrafficLogger = new Runnable() {
        @Override
        public void run() {
            long CurMobileRx = TrafficStats.getMobileRxBytes();
            long CurMobileTx = TrafficStats.getMobileTxBytes();
            long CurTotalRx = TrafficStats.getTotalRxBytes();
            long CurTotalTx = TrafficStats.getTotalTxBytes();

            long DeltaMobileRx = CurMobileRx - mLastMobileRx;
            long DeltaMobileTx = CurMobileTx - mLastMobileTx;
            long DeltaWiFiRx = (CurTotalRx - mLastTotalRx) - DeltaMobileRx;
            long DeltaWiFiTx = (CurTotalTx - mLastTotalTx) - DeltaMobileTx;

            getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.TRAFFIC_STATS, (int)DeltaMobileRx, (int)DeltaMobileTx, (int)DeltaWiFiRx, (int)DeltaWiFiTx);

            mLastMobileRx = CurMobileRx;
            mLastMobileTx = CurMobileTx;
            mLastTotalRx = CurTotalRx;
            mLastTotalTx = CurTotalTx;

            mTaskHandler.postDelayed(mNetworkTrafficLogger, 50);
        }
    };

    private String mLastForegroundApplication = "";
    private AppChecker mForegroundAppChecker = new AppChecker();

    public void startForegroundAppChecker()
    {
        mForegroundAppChecker.other(new AppChecker.Listener() {
            @Override
            public void onForeground(String packageName) {
                if(!packageName.equals(mLastForegroundApplication))
                {
                    mLastForegroundApplication = packageName;
                    getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.FOREGROUND_APPLICATION, mLastForegroundApplication);
                }
            }
        });

        mForegroundAppChecker.timeout(50);
        mForegroundAppChecker.start(getApplicationContext());
    }

    public void stopForegroundAppChecker()
    {
        mForegroundAppChecker.stop();
    }

    @Override
    public void onCreate()
    {

    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent != null) {
            mConfiguration = (LoggingServiceConfiguration) intent.getSerializableExtra(CONFIGURATION_TAG);

            // Initialize sensors
            mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);

            mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GAME_ROTATION_VECTOR);
            if (mRotationVectorSensor == null)
                mRotationVectorSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);

            mGyroscopeSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
            mAccelerometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            mMagnetometerSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            mProximitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            mLightSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
            mPressureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
            mAmbientTemperatureSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE);

            // Initialize log session
            mCurrentLogSessionID = getDBHelper().insertLogSession(mConfiguration);
            getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.LOG_STARTED);

            // This needs to happen after session created
            startLogging();

            NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
            builder.setSmallIcon(R.drawable.ic_graphic_eq_black_24dp);
            builder.setContentTitle(getText(R.string.service_notification_title));
            builder.setContentText(getText(R.string.service_notification_text));

            Intent touchIntent = new Intent(this, MainActivity.class);
            PendingIntent touchPendingIntent = PendingIntent.getActivity(this, 0, touchIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            builder.setContentIntent(touchPendingIntent);

            startForeground(NOTIFICATION_ID, builder.build());
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy()
    {
        stopLogging();

        //this needs to come after listeners have been detached.
        getDBHelper().insertLogEntry(mCurrentLogSessionID, SystemClock.elapsedRealtimeNanos(), LogEventTypes.LOG_STOPPED);

        stopForeground(true);
    }

    private void startLogging()
    {
        IntentFilter filter = new IntentFilter();
        filter.addAction(Intent.ACTION_SCREEN_ON);
        filter.addAction(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_DREAMING_STARTED);
        filter.addAction(Intent.ACTION_DREAMING_STOPPED);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED);
        filter.addAction(Intent.ACTION_NEW_OUTGOING_CALL);
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED);
        filter.addAction(Telephony.Sms.Intents.SMS_RECEIVED_ACTION);
        registerReceiver(mBroadcastReceiver, filter);

        startLoggingSensors();

        mLastMobileRx = TrafficStats.getMobileRxBytes();
        mLastMobileTx = TrafficStats.getMobileTxBytes();
        mLastTotalRx = TrafficStats.getTotalRxBytes();
        mLastTotalTx = TrafficStats.getTotalTxBytes();
        mNetworkTrafficLogger.run();

        startForegroundAppChecker();
    }

    private void stopLogging()
    {
        stopForegroundAppChecker();

        mTaskHandler.removeCallbacks(mNetworkTrafficLogger);

        stopLoggingSensors();

        unregisterReceiver(mBroadcastReceiver);
    }

    private void startLoggingSensors()
    {
        if(mRotationVectorSensor != null) {
            Log.i("SENSOR", "Using RotationVector sensor [" + mRotationVectorSensor.getName() + "]");
            mSensorManager.registerListener(this, mRotationVectorSensor, mConfiguration.SamplingInterval);
        } else {
            Log.e("SENSOR", "No RotationVector sensor available!");
        }

        if(mGyroscopeSensor != null) {
            Log.i("SENSOR", "Using Gyroscope sensor [" + mGyroscopeSensor.getName() + "]");
            mSensorManager.registerListener(this, mGyroscopeSensor, mConfiguration.SamplingInterval);
        } else {
            Log.e("SENSOR", "No Gyroscope sensor available!");
        }

        if(mAccelerometerSensor != null) {
            Log.i("SENSOR", "Using Accelerometer sensor [" + mAccelerometerSensor.getName() + "]");
            mSensorManager.registerListener(this, mAccelerometerSensor, mConfiguration.SamplingInterval);
        } else {
            Log.e("SENSOR", "No Accelerometer sensor available!");
        }

        if(mMagnetometerSensor != null) {
            Log.i("SENSOR", "Using Magnetometer sensor [" + mMagnetometerSensor.getName() + "]");
            mSensorManager.registerListener(this, mMagnetometerSensor, mConfiguration.SamplingInterval);
        } else {
            Log.e("SENSOR", "No Magnetometer sensor available!");
        }

        if(mProximitySensor != null) {
            Log.i("SENSOR", "Using Proximity sensor [" + mProximitySensor.getName() + "]");
            mSensorManager.registerListener(this, mProximitySensor, mConfiguration.SamplingInterval);
        } else {
            Log.e("SENSOR", "No Proximity sensor available!");
        }

        if(mLightSensor != null) {
            Log.i("SENSOR", "Using Light sensor [" + mLightSensor.getName() + "]");
            mSensorManager.registerListener(this, mLightSensor, mConfiguration.SamplingInterval);
        } else {
            Log.e("SENSOR", "No Light sensor available!");
        }

        if(mPressureSensor != null) {
            Log.i("SENSOR", "Using Pressure sensor [" + mPressureSensor.getName() + "]");
            mSensorManager.registerListener(this, mPressureSensor, mConfiguration.SamplingInterval);
        } else {
            Log.e("SENSOR", "No Pressure sensor available!");
        }

        if(mAmbientTemperatureSensor != null) {
            Log.i("SENSOR", "Using Ambient Temperature sensor [" + mAmbientTemperatureSensor.getName() + "]");
            mSensorManager.registerListener(this, mAmbientTemperatureSensor, mConfiguration.SamplingInterval);
        } else {
            Log.e("SENSOR", "No Ambient Temperature sensor available!");
        }
    }

    private void stopLoggingSensors()
    {
        mSensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        LogEventTypes log_event_type;
        int num_values;
        switch(sensorEvent.sensor.getType())
        {
            case Sensor.TYPE_ROTATION_VECTOR:
                log_event_type = LogEventTypes.ROTATION_VECTOR;
                num_values = 4;
                break;
            case Sensor.TYPE_GAME_ROTATION_VECTOR:
                log_event_type = LogEventTypes.GAME_ROTATION_VECTOR;
                num_values = 4;
                break;
            case Sensor.TYPE_GYROSCOPE:
                log_event_type = LogEventTypes.GYROSCOPE;
                num_values = 3;
                break;
            case Sensor.TYPE_ACCELEROMETER:
                log_event_type = LogEventTypes.ACCELEROMETER;
                num_values = 3;
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                log_event_type = LogEventTypes.MAGNETOMETER;
                num_values = 3;
                break;
            case Sensor.TYPE_PROXIMITY:
                log_event_type = LogEventTypes.PROXIMITY;
                num_values = 1;
                break;
            case Sensor.TYPE_LIGHT:
                log_event_type = LogEventTypes.LIGHT;
                num_values = 1;
                break;
            case Sensor.TYPE_PRESSURE:
                log_event_type = LogEventTypes.PRESSURE;
                num_values = 1;
                break;
            case Sensor.TYPE_AMBIENT_TEMPERATURE:
                log_event_type = LogEventTypes.AMBIENT_TEMPERATURE;
                num_values = 1;
                break;
            default:
                throw new RuntimeException("Invalid Sensor Type!");
        }

        if(sensorEvent.values.length < num_values)
            throw new RuntimeException("Not enough sensor values!");

        float[] values = {0.0f, 0.0f, 0.0f, 0.0f};
        System.arraycopy(sensorEvent.values, 0, values, 0, num_values);
        getDBHelper().insertLogEntry(mCurrentLogSessionID, sensorEvent.timestamp,
                log_event_type, values[0], values[1], values[2], values[3]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy)
    {

    }
}
