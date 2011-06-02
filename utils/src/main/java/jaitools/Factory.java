/*
 * Copyright 2009-2011 Michael Bedward
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

package jaitools;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Abstract base class for jai-tools object factories.
 *
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public abstract class Factory {
    private static final Set<String> supportedSpi = new HashSet<String>();

    /**
     * Protected-access constructor.
     */
    protected Factory() {
    }

    /**
     * Adds a service provider interface.
     * 
     * @param spiName the interface name
     */
    protected static void addSpi(String spiName) {
        supportedSpi.add(spiName);
    }

    /**
     * Gets the supported interfaces.
     * 
     * @return an unmodifiable list of the interface names
     */
    public static Collection<String> getSupported() {
        return Collections.unmodifiableCollection(supportedSpi);
    }

}
