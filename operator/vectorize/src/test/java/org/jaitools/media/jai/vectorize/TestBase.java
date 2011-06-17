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

package org.jaitools.media.jai.vectorize;

import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.ParameterListDescriptor;
import javax.media.jai.RenderedOp;

import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.geom.Polygon;

import org.jaitools.numeric.NumberOperations;

import static org.junit.Assert.*;

/**
 * Based class for unit tests.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public abstract class TestBase {
    
    protected static final GeometryFactory gf = new GeometryFactory();
    protected static final WKTReader reader = new WKTReader(gf);
    
    /**
     * Helper function. Builds parameter block and runs the operation.
     * 
     * @param sourceImg source image
     * @param args optional {@code Map} of arguments
     * 
     * @return the destination image
     */
    protected RenderedOp doOp(RenderedImage sourceImg, Map<String, Object> args) {
        ParameterBlockJAI pb = new ParameterBlockJAI("Vectorize");
        pb.setSource("source0", sourceImg);
        
        if (args != null) {
            ParameterListDescriptor pld = pb.getParameterListDescriptor();
            for (String paramName : pld.getParamNames()) {
                Object obj = args.get(paramName);
                if (obj != null) {
                    pb.setParameter(paramName, obj);
                }
            }
        }

        return JAI.create("Vectorize", pb);
    }

    /**
     * Helper function. Gets the vectors property from a destination
     * image and checks the following:
     * <ol type="1">
     * <li> The property is a {@code Collection} </li>
     * <li> Its size equals {@code expectedN} </li>
     * <li> It contains {@code Polygons}
     * </ol>
     * If these checks are satisfied, the {@code Polygons} are returned.
     * 
     * @param dest property from the destination image
     * @param expectedN expected number of polygons
     * 
     * @return the polygons
     */
    protected List<Polygon> getPolygons(RenderedOp dest, int expectedN) {
        Object prop = dest.getProperty(VectorizeDescriptor.VECTOR_PROPERTY_NAME);
        assertTrue(prop != null && prop instanceof Collection);

        Collection coll = (Collection) prop;
        assertEquals(expectedN, coll.size());

        List<Polygon> polys = new ArrayList<Polygon>();
        if (expectedN > 0) {
            Object obj = coll.iterator().next();
            assertTrue(obj instanceof Polygon);

            polys.addAll(coll);
        }


        return polys;
    }

    protected void assertPolygons(ExpectedPoly expected, List<Polygon> observed) throws Exception {
        assertPolygons(new ExpectedPoly[]{expected}, observed);
    }

    /**
     * Assert equality of expected and observed polygons.
     * 
     * @param expectedWKT WKT string for expected polygon
     * @param observed observed polygon
     */
    protected void assertPolygons(ExpectedPoly[] expected, List<Polygon> observed) throws Exception {
        PolyList pl = new PolyList(observed);

        for (ExpectedPoly ep : expected) {
            Polygon poly = (Polygon) reader.read(ep.wkt);
            int index = pl.indexOf(poly);
            assertTrue("Polygon not found", index >= 0);
            
            Polygon matchPoly = pl.get(index);
            Number value = (Number) matchPoly.getUserData();
            assertEquals("User data does not match",
                    0, NumberOperations.compare(value, ep.value));
        }
    }

    /**
     * Class to hold WKT String and a numeric value for
     * an expected polygon.
     */
    protected static final class ExpectedPoly {

        String wkt;
        Number value;

        ExpectedPoly(String wkt, Number value) {
            this.wkt = wkt;
            this.value = value;
        }
    }

    /**
     * A {@code List} class that normalizes {@code Polygons} added to it
     * and overrides the {@code indexOf} method to use {@code Polygon.equalsExact}.
     */
    protected static final class PolyList extends ArrayList<Polygon> {

        private static final double TOL = 0.5d;

        PolyList(List<Polygon> polys) {
            for (Polygon p : polys) {
                add(p);
            }
        }

        @Override
        public boolean add(Polygon p) {
            p.normalize();
            return super.add(p);
        }

        @Override
        public int indexOf(Object o) {
            Polygon op = (Polygon) o;
            op.normalize();
            for (int i = 0; i < size(); i++) {
                if (get(i).equalsExact(op, TOL)) {
                    return i;
                }
            }
            return -1;
        }
    }
    
}
