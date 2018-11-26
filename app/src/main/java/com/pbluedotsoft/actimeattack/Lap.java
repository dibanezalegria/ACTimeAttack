package com.pbluedotsoft.actimeattack;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Created by daniel on 26/11/18.
 */

public class Lap {
    private int mLapNum;
    private int mTime;

    public Lap(int num, int time) {
        mLapNum = num;
        mTime = time;
    }

    /**
     * Formats given time to string.
     *
     * @return string '--:--.---'
     */
    public static String format(int time) {
        if (time <= 0 || time == Integer.MAX_VALUE) {
            return "--:--.---";
        }

        int minutes = time / 1000 < 60 ? 0 : (time / 1000) / 60;
        int seconds = time / 1000 < 60 ? (time / 1000) : (time / 1000) % 60;
        int msec = time % 1000;
        return String.format(Locale.ENGLISH, "%02d", minutes) +
                ":" + String.format(Locale.ENGLISH,"%02d", seconds) + "." +
                String.format(Locale.ENGLISH,"%03d", msec);
    }

    public int getLapNum() {
        return mLapNum;
    }

    public int getTime() {
        return mTime;
    }

    /**
     * Calculates difference between slow time and fast.
     *
     * @return string with format '+00:00.000'
     */
    public static String getGapStr(int slow, int fast) {
        int gap = slow - fast;

        // Plus or minus
        String sign = "+";
        if (gap < 0)
            sign = "-";

        int min = gap / 1000 < 60 ? 0 : (gap / 1000) / 60;
        int sec = gap / 1000 < 60 ? (gap / 1000) : (gap / 1000) % 60;
        int ms = gap % 1000;

        String minStr = (min == 0) ? "" : String.format(Locale.ENGLISH, "%s:", min);
        String secStr = (sec == 0 && min == 0) ? "0." : String.format(Locale.ENGLISH, "%02d.",
                sec);
        String msStr = String.format(Locale.ENGLISH, "%03d", ms);

        return String.format(Locale.ENGLISH, "%s%s%s%s", sign, minStr, secStr, msStr);
    }
}
