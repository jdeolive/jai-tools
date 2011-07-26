/* 
 *  Copyright (c) 2009, Daniele Romagnoli. All rights reserved. 
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

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.numeric.Statistic;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit tests for the ClassifiedStats operator
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @since 1.2
 */
public class ClassifiedStatsTest {

    private static final Logger LOGGER = Logger.getLogger("ClassifiedStatsTest");

    private static final double EPS = 1.0e-6;

    @Test
    @Ignore
    public void testClassification() throws IOException {
        if (LOGGER.isLoggable(Level.INFO)) {
    		LOGGER.info("   test classification");
        }

        ParameterBlockJAI pb = new ParameterBlockJAI("ClassifiedStats");
        pb.addSource(ImageIO.read(new File("d:\\image.tif")));
        pb.addSource(ImageIO.read(new File("d:\\mask1.tif")));
        pb.addSource(ImageIO.read(new File("d:\\mask2.tif")));
        pb.setParameter("stats", new Statistic[]{Statistic.MIN, Statistic.MAX, Statistic.RANGE, Statistic.SUM});
        pb.setParameter("bands", new Integer[]{0});

        RenderedOp op = JAI.create("ClassifiedStats", pb);
        ClassifiedStats stats = (ClassifiedStats) op.getProperty(ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY);

        Map<MultiKey, List<Result>> results = stats.results();
        Set<MultiKey> km = results.keySet();
        Iterator<MultiKey> it = km.iterator();
        while (it.hasNext()) {
            MultiKey key = it.next(); 
            List<Result> rs = results.get(key);
            for (Result r: rs){
                System.out.println(r.toString() + " key:" + key);
            }
        }
    }

}
