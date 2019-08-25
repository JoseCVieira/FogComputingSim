package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.TitledBorder;

import org.fog.application.Application;
import org.fog.gui.GuiConfig;
import org.fog.gui.GuiMsg;
import org.fog.gui.GuiUtils;
import org.fog.gui.core.Node;
import org.fog.gui.core.Graph;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Location;
import org.fog.utils.Movement;
import org.fog.utils.Util;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;

/**
 * Class which allows to add or edit fixed or mobile fog devices (i.e., fog nodes or clients).
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class AddFogDevice extends JDialog {
	private static final long serialVersionUID = -5116677861770319577L;
	
	/** Object which holds the current topology */
	private final Graph graph;
	
	/** Object which contains the device to be edited or null if its a new one */
	private final Node fog;
	
	/** Name of the fog device */
	private JTextField deviceName;
	
	/** Available MIPS of the machine */
	private JTextField mips;
	
	/** Available ram in the machine */
	private JTextField ram;
	
	/** Available storage in the machine */
	private JTextField storage;
	
	 /** Price that will be charged by using processing resources */
	private JTextField rateMips;
	
	/** Price that will be charged by using memory resources */
	private JTextField rateRam;
	
	/** Price that will be charged by using storage resources */
	private JTextField rateStorage;
	
	/** Price that will be charged by bandwidth resources */
	private JTextField rateBw;
	
	/** Price that will be charged by spending energy */
	private JTextField rateEn;
	
	/** Power value while using the full processing capacity of the machine */
	private JTextField idlePower;
	
	/** Power value while using no processing resources in the machine */
	private JTextField busyPower;
	
	/** X coordinate of the machine */
	private JTextField posX;
	
	/** Y coordinate of the machine */
	private JTextField posY;
	
	/** List used in the drop-down containing the names of all possible directions */
	private JComboBox<String> direction;
	
	/** Velocity of the machine (if it is a fixed node this value is ignored) */
	private JTextField velocity;
	
	/** Defines the position at the graphical interface (it is not used by the simulation itself) */
	private JComboBox<String> level;
	
	/** List used in the drop-down containing the names of all defined applications */
	private JComboBox<String> application;	
	
	/** Time interval (deterministic or not) which defines when the sensor will generate new tuples */
	private JComboBox distribution;
	
	/** Minimum value of the uniform distribution */
	private JTextField uniformLowerBound;
	
	/** Maximum value of the uniform distribution */
	private JTextField uniformUpperBound;
	
	/** Value of the deterministic distribution */
	private JTextField deterministicValue;
	
	/** Mean of the normal distribution */
	private JTextField normalMean;
	
	/** Standard deviation of the normal distribution */
	private JTextField normalStdDev;
	
	/** Panel used to define the sensor characteristics (hidden when it is not a client) */
	private JPanel jPanelSensor;
	
	/**
	 * Creates or edits a fog device.
	 * 
	 * @param graph the object which holds the current topology
	 * @param frame the current context
	 * @param fog the node to be edited; can be null when a new fog node is to be added
	 */
	public AddFogDevice(final Graph graph, final JFrame frame, final Node fog) {
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

	/**
	 * Creates the button panel (i.e., Ok, Cancel, Delete) and defines its behavior upon being clicked.
	 * 
	 * @return the panel containing the buttons
	 */
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
		
		// If its a new fog device, hide the delete button
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
		
		// Verify the introduced data and add or edit the fog device when the OK button is pressed 
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String error_msg = "", name_ = "";
				long storage_ = -1;
				int ram_= -1, level_= -1, direction_ = -1;
				double posX_ = -1, posY_ = -1, velocity_ = -1;
				double normalMean_ = -1, normalStdDev_ = -1, uniformLow_ = -1, uniformUp_ = -1, deterministicVal_ = -1;
				double mips_= -1, rateMips_ = -1, rateRam_ = -1, rateStorage_ = -1, rateBw_ = -1, rateEn_ = -1, idlePower_ = -1, busyPower_ = -1;
				
				name_ = deviceName.getText();
				level_ = level.getSelectedIndex();
				direction_ = direction.getSelectedIndex();
				
				// Verify all the introduced data
				if(fog == null || (fog != null && !fog.getName().equals(name_))) {
					if(graph.isRepeatedName(name_))
						error_msg += "\nName already exists";
				}
				
				if(name_.contains(" "))
					error_msg += "Name cannot contain spaces\n";
				
				if (!Util.validString(deviceName.getText())) 	error_msg += GuiMsg.errMissing("Name");
				if (!Util.validString(mips.getText())) 			error_msg += GuiMsg.errMissing("Mips");
				if (!Util.validString(ram.getText())) 			error_msg += GuiMsg.errMissing("Ram");
				if (!Util.validString(storage.getText())) 		error_msg += GuiMsg.errMissing("Storage");
				if (!Util.validString(rateMips.getText())) 		error_msg += GuiMsg.errMissing("Mips price");
				if (!Util.validString(rateRam.getText())) 		error_msg += GuiMsg.errMissing("Ram price");
				if (!Util.validString(rateStorage.getText())) 	error_msg += GuiMsg.errMissing("Storage price");
				if (!Util.validString(rateBw.getText())) 		error_msg += GuiMsg.errMissing("Bandwidth price");
				if (!Util.validString(rateEn.getText())) 		error_msg += GuiMsg.errMissing("Energy price");
				if (!Util.validString(idlePower.getText())) 	error_msg += GuiMsg.errMissing("Idle Power");
				if (!Util.validString(busyPower.getText())) 	error_msg += GuiMsg.errMissing("Busy Power");
				if (!Util.validString(posX.getText())) 			error_msg += GuiMsg.errMissing("X Position");
				if (!Util.validString(posY.getText())) 			error_msg += GuiMsg.errMissing("Y Position");
				if (!Util.validString(velocity.getText())) 		error_msg += GuiMsg.errMissing("Velocity");
				if(level_ < 0) 									error_msg += GuiMsg.errMissing("Level");
				if(direction_ < 0) 								error_msg += GuiMsg.errMissing("Direction");
				
				if((mips_ = Util.stringToDouble(mips.getText())) < 0) 				error_msg += GuiMsg.errFormat("Mips");
				if((ram_ = Util.stringToInt(ram.getText())) < 0) 					error_msg += GuiMsg.errFormat("Ram");
				if((storage_ = Util.stringToInt(storage.getText())) < 0) 			error_msg += GuiMsg.errFormat("Storage");
				if((rateMips_ = Util.stringToDouble(rateMips.getText())) < 0) 		error_msg += GuiMsg.errFormat("Mips price");
				if((rateRam_ = Util.stringToDouble(rateRam.getText())) < 0) 		error_msg += GuiMsg.errFormat("Ram price");
				if((rateStorage_ = Util.stringToDouble(rateStorage.getText())) < 0) error_msg += GuiMsg.errFormat("Storage price");
				if((rateBw_ = Util.stringToDouble(rateBw.getText())) < 0) 			error_msg += GuiMsg.errFormat("Bandwidth price");
				if((rateEn_ = Util.stringToDouble(rateEn.getText())) < 0) 			error_msg += GuiMsg.errFormat("Energy price");
				if((idlePower_ = Util.stringToDouble(idlePower.getText())) < 0) 	error_msg += GuiMsg.errFormat("Idle Power");
				if((busyPower_ = Util.stringToDouble(busyPower.getText())) < 0) 	error_msg += GuiMsg.errFormat("Busy Power");
				if((velocity_ = Util.stringToDouble(velocity.getText())) < 0) 		error_msg += GuiMsg.errFormat("Velocity");
				
				posX_ = Util.stringToDouble(posX.getText());
				posY_ = Util.stringToDouble(posY.getText());
				String dist = (String)distribution.getSelectedItem();
				String appId = (String)application.getSelectedItem();
				
				if(!appId.isEmpty()) {
					if(dist.equals("Normal")) {
						String mean = normalMean.getText();
						String sdt = normalStdDev.getText();
						
						if (!Util.validString(mean)) 								error_msg += GuiMsg.errMissing("Normal mean");
						if (!Util.validString(sdt)) 								error_msg += GuiMsg.errMissing("Standard deviation");
						if((normalMean_ = Util.stringToDouble(mean)) < 0) 			error_msg += GuiMsg.errFormat("Normal mean");
						if((normalStdDev_ = Util.stringToDouble(sdt)) < 0) 			error_msg += GuiMsg.errFormat("Standard deviation");
					}else if(dist.equals("Uniform")) {
						String lower = uniformLowerBound.getText();
						String upper = uniformUpperBound.getText();
						
						if (!Util.validString(lower)) 								error_msg += GuiMsg.errMissing("Uniform Lower Bound");
						if (!Util.validString(upper)) 								error_msg += GuiMsg.errMissing("Uniform Upper Bound");
						if((uniformLow_ = Util.stringToDouble(lower)) < 0) 			error_msg += GuiMsg.errFormat("Uniform Lower Bound");
						if((uniformUp_ = Util.stringToDouble(upper)) < 0) 			error_msg += GuiMsg.errFormat("Uniform Upper Bound");
					}else if(dist.equals("Deterministic")) {
						String value = deterministicValue.getText();
						
						if (!Util.validString(value)) 								error_msg += GuiMsg.errMissing("Deterministic value");
						if((deterministicVal_ = Util.stringToDouble(value)) < 0)	error_msg += GuiMsg.errFormat("Deterministic value");
					}
				}
				
				if(error_msg.isEmpty()) {
					Distribution distribution = null;
					
					// Create the sensor distribution when its a client (i.e., has an application)
					if(!appId.isEmpty()) {
						if(dist.equals("Normal")) {
							distribution = new NormalDistribution(normalMean_, normalStdDev_);
						}else if(dist.equals("Uniform")) {
							distribution = new UniformDistribution(uniformLow_, uniformUp_);
						}else if(dist.equals("Deterministic")){
							distribution = new DeterministicDistribution(deterministicVal_);
						}
					}
					
					// Create the fog device movement
					Movement movement = new Movement(velocity_, direction_, new Location(posX_, posY_));
					
					// Add a new fog device
					if(fog != null)
						fog.setValues(name_, level_, mips_, ram_, storage_, rateMips_, rateRam_, rateStorage_, rateBw_, rateEn_, idlePower_,
								busyPower_, movement, appId, distribution);
					
					// Edit the current fog device
					else {
						Node fogDevice = new Node(name_, level_, mips_, ram_, storage_, rateMips_, rateRam_, rateStorage_,
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
	
	/**
	 * Creates all the inputs that users need to fill up.
	 * 
	 * @return the panel containing the inputs
	 */
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
	
	/**
	 * Creates the fog device form.
	 * 
	 * @return the panel containing the inputs related to the fog device itself
	 */
	private JPanel createFogNodeInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Fog node"));
		
		ArrayList<String> levels = new ArrayList<String>();
		for(int i = 0; i <= graph.getMaxLevel() + 1; i++)
			levels.add(Integer.toString(i));
		
		String nameVal = fog == null ? "Node" : fog.getName();
		String levelVal = fog == null ? "" : Integer.toString(fog.getLevel());
		String mipsVal = fog == null ? Double.toString(GuiConfig.MIPS_MEAN) : Double.toString(fog.getMips());
		String ramVal = fog == null ? Long.toString(GuiConfig.RAM_MEAN) : Long.toString(fog.getRam());
		String storageVal = fog == null ? Long.toString(GuiConfig.STRG_MEAN) : Long.toString(fog.getStorage());
		String rateMipsVal = fog == null ? Double.toString(GuiConfig.RATE_MIPS_MEAN) : Double.toString(fog.getRateMips());
		String rateRamVal = fog == null ? Double.toString(GuiConfig.RATE_RAM_MEAN) : Double.toString(fog.getRateRam());
		String rateStrgVal = fog == null ? Double.toString(GuiConfig.RATE_STRG_MEAN) : Double.toString(fog.getRateStorage());
		String rateBwVal = fog == null ? Double.toString(GuiConfig.RATE_BW_MEAN) : Double.toString(fog.getRateBw());
		String rateEnVal = fog == null ? Double.toString(GuiConfig.RATE_EN_MEAN) : Double.toString(fog.getRateEnergy());
		String BusyPwVal = fog == null ? Double.toString(GuiConfig.BUSY_POWER) : Double.toString(fog.getBusyPower());
		String IdlePwVal = fog == null ? Double.toString(GuiConfig.IDLE_POWER) : Double.toString(fog.getIdlePower());
		String xPosVal = fog == null ? "0.0" : Double.toString(fog.getMovement().getLocation().getX());
		String yPosVal = fog == null ? "0.0" : Double.toString(fog.getMovement().getLocation().getY());
		String dirVal = fog == null ? "" : Movement.S_DIRECTIONS[fog.getMovement().getDirection()];
		String velVal = fog == null ? "0.0" : Double.toString(fog.getMovement().getVelocity());
		
		ComboBoxModel<String> levelModel = new DefaultComboBoxModel(levels.toArray());
		ComboBoxModel<String> directionModel = new DefaultComboBoxModel(Movement.S_DIRECTIONS);
		
		deviceName = GuiUtils.createInput(jPanel, deviceName, "Name: ", nameVal, GuiMsg.TipDevName);
		level = GuiUtils.createDropDown(jPanel, level, "Level: ", levelModel, levelVal, GuiMsg.TipDevLevel);
		mips = GuiUtils.createInput(jPanel, mips, "Mips [MIPS]: ", mipsVal, GuiMsg.TipDevMips);
		ram = GuiUtils.createInput(jPanel, ram, "Ram [Byte]: ", ramVal, GuiMsg.TipDevRam);
		storage = GuiUtils.createInput(jPanel, storage, "Storage [Byte]: ", storageVal, GuiMsg.TipDevStrg);
		rateMips = GuiUtils.createInput(jPanel, rateMips, "Mips price [€]: ", rateMipsVal, GuiMsg.TipDevMipsPrice);
		rateRam = GuiUtils.createInput(jPanel, rateRam, "Ram price [€]: ", rateRamVal, GuiMsg.TipDevRamPrice);
		rateStorage = GuiUtils.createInput(jPanel, rateStorage, "Storage price [€]: ", rateStrgVal, GuiMsg.TipDevStrgPrice);
		rateBw = GuiUtils.createInput(jPanel, rateBw, "Bandwidth price [€]: ", rateBwVal, GuiMsg.TipDevBwPrice);
		rateEn = GuiUtils.createInput(jPanel, rateEn, "Energy price [€]: ", rateEnVal, GuiMsg.TipDevEnPrice);
		busyPower = GuiUtils.createInput(jPanel, busyPower, "Busy power [W]: ", BusyPwVal, GuiMsg.TipDevBusyPw);
		idlePower = GuiUtils.createInput(jPanel, idlePower, "Idle power [W]: ", IdlePwVal, GuiMsg.TipDevIdlePw);
		posX = GuiUtils.createInput(jPanel, posX, "X Coordinate [m]: ", xPosVal, GuiMsg.TipDevXCoord);
		posY = GuiUtils.createInput(jPanel, posY, "Y Coordinate [m]: ", yPosVal, GuiMsg.TipDevYCoord);
		direction = GuiUtils.createDropDown(jPanel, direction, "Direction: ", directionModel, dirVal, GuiMsg.TipDevDir);
		velocity = GuiUtils.createInput(jPanel, velocity, "Velocity [m/s]: ", velVal, GuiMsg.TipDevVel);		
		
		SpringUtilities.makeCompactGrid(jPanel, 16, 2, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	/**
	 * Creates the wrapper for the application and sensor forms (if it is fill up it means that its a client).
	 * 
	 * @return the panel containing the inputs related to the application and sensor forms
	 */
	private JPanel createApplicationInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Application"));
		
		JPanel jPanelApp = createApplicationGeneralInput();
		jPanelSensor = createSensorInput();
		
		jPanel.add(jPanelApp);
		jPanel.add(jPanelSensor);
		
		if (application != null) {
			if (!((String)application.getSelectedItem()).isEmpty())
				jPanelSensor.setVisible(true);
			else
				jPanelSensor.setVisible(false);
		}
		
		SpringUtilities.makeCompactGrid(jPanel, 2, 1, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	/**
	 * Creates the application form.
	 * 
	 * @return the panel containing the inputs related to the application
	 */
	private JPanel createApplicationGeneralInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "General"));
		
		ArrayList<String> applicationIds = new ArrayList<String>();
		for(Application applicationGui : graph.getAppList())
			applicationIds.add(applicationGui.getAppId());
		applicationIds.add("");
		
		String appVal = fog == null ? "" : fog.getApplication();
		
		ComboBoxModel<String> applicationModel = new DefaultComboBoxModel(applicationIds.toArray());
		application = GuiUtils.createDropDown(jPanel, application, "Application: ", applicationModel, appVal, GuiMsg.TipDevApp);
		
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
	
	/**
	 * Creates the sensor form.
	 * 
	 * @return the panel containing the inputs related to the sensor
	 */
	private JPanel createSensorInput() {
		String[] distributionType = {"Normal", "Uniform", "Deterministic"};
		
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Sensor"));
		
		String nMeanVal = (fog != null && fog.getDistribution() instanceof NormalDistribution) ? Double.toString(((NormalDistribution) fog.getDistribution()).getMean()) : "";
		String nStdDevVal = (fog != null && fog.getDistribution() instanceof NormalDistribution) ? Double.toString(((NormalDistribution) fog.getDistribution()).getStdDev()) : "";
		String uLowerVal = (fog != null && fog.getDistribution() instanceof UniformDistribution) ? Double.toString(((UniformDistribution) fog.getDistribution()).getMin()) : "";
		String uUpperVal = (fog != null && fog.getDistribution() instanceof UniformDistribution) ? Double.toString(((UniformDistribution) fog.getDistribution()).getMax()) : "";
		String detVal = (fog != null && fog.getDistribution() instanceof DeterministicDistribution) ? Double.toString(((DeterministicDistribution) fog.getDistribution()).getValue()) : "";
		
		ComboBoxModel<String> distributionModel = new DefaultComboBoxModel(distributionType);
		distribution = GuiUtils.createDropDown(jPanel, distribution, "Distribution Type: ", distributionModel, null, GuiMsg.TipDevDist);
		
		distribution.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				
				JComboBox ctype = (JComboBox)e.getSource();
				String item = (String)ctype.getSelectedItem();
				updatePanel(item);
			}
		});
		
		normalMean = GuiUtils.createInput(jPanel, normalMean, "Mean [s]: ", nMeanVal, GuiMsg.TipDevnMean);
		normalStdDev = GuiUtils.createInput(jPanel, normalStdDev, "StdDev [s]: ", nStdDevVal, GuiMsg.TipDevnStd);
		uniformLowerBound = GuiUtils.createInput(jPanel, uniformLowerBound, "Min [s]: ", uLowerVal, GuiMsg.TipDevuLow);
		uniformUpperBound = GuiUtils.createInput(jPanel, uniformUpperBound, "Max [s]: ", uUpperVal, GuiMsg.TipDevuUp);
		deterministicValue = GuiUtils.createInput(jPanel, deterministicValue, "Det. value [s]: ", detVal, GuiMsg.TipDevdVal);

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
	/**
	 * Updates the visibility of the sensor inputs based on the name of the distribution.
	 * 
	 * @param item the name of the sensor distribution
	 */
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
