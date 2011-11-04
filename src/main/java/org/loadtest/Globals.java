package org.loadtest;

import java.util.HashMap;
import java.util.Map;

/**
 * Object used by scripts to interchange values
 */
public class Globals {
    private Map values = new HashMap();

    public synchronized void put(String name, Object value) {
        values.put(name, value);
    }

    public synchronized Object get(String name) {
        return values.get(name);
    }
    public synchronized long increment(String name) {
        Number num = (Number)values.get(name);
        if (num == null) {
            num = 0L;
        }
        long res = num.longValue();
        values.put(name, res + 1);
        return res;
    }
}
