package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.border.TitledBorder;

import org.fog.core.Config;
import org.fog.gui.GuiConfig;
import org.fog.gui.GuiMsg;
import org.fog.gui.GuiUtils;
import org.fog.gui.core.Graph;
import org.fog.gui.core.Link;
import org.fog.gui.core.Node;
import org.fog.gui.core.SpringUtilities;
import org.fog.test.ApplicationsExample;
import org.fog.utils.Location;
import org.fog.utils.Movement;
import org.fog.utils.Util;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;

/**
 * Class which defines a random example topology to test the simulator.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class DisplayRandom extends JDialog {
	private static final long serialVersionUID = 4794808969864918000L;
	private static final int WIDTH = 1050;
	private static final int HEIGHT = 900;
	
	/** Object which holds the current topology */
	Graph physicalGraph;
	
	private JTextField jTextNrFogDev;
	private JTextField jTextConnProb;
	private JTextField jTextClientProb;
	
	private JTextField jTextLatMean;
	private JTextField jTextLatDev;
	private JTextField jTextBwMean;
	private JTextField jTextBwDev;
	
	private JTextField jTextMipsMean;
	private JTextField jTextMipsDev;
	private JTextField jTextRamMean;
	private JTextField jTextRamDev;
	private JTextField jTextStrgMean;
	private JTextField jTextStrgDev;
	private JTextField jTextLevelDec;
	
	private JTextField jTextRateMipsMean;
	private JTextField jTextRateMipsDev;
	private JTextField jTextRateRamMean;
	private JTextField jTextRateRamDev;
	private JTextField jTextRateStrgMean;
	private JTextField jTextRateStrgDev;
	private JTextField jTextRateBwMean;
	private JTextField jTextRateBwDev;
	private JTextField jTextRateEnMean;
	private JTextField jTextRateEnDev;
	
	private JTextField jTextBusyPw;
	private JTextField jTextIdlePw;
	private JTextField jTextEnergyDev;
	
	/**
	 * Creates a dialog to display and edit the random topology settings.
	 * 
	 * @param graph the object which holds the current topology
	 * @param frame the current context 
	 */
	public DisplayRandom(Graph physicalGraph, final JFrame frame) {
		this.physicalGraph = physicalGraph;
		setLayout(new BorderLayout());

		add(createInputPanel(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(" Random topology");
		setModal(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}

	/**
	 * Creates the editable random topology input settings.
	 * 
	 * @return the panel containing the input settings
	 */
	private JPanel createInputPanel() {
		JPanel jPanel = new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
		
		jPanel.add(createGeneralInput());
		jPanel.add(createNetworkInput());
		jPanel.add(createDeviceInput());
		
		return jPanel;
	}
	
	/**
	 * Creates the general input settings.
	 * 
	 * @return the panel containing the input settings
	 */
	private JPanel createGeneralInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "General"));
		
		String nrFogDevOp = Integer.toString(GuiConfig.NR_FOG_DEVICES);
		String connectionOp = Double.toString(GuiConfig.CONNECTION_PROB);
		String clientOp = Double.toString(GuiConfig.CLIENT_PROBABILITY);
		
		jTextNrFogDev = GuiUtils.createInput(jPanel, jTextNrFogDev, "Number of fog devices: ", nrFogDevOp, GuiMsg.TipRandNrFog);
		jTextConnProb = GuiUtils.createInput(jPanel, jTextConnProb, "Connect fog devices prob.: ", connectionOp, GuiMsg.TipRandConnect);
		jTextClientProb = GuiUtils.createInput(jPanel, jTextClientProb, "Client prob.: ", clientOp, GuiMsg.TipRandClient);
		
		SpringUtilities.makeCompactGrid(jPanel, 3, 2, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	/**
	 * Creates the network input settings.
	 * 
	 * @return the panel containing the input settings
	 */
	private JPanel createNetworkInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Network"));
		
		String latMeanOp = Double.toString(GuiConfig.LAT_MEAN);
		String latDevOp = Double.toString(GuiConfig.LAT_DEV);
		String bwMeanOp = Double.toString(GuiConfig.BW_MEAN);
		String bwDevOp = Double.toString(GuiConfig.BW_DEV);
		
		jTextLatMean = GuiUtils.createInput(jPanel, jTextLatMean, "Latency mean [s]: ", latMeanOp);
		jTextLatDev = GuiUtils.createInput(jPanel, jTextLatDev, "Latency deviation [s]: ", latDevOp);
		jTextBwMean = GuiUtils.createInput(jPanel, jTextBwMean, "Bandwidth mean [Byte/s]: ", bwMeanOp);
		jTextBwDev = GuiUtils.createInput(jPanel, jTextBwDev, "Bandwidth deviation [Byte/s]: ", bwDevOp);
		
		SpringUtilities.makeCompactGrid(jPanel, 2, 4, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	/**
	 * Creates the devices input settings.
	 * 
	 * @return the panel containing the input settings
	 */
	private JPanel createDeviceInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Devices"));
		
		jPanel.add(createDeviceResourceInput());
		jPanel.add(createDevicePriceInput());
		jPanel.add(createDevicePowerInput());
		
		SpringUtilities.makeCompactGrid(jPanel, 3, 1, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	/**
	 * Creates the device resources input settings.
	 * 
	 * @return the panel containing the input settings
	 */
	private JPanel createDeviceResourceInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Resources"));
		
		String mipsMeanOp = Double.toString(GuiConfig.MIPS_MEAN);
		String mipsDevOp = Double.toString(GuiConfig.MIPS_DEV);
		String ramMeanOp = Double.toString(GuiConfig.RAM_MEAN);
		String ramDevOp = Double.toString(GuiConfig.RAM_DEV);
		String strgMeanOp = Double.toString(GuiConfig.STRG_MEAN);
		String strgDevOp = Double.toString(GuiConfig.STRG_DEV);
		String levelDecOp = Double.toString(GuiConfig.LEVEL_DECADENCY);
		
		jTextMipsMean = GuiUtils.createInput(jPanel, jTextMipsMean, "Mips mean: ", mipsMeanOp);
		jTextMipsDev = GuiUtils.createInput(jPanel, jTextMipsDev, "Mips deviation: ", mipsDevOp);
		jTextRamMean = GuiUtils.createInput(jPanel, jTextRamMean, "Ram mean [Byte]: ", ramMeanOp);
		jTextRamDev = GuiUtils.createInput(jPanel, jTextRamDev, "Ram deviation [Byte]: ", ramDevOp);
		jTextStrgMean = GuiUtils.createInput(jPanel, jTextStrgMean, "Storage mean [Byte]: ", strgMeanOp);
		jTextStrgDev = GuiUtils.createInput(jPanel, jTextStrgDev, "Storage deviation [Byte]: ", strgDevOp);
		jTextLevelDec = GuiUtils.createInput(jPanel, jTextLevelDec, "Level decadency factor: ", levelDecOp, GuiMsg.TipRandLevelDec);
		jPanel.add(new JLabel(""));
		jPanel.add(new JLabel(""));
		
		SpringUtilities.makeCompactGrid(jPanel, 4, 4, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	/**
	 * Creates the device prices input settings.
	 * 
	 * @return the panel containing the input settings
	 */
	private JPanel createDevicePriceInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Prices"));
		
		String rateMipsMeanOp = Double.toString(GuiConfig.RATE_MIPS_MEAN);
		String rateMipsDevOp = Double.toString(GuiConfig.RATE_MIPS_DEV);
		String rateRamMeanOp = Double.toString(GuiConfig.RATE_RAM_MEAN);
		String rateRamDevOp = Double.toString(GuiConfig.RATE_RAM_DEV);
		String rateStrgMeanOp = Double.toString(GuiConfig.RATE_STRG_MEAN);
		String rateStrgDevOp = Double.toString(GuiConfig.RATE_STRG_DEV);
		String rateBwMeanOp = Double.toString(GuiConfig.RATE_BW_MEAN);
		String rateBwDevOp = Double.toString(GuiConfig.RATE_BW_DEV);
		String rateEnMeanOp = Double.toString(GuiConfig.RATE_EN_MEAN);
		String rateEnDevOp = Double.toString(GuiConfig.RATE_EN_DEV);
		
		jTextRateMipsMean = GuiUtils.createInput(jPanel, jTextRateMipsMean, "Rate per mips mean [€]: ", rateMipsMeanOp);
		jTextRateMipsDev = GuiUtils.createInput(jPanel, jTextRateMipsDev, "Rate per mips deviation [€]: ", rateMipsDevOp);
		
		jTextRateRamMean = GuiUtils.createInput(jPanel, jTextRateRamMean, "Rate per ram mean [€]: ", rateRamMeanOp);
		jTextRateRamDev = GuiUtils.createInput(jPanel, jTextRateRamDev, "Rate per ram deviation [€]: ", rateRamDevOp);
		
		jTextRateStrgMean = GuiUtils.createInput(jPanel, jTextRateStrgMean, "Rate per storage mean [€]: ", rateStrgMeanOp);
		jTextRateStrgDev = GuiUtils.createInput(jPanel, jTextRateStrgDev, "Rate per storage deviation [€]: ", rateStrgDevOp);
		
		jTextRateBwMean = GuiUtils.createInput(jPanel, jTextRateBwMean, "Rate per bandwidth mean [€]: ", rateBwMeanOp);
		jTextRateBwDev = GuiUtils.createInput(jPanel, jTextRateBwDev, "Rate per bandwidth deviation [€]: ", rateBwDevOp);
		
		jTextRateEnMean = GuiUtils.createInput(jPanel, jTextRateEnMean, "Rate per energy mean [€]: ", rateEnMeanOp);
		jTextRateEnDev = GuiUtils.createInput(jPanel, jTextRateEnDev, "Rate per energy deviation [€]: ", rateEnDevOp);
		
		SpringUtilities.makeCompactGrid(jPanel, 5, 4, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	/**
	 * Creates the device power input settings.
	 * 
	 * @return the panel containing the input settings
	 */
	private JPanel createDevicePowerInput() {
		JPanel jPanel = new JPanel(new SpringLayout());
		jPanel.setBorder(new TitledBorder(BorderFactory.createLineBorder(Color.LIGHT_GRAY), "Power"));
		
		String busyPwOp = Double.toString(GuiConfig.BUSY_POWER);
		String idlePwOp = Double.toString(GuiConfig.IDLE_POWER);
		String energyDevOp = Double.toString(GuiConfig.ENERGY_DEV);
		
		jTextBusyPw = GuiUtils.createInput(jPanel, jTextBusyPw, "Busy power [W]: ", busyPwOp, GuiMsg.TipDevBusyPw);
		jTextIdlePw = GuiUtils.createInput(jPanel, jTextIdlePw, "Idle power [W]: ", idlePwOp, GuiMsg.TipDevIdlePw);
		jTextEnergyDev = GuiUtils.createInput(jPanel, jTextEnergyDev, "Power deviation [W]: ", energyDevOp);
		
		SpringUtilities.makeCompactGrid(jPanel, 3, 2, 6, 6, 6, 6);
		
		return jPanel;
	}
	
	
	/**
	 * Creates the button panel (i.e., generate and close) and defines its behavior upon being clicked.
	 * 
	 * @return the panel containing the buttons
	 */
	private JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
		
        JButton okBtn = new JButton("Generate");
		JButton cancelBtn = new JButton("Close");
		
		cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	setVisible(false);
            }
        });
		
		okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	String error_msg = "";
            	int nrFogDev, levelDec, ramMean, ramDev, strgMean, strgDev;
            	double connProb, clientProb, latMean, latDev, bwMean, bwDev, mipsMean, mipsDev, busyPw, idlePw, energyDev;
            	double rateMipsMean, rateRamMean, rateStrgMean, rateBwMean, rateEnMean, rateMipsDev, rateRamDev, rateStrgDev, rateBwDev, rateEnDev;
            	
            	if (!Util.validString(jTextNrFogDev.getText()))		error_msg += GuiMsg.errMissing("Number of fog devices");
            	if (!Util.validString(jTextConnProb.getText()))		error_msg += GuiMsg.errMissing("Connect fog devices probability");
            	if (!Util.validString(jTextClientProb.getText()))	error_msg += GuiMsg.errMissing("Client probability");
            	
            	if (!Util.validString(jTextLatMean.getText()))		error_msg += GuiMsg.errMissing("Link latency mean");
            	if (!Util.validString(jTextLatDev.getText()))		error_msg += GuiMsg.errMissing("Link latency deviation");
            	if (!Util.validString(jTextBwMean.getText()))		error_msg += GuiMsg.errMissing("Link bandwidth mean");
            	if (!Util.validString(jTextBwDev.getText()))		error_msg += GuiMsg.errMissing("Link bandwidth deviation");
            	
            	if (!Util.validString(jTextMipsMean.getText()))		error_msg += GuiMsg.errMissing("Device mips mean");
            	if (!Util.validString(jTextMipsDev.getText()))		error_msg += GuiMsg.errMissing("Device mips deviation");
            	if (!Util.validString(jTextRamMean.getText()))		error_msg += GuiMsg.errMissing("Device ram mean");
            	if (!Util.validString(jTextRamDev.getText()))		error_msg += GuiMsg.errMissing("Device ram deviation");
            	if (!Util.validString(jTextStrgMean.getText()))		error_msg += GuiMsg.errMissing("Device storage mean");
            	if (!Util.validString(jTextStrgDev.getText()))		error_msg += GuiMsg.errMissing("Device storage deviation");
            	if (!Util.validString(jTextLevelDec.getText()))		error_msg += GuiMsg.errMissing("Level decadency factor");
            	
            	if (!Util.validString(jTextRateMipsMean.getText()))	error_msg += GuiMsg.errMissing("Device mips rate mean");
            	if (!Util.validString(jTextRateMipsDev.getText()))	error_msg += GuiMsg.errMissing("Device mips rate deviation");
            	if (!Util.validString(jTextRateRamMean.getText()))	error_msg += GuiMsg.errMissing("Device ram rate mean");
            	if (!Util.validString(jTextRateRamDev.getText()))	error_msg += GuiMsg.errMissing("Device ram rate deviation");
            	if (!Util.validString(jTextRateStrgMean.getText()))	error_msg += GuiMsg.errMissing("Device storage rate mean");
            	if (!Util.validString(jTextRateStrgDev.getText()))	error_msg += GuiMsg.errMissing("Device storage rate deviation");
            	if (!Util.validString(jTextRateBwMean.getText()))	error_msg += GuiMsg.errMissing("Device bandwidth rate mean");
            	if (!Util.validString(jTextRateBwDev.getText()))	error_msg += GuiMsg.errMissing("Device bandwidth rate deviation");
            	if (!Util.validString(jTextRateEnMean.getText()))	error_msg += GuiMsg.errMissing("Device energy rate mean");
            	if (!Util.validString(jTextRateEnDev.getText()))	error_msg += GuiMsg.errMissing("Device energy rate deviation");
            	
            	if (!Util.validString(jTextBusyPw.getText()))		error_msg += GuiMsg.errMissing("Device busy power mean");
            	if (!Util.validString(jTextIdlePw.getText()))		error_msg += GuiMsg.errMissing("Device idle power mean");
            	if (!Util.validString(jTextEnergyDev.getText()))	error_msg += GuiMsg.errMissing("Energy deviation");
            	
            	if((nrFogDev = Util.stringToInt(jTextNrFogDev.getText())) < 2)				error_msg += "Number of fog devices must be higher than 1";
            	if((connProb = Util.stringToProbability(jTextConnProb.getText())) < 0)		error_msg += "\nConnection prob. should be a number between [0.0; 1.0]";
            	if((clientProb = Util.stringToProbability(jTextClientProb.getText())) < 0)	error_msg += "\nClient prob. should be a number between [0.0; 1.0]";
            	
            	if((latMean = Util.stringToDouble(jTextLatMean.getText())) < 0)				error_msg += GuiMsg.errFormat("Link latency mean");
            	if((latDev = Util.stringToDouble(jTextLatDev.getText())) < 0)				error_msg += GuiMsg.errFormat("Link latency deviation");
            	if((bwMean = Util.stringToDouble(jTextBwMean.getText())) < 0)				error_msg += GuiMsg.errFormat("Link bandwidth mean");
            	if((bwDev = Util.stringToDouble(jTextBwDev.getText())) < 0)					error_msg += GuiMsg.errFormat("Link bandwidth deviation");
            	
            	if((mipsMean = Util.stringToDouble(jTextMipsMean.getText())) < 0)			error_msg += GuiMsg.errFormat("Device mips mean");
            	if((mipsDev = Util.stringToDouble(jTextMipsDev.getText())) < 0)				error_msg += GuiMsg.errFormat("Device mips deviation");
            	if((ramMean = Util.stringToInt(jTextRamMean.getText())) < 0)				error_msg += GuiMsg.errFormat("Device ram mean");
            	if((ramDev = Util.stringToInt(jTextRamDev.getText())) < 0)					error_msg += GuiMsg.errFormat("Device ram deviation");
            	if((strgMean = Util.stringToInt(jTextStrgMean.getText())) < 0)				error_msg += GuiMsg.errFormat("Device storage mean");
            	if((strgDev = Util.stringToInt(jTextStrgDev.getText())) < 0)				error_msg += GuiMsg.errFormat("Device storage deviation");
            	if((levelDec = Util.stringToInt(jTextLevelDec.getText())) < 0)				error_msg += GuiMsg.errFormat("Level decadency factor");
            	
            	if((rateMipsMean = Util.stringToDouble(jTextRateMipsMean.getText())) < 0)	error_msg += GuiMsg.errFormat("Device mips rate mean");
            	if((rateMipsDev = Util.stringToDouble(jTextRateMipsDev.getText())) < 0)		error_msg += GuiMsg.errFormat("Device mips rate deviation");
            	if((rateRamMean = Util.stringToDouble(jTextRateRamMean.getText())) < 0)		error_msg += GuiMsg.errFormat("Device ram rate mean");
            	if((rateRamDev = Util.stringToDouble(jTextRateRamDev.getText())) < 0)		error_msg += GuiMsg.errFormat("Device ram rate deviation");
            	if((rateStrgMean = Util.stringToDouble(jTextRateStrgMean.getText())) < 0)	error_msg += GuiMsg.errFormat("Device storage rate mean");
            	if((rateStrgDev = Util.stringToDouble(jTextRateStrgDev.getText())) < 0)		error_msg += GuiMsg.errFormat("Device storage rate deviation");
            	if((rateBwMean = Util.stringToDouble(jTextRateBwMean.getText())) < 0)		error_msg += GuiMsg.errFormat("Device bandwidth rate mean");
            	if((rateBwDev = Util.stringToDouble(jTextRateBwDev.getText())) < 0)			error_msg += GuiMsg.errFormat("Device bandwidth rate deviation");
            	if((rateEnMean = Util.stringToDouble(jTextRateEnMean.getText())) < 0)		error_msg += GuiMsg.errFormat("Device energy rate mean");
            	if((rateEnDev = Util.stringToDouble(jTextRateEnDev.getText())) < 0)			error_msg += GuiMsg.errFormat("Device energy rate deviation");
            	
            	if((busyPw = Util.stringToDouble(jTextBusyPw.getText())) < 0)				error_msg += GuiMsg.errFormat("Device busy power mean");
            	if((idlePw = Util.stringToDouble(jTextIdlePw.getText())) < 0)				error_msg += GuiMsg.errFormat("Device idle power mean");
            	if((energyDev = Util.stringToDouble(jTextEnergyDev.getText())) < 0)			error_msg += GuiMsg.errFormat("Energy deviation");
            	
            	if(error_msg.isEmpty()) {
            		GuiConfig.NR_FOG_DEVICES = nrFogDev;
            		GuiConfig.CONNECTION_PROB = connProb;
            		GuiConfig.CLIENT_PROBABILITY = clientProb;
            		GuiConfig.LAT_MEAN = latMean;
            		GuiConfig.LAT_DEV = latDev;
            		GuiConfig.BW_MEAN = bwMean;
            		GuiConfig.BW_DEV = bwDev;
            		GuiConfig.MIPS_MEAN = mipsMean;
            		GuiConfig.MIPS_DEV = mipsDev;
            		GuiConfig.RAM_MEAN = ramMean;
            		GuiConfig.RAM_DEV = ramDev;
            		GuiConfig.STRG_MEAN = strgMean;
            		GuiConfig.STRG_DEV = strgDev;
            		GuiConfig.LEVEL_DECADENCY = levelDec;
            		GuiConfig.RATE_MIPS_MEAN = rateMipsMean;
            		GuiConfig.RATE_MIPS_DEV = rateMipsDev;
            		GuiConfig.RATE_RAM_MEAN = rateRamMean;
            		GuiConfig.RATE_RAM_DEV = rateRamDev;
            		GuiConfig.RATE_STRG_MEAN = rateStrgMean;
            		GuiConfig.RATE_STRG_DEV = rateStrgDev;
            		GuiConfig.RATE_BW_MEAN = rateBwMean;
            		GuiConfig.RATE_BW_DEV = rateBwDev;
            		GuiConfig.RATE_EN_MEAN = rateEnMean;
            		GuiConfig.RATE_EN_DEV = rateEnDev;
            		GuiConfig.BUSY_POWER = busyPw;
            		GuiConfig.IDLE_POWER = idlePw;
            		GuiConfig.ENERGY_DEV = energyDev;
            		physicalGraph.clean();
            		
            		createApplications();
            		createFogDevicesNetwork();
            		connectDevices();
            		
            		setVisible(false);
            	}else {
            		GuiUtils.prompt(DisplayRandom.this, error_msg, "Error");
            	}
            }
        });
        
        buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okBtn);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelBtn);
        
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		return buttonPanel;
	}
	
	/**
	 * Creates the predefined example applications which clients may deploy into the fog network.
	 */
	private void createApplications() {
		ApplicationsExample.createExampleApplications();
		
		for(int i = 0; i < ApplicationsExample.getNumberOfApplicationsExample(); i++) {
			physicalGraph.addApp(ApplicationsExample.getAppExampleByIndex(i));
		}
		
		ApplicationsExample.clean();
		
	}
	
	/**
	 * Creates the fog devices within the fog network based on the user defined input values.
	 */
	private void createFogDevicesNetwork() {
		// Create the cloud device (cloud is seen as a single node)
		physicalGraph.addNode(generateNode("Cloud", 0, false));
		
		int iter = 1;
		int nrFogNodes = GuiConfig.NR_FOG_DEVICES - 1;
		
		// While there are some nodes which have been not created yet
		while(nrFogNodes > 0) {
			
			// Generate a random number of nodes
			int nr = Util.rand(1, nrFogNodes);			
			nrFogNodes -= nr;
			
			// For each node
			for(int i = 0; i < nr; i++) {
				// Define if its a client or not
				boolean client = false;
				if(new Random().nextFloat() < GuiConfig.CLIENT_PROBABILITY) {
					client = true;
				}
				
				// If its the last one and there are no clients, then it creates a client
				boolean found = false;
				if(!client && nrFogNodes == 0 && i == nr - 1) {
					for(Node node : physicalGraph.getDevicesList().keySet()) {
						if(!node.getApplication().equals("")) {
							found = true;
							break;
						}
					}
					
					if(!found)
						client = true;
				}
				
				physicalGraph.addNode(generateNode("L"+iter+":F"+i, iter, client));
			}
			
			iter++;
		}
	}
	
	/**
	 * Generates a fog node based on the user defined input values. 
	 * 
	 * @param name the name of the fog node
	 * @param level the GUI level where it is located
	 * @param client the variable which indicates whether the node is a client or not
	 * @return the new fog device
	 */
	private Node generateNode(String name, int level, boolean client) {
		double mips = Util.normalRand(decadencyFactor(GuiConfig.MIPS_MEAN, level), decadencyFactor(GuiConfig.MIPS_DEV, level));
		int ram = (int) Util.normalRand(decadencyFactor(GuiConfig.RAM_MEAN, level), decadencyFactor(GuiConfig.RAM_DEV, level));
		int strg = (int) Util.normalRand(decadencyFactor(GuiConfig.STRG_MEAN, level), decadencyFactor(GuiConfig.STRG_DEV, level));
		
		double bPw = Util.normalRand(decadencyFactor(GuiConfig.BUSY_POWER, level), decadencyFactor(GuiConfig.ENERGY_DEV, level));
		double iPw = Util.normalRand(decadencyFactor(GuiConfig.IDLE_POWER, level), decadencyFactor(GuiConfig.ENERGY_DEV, level));
		
		double rateMips = Util.normalRand(GuiConfig.RATE_MIPS_MEAN, GuiConfig.RATE_MIPS_DEV);
		double rateRam = Util.normalRand(GuiConfig.RATE_RAM_MEAN, GuiConfig.RATE_RAM_DEV);
		double rateStrg = Util.normalRand(GuiConfig.RATE_STRG_MEAN, GuiConfig.RATE_STRG_DEV);
		double rateBw = Util.normalRand(GuiConfig.RATE_BW_MEAN, GuiConfig.RATE_BW_DEV);
		double rateEn = Util.normalRand(GuiConfig.RATE_EN_MEAN, GuiConfig.RATE_EN_DEV);
		
		Distribution distribution = null;
		String appName = "";
		if(client) {
			distribution = new DeterministicDistribution(Util.normalRand(GuiConfig.SENSOR_DESTRIBUTION, 1.0));
			
			int appIndex = new Random().nextInt(physicalGraph.getAppList().size());
			appName = physicalGraph.getAppList().get(appIndex).getAppId();
		}
		
		double posx = Util.rand(0, Config.SQUARE_SIDE);
		double posy = Util.rand(0, Config.SQUARE_SIDE);
		
		// Generate its movement
		Movement movement = new Movement(0.0, Movement.EAST, new Location(posx, posy));
		
		return new Node(name, level, mips, ram, strg, rateMips, rateRam, rateStrg, rateBw, rateEn, iPw, bPw, movement, appName, distribution);
	}
	
	/**
	 * Computes a given value based on the initial value as well as the GUI level in which the node is
	 * located and the level decadency factor. This is used to create an hierarchical network where
	 * nodes at the top have more resources than the ones above.
	 * 
	 * @param value the initial value
	 * @param level the level in which the node is located
	 * @return the result value
	 */
	private double decadencyFactor(double value, int level) {
		return value/((level+1)*GuiConfig.LEVEL_DECADENCY);
	}
	
	/**
	 * Generates the connections between nodes. In this case, there is always one fixed network which is fully connected.
	 * The nodes which are not connected are mobile nodes. The connections between them and the central fixed network
	 * are created and managed during the simulation.
	 */
	private void connectDevices() {
		ArrayList<Node> notConnctedDevices = getListFromSet(physicalGraph.getDevicesList().keySet());
		Node cloud = getFromList(notConnctedDevices, "Cloud");
		notConnctedDevices.remove(cloud);
		Node f = notConnctedDevices.get(new Random().nextInt(notConnctedDevices.size()));
		
		double lat = Util.rand(GuiConfig.LAT_MEAN, GuiConfig.LAT_DEV);
		double bw = Util.rand(GuiConfig.BW_MEAN, GuiConfig.BW_MEAN);
		physicalGraph.addEdge(cloud, new Link(f, lat, bw));
		
		// Defines each are the mobile nodes
		List<Node> toRemove = new ArrayList<Node>();
		for(Node f1 : notConnctedDevices) {
			if(f1.getName().equals(f.getName())) continue;
			if(new Random().nextFloat() < GuiConfig.CONNECTION_PROB) continue;
			toRemove.add(f1);
		}
		notConnctedDevices.removeAll(toRemove);
		
		// The fixed nodes must have be all connected
		while(notConnctedDevices.size() > 1) {
			String lastNodeName = notConnctedDevices.get(notConnctedDevices.size()-1).getName();
			Node f1 = notConnctedDevices.get(0);
			notConnctedDevices.remove(0);
							
			for(Node f2 : notConnctedDevices) {
				// Its above the probability
				if(new Random().nextFloat() >= GuiConfig.CONNECTION_PROB) {
					
					// If its not the last node in the list do nothing
					if(!f2.getName().equals(lastNodeName)) continue;
					
					// Otherwise, connect them
					lat = Util.rand(GuiConfig.LAT_MEAN, GuiConfig.LAT_DEV);
					bw = Util.rand(GuiConfig.BW_MEAN, GuiConfig.BW_MEAN);
					physicalGraph.addEdge(f2, new Link(f1, lat, bw));
					break;
				}
				
				lat = Util.rand(GuiConfig.LAT_MEAN, GuiConfig.LAT_DEV);
				bw = Util.rand(GuiConfig.BW_MEAN, GuiConfig.BW_MEAN);
				physicalGraph.addEdge(f2, new Link(f1, lat, bw));
				break;
			}
		}
	}
	
	/**
	 * Gets a specific node from the list of nodes based on its name.
	 * 
	 * @param nodes the list of fog nodes
	 * @param name the name of the fog node
	 * @return the node with that name
	 */
	private Node getFromList(ArrayList<Node> nodes, String name) {
		for(Node f : nodes) {
			if(f.getName().equals(name)) return f;
		}
		
		return null;
	}
	
	/**
	 * Creates a list of nodes based on a set of nodes.
	 * 
	 * @param nodes the set of nodes
	 * @return the list of nodes
	 */
	private ArrayList<Node> getListFromSet(Set<Node> nodes) {
		ArrayList<Node> returnList = new ArrayList<Node>();
		
		for(Node f : nodes) {
			returnList.add(f);
		}
		
		return returnList;
	}
	
}
