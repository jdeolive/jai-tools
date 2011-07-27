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

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import org.apache.commons.collections.keyvalue.MultiKey;
import org.jaitools.CollectionFactory;
import org.jaitools.imageutils.ImageLayout2;
import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;
import org.junit.Ignore;
import org.junit.Test;

import com.sun.media.jai.operator.ImageReadDescriptor;

/**
 * Unit tests for the ClassifiedStats operator
 * 
 * @author Daniele Romagnoli, GeoSolutions SAS
 * @since 1.2
 */
public class GSClassifiedStatsTest {

    static RenderingHints hints;

    static {
        ImageLayout2 layout = new ImageLayout2();
        layout.setTileGridXOffset(0);
        layout.setTileGridYOffset(0);
        layout.setTileHeight(512);
        layout.setTileWidth(512);
        hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, layout);
        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(1024 * 1024 * 512);
    }

    private static final Logger LOGGER = Logger.getLogger("GSClassifiedStatsTest");

    @Test
    @Ignore
    public void testClassificationOnMeasure() throws IOException {
//        long start = System.nanoTime();
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(" test classification on measured data");
        }
        ImageInputStream sampleIs = null;
        ImageReader sampleImageReader = null;
        ImageInputStream gaulIs = null;
        ImageReader gaulImageReader = null;
        try {
            final File sampleFile = new File("d:\\data\\gs\\measuredData.tif");
            sampleIs = ImageIO.createImageInputStream(sampleFile);
            sampleImageReader = ImageIO.getImageReaders(sampleIs).next();
            RenderedImage sampleImage = ImageReadDescriptor.create(sampleIs, 0, false, false,
                    false, null, null, null, sampleImageReader, hints);

            final File gaulFile = new File("d:\\data\\gs\\worldZoneIdentifiers.tif");
            gaulIs = ImageIO.createImageInputStream(gaulFile);
            gaulImageReader = ImageIO.getImageReaders(gaulIs).next();
            RenderedImage gaulImage = ImageReadDescriptor.create(gaulIs, 0, false, false, false,
                    null, null, null, gaulImageReader, hints);

            ParameterBlockJAI pb = new ParameterBlockJAI("ClassifiedStats");
            pb.addSource(sampleImage);
            pb.setParameter("classifiers", new RenderedImage[] { gaulImage });
            pb.setParameter("noDataClassifiers", new Double[] { -32768d });
            List<Range<Double>> noRanges = CollectionFactory.list();
            noRanges.add(Range.create(-9.0d, null));
            pb.setParameter("noDataRanges", noRanges);

            pb.setParameter("stats", new Statistic[] { Statistic.SUM });
            pb.setParameter("bands", new Integer[] { 0 });

            RenderedOp op = JAI.create("ClassifiedStats", pb);
            ClassifiedStats stats = (ClassifiedStats) op
                    .getProperty(ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY);

            Map<MultiKey, List<Result>> results = stats.results();
            Set<MultiKey> km = results.keySet();
            Iterator<MultiKey> it = km.iterator();
            while (it.hasNext()) {
                MultiKey key = it.next();
                List<Result> rs = results.get(key);
                for (Result r : rs) {
                    System.out.println(r.toString() + " class:" + key);
                }
            }

        } finally {

            if (sampleIs != null) {
                try {
                    sampleIs.close();
                } catch (Throwable t) {

                }
            }

            if (sampleImageReader != null) {
                try {
                    sampleImageReader.dispose();
                } catch (Throwable t) {

                }
            }

            if (gaulIs != null) {
                try {
                    gaulIs.close();
                } catch (Throwable t) {

                }
            }

            if (gaulImageReader != null) {
                try {
                    gaulImageReader.dispose();
                } catch (Throwable t) {

                }
            }
        }
//        long end = System.nanoTime();
//        System.out.println("time: " + (end - start)/1000000 );
    }

    @Test
    @Ignore
    public void testClassificationOnArea() throws IOException {
//        long start = System.nanoTime();
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(" test classification on Area");
        }
        ImageInputStream sampleIs = null;
        ImageReader sampleImageReader = null;
        ImageInputStream gaulIs = null;
        ImageReader gaulImageReader = null;
        ImageInputStream faoIs = null;
        ImageReader faoImageReader = null;
        try {
            final File sampleFile = new File("d:\\data\\gs\\area.tif");
            sampleIs = ImageIO.createImageInputStream(sampleFile);
            sampleImageReader = ImageIO.getImageReaders(sampleIs).next();
            RenderedImage sampleImage = ImageReadDescriptor.create(sampleIs, 0, false, false,
                    false, null, null, null, sampleImageReader, hints);

            final File gaulFile = new File("d:\\data\\gs\\worldZoneIdentifiers.tif");
            gaulIs = ImageIO.createImageInputStream(gaulFile);
            gaulImageReader = ImageIO.getImageReaders(gaulIs).next();
            RenderedImage gaulImage = ImageReadDescriptor.create(gaulIs, 0, false, false, false,
                    null, null, null, gaulImageReader, hints);

            final File faoFile = new File("d:\\data\\gs\\landMap.tif");
            faoIs = ImageIO.createImageInputStream(faoFile);
            faoImageReader = ImageIO.getImageReaders(faoIs).next();
            RenderedImage faoImage = ImageReadDescriptor.create(faoIs, 0, false, false, false,
                    null, null, null, faoImageReader, hints);

            ParameterBlockJAI pb = new ParameterBlockJAI("ClassifiedStats");
            pb.addSource(sampleImage);
            pb.setParameter("classifiers", new RenderedImage[] { gaulImage, faoImage });
            pb.setParameter("noDataClassifiers", new Double[] { -32768d, 0d });
            List<Range<Double>> noRanges = CollectionFactory.list();
            noRanges.add(Range.create(-9.0d, null));
            pb.setParameter("noDataRanges", noRanges);

            pb.setParameter("stats", new Statistic[] { Statistic.SUM });
            pb.setParameter("bands", new Integer[] { 0 });

            RenderedOp op = JAI.create("ClassifiedStats", pb);
            ClassifiedStats stats = (ClassifiedStats) op
                    .getProperty(ClassifiedStatsDescriptor.CLASSIFIED_STATS_PROPERTY);

            Map<MultiKey, List<Result>> results = stats.results();
            Set<MultiKey> km = results.keySet();
            Iterator<MultiKey> it = km.iterator();
            while (it.hasNext()) {
                MultiKey key = it.next();
                List<Result> rs = results.get(key);
                for (Result r : rs) {
                    System.out.println(r.toString() + " class:" + key);
                }
            }

        } finally {

            if (sampleIs != null) {
                try {
                    sampleIs.close();
                } catch (Throwable t) {

                }
            }

            if (sampleImageReader != null) {
                try {
                    sampleImageReader.dispose();
                } catch (Throwable t) {

                }
            }

            if (gaulIs != null) {
                try {
                    gaulIs.close();
                } catch (Throwable t) {

                }
            }

            if (gaulImageReader != null) {
                try {
                    gaulImageReader.dispose();
                } catch (Throwable t) {

                }
            }

            if (faoIs != null) {
                try {
                    faoIs.close();
                } catch (Throwable t) {

                }
            }

            if (faoImageReader != null) {
                try {
                    faoImageReader.dispose();
                } catch (Throwable t) {

                }
            }
        }
//        long end = System.nanoTime();
//        System.out.println("time: " + (end - start)/1000000 );
    }
    
}
