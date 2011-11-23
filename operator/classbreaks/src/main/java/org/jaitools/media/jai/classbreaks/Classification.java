package org.jaitools.media.jai.classbreaks;

public class Classification {

    public static enum Method {
        EQUAL_INTERVAL, QUANTILE, NATURAL_BREAKS;
    }

    /** classification method */
    Method method;

    /** the breaks */
    Double[][] breaks;
    
    /** min/max */
    Double[] min, max;

    public Classification(Method method, int numBands) {
        this.method = method;
        this.breaks = new Double[numBands][];
        this.min = new Double[numBands];
        this.max = new Double[numBands];
    }

    public Method getMethod() {
        return method;
    }

    public int getNumBands() {
        return breaks.length;
    }

    public Number[][] getBreaks() {
        return breaks;
    }

    void setBreaks(int b, Double[] breaks) {
        this. breaks[b] = breaks;
    }

    public Double getMin(int b) {
        return min[b];
    }

    void setMin(int b, Double min) {
        this.min[b] = min;
    }

    public Double getMax(int b) {
        return max[b];
    }

    void setMax(int b, Double max) {
        this.max[b] = max;
    }

    void print() {
        for (int i = 0; i < breaks.length; i++) {
            for (Double d : breaks[i]) {
                System.out.println(d);
            }
        }
    }
}
