package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.entities.Tuple;
import org.fog.gui.core.ApplicationGui;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Config;
import org.fog.utils.Util;
import org.fog.utils.Util.AppModulesCellRenderer;

public class AddAppEdge extends JDialog {
	private static final long serialVersionUID = -511667786177319577L;
	private static final int WIDTH = 600;
	private static final int HEIGHT = 600;
	
	private final AppEdge edge;
	private final ApplicationGui app;
	
	private JTextField tupleCpuLength;
	private JTextField tupleNwLength;
	private JTextField tupleType;
	private JTextField periodicity;
	private JTextField actuatorName;
	private JTextField sensorName;
	
	private JLabel lsensor;
	private JLabel lactuator;
	private JLabel lsourceNode;
	private JLabel ltargetNode;
	
	private JComboBox<String> direction;
	private JComboBox<String> edgeType;
	private JComboBox<String> sourceNode;
	private JComboBox<String> targetNode;
	private JComboBox<String> periodic;
	
	public AddAppEdge(final JFrame frame, final ApplicationGui app, final AppEdge edge) {
		this.app = app;
		this.edge = edge;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(edge == null ? "  Add Application Edge" : "  Edit Application Edge");
		setModal(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}

	@SuppressWarnings("unchecked")
	private JPanel createInputPanelArea() {
		JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		@SuppressWarnings({ "rawtypes" })
		ComboBoxModel<String> sourceModel = new DefaultComboBoxModel(app.getModules().toArray());
		
		@SuppressWarnings({ "rawtypes" })
		ComboBoxModel<String> targetModel = new DefaultComboBoxModel(app.getModules().toArray());

		@SuppressWarnings({ "rawtypes" })
		ComboBoxModel<String> directionModel =
		new DefaultComboBoxModel(Arrays.asList("UP", "DOWN").toArray());
		
		@SuppressWarnings({ "rawtypes" })
		ComboBoxModel<String> edgeTypeModel =
		new DefaultComboBoxModel(Arrays.asList("SENSOR", "ACTUATOR", "MODULE").toArray());
		
		@SuppressWarnings({ "rawtypes" })
		ComboBoxModel<String> periodicModel =
		new DefaultComboBoxModel(Arrays.asList("YES", "NO").toArray());
		
		sourceNode = new JComboBox<>(sourceModel);
		targetNode = new JComboBox<>(targetModel);
		direction = new JComboBox<>(directionModel);
		edgeType = new JComboBox<>(edgeTypeModel);
		periodic = new JComboBox<>(periodicModel); 
		
		AppModulesCellRenderer renderer = new Util.AppModulesCellRenderer();
		sourceNode.setRenderer(renderer);
		targetNode.setRenderer(renderer);
		
		JLabel ledgeType = new JLabel("Edge Type: ");
		springPanel.add(ledgeType);

		String eType = "MODULE";
		if(edge != null) {
			if(edge.getEdgeType() == AppEdge.ACTUATOR)
				eType = "ACTUATOR";
			else if(edge.getEdgeType() == AppEdge.SENSOR)
				eType = "SENSOR";
		}
		
		ledgeType.setLabelFor(edgeType);
		edgeTypeModel.setSelectedItem(edge == null ? null : eType);
		springPanel.add(edgeType);
		
		lsourceNode = new JLabel("From: ");
		springPanel.add(lsourceNode);
		lsourceNode.setLabelFor(sourceNode);
		springPanel.add(sourceNode);
		
		lsensor = new JLabel("Sensor: ");
		sensorName = new JTextField();
		springPanel.add(lsensor);
		lsensor.setLabelFor(sensorName);
		springPanel.add(sensorName);
		
		ltargetNode = new JLabel("To: ");
		springPanel.add(ltargetNode);
		ltargetNode.setLabelFor(targetNode);
		springPanel.add(targetNode);
		
		lactuator = new JLabel("Actuator: ");
		actuatorName = new JTextField();
		springPanel.add(lactuator);
		lactuator.setLabelFor(actuatorName);
		springPanel.add(actuatorName);
		
		String dir = "";
		if(edge != null) {
			if(edge.getDirection() == Tuple.UP)
				dir = "UP";
			else
				dir = "DOWN";
		}
		
		direction = Util.createDropDown(springPanel, direction, "Direction: ", directionModel, dir);
		periodic = Util.createDropDown(springPanel, periodic, "Periodic: ", periodicModel, null);
		periodicity = Util.createInput(springPanel, periodicity, "Periodicity: ", edge == null ? Double.toString(Config.EDGE_PERIODICITY) : Double.toString(edge.getPeriodicity()));		
		tupleCpuLength = Util.createInput(springPanel, tupleCpuLength, "Tuple CPU Length: ", edge == null ? Double.toString(Config.EDGE_CPU_LENGTH) : Double.toString(edge.getTupleCpuLength()));
		tupleNwLength = Util.createInput(springPanel, tupleNwLength, "Tuple NW Length: ", edge == null ? Double.toString(Config.EDGE_NW_LENGTH) : Double.toString(edge.getTupleNwLength()));
		tupleType = Util.createInput(springPanel, tupleType, "Tuple Type: ", edge == null ? "" : edge.getTupleType());
		
		if(edge != null && edge.getEdgeType() == AppEdge.SENSOR) {
			sensorName.setText(edge.getSource());
			sensorName.setEditable(false);
			lsourceNode.setVisible(false);
			sourceNode.setVisible(false);
		}else if(edge != null) {
			sensorName.setVisible(false);
			lsensor.setVisible(false);
		}
		
		if(edge != null && edge.getEdgeType() == AppEdge.ACTUATOR) {
			actuatorName.setText(edge.getDestination());
			actuatorName.setEditable(false);
			ltargetNode.setVisible(false);
			targetNode.setVisible(false);
		}else if(edge != null) {
			actuatorName.setVisible(false);
			lactuator.setVisible(false);
		}
		
		if(edge != null) {
			periodicModel.setSelectedItem(edge.isPeriodic() ? "YES" : "NO");
			if(!edge.isPeriodic()) periodicity.setVisible(false);
		}
		
		sourceNode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				AppModule selectedNode = (AppModule) sourceNode.getSelectedItem();
				if (selectedNode == null) return;
				
				targetNode.removeAllItems();
				List<AppModule> nodesToDisplay = new ArrayList<AppModule>();
				
				for(AppModule appModule : app.getModules())
					if(!appModule.getName().equals(selectedNode.getName()))
						nodesToDisplay.add(appModule);
				
				@SuppressWarnings("rawtypes")
				ComboBoxModel<String> targetModel = new DefaultComboBoxModel(nodesToDisplay.toArray());
				targetNode.setModel(targetModel);
			}
		});
		
		targetNode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				AppModule selectedSourceNode = (AppModule) sourceNode.getSelectedItem();
				AppModule selectedTargetNode = (AppModule) targetNode.getSelectedItem();
				String eType = (String) edgeType.getSelectedItem();
				
				if(selectedSourceNode != null && eType != null && eType.equals("MODULE") &&
						selectedSourceNode.equals(selectedTargetNode))
					targetModel.setSelectedItem(null);
			}
		});
		
		periodic.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if((String) periodic.getSelectedItem() == null) return;
				
				if(((String) periodic.getSelectedItem()).equals("YES"))
					periodicity.setVisible(true);
				else
					periodicity.setVisible(false);
			}
		});
		
		edgeType.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				if((String) edgeType.getSelectedItem() == null) return;
				String eType = (String) edgeType.getSelectedItem();
				
				changeEdgeType(eType);
				
				if(eType.equals("SENSOR")) {
					@SuppressWarnings({ "rawtypes" })
					ComboBoxModel<String> targetModel = new DefaultComboBoxModel(app.getModules().toArray());
					targetNode.setModel(targetModel);
				}else if(eType.equals("ACTUATOR")) {
					@SuppressWarnings({ "rawtypes" })
					ComboBoxModel<String> sourceModel = new DefaultComboBoxModel(app.getModules().toArray());
					sourceNode.setModel(sourceModel);
				}
			}
		});
		
		if(edge != null && edge.getEdgeType() != AppEdge.SENSOR) {
			for(AppModule appModule : app.getModules()) {
				if(appModule.getName().equals(edge.getSource())) {
					sourceModel.setSelectedItem(appModule);
					break;
				}
			}
		}else if(edge == null) {
			sourceModel.setSelectedItem(null);
			targetModel.setSelectedItem(null);
		}
		
		if(edge != null && edge.getEdgeType() != AppEdge.ACTUATOR) {
			for(AppModule appModule : app.getModules()) {
				if(appModule.getName().equals(edge.getDestination())) {
					targetModel.setSelectedItem(appModule);
					break;
				}
			}
		}
		
		SpringUtilities.makeCompactGrid(springPanel, 11, 2, 6, 6, 6, 6);
		return springPanel;
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
		
		if(edge != null) {
			delBtn.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	for(AppModule appModule : app.getModules()) {
	            		for(Pair<String, String> pair : appModule.getSelectivityMap().keySet()) {
	            			if(pair.getFirst().equals(appModule.getName()) ||
	            					pair.getSecond().equals(appModule.getName())) {
	            				appModule.getSelectivityMap().remove(pair);
	            			}
	            		}
	            	}
	            	
	            	app.getEdges().remove(edge);
	                setVisible(false);
	            }
	        });
		}

		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String error_msg = "", srcName_ = "", dstName_ = "";
				double tupleCpuLength_ = -1, tupleNwLength_ = -1, periodicity_ = -1;
				
				if(!sensorName.isVisible()) {
					AppModule source = (AppModule)sourceNode.getSelectedItem();
					if(source != null) srcName_ = source.getName();
					else error_msg += "Missing Source\n";
				}else if (!Util.validString(sensorName.getText()))
					error_msg += "Missing Sensor Name\n";
				else if(!sensorName.getText().contains(" "))
					srcName_ = sensorName.getText();
				else
					error_msg += "Sensor name cannot contain spaces\n";
				
				if(!actuatorName.isVisible()) {
					AppModule dst = (AppModule)targetNode.getSelectedItem();
					if(dst != null) dstName_ = dst.getName();
					else error_msg += "Missing Destination\n";
				}else if (!Util.validString(actuatorName.getText()))
					error_msg += "Missing Sensor Name\n";
				else if(!actuatorName.getText().contains(" "))
					dstName_ = actuatorName.getText();
				else
					error_msg += "Actuator name cannot contain spaces\n";
				
				if(srcName_.equals(dstName_)) error_msg += "Source equals to Destination\n";
				if (!Util.validString(tupleCpuLength.getText())) error_msg += "Missing Tuple CPU Length\n";
				if (!Util.validString(tupleNwLength.getText())) error_msg += "Missing Tuple NW Length\n";
				if (!Util.validString(tupleType.getText())) error_msg += "Missing Tuple Type\n";
				if (!Util.validString((String)direction.getSelectedItem())) error_msg += "Missing Direction\n";
				if (!Util.validString((String)edgeType.getSelectedItem())) error_msg += "Missing Edge Type\n";
				if (!Util.validString((String)periodic.getSelectedItem())) error_msg += "Missing Periodic\n";
				if(periodicity.isVisible() && !Util.validString((String)periodicity.getText()))error_msg += "Missing Periodicity\n";
				for(AppEdge appEdge : app.getEdges())
					if(appEdge.getTupleType().equals(tupleType.getText()))
						error_msg += "Repeated Tuple Type\n";
				
				if(tupleType.getText().contains(" "))
					error_msg += "Tuple Type cannot contain spaces\n";

				if((tupleCpuLength_ = Util.stringToDouble(tupleCpuLength.getText())) < 0) error_msg += "\nTuple CPU Length should be a positive number";
				if((tupleNwLength_ = Util.stringToDouble(tupleNwLength.getText())) < 0) error_msg += "\nTuple NW Length should be a positive number";
				if(periodicity.isVisible() && (periodicity_ = Util.stringToDouble(periodicity.getText())) < 0) error_msg += "\nPeriodicity should be a positive number";
				
				if(error_msg == ""){
					int iEdgeType = 1; // SENSOR
					if(((String)edgeType.getSelectedItem()).equals("ACTUATOR")) iEdgeType = 2;
					if(((String)edgeType.getSelectedItem()).equals("MODULE")) iEdgeType = 3;
					
					if(edge != null) {
						if(periodicity.isVisible())
							edge.setValues(srcName_, dstName_, periodicity_, tupleCpuLength_, tupleNwLength_,
									tupleType.getText(),((String)direction.getSelectedItem()).equals("UP") ?
											Tuple.UP : Tuple.DOWN, iEdgeType);
						else
							edge.setValues(srcName_, dstName_, tupleCpuLength_, tupleNwLength_,
									tupleType.getText(), ((String)direction.getSelectedItem()).equals("UP") ?
											Tuple.UP : Tuple.DOWN, iEdgeType);
					}
					else {
						if(periodicity.isVisible())
							app.addAppEdge(srcName_, dstName_, periodicity_, tupleCpuLength_, tupleNwLength_,
									tupleType.getText(),((String)direction.getSelectedItem()).equals("UP") ?
											Tuple.UP : Tuple.DOWN, iEdgeType);
						else
							app.addAppEdge(srcName_, dstName_, tupleCpuLength_, tupleNwLength_,
									tupleType.getText(), ((String)direction.getSelectedItem()).equals("UP") ?
											Tuple.UP : Tuple.DOWN, iEdgeType);
					}
					setVisible(false);
				}else
					Util.prompt(AddAppEdge.this, error_msg, "Error");
			}
		});

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okBtn);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelBtn);
		if(edge != null) {
			buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
			buttonPanel.add(delBtn);
		}
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		return buttonPanel;
	}
	
	/* Miscellaneous methods */
	private void changeDirection(String value) {
		direction.removeAllItems();
		ArrayList<String> directions = new ArrayList<String>();
		
		if(value != "")
			directions.add(value);
		else {
			directions.add("UP");
			directions.add("DOWN");
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
		ComboBoxModel<String> directionModel = new DefaultComboBoxModel(directions.toArray());
		direction.setModel(directionModel);
	}
	
	private void changeEdgeType(String value) {
		if(value.equals("SENSOR")) {
			sensorName.setVisible(true);
			lsensor.setVisible(true);
			actuatorName.setVisible(false);
			lactuator.setVisible(false);
			targetNode.setVisible(true);
			ltargetNode.setVisible(true);
			sourceNode.setVisible(false);
			lsourceNode.setVisible(false);
			changeDirection("UP");
			for(AppEdge appEdge : app.getEdges()) {
				if(appEdge.getEdgeType() == AppEdge.SENSOR) {
					sensorName.setText(appEdge.getSource());
					sensorName.setEditable(false);
					break;
				}
			}
		}else if(value.equals("ACTUATOR")) {
			sensorName.setVisible(false);
			lsensor.setVisible(false);
			actuatorName.setVisible(true);
			lactuator.setVisible(true);
			targetNode.setVisible(false);
			ltargetNode.setVisible(false);
			sourceNode.setVisible(true);
			lsourceNode.setVisible(true);
			changeDirection("DOWN");
			for(AppEdge appEdge : app.getEdges()) {
				if(appEdge.getEdgeType() == AppEdge.ACTUATOR) {
					actuatorName.setText(appEdge.getDestination());
					actuatorName.setEditable(false);
					break;
				}
			}
		}else {
			sensorName.setVisible(false);
			lsensor.setVisible(false);
			actuatorName.setVisible(false);
			lactuator.setVisible(false);
			targetNode.setVisible(true);
			ltargetNode.setVisible(true);
			sourceNode.setVisible(true);
			lsourceNode.setVisible(true);
			changeDirection("");
		}
	}
}
