package com.pbluedotsoft.actimeattack;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.pbluedotsoft.actimeattack.data.LapContract.LapEntry;


/**
 * Created by daniel on 26/11/18.
 */

public class TrackCursorAdapter extends CursorAdapter {

    private static final String TAG = TrackCursorAdapter.class.getSimpleName();

    private static String mActualCar;
    private static int mLapFastestCar;

    public TrackCursorAdapter(Context context, Cursor c) {
        super(context, c, 0);
    }

    /**
     * Makes a new blank list item view. No data is set (or bound) to the views yet.
     *
     */
    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        return LayoutInflater.from(context).inflate(R.layout.one_track_list_item, parent, false);
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

        TextView carTv = view.findViewById(R.id.car_tv);
        TextView timeTv = view.findViewById(R.id.laptime_tv);
        TextView gapTv = view.findViewById(R.id.gap_tv);
        TextView nlapsTv = view.findViewById(R.id.nlaps_tv);
        TextView speedTv = view.findViewById(R.id.top_speed_tv);

        // Find the columns we are interested in
        int carColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_CAR);
        int timeColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TIME);
        int lapsColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_NLAPS);
        int speedColIndex = cursor.getColumnIndex(LapEntry.COLUMN_LAP_TOP_SPEED);

        // Read laptime attributes from the cursor
        String car = cursor.getString(carColIndex);
        int time = cursor.getInt(timeColIndex);
        int laps = cursor.getInt(lapsColIndex);
        int speed = cursor.getInt(speedColIndex);

        // Update TextViews
        carTv.setText(car);
        timeTv.setText(Lap.format(time));
        nlapsTv.setText(String.valueOf(laps));
        speedTv.setText(String.valueOf(speed));
        if (mLapFastestCar > 0) {
            String gap = (mLapFastestCar == time) ? "-"  : Lap.getGapStr(time, mLapFastestCar);
            gapTv.setText(gap);
        }

        // Highlight actual car
        if (car.equals(mActualCar)) {
            carTv.setTextColor(ContextCompat.getColor(context, R.color.record));
        } else {
            carTv.setTextColor(ContextCompat.getColor(context, R.color.whiteText));
        }
    }


    /**
     * Set actual car
     *
     */
    public static void setActualCar(String car) {
        mActualCar = car;
    }


    /**
     * Set laptime for fastest car on this track
     *
     */
    public static void setLapFastestCar(int laptime) {
        mLapFastestCar = laptime;
    }


    /**
     * Get laptime for fastest car on this track
     */
    public static int getLapFastestCar() {
        return mLapFastestCar;
    }
}
