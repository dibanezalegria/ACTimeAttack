package com.pbluedotsoft.actimeattack;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * Created by daniel on 16/09/18.
 */

public class HShakeParser {
    public String carName;  //50
    public String driverName; //50
    int identifier;
    int version;
    public String trackName; //50;
    public String trackConfig; //50

    public void parse(byte[] packet) {
        try {
            // Car name
            carName = new String(Arrays.copyOfRange(packet, 0, 100), "UTF-16LE");
            int pos = carName.indexOf('%');
            if (pos > 0) {
                carName = carName.substring(0, pos);
            }
            carName = carName.replace("ks_", "");
            carName = carName.substring(0, 1).toUpperCase() + carName.substring(1);

            // Driver name
            driverName = new String(Arrays.copyOfRange(packet, 100, 200), "UTF-16LE");
            pos = driverName.indexOf('%');
            if (pos > 0) {
                driverName = driverName.substring(0, pos);
            }

            // Identifier
            identifier = ByteBuffer.wrap(packet, 200, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

            // Version
            version = ByteBuffer.wrap(packet, 204, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();

            // Track name
            trackName = new String(Arrays.copyOfRange(packet, 208, 308), "UTF-16LE");
            pos = trackName.indexOf('%');
            if (pos > 0) {
                trackName = trackName.substring(0, pos);
            }
            trackName = trackName.replace("ks_", "");
            trackName = trackName.substring(0, 1).toUpperCase() + trackName.substring(1);

            // Track config
            trackConfig = new String(Arrays.copyOfRange(packet, 308, 408), "UTF-16LE");
            pos = trackConfig.indexOf('%');
            if (pos > 0) {
                trackConfig = trackConfig.substring(0, pos);
            }

        } catch(UnsupportedEncodingException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public String toString() {
        return carName + " " + driverName + " " + trackName + " " + trackConfig;
    }
}
