/**
 * 
 */
package spta.gui;

import java.awt.Color;
import java.awt.event.*;

import ij.*;
import ij.gui.*;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.process.*;

/**
 * @author araiyoshiyuki
 *
 */
public class icMouseAdapter extends MouseAdapter implements MouseListener, Measurements {
	
	public ImagePlus imp;
	public double lt, ut; // lower and upper limit of Threshold values
	private MainWindow mw;
	
	public icMouseAdapter(ImagePlus imp, int roisize, MainWindow mw) {
		this.imp = imp;
		this.mw = mw;
	}
	
	public void mouseClicked(MouseEvent e) {
		ImageProcessor ip = imp.getProcessor();
		FloatProcessor fip = ip.convertToFloatProcessor();
		ImageCanvas ic = imp.getCanvas();
		Overlay ol = imp.getOverlay();
		Calibration cal = imp.getCalibration();
		
		if(ol == null) {
			ol = new Overlay();
			imp.setOverlay(ol);
		}
		ol.clear();
		lt = Math.round(ip.getMinThreshold());
		if(imp.getBitDepth()!=32)
			ut = Math.round(ip.getMaxThreshold());
		else
			ut = 65535;
		
		double wdx = ic.offScreenX(e.getX()) / cal.pixelWidth; // to change the xy-cooridnate if the image is zoomed.
		double wdy = ic.offScreenY(e.getY()) / cal.pixelHeight;		
		double fval = Float.intBitsToFloat(fip.getPixel((int)wdx, (int)wdy));

		if(ip.getMinThreshold() != -808080.0D && lt <= fval && fval <= ut) { // Is Threshold set ?
			Wand wand = new Wand(ip);
			wand.autoOutline((int)wdx, (int)wdy, lt, ut);
			Roi wandRoi = new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
			imp.setRoi(wandRoi);
			ImageStatistics is = imp.getStatistics(CENTROID+RECT);
			double roisize = is.roiHeight > is.roiWidth?is.roiHeight:is.roiWidth; // get bounding box of wand roi
			roisize = (int)(roisize * 3.0);
			Roi squareroi = new Roi((int)(is.xCentroid - roisize / 2), (int)(is.yCentroid - roisize / 2),
							(int)roisize, (int)roisize);
			squareroi.setStrokeColor(Color.red);
			ol.add(squareroi);
			imp.updateAndDraw();
			mw.roisize.setValue((int)roisize);
			}
	}
}
