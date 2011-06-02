/*
 * Copyright 2010 Michael Bedward
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

package jaitools.media.jai.vectorbinarize;


import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.Arrays;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.RasterFactory;
import javax.media.jai.SourcelessOpImage;

import com.vividsolutions.jts.awt.ShapeWriter;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;

import jaitools.jts.CoordinateSequence2D;


/**
 * Creates a binary image based on tests of pixel inclusion in a polygonal {@code Geometry}. See
 * {@link VectorBinarizeDescriptor} for details.
 * 
 * @author Michael Bedward
 * @author Andrea Aime
 * @since 1.1
 * @version $Id$
 */
public class VectorBinarizeOpImage extends SourcelessOpImage {
    
    private final PreparedGeometry geom;
    
    private final Shape shape;

    private final CoordinateSequence2D testPointCS;

    private final Point testPoint;

    private final CoordinateSequence2D testRectCS;

    private final Polygon testRect;

    private Raster solidTile;
    
    private Raster blankTile;
    
    static boolean DEFAULT_ANTIALIASING = false;
    
    private boolean antiAliasing = DEFAULT_ANTIALIASING; 

    /**
     * Constructor.
     * 
     * @param sm the {@code SampleModel} used to create tiles
     * @param configuration rendering hints
     * @param minX origin X ordinate
     * @param minY origin Y ordinate
     * @param width image width
     * @param height image height
     * @param geom reference polygonal geometry
     * @param coordType type of coordinates to use when testing pixel inclusion
     *        (corner or center)
     */
    public VectorBinarizeOpImage(SampleModel sm, Map configuration, int minX, int minY, int width,
            int height, PreparedGeometry geom, boolean antiAliasing) {
        super(buildLayout(minX, minY, width, height, sm), configuration, sm, minX, minY, width,
                height);

        this.geom = geom;
        this.shape = new ShapeWriter().toShape(geom.getGeometry());
        this.antiAliasing = antiAliasing;

        GeometryFactory gf = new GeometryFactory();
        testPointCS = new CoordinateSequence2D(1);
        testPoint = gf.createPoint(testPointCS);

        testRectCS = new CoordinateSequence2D(5);
        testRect = gf.createPolygon(gf.createLinearRing(testRectCS), null);
    }

    /**
     * Builds an {@code ImageLayout} for this image. The {@code width} and
     * {@code height} arguments are requested tile dimensions which will 
     * only be used if they are smaller than this operator's default
     * tile dimension.
     * 
     * @param minX origin X ordinate
     * @param minY origin Y ordinate
     * @param width requested tile width
     * @param height requested tile height
     * @param sm sample model
     * 
     * @return the {@code ImageLayout} object
     */
    static ImageLayout buildLayout(int minX, int minY, int width, int height, SampleModel sm) {
        // build a sample model for the single tile
        ImageLayout il = new ImageLayout();
        il.setMinX(minX);
        il.setMinY(minY);
        il.setWidth(width);
        il.setHeight(height);
        il.setSampleModel(sm);

        if (!il.isValid(ImageLayout.TILE_GRID_X_OFFSET_MASK)) {
            il.setTileGridXOffset(il.getMinX(null));
        }
        if (!il.isValid(ImageLayout.TILE_GRID_Y_OFFSET_MASK)) {
            il.setTileGridYOffset(il.getMinY(null));
        }

        return il;
    }

    /**
     * Returns the specified tile.
     * 
     * @param tileX tile X index
     * @param tileY tile Y index
     * 
     * @return the requested tile
     */
    @Override
    public Raster computeTile(int tileX, int tileY) {
        final int x = tileXToX(tileX);
        final int y = tileYToY(tileY);
        
        // get the raster tile
        Raster tile = getTileRaster(x, y);
        
        // create a read only child in the right location
        Raster result = tile.createChild(0, 0, tileWidth, tileHeight, x, y, null);
        return result;
    }

    /**
     * Gets the data for the requested tile. If the tile is either completely
     * within or outside of the reference {@code PreparedGeometry} a cached
     * constant {@code Raster} with 1 or 0 values is returned. Otherwise
     * tile pixels are checked for inclusion and set individually.
     * 
     * @param minX origin X ordinate
     * @param minY origin Y ordinate
     * 
     * @return the requested tile
     */
    protected Raster getTileRaster(int minX, int minY) {
        // check relationship between geometry and the tile we're computing
        updateTestRect(minX, minY);
        if (geom.contains(testRect)) {
            return getSolidTile();
        } else if (geom.disjoint(testRect)) {
            return getBlankTile();
        } else {
            // use java2d to quickly binarize the geometry
            WritableRaster raster = RasterFactory.createWritableRaster(
                    sampleModel, new java.awt.Point(0, 0));
            BufferedImage bi = new BufferedImage(colorModel, raster, false, null);
            Graphics2D graphics = null;
            try {
                graphics = bi.createGraphics();
                
                // translate the geometry to compensate for the tile origin at 0,0
                graphics.setTransform(AffineTransform.getTranslateInstance(-minX, -minY));
                
                if (antiAliasing){
                    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                }
                
                // draw the shape
                graphics.setColor(Color.WHITE);
                graphics.fill(shape);
            } finally {
                if(graphics != null) {
                    graphics.dispose();
                }
            }

            return raster;
        }
    }

    /**
     * Returns (creating and caching if the first call) a constant tile with 1 values
     * 
     * @return the constant tile
     */
    private Raster getSolidTile() {
        if (solidTile == null) {
            solidTile = constantTile(1);
        }
        return solidTile;
    }
    
    /**
     * Returns (creating and caching if the first call) a constant tile with 0 values
     * 
     * @return the constant tile
     */
    private Raster getBlankTile() {
        if (blankTile == null) {
            blankTile = constantTile(0);
        }
        return blankTile;
    }

    /**
     * Builds a tile with constant value
     * 
     * @param value the constant value
     * 
     * @return the new tile
     */
    private Raster constantTile(int value) {
        // build the raster
        WritableRaster raster = RasterFactory.createWritableRaster(
                sampleModel, new java.awt.Point(0, 0));

        // sanity checks
        int dataType = sampleModel.getTransferType();
        int numBands = sampleModel.getNumBands();
        if(dataType != DataBuffer.TYPE_BYTE) {
            throw new IllegalArgumentException("The code works only if the sample model data type is BYTE");
        } 
        if(numBands != 1) {
            throw new IllegalArgumentException("The code works only for single band rasters!");
        }
        
        // flood fill
        int w = sampleModel.getWidth();
        int h = sampleModel.getHeight();
        int[] data = new int[w * h];
        Arrays.fill(data, value);
        raster.setSamples(0, 0, w, h, 0, data);
        
        return raster;
    }

    /**
     * Updates the bounds of the rectangle used to test inclusion in the 
     * reference {@code PreparedGeometry}.
     * 
     * @param x origin X ordinate
     * @param y origin Y ordinate
     */
    private void updateTestRect(int x, int y) {
        testRectCS.setXY(0, x, y);
        testRectCS.setXY(1, x, y + tileHeight);
        testRectCS.setXY(2, x + tileWidth, y + tileHeight);
        testRectCS.setXY(3, x + tileWidth, y);
        testRectCS.setXY(4, x, y);
        testRect.geometryChanged();
    }
}
