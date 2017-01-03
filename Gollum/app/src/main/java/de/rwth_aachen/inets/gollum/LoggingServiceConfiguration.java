package de.rwth_aachen.inets.gollum;

import android.content.SharedPreferences;
import android.hardware.SensorManager;

import java.io.Serializable;

final class LoggingServiceConfiguration implements Serializable
{
    public enum SamplingBehaviors
    {
        ALWAYS_ON(0),
        SCREEN_ON(1);

        private final int value;

        private SamplingBehaviors(int value)
        {
            this.value = value;
        }

        public int getValue()
        {
            return value;
        }

        public static SamplingBehaviors fromInt(int value)
        {
            SamplingBehaviors[] values = values();
            for(SamplingBehaviors type : values)
            {
                if(type.getValue() == value)
                    return type;
            }

            return null;
        }
    }

    public enum PreprocessingModes
    {
        None,
    }

    public int SamplingInterval = SensorManager.SENSOR_DELAY_FASTEST;
    public SamplingBehaviors SamplingBehavior = SamplingBehaviors.ALWAYS_ON;
    public PreprocessingModes Preprocessing = PreprocessingModes.None;
    public String SessionName = "";

    public LoggingServiceConfiguration()
    { }
    public LoggingServiceConfiguration(SharedPreferences preferences)
    {
        String strSamplingInterval = preferences.getString("sampling_interval", "fastest");
        switch (strSamplingInterval)
        {
            case "25ms":
                SamplingInterval = 25000;
                break;
            case "50ms":
                SamplingInterval = 50000;
                break;
            case "100ms":
                SamplingInterval = 100000;
                break;
            default:
                SamplingInterval = SensorManager.SENSOR_DELAY_FASTEST;
        }

        String strSamplingBehavior = preferences.getString("logging_behavior", "alwaysOn");
        switch (strSamplingBehavior)
        {
            case "screenOn":
                SamplingBehavior = SamplingBehaviors.SCREEN_ON;
                break;
            default:
                SamplingBehavior = SamplingBehaviors.ALWAYS_ON;
        }

        String strPreprocessing = preferences.getString("preprocessing", "none");
        switch (strPreprocessing)
        {
            default:
                Preprocessing = PreprocessingModes.None;
        }
    }
}
