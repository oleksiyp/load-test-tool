package org.loadtest;

import java.util.*;

/**
 * Class accumulates statistics: number of runs, errors, queries and http fetch execution time.
 */
@SuppressWarnings({"WeakerAccess"})
public class Stats {
    public Stats() {
        reset();
    }

    public synchronized void reset() {
        runs = 0;
        samples = 0;
        errors = 0;
        sampleStats = new TreeMap<String, SampleStat>();
    }

    private int runs;
    private int samples;
    private int errors;
    private Map<String, SampleStat> sampleStats;

    public int getRuns() {
        return runs;
    }

    private static class SampleStat {
        private int count;
        private long time;
        private long minimum;
        private long maximum;
        private final String name;

        private SampleStat(String name) {
            this.name = name;
        }


        public void add(long time) {
            if (count == 0 || minimum > time) {
                minimum = time;
            }
            if (count == 0 || maximum < time) {
                maximum = time;
            }
            this.time += time;
            this.count++;
        }

        public double getAverage() {
            if (count == 0) {
                return 0;
            } else {
                return (double)time / count;
            }
        }

        public long getMinimum() {
            return minimum;
        }

        public long getMaximum() {
            return maximum;
        }

        public String getName() {
            return name;
        }
    }

    public synchronized void addRun() { runs++; }
    public synchronized void addSample(String url, long time) {
        samples++;
        SampleStat stat = sampleStats.get(url);
        if (stat == null) {
            stat = new SampleStat(url);
            sampleStats.put(url, stat);
        }
        stat.add(time);
    }
    @SuppressWarnings({"UnusedParameters"})
    public synchronized void addError(Throwable thr) { errors++; }

    public synchronized void report(int slowSamplesToShow) {
        System.out.println("Stats{" +
                "runs=" + runs +
                ", samples=" + samples +
                ", errors=" + errors +
                '}');
        List<SampleStat> list = new ArrayList<SampleStat>(sampleStats.values());

        // sort & cut states
        Collections.sort(list, new SlowestSampleStatComparator());
        list = list.subList(0, Math.min(slowSamplesToShow, list.size()));

        for (SampleStat sampleStat : list) {

            // cut string
            String statName = sampleStat.getName();
            statName = statName.substring(0, Math.min(45, statName.length()));

            System.out.printf("min=%5d avg=%8.2f max=%5d %s%n",
                    sampleStat.getMinimum(),
                    sampleStat.getAverage(),
                    sampleStat.getMaximum(),
                    statName);

        }
    }

    private class SlowestSampleStatComparator implements Comparator<SampleStat> {
        public int compare(SampleStat o1, SampleStat o2) {
            return -Double.compare(o1.getAverage(), o2.getAverage());
        }
    }
}
