package de.rwth_aachen.inets.gollum;

import android.Manifest;
import android.app.Activity;
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
import java.io.OutputStreamWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


public class SessionsFragment extends Fragment {

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
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
        Calendar cal = Calendar.getInstance();
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyMMdd_HHmmss");
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "gollum_export_" + format.format(cal.getTime()));
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

    private void saveDataToFile(String data)
    {
        Calendar cal = Calendar.getInstance();
        java.text.SimpleDateFormat format = new java.text.SimpleDateFormat("yyMMdd_HHmmss");
        File file = new File(Environment.DIRECTORY_DOWNLOADS, "gollum_export_" + format.format(cal.getTime()));
        if(!file.mkdirs())
        {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setMessage(R.string.sessions_export_error)
                    .setPositiveButton(R.string.ok, null)
                    .show();
            return;
        }
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

        try {
            writer.write(data);
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
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    @Override
    public void onDetach() {
        super.onDetach();
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
            long duration = cursor.getLong(cursor.getColumnIndex("duration"));
            long num_events = cursor.getLong(cursor.getColumnIndex("num_events"));

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
            item.subTitleText.setText(start_time + " (" + String.valueOf(duration / 1000000000) + " " + getResources().getString(R.string.sec) + ")");
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
