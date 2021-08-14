package de.cowtipper.cowlection.chesttracker;

import java.util.HashMap;

public class LowestBinsCache extends HashMap<String, Integer> {
    public boolean hasData() {
        return size() > 0;
    }
}
