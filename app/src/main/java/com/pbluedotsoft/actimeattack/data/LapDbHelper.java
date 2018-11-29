package com.pbluedotsoft.actimeattack.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.pbluedotsoft.actimeattack.data.LapContract.LapEntry;

/**
 * Created by daniel on 26/11/18.
 */

public class LapDbHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "actimeattack.db";
    private static final int DATABASE_VERSION = 1;

    /**
     * Constructs a new instance of {@link LapDbHelper}.
     *
     * @param context of the app
     */
    public LapDbHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    /**
     * This is called when the database is created for the first time.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // SQL statement
        String SQL_CREATE_LAPS_TABLE = "CREATE TABLE " + LapEntry.TABLE_NAME + " ("
                + LapEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + LapEntry.COLUMN_LAP_TRACK + " TEXT, "
                + LapEntry.COLUMN_LAP_CAR + " TEXT, "
                + LapEntry.COLUMN_LAP_NLAPS + " INTEGER DEFAULT 1, "
                + LapEntry.COLUMN_LAP_TOP_SPEED + " INTEGER DEFAULT 0, "
                + LapEntry.COLUMN_LAP_TIME + " INTEGER DEFAULT 0);";

        // Execute the SQL statement
        db.execSQL(SQL_CREATE_LAPS_TABLE);
    }

    /**
     * This is called when the database needs to be upgraded.
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
