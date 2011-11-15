package org.jaitools.media.jai.classifiedstats;

import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import javax.media.jai.ImageLayout;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.PixelAccessor;
import javax.media.jai.ROI;
import javax.media.jai.StatisticsOpImage;
import javax.media.jai.UnpackedImageData;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.jaitools.numeric.Range;
import org.jaitools.numeric.Statistic;

public class EqualIntervalStatsOpImage extends StatisticsOpImage {

    /* number of classes */
    Integer numBins;

    /* range of values to calculate per band */
    Double[][] extrema;

    /* stats to compute */
    Statistic[] stats;

    /* bands to process */
    Integer[] bands;

    /* no data value */
    Double noData;

    public EqualIntervalStatsOpImage(RenderedImage image, Integer numBins, Double[][] extrema, 
        Statistic[] stats, ROI roi, Integer[] bands, Integer xStart, Integer yStart, 
        Integer xPeriod, Integer yPeriod, Double noData) {

        super(image, roi, xStart, yStart, xPeriod, yPeriod);

        this.numBins = numBins;
        this.extrema = extrema;
        this.stats = stats;
        this.roi = roi;
        this.bands = bands;
        this.xPeriod = xPeriod;
        this.yPeriod = yPeriod;
        this.noData = noData;

        // setup/calculate image/tile dimensions
        /*this.imgWidth = image.getWidth();
        this.imgHeight = image.getHeight();
        this.imgTileWidth = Math.min(image.getTileWidth(), imgWidth);
        this.imgTileHeight = Math.min(image.getTileHeight(), imgHeight);
        this.imgMinY = image.getMinY();
        this.imgMinX = image.getMinX();
        this.imgMaxX = imgMinX + imgWidth - 1;
        this.imgMaxY = imgMinY + imgHeight - 1;
        this.imgMinTileX = image.getMinTileX();
        this.imgMinTileY = image.getMinTileY();
        this.imgMaxTileX = imgMinTileX + image.getNumXTiles();
        this.imgMaxTileY = imgMinTileY + image.getNumYTiles();
        this.imgBounds = new Rectangle(imgMinX, imgMinY, imgWidth, imgHeight);*/
    }

    @Override
    protected String[] getStatisticsNames() {
        return new String[]{EqualIntervalStatsDescriptor.STATS_PROPERTY};
    }

    @Override
    protected Object createStatistics(String name) {
        if (EqualIntervalStatsDescriptor.STATS_PROPERTY.equals(name)) {
            ClassifiedStats2 stats = new ClassifiedStats2(bands.length, this.stats);
            
            //calculate the bins
            for (int b = 0; b < bands.length; b++) {
                double min = extrema[0][b];
                double max = extrema[1][b];

                //calculate the bins
                double delta = (max - min) / (double) numBins;
                double start = min;
                for (int j = 0; j < numBins-1; j++) {
                    double end = start + delta;

                    stats.newBin(b, Range.create(start, true, end, false));

                    start = end;
                }

                //last bin
                stats.newBin(b, Range.create(start, true, max, true));
            }

            return stats;
        }
        return null;
    }

    @Override
    protected void accumulateStatistics(String name, Raster raster, Object obj) {
        if (!EqualIntervalStatsDescriptor.STATS_PROPERTY.equals(name)) {
            return;
        }

        ClassifiedStats2 stats = (ClassifiedStats2) obj;
        SampleModel sampleModel = raster.getSampleModel();

        Rectangle bounds = raster.getBounds();

        LinkedList rectList;
        if (roi == null) {      // ROI is the whole Raster
            rectList = new LinkedList();
            rectList.addLast(bounds);
        } else {
            rectList = roi.getAsRectangleList(bounds.x, bounds.y,
                                              bounds.width, bounds.height);
            if (rectList == null) {
                return; // ROI does not intersect with Raster boundary.
            }
        }

        PixelAccessor accessor = new PixelAccessor(sampleModel, null);

        ListIterator iterator = rectList.listIterator(0);

        while (iterator.hasNext()) {
            Rectangle r = (Rectangle)iterator.next();
            int tx = r.x;
            int ty = r.y;

            // Find the actual ROI based on start and period.
            r.x = startPosition(tx, xStart, xPeriod);
            r.y = startPosition(ty, yStart, yPeriod);
            r.width = tx + r.width - r.x;
            r.height = ty + r.height - r.y;

            if (r.width <= 0 || r.height <= 0) {
                continue;       // no pixel to count in this rectangle
            }

            countPixelsDouble(accessor, raster, r, tx, ty, stats);
        }
        
    }

    private void countPixelsDouble(PixelAccessor accessor, Raster raster, Rectangle rect,
            int xPeriod, int yPeriod, ClassifiedStats2 stats) {
        UnpackedImageData uid = accessor.getPixels(raster, rect, DataBuffer.TYPE_DOUBLE, false);

        double[][] doubleData = uid.getDoubleData();
        int pixelStride = uid.pixelStride * xPeriod;
        int lineStride = uid.lineStride * yPeriod;
        int[] offsets = uid.bandOffsets;

        for (int i = 0; i < bands.length; i++) {
            int b = bands[i];
            
            double[] data = doubleData[b];
            int lineOffset = offsets[b]; // line offset

            double low = extrema[0][b];
            double high = extrema[1][b];
            double delta = (high - low) / (double) numBins;

            for (int h = 0; h < rect.height; h += yPeriod) {
                int pixelOffset = lineOffset; // pixel offset
                lineOffset += lineStride;

                for (int w = 0; w < rect.width; w += xPeriod) {
                    double d = data[pixelOffset];
                    pixelOffset += pixelStride;

                    if (d >= low && d < high) {
                        //assign to a bin
                        int bin =  (int) (d / delta);
                        stats.offer(i, bin, d);
                    }
                }
            }
        }
    }

    /** Finds the first pixel at or after <code>pos</code> to be counted. */
    private int startPosition(int pos, int start, int Period) {
        int t = (pos - start) % Period;
        return t == 0 ? pos : pos + (Period - t);
    }

}
