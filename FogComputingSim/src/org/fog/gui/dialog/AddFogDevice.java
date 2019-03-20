package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.fog.gui.Gui;
import org.fog.gui.core.ApplicationGui;
import org.fog.gui.core.FogDeviceGui;
import org.fog.gui.core.Graph;
import org.fog.gui.core.Node;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Config;
import org.fog.utils.Util;

public class AddFogDevice extends JDialog {
	private static final long serialVersionUID = -5116677861770319577L;
	
	private final Graph graph;
	private final FogDeviceGui fog;
	
	private JLabel deviceNameLabel;
	private JLabel levelLabel;
	
	private JTextField deviceName;
	private JTextField upBw;
	private JTextField downBw;
	private JTextField mips;
	private JTextField ram;
	private JTextField storage;
	private JTextField rateMips;
	private JTextField rateRam;
	private JTextField rateStorage;
	private JTextField rateBwUp;
	private JTextField rateBwDown;
	private JTextField idlePower;
	private JTextField busyPower;
	private JTextField cost;
	
	private JComboBox<String> level;
	private JComboBox<String> application;

	public AddFogDevice(final Graph graph, final JFrame frame, final FogDeviceGui fog) {
		this.graph = graph;
		this.fog = fog;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);
		
		setTitle(fog == null ? "  Add Fog Device" : "  Edit Fog Device");
		setModal(true);
		setPreferredSize(new Dimension(700, 800));
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
	            		Util.prompt(AddFogDevice.this, "Cannot delete because there are fog nodes below this one.", "Error");
	            }
	        });
		}

		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String error_msg = "", name_ = "";
				int level_= -1;
				long ram_= -1, storage_ = -1;
				double mips_= -1, upBw_= -1, downBw_= -1, rateMips_ = -1, rateRam_ = -1, rateStorage_ = -1,
						rateBwUp_ = -1, rateBwDown_ = -1, idlePower_ = -1, busyPower_ = -1, cost_ = -1;
				
				if (Util.validString(deviceName.getText())) {
					if(fog == null || (fog != null && !fog.getName().equals(deviceName.getText())))
						if(graph.isRepeatedName(deviceName.getText()))
							error_msg += "\nName already exists";
				}else
					error_msg += "Missing name\n";
				
				if(deviceName.getText().contains(" "))
					error_msg += "Name cannot contain spaces\n";
				
				if (!Util.validString(upBw.getText())) error_msg += "Missing uplink BW\n";
				if (!Util.validString(downBw.getText())) error_msg += "Missing downlink BW\n";
				if (!Util.validString(mips.getText())) error_msg += "Missing Mips\n";
				if (!Util.validString(ram.getText())) error_msg += "Missing Ram\n";
				if (!Util.validString(storage.getText())) error_msg += "Missing storage\n";
				if (!Util.validString(rateMips.getText())) error_msg += "Missing rate/Mips\n";
				if (!Util.validString(rateRam.getText())) error_msg += "Missing rate/Ram\n";
				if (!Util.validString(rateStorage.getText())) error_msg += "Missing rate/Mem\n";
				if (!Util.validString(rateBwUp.getText())) error_msg += "Missing rate/BwUp\n";
				if (!Util.validString(rateBwDown.getText())) error_msg += "Missing rate/BwDown\n";
				if (!Util.validString(idlePower.getText())) error_msg += "Missing Idle Power\n";
				if (!Util.validString(busyPower.getText())) error_msg += "Missing Busy Power\n";
				if (!Util.validString(cost.getText())) error_msg += "Missing Cost Per Second\n";

				name_ = deviceName.getText();
				if((upBw_ = Util.stringToDouble(upBw.getText())) < 0) error_msg += "\nUplink bandwidth should be a positive number";
				if((downBw_ = Util.stringToDouble(downBw.getText())) < 0) error_msg += "\nDownlink bandwidth should be a positive number";
				if((mips_ = Util.stringToDouble(mips.getText())) < 0) error_msg += "\nMips should be a positive number";
				if((ram_ = Util.stringToInt(ram.getText())) < 0) error_msg += "\nRam should be a positive number";
				if((storage_ = Util.stringToInt(storage.getText())) < 0) error_msg += "\nMem should be a positive number";
				if((rateMips_ = Util.stringToDouble(rateMips.getText())) < 0) error_msg += "\nRate/Mips should be a positive number";
				if((rateRam_ = Util.stringToDouble(rateRam.getText())) < 0) error_msg += "\nRate/Ram should be a positive number";
				if((rateStorage_ = Util.stringToDouble(rateStorage.getText())) < 0) error_msg += "\nRate/Mem should be a positive number";
				if((rateBwUp_ = Util.stringToDouble(rateBwUp.getText())) < 0) error_msg += "\nRate/BwUp should be a positive number";
				if((rateBwDown_ = Util.stringToDouble(rateBwDown.getText())) < 0) error_msg += "\nRate/BwDown should be a positive number";
				if((idlePower_ = Util.stringToDouble(idlePower.getText())) < 0) error_msg += "\nIdle Power should be a positive number";
				if((busyPower_ = Util.stringToDouble(busyPower.getText())) < 0) error_msg += "\nBusy Power should be a positive number";
				if((cost_ = Util.stringToDouble(cost.getText())) < 0) error_msg += "\nCost Per Second should be a positive number";
				
				level_ = level.getSelectedIndex();

				String appId = (String)application.getSelectedItem();
				if(error_msg == "") {
					if(fog != null)
						fog.setValues(name_, level_, mips_, ram_, storage_, upBw_, downBw_, rateMips_, rateRam_,
								rateStorage_, rateBwUp_, rateBwDown_, idlePower_, busyPower_, cost_, appId);
					else {
						FogDeviceGui fogDevice = new FogDeviceGui(name_, level_, mips_, ram_, storage_, upBw_,
								downBw_, rateMips_, rateRam_, rateStorage_, rateBwUp_, rateBwDown_, idlePower_,
								busyPower_, cost_, appId);
						graph.addNode(fogDevice);
					}
					setVisible(false);								
				}else
					Util.prompt(AddFogDevice.this, error_msg, "Error");
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
        
		deviceNameLabel = new JLabel("Name: ");
		springPanel.add(deviceNameLabel);
		deviceName = new JTextField();
		
		int aux = 1;
		for(Node node : graph.getDevicesList().keySet())
			if(node.getType().equals(Config.FOG_TYPE))
				aux++;
		
		deviceName.setText(fog == null ? Config.FOG_NAME + aux : fog.getName());
		deviceNameLabel.setLabelFor(deviceName);
		springPanel.add(deviceName);
		
		levelLabel = new JLabel("Level: ");
		springPanel.add(levelLabel);
		
		ArrayList<String> choices = new ArrayList<String>();
		int maxLevel = graph.getMaxLevel();
		for(int i = 0; i <= maxLevel + 1; i++)
			choices.add(Integer.toString(i));
		level = new JComboBox<String>(new Vector<String>(choices));
		level.setSelectedIndex(fog == null ? maxLevel+1 : fog.getLevel());
		springPanel.add(level);
		
		upBw = Util.createInput(springPanel, upBw, "Uplink BW (MB/s): ", fog == null ? Double.toString(Config.BW_UP) : Double.toString(fog.getUpBw()));
		downBw = Util.createInput(springPanel, downBw, "Downlink BW (MB/s): ", fog == null ? Double.toString(Config.BW_DOWN) : Double.toString(fog.getDownBw()));
		mips = Util.createInput(springPanel, mips, "MIPS: ", fog == null ? Double.toString(Config.MIPS) : Double.toString(fog.getMips()));
		ram = Util.createInput(springPanel, ram, "RAM (MB): ", fog == null ? Long.toString(Config.RAM) : Long.toString(fog.getRam()));
		storage = Util.createInput(springPanel, storage, "MEM (MB): ", fog == null ? Long.toString(Config.MEM) : Long.toString(fog.getStorage()));
		rateMips = Util.createInput(springPanel, rateMips, "Rate/MIPS (€): ", fog == null ? Double.toString(Config.RATE_MIPS) : Double.toString(fog.getRateMips()));
		rateRam = Util.createInput(springPanel, rateRam, "Rate/RAM (€/sec for 1 MB): ", fog == null ? Double.toString(Config.RATE_RAM) : Double.toString(fog.getRateRam()));
		rateStorage = Util.createInput(springPanel, rateStorage, "Rate/MEM (€/sec for 1 MB): ", fog == null ? Double.toString(Config.RATE_MEM) : Double.toString(fog.getRateStorage()));
		rateBwUp = Util.createInput(springPanel, rateBwUp, "Rate/BwUp (€/1 MB): ", fog == null ? Double.toString(Config.RATE_BW_UP) : Double.toString(fog.getRateBwUp()));
		rateBwDown = Util.createInput(springPanel, rateBwDown, "Rate/BwDown (€/1 MB): ", fog == null ? Double.toString(Config.RATE_BW_DOWN) : Double.toString(fog.getRateBwDown()));
		idlePower = Util.createInput(springPanel, idlePower, "Idle Power (W): ", fog == null ? Double.toString(Config.IDLE_POWER) : Double.toString(fog.getIdlePower()));
		busyPower = Util.createInput(springPanel, busyPower, "Busy Power (W): ", fog == null ? Double.toString(Config.BUSY_POWER) : Double.toString(fog.getBusyPower()));
		cost = Util.createInput(springPanel, cost, "Cost Per Second: ", fog == null ? Double.toString(Config.COST_PER_SEC) : Double.toString(fog.getCostPerSec()));
		
		ArrayList<String> applicationIds = new ArrayList<String>();
		for(ApplicationGui applicationGui : graph.getAppList())
			applicationIds.add(applicationGui.getAppId());
		applicationIds.add("");
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		ComboBoxModel<String> applicationModel = new DefaultComboBoxModel(applicationIds.toArray());
		application = new JComboBox<>(applicationModel);
		
		if(fog == null || (fog != null && !Gui.hasSensorActuator(fog)))
			application.setVisible(false);
		
		JLabel lapplication = new JLabel("Application: ");
		springPanel.add(lapplication);
		lapplication.setLabelFor(application);
		
		String appId = "";
		if(fog != null && fog.getApplication() != null && fog.getApplication().length() > 0)
			appId = fog.getApplication();
		
		application.setSelectedItem(appId);
		springPanel.add(application);

		//rows, cols, initX, initY, xPad, yPad
        SpringUtilities.makeCompactGrid(springPanel, 16, 2, 6, 6, 6, 6);
		return springPanel;
	}
	
}
