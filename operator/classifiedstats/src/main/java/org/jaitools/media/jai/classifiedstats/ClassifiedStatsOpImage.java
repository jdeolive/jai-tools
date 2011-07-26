/* 
 *  Copyright (c) 2009-2011, Daniele Romagnoli. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package org.jaitools.media.jai.classifiedstats;

import java.awt.image.RenderedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.media.jai.AreaOpImage;
import javax.media.jai.ImageLayout;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.ROI;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.CollectionFactory;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.RangeUtils;
import org.jaitools.numeric.Statistic;
import org.jaitools.numeric.StreamingSampleStats;


/**
 * Calculates image summary statistics for a data image within zones defined by
 * a integral valued zone image. If a zone image is not provided all data image
 * pixels are treated as being in the same zone (zone 0).
 *
 * @see ClassifiedStatsDescriptor Description of the algorithm and example
 *
 * @author Daniele Romagnoli, GeoSolutions S.A.S.
 * @since 1.2
 */
public class ClassifiedStatsOpImage extends NullOpImage {

    private final Integer[] srcBands;

    private final ROI roi;

    private final Statistic[] stats;

    private final RenderedImage dataImage;
//    private final Rectangle dataImageBounds;
    private final RenderedImage[] classifiedImages;

    /** Optional ranges to exclude/include values from/in statistics computations */
    private final List<Range<Double>> ranges;

    /** Optional ranges to specify which values should be considered as NoData and then excluded from computations */
    private final List<Range<Double>> noDataRanges;

    /** Compute separated statistics on ranges if true */
    private final boolean rangeLocalStats;

    /** Define whether provided ranges of values need to be included or excluded
     * from statistics computations */
    private Range.Type rangesType;

    /**
     * Constructor.
     *
     * @param dataImage a {@code RenderedImage} from which data values will be read.
     *
     * @param classifiedImages a {@code RenderedImage}'s array of integral data type that defines
     *        the classification for which to calculate summary data.
     *
     * @param config configurable attributes of the image (see {@link AreaOpImage}).
     *
     * @param layout an optional {@code ImageLayout} object.
     *
     * @param stats an array of {@code Statistic} constants specifying the data required.
     *
     * @param bands the data image band to process.
     *
     * @param roi an optional {@code ROI} for data image masking.
     *
     * @param ranges an optional list of {@link Range} objects defining values to include or
     *        exclude (depending on {@code rangesType} from the calculations; may be
     *        {@code null} or empty
     * 
     * @param rangesType specifies whether the {@code ranges} argument defines values
     *        to include or exclude
     *
     * @param rangeLocalStats if {@code true}, the statistics should be computed for ranges,
     *        separately.
     *
     * @param noDataRanges an optional list of {@link Range} objects defining values to
     *        treat as NODATA
     * 
     * @see ClassifiedStatsDescriptor
     * @see Statistic
     */
    public ClassifiedStatsOpImage(RenderedImage dataImage, RenderedImage[] classifiedImages,
            Map<?, ?> config,
            ImageLayout layout,
            Statistic[] stats,
            Integer[] bands,
            ROI roi,
            Collection<Range<Double>> ranges,
            Range.Type rangesType,
            final boolean rangeLocalStats,
            Collection<Range<Double>> noDataRanges) {

        super(dataImage, layout, config, OpImage.OP_COMPUTE_BOUND);

        this.dataImage = dataImage;
        this.classifiedImages = classifiedImages;

//        dataImageBounds = new Rectangle(
//                dataImage.getMinX(), dataImage.getMinY(),
//                dataImage.getWidth(), dataImage.getHeight());

        this.stats = new Statistic[stats.length];
        System.arraycopy(stats, 0, this.stats, 0, stats.length);
        
        this.srcBands = new Integer[bands.length];
        System.arraycopy(bands, 0, this.srcBands, 0, bands.length);
        
        this.roi = roi;
        this.rangeLocalStats = rangeLocalStats;
        this.ranges = CollectionFactory.list();
        this.rangesType = rangesType;
        if (ranges != null && !ranges.isEmpty()) {

            // copy the ranges defensively
            for (Range<Double> r : ranges) {
                this.ranges.add(new Range<Double>(r));
            }
        }

        this.noDataRanges = CollectionFactory.list();
        if (noDataRanges != null && !noDataRanges.isEmpty()) {

            // copy the ranges defensively
            for (Range<Double> r : noDataRanges) {
                this.noDataRanges.add(new Range<Double>(r));
            }
        }
    }

    /**
     * Delegates calculation of statistics to either {@linkplain #compileRangeStatistics()}
     * or {@linkplain #compileClassifiedStatistics()}.
     *
     * @return the results as a new instance of {@code ZonalStats}
     */
    private synchronized ClassifiedStats compileStatistics() {
        if (!rangeLocalStats) {
            return compileClassifiedStatistics();
        } else {
            return compileRangeStatistics();
        }
    }

    /**
     * Called by {@link #compileZonalStatistics()} to lazily create a
     * {@link StreamingSampleStats} object for each zone as it is encountered
     * in the zone image. The new object is added to the provided {@code resultsPerBand}
     * {@code Map}.
     * 
     * @param resultsPerBand {@code Map} of results by zone id
     * @param classifier
     * 
     * @return a new {@code StreamingSampleStats} object
     */
    protected StreamingSampleStats setupZoneStats(Map<MultiKey, StreamingSampleStats> resultsPerBand, MultiKey classifier) {
        StreamingSampleStats sampleStats = new StreamingSampleStats(Range.Type.EXCLUDE);
        for (Range<Double> r : ranges) {
            sampleStats.addRange(r);
        }
        for (Range<Double> r : noDataRanges) {
            sampleStats.addNoDataRange(r);
        }
        sampleStats.setStatistics(stats);
        resultsPerBand.put(classifier, sampleStats);
        return sampleStats;
    }

    /**
     * Used to calculate statistics when a zone image was provided.
     *
     * @return the results as a {@code ZonalStats} instance
     */
    private ClassifiedStats compileClassifiedStatistics() {

        Map<Integer, Map<MultiKey, StreamingSampleStats>> results = CollectionFactory.sortedMap();
        for( Integer srcBand : srcBands) {
            Map<MultiKey, StreamingSampleStats> resultsPerBand = new HashMap<MultiKey, StreamingSampleStats>();
            results.put(srcBand, resultsPerBand);
        }

        final double[] sampleValues = new double[dataImage.getSampleModel().getNumBands()];
        RectIter dataIter = RectIterFactory.create(dataImage, null);
        final int classifiers = classifiedImages.length;
        RectIter classIter[] = new RectIter[classifiers];
        for (int i = 0; i <classifiers; i++){
            classIter[i] = RectIterFactory.create(classifiedImages[i], null);
        }
        
        int[] classifierValue = new int[1];
        int y = dataImage.getMinY();
        do {
            int x = dataImage.getMinX();
            do {
                
                if (roi == null || roi.contains(x, y)) {
                    dataIter.getPixel(sampleValues);
                    
                    for (Integer band : srcBands) {
                        Map<MultiKey, StreamingSampleStats> resultPerBand = results.get(band);
                        Integer[] key = new Integer[classifiers];
                        for (int i = 0; i <classifiers; i++){
                            classIter[i].getPixel(classifierValue);
                            key[i] = classifierValue[0];
                        }
                        
                        MultiKey mk = new MultiKey(key);
                        StreamingSampleStats sss = resultPerBand.get(mk);
                        if (sss == null) {
                            sss = setupZoneStats(resultPerBand, mk);
                        }
                        sss.offer(sampleValues[band]);
                    }
                }
                x++;
            } while( !(nextPixels(classIter) && dataIter.nextPixelDone()));
            for (int i=0; i<classifiers; i++){
                classIter[i].startPixels();
            }
            dataIter.startPixels();
            y++;

        } while( !(nextLines(classIter) && dataIter.nextLineDone()));



        // collect all found zones
        Set<MultiKey> classifKeys = new HashSet<MultiKey>();
        for( Integer band : srcBands ) {
            Set<MultiKey> classifiedSetForBand = results.get(band).keySet();
            classifKeys.addAll(classifiedSetForBand);
        }

        // set the results
        ClassifiedStats classifiedStats = new ClassifiedStats();
        for( Integer band : srcBands ) {
            for( MultiKey classifier : classifKeys) {
                classifiedStats.setResults(band, classifier, results.get(band).get(classifier));
            }
        }

        return classifiedStats;
    }

//    /**
//     * Used to calculate statistics when no zone image was provided.
//     *
//     * @return the results as a {@code ZonalStats} instance
//     */
//    private ClassifiedStats compileClassifiedStatistics() {
//        buildZoneList();
//        Integer zoneID = zones.first();
//
//        // create the stats
//        final StreamingSampleStats sampleStatsPerBand[] = new StreamingSampleStats[srcBands.length];
//        for (int index = 0; index < srcBands.length; index++) {
//            final StreamingSampleStats sampleStats = new StreamingSampleStats(rangesType);
//            for (Range<Double> r : ranges) {
//                sampleStats.addRange(r);
//            }
//            for (Range<Double> r : noDataRanges) {
//                sampleStats.addNoDataRange(r);
//            }
//            sampleStats.setStatistics(stats);
//            sampleStatsPerBand[index] = sampleStats;
//        }
//
//        final double[] sampleValues = new double[dataImage.getSampleModel().getNumBands()];
//        RectIter dataIter = RectIterFactory.create(dataImage, null);
//        int y = dataImage.getMinY();
//        do {
//            int x = dataImage.getMinX();
//            do {
//                if (roi == null || roi.contains(x, y)) {
//                    dataIter.getPixel(sampleValues);
//                    for (int index = 0; index < srcBands.length; index++) {
//                        final double value = sampleValues[srcBands[index]];
//                        sampleStatsPerBand[index].offer(value);
//                    }
//                }
//                x++;
//            } while (!dataIter.nextPixelDone() );
//
//            dataIter.startPixels();
//            y++;
//
//        } while (!dataIter.nextLineDone() );
//
//        // get the results
//        final ClassifiedStats zs = new ClassifiedStats();
//        for (int index = 0; index < srcBands.length; index++) {
//            final StreamingSampleStats sampleStats = sampleStatsPerBand[index];
//            List<Range> inclRanges = null;
//            if (ranges != null && !ranges.isEmpty()) {
//                switch (rangesType) {
//                    case INCLUDE:
//                        inclRanges = CollectionFactory.list();
//                        inclRanges.addAll(ranges);
//                        break;
//                    case EXCLUDE:
//                        inclRanges = CollectionFactory.list();
//                        List<Range<Double>> incRanges = RangeUtils.createComplement(RangeUtils.sort(ranges));
//                        inclRanges.addAll(incRanges);
//                        break;
//                }
//            }
//            zs.setResults(srcBands[index], zoneID, sampleStats, inclRanges);
//        }
//        return zs;
//    }
//
    /**
     * Used to calculate statistics when range local statistics are required.
     *
     * @return the results as a {@code ClassifiedStats} instance
     */
    private ClassifiedStats compileRangeStatistics() {
        final ClassifiedStats classifiedStats = new ClassifiedStats();
        final int classifiers = classifiedImages.length;
        RectIter classIter[] = new RectIter[classifiers];
        for (int i = 0; i <classifiers; i++){
            classIter[i] = RectIterFactory.create(classifiedImages[i], null);
        }
        
        int[] classifierValue = new int[1];
        List<Range> localRanges = null;
        switch (rangesType) {
            case EXCLUDE:
                List<Range<Double>> inRanges = RangeUtils.createComplement(RangeUtils.sort(ranges));
                localRanges = CollectionFactory.list();
                localRanges.addAll(inRanges);
                break;
            case INCLUDE:
                localRanges = CollectionFactory.list();
                localRanges.addAll(ranges);
                break;
            case UNDEFINED:
                throw new UnsupportedOperationException("Unable to compute range local statistics on UNDEFINED ranges type");
        }
        
        final double[] sampleValues = new double[dataImage.getSampleModel().getNumBands()];
        RectIter dataIter = RectIterFactory.create(dataImage, null);
        for (Range<Double> range : localRanges) {
            Map<Integer, Map<MultiKey, StreamingSampleStats>> results = CollectionFactory.sortedMap();
            // create the stats
            final StreamingSampleStats sampleStatsPerBand[] = new StreamingSampleStats[srcBands.length];
            for (int index = 0; index < srcBands.length; index++) {
                final StreamingSampleStats sampleStats = new StreamingSampleStats(rangesType);
                sampleStats.addRange(range);
                for (Range<Double> noDataRange : noDataRanges) {
                    sampleStats.addNoDataRange(noDataRange);
                }
                sampleStats.setStatistics(stats);
                sampleStatsPerBand[index] = sampleStats;
            }

            for (int i = 0; i <classifiers; i++){
                classIter[i].startLines();
                classIter[i].startPixels();
            }
            dataIter.startPixels();
            dataIter.startLines();
            
            int y = dataImage.getMinY();
            do {
                int x = dataImage.getMinX();
                do {
                    if (roi == null || roi.contains(x, y)) {
                        dataIter.getPixel(sampleValues);
                                for (Integer band : srcBands) {
                                    Map<MultiKey, StreamingSampleStats> resultPerBand = results.get(band);
                                    Integer[] key = new Integer[classifiers];
                                    for (int i = 0; i <classifiers; i++){
                                        classIter[i].getPixel(classifierValue);
                                        key[i] = classifierValue[0];
                                    }
                                    
                                    MultiKey mk = new MultiKey(key);
                                    StreamingSampleStats sss = resultPerBand.get(mk);
                                    if (sss == null) {
                                        sss = setupZoneStats(resultPerBand, mk);
                                    }
                                    sss.offer(sampleValues[band]);
                                }
                            }
                            x++;
                        } while( !(nextPixels(classIter) && dataIter.nextPixelDone()));
                        for (int i=0; i<classifiers; i++){
                            classIter[i].startPixels();
                        }
                        dataIter.startPixels();
                        y++;

                    } while( !(nextLines(classIter) && dataIter.nextLineDone()));
                
                Set<MultiKey> classifKeys = new HashSet<MultiKey>();
                for( Integer band : srcBands ) {
                    Set<MultiKey> classifiedSetForBand = results.get(band).keySet();
                    classifKeys.addAll(classifiedSetForBand);
                }

                
//                for( Integer band : srcBands ) {
//                    for( MultiKey classifier : classifKeys) {
//                        classifiedStats.setResults(band, classifier, results.get(band).get(classifier));
//                    }
//                }
                
            // get the results
            for (int index = 0; index < srcBands.length; index++) {
                StreamingSampleStats sampleStats = sampleStatsPerBand[index];
                List<Range> resultRanges = CollectionFactory.list();
                resultRanges.add(range);
                for( MultiKey classifier : classifKeys) {
                    classifiedStats.setResults(srcBands[index], classifier, results.get(index).get(classifier), sampleStats, resultRanges);
                }
                ;
            }
        }

        return classifiedStats;
    }

    private boolean nextPixels(RectIter[] classIter) {
        for (int i=0; i<classIter.length; i++){
            classIter[i].nextPixel();
        }
        return true;
    }
    
    private boolean nextLines(RectIter[] classIter) {
        for (int i=0; i<classIter.length; i++){
            classIter[i].nextLine();
        }
        return true;
    }

    /**
     * Get the specified property.
     * <p>
     * Use this method to retrieve the calculated statistics as a map of {@code ZonalStats} per band
     * by setting {@code name} to {@linkplain ClassifiedStatsDescriptor#CLASSIFIED_STATS_PROPERTY}.
     *
     * @param name property name
     *
     * @return the requested property
     */
    @Override
    public Object getProperty( String name ) {
        if (ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY.equalsIgnoreCase(name)) {
            return compileStatistics();
        } else {
            return super.getProperty(name);
        }
    }

    /**
     * Get the class of the given property. For
     * {@linkplain ClassifiedStatsDescriptor#CLASSIFIED_STATS_PROPERTY} this will return
     * {@code Map.class}.
     *
     * @param name property name
     *
     * @return the property class
     */
    @Override
    public Class<?> getPropertyClass( String name ) {
        if (ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY.equalsIgnoreCase(name)) {
            return Map.class;
        } else {
            return super.getPropertyClass(name);
        }
    }

    /**
     * Get all property names
     * @return property names as an array of Strings
     */
    @Override
    public String[] getPropertyNames() {
        String[] names;
        int k = 0;

        String[] superNames = super.getPropertyNames();
        if (superNames != null) {
            names = new String[superNames.length + 1];
            for( String name : super.getPropertyNames() ) {
                names[k++] = name;
            }
        } else {
            names = new String[1];
        }

        names[k] = ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY;
        return names;
    }

}
