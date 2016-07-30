/**
 * 
 */
package pta2.gui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JTable;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.gui.ProgressBar;
import ij.gui.Roi;
import ij.measure.Calibration;
import ij.measure.Measurements;
import ij.measure.ResultsTable;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;
import pta2.PTA2;
import pta2.data.AnalyzeTrack;
import pta2.data.TrackPoint;
import pta2.track.MultiTrackObjects;

/**
 * This class is used for ResultDataTable
 * @author araiY
 *
 */
public class AnalyzeMenuAction extends AbstractAction implements Measurements {

	private ImagePlus imp;
	private ResultDataTable rdt;
	private Calibration cal;
	private JTable jt;
	private int[] selectedlist;
	private int numlen;
	private boolean batchmode = false;
	private List<List<TrackPoint>> tracklist;
	private String directoryloc = "";
	

	public AnalyzeMenuAction(ImagePlus imp, ResultDataTable rdt) {
		this.imp = imp;
		this.rdt = rdt;
		this.tracklist = rdt.tracklist;
		this.jt = rdt.jt;
		this.cal = imp.getCalibration();	
		this.numlen = 0;
		this.batchmode = batchmode;
	}
	
	public AnalyzeMenuAction(ImagePlus imp, List<List<TrackPoint>> tracklist, boolean batchmode, String directoryloc) {
		this.imp = imp;
		this.tracklist = tracklist;
		this.numlen = tracklist.size();
		this.batchmode = batchmode;
		this.directoryloc  = directoryloc;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		AbstractButton b = (AbstractButton)e.getSource();

		if(!batchmode) {
			selectedlist = jt.getSelectedRows();
	
			if (selectedlist.length == 0) {
				numlen = rdt.tracklist.size();
			} else {
				numlen = selectedlist.length;
			}
			for (int ind = 0; ind < numlen; ind++) 
				selectedlist[ind] = jt.convertRowIndexToModel(selectedlist[ind]);
		}
		
		if(b.getText() == "Scatter Plot") 
			scatterplot();
		if(b.getText() == "Show multi-Z intensities") 
			showmultizint();
		if(b.getText() == "Fitting by 2DGaussian")
			fitbytd();
	}

	public synchronized void fitbytd() {
		/*
		 * Fitting current track by two dimensional Gaussian
		 */
		GenericDialog gd = new GenericDialog("Fitting by 2D Gaussian");
		gd.addMessage("Do you want to perform 2D Gaussian Fitting?");
		gd.enableYesNoCancel();
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		for(int rows = 0; rows < numlen; rows++) {
			List<TrackPoint> tmplist = rdt.tracklist.get(selectedlist[rows]);
			for(TrackPoint tp: tmplist) {
				tp = MultiTrackObjects.twoDGFit(imp, tp);
			}
			AnalyzeTrack at = new AnalyzeTrack(imp, tmplist);
			jt.setValueAt((Double)at.aveInt, rows, 4);  // average intensity
			jt.setValueAt((Double)at.aveVel, rows, 5);  // average velocity
			jt.setValueAt((Double)at.runLen, rows, 6);  // runlength
		}
		ChartFrame cf = PTA2.getcframe();
		cf.drawTrajectory(rdt.tracklist.get(selectedlist[0]));
		imp.deleteRoi();
	}
	
	public void showmultizint() {
		/*
		 * Show multi Z-axis profiler
		 * This method will also called from PTA2Dialog
		 */
		boolean isff = false;
		if(!batchmode) {
			GenericDialog gd = new GenericDialog("Show multi-Z intensities");
			gd.addMessage("Show intensities at fixed position");
			gd.addCheckbox("is only use points in first frame?", false);
			gd.enableYesNoCancel();
			gd.showDialog();
			if(gd.wasCanceled())
				return;
			isff = gd.getNextBoolean();
		}
		int tflen = imp.getNFrames();
		int rsize = PTA2.roisize;
		ResultsTable rt = new ResultsTable();
		for(int f = 1; f < tflen; f++) {
			imp.setT(f);
			rt.incrementCounter();
			for(int rows = 0; rows < numlen; rows++) {
				List<TrackPoint> templist = tracklist.get(rows);
				if(!batchmode) {
					templist = rdt.tracklist.get(selectedlist[rows]);
				} 
				TrackPoint fp = templist.get(0);
				if (isff && fp.frame != 1)
					continue;  // only use points in first frame
				Roi r = new Roi((int)(fp.tx - rsize / 2), (int)(fp.ty - rsize / 2), rsize, rsize);
				imp.setRoi(r);
				ImageStatistics is = imp.getStatistics(MEAN + INTEGRATED_DENSITY);
				double integint = is.mean * is.longPixelCount;
				rt.addValue("Track:" + rows, integint);
			}
		}
		if(!batchmode)
			rt.show("Multi Track Data");
		else {
			String fn = directoryloc + imp.getOriginalFileInfo().fileName + "fixedloc.csv";
			IJ.log(fn);
			rt.save(fn);
		}
	}

	private synchronized void scatterplot() {
		/*
		 * Make Scatter plot
		 */
		GenericDialog gdsplot = new GenericDialog("Scatter plot");
		gdsplot.addMessage("Scatter plot");
		gdsplot.addNumericField("Scale", 1.0, 2);
		gdsplot.addCheckbox("Binary?", false);
		gdsplot.enableYesNoCancel();
		gdsplot.showDialog();
		if(gdsplot.wasCanceled())
			return;
		int w = imp.getWidth();
		int h = imp.getHeight();
		double scale = gdsplot.getNextNumber();
		boolean binary = gdsplot.getNextBoolean();
		ImagePlus sp = IJ.createImage("Scatter Plot", (int)(w * scale), (int)(h * scale), 1, 32);
		Calibration ncal = new Calibration();
		ncal.pixelHeight = cal.pixelHeight / scale;
		ncal.pixelWidth = cal.pixelWidth / scale;
		ncal.frameInterval = cal.frameInterval;
		ncal.setXUnit(cal.getXUnit());
		ncal.setYUnit(cal.getYUnit());
		ncal.setTimeUnit(cal.getTimeUnit());
		sp.show();
		ImageProcessor ip = sp.getProcessor();
		
		for(int rows = 0; rows < numlen; rows++) {
			List<TrackPoint> templist = rdt.tracklist.get(selectedlist[rows]);
			for(TrackPoint tp: templist) {
				int x = (int)(tp.tx / ncal.pixelWidth);
				int y = (int)(tp.ty / ncal.pixelHeight);
				double val = ip.getPixelValue(x, y);
				ip.putPixelValue(x, y, (binary?1.0:tp.mean) + val);
				sp.updateAndDraw();
			}
		}
		IJ.showProgress(1.0);  
	}

}
