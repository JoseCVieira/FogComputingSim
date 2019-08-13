package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;
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
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.gui.GuiConstants;
import org.fog.gui.GuiMsg;
import org.fog.gui.GuiUtils;
import org.fog.gui.GuiUtils.AppModulesCellRenderer;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Util;

/**
 * Class which allows to add or edit an application edge.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AddAppEdge extends JDialog {
	private static final long serialVersionUID = -511667786177319577L;
	private static final int WIDTH = 600;
	private static final int HEIGHT = 460;
	private static final String[] EdgeTypes = {"SENSOR", "ACTUATOR", "MODULE"};
	private static final String[] PeriodicOp = {"YES", "NO"};
	
	private final AppEdge edge;
	private final Application app;
	
	private JTextField tupleCpuLength;
	private JTextField tupleNwLength;
	private JTextField tupleType;
	private JTextField periodicity;
	private JTextField actuatorName;
	private JTextField sensorName;
	
	private JComboBox<String> edgeType;
	private JComboBox<String> sourceNode;
	private JComboBox<String> targetNode;
	private JComboBox<String> periodic;
	
	/**
	 * Creates or edits an application edge.
	 * 
	 * @param frame the current context
	 * @param app the application
	 * @param edge the application edge be edited; can be null when a new application edge is to be added
	 */
	public AddAppEdge(final JFrame frame, final Application app, final AppEdge edge) {
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
	
	/**
	 * Creates all the inputs that users need to fill up.
	 * 
	 * @return the panel containing the inputs
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JPanel createInputPanelArea() {
		JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        AppModulesCellRenderer renderer = new AppModulesCellRenderer();
        
        String edgeOp = "", periodicOp = "", sensorOp = "", actuatorOp = "", tupleTypeOp = "";
        String periodicityOp = Double.toString(GuiConstants.EDGE_PERIODICITY);
        String tupleCpuLengthOp = Double.toString(GuiConstants.EDGE_CPU_LENGTH);
        String tupleNwLengthOp = Double.toString(GuiConstants.EDGE_NW_LENGTH);
        AppModule fromOp = null, toOp = null;
        if(edge != null) {
			if(edge.getEdgeType() == AppEdge.ACTUATOR) {
				edgeOp = "ACTUATOR";
				sensorOp = edge.getDestination();
			}else if(edge.getEdgeType() == AppEdge.SENSOR) {
				edgeOp = "SENSOR";
				actuatorOp = edge.getSource();
			}else {
				edgeOp = "MODULE";
			}
			
			periodicOp = edge.isPeriodic() ? "YES" : "NO";
			
			for(AppModule appModule : app.getModules()) {
				if(edge.getEdgeType() != AppEdge.SENSOR && appModule.getName().equals(edge.getSource())) {
					fromOp = appModule;
				}
				
				if(edge.getEdgeType() != AppEdge.ACTUATOR && appModule.getName().equals(edge.getDestination())) {
					toOp = appModule;
				}
			}
			
			periodicityOp = Double.toString(edge.getPeriodicity());
			tupleCpuLengthOp = Double.toString(edge.getTupleCpuLength());
			tupleNwLengthOp = Double.toString(edge.getTupleNwLength());
			tupleTypeOp = edge.getTupleType();
		}
        
		ComboBoxModel<String> sourceModel = new DefaultComboBoxModel(app.getModules().toArray());
		ComboBoxModel<String> targetModel = new DefaultComboBoxModel(app.getModules().toArray());
		ComboBoxModel<String> edgeTypeModel = new DefaultComboBoxModel(EdgeTypes);
		ComboBoxModel<String> periodicModel = new DefaultComboBoxModel(PeriodicOp);
		
		edgeType = GuiUtils.createDropDown(springPanel, edgeType, "Edge type: ", edgeTypeModel, edgeOp, GuiMsg.TipEdgeType);
		sourceNode = GuiUtils.createDropDown(springPanel, sourceNode, "From: ", sourceModel, fromOp, GuiMsg.TipEdgeFrom);
		targetNode = GuiUtils.createDropDown(springPanel, targetNode, "To: ", targetModel, toOp, GuiMsg.TipEdgeTo);
		sensorName = GuiUtils.createInput(springPanel, sensorName, "Sensor: ", sensorOp, GuiMsg.TipEdgeSensor);
		actuatorName = GuiUtils.createInput(springPanel, actuatorName, "Actuator: ", actuatorOp, GuiMsg.TipEdgeActuator);
		periodic = GuiUtils.createDropDown(springPanel, periodic, "Periodic: ", periodicModel, periodicOp, GuiMsg.TipEdgePeri);
		periodicity = GuiUtils.createInput(springPanel, periodicity, "Periodicity [s]: ", periodicityOp, GuiMsg.TipEdgePeriod);
		tupleCpuLength = GuiUtils.createInput(springPanel, tupleCpuLength, "Tuple CPU length [MI]: ", tupleCpuLengthOp, GuiMsg.TipEdgeCPU);
		tupleNwLength = GuiUtils.createInput(springPanel, tupleNwLength, "Tuple NW length [Bytes]: ", tupleNwLengthOp, GuiMsg.TipEdgeNW);
		tupleType = GuiUtils.createInput(springPanel, tupleType, "Tuple type: ", tupleTypeOp, GuiMsg.TipEdgeTupleType);
		
		sourceNode.setRenderer(renderer);
		targetNode.setRenderer(renderer);
		
		if(edge != null) {
			if(!edge.isPeriodic()) periodicity.setVisible(false);
			
			if(edge.getEdgeType() == AppEdge.SENSOR) {
				changeEdgeType("SENSOR");
			}else if(edge.getEdgeType() == AppEdge.ACTUATOR) {
				changeEdgeType("ACTUATOR");
			}else {
				changeEdgeType("MODULE");
			}
		}
		
		sourceNode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				AppModule selectedNode = (AppModule) sourceNode.getSelectedItem();
				if (selectedNode == null) return;
				
				targetNode.removeAllItems();
				List<AppModule> nodesToDisplay = new ArrayList<AppModule>();
				nodesToDisplay.addAll(app.getModules());
				nodesToDisplay.remove(selectedNode);
				
				targetNode.setModel(new DefaultComboBoxModel(nodesToDisplay.toArray()));
				targetNode.setRenderer(renderer);
				targetNode.setSelectedItem(null);
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
				changeEdgeType((String) edgeType.getSelectedItem());
			}
		});
		
		SpringUtilities.makeCompactGrid(springPanel, 10, 2, 6, 6, 6, 6);
		return springPanel;
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
					else error_msg += GuiMsg.errMissing("Source");
				}else if (!Util.validString(sensorName.getText()))
					error_msg += GuiMsg.errMissing("Sensor name");
				else if(!sensorName.getText().contains(" "))
					srcName_ = sensorName.getText();
				else
					error_msg += "Sensor name cannot contain spaces\n";
				
				if(!actuatorName.isVisible()) {
					AppModule dst = (AppModule)targetNode.getSelectedItem();
					if(dst != null) dstName_ = dst.getName();
					else error_msg += GuiMsg.errMissing("Destination");
				}else if (!Util.validString(actuatorName.getText()))
					error_msg += GuiMsg.errMissing("Actuator name");
				else if(!actuatorName.getText().contains(" "))
					dstName_ = actuatorName.getText();
				else
					error_msg += "Actuator name cannot contain spaces\n";
				
				if(srcName_.equals(dstName_)) error_msg += "Source equals to destination\n";
				
				if(periodicity.isVisible() &&
						!Util.validString((String)periodicity.getText()))	error_msg += GuiMsg.errMissing("Periodicity");
				if (!Util.validString(tupleCpuLength.getText())) 			error_msg += GuiMsg.errMissing("Tuple CPU length");
				if (!Util.validString(tupleNwLength.getText())) 			error_msg += GuiMsg.errMissing("Tuple NW length");
				if (!Util.validString(tupleType.getText())) 				error_msg += GuiMsg.errMissing("Tuple type");
				if (!Util.validString((String)edgeType.getSelectedItem()))	error_msg += GuiMsg.errMissing("Edge type");
				if (!Util.validString((String)periodic.getSelectedItem()))	error_msg += GuiMsg.errMissing("Periodic");
				
				for(AppEdge appEdge : app.getEdges()) {
					if(appEdge.getTupleType().equals(tupleType.getText())) {
						if(edge == null || edge != null && !appEdge.getTupleType().equals(edge.getTupleType())) {
							error_msg += "Repeated tuple types\n";
							break;
						}
					}
				}
				
				if(tupleType.getText().contains(" "))
					error_msg += "Tuple type cannot contain spaces\n";
				
				if(periodicity.isVisible() &&
						(periodicity_ = Util.stringToDouble(periodicity.getText())) < 0)	error_msg += GuiMsg.errFormat("Periodicity");
				if((tupleCpuLength_ = Util.stringToDouble(tupleCpuLength.getText())) < 0) 	error_msg += GuiMsg.errFormat("Tuple CPU length");
				if((tupleNwLength_ = Util.stringToDouble(tupleNwLength.getText())) < 0) 	error_msg += GuiMsg.errFormat("Tuple NW length");
				
				
				
				if(error_msg == ""){
					int iEdgeType = 1; // SENSOR
					if(((String)edgeType.getSelectedItem()).equals("ACTUATOR")) iEdgeType = 2;
					if(((String)edgeType.getSelectedItem()).equals("MODULE")) iEdgeType = 3;
					
					if(edge != null) {
						// Edit the periodic application edge
						if(periodicity.isVisible())
							edge.setValues(srcName_, dstName_, periodicity_, tupleCpuLength_, tupleNwLength_, tupleType.getText(), iEdgeType);
						
						// Edit the non periodic application edge
						else
							edge.setValues(srcName_, dstName_, tupleCpuLength_, tupleNwLength_, tupleType.getText(), iEdgeType);
					}
					else {
						// Add a new periodic application edge
						if(periodicity.isVisible())
							app.addAppEdge(srcName_, dstName_, periodicity_, tupleCpuLength_, tupleNwLength_, tupleType.getText(), iEdgeType);
						
						// Add a new non periodic application edge
						else
							app.addAppEdge(srcName_, dstName_, tupleCpuLength_, tupleNwLength_, tupleType.getText(), iEdgeType);
					}
					
					setVisible(false);
				}else
					GuiUtils.prompt(AddAppEdge.this, error_msg, "Error");
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
	
	/**
	 * Modifies the inputs visibility and its content based on the selected edge type value.
	 * 
	 * @param value the selected edge type value
	 */
	private void changeEdgeType(String value) {
		if(value.equals("SENSOR")) {
			sensorName.setVisible(true);
			actuatorName.setVisible(false);
			targetNode.setVisible(true);
			sourceNode.setVisible(false);
			sensorName.setEditable(true);
			for(AppEdge appEdge : app.getEdges()) {
				if(appEdge.getEdgeType() == AppEdge.SENSOR) {
					sensorName.setText(appEdge.getSource());
					sensorName.setEditable(false);
					break;
				}
			}
		}else if(value.equals("ACTUATOR")) {
			sensorName.setVisible(false);
			actuatorName.setVisible(true);
			targetNode.setVisible(false);
			sourceNode.setVisible(true);
			actuatorName.setEditable(true);
			for(AppEdge appEdge : app.getEdges()) {
				if(appEdge.getEdgeType() == AppEdge.ACTUATOR) {
					actuatorName.setText(appEdge.getDestination());
					actuatorName.setEditable(false);
					break;
				}
			}
		}else {
			sensorName.setVisible(false);
			actuatorName.setVisible(false);
			targetNode.setVisible(true);
			sourceNode.setVisible(true);
		}
	}
}
