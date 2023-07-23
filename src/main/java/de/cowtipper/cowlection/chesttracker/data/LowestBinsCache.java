package de.cowtipper.cowlection.chesttracker.data;

import java.util.HashMap;

public class LowestBinsCache extends HashMap<String, Long> {
    public boolean hasData() {
        return size() > 0;
    }
}
