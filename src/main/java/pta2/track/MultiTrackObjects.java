/**
 * 
 */
package pta2.track;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.PolygonRoi;
import ij.gui.Roi;
import ij.gui.Wand;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.plugin.filter.MaximumFinder;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import pta2.PTA2;
import pta2.data.TrackPoint;
import pta2.gui.DrawRois;
import pta2.gui.ResultDataTable;

/**
 * Perfomr multiplt tracking
 * @author araiyoshiyuki
 *
 */
public class MultiTrackObjects extends Thread implements Measurements{

		private ImagePlus imp;
		private int methods;
		private Integer roisize;
		private Integer searchrange;
		private List<List<TrackPoint>> tracklist;
		public List<List<TrackPoint>> allp;
		private long lt;
		private long ut;
		private Double tol;
		private int[] param;
		private boolean batchmode;
		private int startframe;
		private static ResultDataTable rdt;

		public MultiTrackObjects(ImagePlus imp, int startframe, int methods, int param[], double tol, int roisize,
				int searchrange, List<List<TrackPoint>> tracklist, boolean batchmode) {
			this.imp = imp;
			this.startframe = startframe;
			this.methods = methods;
			this.param = param;
			this.roisize = roisize;
			this.searchrange = searchrange;
			this.tol = tol;
			this.tracklist = tracklist;
			this.batchmode = batchmode;
			ImageProcessor ip = imp.getProcessor();
			lt = Math.round(ip.getMinThreshold());
			if(imp.getBitDepth()!=32)
				ut = Math.round(ip.getMaxThreshold());
			else
				ut = 65535;
		}
		
		public void run() {
			int totalframe = imp.getNFrames();
			int f = startframe;
			
			allp = new ArrayList<List<TrackPoint>>(totalframe - startframe);
			IJ.log("Start Multiple Tracking");
			PTA2.isTracking = true;
			Roi arearoi = imp.getRoi();
			
			do {
				imp.setT(f); // move to frame
				if(arearoi != null)
					imp.setRoi(arearoi);
				else
					imp.deleteRoi(); // delete ROI
				MaximumFinder mf = new MaximumFinder();
				Polygon mp = mf.getMaxima(imp.getProcessor(), tol, true);
				DrawRois dr = new DrawRois(imp, mp, roisize);
				dr.show();
				List<TrackPoint> oif = detectObjects(mp); //  oif:object in frame
				allp.add(oif);
				f++;
			} while (f <= totalframe);
			IJ.log("End of Multiple Tracking");
			PTA2.isTracking = false;
			imp.deleteRoi();
			// Should be separated below command
			tracklist = findlinkage(allp);
			PTA2.setTrackList(tracklist);
			rdt = PTA2.getRDT();
			if(!batchmode) {
				if (rdt == null) {
					rdt = new ResultDataTable(tracklist, imp);
				} else {
					rdt.setVisible(false);
					rdt.dispose(); // Destroy JFrame
					rdt = new ResultDataTable(tracklist, imp);
				}
				PTA2.updateRDT(imp, rdt);
			}
		}

		private synchronized List<List<TrackPoint>> findlinkage(List<List<TrackPoint>> allp) {
			int num = allp.size();  // count total frames
			Calibration cal = imp.getCalibration();
			for (int ff = 0; ff < num - 1; ff++) {
				List<TrackPoint> fpointslist = allp.get(ff);  // points list of first frames
				List<TrackPoint> spointslist = allp.get(ff + 1); // points list of second frames
				for(TrackPoint fp: fpointslist) {
					double distance = 100000000.0D;
					TrackPoint sp = new TrackPoint();
					for(TrackPoint tempsp: spointslist) {
						if(tempsp.preTp != null) 
							continue;			// if tempsp have already been linked, skip this point
						double tempd = TrackPoint.calcDistance(fp, tempsp, param, cal);
						if (tempd <= distance) {
							distance = tempd;
							sp = tempsp;
						}
					}
					if (!sp.np && TrackPoint.calcDistance(fp, sp, new int[]{0,0,0,0}, cal) <= searchrange) {
						sp.preTp = fp;
						fp.postTp = sp;
						continue;
					} 
				}
		}
			List<List<TrackPoint>> retTracklist = new ArrayList<List<TrackPoint>>(1000);
			for(List<TrackPoint> plist: allp) {
				for(TrackPoint currentp: plist) {
					List<TrackPoint> newplist = new ArrayList<TrackPoint>(500);
					if (currentp.preTp != null) 
						continue;
					newplist.add(currentp);
					do{
						TrackPoint posttp = currentp.postTp;
						if(posttp == null)
							break;
						newplist.add(posttp);
						currentp = posttp;
					}while (true);
					retTracklist.add(newplist);
				}
			}
			return retTracklist;
		}

		// Detecting objects based on the methods.
		private synchronized List<TrackPoint> detectObjects(Polygon mp) {
			ImageProcessor ip = imp.getProcessor();
			Calibration cal = imp.getCalibration();
			int currentframe = imp.getFrame();
			double mean = 0;
			double integint = 0;
			double cx = 0, cy = 0; // center of x,y
			List<TrackPoint> tmpl = new ArrayList<TrackPoint>(100);
			int p = 0; // num of points
			do {
				TrackPoint tp = new TrackPoint();
				double x = (double)mp.xpoints[p] / cal.pixelWidth;
				double y = (double)mp.ypoints[p] / cal.pixelHeight;
				double val = ip.getPixel((int)x, (int)y);
				if (val < lt || val > ut){
					p++;
					continue; // skip points
				}
				
				ImageStatistics is = imp.getStatistics(CENTROID + RECT);				
				Roi sqroi = new Roi((int)(x * cal.pixelWidth - roisize / 2), (int)(y * cal.pixelHeight - roisize / 2), 
						roisize, roisize);
				imp.setRoi(sqroi);
				is = imp.getStatistics(MEAN + INTEGRATED_DENSITY);
				integint = is.mean * is.longPixelCount; // integrated intensity
				
				if (methods == 4) { // Find maxima
					tp = new TrackPoint(x * cal.pixelWidth, y * cal.pixelHeight, val, integint, currentframe, roisize);
					p++;
					tmpl.add(tp);
					continue;
				}
				
				Wand wand = new Wand(ip);
				wand.autoOutline((int)(x * cal.pixelWidth), (int)(y * cal.pixelHeight), lt, ut);
				Roi wandRoi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON);
				imp.setRoi(wandRoi);
				
				// Calculate circularity (from ij.plugin.filter.ParticleAnalyzer)
				double perimeter = wandRoi.getLength();
				double circularity = perimeter==0.0?0.0:4.0*Math.PI*(is.pixelCount/(perimeter*perimeter));
				if (circularity>1.0) circularity = 1.0;
				
				// Centroid tracking
				is = imp.getStatistics(AREA + CENTROID + MEAN);
				cx = is.xCentroid; cy = is.yCentroid;
				mean = is.mean;
				tp = new TrackPoint(cx, cy, is.area, mean, integint, circularity, currentframe, roisize);
				if (methods == 1) { //CENTER OF MASS
					is = imp.getStatistics(AREA + CENTER_OF_MASS + MEAN);
					cx = is.xCenterOfMass; cy = is.yCenterOfMass;
					mean = is.mean;
					tp = new TrackPoint(cx, cy, is.area, mean, integint, circularity, currentframe, roisize);
				} else if (methods == 2) { //2D Gaussian fitting
					tp = twoDGFit(imp, tp);
				}
				tmpl.add(tp);
				p++;
			}while(p < mp.npoints);
			return tmpl;
		}
		
		public static TrackPoint twoDGFit(ImagePlus imps, TrackPoint tp) {
			/*
			 * @param imps ImagePlus
			 * @param tp, initial trackpoint
			 * @ret two dimensional Gaussian fitted TrackPoint
			 */
			int frame = tp.frame;
			int roisize = tp.roisize;
			imps.setT(frame);
			ImageProcessor ip = imps.getProcessor();
			Calibration cal = imps.getCalibration();			
			Roi sqroi = new Roi((int)(tp.tx * cal.pixelWidth - roisize / 2), (int)(tp.ty * cal.pixelHeight - roisize / 2), 
					roisize, roisize);
			imps.setRoi(sqroi);
			ImageStatistics is = imps.getStatistics(CENTROID + RECT);
			double xx = is.xCentroid - cal.pixelWidth * (double)roisize / 2.0D;
			double yy = is.yCentroid - cal.pixelHeight * (double)roisize / 2.0D;
			int ixx = (int)(xx / cal.pixelWidth);
			int iyy = (int)(yy / cal.pixelHeight);	
			
			// if roi is out of image bound
			if(ixx < 0 || iyy < 0 ||(ixx + roisize) > imps.getWidth() || (iyy + roisize) > imps.getWidth())
			{
				return tp; // cannot fit
			}
			FloatProcessor fip = ip.convertToFloatProcessor();
			double[] inputdata = new double[roisize * roisize];
			float[] pixVal = (float[])fip.getPixels();
			for(int ii = 0;ii < roisize * roisize; ii++) {
				// x position is mod (count (ii), y number )
				// y position is count / x size number
				int ix = ii % roisize, iy = ii / roisize;
				double tmpval = (double)pixVal[ixx + ix + (iyy + iy) * imps.getWidth()];
				inputdata[ix + iy * roisize] = tmpval;
			}				
			double[] newStart = {  // initial values for 2D Gaussian fitting
					(double)is.max,			// intensity
					(double)roisize / 2D,	// x
					(double)roisize / 2D,	// y
					(double)roisize / 10D,	// sigma x
					(double)roisize / 10D,	// sigma y
					(double)is.min			// offset		
			};
			TwoDGaussProblem tdgp = new TwoDGaussProblem(inputdata, newStart, roisize, new int[] {1000, 1000});
			
			try{
				//do LevenbergMarquardt optimization and get optimized parameters
				Optimum opt = tdgp.fit2dGauss();
				final double[] optimalValues = opt.getPoint().toArray();
			
				tp.tx = (double)ixx + optimalValues[1];
				tp.ty = (double)iyy + optimalValues[2];
				tp.mean = optimalValues[0];
				tp.offset = optimalValues[5];
				tp.sx = optimalValues[3];
				tp.sy = optimalValues[4];
				tp.ite = opt.getIterations();
				//IJ.log("Iteration = "+iteration);
			} catch (Exception e) {
				//nothing to do
			}
			return tp;
		}
} 
