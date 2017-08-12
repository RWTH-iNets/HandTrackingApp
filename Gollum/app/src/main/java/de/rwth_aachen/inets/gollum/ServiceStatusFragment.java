package de.rwth_aachen.inets.gollum;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.DatabaseUtils;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.database.DatabaseUtilsCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.rvalerio.fgchecker.AppChecker;
import com.rvalerio.fgchecker.Utils;

import java.util.ArrayList;
import java.util.List;


public class ServiceStatusFragment extends Fragment {
    private OnFragmentInteractionListener mListener;

    public ServiceStatusFragment() {
        // Required empty public constructor
    }

    public static ServiceStatusFragment newInstance() {
        ServiceStatusFragment fragment = new ServiceStatusFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setHasOptionsMenu(true);
    }

    TextView serviceStatus;
    EditText sessionName;
    Button startStopButton;
    EditText trainingModeUsername;
    ArrayList<Button> trainingModeButtons;
    RadioButton trainingModeType_StandingStill;
    RadioButton trainingModeType_Walking;

    private boolean isServiceRunning = false;
    private String currentSessionName = "";

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_service_status, container, false);

        // Check if the required sensors are available
        if(checkSensors()) {
            v.findViewById(R.id.sensors_not_present).setVisibility(View.GONE);
            v.findViewById(R.id.main_layout).setVisibility(View.VISIBLE);

            serviceStatus = (TextView) v.findViewById(R.id.textView_service_status);
            sessionName = (EditText) v.findViewById(R.id.editText_sessionname);

            startStopButton = (Button) v.findViewById(R.id.service_status_startstopbutton);
            startStopButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    // Request permissions to determine current foreground application if required
                    if(Utils.postLollipop() && !Utils.hasUsageStatsPermission(view.getContext()))
                    {
                        startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
                        return;
                    }

                    if(!verifyPhoneCallPermissions(getActivity()))
                    {
                        return;
                    }

                    // Start/stop service
                    if (isServiceRunning) {
                        stopService();
                    } else {
                        if (sessionName.getText().length() <= 0) {
                            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                            builder.setMessage(R.string.service_status_dialog_empty_session_name)
                                    .setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialogInterface, int i) {
                                            startService("");
                                        }
                                    })
                                    .setNegativeButton(R.string.no, null)
                                    .show();
                        } else {
                            startService(sessionName.getText().toString());
                        }
                    }
                }
            });

            trainingModeType_StandingStill = (RadioButton) v.findViewById(R.id.radioButton_trainingmode_standing);
            trainingModeType_Walking = (RadioButton) v.findViewById(R.id.radioButton_trainingmode_walking);

            trainingModeUsername = (EditText) v.findViewById(R.id.editText_trainingmode_username);
            trainingModeButtons = new ArrayList<>();
            registerTrainingModeButton(v, R.id.button_trainingmode_both_hands_landscape, "bothhandslandscape");
            registerTrainingModeButton(v, R.id.button_trainingmode_left_pocket, "leftpocket");
            registerTrainingModeButton(v, R.id.button_trainingmode_left_hand, "lefthand");
            registerTrainingModeButton(v, R.id.button_trainingmode_left_ear, "leftear");
            registerTrainingModeButton(v, R.id.button_trainingmode_right_pocket, "rightpocket");
            registerTrainingModeButton(v, R.id.button_trainingmode_right_hand, "righthand");
            registerTrainingModeButton(v, R.id.button_trainingmode_right_ear, "rightear");

            changeUIStatus(checkIsServiceRunning());
        } else {
            v.findViewById(R.id.sensors_not_present).setVisibility(View.VISIBLE);
            v.findViewById(R.id.main_layout).setVisibility(View.GONE);
        }

        return v;
    }

    public void registerTrainingModeButton(View v, @IdRes int id, final String session_postfix)
    {
        Button btn = (Button) v.findViewById(id);
        trainingModeButtons.add(btn);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(trainingModeUsername.getText().length() <= 0) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("Please enter a username!")
                            .setPositiveButton(R.string.ok, null)
                            .show();
                } else {
                    String type_string = trainingModeType_StandingStill.isChecked() ? "standing" : "walking";
                    startService("training_" + trainingModeUsername.getText() + "_" + type_string + "_" + session_postfix, 5000, 60000);
                }
            }
        });
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.action_refresh:
                changeUIStatus(checkIsServiceRunning());
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private boolean checkSensors()
    {
        SensorManager mSensorManager = (SensorManager) getActivity().getSystemService(Context.SENSOR_SERVICE);

        List<Sensor> sensors = mSensorManager.getSensorList(Sensor.TYPE_ALL);
        Boolean game_sensor_found = false;
        Log.i("SENSORS", "Sensors present on this phone:");
        for(Sensor s:sensors){
            Log.i("SENSORS", s.getName() + "(Type: " + Integer.toString(s.getType()) + " | Vendor: " + s.getVendor() + " | Version: " + Integer.toString(s.getVersion()) + ")");
            switch(s.getType())
            {
                case Sensor.TYPE_ROTATION_VECTOR:
                case Sensor.TYPE_GAME_ROTATION_VECTOR:
                case Sensor.TYPE_GYROSCOPE:
                case Sensor.TYPE_ACCELEROMETER:
                    game_sensor_found = true;
                    break;
            }
        }

        return game_sensor_found;
    }

    private void startService(final String NewSessionName, long delayMillis, long timeoutMillis)
    {
        sessionName.setText("Service starting in " + Long.toString(delayMillis) + "ms!");
        setInputEnabled(false);

        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startService(NewSessionName);

                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, delayMillis);

        final Handler handler2 = new Handler();
        handler2.postDelayed(new Runnable() {
            @Override
            public void run() {
                stopService();

                try {
                    Uri notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                    Ringtone r = RingtoneManager.getRingtone(getContext(), notification);
                    r.play();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, delayMillis + timeoutMillis);
    }

    private void startService(String NewSessionName)
    {
        if(checkIsServiceRunning() != isServiceRunning)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.service_status_dialog_started)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, null)
                    .show();

            changeUIStatus(true);
        }
        else
        {
            LoggingServiceConfiguration config = new LoggingServiceConfiguration(PreferenceManager.getDefaultSharedPreferences(getActivity()));
            config.SessionName = NewSessionName;

            Intent intent = new Intent(getActivity().getApplicationContext(), LoggingService.class);
            intent.putExtra(LoggingService.CONFIGURATION_TAG, config);

            getActivity().startService(intent);

            currentSessionName = NewSessionName;
            changeUIStatus(true);
        }
    }

    private void stopService()
    {
        if(checkIsServiceRunning() != isServiceRunning)
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.service_status_dialog_stopped)
                    .setCancelable(false)
                    .setPositiveButton(R.string.ok, null)
                    .show();
        }
        else
        {
            getActivity().stopService(new Intent(getActivity().getApplicationContext(), LoggingService.class));
        }

        changeUIStatus(false);
    }

    private void changeUIStatus(boolean newIsServiceRunning)
    {
        isServiceRunning = newIsServiceRunning;
        if(newIsServiceRunning)
        {
            startStopButton.setText(R.string.service_status_stop);
            serviceStatus.setText(R.string.service_status_service_running);
            sessionName.setText(currentSessionName);
            setInputEnabled(false);
        }
        else
        {
            startStopButton.setText(R.string.service_status_start);
            serviceStatus.setText(R.string.service_status_service_not_running);
            setInputEnabled(true);
        }
    }

    private void setInputEnabled(boolean enabled)
    {
        sessionName.setEnabled(enabled);

        trainingModeUsername.setEnabled(enabled);
        for(Button btn:trainingModeButtons)
            btn.setEnabled(enabled);
    }

    private boolean checkIsServiceRunning()
    {
        ActivityManager manager = (ActivityManager)getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE))
        {
            if(LoggingService.class.getName().equals(service.service.getClassName()))
                return true;
        }
        return false;
    }

    public static boolean verifyPhoneCallPermissions(Activity activity) {
        // Check if we have permissions
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.READ_PHONE_STATE},
                    MainActivity.PERMISSION_REQUEST_PHONE_STATE);

            return false;
        }

        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.PROCESS_OUTGOING_CALLS) != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.PROCESS_OUTGOING_CALLS},
                    MainActivity.PERMISSION_REQUEST_OUTGOING_CALLS);

            return false;
        }

        return true;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }
}
