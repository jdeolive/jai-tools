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

import java.awt.image.RenderedImage;
import java.io.IOException;
import java.io.InputStream;
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
import org.jaitools.CollectionFactory;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.junit.Test;

/**
 * Unit tests for the ClassifiedStats operator
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @since 1.2
 */
public class ClassifiedStatsTest {

    private static final Logger LOGGER = Logger.getLogger("ClassifiedStatsTest");
    
    

    @Test
//    @Ignore
    public void testClassification() throws IOException {
        if (LOGGER.isLoggable(Level.INFO)) {
    		LOGGER.info("   test classification");
        }
        InputStream sample = null;
        InputStream classifier1 = null;
        InputStream classifiedStripes = null;
        try {
            sample = ClassifiedStatsTest.class.getResourceAsStream("sample.tif");
            RenderedImage sampleImage = ImageIO.read(sample);
            classifier1 = ClassifiedStatsTest.class.getResourceAsStream("mask1.tif");
            RenderedImage classifiedImage = ImageIO.read(classifier1);
            classifiedStripes = ClassifiedStatsTest.class.getResourceAsStream("5stripes.tif");
            RenderedImage stripedImage = ImageIO.read(classifiedStripes);
            ParameterBlockJAI pb = new ParameterBlockJAI("ClassifiedStats");
            pb.addSource(sampleImage);
            pb.addSource(stripedImage);
            pb.addSource(classifiedImage);
    
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
            
            System.out.println("Getting Max from the result coming from the 2nd stripe (The first classified raster, with value = 1), " +
            		"\n and the second classified raster with value = 50");
            System.out.println(stats.band(0).statistic(Statistic.MAX).results().get(new MultiKey(1,50)).get(0));
        } finally {
            if (sample != null){
                try {
                    sample.close();
                } catch (Throwable t){
                    
                }
            }
            
            if (classifier1 != null){
                try {
                    classifier1.close();
                } catch (Throwable t){
                    
                }
            }
            
            if (classifiedStripes != null){
                try {
                    classifiedStripes.close();
                } catch (Throwable t){
                    
                }
            }
        }
        
//        System.out.println(classifiedResult.get(0).getStatistic());
        
    }
    
    @Test
//    @Ignore
    public void testClassificationWithLocalRanges() throws IOException {
        if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info("   test classificationWithLocalRanges");
        }

        ParameterBlockJAI pb = new ParameterBlockJAI("ClassifiedStats");
        InputStream sample = ClassifiedStatsTest.class.getResourceAsStream("sample.tif");
        RenderedImage sampleImage = ImageIO.read(sample);
        InputStream classifier1 = ClassifiedStatsTest.class.getResourceAsStream("mask1.tif");
        RenderedImage classifiedImage = ImageIO.read(classifier1);
        InputStream classifiedStripes = ClassifiedStatsTest.class.getResourceAsStream("5stripes.tif");
        RenderedImage stripedImage = ImageIO.read(classifiedStripes);
        pb.addSource(sampleImage);
        pb.addSource(stripedImage);
        pb.addSource(classifiedImage);
        pb.setParameter("stats", new Statistic[]{Statistic.MIN, Statistic.MAX, Statistic.RANGE, Statistic.SUM});
        pb.setParameter("bands", new Integer[]{0});
        
        List<Range<Double>> ranges = CollectionFactory.list();
        ranges.add(Range.create(0d, true, 100d , true));
        ranges.add(Range.create(101d, true, 255d , true));
        pb.setParameter("ranges", ranges);
        pb.setParameter("rangesType", Range.Type.INCLUDE);
        
        pb.setParameter("rangeLocalStats", true);

        RenderedOp op = JAI.create("ClassifiedStats", pb);
        ClassifiedStats stats = (ClassifiedStats) op.getProperty(ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY);

        Map<MultiKey, List<Result>> results = stats.results();
        Set<MultiKey> km = results.keySet();
        Iterator<MultiKey> it = km.iterator();
        while (it.hasNext()) {
            MultiKey key = it.next(); 
            List<Result> rs = results.get(key);
            for (Result r: rs){
                System.out.println(r.toString() + " classifiers:" + key);
            }
        }
        System.out.println("Getting Max from the result coming from the 2nd stripe (The first classified raster, with value = 1), " +
        "\n and the second classified raster with value = 50, for the first and second range");
        System.out.println(stats.band(0).statistic(Statistic.MAX).results().get(new MultiKey(1,50)).get(0));
        System.out.println(stats.band(0).statistic(Statistic.MAX).results().get(new MultiKey(1,50)).get(1));
    }

}