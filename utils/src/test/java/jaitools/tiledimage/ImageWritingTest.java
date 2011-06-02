/*
 * Copyright 2009 Michael Bedward
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
package jaitools.tiledimage;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;
import javax.media.jai.iterator.WritableRectIter;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests of DiskMemTilesImage: writing and retrieving data
 * at the image level
 *
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public class ImageWritingTest extends TiledImageTestBase {

    private static final int TILE_WIDTH = 128;
    private static final int XTILES = 2;
    private static final int YTILES = 3;

    private DiskMemImage image;

    @Before
    public void setup() {
        image = makeImage(TILE_WIDTH, XTILES, YTILES);
    }

    /**
     * Test setting and getting individual pixel / band values
     * as integers
     */
    @Test
    public void testPixelInt() {
        System.out.println("   setting and getting int values");

        Rectangle bounds = new Rectangle(TILE_WIDTH / 2, TILE_WIDTH / 2, TILE_WIDTH, TILE_WIDTH);
        int numBands = image.getNumBands();
        int numValues = 256 - numBands;

        int val = 0;
        for (int y = bounds.y, ny=0; ny < bounds.height; y++, ny++) {
            for (int x = bounds.x, nx = 0; nx < bounds.width; x++, nx++) {
                for (int band = 0; band < numBands; band++) {
                    image.setSample(x, y, band, val + band);
                }
                val = (val + 1) % numValues ;
            }
        }

        val = 0;
        for (int y = bounds.y, ny=0; ny < bounds.height; y++, ny++) {
            for (int x = bounds.x, nx = 0; nx < bounds.width; x++, nx++) {
                for (int band = 0; band < numBands; band++) {
                    int obs = image.getSample(x, y, band);
                    assertTrue(obs == (val + band));
                }
                val = (val + 1) % numValues ;
            }
        }

        /*
         * Out of bounds test
         */
        boolean gotException = false;
        try {
            image.getSample(image.getMinX() - 1, 0, 0);
        } catch (PixelOutsideImageException ex) {
            gotException = true;
        } catch (Exception ex) {
            fail("unexpected Exception type: " + ex.getClass().getSimpleName());
        }
        assertTrue(gotException);

        gotException = false;
        try {
            image.setSample(image.getMinX() - 1, 0, 0, 0);
        } catch (PixelOutsideImageException ex) {
            gotException = true;
        } catch (Exception ex) {
            fail("unexpected Exception type: " + ex.getClass().getSimpleName());
        }
        assertTrue(gotException);
    }

    /**
     * Test using JAI iterators to write and read image data
     */
    @Test
    public void testWriteIter() {
        System.out.println("   read/write using JAI iterators");

        WritableRectIter writeIter = RectIterFactory.createWritable(image, null);
        int i = 1;
        do {
            do {
                do {
                    writeIter.setSample(i);
                    i = (i % 31) + 1;
                } while (!writeIter.nextPixelDone());

                writeIter.startPixels();
            } while (!writeIter.nextLineDone());

            writeIter.startLines();
        } while (!writeIter.nextBandDone());


        RectIter readIter = RectIterFactory.create(image, null);
        i = 1;
        do {
            do {
                do {
                    assertTrue(readIter.getSample() == i);
                    i = (i % 31) + 1;
                } while (!readIter.nextPixelDone());

                readIter.startPixels();
            } while (!readIter.nextLineDone());

            readIter.startLines();
        } while (!readIter.nextBandDone());
    }


    /**
     * Test setting a rectangle of image data
     */
    @Test
    public void testSetData() {
        System.out.println("   setting a rectangle of image data");

        // create a raster that overlaps the image but extends partly
        // beyond it
        int minX = image.getMinX() + image.getWidth() / 2;
        int minY = image.getMinY() + image.getHeight() / 2;
        int maxX = image.getMaxX() + 10;
        int maxY = image.getMaxY() + 10;

        SampleModel sm = image.getSampleModel().createCompatibleSampleModel(maxX - minX + 1, maxY - minY + 1);
        WritableRaster data = Raster.createWritableRaster(sm, new Point(minX, minY));

        // write into the raster
        int numBands = sm.getNumBands();
        int value = 1;
        for (int b = 0; b < numBands; b++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    data.setSample(x, y, b, value);
                    value = (value % 31) + 1;
                }
            }
        }

        // copy the raster to the image
        image.setData(data);

        // read the image and check data values
        Rectangle bounds = image.getBounds();
        RandomIter iter = RandomIterFactory.create(image, null);
        value = 1;
        for (int b = 0; b < numBands; b++) {
            for (int y = minY; y <= maxY; y++) {
                for (int x = minX; x <= maxX; x++) {
                    if (bounds.contains(x, y)) {
                        assertTrue(iter.getSample(x, y, b) == value);
                    }
                    value = (value % 31) + 1;
                }
            }
        }
    }

}
