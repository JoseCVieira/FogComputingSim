package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

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

import org.fog.core.Constants;
import org.fog.gui.GuiConfig;
import org.fog.gui.core.Graph;
import org.fog.gui.core.Node;
import org.fog.gui.core.SensorGui;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Util;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;

@SuppressWarnings({ "rawtypes", "unchecked" })
public class AddSensor extends JDialog {
	private static final long serialVersionUID = -511667786177319577L;
	
	private final Graph graph;
	private final SensorGui sensor;
	private JTextField sensorName;
	private JComboBox distribution;
	private JTextField uniformLowerBound;
	private JTextField uniformUpperBound;
	private JTextField deterministicValue;
	private JTextField normalMean;
	private JTextField normalStdDev;
	
	/**
	 * Constructor.
	 * 
	 * @param frame the parent frame
	 */
	public AddSensor(final Graph graph, final JFrame frame, final SensorGui sensor) {
		this.graph = graph;
		this.sensor = sensor;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(sensor == null ? "  Add Sensor" : "  Edit Sensor");
		setModal(true);
		setPreferredSize(new Dimension(500, 400));
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
		
		if(sensor != null) {
			delBtn.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	graph.removeNode(sensor);
	                setVisible(false);
	            }
	        });
		}
		
		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String error_msg = "";
				
				if (sensorName.getText() == null || sensorName.getText().length() < 1)
					error_msg += "Please type Sensor name";
				else if (distribution.getSelectedIndex() < 0)
					error_msg += "Please select Emission time distribution";
				else {
					double normalMean_ = -1;
					double normalStdDev_ = -1;
					double uniformLow_ = -1;
					double uniformUp_ = -1;
					double deterministicVal_ = -1;
					String dist = (String)distribution.getSelectedItem();
					
					if(sensor == null || (sensor != null && !sensor.getName().equals(sensorName.getText())))
						if(graph.isRepeatedName(sensorName.getText()))
							error_msg += "Name already exists";
					
					if(sensorName.getText().contains(" "))
						error_msg += "Name cannot contain spaces\n";
					
					if(dist.equals("Normal")){
						if (!Util.validString(normalMean.getText())) error_msg += "Missing Normal Mean\n";
						if (!Util.validString(normalStdDev.getText())) error_msg += "Missing Standard Deviation\n";
						if((normalMean_ = Util.stringToDouble(normalMean.getText())) < 0) error_msg += "\nNormal Mean should be a positive number";
						if((normalStdDev_ = Util.stringToDouble(normalStdDev.getText())) < 0) error_msg += "\nStandard Deviation should be a positive number";
						
						if(error_msg == "") {
							if(sensor != null) {
								sensor.setValues(sensorName.getText().toString(), (String)distribution.getSelectedItem(),
										normalMean_, normalStdDev_, uniformLow_, uniformUp_, deterministicVal_);
							}else {
								SensorGui sensor = new SensorGui(sensorName.getText().toString(), (String)distribution.getSelectedItem(),
										normalMean_, normalStdDev_, uniformLow_, uniformUp_, deterministicVal_);
								graph.addNode(sensor);
							}
						}
					}else if(dist.equals("Uniform")){
						if (!Util.validString(uniformLowerBound.getText())) error_msg += "Missing Uniform Lower Bound\n";
						if (!Util.validString(uniformUpperBound.getText())) error_msg += "Missing Uniform Upper Bound\n";
						if((uniformLow_ = Util.stringToDouble(uniformLowerBound.getText())) < 0) error_msg += "\nUniform Lower Bound should be a positive number";
						if((uniformUp_ = Util.stringToDouble(uniformUpperBound.getText())) < 0) error_msg += "\nUniform Upper Bound should be a positive number";
						
						if(error_msg == "") {
							if(sensor != null) {
								sensor.setValues(sensorName.getText().toString(), (String)distribution.getSelectedItem(),
										normalMean_, normalStdDev_, uniformLow_, uniformUp_, deterministicVal_);
							}else {
								SensorGui sensor = new SensorGui(sensorName.getText().toString(), (String)distribution.getSelectedItem(),
										normalMean_, normalStdDev_, uniformLow_, uniformUp_, deterministicVal_);
								graph.addNode(sensor);
							}
						}
					}else if(dist.equals("Deterministic")){
						if (!Util.validString(deterministicValue.getText())) error_msg += "Missing Deterministic Value\n";
						if((deterministicVal_ = Util.stringToDouble(deterministicValue.getText())) < 0) error_msg += "\nDeterministic Value should be a positive number";
						
						if(error_msg == "") {
							if(sensor != null) {
								sensor.setValues(sensorName.getText().toString(), (String)distribution.getSelectedItem(),
										normalMean_, normalStdDev_, uniformLow_, uniformUp_, deterministicVal_);
							}else {
								System.out.println("AQUI3");
								SensorGui sensor = new SensorGui(sensorName.getText().toString(), (String)distribution.getSelectedItem(),
										normalMean_, normalStdDev_, uniformLow_, uniformUp_, deterministicVal_);
								graph.addNode(sensor);
							}
						}
					}
					
					if(error_msg == "")
						setVisible(false);
					else
						Util.prompt(AddSensor.this, error_msg, "Error");
				}
			}
		});

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okBtn);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelBtn);
		if(sensor != null) {
			buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
			buttonPanel.add(delBtn);
		}
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		return buttonPanel;
	}

	private JPanel createInputPanelArea() {
	    String[] distributionType = {"Normal", "Uniform", "Deterministic"};
 
        //Create and populate the panel.
        JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		
		JLabel lName = new JLabel("Name: ");
		springPanel.add(lName);
		sensorName = new JTextField();
		
		int aux = 1;
		for(Node node : graph.getDevicesList().keySet())
			if(node.getType().equals(Constants.SENSOR_TYPE))
				aux++;
		
		sensorName.setText(sensor == null ? GuiConfig.SENSOR_NAME + aux : sensor.getName());
		lName.setLabelFor(sensorName);
		springPanel.add(sensorName);
				
		JLabel distLabel = new JLabel("Distribution Type: ", JLabel.TRAILING);
		springPanel.add(distLabel);	
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
		
		springPanel.add(distribution);
		
		normalMean = Util.createInput(springPanel, normalMean, "Mean: ", (sensor != null && sensor.getDistribution() instanceof NormalDistribution) ? Double.toString(((NormalDistribution) sensor.getDistribution()).getMean()) : "");
		normalStdDev = Util.createInput(springPanel, normalStdDev, "StdDev: ", (sensor != null && sensor.getDistribution() instanceof NormalDistribution) ? Double.toString(((NormalDistribution) sensor.getDistribution()).getStdDev()) : "");
		uniformLowerBound = Util.createInput(springPanel, uniformLowerBound, "Min: ", (sensor != null && sensor.getDistribution() instanceof UniformDistribution) ? Double.toString(((UniformDistribution) sensor.getDistribution()).getMin()) : "");
		uniformUpperBound = Util.createInput(springPanel, uniformUpperBound, "Max: ", (sensor != null && sensor.getDistribution() instanceof UniformDistribution) ? Double.toString(((UniformDistribution) sensor.getDistribution()).getMax()) : "");
		deterministicValue = Util.createInput(springPanel, deterministicValue, "Value: ", (sensor != null && sensor.getDistribution() instanceof DeterministicDistribution) ? Double.toString(((DeterministicDistribution) sensor.getDistribution()).getValue()) : "");

		if(sensor != null) {
			String item = "";
			if(sensor.getDistributionType() == Distribution.NORMAL) {
				item = "Normal";
				distribution.setSelectedIndex(0);
			}else if(sensor.getDistributionType() == Distribution.UNIFORM) {
				item = "Uniform";
				distribution.setSelectedIndex(1);
			}else if(sensor.getDistributionType() == Distribution.DETERMINISTIC) {
				item = "Deterministic";
				distribution.setSelectedIndex(2);
			}
			
			updatePanel(item);
		}
						
		//rows, cols, initX, initY, xPad, yPad
        SpringUtilities.makeCompactGrid(springPanel, 7, 2, 6, 6, 6, 6);
		return springPanel;
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
