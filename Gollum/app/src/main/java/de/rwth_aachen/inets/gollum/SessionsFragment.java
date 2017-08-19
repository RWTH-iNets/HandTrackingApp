package de.rwth_aachen.inets.gollum;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.icu.text.DateFormat;
import android.icu.text.SimpleDateFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.CursorAdapter;
import android.support.v7.app.AlertDialog;
import android.util.JsonWriter;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.CheckBox;
import android.widget.Checkable;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.os.Environment.getExternalStorageState;


public class SessionsFragment extends Fragment implements IUploadProgress {

    private boolean upload_running = false;
    private ListView listView = null;
    private SessionsAdapter adapter = null;
    public SessionsFragment() {
        // Required empty public constructor
    }
    // TODO: Rename and change types and number of parameters
    public static SessionsFragment newInstance(String param1, String param2) {
        SessionsFragment fragment = new SessionsFragment();
        return fragment;
    }

    public static UploadSessionNetworkThread session_upload_thread = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        Log.e("WARN", "Creating new SessionsFragment");
        if(session_upload_thread != null) {
            session_upload_thread.register_progress_handler(this);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_sessions, container, false);

        listView = (ListView)view.findViewById(R.id.sessions_list);
        listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE);
        adapter = new SessionsAdapter(getContext(), LoggingServiceDBHelper.getInstance(getActivity().getApplicationContext()).getAllSessionData());
        listView.setAdapter(adapter);

        return view;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater)
    {
        inflater.inflate(R.menu.menu_sessions, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        switch(item.getItemId())
        {
            case R.id.action_delete:
                if(listView == null)
                    return true;
                for(long id : listView.getCheckedItemIds())
                {
                    LoggingServiceDBHelper.getInstance(getActivity().getApplicationContext()).deleteSession(id);
                }
                Toast.makeText(getContext(), getString(R.string.delete_successful), Toast.LENGTH_SHORT).show();

                adapter.changeCursor(LoggingServiceDBHelper.getInstance(getActivity().getApplicationContext()).getAllSessionData());
                return true;

            case R.id.action_export:
                if(listView == null)
                    return true;
                String data = "";
                saveToFile(listView.getCheckedItemIds());
                return true;

            case R.id.action_mv_db:
                if(checkIsServiceRunning()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
                    builder.setMessage("Please stop the running session first.")
                            .setPositiveButton(R.string.ok, null)
                            .show();
                    return true;
                }
                moveDbToServer();
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public static boolean verifyStoragePermissions(Activity activity) {
        // Check if we have write permission
        int permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    MainActivity.PERMISSION_REQUEST_STORAGE);
        }
        return permission == PackageManager.PERMISSION_GRANTED;
    }

    private void saveToFile(long[] ids) {

        //check public storage state.
        String external_storage_state = Environment.getExternalStorageState();
        if(!external_storage_state.equals(Environment.MEDIA_MOUNTED)) {
            return;
        }

        Calendar cal = Calendar.getInstance();
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyMMdd_HHmmss");

        File public_storage = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        public_storage.mkdirs();

        File file = new File(public_storage, "gollum_export_" + format.format(cal.getTime()) + ".json");
        /*if(!file.mkdirs())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.sessions_export_error)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }*/
        if(!verifyStoragePermissions(getActivity()))
            return;

        FileWriter writer;
        try {
            writer = new FileWriter(file);
        } catch (IOException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.sessions_export_error)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }

        JsonWriter json = new JsonWriter(writer);

        try {
            json.beginArray();
            for(long id : ids)
            {
                LoggingServiceDBHelper.getInstance(getActivity().getApplicationContext()).exportSessionToJSON(id, json);
            }
            json.endArray();
            writer.close();
        } catch (IOException e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.sessions_export_error)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.sessions_export_successful)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    public void onProgressUpdate() {
        return;
    }

    @Override
    public void onUploadComplete() {
        Log.e("WARN", "Hello from UI thread");

        try {
            this.session_upload_thread.join();
        } catch(Exception e) {
            Log.e("WARN",e.toString());
        }
        this.upload_running = false;
        this.session_upload_thread = null;

        adapter.changeCursor(LoggingServiceDBHelper.getInstance(getActivity().getApplicationContext()).getAllSessionData());

        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.sessions_upload_successful)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    public void onUploadError() {
        try {
            this.session_upload_thread.join();
        } catch(Exception e) {
            Log.e("WARN",e.toString());
        }
        this.upload_running = false;
        this.session_upload_thread = null;
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setMessage(R.string.sessions_upload_error)
                .setPositiveButton(R.string.ok, null)
                .show();
    }

    @Override
    public void dispatchProgressUpdate() {

    }

    @Override
    public void dispatchUploadComplete() {
        getActivity().runOnUiThread(new UploadStatusHandler(this, 3));
    }

    @Override
    public void dispatchUploadError() {
        getActivity().runOnUiThread(new UploadStatusHandler(this, 4));
    }

    public class UploadSessionNetworkThread extends Thread
    {
        IUploadProgress cb;
        HttpUploadHelper upload_helper = null;

        public UploadSessionNetworkThread()
        {
            this.upload_helper = new HttpUploadHelper(
                    LoggingServiceDBHelper.getInstance(getActivity().getApplicationContext()),
                    getResources().getString(R.string.backend_endpoint_new),
                    getResources().getString(R.string.backend_endpoint_add),
                    getResources().getString(R.string.backend_endpoint_done),
                    getResources().getString(R.string.backend_endpoint_register));
        }

        public void register_progress_handler(IUploadProgress ph)
        {
            this.cb = ph;
        }

        public void deregister_progress_handler()
        {
            this.cb = null;
        }

        @Override
        public void run()
        {
            try {

                String ul_session_id = upload_helper.start_upload_session();

                if(ul_session_id.equals("")) {
                    cb.dispatchUploadError();
                    return;
                }

                int ret = LoggingServiceDBHelper.getInstance(getActivity().getApplicationContext()
                            ).exportDbToHttp(upload_helper, ul_session_id);

                if(ret < 0) {
                    if(cb != null) {
                        cb.dispatchUploadError();
                    }
                    return;
                }

                ret = upload_helper.end_upload_session(ul_session_id);

                if(ret < 0) {
                    if(cb != null) {
                        cb.dispatchUploadError();
                    }
                    return;
                }

                Log.e("WARN", "Upload thread done.");
                //LoggingServiceDBHelper.getInstance(getActivity().getApplicationContext()).clearSessions();

                if(cb != null) {
                    cb.dispatchUploadComplete();
                }

            } catch(Exception e) {
                Log.e("WARN",e.toString());
                if(cb != null) {
                    cb.dispatchUploadError();
                }
            }

        }

    }

    private void moveDbToServer() {

        if(LoggingServiceDBHelper.getInstance(getActivity().getApplicationContext()).getNumSessions() == 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage("No sessions to move to server")
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }

        if(this.upload_running) {
            Log.e("WARN", "An upload thread is already running.");
        } else {
            session_upload_thread = new UploadSessionNetworkThread();
            session_upload_thread.register_progress_handler(this);
            this.upload_running = true;
            session_upload_thread.start();
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

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
        if(session_upload_thread != null) {
            this.session_upload_thread.deregister_progress_handler();
        }
    }

    public class SessionsAdapter extends CursorAdapter
    {

        public SessionsAdapter(Context context, Cursor c) {
            super(context, c, 0);
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            SessionListItem item = new SessionListItem(context, parent);
            return item;
        }

        @Override
        public void bindView(View view, Context context, Cursor cursor) {
            long id = cursor.getLong(cursor.getColumnIndex("_id"));
            String description = cursor.getString(cursor.getColumnIndex("description"));
            long start_timestamp = cursor.getLong(cursor.getColumnIndex("start_time"));

            //fetching this data takes too long for long sessions.
            //long duration = cursor.getLong(cursor.getColumnIndex("duration"));
            //long num_events = cursor.getLong(cursor.getColumnIndex("num_events"));

            String start_time = "";
            try{
                Date date = new Date(start_timestamp * 1000);
                java.text.DateFormat dateFormat = android.text.format.DateFormat.getDateFormat(context);
                start_time = dateFormat.format(date);
                dateFormat = android.text.format.DateFormat.getTimeFormat(context);
                start_time += " " + dateFormat.format(date);
            }
            catch(Exception ex) { }

            SessionListItem item = (SessionListItem)view;
            item.titleText.setText(description == null || description.isEmpty() ? "< " + getResources().getString(R.string.empty_session) + " >" : description);
            item.subTitleText.setText("no additional information loaded");
        }
    }

    public class SessionListItem extends LinearLayout implements Checkable
    {
        private View v;
        private TextView titleText;
        private TextView subTitleText;
        private CheckBox checkBox;

        public SessionListItem(Context context, ViewGroup parent)
        {
            super(context);
            LayoutInflater inflater = LayoutInflater.from(context);
            v = inflater.inflate(R.layout.sessions_list_item, this, true); // TODO must be (R... , parent, false)
            titleText = (TextView) v.findViewById(R.id.list_text1);
            subTitleText = (TextView) v.findViewById(R.id.list_text2);
            subTitleText.setText("");
            checkBox = (CheckBox) v.findViewById(R.id.sessions_list_checkbox);

        }
        @Override
        public void setChecked(boolean b) {
            checkBox.setChecked(b);
        }

        @Override
        public boolean isChecked() {
            return checkBox.isChecked();
        }

        @Override
        public void toggle() {
            checkBox.toggle();
        }
    }
}
