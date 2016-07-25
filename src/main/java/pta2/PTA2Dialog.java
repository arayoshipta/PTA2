/**
 * 
 */
package pta2;

import java.util.ArrayList;
import java.util.List;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import ij.plugin.filter.PlugInFilter;
import ij.process.ImageProcessor;
import pta2.data.TrackPoint;
import pta2.gui.AnalyzeMenuAction;
import pta2.gui.SaveDataAction;
import pta2.track.MultiTrackObjects;

/**
 * @author araiyoshiyuki
 *
 */
public class PTA2Dialog implements PlugInFilter {

	public String[] methods = new String[]{"Find Maxima", "Centroid", "Center of Mass", "2D Gaussian"};
	public ImagePlus imp;
	public List<List<TrackPoint>> tracklist = new ArrayList<List<TrackPoint>>(100);
	
	@Override
	public void run(ImageProcessor arg0) {
		ArrayList<String> met = new ArrayList<String>(4);
		for(String s: methods)
			met.add(s);
		if(imp.getStackSize() != imp.getNFrames())
			imp.setDimensions(1, 1, imp.getStackSize());
		IJ.log("directory is = " + imp.getOriginalFileInfo().directory);
		GenericDialog gd = new GenericDialog("PTA2 Dialog");
		gd.addMessage("Parameters");
		gd.addChoice("Methods", methods, "Centroid");
		gd.addNumericField("Tol", 40, 1);
		gd.addNumericField("roisize", 12, 1);
		gd.addNumericField("Search range", 3, 1);
		gd.addCheckbox("Intensity", false);
		gd.addCheckbox("Size", false);
		gd.addCheckbox("Angle", false);
		gd.addCheckbox("Circularity", false);
		gd.addStringField("Filename", imp.getOriginalFileInfo().directory);
		gd.addCheckbox("Save fixed-loc data?", false);
		gd.showDialog();
		if(gd.wasCanceled())
			return;
		
		String m = gd.getNextChoice();
		double tol = gd.getNextNumber();
		int roisize = (int)gd.getNextNumber();
		int searchrange = (int)gd.getNextNumber();
		int[] param = new int[]{gd.getNextBoolean()?1:0, gd.getNextBoolean()?1:0, gd.getNextBoolean()?1:0, gd.getNextBoolean()?1:0};
		String directory = gd.getNextString();
		boolean sfl = gd.getNextBoolean();
		IJ.log("methods = " + m + ", Tol. = " + tol);
		int metnum = met.indexOf(m);
		PTA2.roisize = roisize;
		//MultiTrackObjects(ImagePlus imp, int methods, int param[], double tol, int roisize,
		//SpinnerNumberModel searchrange, List<List<TrackPoint>> tracklist, boolean batchmode)
		MultiTrackObjects mto = new MultiTrackObjects(imp, metnum, param, tol, roisize, searchrange, tracklist, true);
		mto.start();
		try {
			mto.join();
		} catch (InterruptedException e) {
			IJ.log(e.toString());
		}
		tracklist = PTA2.tracklist;
		IJ.log("size = " + tracklist.size());
		SaveDataAction sda = new SaveDataAction(imp, tracklist, true, directory);
		sda.saveAll();
		if(sfl) {
			AnalyzeMenuAction ama = new AnalyzeMenuAction(imp, tracklist, true, directory);
			ama.showmultizint();
		}
		tracklist = null;
	}

	@Override
	public int setup(String arg0, ImagePlus imp) {
		this.imp = imp;
		return DOES_8G + DOES_16 + DOES_32;
	}

}
