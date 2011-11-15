package org.jaitools.media.jai.classifiedstats;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ROI;

import org.jaitools.numeric.Statistic;

public class EqualIntervalStatsRIF implements RenderedImageFactory {

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        RenderedImage src = pb.getRenderedSource(0);

        int xStart = src.getMinX();     // default values
        int yStart = src.getMinY();

        /*
        static final int NUM_BINS_ARG = 0;
        static final int EXTREMA_ARG = 1;
        static final int STATS_ARG = 2;
        static final int ROI_ARG = 3;
        static final int BAND_ARG = 4;
        static final int X_PERIOD_ARG = 5;
        static final int Y_PERIOD_ARG = 6;
        static final int NODATA_ARG = 7;
        */
        Integer numBins = pb.getIntParameter(0);
        Double[][] extrema = (Double[][]) pb.getObjectParameter(1);
        Statistic[] stats = (Statistic[]) pb.getObjectParameter(2);
        ROI roi = (ROI)pb.getObjectParameter(3);
        Integer[] bands = (Integer[]) pb.getObjectParameter(4);
        Integer xPeriod = pb.getIntParameter(5);
        Integer yPeriod = pb.getIntParameter(6);
        Double noData = (Double) pb.getObjectParameter(7);

        return new EqualIntervalStatsOpImage(src, numBins, extrema, stats, roi, bands, xStart, 
            yStart, xPeriod, yPeriod, noData);
    }

}
