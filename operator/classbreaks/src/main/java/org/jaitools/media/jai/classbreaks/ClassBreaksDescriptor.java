package org.jaitools.media.jai.classbreaks;

import java.awt.image.RenderedImage;

import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.ROI;
import javax.media.jai.registry.RenderedRegistryMode;

import org.jaitools.media.jai.classbreaks.Classification.Method;

public class ClassBreaksDescriptor extends OperationDescriptorImpl {

    public static final String CLASSIFICATION_PROPERTY = "Classification";

    public static final String NAME = "ClassBreaks";

    static final int NUM_CLASSES_ARG = 0;
    static final int METHOD_ARG = 1;
    static final int EXTREMA_ARG = 2;
    static final int ROI_ARG = 3;
    static final int BAND_ARG = 4;
    static final int X_PERIOD_ARG = 5;
    static final int Y_PERIOD_ARG = 6;
    static final int NODATA_ARG = 7;

    static String[] paramNames = new String[]{
        "numClasses", "method", "extrema", "roi", "band", "xPeriod", "yPeriod", "noData" 
    };

    static final Class<?>[] paramClasses = {
        Integer.class,
        Method.class,
        Double[][].class,
        ROI.class,
        Integer[].class,
        Integer.class,
        Integer.class,
        Double.class
    };

    static final Object[] paramDefaults = {
        10,
        Method.EQUAL_INTERVAL,
        null,
        (ROI) null,
        new Integer[]{Integer.valueOf(0)}, 
        1,
        1,
        null
    };
    
    public ClassBreaksDescriptor() {
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
                    String.format("arg%dDesc", NUM_CLASSES_ARG),
                    String.format(
                        "%s - number of classes or bins", paramNames[NUM_CLASSES_ARG]) },
                {
                    String.format("arg%dDesc", METHOD_ARG),
                    String.format(
                        "%s - classification method", paramNames[METHOD_ARG]) },
                {
                    String.format("arg%dDesc", EXTREMA_ARG),
                    String.format(
                        "%s - range of values to include", paramNames[EXTREMA_ARG]) },
                {
                    String.format("arg%dDesc", ROI_ARG),
                    String.format(
                        "%s (default %s) - region-of-interest constrainting the values to be counted",
                        paramNames[ROI_ARG], paramDefaults[ROI_ARG]) },
                {
                    String.format("arg%dDesc", BAND_ARG),
                    String.format(
                        "%s (default %s) - bands of the image to process",
                        paramNames[BAND_ARG], paramDefaults[BAND_ARG]) },
                {
                    String.format("arg%dDesc", X_PERIOD_ARG),
                    String.format(
                        "%s (default %s) - horizontal sampling rate", 
                        paramNames[X_PERIOD_ARG], paramDefaults[X_PERIOD_ARG]) },
                {
                    String.format("arg%dDesc", Y_PERIOD_ARG),
                    String.format(
                        "%s (default %s) - vertical sampling rate", 
                        paramNames[Y_PERIOD_ARG], paramDefaults[Y_PERIOD_ARG]) },

                {
                    String.format("arg%dDesc", NODATA_ARG),
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
