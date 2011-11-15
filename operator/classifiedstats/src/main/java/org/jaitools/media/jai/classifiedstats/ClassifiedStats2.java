package org.jaitools.media.jai.classifiedstats;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.jaitools.numeric.StreamingSampleStats;

public class ClassifiedStats2 {

    Statistic[] stats;
    List<Bin>[] bins;

    public ClassifiedStats2(int numBands, Statistic[] stats) {
        this.stats = stats;
        bins = new List[numBands];
        for (int i = 0; i < numBands; i++) {
            bins[i] = new ArrayList<Bin>();
        }
    }

    public int getNumBands() {
        return bins.length;
    }

    public int getNumBins(int band) {
        return bins[0].size();
    }

    public Range<Double> range(int band, int bin) {
        return bin(band,bin).range;
    }

    public StreamingSampleStats stats(int band, int bin) {
        return bin(band,bin).stats;
    }

    public Double stat(int band, int bin, Statistic stat) {
        return bin(band, bin).stats.getStatisticValue(stat);
    }

    Bin bin(int band, int index) {
        return bins[band].get(index);
    }

    void newBin(int band, Range<Double> range) {
        bins[band].add(new Bin(range));
    }

    void offer(int band, int bin, Double value) {
        bin(band, bin).stats.offer(value);
    }

    class Bin {
        Range<Double> range;
        StreamingSampleStats stats;

        Bin(Range<Double> range) {
            this.range = range;
            this.stats = new StreamingSampleStats(Range.Type.INCLUDE);
            this.stats.addRange(range);
            this.stats.setStatistics(ClassifiedStats2.this.stats);
        }
    }

}
