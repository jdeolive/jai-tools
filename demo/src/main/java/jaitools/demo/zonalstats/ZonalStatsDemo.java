/*
 * Copyright 2009-2011 Michael Bedward
 *
 * This file is part of jai-tools.
 *
 * jai-tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * jai-tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with jai-tools.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package jaitools.demo.zonalstats;

import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import jaitools.demo.DemoImages;
import jaitools.media.jai.zonalstats.ZonalStats;
import jaitools.media.jai.zonalstats.ZonalStatsDescriptor;
import jaitools.numeric.Statistic;

/**
 * Demonstrates using the ZonalStats operator to calculate summary statistics of values
 * in a data image within zones defined by a zone image. In this example the data image
 * contains uniform random values and the zones are equal in size, so we expect the value
 * of each summary statistic to be very similar across zones.
 *
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public class ZonalStatsDemo {

    private RenderedImage dataImg;
    private RenderedImage zoneImg;

    /**
     * Main method: constructs an instance of this class (which
     * causes test data to be generated) and runs the demo
     * @param args ignored
     * @throws java.lang.Exception if there was a problem generating
     * the test data
     */
    public static void main(String[] args) throws Exception {
        ZonalStatsDemo me = new ZonalStatsDemo();
        me.demo();
    }
    
    /**
     * Constructor: gets test images.
     */
    public ZonalStatsDemo() {
        dataImg = DemoImages.createUniformRandomImage(500, 500, 10.0);
        zoneImg = DemoImages.createBandedImage(500, 500, 5);
    }

    /**
     * Calculates min, max, median, approximate median and sample standard
     * deviation for a data image of uniformly distributed random values
     * between 0 and 10, within zones which are equal area horizontal bands.
     * The results should be very similar across zones and approximately
     * equal to: min=0; max=10; median = approx median = 5; sdev = 2.88
     */
    private void demo() {
        /*
         * Define the parameters for the ZonalStats operator. We let the
         * default values apply for data image band (0), roi (null),
         * zoneTransform (null / identity) and ignoreNaN (true)
         */
        ParameterBlockJAI pb = new ParameterBlockJAI("zonalstats");
        pb.setSource("dataImage", dataImg);
        pb.setSource("zoneImage", zoneImg);

        Statistic[] statistics = {
            Statistic.MIN,
            Statistic.MAX,
            Statistic.MEDIAN,
            Statistic.APPROX_MEDIAN,
            Statistic.SDEV
        };

        pb.setParameter("stats", statistics);

        RenderedOp zsImg = JAI.create("zonalstats", pb);

        ZonalStats zs = (ZonalStats) zsImg.getProperty(ZonalStatsDescriptor.ZONAL_STATS_PROPERTY);
        System.out.println("                               exact    approx");
        System.out.println(" band zone      min      max   median   median     sdev");
        System.out.println("-----------------------------------------------------------");

        final int band = 0;
        for (int z : zs.getZones()) {
            System.out.printf(" %4d %4d", band, z);

            ZonalStats zoneSubset = zs.band(0).zone(z);
            for (Statistic s : statistics) {
                System.out.printf(" %8.4f", zoneSubset.statistic(s).results().get(0).getValue());
            }
            System.out.println();
        }
    }

}
