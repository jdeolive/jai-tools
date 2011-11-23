package org.jaitools.media.jai.classbreaks;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.ROI;

import org.jaitools.media.jai.classbreaks.Classification.Method;

import static org.jaitools.media.jai.classbreaks.ClassBreaksDescriptor.*;

public class ClassBreaksRIF implements RenderedImageFactory {

    public RenderedImage create(ParameterBlock pb, RenderingHints hints) {
        RenderedImage src = pb.getRenderedSource(0);

        int xStart = src.getMinX();     // default values
        int yStart = src.getMinY();

        Integer numBins = pb.getIntParameter(NUM_CLASSES_ARG);
        Method method = (Method) pb.getObjectParameter(METHOD_ARG);
        Double[][] extrema = (Double[][]) pb.getObjectParameter(EXTREMA_ARG);
        ROI roi = (ROI)pb.getObjectParameter(ROI_ARG);
        Integer[] bands = (Integer[]) pb.getObjectParameter(BAND_ARG);
        Integer xPeriod = pb.getIntParameter(X_PERIOD_ARG);
        Integer yPeriod = pb.getIntParameter(Y_PERIOD_ARG);
        Double noData = (Double) pb.getObjectParameter(NODATA_ARG);

        switch(method) {
            case EQUAL_INTERVAL:
                return new EqualIntervalBreaksOpImage(src, numBins, extrema, roi, bands, xStart, 
                    yStart, xPeriod, yPeriod, noData);
            case QUANTILE:
                return new QuantileBreaksOpImage(src, numBins, extrema, roi, bands, xStart, yStart, 
                    xPeriod, yPeriod, noData);
            case NATURAL_BREAKS:
                return new NaturalBreaksOpImage(src, numBins, extrema, roi, bands, xStart, yStart, 
                    xPeriod, yPeriod, noData);
            default:
                throw new IllegalArgumentException(method.name());
        }
        
    }

}
