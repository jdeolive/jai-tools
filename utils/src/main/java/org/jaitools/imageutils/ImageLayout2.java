/* 
 *  Copyright (c) 2010-2011, Simone Giannecchini. All rights reserved. 
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

package org.jaitools.imageutils;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;

import javax.media.jai.ImageLayout;

/**
 * Extends the standard JAI {@link ImageLayout} to provide a reliable hash function. 
 * {@code ImageLayout} has a bug that will cause an application to crash if doing
 * hashing when some fields have not been initialized.
 * 
 * @see javax.media.jai.ImageLayout
 * 
 * @author Simone Giannecchini, GeoSolutions S.A.S.
 * @author Daniele Romagnoli, GeoSolutions S.A.S.
 * @since 1.1
 * @version $Id$
 */
public class ImageLayout2 extends ImageLayout{

    private static final long serialVersionUID = -7921590012423277029L;

    /** 
     * Default constructor. Constructs an {@link ImageLayout2} without any parameter set.
     */
    public ImageLayout2() {
        super();
    }

    /**
     * Construct an {@link ImageLayout2} with the parameter set.
     * @param minX
     *          the image's minimum X coordinate.
     * @param minY
     *          the image's minimum X coordinate.
     * @param width
     *          the image's width.
     * @param height
     *          the image's height.
     * @param tileGridXOffset
     *          the x coordinate of the tile (0,0)
     * @param tileGridYOffset
     *          the y coordinate of the tile (0,0)
     * @param tileWidth
     *          the tile's width.
     * @param tileHeight
     *          the tile's height.
     * @param sampleModel
     *          the image's {@link SampleModel}
     * @param colorModel
     *          the image's {@link ColorModel}
     */
    public ImageLayout2(
            final int minX, 
            final int minY, 
            final int width, 
            final int height, 
            final int tileGridXOffset,
            final int tileGridYOffset, 
            final int tileWidth, 
            final int tileHeight, 
            final SampleModel sampleModel,
            final ColorModel colorModel) {
        super(minX, minY, width, height, tileGridXOffset, tileGridYOffset, tileWidth, tileHeight,
                sampleModel, colorModel);
    }

    /**
     * Construct an {@link ImageLayout2} with only tiling layout properties, sampleModel and 
     * colorModel set.
     * @param tileGridXOffset
     *          the x coordinate of the tile (0,0)
     * @param tileGridYOffset
     *          the y coordinate of the tile (0,0)
     * @param tileWidth
     *          the tile's width.
     * @param tileHeight
     *          the tile's height.
     * @param sampleModel
     *          the image's {@link SampleModel}
     * @param colorModel
     *          the image's {@link ColorModel}
     */
    public ImageLayout2(
            final int tileGridXOffset, 
            final int tileGridYOffset, 
            final int tileWidth, 
            final int tileHeight,
            final SampleModel sampleModel, 
            final ColorModel colorModel) {
        super(tileGridXOffset, tileGridYOffset, tileWidth, tileHeight, sampleModel, colorModel);
    }

    /**
     * Construct an {@link ImageLayout2} with only the image's properties set.
     * @param minX
     *          the image's minimum X coordinate.
     * @param minY
     *          the image's minimum X coordinate.
     * @param width
     *          the image's width.
     * @param height
     *          the image's height.
     */
    public ImageLayout2(
            final int minX, 
            final int minY, 
            final int width, 
            final int height) {
        super(minX, minY, width, height);
    }

    /**
     * Construct an {@link ImageLayout2} on top of a RenderedImage. The layout parameters are set
     * from the related values of the input image.
     * @param im a {@link RenderedImage} whose layout will be copied.
     */
    public ImageLayout2(RenderedImage im) {
        super(im);
    }

    /**
     * Returns the hash code for this {@link ImageLayout2}.
     * With respect to the super {@link ImageLayout}, this method also does 
     * validity check on the parameters during hashing.
     */
    @Override
    public int hashCode() {
        int code = 0, i = 1;

        if (isValid(ImageLayout2.WIDTH_MASK)){
            code += (getWidth(null) * i++);
        }

        if (isValid(ImageLayout2.HEIGHT_MASK)){
            code += (getHeight(null) * i++);
        }

        if (isValid(ImageLayout2.MIN_X_MASK)){
            code += (getMinX(null) * i++);
        }

        if (isValid(ImageLayout2.MIN_Y_MASK)){
            code += (getMinY(null) * i++);
        }

        if (isValid(ImageLayout2.TILE_HEIGHT_MASK)){
            code += (getTileHeight(null) * i++);
        }

        if (isValid(ImageLayout2.TILE_WIDTH_MASK)){
            code += (getTileWidth(null) * i++);
        }

        if (isValid(ImageLayout2.TILE_GRID_X_OFFSET_MASK)){
            code += (getTileGridXOffset(null) * i++);
        }

        if (isValid(ImageLayout2.TILE_GRID_Y_OFFSET_MASK)){
            code += (getTileGridYOffset(null) * i++);
        }

        if (isValid(ImageLayout2.SAMPLE_MODEL_MASK)){
            code ^= getSampleModel(null).hashCode();
        }

        code ^= validMask;

        if (isValid(ImageLayout2.COLOR_MODEL_MASK))
            code ^= getColorModel(null).hashCode();

        return code;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (obj == this) {
            return true;
        }

        if (!ImageLayout.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        
        ImageLayout other = (ImageLayout) obj;
        
        if (getValidMask() != other.getValidMask()) {
            return false;
        }
        if (getWidth(null) != other.getWidth(null)) {
            return false;
        }
        if (getHeight(null) != other.getHeight(null)) {
            return false;
        }
        if (getMinX(null) != other.getMinX(null)) {
            return false;
        }
        if (getMinY(null) != other.getMinY(null)) {
            return false;
        }
        if (getTileWidth(null) != other.getTileWidth(null)) {
            return false;
        }
        if (getTileHeight(null) != other.getTileHeight(null)) {
            return false;
        }
        if (getTileGridXOffset(null) != other.getTileGridXOffset(null)) {
            return false;
        }
        if (getTileGridYOffset(null) != other.getTileGridYOffset(null)) {
            return false;
        }
        
        SampleModel sm = getSampleModel(null);
        if (sm == null) {
            if (other.getSampleModel(null) != null) {
                return false;
            }
        } else {
            if (!sm.equals(other.getSampleModel(null))) {
                return false;
            }
        }

        ColorModel cm = getColorModel(null);
        if (cm == null) {
            if (other.getColorModel(null) != null) {
                return false;
            }
        } else {
            if (!cm.equals(other.getColorModel(null))) {
                return false;
            }
        }

        return true;
    }

}
