package com.pbluedotsoft.actimeattack;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;

/**
 * Created by daniel on 17/09/18.
 */

public class PacketParser {
    private static final String LOG = PacketParser.class.getSimpleName();

    char identifier;    // set to 'a', it is used to verify if right packet
    int size;
    float speed;
    byte isABS;
    int lapTime;
    int lastLap;
    int bestLap;
    int lapCount;

    // Parser for struct RTLap (SUBSCRIBE_SPOT)
//    int cardIdentifierNumber;
//    int lap;
//    String driverName;    // 50 char * 2 bytes/char = 100 bytes (UTF-16LE)
//    String carName;       // 50
//    int time;

    public void parse(byte[] packet) {
        identifier = ByteBuffer.wrap(packet, 0, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getChar();
        size = ByteBuffer.wrap(packet, 4, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getInt();
        speed = ByteBuffer.wrap(packet, 8, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getFloat();
        isABS = packet[20];
        lapTime = ByteBuffer.wrap(packet, 40, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getInt();
        lastLap = ByteBuffer.wrap(packet, 44, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getInt();
        bestLap = ByteBuffer.wrap(packet, 48, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getInt();
        lapCount = ByteBuffer.wrap(packet, 52, 4)
                .order(ByteOrder.LITTLE_ENDIAN).getInt();

        // Parser for struct RTLap (SUBSCRIBE_SPOT)
//        try {
//            cardIdentifierNumber = ByteBuffer.wrap(packet, 0, 4)
//                    .order(ByteOrder.LITTLE_ENDIAN).getInt();
//            lap = ByteBuffer.wrap(packet, 4, 4)
//                    .order(ByteOrder.LITTLE_ENDIAN).getInt();
//            driverName = new String(Arrays.copyOfRange(packet, 8, 108), "UTF-16LE");
//            int pos = driverName.indexOf('%');
//            if (pos > 0) {
//                driverName = driverName.substring(0, pos);
//            }
//            carName = new String(Arrays.copyOfRange(packet, 108, 208), "UTF-16LE");
//            pos = carName.indexOf('%');
//            if (pos > 0) {
//                carName = carName.substring(0, pos);
//            }
//            time = ByteBuffer.wrap(packet, 208, 4)
//                    .order(ByteOrder.LITTLE_ENDIAN).getInt();
//
//        } catch(UnsupportedEncodingException ex) {
//            ex.printStackTrace();
//        }
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

    @Override
    public String toString() {
        return "id: " + identifier +
                " size: " + size +
                " ABS: " + isABS +
                " lapTime: " + lapTime +
                " lastLap: " + lastLap +
                " bestLap: " + bestLap +
                " lapCount: " + lapCount;

        // Parser for struct RTLap (SUBSCRIBE_SPOT)
//        return "id: " + cardIdentifierNumber +
//                " lap: " + lap +
//                " car: " + carName +
//                " driver: " + driverName +
//                " time: " + time;
    }

    public String toByteStr(byte[] bytes) {
        String str = "";
        for (short b : bytes) {
            str += String.format("%02x ", b & 0xFF);
        }
        return str;
    }
}

