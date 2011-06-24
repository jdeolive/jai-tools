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

package org.jaitools.swing;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.HeadlessException;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;


/**
 * A simple Swing widget to display JTS objects.
 * 
 * @author Michael Bedward
 * @since 1.0
 * @version $Id$
 */
public class JTSFrame extends JFrame {

    private static final int MARGIN = 2;
    private Canvas canvas;

    /**
     * Creates a new frame.
     * 
     * @param title the frame title
     */
    public JTSFrame(String title) {
        super(title);
        initComponents();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    /**
     * Adds a {@code Geometry} to display
     * 
     * @param geom the geometry
     * @param col the display color
     */
    public void addGeometry(Geometry geom, Color col) {
        canvas.elements.add(new Element(geom, col));
    }
    
    /**
     * Adds a {@code Coordinate} to display as a point.
     * 
     * @param c the {@code Coordinate}
     * @param col the display color
     */
    public void addCoordinate(Coordinate c, Color col) {
        canvas.elements.add(new Element(c, col));
    }

    private void initComponents() {
        canvas = new Canvas();
        getContentPane().add(canvas);
    }

    private static class Element {

        Object geom;
        Color color;

        public Element(Object g, Color c) {
            geom = g;
            color = c;
        }
    }

    private static class Canvas extends JPanel {

        AffineTransform tr;
        List<Element> elements = new ArrayList<Element>();
        
        private static final int POINT_RADIUS = 4;

        @Override
        protected void paintComponent(Graphics g) {
            if (!elements.isEmpty()) {
                setTransform();
                Graphics2D g2 = (Graphics2D) g;
                g2.setStroke(new BasicStroke(2.0f));
                Coordinate[] coords;
                for (Element e : elements) {
                    g2.setColor(e.color);
                    if (e.geom instanceof Polygon) {
                        Polygon poly = (Polygon) e.geom;
                        coords = poly.getExteriorRing().getCoordinates();
                        draw(g2, coords);
                        for (int i = 0; i < poly.getNumInteriorRing(); i++) {
                            coords = poly.getInteriorRingN(i).getCoordinates();
                            draw(g2, coords);
                        }
                    } else if (e.geom instanceof Geometry) {
                        coords = ((Geometry)e.geom).getCoordinates();
                        draw(g2, coords);
                    } else if (e.geom instanceof Coordinate) {
                        draw(g2, (Coordinate)e.geom);
                    }
                }
            }
        }

        private void draw(Graphics2D g2, Coordinate[] coords) {
            for (int i = 1; i < coords.length; i++) {
                Point2D p0 = new Point2D.Double(coords[i - 1].x, coords[i - 1].y);
                tr.transform(p0, p0);
                Point2D p1 = new Point2D.Double(coords[i].x, coords[i].y);
                tr.transform(p1, p1);
                g2.drawLine((int) p0.getX(), (int) p0.getY(), (int) p1.getX(), (int) p1.getY());
            }
        }
        
        private void draw(Graphics2D g2, Coordinate coord) {
            Point2D p = new Point2D.Double(coord.x, coord.y);
            tr.transform(p, p);
            g2.fillOval((int)p.getX() - POINT_RADIUS, (int)p.getY() - POINT_RADIUS, 
                    2 * POINT_RADIUS, 2 * POINT_RADIUS );
        }

        private void setTransform() {
            Envelope env = new Envelope();
            for (int i = 0; i < elements.size(); i++) {
                Object obj = elements.get(i).geom;
                if (obj instanceof Geometry) {
                    Geometry g = (Geometry) obj;
                    env.expandToInclude(g.getEnvelopeInternal());
                } else if (obj instanceof Coordinate) {
                    Coordinate c = (Coordinate) obj;
                    env.expandToInclude(c);
                }
            }

            Rectangle visRect = getVisibleRect();
            Rectangle drawingRect = new Rectangle(
                    visRect.x + MARGIN, visRect.y + MARGIN, visRect.width - 2 * MARGIN, visRect.height - 2 * MARGIN);

            double scale = Math.min(drawingRect.getWidth() / env.getWidth(), drawingRect.getHeight() / env.getHeight());
            double xoff = MARGIN - scale * env.getMinX();
            double yoff = MARGIN + env.getMaxY() * scale;
            tr = new AffineTransform(scale, 0, 0, -scale, xoff, yoff);
        }
    }
}
