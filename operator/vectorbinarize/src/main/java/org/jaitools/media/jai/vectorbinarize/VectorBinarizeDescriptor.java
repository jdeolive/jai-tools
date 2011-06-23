/* 
 *  Copyright (c) 2010-2011, Michael Bedward. All rights reserved. 
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

package org.jaitools.media.jai.vectorbinarize;

import java.awt.image.renderable.ParameterBlock;

import javax.media.jai.OperationDescriptorImpl;
import javax.media.jai.registry.RenderedRegistryMode;

import com.vividsolutions.jts.geom.Polygonal;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;


/**
 * Describes the "VectorBinarize" operation which creates a binary image based
 * on pixel inclusion in a polygonal {@code Geometry} object. No source image
 * is used.
 * The reference polygon must be one of the following JTS classes:
 * {@code Polygon}, {@code MultiPolygon} or {@code PreparedGeometry}.
 * <p>
 * Pixels are tested
 * for inclusion using either their corner coordinates (equivalent to standard JAI
 * pixel indexing) or center coordinates (0.5 added to each ordinate) depending
 * on the "coordtype" parameter.
 * <p>
 * Example of use:
 * <pre><code>
 * // Using a JTS polygon object as the reference geometry
 * Polygon triangle = WKTReader.read("POLYGON((100 100, 4900 4900, 4900 100, 100 100))"); 
 * 
 * ParameterBlockJAI pb = new ParameterBlockJAI("VectorBinarize");
 * pb.setParameter("minx", 0);
 * pb.setParameter("miny", 0);
 * pb.setParameter("width", 5000);
 * pb.setParameter("height", 5000);
 * pb.setParameter("geometry", triangle);
 * 
 * // specify that we want to use center coordinates of pixels
 * pb.setParameter("coordtype", PixelCoordType.CENTER);
 * 
 * RenderedOp dest = JAI.create("VectorBinarize", pb);
 * </code></pre>
 * 
 * By default, the destination image is type BYTE, with a {@link java.awt.image.MultiPixelPackedSampleModel}
 * and JAI's default tile size. If an alternative image type is desired this can be specified via
 * rendering hints as in this example:
 * <pre><code>
 * SampleModel sm = ...
 * ImageLayout il = new ImageLayout();
 * il.setSampleModel(sm);
 * RenderingHints hints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, il);
 * RenderedOp dest = JAI.create("VectorBinarize", pb, hints);
 * </code></pre>
 * 
 * <b>Summary of parameters:</b>
 * <table border="1", cellpadding="3">
 * <tr>
 * <th>Name</th>
 * <th>Class</th>
 * <th>Default</th>
 * <th>Description</th>
 * </tr>
 * 
 * <tr>
 * <td>minx</td>
 * <td>int</td>
 * <td>0</td>
 * <td>Min image X ordinate</td>
 * </tr>
 * 
 * <tr>
 * <td>miny</td>
 * <td>int</td>
 * <td>0</td>
 * <td>Min image Y ordinate</td>
 * </tr>
 * 
 * <tr>
 * <td>width</td>
 * <td>int</td>
 * <td>No default</td>
 * <td>Image width</td>
 * </tr>
 * 
 * <tr>
 * <td>height</td>
 * <td>int</td>
 * <td>No default</td>
 * <td>Image height</td>
 * </tr>
 * 
 * <tr>
 * <td>geometry</td>
 * <td>Geometry or PreparedGeometry</td>
 * <td>No default</td>
 * <td>The reference polygonal geometry</td>
 * </tr>
 * 
 * <tr>
 * <td>antiAliasing</td>
 * <td>Boolean</td>
 * <td>{@linkplain VectorBinarizeOpImage#DEFAULT_ANTIALIASING}</td>
 * <td>Whether to use anti-aliasing when rendering (pixellating) the reference geometry</td>
 * </tr>
 * </table>
 * 
 * @author Michael Bedward.
 * @author Andrea Aime, GeoSolutions.
 * @since 1.1
 * @version $Id$
 */
public class VectorBinarizeDescriptor extends OperationDescriptorImpl {
    
    static final int MINX_ARG = 0;
    static final int MINY_ARG = 1;
    static final int WIDTH_ARG = 2;
    static final int HEIGHT_ARG = 3;
    static final int GEOM_ARG = 4;
    static final int ANTIALIASING_ARG = 5;

    private static final String[] paramNames = {
        "minx",
        "miny",
        "width",
        "height",
        "geometry",
        "antiAliasing"
    };
    
    private static final Class[] paramClasses = {
        Integer.class,
        Integer.class,
        Integer.class,
        Integer.class,
        Object.class,
        Boolean.class
    };

    private static final Object[] paramDefaults = {
        Integer.valueOf(0),
        Integer.valueOf(0),
        NO_PARAMETER_DEFAULT,
        NO_PARAMETER_DEFAULT,
        NO_PARAMETER_DEFAULT,
        VectorBinarizeOpImage.DEFAULT_ANTIALIASING
    };

    
    /**
     * Creates a new instance.
     */
    public VectorBinarizeDescriptor() {
        super(new String[][]{
                    {"GlobalName", "VectorBinarize"},
                    {"LocalName", "VectorBinarize"},
                    {"Vendor", "org.jaitools.media.jai"},
                    {"Description", "Creates a binary image based on the inclusion of " +
                            "pixels within a polygonal Geometry"},
                    {"DocURL", "http://code.google.com/p/jaitools/"},
                    {"Version", "1.3.0"},
                    
                    {"arg0Desc", paramNames[MINX_ARG] + " (Integer, default = 0) min image X"},
                    
                    {"arg1Desc", paramNames[MINY_ARG] + " (Integer, default = 0) min image Y"},
                    
                    {"arg2Desc", paramNames[WIDTH_ARG] + " (Integer) image width"},
                    
                    {"arg3Desc", paramNames[HEIGHT_ARG] + " (Integer) image height"},
                    
                    {"arg4Desc", paramNames[GEOM_ARG] + " the reference Geometry: " + 
                              "either a Polygon, a MultiPolygon or a polygonal PreparedGeometry"},
                    
                    {"arg6Desc", paramNames[ANTIALIASING_ARG] + " (Boolean, default = false) " +
                              "Whether to use antiAliasing as Hints on geometry rendering" }
                },
                
                new String[]{RenderedRegistryMode.MODE_NAME},   // supported modes
                
                0,                                              // number of sources
                
                paramNames,
                paramClasses,
                paramDefaults,
                    
                null                                            // valid values (none defined)
                );    
    }

    /**
     * Validates supplied parameters.
     * 
     * @param modeName the rendering mode
     * @param pb the parameter block
     * @param msg a {@code StringBuffer} to receive error messages
     * 
     * @return {@code true} if parameters are valid; {@code false} otherwise
     */
    @Override
    protected boolean validateParameters(String modeName, ParameterBlock pb, StringBuffer msg) {
        boolean ok = super.validateParameters(modeName, pb, msg);
        
        if (ok) {
            Object obj = pb.getObjectParameter(GEOM_ARG);
            if (!(obj instanceof Polygonal || obj instanceof PreparedGeometry)) {
                ok = false;
                msg.append("The reference geometry must be either Polygon, MultiPolygon, or a "
                        + "polygonal PreparedGeometry");
            }
        }
        
        return ok;
    }    
}
