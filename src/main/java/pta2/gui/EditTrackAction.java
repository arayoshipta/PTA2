/**
 * 
 */
package pta2.gui;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JTable;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.GenericDialog;
import pta2.PTA2;
import pta2.data.TrackPoint;

/**
 * This class implements edit menu for ResultDataTable
 * @author araiyoshiyuki
 *
 */
public class EditTrackAction extends AbstractAction {

	private ImagePlus imp;
	private ResultDataTable rdt;
	private JTable jt;
	private List<List<TrackPoint>> tracklist;

	public EditTrackAction(ImagePlus imp, ResultDataTable rdt) {
		this.imp = imp;
		this.rdt = rdt;
		this.jt = rdt.jt;
		this.tracklist = rdt.tracklist;
	}

	/* (non-Javadoc)
	 * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
	 */
	@Override
	public void actionPerformed(ActionEvent e) {
		
		AbstractButton b = (AbstractButton)e.getSource();

		if(b.getText() == "Delete Track") 
			deleteTrack();
		if(b.getText() == "Split Track") 
			splitTrack();
		if(b.getText() == "Concatenate Track")
			concatenateTrack();
	}

	private void concatenateTrack() {
		int index = jt.convertRowIndexToModel(jt.getSelectedRow());
		int[] selectedlist = jt.getSelectedRows();
		if(selectedlist.length != 2) {
			IJ.error("Two tracks should be selected!");
			return;
		}
		for (int ind = 0; ind < selectedlist.length; ind++) 
			selectedlist[ind] = jt.convertRowIndexToModel(selectedlist[ind]);
		List<TrackPoint> track1 = tracklist.get(selectedlist[0]);
		List<TrackPoint> track2 = tracklist.get(selectedlist[1]);
		if (track1.get(track1.size() - 1).frame >= track2.get(0).frame ||
				track1.get(0).frame >= track2.get(track2.size() - 1).frame) {
			IJ.error("These tracks are overlapped.\n");
			return;
		}
		GenericDialog concateRows = new GenericDialog("Concatenete rows");
		concateRows.addMessage("Do you want to concatenate track #" + selectedlist[0] + " and #" + selectedlist[1] + " tracks?");
		concateRows.addCheckbox("Interporate?", true);
		concateRows.enableYesNoCancel();
		concateRows.showDialog();
		boolean interp = concateRows.getNextBoolean();
		List<TrackPoint> concateTrack = new ArrayList<TrackPoint>(track1.size() + track2.size());
		List<TrackPoint> firsttrack, secondtrack;
		if(track1.get(0).frame < track2.get(0).frame) {
			firsttrack = track1;
			secondtrack = track2;
		} else {
			firsttrack = track2;
			secondtrack = track1;
		}
		
		for(TrackPoint ft: firsttrack) {
			concateTrack.add(ft);
		}
		
		if(interp) {
			TrackPoint lpft = firsttrack.get(firsttrack.size() - 1);  // lpft: last trackpoint of first track
			TrackPoint fpst = secondtrack.get(0);  // fpst: first trackpoint of second track
			int lfft = lpft.frame; //lfft: last frame of first track
			int ffst = fpst.frame; //ffst: first frame of second track
			int flen = ffst - lfft;
			double dx =  - (lpft.tx - fpst.tx) / (double)flen;
			double dy =  - (lpft.ty - fpst.ty) / (double)flen;
			double darea =  - (lpft.area - fpst.area) / (double)flen;
			double dmean =  - (lpft.mean - fpst.mean) / (double)flen;
			double dinte =  - (lpft.integint - fpst.integint) / (double)flen;
			double dcirc =  - (lpft.circ - fpst.circ) / (double)flen;
			TrackPoint preTp = concateTrack.get(concateTrack.size() - 1);
			for (int ip = 1;ip < flen;ip++) {
				//TrackPoint(double x, double y, double area, double mean, double circ, int frame, int roisize)
				TrackPoint inttp = new TrackPoint(lpft.tx + dx * ip, lpft.ty + dy * ip, lpft.area + darea * ip,
						lpft.mean + dmean * ip, lpft.integint + dinte * ip, 
						lpft.circ + dcirc * ip, lpft.frame + ip, lpft.roisize);
				inttp.preTp = preTp;  // link previous point to current point
				concateTrack.add(inttp);
				preTp = inttp;
			}
		}
		
		int cnt = 0;
		for(TrackPoint st: secondtrack) {
			if(cnt == 0) 
				st.preTp = concateTrack.get(concateTrack.size() - 1); // link last trackpoint of firsttrack to the first trackPoint of secondtract
			concateTrack.add(st);
			cnt++;
		}
		tracklist.add(concateTrack);
		rdt.setVisible(false);
		rdt.dispose(); // Destroy JFrame
		rdt = new ResultDataTable(tracklist, imp);
		PTA2.updateRDT(imp, rdt);
	}

	private void splitTrack() {
		int index = jt.convertRowIndexToModel(jt.getSelectedRow());
		int currentframe = imp.getFrame();
		List<TrackPoint> editTrack = tracklist.get(index);
		int startframe = editTrack.get(0).frame;
		//int endframe = editTrack.get(editTrack.size() - 1).frame;
		GenericDialog splitRow = new GenericDialog("Split Track?");
		splitRow.addMessage("Are you sure delete #" + index + " Track in this frame?");
		splitRow.enableYesNoCancel();
		splitRow.showDialog();
		if(splitRow.wasOKed()) {
			List<TrackPoint> formertrack = new ArrayList<TrackPoint>(editTrack.subList(0, currentframe - startframe + 1)); // Since ret of sublist is not sublist but original list.
			List<TrackPoint> lattertrack = new ArrayList<TrackPoint>(editTrack.subList(currentframe - startframe + 1, editTrack.size()));
			tracklist.remove(index);
			tracklist.add(formertrack);
			tracklist.add(lattertrack);
			rdt.setVisible(false);
			rdt.dispose(); // Destroy JFrame
			rdt = new ResultDataTable(tracklist, imp);
			PTA2.updateRDT(imp, rdt);
		}
	}

	private void deleteTrack() {
		int index = jt.convertRowIndexToModel(jt.getSelectedRow());
		GenericDialog deleteRow = new GenericDialog("Delete Track?");
		deleteRow.addMessage("Are you sure delete #" + index + " track?");
		deleteRow.enableYesNoCancel();
		deleteRow.showDialog();
		if(deleteRow.wasOKed()) {
			tracklist.remove(index);
			rdt.setVisible(false);
			rdt.dispose(); // Destroy JFrame
			rdt = new ResultDataTable(tracklist, imp);
			PTA2.updateRDT(imp, rdt);
		}
	}

}
