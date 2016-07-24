package pta2.data;

import org.apache.commons.math3.ml.distance.EuclideanDistance;

import ij.IJ;
import ij.measure.Calibration;

public class TrackPoint {

	public double tx, ty;
	public TrackPoint preTp;
	public TrackPoint postTp;
	public int frame;
	public double mean;
	public double integint;
	public double area;
	public double sx = 0, sy = 0; // sigma x, sigma y
	public int ite = 0; // number of itteration
	public double circ;
	public int roisize;
	public double offset = 0;
	public boolean np = false;
	
	public TrackPoint() {
		this.tx = 0;
		this.ty = 0;
		this.np = true;
	}
	
	public TrackPoint(double x, double y, double peakIntensity, double integint, int frame, int roisize) {
		this.tx = x;
		this.ty = y;
		this.mean = peakIntensity;
		this.integint = integint;
		this.frame = frame;
		this.circ = 1;
		this.roisize = roisize;
		this.area = roisize * roisize;
	}

	public TrackPoint(double x, double y, double area, double mean, double integint, double circ, int frame, int roisize) {
		this.tx = x;
		this.ty = y;
		this.area = area;
		this.mean = mean;
		this.integint = integint;
		this.frame = frame;
		this.circ = circ;
		this.roisize = roisize;
	}
	
	public TrackPoint(double x, double y, double sigmax, double sigmay, 
			double area, double mean, double integint, double offset, double circ, int frame, int roisize, int iteration) {
		this.tx = x;
		this.ty = y;
		this.sx = sigmax;
		this.sy = sigmay;
		this.area = area;
		this.mean = mean;
		this.integint = integint;
		this.frame = frame;
		this.circ = circ;
		this.roisize = roisize;
		this.ite = iteration;
	}
	
	public static double calcDistance(TrackPoint fp, TrackPoint sp, int[] param, Calibration cal) {
		/*
		 * fp: first point
		 * sp: second point
		 * param: parameter for calc distance
		 */
		EuclideanDistance ed = new EuclideanDistance();  // calculate euclideanDistance
		double d = ed.compute(new double[]{fp.tx, fp.ty}, new double[]{sp.tx, sp.ty});
		d = d / cal.pixelWidth;  // convert to pixel value

		d += param[0] * Math.sqrt((fp.mean - sp.mean) * (fp.mean - sp.mean));
		d += param[1] * Math.sqrt((fp.area - sp.area) * (fp.area - sp.area));
		if(fp.preTp != null && param[2] == 1) {
			d += (-1) * retCost(fp, sp);
			IJ.log("d = " + d);
		}
		d += param[3] * Math.sqrt((fp.circ - sp.circ) * (fp.circ - sp.circ));
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
