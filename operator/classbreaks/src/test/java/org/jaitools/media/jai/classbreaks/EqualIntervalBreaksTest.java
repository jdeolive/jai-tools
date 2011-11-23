package org.jaitools.media.jai.classbreaks;

import java.awt.image.RenderedImage;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import org.jaitools.imageutils.ImageUtils;
import org.junit.Test;

import static org.junit.Assert.*;

public class EqualIntervalBreaksTest {

    @Test
    public void testWithExtrema() throws Exception {
     
      RenderedOp classify = doOp(4, new Double[][]{{1d},{53d}});
      Classification classification = 
          (Classification) classify.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
      assertNotNull(classification);

      Number[][] breaks = classification.getBreaks();
      assertEquals(1, breaks.length);

      Number[] b = breaks[0];
      assertEquals(5, b.length);
      assertEquals(1.0, b[0]);
      assertEquals(14.0, b[1]);
      assertEquals(27.0, b[2]);
      assertEquals(40.0, b[3]);
      assertEquals(53.0, b[4]);
    }

    @Test
    public void testWithConstrainedExtrema() throws Exception {
        RenderedOp classify = doOp(4, new Double[][]{{8d},{26d}});
        Classification classification = 
            (Classification) classify.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        assertNotNull(classification);

        Number[][] breaks = classification.getBreaks();
        assertEquals(1, breaks.length);

        Number[] b = breaks[0];
        assertEquals(5, b.length);
        assertEquals(8.0, b[0]);
        assertEquals(12.5, b[1]);
        assertEquals(17.0, b[2]);
        assertEquals(21.5, b[3]);
        assertEquals(26.0, b[4]);
    }

    @Test
    public void testWithNoExtrema() throws Exception {
        RenderedOp classify = doOp(4, null);
        Classification classification = 
            (Classification) classify.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        assertNotNull(classification);

        Number[][] breaks = classification.getBreaks();
        assertEquals(1, breaks.length);

        Number[] b = breaks[0];
        assertEquals(5, b.length);
        assertEquals(1.0, b[0]);
        assertEquals(14.0, b[1]);
        assertEquals(27.0, b[2]);
        assertEquals(40.0, b[3]);
        assertEquals(53.0, b[4]);
    }

    @Test
    public void testWithNoData() throws Exception {
        TiledImage img = ImageUtils.createImageFromArray(
            new Integer[]{
              99,1,1,2,99,
              3,99,3,8,99,
              8,9,99,11,99,
              14,16,24,99,99,
              26,26,45,53,99}, 5, 5);
                
        RenderedOp classify = doOp(img, 4, null, 99d);
        Classification classification = 
            (Classification) classify.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        assertNotNull(classification);

        Number[][] breaks = classification.getBreaks();
        assertEquals(1, breaks.length);

        Number[] b = breaks[0];
        assertEquals(5, b.length);
        assertEquals(1.0, b[0]);
        assertEquals(14.0, b[1]);
        assertEquals(27.0, b[2]);
        assertEquals(40.0, b[3]);
        assertEquals(53.0, b[4]);
    }

    RenderedOp doOp(int numClasses, Double[][] extrema) {
        TiledImage img = ImageUtils.createImageFromArray(
            new Byte[]{1,1,2,3,3,8,8,9,11,14,16,24,26,26,45,53}, 4, 4);
        return doOp(img, numClasses, extrema, null);
    }
    
    RenderedOp doOp(RenderedImage img, int numClasses, Double[][] extrema, Double noData) {

        ParameterBlockJAI pb = new ParameterBlockJAI(ClassBreaksDescriptor.NAME);
        pb.setSource("source0", img);
        pb.setParameter("numClasses", numClasses);
        if (extrema != null) {
            pb.setParameter("extrema", extrema);
        }
        if (noData != null) {
            pb.setParameter("noData", noData);
        }

        return JAI.create(ClassBreaksDescriptor.NAME, pb);
    }
}
