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
import java.awt.geom.PathIterator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


/**
 * A path iterator for the LiteShape class, specialized to iterate over a
 * geometry collection. It can be seen as a composite, since uses in fact
 * other, simpler iterator to carry on its duties.
 * This class was ported back and simplified from GeoTools, with permission from the author(s)
 *
 * @author Andrea Aime
 */
public final class GeomCollectionIterator extends AbstractLiteIterator {
    /** Transform applied on the coordinates during iteration */
    private AffineTransform at;

    /** The set of geometries that we will iterate over */
    private GeometryCollection gc;
    
    /** The current geometry */
    private int currentGeom;

    /** The current sub-iterator */
    private PathIterator currentIterator;

    /** True when the iterator is terminate */
    private boolean done = false;

    public GeomCollectionIterator(){
    	
    }
    
    /**
     * @param gc
     * @param at
     */
    public void init(GeometryCollection gc, AffineTransform at) {
        this.gc = gc;
        this.at = at == null ? new AffineTransform() : at;
        currentGeom = 0;
        done = false;
        currentIterator = gc.isEmpty() ? EmptyIterator.INSTANCE : getIterator(gc.getGeometryN(0));
    }

    /**
     * Creates a new instance of GeomCollectionIterator
     *
     * @param gc The geometry collection the iterator will use
     * @param at The affine transform applied to coordinates during iteration
     *        distance from the previous is less than maxDistance
     */
    public GeomCollectionIterator(GeometryCollection gc, AffineTransform at) {
        init(gc, at);
    }

    /**
     * Returns the specific iterator for the geometry passed.
     *
     * @param g The geometry whole iterator is requested
     *
     * @return the specific iterator for the geometry passed.
     */
    private AbstractLiteIterator getIterator(Geometry g) {
        AbstractLiteIterator pi = null;

        if (g.isEmpty())
            return EmptyIterator.INSTANCE;
        if (g instanceof Polygon) {
            Polygon p = (Polygon) g;
            pi = new PolygonIterator(p, at);
        } else if (g instanceof GeometryCollection) {
            GeometryCollection gc = (GeometryCollection) g;
            pi = new GeomCollectionIterator(gc, at);
        } else if (g instanceof LineString || g instanceof LinearRing) {
            LineString ls = (LineString) g;
            pi = new LineIterator(ls, at);
        } else if (g instanceof Point) {
            Point p = (Point) g;
            pi = new PointIterator(p, at);
        }
        
        return pi;
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
    public int currentSegment(double[] coords) {
        return currentIterator.currentSegment(coords);
    }

    /**
     * Returns the coordinates and type of the current path segment in the
     * iteration. The return value is the path-segment type: SEG_MOVETO,
     * SEG_LINETO, SEG_QUADTO, SEG_CUBICTO, or SEG_CLOSE. A float array of
     * length 6 must be passed in and can be used to store the coordinates of
     * the point(s). Each point is stored as a pair of float x,y coordinates.
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
        return currentIterator.currentSegment(coords);
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
        // try to move the current iterator forward
        if(!currentIterator.isDone()) {
            currentIterator.next();
        }
        // if the iterator is finished, let's move to the next one (and if
        // the next one, should the next one be empty)
        while(currentIterator.isDone() && !done) {
            if (currentGeom < (gc.getNumGeometries() - 1)) {
                currentGeom++;
                currentIterator = getIterator(gc.getGeometryN(currentGeom));
            } else {
                done = true;
            }
        } 
    }

}
