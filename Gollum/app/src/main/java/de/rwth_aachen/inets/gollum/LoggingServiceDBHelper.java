package de.rwth_aachen.inets.gollum;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;


final class LoggingServiceDBHelper extends SQLiteOpenHelper
{
    public static final String DATABASE_NAME = "LoggingService.db";
    public static final int DATABASE_VERSION = 1;

    public LoggingServiceDBHelper(Context context)
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
                "`start_time` TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
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
        return (int)db.insert("log_sessions", null, values);
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
