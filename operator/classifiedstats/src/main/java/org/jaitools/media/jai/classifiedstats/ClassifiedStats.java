/* 
 *  Copyright (c) 2009-2010, Daniele Romagnoli. All rights reserved. 
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.CollectionFactory;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.jaitools.numeric.StreamingSampleStats;


/**
 * Holds the results of the ClassifiedStats operator.
 * An instance of this class is stored as a property of the destination
 * image.
 * <p>
 *
 * @see Result
 * @see ClassifiedStatsDescriptor
 *
 * @author Daniele Romagnoli, GeoSolutions S.A.S.
 * @since 1.2
 */
public class ClassifiedStats {

    private Map<MultiKey, List<Result>> results;
    
    /**
     * Constructor. Package-private; called by ClassifiedStatsOpImage.
     */
    ClassifiedStats() {
        results = new HashMap<MultiKey, List<Result>>();
    }

    /**
     * Copy constructor. Used by the chaining methods such as {@linkplain #band(int)}.
     *
     * @param src source object
     * @param band selected image band or {@code null} for all bands
     * @param stat selected statistic or {@code null} for all statistics
     * @param ranges selected ranges or {@code null} for all ranges
     */
    private ClassifiedStats(ClassifiedStats src, Integer band, Statistic stat, List<Range> ranges) {
        results = new HashMap<MultiKey, List<Result>>();
        Set<MultiKey> ks = src.results.keySet();
        Iterator<MultiKey> it = ks.iterator();
        while (it.hasNext()){
            MultiKey mk = it.next();
            List<Result> rs = src.results.get(mk);
            List<Result> rsCopy = CollectionFactory.list();
            for (Result r: rs){
                if ((band == null || r.getImageBand() == band) &&
                    (stat == null || r.getStatistic() == stat)) {
                    if (ranges == null || ranges.isEmpty()) {
                        rsCopy.add(r);
                    } else {
                        if (r.getRanges().containsAll(ranges)) {
                            rsCopy.add(r);
                        } else {
                            for (Range range : ranges) {
                                if (r.getRanges().contains(range)) {
                                    rsCopy.add(r);
                                }
                            }
                        }
                    }
                }
            }
            results.put(mk, rsCopy);
        }
    }

    /**
     * Store the results for the given classificationKey. Package-private method used by
     * {@code ClassifiedStatsOpImage}.
     */
    void setResults(int band, MultiKey classificationKey, StreamingSampleStats stats, List<Range> ranges) {

        //First preliminary check on an already populated list of results for that key
        List<Result> rs = results.get(classificationKey);
        if (rs == null) {
            rs = CollectionFactory.list();    
        }
        
        //Populate the results list by scanning for statistics.
        for (Statistic s : stats.getStatistics()) {
            Result r = new Result(band, s, ranges,
                    stats.getStatisticValue(s),
                    stats.getNumOffered(s),
                    stats.getNumAccepted(s),
                    stats.getNumNaN(s),
                    stats.getNumNoData(s), classificationKey);
            rs.add(r);
        }
        results.put(classificationKey, rs);
    }

    void setResults(int band, MultiKey classifierKey, StreamingSampleStats stats) {
        setResults(band, classifierKey, stats, null);
    }


    /**
     * Get the subset of results for the given band.
     *
     * See the example of chaining this method in the class docs.
     *
     * @param b band index
     *
     * @return a new {@code ClassifiedStats} object containing results for the band
     *         (data are shared with the source object rather than copied)
     */
    public ClassifiedStats band(int b) {
        return new ClassifiedStats(this, b, null, null);
    }

    /**
     * Get the subset of results for the given {@code Statistic}.
     *
     * See the example of chaining this method in the class docs.
     *
     * @param s the statistic
     *
     * @return a new {@code ClassifiedStats} object containing results for the statistic
     *         (data are shared with the source object rather than copied)
     */
    public ClassifiedStats statistic(Statistic s) {
        return new ClassifiedStats(this, null, s, null);
    }

    /**
     * Get the subset of results for the given {@code Ranges}.
     *
     * @param ranges the Ranges
     *
     * @return a new {@code ClassifiedStats} object containing results for the ranges
     *         (data are shared with the source object rather than copied)
     */
    public ClassifiedStats ranges(List<Range> ranges) {
        return new ClassifiedStats(this, null, null, ranges);
    }

    /**
     * Returns the {@code Result} objects as a Map<MultiKey, List<Result>> 
     * The keys are multiKey setup on top of the classified pixel values. For each of them,
     * a List of {@code Result}s is provided. In case of classified stats against local ranges,
     * the list will contain the Result for each range.
     *
     * @return the results
     * @see Result
     */
    public Map<MultiKey, List<Result>> results() {
        return Collections.unmodifiableMap(results);
    }
}
