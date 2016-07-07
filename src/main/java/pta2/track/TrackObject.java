/**
 * 
 */
package pta2.track;

import java.awt.Color;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import javax.swing.SpinnerNumberModel;

import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer.Optimum;

import ij.*;
import ij.gui.*;
import ij.measure.*;
import ij.process.*;
import pta2.PTA2;
import pta2.data.TrackPoint;
import pta2.gui.MainWindow;
import pta2.gui.ResultDataTable;

/**
 * @author araiyoshiyuki
 *
 */
public class TrackObject extends Thread implements Measurements{
	
	private ImagePlus imp;
	private int method;
	private int[] param;
	private int roisize;
	private int searchrange;
	private double cx,cy; // current x,y
	private double lt, ut;
	private TrackPoint preTp = null;
	private List<List<TrackPoint>> tracklist;
	private Calibration cal;
	private static ResultDataTable rdt;
	private MainWindow mw;

	public TrackObject(ImagePlus imp, MainWindow mw) {
		this.imp =  imp;
		this.mw = mw;
	}
	
	public TrackObject(ImagePlus imp, int method) {
		this.imp = imp;
		this.method = method;
	}
	
	public TrackObject(ImagePlus imp, Roi selRoi, int method, int[] param, SpinnerNumberModel roisize,
			SpinnerNumberModel searchrange, List<List<TrackPoint>> tracklist) {
		this.imp = imp;
		this.method = method;
		this.param = param;
		this.roisize = (Integer)roisize.getValue();
		this.searchrange = (Integer)searchrange.getValue();
		this.cal = imp.getCalibration();
		this.tracklist = tracklist;
		ImageProcessor ip = imp.getProcessor();
		lt = Math.round(ip.getMinThreshold());
		if(imp.getBitDepth()!=32)
			ut = Math.round(ip.getMaxThreshold());
		else
			ut = 65535;
	}

	public synchronized TrackPoint detectObject() {
		ImageStatistics is = imp.getStatistics();
		int currentframe = imp.getFrame();
		double mean = 0;
		double roiInt = 0;
		double offset = 0;
		double sx = 0, sy = 0;
		int itteration = 0;
		
		TrackPoint tp;
		if (method == 0) { // Centroid tracking
			is = imp.getStatistics(AREA + CENTROID + CIRCULARITY + MEAN);
			cx = is.xCentroid; cy = is.yCentroid;
			mean = is.mean;
			tp = new TrackPoint(cx, cy, is.area, mean, is.CIRCULARITY, currentframe, roisize);

		} else if (method == 1) { //CENTER OF MASS
			is = imp.getStatistics(AREA + CENTER_OF_MASS + CIRCULARITY + MEAN);
			cx = is.xCenterOfMass; cy = is.yCenterOfMass;
			mean = is.mean;
			tp = new TrackPoint(cx, cy, is.area, mean, is.CIRCULARITY, currentframe, roisize);

		} else if (method == 2) { //2D Gaussian
			ImageProcessor ip = imp.getProcessor();
			FloatProcessor fip = ip.convertToFloatProcessor();
			float[] pixVal = (float[])fip.getPixels();
			is = imp.getStatistics(AREA + CENTROID + CIRCULARITY + MEAN);
			double xx = is.xCentroid - cal.pixelWidth * (double)roisize / 2.0D;
			double yy = is.yCentroid - cal.pixelHeight * (double)roisize / 2.0D;
			int ixx = (int)(xx / cal.pixelWidth);
			int iyy = (int)(yy / cal.pixelHeight);
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
				(double)roiInt,			// intensity
				(double)roisize / 2D,	// x
				(double)roisize / 2D,	// y
				(double)roisize / 10D,	// sigma x
				(double)roisize / 10D,	// sigma y
				(double)is.min			// offset
				
			};
			TwoDGaussProblem tdgp = new TwoDGaussProblem(inputdata, newStart, roisize, new int[] {1000,100});
			
			try{
				//do LevenbergMarquardt optimization and get optimized parameters
				Optimum opt = tdgp.fit2dGauss();
				final double[] optimalValues = opt.getPoint().toArray();
				
				//output data
				/*
				IJ.log("Intensity: " + optimalValues[0]);
				IJ.log("x: " + optimalValues[1]);
				IJ.log("y: " + optimalValues[2]);
				IJ.log("sx: " + optimalValues[3]);
				IJ.log("sy: " + optimalValues[4]);
				IJ.log("offset: " + optimalValues[5]);
				IJ.log("Iteration number: "+opt.getIterations());
				IJ.log("Evaluation number: "+opt.getEvaluations());
				*/
				cx = (double)ixx + optimalValues[1];
				cy = (double)iyy + optimalValues[2];
				mean = optimalValues[0];
				offset = optimalValues[5];
				sx = optimalValues[3];
				sy = optimalValues[4];
				itteration = opt.getIterations();
			} catch (Exception e) {
				IJ.log(e.toString());
			}

		}
		tp = new TrackPoint(cx, cy, is.area, mean, is.CIRCULARITY, currentframe, roisize);
		tp.sx = sx;
		tp.sy = sy;
		tp.ite = itteration;
		tp.preTp = preTp;
		return tp;		
	}
	
	public boolean findnext(TrackPoint cp) {
		if (imp.getFrame() == imp.getNFrames()) {
			IJ.log("1. Last frame");
			return false;
		}
		imp.setT(imp.getFrame() + 1); // move to next frame
		ImageProcessor ip = imp.getProcessor();
		FloatProcessor fip = ip.convertToFloatProcessor();
		double fval = Float.intBitsToFloat(fip.getPixel((int)(cp.tx / cal.pixelWidth), (int)(cp.ty / cal.pixelHeight)));
		
		if (fval >= lt && fval <= ut) {
			Wand wand = new Wand(ip);
			wand.autoOutline((int)(cp.tx / cal.pixelWidth), (int)(cp.ty / cal.pixelHeight), lt, ut);
			Roi wandRoi = new PolygonRoi(wand.xpoints, wand.ypoints, wand.npoints, Roi.POLYGON);
			imp.setRoi(wandRoi);
		} else {
			IJ.log("search candidate. searchrange = " + searchrange);
			// find next candidate of objects by scanning
			List<TrackPoint> templist = retCandidate();
			// evaluate each trackpoint candidate
			double score = 10000000.0D;
			TrackPoint nexttp = null;
			for(TrackPoint candtp: templist) {
				double tempscore = TrackPoint.calcDistance(cp, candtp, param, searchrange, cal);
				if (tempscore == 0) {
					IJ.log("3. search failed");
					return false;
				}
				if (tempscore < score) {
					score = tempscore;
					nexttp = candtp;
				}
			}
			if (nexttp == null) {
				IJ.log("4. No Candidate");
				IJ.log(cp.toString());
				IJ.log("score = " + score);
				return false;
			}
			else {
				cx = nexttp.tx; cy = nexttp.ty;
				Wand wand = new Wand(ip);
				wand.autoOutline((int)cx, (int)cy, lt, ut);
				Roi wandRoi = new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
				imp.setRoi(wandRoi);
				return true;
			}
		}
		return true;
	}
	
	public void run() {
		int totalframe = imp.getNFrames();
		List<TrackPoint> track = new ArrayList<TrackPoint>(100);
		tracklist = PTA2.getTrackList();
		Overlay ol;
		
		if(imp.getOverlay() == null) 
			ol = new Overlay();
		else
			ol = imp.getOverlay();
			
		int startFrame = imp.getFrame();
		int c = 0;
		int f = startFrame;
		do{
			TrackPoint temptp = detectObject(); // detect object by current roi
			temptp.preTp = preTp;
			track.add(temptp);
			if(!findnext(temptp)) {// if findnext() cannot find the next object
				IJ.log("Can't find next object");
				break;
			}
			if (c > 1) {
				GeneralPath gp = new GeneralPath();
				gp.moveTo(track.get(c - 1).tx / cal.pixelWidth, track.get(c - 1).ty / cal.pixelHeight);
				gp.lineTo(track.get(c).tx / cal.pixelWidth, track.get(c).ty / cal.pixelHeight);
				ShapeRoi sr = new ShapeRoi(gp);
				sr.setStrokeColor(Color.cyan);
				ol.add(sr);
				imp.setOverlay(ol);
			}
			preTp = temptp;			
			f++;
			c++;
		} while (f <= totalframe);
		
		imp.setOverlay(ol);
		tracklist.add(track);
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
		imp.updateAndDraw();
		IJ.log("Tracking ends");
	}
	
	public List<TrackPoint> retCandidate() {
		ImageProcessor ip = imp.getProcessor();
		FloatProcessor fip = ip.convertToFloatProcessor();
		
		lt = Math.round(ip.getMinThreshold());
		if(imp.getBitDepth()!=32)
			ut = Math.round(ip.getMaxThreshold());
		else
			ut = 65535;
		
		int sx = ((int)cx - searchrange / 2)<0?0:((int)cx - searchrange / 2);
		int sy = ((int)cy - searchrange / 2)<0?0:((int)cy - searchrange / 2);
		int ex = ((int)cx + searchrange / 2)>ip.getWidth()?ip.getWidth():((int)cx + searchrange / 2);
		int ey = ((int)cy + searchrange / 2)>ip.getHeight()?ip.getHeight():((int)cy + searchrange / 2);
		List<TrackPoint> templist = new ArrayList<TrackPoint>(5);
		byte[] mask = new byte[searchrange * searchrange];
		TrackObject to = new TrackObject(imp, mw);
		for(int dx = sx, ax = 0;dx<ex;dx++, ax++) {
			for(int dy = sy, ay = 0;dy<ey;dy++, ay++) {
				double val = Float.intBitsToFloat(fip.getPixel(dx, dy));
				if(val >= lt && val <= ut && mask[ax + searchrange * ay] == 0) {
					Wand wand = new Wand(ip);
					wand.autoOutline(dx, dy, lt, ut);
					Roi wandRoi = new PolygonRoi(wand.xpoints,wand.ypoints,wand.npoints,Roi.POLYGON);
					imp.setRoi(wandRoi);
					templist.add(to.detectObject());
					// register mask data
					for(int mx = sx, bx = 0;mx<ex;mx++, bx++) {
						for(int my = sy, by = 0;my<ey;my++, by++) {
							if(wandRoi.contains(mx, my))
								mask[bx + by * searchrange] = (byte)255;
						}
					}
				}
			}
		}
		return templist;
	}
}
