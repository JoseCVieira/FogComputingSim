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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.fog.gui.core.FogDeviceGui;
import org.fog.gui.core.Graph;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Config;
import org.fog.utils.Util;

public class AddFogDevice extends JDialog {
	private static final long serialVersionUID = -5116677861770319577L;
	
	private final Graph graph;
	private final FogDeviceGui fog;
	
	private JLabel deviceNameLabel;
	private JLabel upBwLabel;
	private JLabel downBwLabel;
	private JLabel mipsLabel;
	private JLabel ramLabel;
	private JLabel storageLabel;
	private JLabel levelLabel;
	private JLabel rateMipsLabel;
	private JLabel rateRamLabel;
	private JLabel rateStorageLabel;
	private JLabel rateBwUpLabel;
	private JLabel rateBwDownLabel;
	
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
	private JComboBox<String> level;

	public AddFogDevice(final Graph graph, final JFrame frame, final FogDeviceGui fog) {
		this.graph = graph;
		this.fog = fog;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);
		
		setTitle(fog == null ? "  Add Fog Device" : "  Edit Fog Device");
		setModal(true);
		setPreferredSize(new Dimension(500, 500));
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
	            	graph.removeNode(fog);
	                setVisible(false);
	            }
	        });
		}

		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String error_msg = "", name_ = "";
				int level_= -1;
				long ram_= -1, storage_ = -1;
				double mips_= -1, upBw_= -1, downBw_= -1, rateMips_ = -1, rateRam_ = -1, rateStorage_ = -1,
						rateBwUp_ = -1, rateBwDown_ = -1;
				
				if (Util.validString(deviceName.getText())) {
					if(fog == null || (fog != null && !fog.getName().equals(deviceName.getText()))) {
						if(graph.isRepeatedName(deviceName.getText())) {
							error_msg += "\nName already exists";
						}
					}
				}else
					error_msg += "Missing name\n";
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
				

				name_ = deviceName.getText();
				if((upBw_ = Util.stringToDouble(upBw.getText())) < 0) error_msg += "\nUplink bandwidth be a positive number";
				if((downBw_ = Util.stringToDouble(downBw.getText())) < 0) error_msg += "\nDownlink bandwidth should be a positive number";
				if((mips_ = Util.stringToDouble(mips.getText())) < 0) error_msg += "\nMips should be a positive number";
				if((ram_ = Util.stringToInt(ram.getText())) < 0) error_msg += "\nRam should be a positive number";
				if((storage_ = Util.stringToInt(storage.getText())) < 0) error_msg += "\nMem should be a positive number";
				if((rateMips_ = Util.stringToDouble(rateMips.getText())) < 0) error_msg += "\nRate/Mips should be a positive number";
				if((rateRam_ = Util.stringToDouble(rateRam.getText())) < 0) error_msg += "\nRate/Ram should be a positive number";
				if((rateStorage_ = Util.stringToDouble(rateStorage.getText())) < 0) error_msg += "\nRate/Mem should be a positive number";
				if((rateBwUp_ = Util.stringToDouble(rateBwUp.getText())) < 0) error_msg += "\nRate/BwUp should be a positive number";
				if((rateBwDown_ = Util.stringToDouble(rateBwDown.getText())) < 0) error_msg += "\nRate/BwDown should be a positive number";
				level_ = level.getSelectedIndex();

				if(error_msg == "") {
					if(fog != null)
						fog.setValues(name_, level_, mips_, ram_, storage_, upBw_, downBw_, rateMips_, rateRam_, rateStorage_, rateBwUp_, rateBwDown_);
					else {
						FogDeviceGui fogDevice = new FogDeviceGui(name_, level_, mips_, ram_, storage_, upBw_, downBw_, rateMips_, rateRam_, rateStorage_,
								rateBwUp_, rateBwDown_);
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
		deviceName.setText(fog == null ? Config.FOG_NAME + graph.getDevicesList().size() : fog.getName());
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
		
		upBwLabel = new JLabel("Uplink BW (MB/s): ");
		springPanel.add(upBwLabel);
		upBw = new JTextField();
		upBw.setText(fog == null ? Double.toString(Config.BW_UP) : Double.toString(fog.getUpBw()));
		upBwLabel.setLabelFor(upBw);
		springPanel.add(upBw);
		
		downBwLabel = new JLabel("Downlink BW (MB/s): ");
		springPanel.add(downBwLabel);
		downBw = new JTextField();
		downBw.setText(fog == null ? Double.toString(Config.BW_DOWN) : Double.toString(fog.getDownBw()));
		downBwLabel.setLabelFor(downBw);
		springPanel.add(downBw);
		
		mipsLabel = new JLabel("MIPS: ");
		springPanel.add(mipsLabel);	
		mips = new JTextField();
		mips.setText(fog == null ? Double.toString(Config.MIPS) : Double.toString(fog.getMips()));
		mipsLabel.setLabelFor(mips);
		springPanel.add(mips);
		
		ramLabel = new JLabel("RAM (MB): ");
		springPanel.add(ramLabel);
		ram = new JTextField();
		ram.setText(fog == null ? Long.toString(Config.RAM) : Long.toString(fog.getRam()));
		ramLabel.setLabelFor(ram);
		springPanel.add(ram);
		
		storageLabel = new JLabel("MEM (MB): ");
		springPanel.add(storageLabel);
		storage = new JTextField();
		storage.setText(fog == null ? Long.toString(Config.MEM) : Long.toString(fog.getStorage()));
		storageLabel.setLabelFor(storage);
		springPanel.add(storage);
		
		rateMipsLabel = new JLabel("Rate/MIPS (€): ");
		springPanel.add(rateMipsLabel);
		rateMips = new JTextField();
		rateMips.setText(fog == null ? Double.toString(Config.RATE_MIPS) : Double.toString(fog.getRateMips()));
		rateMipsLabel.setLabelFor(rateMips);
		springPanel.add(rateMips);
		
		rateRamLabel = new JLabel("Rate/RAM (€/sec for 1 MB): ");
		springPanel.add(rateRamLabel);
		rateRam = new JTextField();
		rateRam.setText(fog == null ? Double.toString(Config.RATE_RAM) : Double.toString(fog.getRateRam()));
		rateRamLabel.setLabelFor(rateRam);
		springPanel.add(rateRam);
		
		rateStorageLabel = new JLabel("Rate/MEM (€/sec for 1 MB): ");
		springPanel.add(rateStorageLabel);
		rateStorage = new JTextField();
		rateStorage.setText(fog == null ? Double.toString(Config.RATE_MEM) : Double.toString(fog.getRateStorage()));
		rateStorageLabel.setLabelFor(rateStorage);
		springPanel.add(rateStorage);
		
		rateBwUpLabel = new JLabel("Rate/BwUp (€/1 MB): ");
		springPanel.add(rateBwUpLabel);
		rateBwUp = new JTextField();
		rateBwUp.setText(fog == null ? Double.toString(Config.RATE_BW_UP) : Double.toString(fog.getRateBwUp()));
		rateBwUpLabel.setLabelFor(rateBwUp);
		springPanel.add(rateBwUp);
		
		rateBwDownLabel = new JLabel("Rate/BwDown (€/1 MB): ");
		springPanel.add(rateBwDownLabel);
		rateBwDown = new JTextField();
		rateBwDown.setText(fog == null ? Double.toString(Config.RATE_BW_DOWN) : Double.toString(fog.getRateBwDown()));
		rateBwDownLabel.setLabelFor(rateBwDown);
		springPanel.add(rateBwDown);

		//rows, cols, initX, initY, xPad, yPad
        SpringUtilities.makeCompactGrid(springPanel, 12, 2, 6, 6, 6, 6);
		return springPanel;
	}
}
