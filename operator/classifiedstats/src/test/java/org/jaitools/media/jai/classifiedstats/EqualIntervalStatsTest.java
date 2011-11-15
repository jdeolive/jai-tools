package org.jaitools.media.jai.classifiedstats;

import javax.media.jai.JAI;
import javax.media.jai.ParameterBlockJAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.TiledImage;

import org.jaitools.imageutils.ImageUtils;
import org.junit.Test;

public class EqualIntervalStatsTest {

    @Test
    public void test() throws Exception {
      TiledImage img = ImageUtils.createImageFromArray(
          new Byte[]{1,1,2,3,3,8,8,9,11,14,16,24,26,26,45,53}, 4, 4);

      
      ParameterBlockJAI pb = new ParameterBlockJAI(EqualIntervalStatsDescriptor.NAME);
      pb.setSource("source0", img);
      pb.setParameter("extrema", new Double[][]{{1d},{53d}});
     
      RenderedOp classify = JAI.create(EqualIntervalStatsDescriptor.NAME, pb);
      classify.getProperty(EqualIntervalStatsDescriptor.STATS_PROPERTY);
    }
}
