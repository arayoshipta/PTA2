/**
 * 
 */
package pta2.gui;

import javax.swing.JFrame;

import ij.*;
import ij.gui.*;
import ij.plugin.filter.MaximumFinder;
import ij.process.*;
import pta2.PTA2;
import pta2.data.TrackPoint;
import pta2.track.MultiTrackObjects;

import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Polygon;

import javax.swing.JButton;
import javax.swing.JPanel;
import java.awt.FlowLayout;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;
import javax.swing.border.BevelBorder;
import java.awt.Color;
import javax.swing.border.EtchedBorder;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JRadioButton;
import java.awt.Font;
import javax.swing.ButtonGroup;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import java.util.Properties;
import java.awt.event.ActionEvent;
import java.awt.Rectangle;
import javax.swing.BoxLayout;

/**
 * Create Main window frame
 * @author araiyoshiyuki
 *
 */
public class MainWindow extends JFrame {
	private final ButtonGroup methodButtonGroup = new ButtonGroup();
	private final ButtonGroup roiButtonGroup = new ButtonGroup();
	
	public SpinnerNumberModel roisize;
	public SpinnerNumberModel searchrange;
	public SpinnerNumberModel tol;
	public int method;
	public boolean stateOfTrajectory = true;

	public ImagePlus imp;

	private JCheckBox chckbx_Intensity;
	private JCheckBox chckbx_Size;
	private JCheckBox chckbx_Angle;
	private JCheckBox chckbx_Circularity;
	private JCheckBox AllCheckBox;
	private JCheckBox ROICheckBox;
	private JCheckBox NumberCheckBox;
		
	public MainWindow() {
		setBounds(new Rectangle(500, 220, 550, 250));
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
				
		setTitle("PTA2");
		setResizable(false);
		getContentPane().setLayout(new GridLayout(1, 2, 0, 0));
		
		JPanel command_panel = new JPanel();
		getContentPane().add(command_panel);
		command_panel.setLayout(new GridLayout(2, 1, 0, 0));
		
		JPanel TrackPanel = new JPanel();
		command_panel.add(TrackPanel);
		TrackPanel.setLayout(new GridLayout(1, 2, 0, 0));
		
		JPanel panel = new JPanel();
		TrackPanel.add(panel);
		panel.setLayout(new GridLayout(3, 1, 0, 0));
		
		JButton PreviewButton = new JButton("Preview");
		panel.add(PreviewButton);
		PreviewButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				imp = WindowManager.getCurrentImage();
				MaximumFinder mf = new MaximumFinder();
				Polygon mp = mf.getMaxima(imp.getProcessor(), (Double)tol.getValue(), true);
				DrawRois dr = new DrawRois(imp, mp, (Integer)roisize.getValue());
				PTA2.isTracking = true;
				dr.show();
				PTA2.isTracking = false;
			}
			
		});
		
		JButton DoTrackButton = new JButton("Multi Track");
		DoTrackButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				imp = WindowManager.getCurrentImage();
				Roi currentRoi = imp.getRoi();
				int roitype = 0;
				if (currentRoi != null)
					roitype = currentRoi.getType();
				
				int[] param = new int[4];

				param = retParam();
				
				if(imp.getDimensions()[2] != 1) {
					IJ.error("only single channel image");
					return;
				}
				
				// convert slice to frame
				if(imp.getStackSize() != imp.getNFrames()) {
					GenericDialog yesnostack = new GenericDialog("Frame or Slice?");
					yesnostack.addMessage("Total Slice size: " + imp.getNSlices() + " is not equal "
							+ "to the total stack size: "+ imp.getStackSize() + " ");
					yesnostack.addMessage("Is it ok to convert slice to stack?");
					yesnostack.enableYesNoCancel();
					yesnostack.showDialog();
					if(yesnostack.wasOKed()) {
						imp.setDimensions(1, 1, imp.getStackSize());
					}
				}
				int startframe = 1;
				// whether mutli-track starts from the first frame or not
				if(imp.getFrame() != 1) {
					GenericDialog gd = new GenericDialog("Start frame");
					gd.addMessage("Do you want to start multitrack from curret frame?");
					gd.enableYesNoCancel("Current", "First");
					gd.showDialog();
					if(gd.wasOKed())
						startframe = imp.getFrame();
				}
				
				if (roitype == Roi.RECTANGLE && (imp.getSlice() != imp.getStackSize())) { // i.e Roi is wand
					GenericDialog yesnotrack = new GenericDialog("Track?");
					yesnotrack.addMessage("Track all object?");
					yesnotrack.enableYesNoCancel();
					yesnotrack.showDialog();
					if(yesnotrack.wasOKed()) {
						MultiTrackObjects mto = new MultiTrackObjects(imp, startframe, method, param, 
								(Double)tol.getValue(), (Integer)roisize.getValue(), (Integer)searchrange.getValue(),
								PTA2.tracklist, false);
						mto.start();
					}
				}
			}
		});
		panel.add(DoTrackButton);
		
		JPanel TolerancePanel = new JPanel();
		panel.add(TolerancePanel);
		TolerancePanel.setLayout(new GridLayout(1, 2, 0, 0));
		
		JLabel LabelTol = new JLabel("Tol.");
		LabelTol.setToolTipText("Noise Tolerance for find maxima");
		TolerancePanel.add(LabelTol);

		tol = new SpinnerNumberModel(40D, 0D, 100D, 0.1D); //
		JSpinner Tol = new JSpinner(tol);
		TolerancePanel.add(Tol);
		
		JPanel DetectionPanel = new JPanel();
		TrackPanel.add(DetectionPanel);
		DetectionPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null), "Localization", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		DetectionPanel.setLayout(new GridLayout(4, 1, 0, 0));
		
		JRadioButton FindMaxima_RadioButton = new JRadioButton("Find Maxima");
		DetectionPanel.add(FindMaxima_RadioButton);
		FindMaxima_RadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				method = 4;
			}
		});
		methodButtonGroup.add(FindMaxima_RadioButton);
		FindMaxima_RadioButton.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		
		JRadioButton Centroid_RadioButton = new JRadioButton("Centroid");
		Centroid_RadioButton.setSelected(true);
		DetectionPanel.add(Centroid_RadioButton);
		Centroid_RadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				method = 0;
			}
		});
		methodButtonGroup.add(Centroid_RadioButton);
		Centroid_RadioButton.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		
		JRadioButton CenterOfMass_RadioButton = new JRadioButton("CeterOfMass");
		DetectionPanel.add(CenterOfMass_RadioButton);
		CenterOfMass_RadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				method = 1;
			}
		});
		methodButtonGroup.add(CenterOfMass_RadioButton);
		CenterOfMass_RadioButton.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		
		JRadioButton Gaussian_RadioButton = new JRadioButton("2D Gaussian");
		DetectionPanel.add(Gaussian_RadioButton);
		Gaussian_RadioButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				method = 2;
			}
		});
		methodButtonGroup.add(Gaussian_RadioButton);
		Gaussian_RadioButton.setFont(new Font("Lucida Grande", Font.PLAIN, 13));
		
		JPanel AppearancePanel = new JPanel();
		command_panel.add(AppearancePanel);
		AppearancePanel.setLayout(new GridLayout(2, 1, 0, 0));
		
		JPanel TrajectoryPanel = new JPanel();
		TrajectoryPanel.setBorder(new TitledBorder(null, "Trajectory",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		AppearancePanel.add(TrajectoryPanel);
		TrajectoryPanel.setLayout(new GridLayout(1, 2, 0, 0));
		
		JRadioButton AlwaysRadioButton = new JRadioButton("Always");
		AlwaysRadioButton.setToolTipText("Whole trajecotires are always shown.");
		AlwaysRadioButton.setSelected(true);
		roiButtonGroup.add(AlwaysRadioButton);
		TrajectoryPanel.add(AlwaysRadioButton);
		AlwaysRadioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				stateOfTrajectory = true;
				imp.updateAndDraw();
			}
			
		});
		
		JRadioButton GrowingRadioButton = new JRadioButton("Growing");
		GrowingRadioButton.setToolTipText("Trajectories will be drawn with corresponding to the frame position");
		roiButtonGroup.add(GrowingRadioButton);
		TrajectoryPanel.add(GrowingRadioButton);
		GrowingRadioButton.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				stateOfTrajectory = false;
				imp.updateAndDraw();
			}
			
		});
		
		JPanel AppearancePanel2 = new JPanel();
		AppearancePanel.add(AppearancePanel2);
		AppearancePanel2.setLayout(new GridLayout(2, 3, 0, 0));
		
		AllCheckBox = new JCheckBox("All");
		AllCheckBox.setToolTipText("Show all tracks");
		AppearancePanel2.add(AllCheckBox);
		AllCheckBox.addActionListener(new ActionListener () {

			@Override
			public void actionPerformed(ActionEvent e) {
				imp.updateAndDraw();
			}
			
		});
		
		ROICheckBox = new JCheckBox("ROI");
		ROICheckBox.setSelected(true);
		ROICheckBox.setToolTipText("Show squrare ROI\n");
		AppearancePanel2.add(ROICheckBox);
		ROICheckBox.addActionListener(new ActionListener () {

			@Override
			public void actionPerformed(ActionEvent e) {
				imp.updateAndDraw();
			}
			
		});
		
		NumberCheckBox = new JCheckBox("Number");
		NumberCheckBox.setSelected(true);
		NumberCheckBox.setToolTipText("Show number of each track");
		AppearancePanel2.add(NumberCheckBox);
		NumberCheckBox.addActionListener(new ActionListener () {

			@Override
			public void actionPerformed(ActionEvent e) {
				imp.updateAndDraw();
			}
			
		});
		
		JPanel param_panel = new JPanel();
		getContentPane().add(param_panel);
		param_panel.setLayout(new GridLayout(2, 1, 0, 0));
		
		JPanel DetectionParamPanel = new JPanel();
		DetectionParamPanel.setBorder(new TitledBorder(new EtchedBorder(EtchedBorder.LOWERED, null, null),
				"Detection Parameters", TitledBorder.LEADING, TitledBorder.TOP, null, new Color(0, 0, 0)));
		param_panel.add(DetectionParamPanel);
		DetectionParamPanel.setLayout(new GridLayout(2, 2, 0, 0));
		
		JLabel LabelRoi = new JLabel("Roi Size (pixels)");
		LabelRoi.setToolTipText("The lenght of the side of square ROI");
		DetectionParamPanel.add(LabelRoi);
		
		roisize = new SpinnerNumberModel(12, 5, 50, 1); //
		JSpinner RoiSize = new JSpinner(roisize);
		RoiSize.setToolTipText("The lenght of the side of square ROI (Pixels)");
		RoiSize.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				PTA2.roisize = (Integer)roisize.getValue();
			}
		});
		DetectionParamPanel.add(RoiSize);
		
		JLabel lblSearchRangepixels = new JLabel("Search Size (pixels)");
		lblSearchRangepixels.setToolTipText("The length of the side of a square ROI which limits the search range to find next object. ");
		DetectionParamPanel.add(lblSearchRangepixels);
		
		searchrange = new SpinnerNumberModel(3, 1, 100, 1);
		JSpinner SearchRange = new JSpinner(searchrange);
		SearchRange.setToolTipText("The length of the side of a square ROI which limits the search range to find next object. ");
		SearchRange.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent e) {
				PTA2.searchrange = (Integer)searchrange.getValue();
			}
		});
		DetectionParamPanel.add(SearchRange);
		
		JPanel TrackParamPanel = new JPanel();
		TrackParamPanel.setBorder(new TitledBorder(null, "Tracking Parameters",
				TitledBorder.LEADING, TitledBorder.TOP, null, null));
		param_panel.add(TrackParamPanel);
		TrackParamPanel.setLayout(new GridLayout(4, 1, 0, 0));
		
		chckbx_Intensity = new JCheckBox("Intensity");
		TrackParamPanel.add(chckbx_Intensity);
		
		chckbx_Size = new JCheckBox("Size");
		TrackParamPanel.add(chckbx_Size);
		
		chckbx_Angle = new JCheckBox("Angle");
		TrackParamPanel.add(chckbx_Angle);
		
		chckbx_Circularity = new JCheckBox("Circularity");
		TrackParamPanel.add(chckbx_Circularity);
		
		addWindowListener(new myListener());
	}

	public int[] retParam() {
		int[] param = new int[4];
		param[0] = chckbx_Intensity.isSelected()?1:0;
		param[1] = chckbx_Size.isSelected()?1:0;
		param[2] = chckbx_Angle.isSelected()?1:0;
		param[3] = chckbx_Circularity.isSelected()?1:0;
		return param;
	}
	
	public int retMethod() {
		return method;
	}

	public boolean isAllTrack() {
		return AllCheckBox.isSelected();
	}
	
	public boolean isRoiTrack() {
		return ROICheckBox.isSelected();
	}
	
	public boolean isNumTrack() {
		return NumberCheckBox.isSelected();
	}
	
	public class myListener extends WindowAdapter {
		@Override
		public void windowClosing(WindowEvent e) {
			ResultDataTable rdt = PTA2.getRDT();
			if (rdt != null) {
				GenericDialog gd = new GenericDialog("Close");
				gd.addMessage("Do you want to close PTA2?");
				gd.enableYesNoCancel();
				gd.showDialog();
				if(gd.wasCanceled())
					return;
				rdt.dispose();
			}
		}
		
		@Override
		public void windowClosed(WindowEvent e) {
			ImagePlus.removeImageListener(PTA2.listener);
			WindowManager.removeWindow(PTA2.mw);
			imp.setOverlay(null);
		}
	}
	
}
