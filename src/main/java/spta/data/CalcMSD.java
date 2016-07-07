package spta.data;
import ij.IJ;
import ij.measure.Calibration;

import java.util.ArrayList;
import java.util.List;


public class CalcMSD {

	private ArrayList<Double> msdList;
	private ArrayList<Double> dframe;

	public CalcMSD(final List<TrackPoint> pointlist, final int leastLength, Calibration cal) {
		try {
			if (pointlist.size()<leastLength) 
				return;
			int calcFrameLen = leastLength;
			double frameint = cal.frameInterval == 0?1:cal.frameInterval;
			
			calcFrameLen = calcFrameLen>pointlist.get(pointlist.size() - 1).frame - pointlist.get(0).frame?
			pointlist.get(pointlist.size() - 1).frame - pointlist.get(0).frame:calcFrameLen;
			calcFrameLen = calcFrameLen<3?3:calcFrameLen;

			msdList = new ArrayList<Double>(0);
			dframe = new ArrayList<Double>(0);

			int n = pointlist.size();
			for(int k = 1; k <= pointlist.get(pointlist.size() - 1).frame; k++) { // shift value of k
					double len = 0D;
					int cnt = 0;	// count for data division
					for(int j = 0; j < n; j++) {
						TrackPoint fp = pointlist.get(j);
						int l = 1;
						// the second point index of j+l must be less than the length of the pointlist
						while((j + l) < n) {
							TrackPoint sp = pointlist.get(j + l);
							if(sp.frame == fp.frame + k) {
								len += ((sp.tx - fp.tx) * (sp.tx - fp.tx) + (sp.ty - fp.ty) * (sp.ty - fp.ty));
								cnt++;
								break;
							}
							l++;
						}
					}
					if(cnt != 0) {
						msdList.add(new Double(len / cnt));
						dframe.add(new Double(k * frameint));
					}
			}
		} catch (Exception e) {
			IJ.log("calcMsd:" + e.toString());
		}
	}
	public double[] getMsdList() {
		double[] retd = new double[msdList.size()];
		int i=0;
		try {
			for(Double dval:msdList) {
				retd[i] = dval.doubleValue();
				i++;
			}
		} catch (Exception e) {
			IJ.log("getMsdList:" + e.toString());
		}
		return retd;
	}
	public double[] getDFrame() {
		double[] retd = new double[dframe.size()];
		int i=0;
		try {
			for(Double dval:dframe) {
				retd[i] = dval.doubleValue();
				i++;
			}
		} catch (Exception e) {
			IJ.log("getDFrame:" + e.toString());
		}
		return retd;
	}
}
