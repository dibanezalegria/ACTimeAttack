package com.pbluedotsoft.actimeattack;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.pbluedotsoft.actimeattack.data.LapContract.LapEntry;


/**
 * Created by daniel on 13/03/18.
 */

public class DBCursorAdapter extends CursorAdapter {

    private static final String TAG = DBCursorAdapter.class.getSimpleName();

    public DBCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.db_list_item, parent, false);
    }

    /**
     * This method binds the laptime data (in the current row pointed to by cursor) to the given
     * list item layout. For example, the name for the current laptime can be set on the name
     * TextView in the list item layout.
     *
     */
    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        // Alternate background color
        int position = cursor.getPosition();
        int rowBgColor = (position % 2 == 0) ? R.color.listItemBgDark : R.color.listItemBgLight;
        view.setBackgroundColor(ContextCompat.getColor(view.getContext(), rowBgColor));

        TextView idTv = view.findViewById(R.id.list_item_id);
        TextView trackTv = view.findViewById(R.id.list_item_track);
        TextView carTv = view.findViewById(R.id.list_item_car);
        TextView nlapsTv = view.findViewById(R.id.list_item_laps);
        TextView speedTv = view.findViewById(R.id.list_item_speed);
        TextView timeTv = view.findViewById(R.id.list_item_lap_time);

        // Find the columns we are interested in
        int idColIndex = cursor.getColumnIndex(LapEntry._ID);
        int trackColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TRACK);
        int carColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_CAR);
        int nlapsColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_NLAPS);
        int speedColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TOP_SPEED);
        int timeColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME);

        // Read laptime attributes from the cursor
        int id = cursor.getInt(idColIndex);
        String track = cursor.getString(trackColIndex);
        String car = cursor.getString(carColIndex);
        int nlaps = cursor.getInt(nlapsColIndex);
        int speed = cursor.getInt(speedColIndex);
        int time = cursor.getInt(timeColIndex);

        // Update TextViews
        idTv.setText(String.valueOf(id));
        trackTv.setText(track);
        carTv.setText(car);
        nlapsTv.setText(String.valueOf(nlaps));
        speedTv.setText(String.valueOf(speed));
        timeTv.setText(Lap.format(time));

        // Delete button
        Button btn = view.findViewById(R.id.delete_btn);
        final Context fcontext = context;
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                TextView idTV = ((LinearLayout)view.getParent()).findViewById(R.id.list_item_id);
//                Log.d(TAG, "view: " + idTV.getText().toString());
                String selection = LapEntry._ID + "=?";
                String[] selArgs = { idTV.getText().toString() };
                fcontext.getContentResolver().delete(LapEntry.CONTENT_URI,
                        selection, selArgs);
            }
        });
    }

}
