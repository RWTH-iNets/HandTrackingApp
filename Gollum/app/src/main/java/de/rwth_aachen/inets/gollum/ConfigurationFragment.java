package de.rwth_aachen.inets.gollum;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.preference.PreferenceFragment;
import android.support.v7.preference.PreferenceManager;
import android.widget.Toast;

/**
 * Created by androiddev on 28.10.16.
 */

public class ConfigurationFragment extends PreferenceFragmentCompat
{
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey)
    {
        addPreferencesFromResource(R.xml.configuration);
    }
}
