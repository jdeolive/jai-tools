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

    private final Rectangle dataImageBounds;

    private final RenderedImage[] classifiedImages;
    
    private double[] noDataForClassifiedImages;

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
     * @param classifiedImages
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
     * @param noDataClassified
     *            an optional array of Doubles defining values to
     *            treat as NODATA for the related classifiedImage. Note that 
     *            classified images will always leverage on integer types 
     *            (BYTE, INTEGER, SHORT, ...). Such noData are specified
     *            as Double to allow the users to provide NaN in case a NoData
     *            is unavailable for the related classifiedImage.
     * 
     * @see ClassifiedStatsDescriptor
     * @see Statistic
     */
    public ClassifiedStatsOpImage(
            final RenderedImage dataImage, 
            final RenderedImage[] classifiedImages,
            final Map<?, ?> config, 
            final ImageLayout layout, 
            final Statistic[] stats, 
            final Integer[] bands, 
            final ROI roi,
            final Collection<Range<Double>> ranges, 
            final Range.Type rangesType, 
            final boolean rangeLocalStats,
            final Collection<Range<Double>> noDataRanges, 
            final Double[] noDataClassified) {

        super(dataImage, layout, config, OpImage.OP_COMPUTE_BOUND);

        this.dataImage = dataImage;
        this.classifiedImages = classifiedImages;

        dataImageBounds = new Rectangle(dataImage.getMinX(), dataImage.getMinY(),
                dataImage.getWidth(), dataImage.getHeight());

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
        if (noDataClassified != null){
            this.noDataForClassifiedImages = new double[noDataClassified.length];
            for (int i = 0; i < noDataClassified.length; i++){
                this.noDataForClassifiedImages[i] = noDataClassified[i];
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
        if (!rangeLocalStats) {
            return compileClassifiedStatistics();
        } else {
            return compileRangeStatistics();
        }
    }

    /**
     * Called by {@link #compileClassifiedStatistics()} to lazily create a
     * {@link StreamingSampleStats} object for each classifier. The new object
     * is added to the provided {@code resultsPerBand} {@code Map}.
     * 
     * @param resultsPerBand
     *            {@code Map} of results by classifier
     * @param classifier
     * 
     * @return a new {@code StreamingSampleStats} object
     */
    protected StreamingSampleStats setupStats(Map<MultiKey, StreamingSampleStats> resultsPerBand,
            MultiKey classifier) {
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
     * Setup statistics 
     * @param resultsPerBand
     * @param classifier
     * @param rangesType
     * @param range
     * @param noDataRanges
     * @param stats
     * @return
     */
    protected StreamingSampleStats setupStats(Map<MultiKey, StreamingSampleStats> resultsPerBand,
            MultiKey classifier, Range.Type rangesType, Range<Double> range,
            List<Range<Double>> noDataRanges, Statistic[] stats) {

        final StreamingSampleStats sampleStats = new StreamingSampleStats(rangesType);
        sampleStats.addRange(range);
        for (Range<Double> r : noDataRanges) {
            sampleStats.addNoDataRange(r);
        }
        sampleStats.setStatistics(stats);
        resultsPerBand.put(classifier, sampleStats);
        return sampleStats;
    }

    /**
     * Used to calculate statistics against classified rasters.
     * 
     * @return the results as a {@code ClassifiedStats} instance
     */
    private ClassifiedStats compileClassifiedStatistics() {
        final ClassifiedStats classifiedStats = new ClassifiedStats();
        Map<Integer, Map<MultiKey, StreamingSampleStats>> results = CollectionFactory.sortedMap();
        for (Integer srcBand : srcBands) {
            Map<MultiKey, StreamingSampleStats> resultsPerBand = new HashMap<MultiKey, StreamingSampleStats>();
            results.put(srcBand, resultsPerBand);
        }

        final double[] sampleValues = new double[dataImage.getSampleModel().getNumBands()];
        final RandomIter dataIter = RandomIterFactory.create(dataImage, dataImageBounds);
        final int numClassified = classifiedImages.length;
        RandomIter[] classIter = new RandomIter[numClassified];
        final boolean [] checkClassified = new boolean[numClassified];
        final int[] noDataClassifiedValues = new int[numClassified]; 
        for (int i = 0; i < numClassified; i++) {
            classIter[i] = RandomIterFactory.create(classifiedImages[i], dataImageBounds);
            checkClassified[i] = (noDataForClassifiedImages != null && !Double.isNaN(noDataForClassifiedImages[i])) ?
                    true : false; 
            noDataClassifiedValues[i] = checkClassified[i] ? (int)noDataForClassifiedImages[i] : 0;        
        }

        // //
        //
        // Init iterations parameters
        //
        // //
        final int width = dataImage.getWidth();
        final int height = dataImage.getHeight();
        final int tileWidth = Math.min(dataImage.getTileWidth(), width);
        final int tileHeight = Math.min(dataImage.getTileHeight(), height);
        final int minY = dataImage.getMinY();
        final int minX = dataImage.getMinX();
        final int maxX = minX + width - 1;
        final int maxY = minY + height - 1;
        final int minTileX = dataImage.getMinTileX();
        final int minTileY = dataImage.getMinTileY();
        final int maxTileX = minTileX+dataImage.getNumXTiles();
        final int maxTileY = minTileY+dataImage.getNumYTiles();
        Integer[] key = new Integer[numClassified];
        // //
        //
        // Iterate
        //
        // //
        // Loop over the tiles
        for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
            for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                for (int tRow = 0; tRow < tileWidth; tRow++) {
                    int row = tileY * tileHeight + tRow;
                    if (row >= minY && row <= maxY) {
                        for (int tCol = 0; tCol < tileHeight; tCol++) {
                            int col = tileX * tileWidth + tCol;
                            if (col >= minX && col <= maxX) {
                                if (roi == null || roi.contains(col, row)) {
                                    //Check for noData on classified Images
                                    boolean skip = false;
                                    for (int i = 0; i < numClassified; i++) {
                                        key[i] = classIter[i].getSample(col, row, 0);
                                        if (checkClassified[i]){
                                            skip = skip || ((int)key[i] == (int)noDataClassifiedValues[i]);
                                        }
                                    }
                                    if (skip){
                                        continue;
                                    }
                                    
                                    //Offer values to statistics operations                                    
                                    for (Integer band : srcBands) {
                                        sampleValues[band] = dataIter.getSample(col, row, band);
                                        Map<MultiKey, StreamingSampleStats> resultPerBand = results.get(band);
                                        MultiKey mk = new MultiKey(key);
                                        StreamingSampleStats sss = resultPerBand.get(mk);
                                        if (sss == null) {
                                            sss = setupStats(resultPerBand, mk);
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
        
        // //
        //
        // Setting results
        //
        // //
        for (Integer band : srcBands) {
            Set<MultiKey> classifiedSetForBand = results.get(band).keySet();
            for (MultiKey classifier : classifiedSetForBand) {
                classifiedStats.setResults(band, classifier, results.get(band).get(classifier));
            }
        }
        
        // //
        // 
        // Closing/disposing the iterators
        // 
        // //
        dataIter.done();
        for (int i = 0; i < numClassified; i++) {
            classIter[i].done();
        }

        return classifiedStats;
    }

    /**
     * Used to calculate statistics when range local statistics are required.
     * 
     * @return the results as a {@code ClassifiedStats} instance
     */
    private ClassifiedStats compileRangeStatistics() {
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
            throw new UnsupportedOperationException(
                    "Unable to compute range local statistics on UNDEFINED ranges type");
        }

        final ClassifiedStats classifiedStats = new ClassifiedStats();
        final double[] sampleValues = new double[dataImage.getSampleModel().getNumBands()];
        final RandomIter dataIter = RandomIterFactory.create(dataImage, dataImageBounds);
        final int numClassified = classifiedImages.length;
        RandomIter classIter[] = new RandomIter[numClassified];
        final boolean [] checkClassified = new boolean[numClassified];
        final int[] noDataClassifiedValues = new int[numClassified]; 
        for (int i = 0; i < numClassified; i++) {
            classIter[i] = RandomIterFactory.create(classifiedImages[i], dataImageBounds);
            checkClassified[i] = (noDataForClassifiedImages != null && !Double.isNaN(noDataForClassifiedImages[i])) ?
                    true : false; 
            noDataClassifiedValues[i] = checkClassified[i] ? (int)noDataForClassifiedImages[i] : 0;        
        }


        // //
        //
        // Init iterations parameters
        //
        // //
        final int width = dataImage.getWidth();
        final int height = dataImage.getHeight();
        final int tileWidth = Math.min(dataImage.getTileWidth(), width);
        final int tileHeight = Math.min(dataImage.getTileHeight(), height);
        final int minY = dataImage.getMinY();
        final int minX = dataImage.getMinX();
        final int maxX = minX + width - 1;
        final int maxY = minY + height - 1;
        final int minTileX = dataImage.getMinTileX();
        final int minTileY = dataImage.getMinTileY();
        final int maxTileX = minTileX+dataImage.getNumXTiles();
        final int maxTileY = minTileY+dataImage.getNumYTiles();


        // //
        //
        // Iterate
        //
        // //
        for (Range<Double> range : localRanges) {
            Map<Integer, Map<MultiKey, StreamingSampleStats>> results = CollectionFactory.sortedMap();
            for (int index = 0; index < srcBands.length; index++) {
                Map<MultiKey, StreamingSampleStats> resultsPerBand = new HashMap<MultiKey, StreamingSampleStats>();
                results.put(index, resultsPerBand);

            }
            Integer[] key = new Integer[numClassified];
            // Loop over the tiles
            for (int tileY = minTileY; tileY <= maxTileY; tileY++) {
                for (int tileX = minTileX; tileX <= maxTileX; tileX++) {
                    for (int tRow = 0; tRow < tileWidth; tRow++) {
                        int row = tileY * tileHeight + tRow;
                        if (row >= minY && row <= maxY) {
                            for (int tCol = 0; tCol < tileHeight; tCol++) {
                                int col = tileX * tileWidth + tCol;
                                if (col >= minX && col <= maxX) {
                                    if (roi == null || roi.contains(col, row)) {
                                        //Check for noData on classified Images
                                        boolean skip = false;
                                        for (int i = 0; i < numClassified; i++) {
                                            key[i] = classIter[i].getSample(col, row, 0);
                                            if (checkClassified[i]){
                                                skip &= (key[i] == noDataClassifiedValues[i]);
                                            }
                                        }
                                        if (skip){
                                            continue;
                                        }
                                        
                                        //Offer values to statistics operations
                                        for (Integer band : srcBands) {
                                            sampleValues[band] = dataIter.getSample(col, row, band);
                                            Map<MultiKey, StreamingSampleStats> resultPerBand = results
                                                    .get(band);

                                            MultiKey mk = new MultiKey(key);
                                            StreamingSampleStats sss = resultPerBand.get(mk);
                                            if (sss == null) {
                                                sss = setupStats(resultPerBand, mk, rangesType,
                                                        range, noDataRanges, stats);
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

            // //
            //
            // Setting results
            //
            // //
            
            Set<MultiKey> classifKeys = new HashSet<MultiKey>();
            for (Integer band : srcBands) {
                Set<MultiKey> classifiedSetForBand = results.get(band).keySet();
                classifKeys.addAll(classifiedSetForBand);
            }

            for (int index = 0; index < srcBands.length; index++) {
                List<Range> resultRanges = CollectionFactory.list();
                resultRanges.add(range);
                for (MultiKey classifier : classifKeys) {
                    classifiedStats.setResults(srcBands[index], classifier,
                            results.get(index).get(classifier), resultRanges);
                }
            }
        }
        
        // //
        // 
        // Closing/disposing the iterators
        // 
        // //
        dataIter.done();
        for (int i = 0; i < numClassified; i++) {
            classIter[i].done();
        }

        return classifiedStats;
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

    /**
     * Set<MultiKey> classifKeys = new HashSet<MultiKey>(); // for( Integer band
     * : srcBands ) { // Set<MultiKey> classifiedSetForBand =
     * results.get(band).keySet(); // classifKeys.addAll(classifiedSetForBand);
     * // } // // // set the results // for( Integer band : srcBands ) { // for(
     * MultiKey classifier : classifKeys) { // classifiedStats.setResults(band,
     * classifier, results.get(band).get(classifier)); // } // }
     */
}
