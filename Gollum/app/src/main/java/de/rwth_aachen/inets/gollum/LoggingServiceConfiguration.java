package de.rwth_aachen.inets.gollum;

import android.content.SharedPreferences;

import java.io.Serializable;

final class LoggingServiceConfiguration implements Serializable
{
    public String SamplingFrequency = "-1";
    public String SessionName = "";

    public LoggingServiceConfiguration()
    { }
    public LoggingServiceConfiguration(SharedPreferences preferences)
    {
        SamplingFrequency = preferences.getString("sampling_frequency", "-1");
    }
}
