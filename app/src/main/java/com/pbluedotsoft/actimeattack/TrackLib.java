package com.pbluedotsoft.actimeattack;

import java.util.HashMap;

/**
 * Created by daniel on 27/11/18.
 */

public class TrackLib {

    /**
     * Get track configurations for a given location.
     *
     * @param location
     *
     * @return String[]
     */
    public static String[] getTrackConfig(String location) {
        // Generate hash map
        HashMap<String, String[]> trackMap = new HashMap<>();
        trackMap.put("Barcelona", new String[]{"Barcelona - GP", "Barcelona - Moto"});
        trackMap.put("Black_cat_county", new String[]{"Black Cat County", "Black Cat " +
                "County - " +
                "Long", "Black Cat County - Short"});
        trackMap.put("Brands_hatch", new String[]{"Brands Hatch - GP", "Brands Hatch - Indy"});
        trackMap.put("Highlands", new String[]{"Highlands", "Highlands - Drift", "Highlands - " +
                "Long", "Highlands - Short"});
        trackMap.put("Imola", new String[]{"Imola"});
        trackMap.put("Laguna_seca", new String[]{"Laguna Seca"});
        trackMap.put("Magione", new String[]{"Magione"});
        trackMap.put("Monza", new String[]{"Monza", "Monza 1966 - Full", "Monza 1966 - " +
                "Junior", "Monza 1966 - Road"});
        trackMap.put("Mugello", new String[]{"Mugello"});
        trackMap.put("Nordschleife", new String[]{"Nordschleife", "Nordschleife - Endurance",
                "Nordschleife - Endurance Cup", "Nordschleife - Tourist"});
        trackMap.put("Nurburgring", new String[]{"Nurburgring - GP", "Nurburgring - GP(GT)",
                "Nurburgring - Sprint", "Nurburgring - Sprint(GT)"});
        trackMap.put("Red_bull_ring", new String[]{"Red Bull Ring - GP", "Red Bull Ring - " +
                "National"});
        trackMap.put("Silverstone", new String[]{"Silverstone - International", "Silverstone - " +
                "National", "Silverstone - 1967", "Silverstone - GP"});
        trackMap.put("Spa", new String[]{"Spa"});
        trackMap.put("Vallelunga", new String[]{"Vallelunga", "Vallelunga - Classic",
                "Vallelunga - Club"});
        trackMap.put("Zandvoort", new String[]{"Zandvoort"});

        if (trackMap.containsKey(location)) {
            return trackMap.get(location);
        }
        return new String[]{"location unknown"};
    }
}
