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

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.util.Collection;
import java.util.Collections;
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
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.CollectionFactory;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Range.Type;
import org.jaitools.numeric.RangeUtils;
import org.jaitools.numeric.Statistic;
import org.jaitools.numeric.StreamingSampleStats;

/**
 * Calculates image summary statistics for a data image.
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
    
    private int imageWidth;
    private int imageHeigth;
    private int imageMinY;
    private int imageMinX;
    private int imageMaxX;
    private int imageMaxY;
    private int imageMinTileX;
    private int imageMinTileY;
    private int imageMaxTileY;
    private int imageMaxTileX;
    private int imageTileHeight;
    private int imageTileWidth;

    private final Rectangle dataImageBounds;

    private final RenderedImage[] classifierImages;
    
    private double[] noDataForClassifierImages;

    /**
     * A simple object holding classifier properties:
     * - a RandomIterator attached to the classifier Image
     * - a boolean stating whether we need to check for noData on it
     * - a noData value in case we need to check for it. In case the previous boolean
     * is set to false, the noData value will be ignored. 
     * 
     * @author Daniele Romagnoli, GeoSolutions SAS
     */
    private class ClassifierObject {
        /**
         * @param classifierIter
         * @param checkForNoData
         * @param noData
         */
        public ClassifierObject(RandomIter classifierIter, boolean checkForNoData, int noData) {
            this.classifierIter = classifierIter;
            this.checkForNoData = checkForNoData;
            this.noData = noData;
        }
        
        RandomIter classifierIter;
        boolean checkForNoData;
        int noData; //Classifiers are always of integer type
    }
    
    /**
     * Optional ranges to exclude/include values from/in statistics computations
     */
    private final List<Range<Double>> ranges;

    /**
     * Optional ranges to specify which values should be considered as NoData
     * and then excluded from computations
     */
    private final List<Range<Double>> noDataRanges;

    /** Compute separated statistics on ranges if true */
    private final boolean rangeLocalStats;

    /**
     * Define whether provided ranges of values need to be included or excluded
     * from statistics computations
     */
    private Range.Type rangesType;

    /**
     * Constructor.
     * 
     * @param dataImage
     *            a {@code RenderedImage} from which data values will be read.
     * 
     * @param classifierImages
     *            a {@code RenderedImage}'s array of integral data type that
     *            defines the classification for which to calculate summary
     *            data.
     * 
     * @param config
     *            configurable attributes of the image (see {@link AreaOpImage}
     *            ).
     * 
     * @param layout
     *            an optional {@code ImageLayout} object.
     * 
     * @param stats
     *            an array of {@code Statistic} constants specifying the data
     *            required.
     * 
     * @param bands
     *            the data image band to process.
     * 
     * @param roi
     *            an optional {@code ROI} for data image masking.
     * 
     * @param ranges
     *            an optional list of {@link Range} objects defining values to
     *            include or exclude (depending on {@code rangesType} from the
     *            calculations; may be {@code null} or empty
     * 
     * @param rangesType
     *            specifies whether the {@code ranges} argument defines values
     *            to include or exclude
     * 
     * @param rangeLocalStats
     *            if {@code true}, the statistics should be computed for ranges,
     *            separately.
     * 
     * @param noDataRanges
     *            an optional list of {@link Range} objects defining values to
     *            treat as NODATA
     * @param noDataClassifiers
     *            an optional array of Doubles defining values to
     *            treat as NODATA for the related classifierImage. Note that 
     *            classifier images will always leverage on integer types 
     *            (BYTE, INTEGER, SHORT, ...). Such noData are specified
     *            as Double to allow the users to provide NaN in case a NoData
     *            is unavailable for the related classifierImage.
     * 
     * @see ClassifiedStatsDescriptor
     * @see Statistic
     */
    public ClassifiedStatsOpImage(
            final RenderedImage dataImage, 
            final RenderedImage[] classifierImages,
            final Map<?, ?> config, 
            final ImageLayout layout, 
            final Statistic[] stats, 
            final Integer[] bands, 
            final ROI roi,
            final Collection<Range<Double>> ranges, 
            final Range.Type rangesType, 
            final boolean rangeLocalStats,
            final Collection<Range<Double>> noDataRanges, 
            final Double[] noDataClassifiers) {

        super(dataImage, layout, config, OpImage.OP_COMPUTE_BOUND);

        this.dataImage = dataImage;
        this.classifierImages = classifierImages;
        
        // Setting imagesParameters
        this.imageWidth = dataImage.getWidth();
        this.imageHeigth = dataImage.getHeight();
        this.imageTileWidth = Math.min(dataImage.getTileWidth(), imageWidth);
        this.imageTileHeight = Math.min(dataImage.getTileHeight(), imageHeigth);
        this.imageMinY = dataImage.getMinY();
        this.imageMinX = dataImage.getMinX();
        this.imageMaxX = imageMinX + imageWidth - 1;
        this.imageMaxY = imageMinY + imageHeigth - 1;
        this.imageMinTileX = dataImage.getMinTileX();
        this.imageMinTileY = dataImage.getMinTileY();
        this.imageMaxTileX = imageMinTileX + dataImage.getNumXTiles();
        this.imageMaxTileY = imageMinTileY + dataImage.getNumYTiles();
        dataImageBounds = new Rectangle(imageMinX, imageMinY, imageWidth, imageHeigth);

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
        if (noDataClassifiers != null){
            this.noDataForClassifierImages = new double[noDataClassifiers.length];
            for (int i = 0; i < noDataClassifiers.length; i++){
                this.noDataForClassifierImages[i] = noDataClassifiers[i];
            }
        }
    }

    /**
     * Delegates calculation of statistics to either
     * {@linkplain #compileRangeStatistics()} or
     * {@linkplain #compileClassifiedStatistics()}.
     * 
     * @return the results as a new instance of {@code ClassifiedStats}
     */
    private synchronized ClassifiedStats compileStatistics() {
        ClassifiedStats classifiedStats = null;

        // //
        //
        // Init classifiers and iterators
        //
        // //
        final RandomIter dataIter = RandomIterFactory.create(dataImage, dataImageBounds);
        final int numClassifiers = classifierImages.length;
        final ClassifierObject[] classifiers = new ClassifierObject[numClassifiers];
        for (int i = 0; i < numClassifiers; i++) {
            final RandomIter classifierIter = RandomIterFactory.create(classifierImages[i], dataImageBounds);
            final boolean checkForNoData = (noDataForClassifierImages != null && !Double.isNaN(noDataForClassifierImages[i])) ?
                    true : false; 
            final int noDataClassifierValue = checkForNoData ? (int)noDataForClassifierImages[i] : 0;
            classifiers[i] = new ClassifierObject(classifierIter, checkForNoData, noDataClassifierValue);
        }
        
        // //
        //
        // Compute statistics
        //
        // //
        if (!rangeLocalStats) {
            classifiedStats = compileClassifiedStatistics(dataIter, classifiers);
        } else {
            classifiedStats = compileLocalRangeStatistics(dataIter, classifiers);
        }
        
        // //
        // 
        // Closing/disposing the iterators
        // 
        // //
        dataIter.done();
        for (int i = 0; i < numClassifiers; i++) {
            classifiers[i].classifierIter.done();
        }

        return classifiedStats;
    }

    /**
     * Called by {@link #compileClassifiedStatistics()} to lazily create a
     * {@link StreamingSampleStats} object for each classifier. The new object
     * is added to the provided {@code resultsPerBand} {@code Map}.
     * 
     * @param resultsPerBand
     *            {@code Map} of results by classifier
     * @param classifierKey
     *          the classifier key referring to this statistic
     * @param rangesType
     *          the range type
     * @param ranges
     *          a List of Range to be added to these stats.
     *          
     * 
     * @return a new {@code StreamingSampleStats} object
     */
    protected StreamingSampleStats setupStats(Map<MultiKey, StreamingSampleStats> resultsPerBand,
            MultiKey classifierKey, Range.Type rangesType, List<Range<Double>> ranges) {
        StreamingSampleStats sampleStats = new StreamingSampleStats(rangesType);
        for (Range<Double> r : ranges) {
            sampleStats.addRange(r);
        }
        for (Range<Double> r : noDataRanges) {
            sampleStats.addNoDataRange(r);
        }
        sampleStats.setStatistics(stats);
        resultsPerBand.put(classifierKey, sampleStats);
        return sampleStats;
    }

    /**
     * Used to calculate statistics against classifier rasters.
     * @param dataIter 
     *          the input image data iterator
     * @param classifiers 
     *          the classifiers objects for the classified stat 
     * 
     * @return the results as a {@code ClassifiedStats} instance
     */
    private ClassifiedStats compileClassifiedStatistics(
            final RandomIter dataIter,
            ClassifierObject[] classifiers) {
        ClassifiedStats classifiedStats = new ClassifiedStats();
        Map<Integer, Map<MultiKey, StreamingSampleStats>> results = CollectionFactory.sortedMap();
        for (Integer srcBand : srcBands) {
            Map<MultiKey, StreamingSampleStats> resultsPerBand = new HashMap<MultiKey, StreamingSampleStats>();
            results.put(srcBand, resultsPerBand);
        }
        
        // //
        //
        // Init iterations parameters
        //
        // //
        Type localRangeType = Range.Type.EXCLUDE;
        List<Range<Double>> localRanges = ranges;
        // //
        //
        // Iterate
        //
        // //
        // Loop over the tiles
        computeStatsOnTiles(dataIter, classifiers, localRangeType, localRanges, results);
        
        // //
        //
        // Setting results
        //
        // //
        for (Integer band : srcBands) {
            Set<MultiKey> classifierSetForBand = results.get(band).keySet();
            for (MultiKey classifier : classifierSetForBand) {
                classifiedStats.setResults(band, classifier, results.get(band).get(classifier));
            }
        }
        return classifiedStats;
       
    }


    /**
     * Used to calculate statistics when range local statistics are required.
     * @param dataIter 
     *          the input image data iterator
     * @param classifiers 
     *          the classifiers objects for the classified stat 
     * 
     * @return the results as a {@code ClassifiedStats} instance
     */
    private ClassifiedStats compileLocalRangeStatistics(
            final RandomIter dataIter,
            final ClassifierObject[] classifiers) {
        ClassifiedStats classifiedStats = new ClassifiedStats();
        List<Range<Double>> rangesList = null;
        switch (rangesType) {
        case EXCLUDE:
            List<Range<Double>> inRanges = RangeUtils.createComplement(RangeUtils.sort(ranges));
            rangesList = CollectionFactory.list();
            rangesList.addAll(inRanges);
            break;
        case INCLUDE:
            rangesList = CollectionFactory.list();
            rangesList.addAll(ranges);
            break;
        case UNDEFINED:
            throw new UnsupportedOperationException(
                    "Unable to compute range local statistics on UNDEFINED ranges type");
        }

        Type localRangeType = rangesType;

        // //
        //
        // Iterate
        //
        // //
        for (Range<Double> range : rangesList) {
            Map<Integer, Map<MultiKey, StreamingSampleStats>> results = CollectionFactory.sortedMap();
            for (int index = 0; index < srcBands.length; index++) {
                Map<MultiKey, StreamingSampleStats> resultsPerBand = new HashMap<MultiKey, StreamingSampleStats>();
                results.put(index, resultsPerBand);

            }
            final List<Range<Double>> localRanges = Collections.singletonList(range);
            
            // Loop over the tiles
            computeStatsOnTiles(dataIter, classifiers, localRangeType, localRanges, results);


            // //
            //
            // Setting results
            //
            // //
            final Set<MultiKey> classifKeys = new HashSet<MultiKey>();
            for (Integer band : srcBands) {
                Set<MultiKey> classifierSetForBand = results.get(band).keySet();
                classifKeys.addAll(classifierSetForBand);
            }

            for (int index = 0; index < srcBands.length; index++) {
                for (MultiKey classifier : classifKeys) {
                    classifiedStats.setResults(srcBands[index], classifier,
                            results.get(index).get(classifier), localRanges);
                }
            }
        }
        return classifiedStats;
    }

    /**
     * Compute statistics looping along tiles.
     * 
     * @param dataIter
     * @param numClassifiers
     * @param classIter
     * @param checkClassifiers
     * @param noDataClassifierValues
     * @param localRangeType
     * @param localRanges
     * @param results
     */
    private void computeStatsOnTiles( 
            final RandomIter dataIter,
            final ClassifierObject[] classifiers, 
            final Type localRangeType, List<Range<Double>> localRanges, 
            final Map<Integer, Map<MultiKey, StreamingSampleStats>> results) {
        
        // Initialization
        final int numClassifiers = classifiers.length;
        final double[] sampleValues = new double[dataImage.getSampleModel().getNumBands()];
        final Integer[] key = new Integer[numClassifiers];
        
        // Loop over tiles
        for (int tileY = imageMinTileY; tileY <= imageMaxTileY; tileY++) {
            for (int tileX = imageMinTileX; tileX <= imageMaxTileX; tileX++) {
                for (int tRow = 0; tRow < imageTileWidth; tRow++) {
                    int row = tileY * imageTileHeight + tRow;
                    if (row >= imageMinY && row <= imageMaxY) {
                        for (int tCol = 0; tCol < imageTileHeight; tCol++) {
                            int col = tileX * imageTileWidth + tCol;
                            if (col >= imageMinX && col <= imageMaxX) {
                                if (roi == null || roi.contains(col, row)) {
                                    // Check for noData on classifier Images
                                    // in case a classifier will refer to a noData pixel
                                    // skip the stat for it.
                                    boolean skipStats = false;
                                    for (int i = 0; i < numClassifiers; i++) {
                                        key[i] = classifiers[i].classifierIter.getSample(col, row, 0);
                                        if (classifiers[i].checkForNoData){
                                            skipStats = skipStats || (key[i] == classifiers[i].noData);
                                        }
                                    }
                                    if (skipStats){
                                        continue;
                                    }
                                    
                                    //Offer values to statistics operations                                    
                                    for (Integer band : srcBands) {
                                        sampleValues[band] = dataIter.getSample(col, row, band);
                                        Map<MultiKey, StreamingSampleStats> resultPerBand = results.get(band);
                                        MultiKey mk = new MultiKey(key);
                                        StreamingSampleStats sss = resultPerBand.get(mk);
                                        if (sss == null) {
                                            sss = setupStats(resultPerBand, mk, localRangeType, localRanges);
                                        }
                                        sss.offer(sampleValues[band]);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        
    }
    
    /**
     * Get the specified property.
     * <p>
     * Use this method to retrieve the calculated statistics as a map of
     * {@code ClassifiedStats} per band by setting {@code name} to
     * {@linkplain ClassifiedStatsDescriptor#CLASSIFIED_STATS_PROPERTY}.
     * 
     * @param name
     *            property name
     * 
     * @return the requested property
     */
    @Override
    public Object getProperty(String name) {
        if (ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY.equalsIgnoreCase(name)) {
            return compileStatistics();
        } else {
            return super.getProperty(name);
        }
    }

    /**
     * Get the class of the given property. For
     * {@linkplain ClassifiedStatsDescriptor#CLASSIFIED_STATS_PROPERTY} this
     * will return {@code Map.class}.
     * 
     * @param name
     *            property name
     * 
     * @return the property class
     */
    @Override
    public Class<?> getPropertyClass(String name) {
        if (ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY.equalsIgnoreCase(name)) {
            return Map.class;
        } else {
            return super.getPropertyClass(name);
        }
    }

    /**
     * Get all property names
     * 
     * @return property names as an array of Strings
     */
    @Override
    public String[] getPropertyNames() {
        String[] names;
        int k = 0;

        String[] superNames = super.getPropertyNames();
        if (superNames != null) {
            names = new String[superNames.length + 1];
            for (String name : super.getPropertyNames()) {
                names[k++] = name;
            }
        } else {
            names = new String[1];
        }

        names[k] = ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY;
        return names;
    }

}
