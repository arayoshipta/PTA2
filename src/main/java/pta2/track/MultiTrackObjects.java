/**
 * 
 */
package pta2.track;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SpinnerNumberModel;

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
 * @author araiyoshiyuki
 *
 */
public class MultiTrackObjects extends Thread implements Measurements{

		private ImagePlus imp;
		private int methods;
		private Integer roisize;
		private Integer searchrange;
		private List<List<TrackPoint>> tracklist;
		private long lt;
		private long ut;
		private Double tol;
		private int[] param;
		private static ResultDataTable rdt;

		public MultiTrackObjects(ImagePlus imp, int methods, int param[], SpinnerNumberModel tol, SpinnerNumberModel roisize,
				SpinnerNumberModel searchrange, List<List<TrackPoint>> tracklist) {
			this.imp = imp;
			this.methods = methods;
			this.param = param;
			this.roisize = (Integer)roisize.getValue();
			this.searchrange = (Integer)searchrange.getValue();
			this.tol = (Double)tol.getValue();
			this.tracklist = tracklist;
			ImageProcessor ip = imp.getProcessor();
			lt = Math.round(ip.getMinThreshold());
			if(imp.getBitDepth()!=32)
				ut = Math.round(ip.getMaxThreshold());
			else
				ut = 65535;
		}
		
		public void run() {
			int totalframe = imp.getNFrames();
			int startframe = imp.getFrame();
			int f = startframe;
			List<List<TrackPoint>> allp = new ArrayList<List<TrackPoint>>(totalframe - startframe);
			IJ.log("Start Multiple Tracking");
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
			imp.deleteRoi();
			tracklist = findlinkage(allp);
			PTA2.setTrackList(tracklist);
			rdt = PTA2.getRDT();
			
			if (rdt == null) {
				rdt = new ResultDataTable(tracklist, imp);
				PTA2.updateRDT(imp, rdt);
			} else {
				rdt.setVisible(false);
				rdt.dispose(); // Destroy JFrame
				rdt = new ResultDataTable(tracklist, imp);
				PTA2.updateRDT(imp, rdt);
			}
			PTA2.updateRDT(imp, rdt);
		}

		private synchronized List<List<TrackPoint>> findlinkage(List<List<TrackPoint>> allp) {
			int num = allp.size();  // count total frames
			Calibration cal = imp.getCalibration();
			for (int ff = 0; ff < num - 1; ff++) {
				List<TrackPoint> fpointslist = allp.get(ff);  // points list of first frames
				List<TrackPoint> spointslist = allp.get(ff + 1); // points list of second frames
				IJ.log("frame = " + ff);
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
					if (!sp.np && distance <= searchrange) {
						sp.preTp = fp;
						fp.postTp = sp;
						continue;
					} 
					IJ.log("distance = " + distance);
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
			double roiInt = 0;
			double offset = 0;
			double cx = 0, cy = 0; // center of x,y
			double sx = 0, sy = 0; // sigma x,y
			int iteration = 0;
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
				
				if (methods == 4) { // Find maxima
					tp = new TrackPoint(x * cal.pixelWidth, y * cal.pixelHeight, val, currentframe, roisize);
					p++;
					continue;
				}
				
				Wand wand = new Wand(ip);
				wand.autoOutline((int)x, (int)y, lt, ut);
				Roi wandRoi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON);
				imp.setRoi(wandRoi);
				ImageStatistics is = imp.getStatistics(CENTROID + RECT);
				if (methods == 0) { // Centroid tracking
					is = imp.getStatistics(AREA + CENTROID + CIRCULARITY + MEAN);
					cx = is.xCentroid; cy = is.yCentroid;
					mean = is.mean;
					tp = new TrackPoint(cx, cy, is.area, mean, is.CIRCULARITY, currentframe, roisize);
				} else if (methods == 1) { //CENTER OF MASS
					is = imp.getStatistics(AREA + CENTER_OF_MASS + CIRCULARITY + MEAN);
					cx = is.xCenterOfMass; cy = is.yCenterOfMass;
					mean = is.mean;
					tp = new TrackPoint(cx, cy, is.area, mean, is.CIRCULARITY, currentframe, roisize);
				} else if (methods == 2) { //2D Gaussian
					FloatProcessor fip = ip.convertToFloatProcessor();
					float[] pixVal = (float[])fip.getPixels();
					is = imp.getStatistics(AREA + CENTROID + CIRCULARITY + MEAN);
					double xx = is.xCentroid - cal.pixelWidth * (double)roisize / 2.0D;
					double yy = is.yCentroid - cal.pixelHeight * (double)roisize / 2.0D;
					int ixx = (int)(xx / cal.pixelWidth);
					int iyy = (int)(yy / cal.pixelHeight);
					
					// if roi is out of image bound
					if(ixx < 0 || iyy < 0 ||(ixx + roisize) > imp.getWidth() || (iyy + roisize) > imp.getWidth())
					{
						p++;
						continue;
					}
					
					double[] inputdata = new double[roisize * roisize];
				
					for(int ii = 0;ii < roisize * roisize; ii++) {
						// x position is mod (count (ii), y number )
						// y position is count / x size number
						int ix = ii % roisize, iy = ii / roisize;
						double tmpval = (double)pixVal[ixx + ix + (iyy + iy) * imp.getWidth()];
						inputdata[ix + iy * roisize] = tmpval;
						roiInt += tmpval;
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
					
						cx = (double)ixx + optimalValues[1];
						cy = (double)iyy + optimalValues[2];
						mean = optimalValues[0];
						offset = optimalValues[5];
						sx = optimalValues[3];
						sy = optimalValues[4];
						iteration = opt.getIterations();
						//IJ.log("Iteration = "+iteration);
					} catch (Exception e) {
						//IJ.log("cx="+cx+", cy="+cy+", mean="+mean+", offset="+offset+", sx="+sx+", sy="+sy);
						//IJ.log(e.toString());
					}
					tp = new TrackPoint(cx, cy, sx, sy, is.area, mean, is.CIRCULARITY, currentframe, roisize, iteration);
				}
				tmpl.add(tp);
				p++;
			}while(p < mp.npoints);
			return tmpl;
		}
} 
