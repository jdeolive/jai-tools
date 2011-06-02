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
package jaitools.demo.regionalize;

import jaitools.CollectionFactory;
import java.awt.Color;
import java.awt.image.RenderedImage;
import java.util.List;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;

import jaitools.demo.DemoImages;
import jaitools.imageutils.ImageUtils;
import jaitools.media.jai.regionalize.Region;
import jaitools.media.jai.regionalize.RegionalizeDescriptor;
import jaitools.swing.ImageFrame;
import java.util.Map;

/**
 * Demonstrates using the Regionalize operation to identify regions
 * of uniform value in a source image.
 *
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public class RegionalizeDemo {

    /**
     * Main method: simple calls the demo method
     * @param args ignored
     */
    public static void main(String[] args) {
        RegionalizeDemo me = new RegionalizeDemo();
        me.demo();
    }

    /**
     * Gets a test image (the chessboard image) from the
     * {@linkplain DemoImages object}. When the image
     * has been created the receiveImage method will be called.
     */
    public void demo() {
        RenderedImage image = DemoImages.createChessboardImage(320, 320);
        ImageFrame frame;
        frame = new ImageFrame(image, "Regionalize demo: test image");
        frame.setVisible(true);

        regionalizeImage(image);
    }


    /**
     * Regionalizes the test chessboard image in two ways:
     * firstly with only orthogonal connectedness; then
     * allowing diagonal connectedness. Displays the results
     * of each regionalization in an {@linkplain ImageFrame}.
     *
     * @param image the test image
     */
    public void regionalizeImage(RenderedImage image) {

        ImageFrame frame;

        /*
         * Regionalize the source chessboard image,
         * specifying orthogonal connectedness by setting the
         * diagonal parameter to false
         */
        ParameterBlockJAI pb = new ParameterBlockJAI("regionalize");
        pb.setSource("source0", image);
        pb.setParameter("diagonal", false);
        RenderedOp orthoImg = JAI.create("Regionalize", pb);

        /*
         * At present, we have to force JAI to render the image
         * before we can access the region data in the image
         * properties. Calling getAsBufferedImage() accomplishes
         * this.
         *
         * @todo remove this necessity
         */
        orthoImg.getData();

        /*
         * Get summary data for regions and print it to the console
         */
        List<Region> regions = (List<Region>) orthoImg.getProperty(RegionalizeDescriptor.REGION_DATA_PROPERTY);
        for (Region r : regions) {
            System.out.println(r);
        }
        
        /*
         * We use an ImageUtils method to make a nice colour image
         * of the regions to display
         */
        Color[] colors = ImageUtils.createRampColours(regions.size());
        Map<Integer, Color> colorMap = CollectionFactory.map();
        int k = 0;
        for (Region r : regions) {
            colorMap.put(r.getId(), colors[k++]);
        }
        
        RenderedImage displayImg = ImageUtils.createDisplayImage(orthoImg, colorMap);
        frame = new ImageFrame(displayImg, orthoImg, "Regions with orthogonal connection");
        frame.setVisible(true);

        /*
         * Repeat the regionalization of the source image
         * allowing diagonal connections within regions
         */

        pb = new ParameterBlockJAI("regionalize");
        pb.setSource("source0", image);
        pb.setParameter("diagonal", true);
        RenderedOp diagImg = JAI.create("regionalize", pb);

        colorMap.clear();
        colorMap.put(1, Color.CYAN);
        colorMap.put(2, Color.ORANGE);
        RenderedImage diagDisplayImg = ImageUtils.createDisplayImage(diagImg, colorMap);

        frame = new ImageFrame(diagDisplayImg, diagImg, "Regions with diagonal connection");
        frame.setVisible(true);
    }

}
