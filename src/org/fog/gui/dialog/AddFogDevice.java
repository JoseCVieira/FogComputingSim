package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Vector;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.TitledBorder;

import org.fog.gui.GuiConstants;
import org.fog.gui.GuiUtils;
import org.fog.gui.core.ApplicationGui;
import org.fog.gui.core.FogDeviceGui;
import org.fog.gui.core.Graph;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Location;
import org.fog.utils.Movement;
import org.fog.utils.Util;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;

public class AddFogDevice extends JDialog {
	private static final long serialVersionUID = -5116677861770319577L;
	
	// Fog device
	private final Graph graph;
	private final FogDeviceGui fog;
	
	private JLabel deviceNameLabel;
	private JLabel levelLabel;
	
	private JTextField deviceName;
	private JTextField mips;
	private JTextField ram;
	private JTextField storage;
	private JTextField rateMips;
	private JTextField rateRam;
	private JTextField rateStorage;
	private JTextField rateBw;
	private JTextField rateEn;
	private JTextField idlePower;
	private JTextField busyPower;
	private JTextField posX;
	private JTextField posY;
	private JComboBox<String> direction;
	private JTextField velocity;
	
	// GUI only
	private JComboBox<String> level;
	
	// Aplication
	private JComboBox<String> application;
	
	// Sensor
	@SuppressWarnings("rawtypes")
	private JComboBox distribution;
	private JTextField uniformLowerBound;
	private JTextField uniformUpperBound;
	private JTextField deterministicValue;
	private JTextField normalMean;
	private JTextField normalStdDev;
	
	private JPanel jPanelSensor;

	public AddFogDevice(final Graph graph, final JFrame frame, final FogDeviceGui fog) {
		this.graph = graph;
		this.fog = fog;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);
		
		setTitle(fog == null ? "  Add Fog Device" : "  Edit Fog Device");
		setModal(true);
		setPreferredSize(new Dimension(700, 1000));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}

	private JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		
		JButton okBtn = new JButton("Ok");
		JButton cancelBtn = new JButton("Cancel");
		JButton delBtn = new JButton("Delete");
		
		cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                setVisible(false);
            }
        });
		
		if(fog != null) {
			delBtn.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	if(graph.getMaxLevel() == fog.getLevel()) {
		            	graph.removeNode(fog);
		                setVisible(false);
	            	}else
	            		GuiUtils.prompt(AddFogDevice.this, "Cannot delete because there are fog nodes below this one.", "Error");
	            }
	        });
		}

		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String error_msg = "", name_ = "";
				int ram_= -1, level_= -1, direction_ = -1;
				long storage_ = -1;
				double mips_= -1, rateMips_ = -1, rateRam_ = -1, rateStorage_ = -1, rateBw_ = -1, rateEn_ = -1, idlePower_ = -1, busyPower_ = -1;
				double posX_ = -1, posY_ = -1, velocity_ = -1;
				double normalMean_ = -1, normalStdDev_ = -1, uniformLow_ = -1, uniformUp_ = -1, deterministicVal_ = -1;
				
				if (Util.validString(deviceName.getText())) {
					if(fog == null || (fog != null && !fog.getName().equals(deviceName.getText())))
						if(graph.isRepeatedName(deviceName.getText()))
							error_msg += "\nName already exists";
				}else
					error_msg += "Missing name\n";
				
				if(deviceName.getText().contains(" "))
					error_msg += "Name cannot contain spaces\n";
				
				level_ = level.getSelectedIndex();
				String dist = (String)distribution.getSelectedItem();
				String appId = (String)application.getSelectedItem();
				
				if (!Util.validString(mips.getText())) error_msg += "Missing Mips\n";
				if (!Util.validString(ram.getText())) error_msg += "Missing Ram\n";
				if (!Util.validString(storage.getText())) error_msg += "Missing storage\n";
				if (!Util.validString(rateMips.getText())) error_msg += "Missing rate/Mips\n";
				if (!Util.validString(rateRam.getText())) error_msg += "Missing rate/Ram\n";
				if (!Util.validString(rateStorage.getText())) error_msg += "Missing rate/Strg\n";
				if (!Util.validString(rateBw.getText())) error_msg += "Missing rate/Bw\n";
				if (!Util.validString(rateEn.getText())) error_msg += "Missing rate/En\n";
				if (!Util.validString(idlePower.getText())) error_msg += "Missing Idle Power\n";
				if (!Util.validString(busyPower.getText())) error_msg += "Missing Busy Power\n";
				if (!Util.validString(posX.getText())) error_msg += "Missing X Position\n";
				if (!Util.validString(posY.getText())) error_msg += "Missing Y Position\n";
				if (!Util.validString(velocity.getText())) error_msg += "Missing Velocity\n";				
				
				name_ = deviceName.getText();
				if((mips_ = Util.stringToDouble(mips.getText())) < 0) error_msg += "\nMips should be a positive number";
				if((ram_ = Util.stringToInt(ram.getText())) < 0) error_msg += "\nRam should be a positive number";
				if((storage_ = Util.stringToInt(storage.getText())) < 0) error_msg += "\nStrg should be a positive number";
				if((rateMips_ = Util.stringToDouble(rateMips.getText())) < 0) error_msg += "\nRate/Mips should be a positive number";
				if((rateRam_ = Util.stringToDouble(rateRam.getText())) < 0) error_msg += "\nRate/Ram should be a positive number";
				if((rateStorage_ = Util.stringToDouble(rateStorage.getText())) < 0) error_msg += "\nRate/Strg should be a positive number";
				if((rateBw_ = Util.stringToDouble(rateBw.getText())) < 0) error_msg += "\nRate/Bw should be a positive number";
				if((rateEn_ = Util.stringToDouble(rateEn.getText())) < 0) error_msg += "\nRate/En should be a positive number";
				if((idlePower_ = Util.stringToDouble(idlePower.getText())) < 0) error_msg += "\nIdle Power should be a positive number";
				if((busyPower_ = Util.stringToDouble(busyPower.getText())) < 0) error_msg += "\nBusy Power should be a positive number";
				if((velocity_ = Util.stringToDouble(velocity.getText())) < 0) error_msg += "\nVelocity should be a positive number";
				
				posX_ = Util.stringToDouble(posX.getText());
				posY_ = Util.stringToDouble(posY.getText());
				direction_ = direction.getSelectedIndex();
				
				Movement movement = new Movement(velocity_, direction_, new Location(posX_, posY_));
				
				if(!appId.isEmpty()) {
					if(dist.equals("Normal")) {
						if (!Util.validString(normalMean.getText())) error_msg += "Missing Normal Mean\n";
						if (!Util.validString(normalStdDev.getText())) error_msg += "Missing Standard Deviation\n";
						if((normalMean_ = Util.stringToDouble(normalMean.getText())) < 0) error_msg += "\nNormal Mean should be a positive number";
						if((normalStdDev_ = Util.stringToDouble(normalStdDev.getText())) < 0) error_msg += "\nStandard Deviation should be a positive number";
					}else if(dist.equals("Uniform")) {
						if (!Util.validString(uniformLowerBound.getText())) error_msg += "Missing Uniform Lower Bound\n";
						if (!Util.validString(uniformUpperBound.getText())) error_msg += "Missing Uniform Upper Bound\n";
						if((uniformLow_ = Util.stringToDouble(uniformLowerBound.getText())) < 0) error_msg += "\nUniform Lower Bound should be a positive number";
						if((uniformUp_ = Util.stringToDouble(uniformUpperBound.getText())) < 0) error_msg += "\nUniform Upper Bound should be a positive number";
					}else if(dist.equals("Deterministic")) {
						if (!Util.validString(deterministicValue.getText())) error_msg += "Missing Deterministic Value\n";
						if((deterministicVal_ = Util.stringToDouble(deterministicValue.getText())) < 0) error_msg += "\nDeterministic Value should be a positive number";
					}
				}
				
				if(error_msg == "") {
					Distribution distribution = null;
					
					if(!appId.isEmpty()) {
						if(dist.equals("Normal")) {
							distribution = new NormalDistribution(normalMean_, normalStdDev_);
						}else if(dist.equals("Uniform")) {
							distribution = new UniformDistribution(uniformLow_, uniformUp_);
						}else if(dist.equals("Deterministic")){
							distribution = new DeterministicDistribution(deterministicVal_);
						}
					}
					
					if(fog != null)
						fog.setValues(name_, level_, mips_, ram_, storage_, rateMips_, rateRam_, rateStorage_, rateBw_, rateEn_, idlePower_,
								busyPower_, movement, appId, distribution);
					else {
						FogDeviceGui fogDevice = new FogDeviceGui(name_, level_, mips_, ram_, storage_, rateMips_, rateRam_, rateStorage_,
								rateBw_, rateEn_, idlePower_, busyPower_, movement, appId, distribution);
						graph.addNode(fogDevice);
					}
					setVisible(false);
				}else
					GuiUtils.prompt(AddFogDevice.this, error_msg, "Error");
			}
		});
		
		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okBtn);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelBtn);
		if(fog != null) {
			buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
			buttonPanel.add(delBtn);
		}
		
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		return buttonPanel;
	}
	
	private JPanel createInputPanelArea() {
        JPanel springPanel = new JPanel(new SpringLayout());
        
        JPanel jPanelFog = createFogNodeInput();
        JPanel jPanelApplication = createApplicationInput();
        
        springPanel.add(jPanelFog);
        springPanel.add(jPanelApplication);

		//rows, cols, initX, initY, xPad, yPad
        SpringUtilities.makeCompactGrid(springPanel, 2, 1, 6, 6, 6, 6);
		return springPanel;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JPanel createFogNodeInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Fog node"));
        
		deviceNameLabel = new JLabel("Name: ");
		jPanel.add(deviceNameLabel);
		deviceName = new JTextField();
		
		deviceName.setText(fog == null ? GuiConstants.FOG_NAME : fog.getName());
		deviceNameLabel.setLabelFor(deviceName);
		jPanel.add(deviceName);
		
		levelLabel = new JLabel("Level: ");
		jPanel.add(levelLabel);
		
		ArrayList<String> choices = new ArrayList<String>();
		int maxLevel = graph.getMaxLevel();
		for(int i = 0; i <= maxLevel + 1; i++)
			choices.add(Integer.toString(i));
		level = new JComboBox<String>(new Vector<String>(choices));
		level.setSelectedIndex(fog == null ? maxLevel+1 : fog.getLevel());
		jPanel.add(level);
		
		mips = GuiUtils.createInput(jPanel, mips, "MIPS: ", fog == null ? Double.toString(GuiConstants.MIPS) : Double.toString(fog.getMips()));
		ram = GuiUtils.createInput(jPanel, ram, "RAM (MB): ", fog == null ? Long.toString(GuiConstants.RAM) : Long.toString(fog.getRam()));
		storage = GuiUtils.createInput(jPanel, storage, "Storage (MB): ", fog == null ? Long.toString(GuiConstants.STRG) : Long.toString(fog.getStorage()));
		rateMips = GuiUtils.createInput(jPanel, rateMips, "Rate/MIPS (€): ", fog == null ? Double.toString(GuiConstants.RATE_MIPS) : Double.toString(fog.getRateMips()));
		rateRam = GuiUtils.createInput(jPanel, rateRam, "Rate/RAM (€/sec for 1 MB): ", fog == null ? Double.toString(GuiConstants.RATE_RAM) : Double.toString(fog.getRateRam()));
		rateStorage = GuiUtils.createInput(jPanel, rateStorage, "Rate/Storage (€/sec for 1 MB): ", fog == null ? Double.toString(GuiConstants.RATE_STRG) : Double.toString(fog.getRateStorage()));
		rateBw = GuiUtils.createInput(jPanel, rateBw, "Rate/Bw (€/1 MB): ", fog == null ? Double.toString(GuiConstants.RATE_BW) : Double.toString(fog.getRateBw()));
		rateEn = GuiUtils.createInput(jPanel, rateEn, "Rate/En (€/W): ", fog == null ? Double.toString(GuiConstants.RATE_EN) : Double.toString(fog.getRateEnergy()));
		idlePower = GuiUtils.createInput(jPanel, idlePower, "Idle Power (W): ", fog == null ? Double.toString(GuiConstants.IDLE_POWER) : Double.toString(fog.getIdlePower()));
		busyPower = GuiUtils.createInput(jPanel, busyPower, "Busy Power (W): ", fog == null ? Double.toString(GuiConstants.BUSY_POWER) : Double.toString(fog.getBusyPower()));
		posX = GuiUtils.createInput(jPanel, posX, "X position: ", fog == null ? "0.0" : Double.toString(fog.getMovement().getLocation().getX()));
		posY = GuiUtils.createInput(jPanel, posY, "Y position: ", fog == null ? "0.0" : Double.toString(fog.getMovement().getLocation().getY()));
		
		ArrayList<String> directions = new ArrayList<String>();
		for(int i = Movement.EAST; i <= Movement.SOUTHEAST; i++) {
			directions.add(Movement.S_DIRECTIONS[i]);
		}
		
		ComboBoxModel<String> directionModel = new DefaultComboBoxModel(directions.toArray());
		direction = new JComboBox<>(directionModel);
		directionModel.setSelectedItem("");
		
		JLabel ldirection = new JLabel("Direction: ");
		jPanel.add(ldirection);
		ldirection.setLabelFor(direction);
		
		String directionOp = fog == null ? Movement.S_DIRECTIONS[0] : Movement.S_DIRECTIONS[fog.getMovement().getDirection()];
		
		direction.setSelectedItem(directionOp);
		jPanel.add(direction);
		
		velocity = GuiUtils.createInput(jPanel, velocity, "Velocity: ", fog == null ? "0.0" : Double.toString(fog.getMovement().getVelocity()));
		
		SpringUtilities.makeCompactGrid(jPanel, 16, 2, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	private JPanel createApplicationInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Application"));
		
		JPanel jPanelApp = createApplicationGeneralInput();
		jPanelSensor = createSensorInput();
		
		jPanel.add(jPanelApp);
		jPanel.add(jPanelSensor);
		
		if (((String)application.getSelectedItem()).isEmpty()) {
			jPanelSensor.setVisible(false);
		}else {
			jPanelSensor.setVisible(true);
		}
		
		SpringUtilities.makeCompactGrid(jPanel, 2, 1, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JPanel createApplicationGeneralInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "General"));
		
		ArrayList<String> applicationIds = new ArrayList<String>();
		for(ApplicationGui applicationGui : graph.getAppList())
			applicationIds.add(applicationGui.getAppId());
		applicationIds.add("");
		
		ComboBoxModel<String> applicationModel = new DefaultComboBoxModel(applicationIds.toArray());
		
		application = new JComboBox<>(applicationModel);
		applicationModel.setSelectedItem("");
		
		JLabel lapplication = new JLabel("Application: ");
		jPanel.add(lapplication);
		lapplication.setLabelFor(application);
		
		if(fog != null && fog.getApplication() != null && fog.getApplication().length() > 0)
			application.setSelectedItem(fog.getApplication());
		
		jPanel.add(application);
		
		SpringUtilities.makeCompactGrid(jPanel, 1, 2, 6, 6, 6, 6);
		
		application.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if (((String)application.getSelectedItem()).isEmpty()) {
					jPanelSensor.setVisible(false);
				}else {
					jPanelSensor.setVisible(true);
				}
			}
		});
		
		return jPanel;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JPanel createSensorInput() {
		String[] distributionType = {"Normal", "Uniform", "Deterministic"};
		
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Sensor"));
		
		
		JLabel distLabel = new JLabel("Distribution Type: ", JLabel.TRAILING);
		jPanel.add(distLabel);	
		distribution = new JComboBox(distributionType);
		distLabel.setLabelFor(distribution);
		distribution.setSelectedIndex(-1);
		distribution.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				
				JComboBox ctype = (JComboBox)e.getSource();
				String item = (String)ctype.getSelectedItem();
				updatePanel(item);				
			}
		});
		
		jPanel.add(distribution);
		
		normalMean = GuiUtils.createInput(jPanel, normalMean, "Mean: ", (fog != null && fog.getDistribution() instanceof NormalDistribution) ? Double.toString(((NormalDistribution) fog.getDistribution()).getMean()) : "");
		normalStdDev = GuiUtils.createInput(jPanel, normalStdDev, "StdDev: ", (fog != null && fog.getDistribution() instanceof NormalDistribution) ? Double.toString(((NormalDistribution) fog.getDistribution()).getStdDev()) : "");
		uniformLowerBound = GuiUtils.createInput(jPanel, uniformLowerBound, "Min: ", (fog != null && fog.getDistribution() instanceof UniformDistribution) ? Double.toString(((UniformDistribution) fog.getDistribution()).getMin()) : "");
		uniformUpperBound = GuiUtils.createInput(jPanel, uniformUpperBound, "Max: ", (fog != null && fog.getDistribution() instanceof UniformDistribution) ? Double.toString(((UniformDistribution) fog.getDistribution()).getMax()) : "");
		deterministicValue = GuiUtils.createInput(jPanel, deterministicValue, "Value: ", (fog != null && fog.getDistribution() instanceof DeterministicDistribution) ? Double.toString(((DeterministicDistribution) fog.getDistribution()).getValue()) : "");

		String item = "";
		if(fog != null && fog.getDistribution() != null) {
			if(fog.getDistributionType() == Distribution.NORMAL) {
				item = "Normal";
				distribution.setSelectedIndex(0);
			}else if(fog.getDistributionType() == Distribution.UNIFORM) {
				item = "Uniform";
				distribution.setSelectedIndex(1);
			}else if(fog.getDistributionType() == Distribution.DETERMINISTIC) {
				item = "Deterministic";
				distribution.setSelectedIndex(2);
			}
		}else {
			item = "Normal";
			distribution.setSelectedIndex(0);
		}
		
		updatePanel(item);
		
		SpringUtilities.makeCompactGrid(jPanel, 6, 2, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	/* Miscellaneous methods */
    private void updatePanel(String item) {
		switch(item){
		case "Normal":
			normalMean.setVisible(true);
			normalStdDev.setVisible(true);
			uniformLowerBound.setVisible(false);
			uniformUpperBound.setVisible(false);
			deterministicValue.setVisible(false);
			break;
		case "Uniform":
			normalMean.setVisible(false);
			normalStdDev.setVisible(false);
			uniformLowerBound.setVisible(true);
			uniformUpperBound.setVisible(true);
			deterministicValue.setVisible(false);
			break;
		case "Deterministic":
			normalMean.setVisible(false);
			normalStdDev.setVisible(false);
			uniformLowerBound.setVisible(false);
			uniformUpperBound.setVisible(false);
			deterministicValue.setVisible(true);
			break;
		default:
			break;
		}
	}
	
}
