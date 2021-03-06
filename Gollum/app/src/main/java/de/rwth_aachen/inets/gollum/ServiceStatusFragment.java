package de.rwth_aachen.inets.gollum;

import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.DatabaseUtils;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
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
import android.widget.TextView;

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

            trainingModeUsername = (EditText) v.findViewById(R.id.editText_trainingmode_username);

            trainingModeButtons = new ArrayList<>();
            Button trainingRightPocket = (Button) v.findViewById(R.id.button_trainingmode_right_pocket);
            trainingModeButtons.add(trainingRightPocket);
            trainingRightPocket.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(trainingModeUsername.getText().length() <= 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage("Please enter a username!")
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    } else {
                        startService("training_" + trainingModeUsername.getText() + "_rightpocket");
                    }
                }
            });

            Button trainingRightHand = (Button) v.findViewById(R.id.button_trainingmode_right_hand);
            trainingModeButtons.add(trainingRightHand);
            trainingRightHand.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(trainingModeUsername.getText().length() <= 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage("Please enter a username!")
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    } else {
                        startService("training_" + trainingModeUsername.getText() + "_righthand");
                    }
                }
            });

            Button trainingRightEar = (Button) v.findViewById(R.id.button_trainingmode_right_ear);
            trainingModeButtons.add(trainingRightEar);
            trainingRightEar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if(trainingModeUsername.getText().length() <= 0) {
                        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                        builder.setMessage("Please enter a username!")
                                .setPositiveButton(R.string.ok, null)
                                .show();
                    } else {
                        startService("training_" + trainingModeUsername.getText() + "_rightear");
                    }
                }
            });

            changeUIStatus(checkIsServiceRunning());
        } else {
            v.findViewById(R.id.sensors_not_present).setVisibility(View.VISIBLE);
            v.findViewById(R.id.main_layout).setVisibility(View.GONE);
        }

        return v;
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

    private void startService(String sessionName)
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
            config.SessionName = sessionName;

            Intent intent = new Intent(getActivity().getApplicationContext(), LoggingService.class);
            intent.putExtra(LoggingService.CONFIGURATION_TAG, config);

            getActivity().startService(intent);

            currentSessionName = sessionName;
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
            sessionName.setEnabled(false);
            sessionName.setText(currentSessionName);

            trainingModeUsername.setEnabled(false);
            for(Button btn:trainingModeButtons)
                btn.setEnabled(false);
        }
        else
        {
            startStopButton.setText(R.string.service_status_start);
            serviceStatus.setText(R.string.service_status_service_not_running);
            sessionName.setEnabled(true);

            trainingModeUsername.setEnabled(true);
            for(Button btn:trainingModeButtons)
                btn.setEnabled(true);
        }
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
