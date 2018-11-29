package com.pbluedotsoft.actimeattack;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

/**
 * Created by daniel on 26/11/18.
 */

public class SessionLapAdapter extends ArrayAdapter<Lap> {

    private int mBestLap;
    private int mRecordLap;

    public SessionLapAdapter(Context context, Lap[] laptime) {
        super(context, 0, laptime);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Lap aLap = getItem(position);
        if (convertView == null) {
            convertView = (LayoutInflater.from(getContext()))
                    .inflate(R.layout.session_list_item, parent, false);
        }

        // Alternate row background color
        int rowBgColor = (position % 2 == 0) ? R.color.listItemBgDark : R.color.listItemBgLight;
        convertView.setBackgroundColor(ContextCompat.getColor(convertView.getContext(),
                rowBgColor));

        TextView tvLapNumber = convertView.findViewById(R.id.lapnumber_tv);
        TextView tvLaptime = convertView.findViewById(R.id.laptime_tv);
        TextView tvGap = convertView.findViewById(R.id.gap_tv);

        if (aLap != null) {
            if (aLap.getTime() == mRecordLap) {
                tvLaptime.setTextColor(ContextCompat.getColor(getContext(), R.color.record));
            } else if (aLap.getTime() == mBestLap) {
                tvLaptime.setTextColor(ContextCompat.getColor(getContext(), R.color.bestTime));
            } else {
                tvLaptime.setTextColor(ContextCompat.getColor(getContext(), R.color.whiteText));
            }
            if (position == 0) {
                tvLapNumber.setTextColor(ContextCompat.getColor(getContext(), R.color.lastlap));
            } else {
                tvLapNumber.setTextColor(ContextCompat.getColor(getContext(), R.color.whiteText));
            }
            tvLapNumber.setText(String.format("%s", aLap.getLapNum()));
            tvLaptime.setText(PacketParser.format(aLap.getTime()));
            tvGap.setText(Lap.getGapStr(aLap.getTime(), mBestLap));
        }

        return convertView;
    }

    /**
     * Set best lap so we can calculate gaps for every lap
     *
     * @param time
     */
    public void setBestLap(int time) {
        mBestLap = time;
    }
}
