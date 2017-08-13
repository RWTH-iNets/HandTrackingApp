package de.rwth_aachen.inets.gollum;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.util.JsonWriter;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.StringWriter;



final class LoggingServiceDBHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "LoggingService.db";
    private static final int DATABASE_VERSION = 2;

    private static LoggingServiceDBHelper sSingletonInstance = null;
    private Context context;

    public static synchronized LoggingServiceDBHelper getInstance(Context ctx)
    {
        // Use the application context, which will ensure that you
        // don't accidentally leak an Activity's context.
        // See this article for more information: https://android-developers.googleblog.com/2009/01/avoiding-memory-leaks.html
        if(sSingletonInstance == null)
        {
            sSingletonInstance = new LoggingServiceDBHelper(ctx.getApplicationContext());
        }

        return sSingletonInstance;
    }

    private LoggingServiceDBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        this.context = context;
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase)
    {
        sqLiteDatabase.execSQL("CREATE TABLE `log_entries` (" +
                "`id` INTEGER PRIMARY KEY," +
                "`session_id` INTEGER," +
                "`session_time` BIGINT," +
                "`type` INTEGER," +
                "`data_int_0` INTEGER NOT NULL DEFAULT '0'," +
                "`data_float_0` FLOAT NOT NULL DEFAULT '0'," +
                "`data_float_1` FLOAT NOT NULL DEFAULT '0'," +
                "`data_float_2` FLOAT NOT NULL DEFAULT '0'," +
                "`data_float_3` FLOAT NOT NULL DEFAULT '0'," +
                "`data_string_0` TEXT NOT NULL DEFAULT ''" +
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE `log_sessions` (" +
                "`id` INTEGER PRIMARY KEY," +
                "`description` TEXT," +
                "`start_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "`sampling_behavior` INTEGER," +
                "`sampling_interval` INTEGER" +
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE `user` (" +
                "`id` INTEGER PRIMARY KEY," +
                "`server_user_id` TEXT" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion)
    {
        // TODO: this should probably be done in a less destructive way...
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS `log_entries`");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS `log_sessions`");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS `user`");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS `sync`");
        onCreate(sqLiteDatabase);
    }

    public long insertLogSession(LoggingServiceConfiguration Configuration)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("description", Configuration.SessionName);
        values.put("sampling_behavior", Configuration.SamplingBehavior.getValue());
        values.put("sampling_interval", Configuration.SamplingInterval);
        return db.insert("log_sessions", null, values);
    }

    public void setSyncStatus(int session_id, int status)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("session_id", session_id);
        values.put("status", status);
        db.insert("sync", null, values);

        flushInsertStatement();
    }

    public void clearSessions()
    {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DROP TABLE IF EXISTS `log_entries`");
        db.execSQL("DROP TABLE IF EXISTS `log_sessions`");

        db.execSQL("CREATE TABLE `log_entries` (" +
                "`id` INTEGER PRIMARY KEY," +
                "`session_id` INTEGER," +
                "`session_time` BIGINT," +
                "`type` INTEGER," +
                "`data_int_0` INTEGER NOT NULL DEFAULT '0'," +
                "`data_float_0` FLOAT NOT NULL DEFAULT '0'," +
                "`data_float_1` FLOAT NOT NULL DEFAULT '0'," +
                "`data_float_2` FLOAT NOT NULL DEFAULT '0'," +
                "`data_float_3` FLOAT NOT NULL DEFAULT '0'," +
                "`data_string_0` TEXT NOT NULL DEFAULT ''" +
                ")");

        db.execSQL("CREATE TABLE `log_sessions` (" +
                "`id` INTEGER PRIMARY KEY," +
                "`description` TEXT," +
                "`start_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "`sampling_behavior` INTEGER," +
                "`sampling_interval` INTEGER" +
                ")");

        flushInsertStatement();
    }

    public Cursor getAllSessionData()
    {
        SQLiteDatabase db = getReadableDatabase();
        /*
        return db.rawQuery("SELECT " +
                                "log_sessions.id AS _id, " +
                                "log_sessions.description AS description, " +
                                "strftime('%s', log_sessions.start_time) AS start_time, " +
                                "(" +
                                    "SELECT " +
                                        "MAX(session_time) " +
                                    "FROM " +
                                        "log_entries " +
                                    "WHERE " +
                                        "session_id = log_sessions.id" +
                                ") AS duration, " +
                                "(" +
                                    "SELECT " +
                                        "COUNT(*) " +
                                    "FROM " +
                                        "log_entries " +
                                    "WHERE " +
                                        "session_id = log_sessions.id" +
                                ") AS num_events " +
                            "FROM " +
                                "log_sessions", null);
        */
        //speed up the query
        return db.rawQuery("SELECT " +
                "log_sessions.id AS _id, " +
                "log_sessions.description AS description, " +
                "strftime('%s', log_sessions.start_time) AS start_time " +
                "FROM " +
                "log_sessions", null);
    }

    public String getUserId()
    {
        String res = "";

        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT server_user_id FROM user", null);
        if(c.getCount() > 0) {
            c.moveToFirst();
            int col = c.getColumnIndex("server_user_id");
            res = c.getString(col);
        }
        c.close();

        return res;
    }

    public void addUserId(String server_user_id)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("server_user_id", server_user_id);
        db.insert("user", null, values);

        flushInsertStatement();
    }

    public void deleteSession(long SessionID)
    {
        SQLiteDatabase db = getWritableDatabase();

        db.delete("log_sessions", "id = ?", new String[]{String.valueOf(SessionID)});
        db.delete("log_entries", "session_id = ?", new String[]{String.valueOf(SessionID)});

        flushInsertStatement();
    }

    public int getNumSessions()
    {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM log_sessions", null);

        c.moveToFirst();
        int res = c.getInt(0);
        c.close();

        return res;
    }

    public String exportSessionToJSON(long SessionID) throws IOException
    {
        StringWriter output = new StringWriter();
        JsonWriter json = new JsonWriter(output);
        exportSessionToJSON(SessionID, json);
        json.close();

        return output.toString();
    }

    //Don't run on UI thread!
    public int exportSessionToHttp(long SessionID, HttpUploadHelper upload_helper, String ul_session_id) throws IOException
    {
        File root_folder = this.context.getFilesDir();
        File sqlite_db = new File(root_folder.getParent(), "/databases/LoggingService.db");
        Log.e("WARN", String.valueOf(sqlite_db.length()));

        StringWriter string_writer = new StringWriter();
        JsonWriter Writer = new JsonWriter(string_writer);
        SQLiteDatabase db = getReadableDatabase();
        long offset = 0;
        int ret = -1;
        String data = "";

        Writer.beginObject();

        // Session information
        Cursor cursor = db.query("log_sessions", new String[]{"description", "strftime('%s', start_time) AS start_time", "sampling_behavior", "sampling_interval"}, "id = ?", new String[]{String.valueOf(SessionID)}, null, null, null);
        if (cursor != null)
        {
            cursor.moveToFirst();
            Writer.name("id").value(SessionID);
            Writer.name("description").value(cursor.getString(cursor.getColumnIndex("description")));
            Writer.name("start_time").value(cursor.getString(cursor.getColumnIndex("start_time")));
            Writer.name("sampling_interval").value(cursor.getInt(cursor.getColumnIndex("sampling_interval")));
            switch(LoggingServiceConfiguration.SamplingBehaviors.fromInt(cursor.getInt(cursor.getColumnIndex("sampling_behavior"))))
            {
                case ALWAYS_ON:
                    Writer.name("sampling_behavior").value("ALWAYS_ON");
                    break;
                case SCREEN_ON:
                    Writer.name("sampling_behavior").value("SCREEN_ON");
                    break;
            }

            cursor.close();

            // Session events
            Writer.name("events");
            Writer.beginArray();

            Writer.flush();
            string_writer.flush();
            data = string_writer.toString();

            ret = upload_helper.send_chunk(ul_session_id, data, offset);
            if(ret < 0) {
                return ret;
            }

            offset += data.length();
            string_writer.getBuffer().setLength(0);

            cursor = db.query("log_entries", new String[]{"session_time", "type", "data_int_0", "data_float_0", "data_float_1", "data_float_2", "data_float_3", "data_string_0"}, "session_id = ?", new String[]{String.valueOf(SessionID)}, null, null, null);
            if (cursor != null)
            {
                int session_time_idx = cursor.getColumnIndex("session_time");
                int type_idx = cursor.getColumnIndex("type");
                int data_int_0_idx = cursor.getColumnIndex("data_int_0");
                int data_float_0_idx = cursor.getColumnIndex("data_float_0");
                int data_float_1_idx = cursor.getColumnIndex("data_float_1");
                int data_float_2_idx = cursor.getColumnIndex("data_float_2");
                int data_float_3_idx = cursor.getColumnIndex("data_float_3");
                int data_string_0_idx = cursor.getColumnIndex("data_string_0");

                int i = 0;
                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                {
                    Writer.beginObject();
                    Writer.name("session_time").value(cursor.getLong(session_time_idx));
                    switch (LoggingService.LogEventTypes.fromInt(cursor.getInt(type_idx)))
                    {
                        case LOG_STARTED:
                            Writer.name("type").value("LOG_STARTED");
                            break;
                        case LOG_STOPPED:
                            Writer.name("type").value("LOG_STOPPED");
                            break;
                        case ROTATION_VECTOR:
                            Writer.name("type").value("ROTATION_VECTOR");
                            Writer.name("quaternion");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.value(cursor.getFloat(data_float_3_idx));
                            Writer.endArray();
                            break;
                        case SCREEN_ON_OFF:
                            Writer.name("type").value("SCREEN_ON_OFF");
                            Writer.name("is_on").value(cursor.getInt(data_int_0_idx) == 1);
                            break;
                        case GAME_ROTATION_VECTOR:
                            Writer.name("type").value("GAME_ROTATION_VECTOR");
                            Writer.name("quaternion");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.value(cursor.getFloat(data_float_3_idx));
                            Writer.endArray();
                            break;
                        case GYROSCOPE:
                            Writer.name("type").value("GYROSCOPE");
                            Writer.name("vector");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.endArray();
                            break;
                        case ACCELEROMETER:
                            Writer.name("type").value("ACCELEROMETER");
                            Writer.name("vector");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.endArray();
                            break;
                        case MAGNETOMETER:
                            Writer.name("type").value("MAGNETOMETER");
                            Writer.name("vector");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.endArray();
                            break;
                        case PROXIMITY:
                            Writer.name("type").value("PROXIMITY");
                            Writer.name("value").value(cursor.getFloat(data_float_0_idx));
                            break;
                        case LIGHT:
                            Writer.name("type").value("LIGHT");
                            Writer.name("value").value(cursor.getFloat(data_float_0_idx));
                            break;
                        case PRESSURE:
                            Writer.name("type").value("PRESSURE");
                            Writer.name("value").value(cursor.getFloat(data_float_0_idx));
                            break;
                        case AMBIENT_TEMPERATURE:
                            Writer.name("type").value("AMBIENT_TEMPERATURE");
                            Writer.name("value").value(cursor.getFloat(data_float_0_idx));
                            break;
                        case TRAFFIC_STATS:
                            Writer.name("type").value("TRAFFIC_STATS");
                            Writer.name("mobile_rx_bytes").value(cursor.getFloat(data_float_0_idx));
                            Writer.name("mobile_tx_bytes").value(cursor.getFloat(data_float_1_idx));
                            Writer.name("wifi_rx_bytes").value(cursor.getFloat(data_float_2_idx));
                            Writer.name("wifi_tx_bytes").value(cursor.getFloat(data_float_3_idx));
                            break;
                        case FOREGROUND_APPLICATION:
                            Writer.name("type").value("FOREGROUND_APPLICATION");
                            Writer.name("package_name").value(cursor.getString(data_string_0_idx));
                            break;
                        case POWER_CONNECTED:
                            Writer.name("type").value("POWER_CONNECTED");
                            Writer.name("is_connected").value(cursor.getInt(data_int_0_idx) == 1);
                            break;
                        case DAYDREAM_ACTIVE:
                            Writer.name("type").value("DAYDREAM_ACTIVE");
                            Writer.name("is_active").value(cursor.getInt(data_int_0_idx) == 1);
                            break;
                        case PHONE_CALL:
                            Writer.name("type").value("PHONE_CALL");
                            switch(LoggingService.PhoneCallEvents.fromInt(cursor.getInt(data_int_0_idx))) {
                                case INCOMING_CALL: Writer.name("state").value("INCOMING_CALL"); break;
                                case INCOMING_CALL_ATTENDED: Writer.name("state").value("INCOMING_CALL_ATTENDED"); break;
                                case INCOMING_CALL_MISSED: Writer.name("state").value("INCOMING_CALL_MISSED"); break;
                                case OUTGOING_CALL_PLACED: Writer.name("state").value("OUTGOING_CALL_PLACED"); break;
                                case CALL_ENDED: Writer.name("state").value("CALL_ENDED"); break;
                            }

                            String phoneNumber = cursor.getString(data_string_0_idx);
                            if(!phoneNumber.isEmpty()) {
                                Writer.name("number").value(phoneNumber);
                            }
                            break;
                        case SMS_RECEIVED:
                            Writer.name("type").value("SMS_RECEIVED");
                            break;
                    }
                    Writer.endObject();
                    i++;

                    if(i>=500) {
                        Writer.flush();
                        string_writer.flush();
                        data = string_writer.toString();
                        ret = upload_helper.send_chunk(ul_session_id, data, offset);
                        if(ret < 0) {
                            return ret;
                        }
                        offset += data.length();
                        string_writer.getBuffer().setLength(0);
                        i=0;
                    }
                }

                cursor.close();
            }

            Writer.endArray();
        }

        Writer.endObject();

        Writer.flush();
        string_writer.flush();
        data = string_writer.toString();
        ret = upload_helper.send_chunk(ul_session_id, data, offset);
        string_writer.getBuffer().setLength(0);

        Writer.close();
        string_writer.close();

        if(ret < 0) {
            return ret;
        }

        return 0;
    }

    public int exportDbToHttp(HttpUploadHelper upload_helper, String ul_session_id) throws IOException
    {
        File root_folder = this.context.getFilesDir();
        File sqlite_db = new File(root_folder.getParent(), "/databases/LoggingService.db");
        Log.e("WARN", String.valueOf(sqlite_db.length()));

        byte[] buffer = new byte[1000*1000];
        FileInputStream fs = new FileInputStream(sqlite_db);
        int len;
        int ret;
        long offset = 0;
        while(true) {
            len = fs.read(buffer);

            if(len == -1) {
                break;
            }

            if(len < 4096) {
                buffer[len] = 0;
            }
            //String base64_data = Base64.encodeToString(buffer, 0, len, Base64.DEFAULT);
            //ret = upload_helper.send_chunk(ul_session_id, base64_data, offset);
            //offset += base64_data.length();
            ret = upload_helper.send_chunk(ul_session_id, buffer, len, offset);
            offset += buffer.length;
            if(ret < 0) {
                fs.close();
                return ret;
            }
        }

        return 0;
    }

    public void exportSessionToJSON(long SessionID, JsonWriter Writer) throws IOException
    {
        SQLiteDatabase db = getReadableDatabase();

        Writer.beginObject();

        // Session information
        Cursor cursor = db.query("log_sessions", new String[]{"description", "strftime('%s', start_time) AS start_time", "sampling_behavior", "sampling_interval"}, "id = ?", new String[]{String.valueOf(SessionID)}, null, null, null);
        if (cursor != null)
        {
            cursor.moveToFirst();
            Writer.name("id").value(SessionID);
            Writer.name("description").value(cursor.getString(cursor.getColumnIndex("description")));
            Writer.name("start_time").value(cursor.getString(cursor.getColumnIndex("start_time")));
            Writer.name("sampling_interval").value(cursor.getInt(cursor.getColumnIndex("sampling_interval")));
            switch(LoggingServiceConfiguration.SamplingBehaviors.fromInt(cursor.getInt(cursor.getColumnIndex("sampling_behavior"))))
            {
                case ALWAYS_ON:
                    Writer.name("sampling_behavior").value("ALWAYS_ON");
                    break;
                case SCREEN_ON:
                    Writer.name("sampling_behavior").value("SCREEN_ON");
                    break;
            }
            cursor.close();


            // Session events
            Writer.name("events");
            Writer.beginArray();

            cursor = db.query("log_entries", new String[]{"session_time", "type", "data_int_0", "data_float_0", "data_float_1", "data_float_2", "data_float_3", "data_string_0"}, "session_id = ?", new String[]{String.valueOf(SessionID)}, null, null, null);
            if (cursor != null)
            {
                int session_time_idx = cursor.getColumnIndex("session_time");
                int type_idx = cursor.getColumnIndex("type");
                int data_int_0_idx = cursor.getColumnIndex("data_int_0");
                int data_float_0_idx = cursor.getColumnIndex("data_float_0");
                int data_float_1_idx = cursor.getColumnIndex("data_float_1");
                int data_float_2_idx = cursor.getColumnIndex("data_float_2");
                int data_float_3_idx = cursor.getColumnIndex("data_float_3");
                int data_string_0_idx = cursor.getColumnIndex("data_string_0");

                for (cursor.moveToFirst(); !cursor.isAfterLast(); cursor.moveToNext())
                {
                    Writer.beginObject();
                    Writer.name("session_time").value(cursor.getLong(session_time_idx));
                    switch (LoggingService.LogEventTypes.fromInt(cursor.getInt(type_idx)))
                    {
                        case LOG_STARTED:
                            Writer.name("type").value("LOG_STARTED");
                            break;
                        case LOG_STOPPED:
                            Writer.name("type").value("LOG_STOPPED");
                            break;
                        case ROTATION_VECTOR:
                            Writer.name("type").value("ROTATION_VECTOR");
                            Writer.name("quaternion");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.value(cursor.getFloat(data_float_3_idx));
                            Writer.endArray();
                            break;
                        case SCREEN_ON_OFF:
                            Writer.name("type").value("SCREEN_ON_OFF");
                            Writer.name("is_on").value(cursor.getInt(data_int_0_idx) == 1);
                            break;
                        case GAME_ROTATION_VECTOR:
                            Writer.name("type").value("GAME_ROTATION_VECTOR");
                            Writer.name("quaternion");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.value(cursor.getFloat(data_float_3_idx));
                            Writer.endArray();
                            break;
                        case GYROSCOPE:
                            Writer.name("type").value("GYROSCOPE");
                            Writer.name("vector");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.endArray();
                            break;
                        case ACCELEROMETER:
                            Writer.name("type").value("ACCELEROMETER");
                            Writer.name("vector");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.endArray();
                            break;
                        case MAGNETOMETER:
                            Writer.name("type").value("MAGNETOMETER");
                            Writer.name("vector");
                            Writer.beginArray();
                            Writer.value(cursor.getFloat(data_float_0_idx));
                            Writer.value(cursor.getFloat(data_float_1_idx));
                            Writer.value(cursor.getFloat(data_float_2_idx));
                            Writer.endArray();
                            break;
                        case PROXIMITY:
                            Writer.name("type").value("PROXIMITY");
                            Writer.name("value").value(cursor.getFloat(data_float_0_idx));
                            break;
                        case LIGHT:
                            Writer.name("type").value("LIGHT");
                            Writer.name("value").value(cursor.getFloat(data_float_0_idx));
                            break;
                        case PRESSURE:
                            Writer.name("type").value("PRESSURE");
                            Writer.name("value").value(cursor.getFloat(data_float_0_idx));
                            break;
                        case AMBIENT_TEMPERATURE:
                            Writer.name("type").value("AMBIENT_TEMPERATURE");
                            Writer.name("value").value(cursor.getFloat(data_float_0_idx));
                            break;
                        case TRAFFIC_STATS:
                            Writer.name("type").value("TRAFFIC_STATS");
                            Writer.name("mobile_rx_bytes").value(cursor.getFloat(data_float_0_idx));
                            Writer.name("mobile_tx_bytes").value(cursor.getFloat(data_float_1_idx));
                            Writer.name("wifi_rx_bytes").value(cursor.getFloat(data_float_2_idx));
                            Writer.name("wifi_tx_bytes").value(cursor.getFloat(data_float_3_idx));
                            break;
                        case FOREGROUND_APPLICATION:
                            Writer.name("type").value("FOREGROUND_APPLICATION");
                            Writer.name("package_name").value(cursor.getString(data_string_0_idx));
                            break;
                        case POWER_CONNECTED:
                            Writer.name("type").value("POWER_CONNECTED");
                            Writer.name("is_connected").value(cursor.getInt(data_int_0_idx) == 1);
                            break;
                        case DAYDREAM_ACTIVE:
                            Writer.name("type").value("DAYDREAM_ACTIVE");
                            Writer.name("is_active").value(cursor.getInt(data_int_0_idx) == 1);
                            break;
                        case PHONE_CALL:
                            Writer.name("type").value("PHONE_CALL");
                            switch(LoggingService.PhoneCallEvents.fromInt(cursor.getInt(data_int_0_idx))) {
                                case INCOMING_CALL: Writer.name("state").value("INCOMING_CALL"); break;
                                case INCOMING_CALL_ATTENDED: Writer.name("state").value("INCOMING_CALL_ATTENDED"); break;
                                case INCOMING_CALL_MISSED: Writer.name("state").value("INCOMING_CALL_MISSED"); break;
                                case OUTGOING_CALL_PLACED: Writer.name("state").value("OUTGOING_CALL_PLACED"); break;
                                case CALL_ENDED: Writer.name("state").value("CALL_ENDED"); break;
                            }

                            String phoneNumber = cursor.getString(data_string_0_idx);
                            if(!phoneNumber.isEmpty()) {
                                Writer.name("number").value(phoneNumber);
                            }
                            break;
                        case SMS_RECEIVED:
                            Writer.name("type").value("SMS_RECEIVED");
                            break;
                    }
                    Writer.endObject();
                }

                cursor.close();
            }
            Writer.endArray();
        }

        Writer.endObject();
    }

    private SQLiteStatement mInsertStatement = null;
    private long mNumRowsInInsertStatement = 0;

    private void flushInsertStatement()
    {
        SQLiteDatabase db = getWritableDatabase();

        if(mInsertStatement != null)
        {
            db.setTransactionSuccessful();
            db.endTransaction();
        }

        db.beginTransaction();
        mInsertStatement = db.compileStatement("INSERT INTO `log_entries` (session_id, session_time, type, data_int_0, data_float_0, data_float_1, data_float_2, data_float_3, data_string_0) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
        mNumRowsInInsertStatement = 0;
    }

    private void addLogEntryToInsertStatement(long SessionID, long SessionTime, LoggingService.LogEventTypes Type, int DataInt0, float DataFloat0, float DataFloat1, float DataFloat2, float DataFloat3, String DataString0)
    {
        if(mInsertStatement == null)
        {
            flushInsertStatement();
        }

        mInsertStatement.bindLong(1, SessionID);
        mInsertStatement.bindLong(2, SessionTime);
        mInsertStatement.bindLong(3, Type.getValue());
        mInsertStatement.bindLong(4, DataInt0);
        mInsertStatement.bindDouble(5, DataFloat0);
        mInsertStatement.bindDouble(6, DataFloat1);
        mInsertStatement.bindDouble(7, DataFloat2);
        mInsertStatement.bindDouble(8, DataFloat3);
        mInsertStatement.bindString(9, DataString0);
        mInsertStatement.execute();

        ++mNumRowsInInsertStatement;
        if(Type == LoggingService.LogEventTypes.LOG_STOPPED || mNumRowsInInsertStatement > 500)
        {
            flushInsertStatement();
        }
    }

    public void insertLogEntry(long SessionID, long SessionTime, LoggingService.LogEventTypes Type)
    {
        addLogEntryToInsertStatement(SessionID, SessionTime, Type, 0, 0.0f, 0.0f, 0.0f, 0.0f, "");
    }

    public void insertLogEntry(long SessionID, long SessionTime, LoggingService.LogEventTypes Type, int Data0)
    {
        addLogEntryToInsertStatement(SessionID, SessionTime, Type, Data0, 0.0f, 0.0f, 0.0f, 0.0f, "");
    }

    public void insertLogEntry(long SessionID, long SessionTime, LoggingService.LogEventTypes Type, float Data0, float Data1, float Data2, float Data3)
    {
        addLogEntryToInsertStatement(SessionID, SessionTime, Type, 0, Data0, Data1, Data2, Data3, "");
    }

    public void insertLogEntry(long SessionID, long SessionTime, LoggingService.LogEventTypes Type, String Data0)
    {
        insertLogEntry(SessionID, SessionTime, Type, 0, Data0);
    }

    public void insertLogEntry(long SessionID, long SessionTime, LoggingService.LogEventTypes Type, int Data0, String Data1)
    {
        if(Data1 == null)
            Data1 = "";
        addLogEntryToInsertStatement(SessionID, SessionTime, Type, Data0, 0.0f, 0.0f, 0.0f, 0.0f, Data1);
    }
}
