package org.jaitools.media.jai.classifiedstats;

import java.awt.image.RenderedImage;

import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ROI;
import javax.media.jai.registry.RenderedRegistryMode;

import org.jaitools.numeric.Statistic;

public class EqualIntervalStatsDescriptor extends OperationDescriptorImpl {

    public static final String STATS_PROPERTY = "Stats";

    public static final String NAME = "EqualIntervalStats";

    static final int NUM_BINS_ARG = 0;
    static final int EXTREMA_ARG = 1;
    static final int STATS_ARG = 2;
    static final int ROI_ARG = 3;
    static final int BAND_ARG = 4;
    static final int X_PERIOD_ARG = 5;
    static final int Y_PERIOD_ARG = 6;
    static final int NODATA_ARG = 7;

    static String[] paramNames = new String[]{
        "numBins", "extrema", "stats", "roi", "band", "xPeriod", "yPeriod", "noData" 
    };

    static final Class<?>[] paramClasses = {
        Integer.class,
        Double[][].class,
        Statistic[].class,
        ROI.class,
        Integer[].class,
        Integer.class,
        Integer.class,
        Double.class
    };

    static final Object[] paramDefaults = {
        10,
        NO_PARAMETER_DEFAULT,
        new Statistic[]{Statistic.MIN, Statistic.MAX, Statistic.MEAN},
        (ROI) null,
        new Integer[]{Integer.valueOf(0)}, 
        1,
        1,
        null
    };
    
    public EqualIntervalStatsDescriptor() {
        super(
            new String[][] {
                { "GlobalName", NAME },
                { "LocalName", NAME },
                { "Vendor", "org.jaitools.media.jai" },
                { "Description", "Classifies image values using equal interval method and calculates " +
                    "statistics for each class" },
                { "DocURL", "http://code.google.com/p/jaitools/" },
                { "Version", "1.3.0" },
                {
                    "arg0Desc",
                    String.format(
                        "%s - number of classes or bins", paramNames[NUM_BINS_ARG]) },
                {
                    "arg1Desc",
                    String.format(
                        "%s - range of values to ", paramNames[EXTREMA_ARG]) },
                {
                    "arg2Desc",
                    String.format(
                        "%s - array of Statistic constants specifying the "
                        + "statistics to be calculated", paramNames[STATS_ARG]) },
                {
                    "arg3Desc",
                    String.format(
                        "%s (default %s) - region-of-interest constrainting the values to be counted",
                        paramNames[ROI_ARG], paramDefaults[ROI_ARG]) },
                {
                    "arg4Desc",
                    String.format(
                        "%s (default %s) - bands of the image to process",
                        paramNames[BAND_ARG], paramDefaults[BAND_ARG]) },
                {
                    "arg5Desc",
                    String.format(
                        "%s (default %s) - horizontal sampling rate", 
                        paramNames[X_PERIOD_ARG], paramDefaults[X_PERIOD_ARG]) },
                {
                    "arg6Desc",
                    String.format(
                        "%s (default %s) - vertical sampling rate", 
                        paramNames[Y_PERIOD_ARG], paramDefaults[Y_PERIOD_ARG]) },

                {
                    "arg7Desc",
                    String.format(
                        "%s (default %s) - value to treat as NODATA",
                        paramNames[NODATA_ARG], paramDefaults[NODATA_ARG]) },

            },

            new String[] { RenderedRegistryMode.MODE_NAME },
            new String[] {"source0"}, new Class<?>[][] {{RenderedImage.class}}, 
            paramNames, paramClasses, paramDefaults,
            null // valid values (none defined)
        );
    }

//    @Override
//    public boolean validateArguments(String modeName, ParameterBlock args, StringBuffer msg) {
//        //validate source
//        if (args.getNumSources() == 0) {
//            msg.append(NAME + " operator takes 1 source image");
//            return false;
//        }
//
//        if (!checkArg(args, NUM_BINS_ARG, Integer.class, msg)) return false;
//        if (!checkArg(args, STATS_ARG, Statistic[].class, msg)) return false;
//        if (!checkArg(args, ROI_ARG, ROI.class, msg)) return false;
//        if (!checkArg(args, BAND_ARG, Integer[].class, msg)) return false;
//        if (!checkArg(args, NUM_BINS_ARG, Integer.class, msg)) return false;
//        if (!checkArg(args, NUM_BINS_ARG, Integer.class, msg)) return false;
//        if (!checkArg(args, NUM_BINS_ARG, Integer.class, msg)) return false;
//
//        Object stats = args.getObjectParameter(STATS_ARG);
//        if (stats != null )
//        static final int  = 1;
//        static final int  = 2;
//        static final int  = 3;
//        static final int X_PERIOD_ARG = 4;
//        static final int Y_PERIOD_ARG = 5;
//        static final int NODATA_ARG = 6;
//
//        return true;
//    }
//
//    private boolean checkArg(ParameterBlock args, int arg, Class clazz, StringBuffer msg) {
//        Object obj = args.getObjectParameter(arg);
//        if (obj != null && !clazz.isInstance(obj)) {
//            msg.append(paramNames[arg] + " must be specified as").append(clazz.getName());
//            return false;
//        }
//        return true;
//    }

}
