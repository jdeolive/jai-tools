/* 
 *  Copyright (c) 2010, Michael Bedward. All rights reserved. 
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

package org.jaitools.lookup;

import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * @author Michael Bedward
 */
public class LookupTest {
    
    @After
    public void tearDown() {
        Lookup.clearCache();
    }

    @Test
    public void testLookup() {
        System.out.println("   testLookup");

        final Class clazz = org.jaitools.numeric.Processor.class;

        List<Class> providers = Lookup.getProviders(clazz.getName());
        assertFalse(providers.isEmpty());

        for (Class c : providers) {
            assertTrue(clazz.isAssignableFrom(c));
        }
    }

    @Test
    public void testLookupCache() {
        System.out.println("   testLookupCache");

        final Class clazz = org.jaitools.numeric.Processor.class;

        List<Class> providers = Lookup.getProviders(clazz.getName());
        Map<String, List<Class>> cachedProviders = Lookup.getCachedProviders();

        List<Class> cachedList = cachedProviders.get(clazz.getName());
        assertNotNull(cachedList);
        assertTrue(cachedList.containsAll(providers));
    }

}
