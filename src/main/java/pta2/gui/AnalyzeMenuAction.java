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
import pta2.data.TrackPoint;

/**
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

	public AnalyzeMenuAction(ImagePlus imp, ResultDataTable rdt) {
		this.imp = imp;
		this.rdt = rdt;
		this.jt = rdt.jt;
		this.cal = imp.getCalibration();	
		this.numlen = 0;
	}
	
	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		AbstractButton b = (AbstractButton)e.getSource();

		selectedlist = jt.getSelectedRows();

		if (selectedlist.length == 0) {
			numlen = rdt.tracklist.size();
		} else {
			numlen = selectedlist.length;
		}
		IJ.log("numlen = " + numlen);
		
		for (int ind = 0; ind < numlen; ind++) 
			selectedlist[ind] = jt.convertRowIndexToModel(selectedlist[ind]);
		
		if(b.getText() == "Scatter Plot") 
			scatterplot();
		if(b.getText() == "Show multi-Z intensities") 
			showmultizint();
	}

	private void showmultizint() {
		GenericDialog gd = new GenericDialog("Show multi-Z intensities");
		gd.addMessage("Show intensities at fixed position");
		gd.addCheckbox("is only use points in first frame?", false);
		gd.enableYesNoCancel();
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		boolean isff = gd.getNextBoolean();
		int tflen = imp.getNFrames();
		int rsize = PTA2.roisize;
		ResultsTable rt = new ResultsTable();
		for(int f = 1; f < tflen; f++) {
			imp.setT(f);
			rt.incrementCounter();
			for(int rows = 0; rows < numlen; rows++) {
				List<TrackPoint> templist = rdt.tracklist.get(selectedlist[rows]);
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
		rt.show("Multi Track Data");
	}

	private synchronized void scatterplot() {
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
