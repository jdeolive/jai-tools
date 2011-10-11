/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
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
package org.jaitools.imageutils.shape;


import java.awt.geom.AffineTransform;

import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence;
import com.vividsolutions.jts.geom.impl.PackedCoordinateSequence.Double;


/**
 * A path iterator for the LiteShape class, specialized to iterate over
 * LineString object. This class was ported back and simplified from GeoTools, with permission from the author(s)
 *
 * @author Andrea Aime
 * @author simone giannecchini  
 */
public final class PackedLineIterator extends AbstractLiteIterator {
    /** Transform applied on the coordinates during iteration */
    private AffineTransform at;

    /** The array of coordinates that represents the line geometry */
    private PackedCoordinateSequence.Double coordinates = null;
    
    /** Current line coordinate */
    private int currentCoord = 0;

    /** True when the iteration is terminated */
    private boolean done = false;

    /** True if the line is a ring */
    private boolean isClosed;

    private int coordinateCount;

    /**
     * Creates a new instance of LineIterator
     *
     * @param ls The line string the iterator will use
     * @param at The affine transform applied to coordinates during iteration
     */
    public PackedLineIterator(LineString ls, AffineTransform at) {
        if (at == null) {
            at = new AffineTransform();
        }

        this.at = at;
        coordinates = (Double) ls.getCoordinateSequence();
        coordinateCount = coordinates.size();
        isClosed = ls instanceof LinearRing;
    }

    /**
     * Returns the coordinates and type of the current path segment in the
     * iteration. The return value is the path-segment type: SEG_MOVETO,
     * SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE. A double array of
     * length 6 must be passed in and can be used to store the coordinates of
     * the point(s). Each point is stored as a pair of double x,y coordinates.
     * SEG_MOVETO and SEG_LINETO types returns one point, SEG_QUADTO returns
     * two points, SEG_CUBICTO returns 3 points and SEG_CLOSE does not return
     * any points.
     *
     * @param coords an array that holds the data returned from this method
     *
     * @return the path-segment type of the current path segment.
     *
     * @see #SEG_MOVETO
     * @see #SEG_LINETO
     * @see #SEG_QUADTO
     * @see #SEG_CUBICTO
     * @see #SEG_CLOSE
     */
    public int currentSegment(float[] coords) {
        if (currentCoord == 0) {
            coords[0] = (float) coordinates.getX(0);
            coords[1] = (float) coordinates.getY(0);
            at.transform(coords, 0, coords, 0, 1);

            return SEG_MOVETO;
        } else if ((currentCoord == coordinateCount) && isClosed) {
            return SEG_CLOSE;
        } else {
            coords[0] = (float) coordinates.getX(currentCoord);
            coords[1] = (float) coordinates.getY(currentCoord);
            at.transform(coords, 0, coords, 0, 1);

            return SEG_LINETO;
        }
    }

    /**
     * Returns the winding rule for determining the interior of the path.
     *
     * @return the winding rule.
     *
     * @see #WIND_EVEN_ODD
     * @see #WIND_NON_ZERO
     */
    public int getWindingRule() {
        return WIND_NON_ZERO;
    }

    /**
     * Tests if the iteration is complete.
     *
     * @return <code>true</code> if all the segments have been read;
     *         <code>false</code> otherwise.
     */
    public boolean isDone() {
        return done;
    }

    /**
     * Moves the iterator to the next segment of the path forwards along the
     * primary direction of traversal as long as there are more points in that
     * direction.
     */
    public void next() {
		if (
            ((currentCoord == (coordinateCount - 1)) && !isClosed)
                || ((currentCoord == coordinateCount) && isClosed)) {
            done = true;
        } else {
            currentCoord++;
        }
    }

    /**
     * @see java.awt.geom.PathIterator#currentSegment(double[])
     */
    public int currentSegment(double[] coords) {
        System.out.println("Double!");
        return 0;
    }


}
