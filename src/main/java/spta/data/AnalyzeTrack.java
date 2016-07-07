/**
 * 
 */
package spta.data;

import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.measure.Calibration;

/**
 * @author araiyoshiyuki
 * 
 * 	final static String[] columnNames = {"#", "From", "To", "FrameLength", "Ave. Intensity", "Ave. Velocity", "RunLength", "Check", "Color"};
 */
public class AnalyzeTrack {

	private List<TrackPoint> track;
	private ImagePlus imp;
	private Calibration cal;
	public int startframe;
	public int endframe;
	public int framelength;
	public double aveInt;
	public double aveVel;
	public double runLen;
	public double[] timeseries;
	public double[] timeseriesm;
	public double[] timeseriesc;
	public double[] intensities;
	public double[] xtrack;
	public double[] ytrack;
	public double[] velocities;
	public double[] cost;
	public double[] objarea;
	public double[] msd;
	public double[] msdlag;
	
	public AnalyzeTrack(ImagePlus imp, List<TrackPoint> track) {
		this.imp = imp;
		this.cal = imp.getCalibration();
		this.track = track;
		
		double frameint;
		frameint = cal.frameInterval == 0?1:cal.frameInterval;
		
		// calculate track parameters
		this.startframe = track.get(0).frame;
		this.endframe = track.get(track.size() - 1).frame;
		this.framelength = endframe - startframe + 1;
		
		timeseries = new double[framelength];
		if (framelength > 1) {
			timeseriesm = new double[framelength - 1];
			velocities = new double[framelength - 1];		
		} else {
			velocities = new double[]{0};
			timeseriesm = new double[]{0};
		}
		if (framelength > 2) {
			timeseriesc = new double[framelength - 2];
			cost = new double[framelength - 2];
		} else {
			timeseriesc = new double[]{0, 0};
			cost = new double[]{0, 0};
		}
		intensities = new double[framelength];
		xtrack = new double[framelength];
		ytrack = new double[framelength];
		objarea = new double[framelength];
		
		aveInt = 0;
		for(int i = 0; i < framelength; i++) {
			timeseries[i] = (i + track.get(0).frame) * frameint;
			intensities[i] = track.get(i).mean;
			aveInt += track.get(i).mean;
			xtrack[i] = track.get(i).tx * cal.pixelWidth;
			ytrack[i] = track.get(i).ty * cal.pixelHeight;
			objarea[i] = track.get(i).area * cal.pixelHeight * cal.pixelWidth;
		}
		aveInt /= framelength; 
		runLen = 0;
		aveVel = 0;
		for(int i = 1, j = 0;i < framelength; i++, j++) {
			double length;
			length =  (track.get(j).tx - track.get(i).tx) * (track.get(j).tx - track.get(i).tx);
			length += (track.get(j).ty - track.get(i).ty) * (track.get(j).ty - track.get(i).ty);
			length = Math.sqrt(length);
			runLen += length;
			timeseriesm[j] = (i + track.get(0).frame) * frameint; 
			velocities[j] = length / frameint;
			aveVel = aveVel + velocities[j] / frameint;
		}
		if (track.size() > 2) {
			for(int i = 2, j = 1, k = 0;i < framelength; i++, j++, k++) {
				timeseriesc[k] = (i + track.get(0).frame) * frameint;
				cost[k] = TrackPoint.retCost(track.get(j), track.get(i));
			}
		}
		aveVel /= framelength;
		if (track.size() > 2) {
			CalcMSD cm = new CalcMSD(track, 2, imp.getCalibration());
			msd = cm.getMsdList(); // cal MSD if the length of track is more than 5
			msdlag = cm.getDFrame();
		} else {
			msd = new double[]{0}; // return 0 value 
			msdlag = new double[]{0};
		}
	}
}
