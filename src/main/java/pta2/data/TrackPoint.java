package pta2.data;

import ij.IJ;
import ij.measure.Calibration;

public class TrackPoint {

	public double tx, ty;
	public TrackPoint preTp;
	public int frame;
	public double mean;
	public double area;
	public double sx, sy; // sigma x, sigma y
	public int ite; // number of itteration
	public double circ;
	public int roisize;
	public double offset;
	
	public TrackPoint() {
		this.tx = 0;
		this.ty = 0;
	}

	public TrackPoint(double xCentroid, double yCentroid, double area, double mean, double circ, int frame, int roisize) {
		this.tx = xCentroid;
		this.ty = yCentroid;
		this.area = area;
		this.mean = mean;
		this.frame = frame;
		this.circ = circ;
		this.roisize = roisize;
	}
	
	public static double calcDistance(TrackPoint fp, TrackPoint sp, int[] param, int searchrange, Calibration cal) {
		/*
		 * fp: first point
		 * sp: second point
		 * param: parameter for calc distance
		 */
		double d = (fp.tx / cal.pixelWidth - sp.tx / cal.pixelWidth) * 
				(fp.tx / cal.pixelWidth - sp.tx / cal.pixelWidth) +
				(fp.ty / cal.pixelHeight - sp.ty / cal.pixelHeight) * 
				(fp.ty / cal.pixelHeight - sp.ty / cal.pixelHeight);
		if (Math.sqrt(d) > searchrange) {
			IJ.log("d = " + d);
			return 0;
		}
		d += param[0] * (fp.mean - sp.mean) * (fp.mean - sp.mean);
		d += param[1] * (fp.area - sp.area) * (fp.area - sp.area);
		if(fp.preTp != null && param[2] == 1) {
			d += (-1) * retCost(fp, sp);
			IJ.log("d = " + d);
		}
		d += param[3] * (fp.circ - sp.circ) * (fp.circ - sp.circ);
		return d;
	}
	
	public static double retCost(TrackPoint ap, TrackPoint bp) {
		double lenlen = Math.sqrt((ap.tx - bp.tx) * (ap.tx - bp.tx) + (ap.ty - bp.ty) * (ap.ty - bp.ty)) *
				Math.sqrt((ap.preTp.tx - ap.tx) * (ap.preTp.tx - ap.tx) + (ap.preTp.ty - ap.ty) * (ap.preTp.ty - ap.ty));
		return ((ap.tx - ap.preTp.tx) * (bp.tx - ap.tx) + (ap.ty - ap.preTp.ty) * (bp.ty - ap.ty)) / lenlen;
	}
	
	public String toString() {
		return String.format("frame:%d; (x, y)=(%f, %f)", frame , tx , ty);
	}
}
