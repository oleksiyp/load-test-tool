package org.loadtest;

import javax.swing.event.ListSelectionEvent;
import java.util.*;

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

    public synchronized List list(String name, Object ...defaultValues) {
        List list = (List) values.get(name);
        if (list == null) {
            list = Collections.synchronizedList(new ArrayList());
            list.addAll(Arrays.asList(defaultValues));
            values.put(name, list);
        }
        return list;
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
