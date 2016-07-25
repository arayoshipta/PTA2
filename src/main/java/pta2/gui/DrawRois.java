package pta2.gui;

import java.awt.Polygon;

import ij.ImagePlus;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.measure.Calibration;

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
			
			ol = imp.getOverlay();
			if (ol == null) {
				ol = new Overlay();
			}
			ol.clear();
			
			for(int p = 0;p<mp.npoints;p++) {
				double x = mp.xpoints[p] / cal.pixelWidth;
				double y = mp.ypoints[p] / cal.pixelHeight;
				Roi r = new Roi(x - roi / 2, y - roi / 2, roi, roi);
				ol.add(r);
			}

		}
		
		public synchronized void show() {
			imp.setOverlay(ol);
			imp.updateAndDraw();
		}
}
