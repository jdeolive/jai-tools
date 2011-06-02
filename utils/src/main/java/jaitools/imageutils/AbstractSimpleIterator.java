/*
 * Copyright 2011 Michael Bedward
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

package jaitools.imageutils;

import jaitools.CollectionFactory;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.lang.ref.WeakReference;
import java.util.List;

import javax.media.jai.iterator.RectIter;


/**
 * Base class for image iterators with row-column (line-pixel) movement.
 * 
 * @author michael
 */
public abstract class AbstractSimpleIterator {

    /** 
     * Constants defining the visiting order that the iterator will
     * follow when moved with the {@code next()} method. Choices are:
     * <ul>
     * <li>{@linkplain Order#IMAGE_X_Y}
     * <li>{@linkplain Order#TILE_X_Y}
     * </ul>
     */
    public static enum Order {
        /** 
         * The iterator will move by X (pixel) then Y (line) across
         * the whole image.
         */
        IMAGE_X_Y,

        /** 
         * The iterator will move by X (pixel) then Y (line) within
         * each tile of the image in turn. The tiles are visited in 
         * X (tile grid column) then Y (tile grid row) order. This
         * movement order is most efficient when dealing with large
         * images because it minimizes tile-swapping in memory.
         */ 
        TILE_X_Y;
    }

    /**
     * This is implemented by sub-classes to pass a method back to
     * this class to create the delegate iterator. This allows the
     * delegate to be a final field.
     */
    protected static interface DelegateHelper {
        RectIter create(RenderedImage image, Rectangle bounds);
    }
    
    
    /** A weak reference to the target image. */
    protected final WeakReference<RenderedImage> imageRef;

    /** The data type of the target image (value of a DataBuffer constant). */
    protected final int imageDataType;

    /** The bounds of this iterator */
    protected final Rectangle iterBounds;

    /** The delegate iterator. */
    protected final RectIter delegateIter;


    // number of bands in the target image
    private final int numImageBands;
    
    // visiting order for this iterator
    private final Order order;

    // the value to return when the iterator is positioned beyond
    // the bounds of the target image; three types are created to
    // save time a little in the getSample method
    private final Integer outsideValue_Integer;
    private final Float outsideValue_Float;
    private final Double outsideValue_Double;

    // list of sub-bounds (a single rectangle for image-wise iteration or
    // a series of tile portions for tile-wise iteration)
    private final List<Rectangle> subBoundList;

    // index of the current sub-bound being processed
    private int   currentSubBound;

    // index of the final sub-bound to process
    private final int lastSubBound;
    
    // start position in the current sub-bounds
    private final Point startSubPos;

    // end position in the current sub-bounds
    private final Point endSubPos;

    // this iterator's current position (will differ from delegate
    // iterator's position when outside target image bounds)
    private final Point mainPos;

    // bounds of the delegate iterator (intersection of iterBounds
    // and the bounds of the target image)
    private final Rectangle delegateBounds;

    // the current delegate position
    private final Point delegatePos;

    
    /**
     * Creates a new instance. The helper object is provided by a sub-class 
     * to create the delegate iterator that will then be held by this class as
     * a final field. The iterator bounds are allowed to extend beyond the 
     * target image bounds. When the iterator is positioned outside the target
     * image area it returns the specified outside value.
     * 
     * @param helper a helper provided by sub-class to create the delegate iterator
     * @param image the target image
     * @param bounds the bounds for this iterator; {@code null} means to use the
     *     target image bounds
     * @param outsideValue value to return when positioned outside the bounds of
     *     the target image
     * @param order order of movement for this iterator
     * 
     * @throws IllegalArgumentException if the image argument is {@code null}
     */
    public AbstractSimpleIterator(DelegateHelper helper, RenderedImage image, 
            Rectangle bounds, Number outsideValue, Order order) {
        
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        if (order == null) {
            throw new IllegalArgumentException("order must not be null");
        }
        
        imageRef = new WeakReference<RenderedImage>(image);
        imageDataType = image.getSampleModel().getDataType();
        numImageBands = image.getSampleModel().getNumBands();
        
        final Rectangle imageBounds = new Rectangle(image.getMinX(), image.getMinY(),
                image.getWidth(), image.getHeight());
        
        if (bounds == null) {
            iterBounds = imageBounds;
        } else {
            iterBounds = new Rectangle(bounds);
        }

        delegateBounds = imageBounds.intersection(iterBounds);
        if (delegateBounds.isEmpty()) {
            delegatePos = null;
            delegateIter = null;
            
        } else {
            delegatePos = new Point(delegateBounds.x, delegateBounds.y);
            delegateIter = helper.create(image, delegateBounds);
        }
        
        mainPos = new Point(iterBounds.x, iterBounds.y);
        
        this.outsideValue_Integer = outsideValue == null ? null : outsideValue.intValue();
        this.outsideValue_Float = outsideValue == null ? null : outsideValue.floatValue();
        this.outsideValue_Double = outsideValue == null ? null : outsideValue.doubleValue();
        
        this.order = order;
        this.startSubPos = new Point();
        this.endSubPos = new Point();
        subBoundList = buildSubBoundList(image);
        lastSubBound = subBoundList.size() - 1;

        setCurrentSubBound(0);
    }

    /**
     * Returns the image that this iterator is working with. Note
     * that the iterator only maintains a {@linkplain WeakReference} to
     * the image so it is possible for this method to return {@code null}.
     * 
     * @return the image that this iterator is working with
     */
    public RenderedImage getImage() {
        return imageRef.get();
    }

    /**
     * Gets the bounds of this iterator. Note that these may extend
     * beyond the bounds of the target image.
     * 
     * @return the iterator bounds
     */
    public Rectangle getBounds() {
        return new Rectangle(iterBounds);
    }

    /**
     * Gets the starting position for this iterator. This is the upper-left
     * point of the iterator's bounding rectangle. Note that it may lie
     * outside the target image.
     * 
     * @return the starting position
     */
    public Point getStartPos() {
        return new Point(iterBounds.x, iterBounds.y);
    }

    /**
     * Gets the final position for this iterator. This is the lower-right
     * point of the iterator's bounding rectangle. Note that it may lie
     * outside the target image.
     * 
     * @return the end position
     */
    public Point getEndPos() {
        return new Point(iterBounds.x + iterBounds.width - 1, iterBounds.y + iterBounds.height - 1);
    }

    /**
     * Tests whether the iterator is currently positioned within the bounds of 
     * the target image.
     * 
     * @return {@code true} if within the target image; {@code false} otherwise
     */
    public boolean isWithinImage() {
        return delegatePos != null && mainPos.equals(delegatePos);
    }

    /**
     * Tests if this iterator can be advanced further, ie. if a call to 
     * {@link #next()} would return {@code true}.
     * 
     * @return {@code true} if the iterator can be advanced; 
     *     {@code false} if it is at the end of its bounds
     */
    public boolean hasNext() {
        return (currentSubBound < lastSubBound || mainPos.x < endSubPos.x || mainPos.y < endSubPos.y);
    }

    /**
     * Advances the iterator to the next position. The iterator moves by
     * column (pixel), then row (line). It is always safe to call this
     * method speculatively.
     * 
     * @return {@code true} if the iterator was successfully advanced;
     *     {@code false} if it was already at the end of its bounds
     */
    public boolean next() {
        if (hasNext()) {
            mainPos.x++ ;
            if (mainPos.x > endSubPos.x) {
                mainPos.x = startSubPos.x;
                mainPos.y++ ;
                
                if (mainPos.y > endSubPos.y) {
                    setCurrentSubBound(currentSubBound + 1);
                    mainPos.setLocation(startSubPos);
                }
            }
            setDelegatePosition();
            return true;
        }

        return false;
    }

    /**
     * Resets the iterator to its first position.
     */
    public void reset() {
        setPos(iterBounds.x, iterBounds.y);
    }

    /**
     * Gets the current iterator position. It is always safe to call
     * this method.
     * 
     * @return current position
     */
    public Point getPos() {
        return new Point(mainPos);
    }

    /**
     * Sets the iterator to a new position. Note that a return value of
     * {@code true} indicates that the new position was valid. If the new position
     * is equal to the iterator's current position this method will still
     * return {@code true} even though the iterator has not moved.
     * <p>
     * If the new position is outside this iterator's bounds, the position remains
     * unchanged and {@code false} is returned.
     * 
     * @param pos the new position
     * @return {@code true} if the new position was valid; {@code false}
     *     otherwise
     * 
     * @throws IllegalArgumentException if {@code pos} is {@code null}
     */
    public boolean setPos(Point pos) {
        if (pos == null) {
            throw new IllegalArgumentException("pos must not be null");
        }
        return setPos(pos.x, pos.y);
    }

    /**
     * Sets the iterator to a new position. Note that a return value of
     * {@code true} indicates that the new position was valid. If the new position
     * is equal to the iterator's current position this method will still
     * return {@code true} even though the iterator has not moved.
     * <p>
     * If the new position is outside this iterator's bounds, the position remains
     * unchanged and {@code false} is returned.
     * 
     * @param x image X position
     * @param y image Y position
     * @return {@code true} if the new position was valid; {@code false}
     *     otherwise
     */
    public boolean setPos(int x, int y) {
        if (iterBounds.contains(x, y)) {
            int index = findSubBound(x, y);
            setCurrentSubBound(index);
            mainPos.setLocation(x, y);
            setDelegatePosition();
            return true;
        }
        
        return false;
    }

    /**
     * Returns the value from the first band of the image at the current position,
     * or the outside value if the iterator is positioned beyond the image bounds.
     * 
     * @return image or outside value
     */
    public Number getSample() {
        return getSample(0);
    }

    /**
     * Returns the value from the first band of the image at the specified position,
     * If the position is within the iterator's bounds, but outside the target
     * image bounds, the outside value is returned. After calling this method, the
     * iterator will be set to the specified position.
     * <p>
     * If the position is outside the iterator's bounds, {@code null} is returned
     * and the iterator's position will remain unchanged.
     * 
     * @param pos the position to sample
     * @return image, outside value or {@code null}
     * 
     * @throws IllegalArgumentException if {@code pos} is {@code null}
     */
    public Number getSample(Point pos) {
        if (pos == null) {
            throw new IllegalArgumentException("pos must not be null");
        }
        return getSample(pos.x, pos.y);
    }

    /**
     * Returns the value from the first band of the image at the specified position,
     * If the position is within the iterator's bounds, but outside the target
     * image bounds, the outside value is returned. After calling this method, the
     * iterator will be set to the specified position.
     * <p>
     * If the position is outside the iterator's bounds, {@code null} is returned
     * and the iterator's position will remain unchanged.
     * 
     * @param x sampling position X-ordinate
     * @param y sampling position Y-ordinate
     * @return image, outside value or {@code null}
     */
    public Number getSample(int x, int y) {
        if (setPos(x, y)) {
            return getSample();
        } else {
            return null;
        }
    }

    /**
     * Returns the value from the specified band of the image at the current position,
     * or the outside value if the iterator is positioned beyond the image bounds.
     * 
     * @param band image band
     * @return image or outside value
     * @throws IllegalArgumentException if {@code band} is out of range for the the
     *     target image
     */
    public Number getSample(int band) {
        RenderedImage image = imageRef.get();
        if (image == null) {
            throw new IllegalStateException("Target image has been deleted");
        }

        final boolean inside = delegateBounds.contains(mainPos);
        Number value;

        switch (imageDataType) {
            case DataBuffer.TYPE_DOUBLE:
                if (inside) {
                    value = new Double(delegateIter.getSampleDouble(band));
                } else {
                    value = outsideValue_Double;
                }
                break;

            case DataBuffer.TYPE_FLOAT:
                if (inside) {
                    value = new Float(delegateIter.getSampleFloat(band));
                } else {
                    value = outsideValue_Float;
                }
                break;

            default:
                if (inside) {
                    value = Integer.valueOf(delegateIter.getSample(band));
                } else {
                    value = outsideValue_Integer;
                }
        }

        return value;
    }

    /**
     * Returns the value from the specified band of the image at the specified position,
     * If the position is within the iterator's bounds, but outside the target
     * image bounds, the outside value is returned. After calling this method, the
     * iterator will be set to the specified position.
     * <p>
     * If the position is outside the iterator's bounds, {@code null} is returned
     * and the iterator's position will remain unchanged.
     * 
     * @param pos the position to sample
     * @param band image band
     * @return image, outside value or {@code null}
     * 
     * @throws IllegalArgumentException if {@code pos} is {@code null} or
     *     {@code band} is out of range
     */
    public Number getSample(Point pos, int band) {
        if (pos == null) {
            throw new IllegalArgumentException("pos must not be null");
        }
        return getSample(pos.x, pos.y, band);
    }

    /**
     * Returns the value from the specified band of the image at the specified position,
     * If the position is within the iterator's bounds, but outside the target
     * image bounds, the outside value is returned. After calling this method, the
     * iterator will be set to the specified position.
     * <p>
     * If the position is outside the iterator's bounds, {@code null} is returned
     * and the iterator's position will remain unchanged.
     * 
     * @param x sampling position X-ordinate
     * @param y sampling position Y-ordinate
     * @param band image band
     * @return image, outside value or {@code null}
     * 
     * @throws IllegalArgumentException if {@code band} is out of range
     */
    public Number getSample(int x, int y, int band) {
        if (setPos(x, y)) {
            return getSample(band);
        } else {
            return null;
        }
    }

    /**
     * Sets the delegate iterator position. If {@code newPos} is outside
     * the target image bounds, the delegate iterator does not move.
     */
    protected void setDelegatePosition() {
        if (isInsideDelegateBounds()) {
            int dy = mainPos.y - delegatePos.y;
            if (dy < 0) {
                delegateIter.startLines();
                delegatePos.y = delegateBounds.y;
                dy = mainPos.y - delegateBounds.y;
            }

            while (dy > 0) {
                delegateIter.nextLineDone();
                delegatePos.y++ ;
                dy--;
            }

            int dx = mainPos.x - delegatePos.x;
            if (dx < 0) {
                delegateIter.startPixels();
                delegatePos.x = delegateBounds.x;
                dx = mainPos.x - delegateBounds.x;
            }

            while (dx > 0) {
                delegateIter.nextPixelDone();
                delegatePos.x++ ;
                dx--;
            }
        }
    }

    /**
     * Tests if the iterator is currently positioned inside the delegate
     * iterator's area (which must lie within the image bounds).
     * 
     * @return {@code true} if the current position is inside the delegate's bounds;
     *     {@code false} otherwise
     */
    protected boolean isInsideDelegateBounds() {
        return delegateIter != null && delegateBounds.contains(mainPos);
    }

    /**
     * Helper method to check that a band value is valid.
     * 
     * @param band band value
     */
    protected void checkBandArg(int band) {
        if (band < 0 || band >= numImageBands) {
            throw new IllegalArgumentException( String.format(
                    "band argument (%d) is out of range: number of image bands is %d",
                    band, numImageBands) );
        }
    }

    /**
     * Builds the list of sub-bounds, each of which is a Rectangle to (possibly)
     * be processed by this iterator.
     * 
     * @param image the target image
     * @return the list of sub-bounds
     */
    private List<Rectangle> buildSubBoundList(RenderedImage image) {
        List<Rectangle> boundsList = CollectionFactory.list();
        
        switch (order) {
            case IMAGE_X_Y:
                boundsList.add(iterBounds);
                break;
                
            case TILE_X_Y:
                getIntersectingTileBounds(image, boundsList);
                break;
                
            default:
                throw new IllegalArgumentException("Unrecognized iterator order: " + order);
        }

        return boundsList;
    }

    /**
     * Sets the sub-bound that the iterator is to process next.
     * 
     * @param index position in the list of sub-bounds
     */
    private void setCurrentSubBound(int index) {
        Rectangle r = subBoundList.get(index);
        startSubPos.setLocation(r.x, r.y);
        endSubPos.setLocation(r.x + r.width - 1, r.y + r.height - 1);
        currentSubBound = index;
    }

    /**
     * Finds the iterator sub-bound which contains the given pixel position.
     * Note: the position might lie outside the image bounds.
     * 
     * @param x pixel X ordinate
     * @param y pixel Y ordinate
     * @return the corresponding sub-bound or -1 if the position is outside
     *     any sub-bound
     */
    private int findSubBound(int x, int y) {
        for (int i = 0; i < subBoundList.size(); i++) {
            if (subBoundList.get(i).contains(x, y)) {
                return i;
            }
        }

        return -1;
    }
    
    /**
     * Builds a list of Rectangles, each of which is the intersection of the iterator
     * bounds and a tile's bounds. These are termed sub-bounds.
     * <p>
     * If the iterator bounds extend beyond the iamge bounds some of the resulting 
     * rectangles may be for non-existent tiles.
     * 
     * @param image the target image
     * @param destList the list to receive the sub-bounds
     */
    private void getIntersectingTileBounds(RenderedImage image, List<Rectangle> destList) {
        final int tw = image.getTileWidth();
        final int th = image.getTileHeight();
        final int ox = image.getTileGridXOffset();
        final int oy = image.getTileGridYOffset();

        final int lastX = iterBounds.x + iterBounds.width - 1;
        final int lastY = iterBounds.y + iterBounds.height - 1;

        boolean moreY = true;
        for (int y = iterBounds.y; moreY; y = Math.min(y + th, lastY)) {
            moreY = y < lastY;
            
            boolean moreX = true;
            for (int x = iterBounds.x; moreX; x = Math.min(x + tw, lastX)) {
                moreX = x < lastX;

                // Get tile X and Y ordinates. Remember that the tile might 
                // not exist in the image if the iterator's bounds lie beyond the
                // image bounds, but we allow this.
                int tileX = pixelToTileOrdinate(x, ox, tw);
                int tileY = pixelToTileOrdinate(y, oy, th);

                Rectangle tileRect = new Rectangle(
                        tileX * tw + ox, tileY * th + oy, tw, th);

                Rectangle within = tileRect.intersection(iterBounds);

                if (!within.isEmpty()) {
                    destList.add(within);
                }
            }
        }
    }

    /**
     * Converts a pixel X or Y ordinate to the corresponding tile X or Y ordinate.
     * 
     * @param ordinate the pixel ordinate
     * @param offset the pixel offset (origin tile's upper-left pixel ordinate)
     * @param dim tile width (for X ordinate) or height (for Y ordinate)
     * @return tile ordinate
     */
    private int pixelToTileOrdinate(int ordinate, int offset, int dim) {
        ordinate -= offset;
        if (ordinate < 0) {
            ordinate += 1 - dim;
        }
        return ordinate / dim;
    }

}
