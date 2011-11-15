package org.jaitools.media.jai.classifiedstats;

import java.awt.image.renderable.RenderedImageFactory;

import javax.media.jai.OperationRegistry;
import javax.media.jai.OperationRegistrySpi;
import javax.media.jai.registry.RenderedRegistryMode;

public class EqualIntervalStatsSpi implements OperationRegistrySpi {

    /** The name of the product to which these operations belong. */
    private String productName = "org.jaitools.media.jai.classifiedstats";

    /** Default constructor. */
    public EqualIntervalStatsSpi() {}


    public void updateRegistry(OperationRegistry registry) {
        EqualIntervalStatsDescriptor op = new EqualIntervalStatsDescriptor();
        registry.registerDescriptor(op);

        String descName = op.getName();

        RenderedImageFactory rif = new EqualIntervalStatsRIF();
        registry.registerFactory(RenderedRegistryMode.MODE_NAME, descName, productName, rif);
    }

}
