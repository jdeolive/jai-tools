package org.jaitools.media.jai.classbreaks;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

public class NaturalClassification extends Classification {

    List<Double>[] values;

    public NaturalClassification(int numBands) {
        super(Method.NATURAL_BREAKS, numBands);
        values = new List[numBands];
        for (int i = 0; i < values.length; i++) {
            values[i] = new ArrayList<Double>(); 
        }
    }

    void count(double value, int band) {
        values[band].add(value);
    }

    List<Double> getValues(int band) {
        return values[band];
    }
}
