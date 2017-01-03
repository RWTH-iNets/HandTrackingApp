package de.rwth_aachen.inets.gollum;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.JsonWriter;

import java.io.IOException;
import java.io.StringWriter;


final class LoggingServiceDBHelper extends SQLiteOpenHelper
{
    private static final String DATABASE_NAME = "LoggingService.db";
    private static final int DATABASE_VERSION = 1;

    private static LoggingServiceDBHelper sSingletonInstance = null;

    public static LoggingServiceDBHelper getInstance(Context ctx)
    {
        if(sSingletonInstance == null)
        {
            sSingletonInstance = new LoggingServiceDBHelper(ctx);
        }

        return sSingletonInstance;
    }

    private LoggingServiceDBHelper(Context context)
    {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
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
                "`data_float_3` FLOAT NOT NULL DEFAULT '0'" +
                ")");

        sqLiteDatabase.execSQL("CREATE TABLE `log_sessions` (" +
                "`id` INTEGER PRIMARY KEY," +
                "`description` TEXT," +
                "`start_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "`sampling_behavior` INTEGER," +
                "`sampling_interval` INTEGER" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion)
    {
        // TODO: this should probably be done in a less destructive way...
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS `log_entries`");
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS `log_sessions`");
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

    public Cursor getAllSessionData()
    {
        SQLiteDatabase db = getReadableDatabase();

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
    }

    public void deleteSession(long SessionID)
    {
        SQLiteDatabase db = getWritableDatabase();

        db.delete("log_sessions", "id = ?", new String[]{String.valueOf(SessionID)});
        db.delete("log_entries", "session_id = ?", new String[]{String.valueOf(SessionID)});
    }

    public String exportSessionToJSON(long SessionID) throws IOException
    {
        StringWriter output = new StringWriter();
        JsonWriter json = new JsonWriter(output);
        exportSessionToJSON(SessionID, json);
        json.close();

        return output.toString();
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

            cursor = db.query("log_entries", new String[]{"session_time", "type", "data_int_0", "data_float_0", "data_float_1", "data_float_2", "data_float_3"}, "session_id = ?", new String[]{String.valueOf(SessionID)}, null, null, null);
            if (cursor != null)
            {
                int session_time_idx = cursor.getColumnIndex("session_time");
                int type_idx = cursor.getColumnIndex("type");
                int data_int_0_idx = cursor.getColumnIndex("data_int_0");
                int data_float_0_idx = cursor.getColumnIndex("data_float_0");
                int data_float_1_idx = cursor.getColumnIndex("data_float_1");
                int data_float_2_idx = cursor.getColumnIndex("data_float_2");
                int data_float_3_idx = cursor.getColumnIndex("data_float_3");

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
                    }
                    Writer.endObject();
                }

                cursor.close();
            }
            Writer.endArray();
        }

        Writer.endObject();
    }

    public void insertLogEntry(long SessionID, long SessionTime, LoggingService.LogEventTypes Type)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("session_id", SessionID);
        values.put("session_time", SessionTime);
        values.put("type", Type.getValue());
        db.insert("log_entries", null, values);
    }

    public void insertLogEntry(long SessionID, long SessionTime, LoggingService.LogEventTypes Type, int Data0)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("session_id", SessionID);
        values.put("session_time", SessionTime);
        values.put("type", Type.getValue());
        values.put("data_int_0", Data0);
        db.insert("log_entries", null, values);
    }

    public void insertLogEntry(long SessionID, long SessionTime, LoggingService.LogEventTypes Type, float Data0, float Data1, float Data2, float Data3)
    {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("session_id", SessionID);
        values.put("session_time", SessionTime);
        values.put("type", Type.getValue());
        values.put("data_float_0", Data0);
        values.put("data_float_1", Data1);
        values.put("data_float_2", Data2);
        values.put("data_float_3", Data3);
        db.insert("log_entries", null, values);
    }
}
