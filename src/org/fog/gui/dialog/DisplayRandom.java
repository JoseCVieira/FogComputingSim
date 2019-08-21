package org.fog.gui.dialog;

import java.awt.BorderLayout;
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
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.gui.GuiConfig;
import org.fog.gui.GuiMsg;
import org.fog.gui.GuiUtils;
import org.fog.gui.core.Graph;
import org.fog.gui.core.Link;
import org.fog.gui.core.Node;
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
	private static final int WIDTH = 500;
	private static final int HEIGHT = 500;
	
	Graph physicalGraph;
	
	private JTextField jTextNrFogDev;
	private JTextField jTextMaxLat;
	private JTextField jTextMaxBw;
	private JTextField jTextConnProb;
	private JTextField jTextClientProb;
	private JTextField jTextResourceDev;
	private JTextField jTextEnergyDev;
	private JTextField jTextCostDev;
	
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
	 * Creates the editable random topology settings.
	 * 
	 * @return the panel containing the input settings
	 */
	private JPanel createInputPanel() {
		JPanel jPanel = new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
		
		String nrFogDevOp = Integer.toString(GuiConfig.NR_FOG_DEVICES);
		String maxLatOp = Double.toString(GuiConfig.MAX_CONN_LAT);
		String maxBwOp = Double.toString(GuiConfig.MAX_CONN_BW);
		String connectionOp = Double.toString(GuiConfig.CONNECTION_PROB);
		String clientOp = Double.toString(GuiConfig.CLIENT_PROBABILITY);
		String resDevOp = Double.toString(GuiConfig.RESOURCES_DEV);
		String enDevOp = Double.toString(GuiConfig.ENERGY_DEV);
		String costDevOp = Double.toString(GuiConfig.COST_DEV);
		
		jTextNrFogDev = GuiUtils.createInput(jPanel, jTextNrFogDev, "Number of fog devices: ", nrFogDevOp, GuiMsg.TipRandNrFog);
		jTextMaxLat = GuiUtils.createInput(jPanel, jTextMaxLat, "Maximum link latency [s]: ", maxLatOp, GuiMsg.TipRandMaxLat);
		jTextMaxBw = GuiUtils.createInput(jPanel, jTextMaxBw, "Maximum link bandwidth [B/s]: ", maxBwOp, GuiMsg.TipRandMaxBw);
		jTextConnProb = GuiUtils.createInput(jPanel, jTextConnProb, "Connect fog devices probability: ", connectionOp, GuiMsg.TipRandConnect);
		jTextClientProb = GuiUtils.createInput(jPanel, jTextClientProb, "Client probability: ", clientOp, GuiMsg.TipRandClient);
		jTextResourceDev = GuiUtils.createInput(jPanel, jTextResourceDev, "Resource deviation: ", resDevOp, GuiMsg.TipRandResDev);
		jTextEnergyDev = GuiUtils.createInput(jPanel, jTextEnergyDev, "Energy deviation: ", enDevOp, GuiMsg.TipRandEnDev);
		jTextCostDev = GuiUtils.createInput(jPanel, jTextCostDev, "Cost deviation: ", costDevOp, GuiMsg.TipRandCostDev);
        
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
            	int nrFogDev_;
            	double maxLat_, maxBw_, connProb_, clientProb_, resDev_, enDev_, costDev_;
            	
            	if (!Util.validString(jTextNrFogDev.getText()))		error_msg += GuiMsg.errMissing("Number of fog devices");
            	if (!Util.validString(jTextMaxLat.getText())) 		error_msg += GuiMsg.errMissing("Maximum link latency");
            	if (!Util.validString(jTextMaxBw.getText())) 		error_msg += GuiMsg.errMissing("Maximum link bandwidth");
            	if (!Util.validString(jTextConnProb.getText())) 	error_msg += GuiMsg.errMissing("Connect fog devices probability");
            	if (!Util.validString(jTextClientProb.getText())) 	error_msg += GuiMsg.errMissing("Client probability");
            	if (!Util.validString(jTextResourceDev.getText())) 	error_msg += GuiMsg.errMissing("Resource deviation");
            	if (!Util.validString(jTextEnergyDev.getText())) 	error_msg += GuiMsg.errMissing("Energy deviation");
            	if (!Util.validString(jTextCostDev.getText())) 		error_msg += GuiMsg.errMissing("Cost deviation");
            	
            	if((nrFogDev_ = Util.stringToInt(jTextNrFogDev.getText())) < 0)				error_msg += GuiMsg.errFormat("Number of fog devices");
            	if((maxLat_ = Util.stringToDouble(jTextMaxLat.getText())) < 0)				error_msg += GuiMsg.errFormat("Maximum link latency");
            	if((maxBw_ = Util.stringToDouble(jTextMaxBw.getText())) < 0)				error_msg += GuiMsg.errFormat("Maximum link bandwidth");
            	if((connProb_ = Util.stringToProbability(jTextConnProb.getText())) < 0)		error_msg += GuiMsg.errFormat("Connect fog devices probability");
            	if((clientProb_ = Util.stringToProbability(jTextClientProb.getText())) < 0)	error_msg += GuiMsg.errFormat("Client probability");
            	if((resDev_ = Util.stringToDouble(jTextResourceDev.getText())) < 0)			error_msg += GuiMsg.errFormat("Resource deviation");
            	if((enDev_ = Util.stringToDouble(jTextEnergyDev.getText())) < 0)			error_msg += GuiMsg.errFormat("Energy deviation");
            	if((costDev_ = Util.stringToDouble(jTextCostDev.getText())) < 0)			error_msg += GuiMsg.errFormat("Cost deviation");
				
            	if(error_msg.isEmpty()) {
            		GuiConfig.NR_FOG_DEVICES = nrFogDev_;
            		GuiConfig.MAX_CONN_LAT = maxLat_;
            		GuiConfig.MAX_CONN_BW = maxBw_;
            		GuiConfig.CONNECTION_PROB = connProb_;
            		GuiConfig.CLIENT_PROBABILITY = clientProb_;
            		GuiConfig.RESOURCES_DEV = resDev_;
            		GuiConfig.ENERGY_DEV = enDev_;
            		GuiConfig.COST_DEV = costDev_;
            		
            		physicalGraph.clean();
            		
            		createApplications();
            		createFogDevices();
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
	
	private void createApplications() {
		ApplicationsExample.createExampleApplications();
		
		for(int i = 0; i < ApplicationsExample.getNumberOfApplicationsExample(); i++) {
			physicalGraph.addApp(ApplicationsExample.getAppExampleByIndex(i));
		}
		
		ApplicationsExample.clean();
		
	}
	
	private void createFogDevices() {
		// Create the movement for the cloud
		// Does not matter what direction because velocity is 0
		Movement movement = new Movement(0.0, Movement.EAST, new Location(0, 0));
		
		// Create the cloud device (cloud is seen as a single node)		
		Node fogDevice = new Node("Cloud", 0, Double.MAX_VALUE, (int) Constants.INF, (int) Constants.INF, GuiConfig.RATE_MIPS,
				GuiConfig.RATE_RAM, GuiConfig.RATE_STRG, GuiConfig.RATE_BW, GuiConfig.RATE_EN, 16*GuiConfig.IDLE_POWER,
				16*GuiConfig.BUSY_POWER, movement, "", null);
		physicalGraph.addNode(fogDevice);
		
		int iter = 1;
		int nrFogNodes = GuiConfig.NR_FOG_DEVICES - 1;
		
		// While there are some nodes which have been not created yet
		while(nrFogNodes > 0) {
			
			// Generate a random number of nodes
			int nr = Util.rand(1, nrFogNodes);			
			nrFogNodes -= nr;
			
			// For each node
			for(int i = 0; i < nr; i++) {
				// Generate the quantity of resources
				double mips = Util.normalRand(GuiConfig.MIPS/iter, GuiConfig.RESOURCES_DEV/iter);
				double ram = Util.normalRand(GuiConfig.RAM/iter, GuiConfig.RESOURCES_DEV/iter);
				double strg = Util.normalRand(GuiConfig.STRG/iter, GuiConfig.RESOURCES_DEV/iter);
				
				double bPw = Util.normalRand(GuiConfig.BUSY_POWER, GuiConfig.ENERGY_DEV);
				double iPw = Util.normalRand(GuiConfig.IDLE_POWER, GuiConfig.ENERGY_DEV);
				
				double rateMips = Util.normalRand(GuiConfig.RATE_MIPS, GuiConfig.COST_DEV);
				double rateRam = Util.normalRand(GuiConfig.RATE_RAM, GuiConfig.COST_DEV);
				double rateStrg = Util.normalRand(GuiConfig.RATE_STRG, GuiConfig.COST_DEV);
				double rateBw = Util.normalRand(GuiConfig.RATE_BW, GuiConfig.COST_DEV);
				double rateEn = Util.normalRand(GuiConfig.RATE_EN, GuiConfig.COST_DEV);
				
				double posx = Util.rand(0, Config.SQUARE_SIDE);
				double posy = Util.rand(0, Config.SQUARE_SIDE);
				
				// Generate its movement
				movement = new Movement(0.0, Movement.EAST, new Location(posx, posy));
				
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
				
				Distribution distribution = null;
				String appName = "";
				if(client) {
					distribution = new DeterministicDistribution(Util.normalRand(GuiConfig.SENSOR_DESTRIBUTION, 1.0));
					
					int appIndex = new Random().nextInt(physicalGraph.getAppList().size());
					appName = physicalGraph.getAppList().get(appIndex).getAppId();
				}
				
				fogDevice = new Node("L"+iter+":F"+i, iter, mips, (int) ram, (long) strg, rateMips,
						rateRam, rateStrg, rateBw, rateEn, iPw, bPw, movement, appName, distribution);
				physicalGraph.addNode(fogDevice);
			}
			
			iter++;
		}
	}
	
	private void connectDevices() {
		ArrayList<Node> notConnctedDevices = getListFromSet(physicalGraph.getDevicesList().keySet());
		Node cloud = getFromList(notConnctedDevices, "Cloud");
		notConnctedDevices.remove(cloud);
		Node f = notConnctedDevices.get(new Random().nextInt(notConnctedDevices.size()));
		
		double lat = Util.rand(GuiConfig.MAX_CONN_LAT/3, GuiConfig.MAX_CONN_LAT);
		double bw = Util.rand(GuiConfig.MAX_CONN_BW/3, GuiConfig.MAX_CONN_BW);
		physicalGraph.addEdge(cloud, new Link(f, lat, bw));
		
		// Defines each are the mobile nodes
		List<Node> toRemove = new ArrayList<Node>();
		for(Node f1 : notConnctedDevices) {
			if(f1.getName().equals(f.getName())) continue;
			if(new Random().nextFloat() < GuiConfig.CONNECTION_PROB) continue;
			toRemove.add(f1);
		}
		notConnctedDevices.removeAll(toRemove);
		
		// The fixed nodes must have a closed loop
		while(notConnctedDevices.size() > 1) {
			toRemove = new ArrayList<Node>();
			boolean connected = false;
			
			// Randomly connect the remaining nodes
			for(Node f1 : notConnctedDevices) {
				String lastNodeName = notConnctedDevices.get(notConnctedDevices.size()-1).getName();
				
				for(Node f2 : notConnctedDevices) {
					if(f1.getName().equals(f2.getName())) continue;
					if(new Random().nextFloat() >= GuiConfig.CONNECTION_PROB) {
						if(!f2.getName().equals(lastNodeName)) continue;
						
						lat = Util.rand(GuiConfig.MAX_CONN_LAT/3, GuiConfig.MAX_CONN_LAT);
						bw = Util.rand(GuiConfig.MAX_CONN_BW/3, GuiConfig.MAX_CONN_BW);
						physicalGraph.addEdge(f2, new Link(f1, lat, bw));
						connected = true;
						break;
					}
					
					lat = Util.rand(GuiConfig.MAX_CONN_LAT/3, GuiConfig.MAX_CONN_LAT);
					bw = Util.rand(GuiConfig.MAX_CONN_BW/3, GuiConfig.MAX_CONN_BW);
					
					physicalGraph.addEdge(f2, new Link(f1, lat, bw));
					
					toRemove.add(f1);
					
					connected = true;
					break;
				}
				
				if(connected) break;
			}
			
			notConnctedDevices.removeAll(toRemove);
		}
	}
	
	private Node getFromList(ArrayList<Node> nodes, String name) {
		for(Node f : nodes) {
			if(f.getName().equals(name)) return f;
		}
		
		return null;
	}
	
	private ArrayList<Node> getListFromSet(Set<Node> nodes) {
		ArrayList<Node> returnList = new ArrayList<Node>();
		
		for(Node f : nodes) {
			returnList.add(f);
		}
		
		return returnList;
	}
	
}
