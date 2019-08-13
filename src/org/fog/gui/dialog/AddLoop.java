package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
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

import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;

/**
 * Class which allows to add an application module to a loop.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AddLoop extends JDialog {
	private static final long serialVersionUID = -511667786177319577L;
	private static final int WIDTH = 300;
	private static final int HEIGHT = 95;
	
	/** Application of the loop */
	private final Application app;
	
	/** Module name to be added to the loop */
	private JComboBox<String> name;
	
	/** List which contains all the module names of the loop */
	private final List<String> loop;
	
	/**
	 * Creates a dialog to add a new application module to an application loop.
	 * 
	 * @param frame the current context
	 * @param app the application of the loop
	 * @param loop the loop itself
	 */
	public AddLoop(final JFrame frame, final Application app, final List<String> loop) {
		this.app = app;
		this.loop = loop;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle("  Add Loop Module");
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
		JPanel inputPanelWrapper = new JPanel();
        
		inputPanelWrapper.setLayout(new BoxLayout(inputPanelWrapper, BoxLayout.PAGE_AXIS));
		
        ArrayList<String> names = new ArrayList<String>();
        for(AppModule appModule : app.getModules())
    		names.add(appModule.getName());
        
        for(AppEdge appEdge : app.getEdges()) {
			if(appEdge.getEdgeType() == AppEdge.SENSOR &&
					!loop.contains(appEdge.getSource()) && !names.contains(appEdge.getSource()))
				names.add(appEdge.getSource());
			else if(appEdge.getEdgeType() == AppEdge.ACTUATOR &&
					!loop.contains(appEdge.getDestination()) && !names.contains(appEdge.getDestination()))
				names.add(appEdge.getDestination());
		}
		ComboBoxModel<String> nameModel = new DefaultComboBoxModel(names.toArray());
		
        name = new JComboBox<>(nameModel);
        inputPanelWrapper.add(name);
		
		JPanel jPanel = new JPanel();
		jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		jPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		inputPanelWrapper.add(jPanel);
		
		return inputPanelWrapper;
	}
	
	/**
	 * Creates the button panel (i.e., Ok, Cancel) and defines its behavior upon being clicked.
	 * 
	 * @return the panel containing the buttons
	 */
	private JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		
		JButton okBtn = new JButton("Ok");
		JButton cancelBtn = new JButton("Cancel");
		
		cancelBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	setVisible(false);
            }
        });

		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(!(name.getSelectedItem() == null))
					loop.add(name.getSelectedItem().toString());
				setVisible(false);
			}
		});

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okBtn);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelBtn);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		return buttonPanel;
	}
}
