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

import java.awt.image.renderable.RenderedImageFactory;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationRegistry;
import javax.media.jai.OperationRegistrySpi;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * OperationRegistrySpi implementation to register the "VectorBinarize"
 * operation and its associated image factories.
 *
 * @author Michael Bedward
 * @author Andrea Aime
 * @since 1.1
 * @version $Id$
 */
public class VectorBinarizeSpi implements OperationRegistrySpi {

    /** The name of the product to which these operations belong. */
    private String productName = "jaitools.media.jai.vectorbinarize";
 
    /** Default constructor. */
    public VectorBinarizeSpi() {}

    /**
     * Registers the VectorBinarize operation and its
     * associated image factories across all supported operation modes.
     *
     * @param registry The registry with which to register the operations
     * and their factories.
     */
    public void updateRegistry(OperationRegistry registry) {
        OperationDescriptor op = new VectorBinarizeDescriptor();
        registry.registerDescriptor(op);
        String descName = op.getName();
        
        RenderedImageFactory rif = new VectorBinarizeRIF();

        registry.registerFactory(RenderedRegistryMode.MODE_NAME,
                                 descName,
                                 productName,
                                 rif);
    }
}
