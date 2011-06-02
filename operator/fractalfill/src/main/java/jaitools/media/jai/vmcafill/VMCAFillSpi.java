/*
 * Copyright 2011 Michael Bedward
 * 
 * This file is part of jai-tools.

 * jai-tools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.

 * jai-tools is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.

 * You should have received a copy of the GNU Lesser General Public 
 * License along with jai-tools.  If not, see <http://www.gnu.org/licenses/>.
 * 
 */

package jaitools.media.jai.vmcafill;

import java.awt.image.renderable.RenderedImageFactory;
import javax.media.jai.OperationDescriptor;
import javax.media.jai.OperationRegistry;
import javax.media.jai.OperationRegistrySpi;
import javax.media.jai.registry.RenderedRegistryMode;

/**
 * OperationRegistrySpi implementation to register the "VMCAFill"
 * operation and its associated image factories.
 *
 * @author Michael Bedward
 * @since 1.2
 * @version $Id$
 */
public class VMCAFillSpi implements OperationRegistrySpi {

    /** The name of the product to which these operations belong. */
    private String productName = "jaitools.media.jai.fractalfill";
 
    /** Default constructor. */
    public VMCAFillSpi() {}

    /**
     * Registers the MaskedConvolve operation and its
     * associated image factories across all supported operation modes.
     *
     * @param registry The registry with which to register the operations
     * and their factories.
     */
    public void updateRegistry(OperationRegistry registry) {
        OperationDescriptor op = new VMCAFillDescriptor();
        registry.registerDescriptor(op);
        String descName = op.getName();
        
        RenderedImageFactory rif = new VMCAFillRIF();

        registry.registerFactory(RenderedRegistryMode.MODE_NAME,
                                 descName,
                                 productName,
                                 rif);

    }
}
