package pta2.gui;

import java.awt.Polygon;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.process.ImageProcessor;

/*
 * Draw square rois obtained by Find Maxima
 * @author araiyoshiyuki
 */
public class DrawRois {

		Overlay ol;
		ImagePlus imp;
	
		public DrawRois(ImagePlus imp, Polygon mp, int roi) {
			this.imp = imp;
			Calibration cal = imp.getCalibration();
			ImageProcessor ip = imp.getProcessor();
			
			ol = imp.getOverlay();
			if (ol == null) {
				ol = new Overlay();
			}
			ol.clear();
			
			for(int p = 0;p<mp.npoints;p++) {
				double x = mp.xpoints[p] / cal.pixelWidth;
				double y = mp.ypoints[p] / cal.pixelHeight;
				double val = ip.getPixel((int)x, (int)y);
				if(ip.getMinThreshold() != -808080.0D && 
						val >= ip.getMinThreshold() && val <= ip.getMaxThreshold()) {
					Roi r = new Roi(x - roi / 2, y - roi / 2, roi, roi);
					ol.add(r);
				}
				if(ip.getMinThreshold() == -808080.0D) {
					Roi r = new Roi(x - roi / 2, y - roi / 2, roi, roi);
					ol.add(r);
				}
			}

		}
		
		public synchronized void show() {
			imp.setOverlay(ol);
			imp.updateAndDraw();
		}
}
