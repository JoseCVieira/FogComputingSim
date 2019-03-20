package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

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

import org.fog.gui.core.ActuatorGui;
import org.fog.gui.core.Graph;
import org.fog.gui.core.Node;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Config;
import org.fog.utils.Util;

public class AddActuator extends JDialog {
	private static final long serialVersionUID = -511667786177319577L;
	
	private final Graph graph;
	private final ActuatorGui actuator;
	
	private JTextField actuatorName;
	
	/**
	 * Constructor.
	 * 
	 * @param frame the parent frame
	 */
	public AddActuator(final Graph graph, final JFrame frame, final ActuatorGui actuator) {
		this.graph = graph;
		this.actuator = actuator;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(actuator == null ? "  Add Actuator" : "  Edit Actuator");
		setModal(true);
		setPreferredSize(new Dimension(500, 150));
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
		
		if(actuator != null) {
			delBtn.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	graph.removeNode(actuator);
	                setVisible(false);
	            }
	        });
		}

		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String error_msg = "";
				
				if (Util.validString(actuatorName.getText())) {
					if(actuator == null || (actuator != null && !actuator.getName().equals(actuatorName.getText()))) {
						if(graph.isRepeatedName(actuatorName.getText()))
							error_msg += "Name already exists\n";
					}
				}else
					error_msg += "Missing name\n";
				
				if(actuatorName.getText().contains(" "))
					error_msg += "Name cannot contain spaces\n";

				if(error_msg == ""){
					if(actuator != null) {
						actuator.setName(actuatorName.getText());
					}else {
						ActuatorGui actuator = new ActuatorGui(actuatorName.getText());
						graph.addNode(actuator);
					}
					setVisible(false);
				}else
					Util.prompt(AddActuator.this, error_msg, "Error");
			}
		});

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okBtn);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelBtn);
		if(actuator != null) {
			buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
			buttonPanel.add(delBtn);
		}
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		return buttonPanel;
	}

	private JPanel createInputPanelArea() {
        JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		
		JLabel lName = new JLabel("Name: ");
		springPanel.add(lName);
		actuatorName = new JTextField();
		
		int aux = 1;
		for(Node node : graph.getDevicesList().keySet())
			if(node.getType().equals(Config.ACTUATOR_TYPE))
				aux++;
		
		actuatorName.setText(actuator == null ? Config.ACTUATOR_NAME + aux : actuator.getName());
		lName.setLabelFor(actuatorName);
		springPanel.add(actuatorName);

		//rows, cols, initX, initY, xPad, yPad
        SpringUtilities.makeCompactGrid(springPanel, 1, 2, 6, 6, 6, 6);
		return springPanel;
	}
	
}
