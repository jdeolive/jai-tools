package org.jaitools.media.jai.classbreaks;

import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.OperationRegistry;
import javax.media.jai.OperationRegistrySpi;
import javax.media.jai.registry.RenderedRegistryMode;

public class ClassBreaksSpi implements OperationRegistrySpi {

    /** The name of the product to which these operations belong. */
    private String productName = "org.jaitools.media.jai.classifiedstats";

    /** Default constructor. */
    public ClassBreaksSpi() {}


    public void updateRegistry(OperationRegistry registry) {
        ClassBreaksDescriptor op = new ClassBreaksDescriptor();
        registry.registerDescriptor(op);

        String descName = op.getName();

        RenderedImageFactory rif = new ClassBreaksRIF();
        registry.registerFactory(RenderedRegistryMode.MODE_NAME, descName, productName, rif);
    }

}
