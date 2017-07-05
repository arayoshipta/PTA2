/**
 * 
 */
package pta2;

import ij.plugin.frame.*;
import ij.process.*;
import pta2.data.*;
import pta2.gui.*;

import java.awt.Font;
import java.awt.Frame;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.List;

import ij.*;
import ij.gui.ImageCanvas;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.gui.ShapeRoi;
import ij.gui.TextRoi;
import ij.measure.Calibration;

/**
 * PTA2 is a main class of this plugins.
 * 
 * @author araiyoshiyuki
 */
public class PTA2 extends PlugInFrame {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	public static Frame frame = null;
	private static ImagePlus imp;
	private static ImageCanvas ic;
	private static Calibration cal;
	public static MainWindow mw;
	public static List<List<TrackPoint>> tracklist;
	public static int roisize = 12;
	public static int searchrange = 12;
	private static ResultDataTable rdt;
	private static ChartFrame cframe;
	public static boolean isTracking = false;
	public static ImageListener listener;
	public static int[] selectedlist;

	public PTA2() {
		super("PTA");
		// VersionCheck
		if (IJ.versionLessThan("1.43g"))
			return;
		if (frame != null){
			IJ.error("PTA2 is already implemented");
			return;
		}
		frame = this;

		// panel coordinate

		imp = WindowManager.getCurrentImage();
		// open the Threshold Adjuster
		ThresholdAdjuster ta = new ThresholdAdjuster();
		ta.setEnabled(true);
		IJ.setAutoThreshold(imp, "Triangle");
		ta.imageUpdated(imp);
		
		// if the image is already opened
		if (imp != null) {
			ImageProcessor ip = imp.getProcessor();
			mw = new MainWindow();
			mw.setVisible(true);
			tracklist = new ArrayList<List<TrackPoint>>(100);  // for data storage

			//to avoid automatic point detection
			//ip.setThreshold(-808080.0D, ip.getMax(), ImageProcessor.RED_LUT);
			ic = imp.getCanvas();
			cal = imp.getCalibration();
			WindowManager.addWindow(mw);
			ImagePlus.addImageListener(listener = new ImageListener() {

				@Override
				public void imageClosed(ImagePlus arg0) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void imageOpened(ImagePlus arg0) {
					imp = arg0;
					if (mw != null)
						mw.imp = arg0;
				}

				@Override
				public void imageUpdated(ImagePlus arg0) {
					if(selectedlist == null) {
						return;
					}					
					if(selectedlist.length < 1)
						return;
					if(arg0 != rdt.imp)
						return;
					imp = arg0;
					
					if (isTracking)
						return;
					
					Overlay tempol = new Overlay();
					int focusedlistlen = mw.isAllTrack()?tracklist.size():selectedlist.length;
					for(int slist = 0;slist < focusedlistlen;slist++) {
						List<TrackPoint> focusedlist;
						if(mw.isAllTrack())
							focusedlist = tracklist.get(slist);
						else
							focusedlist = tracklist.get(selectedlist[slist]);

						for(TrackPoint tp: focusedlist) {
							if (tp.frame == imp.getCurrentSlice()) {
								GeneralPath gp = new GeneralPath();
								if(mw.stateOfTrajectory) {
									for(int i = 0; i<focusedlist.size() - 1; i++) {
										TrackPoint fp = focusedlist.get(i);
										TrackPoint sp = focusedlist.get(i+1);
										gp.moveTo(fp.tx / cal.pixelWidth, fp.ty / cal.pixelHeight);
										gp.lineTo(sp.tx / cal.pixelWidth, sp.ty / cal.pixelHeight);
									}
								} else {
									for(int i = 0; i<focusedlist.indexOf(tp); i++) {
										gp.moveTo(focusedlist.get(i).tx / cal.pixelWidth, 
												focusedlist.get(i).ty / cal.pixelHeight);
										gp.lineTo(focusedlist.get(i + 1).tx / cal.pixelWidth, 
												focusedlist.get(i + 1).ty / cal.pixelHeight);
									}
								}
								ShapeRoi sr = new ShapeRoi(gp);
								sr.setStrokeColor(rdt.getDataofColor(tracklist.indexOf(focusedlist)));
								tempol.add(sr);
								if(mw.isNumTrack()) {
									Roi numroi;
									int rs = tp.roisize;
									numroi = new TextRoi((int)(tp.tx / cal.pixelWidth + rs / 2 + 2), 
											(int)(tp.ty / cal.pixelHeight - rs / 2), 
											String.valueOf(tracklist.indexOf(focusedlist)),
											new Font("SansSerif", Font.PLAIN, 10));
									numroi.setStrokeColor(rdt.getDataofColor(tracklist.indexOf(focusedlist)));
									tempol.add(numroi);
								}
								if(mw.isRoiTrack()) {
									Roi squareroi;
									int rs = tp.roisize;
									squareroi = new Roi((int)(tp.tx - rs / 2.0), (int)(tp.ty - rs / 2.0), rs, rs);
									squareroi.setStrokeColor(rdt.getDataofColor(tracklist.indexOf(focusedlist)));
									tempol.add(squareroi);
								}
							}
						}
					}
					imp.setOverlay(tempol);
					if (cframe != null) {
						cframe.setFrame(imp.getFrame()); // update crosshair in chartframe
						cframe.validate();
					}
				}
				
			});
		} else {
			IJ.error("There are no image.");
			frame = null;
		}
	}

	public static void setTrackList(List<List<TrackPoint>> tlist) {
		/*
		 * set to tracked list
		 * @param tracklist, tracklist
		 */
		tracklist = tlist;
	}
	
	public static List<List<TrackPoint>> getTrackList() {
		/*
		 * return current tracklist
		 * @return tracklist, current tracklist
		 */
		return tracklist;
	}
	
	public static void updateRDT(ImagePlus simp, ResultDataTable srdt) {
		/*
		 * update Result Data Table
		 * @param simp, set new ImagePlus
		 * @param srdt, set new ResultDataTable
		 */
		imp = simp;
		ic = imp.getCanvas();
		rdt = srdt;
	}
	
	public static void setlist(List<List<TrackPoint>> tlist, int[] slist) {
		/*
		 * update selected list of ResultTableData
		 * @param tracklist, tracklist
		 * @param slist, indices of selected rows
		 */
		tracklist = tlist;
		selectedlist = slist;
	}

	public static ResultDataTable getRDT() {
		/*
		 * @return return current ResultDataTable
		 */
		return rdt;
	}
	
	public static ChartFrame getcframe() {
		/*
		 * @return, current ChartFrame object
		 */
		return cframe;
	}
	
	public static void setcframe(ChartFrame cf) {
		/*
		 * @param cf, set new ChartFrame object
		 */
		cframe = cf;
	}
	
}
