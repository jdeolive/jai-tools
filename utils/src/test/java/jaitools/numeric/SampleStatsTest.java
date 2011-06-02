/*
 * Copyright 2009 Michael Bedward
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

package jaitools.numeric;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the SampleStats class
 *
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public class SampleStatsTest {
    
    private static final double TOL = 1.0e-8;
    
    static final Double[] values;
    static {
        values = new Double[20];
        for (int i = 1, k=0; i <= 10; i++) {
            values[k++] = (double)i;
            values[k++] = Double.NaN;
        }
    }

    static final Double[] singleValue = { 42.0 };

    @Test
    public void testMax() {
        System.out.println("   max");
        double expResult = 10.0d;
        double result = SampleStats.max(values, true);
        assertEquals(expResult, result, TOL);
    }
    
    @Test
    public void testMaxSingleValue() {
        System.out.println("   max with single value");
        assertEquals(singleValue[0], Double.valueOf(SampleStats.max(singleValue, true)));
    }

    @Test
    public void testMin() {
        System.out.println("   min");
        double expResult = 1.0d;
        double result = SampleStats.min(values, true);
        assertEquals(expResult, result, TOL);
    }

    @Test
    public void testMinSingleValue() {
        System.out.println("   min with single value");
        assertEquals(singleValue[0], Double.valueOf(SampleStats.min(singleValue, true)));
    }

    @Test
    public void testMedian() {
        System.out.println("   median");
        double expResult = 5.5d;
        double result = SampleStats.median(values, true);
        assertEquals(expResult, result, TOL);
    }
    
    @Test
    public void testMedianSingleValue() {
        System.out.println("   median with single value");
        assertEquals(singleValue[0], Double.valueOf(SampleStats.median(singleValue, true)));
    }

    @Test
    public void testRange() {
        System.out.println("   range");
        double expResult = 9.0d;
        double result = SampleStats.range(values, true);
        assertEquals(expResult, result, TOL);
    }
    
    @Test
    public void testRangeSingleValue() {
        System.out.println("   range with single value");
        assertEquals(Double.valueOf(0), Double.valueOf(SampleStats.range(singleValue, true)));
    }

    @Test
    public void testMean() {
        System.out.println("   mean");
        double expResult = 5.5d;
        double result = SampleStats.mean(values, true);
        assertEquals(expResult, result, TOL);
    }

    @Test
    public void testMeanSingleValue() {
        System.out.println("   mean with single value");
        assertEquals(singleValue[0], Double.valueOf(SampleStats.mean(singleValue, true)));
    }

    @Test
    public void testVariance() {
        System.out.println("   variance");
        double expResult = 9.0d + 1.0d / 6;
        double result = SampleStats.variance(values, true);
        assertEquals(expResult, result, TOL);
    }
    
    @Test
    public void testSum() {
        System.out.println("   sum");
        double expResult = 55.0d;
        double result = SampleStats.sum(values, true);
        assertEquals(expResult, result, TOL);
    }

    @Test
    public void testVarianceSingleValue() {
        System.out.println("   variance with single value");
        assertTrue(Double.isNaN(SampleStats.variance(singleValue, true)));
    }

}
