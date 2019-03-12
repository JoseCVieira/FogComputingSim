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
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.gui.core.ApplicationGui;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Util;
import org.fog.utils.Util.AppModulesCellRenderer;

public class AddTuple extends JDialog {
	private static final long serialVersionUID = -511667786177319577L;
	private static final int WIDTH = 600;
	private static final int HEIGHT = 300;
	
	private final AppModule module;
	private final ApplicationGui app;
	private final Pair<String, String> pair;
	
	private JTextField probability;

	private JComboBox<String> moduleName;
	private JComboBox<String> inputTuple;
	private JComboBox<String> outputTuple;
	
	public AddTuple(final JFrame frame, final ApplicationGui app, final AppModule module, final Pair<String, String> pair) {
		this.app = app;
		this.pair = pair;
		this.module = module;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(module == null ? "  Add Tuple" : "  Edit Tuple");
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
		ComboBoxModel<String> mNameModel = new DefaultComboBoxModel(app.getModules().toArray());
		mNameModel.setSelectedItem(module == null ? null : module);
		
		ArrayList<String> aux = new ArrayList<String>();
		for(AppEdge appEdge : app.getEdges())
			if(!aux.contains(appEdge.getTupleType()))
				aux.add(appEdge.getTupleType());
		
		@SuppressWarnings({ "rawtypes" })
		ComboBoxModel<String> inputModel = new DefaultComboBoxModel(aux.toArray());
		inputModel.setSelectedItem(module == null ? null : pair.getFirst());
		
		
		aux = new ArrayList<String>();
		if(module != null)
			for(AppEdge appEdge : app.getEdges())
				if(!aux.contains(appEdge.getTupleType()))
					if(!((String)inputModel.getSelectedItem()).equals(appEdge.getTupleType()))
						aux.add(appEdge.getTupleType());
		
		@SuppressWarnings({ "rawtypes" })
		ComboBoxModel<String> outputModel = new DefaultComboBoxModel(aux.toArray());
		outputModel.setSelectedItem(module == null ? null : pair.getSecond());
		
		moduleName = new JComboBox<>(mNameModel);
		inputTuple = new JComboBox<>(inputModel);
		outputTuple = new JComboBox<>(outputModel);
		
		AppModulesCellRenderer renderer = new Util.AppModulesCellRenderer();
		moduleName.setRenderer(renderer);
		
		JLabel lmNameModel = new JLabel("Module Name: ");
		springPanel.add(lmNameModel);
		lmNameModel.setLabelFor(moduleName);
		springPanel.add(moduleName);
		
		JLabel linputModel = new JLabel("Input Tuple: ");
		springPanel.add(linputModel);
		linputModel.setLabelFor(inputTuple);
		springPanel.add(inputTuple);
		
		JLabel loutputTuple = new JLabel("Output Tuple: ");
		springPanel.add(loutputTuple);
		loutputTuple.setLabelFor(outputTuple);
		springPanel.add(outputTuple);
		
		JLabel lprob = new JLabel("Probability: ");
		springPanel.add(lprob);
		probability = new JTextField();		
		probability.setText(module == null ? "" :
			Double.toString(((FractionalSelectivity)module.getSelectivityMap().get(pair)).getSelectivity()));
		lprob.setLabelFor(probability);
		springPanel.add(probability);
		
		inputTuple.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				outputTuple.removeAllItems();

				String selectedNode = (String) inputTuple.getSelectedItem();
				if (selectedNode == null) return;
				
				List<String> tuplesToDisplay = new ArrayList<String>();
				
				for(AppEdge appEdge : app.getEdges()) {					
					if(!selectedNode.equals(appEdge.getTupleType())) {
						if(!tuplesToDisplay.contains(appEdge.getTupleType())) {
							boolean canBeAdded = true;
							
							if(module == null) {
								for(AppModule appModule : app.getModules()) {
									for(Pair<String, String> p : appModule.getSelectivityMap().keySet()) {
										if(selectedNode.equals(p.getFirst()) &&
												(appEdge.getTupleType()).equals(p.getSecond()))
										canBeAdded = false;
										break;
									}
								}
							}
							
							if(canBeAdded)
								tuplesToDisplay.add(appEdge.getTupleType());
						}
					}
				}				

				@SuppressWarnings("rawtypes")
				ComboBoxModel<String> outputModel = new DefaultComboBoxModel(tuplesToDisplay.toArray());
				outputTuple.setModel(outputModel);
			}
		});
		
		SpringUtilities.makeCompactGrid(springPanel, 4, 2, 6, 6, 6, 6);
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
		
		if(module != null) {
			delBtn.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	module.getSelectivityMap().remove(pair);
	                setVisible(false);
	            }
	        });
		}

		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String error_msg = "", moduleName_ = "", inputTuple_ = "", outputTuple_ = "";
				double probability_ = -1;
				
				
				AppModule source = (AppModule)moduleName.getSelectedItem();
				if(source != null) moduleName_ = source.getName();
				else error_msg += "Missing Module Name\n";
				
				String input = (String)inputTuple.getSelectedItem();
				if(input != null) inputTuple_ = input;
				else error_msg += "Missing Input Tuple\n";
				
				String output = (String)outputTuple.getSelectedItem();
				if(output != null) outputTuple_ = output;
				else error_msg += "Missing Output Tuple\n";
				
				if (!Util.validString(probability.getText())) error_msg += "Missing Probability\n";
				if((probability_ = Util.stringToProbability(probability.getText())) < 0) error_msg +=
						"\nProbability should be a number between [0.0; 1.0]";
				
				if(error_msg == ""){
					app.addTupleMapping(moduleName_, inputTuple_, outputTuple_, new FractionalSelectivity(probability_));
					setVisible(false);
				}else
					Util.prompt(AddTuple.this, error_msg, "Error");
			}
		});

		buttonPanel.add(Box.createHorizontalGlue());
		buttonPanel.add(okBtn);
		buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
		buttonPanel.add(cancelBtn);
		if(module != null) {
			buttonPanel.add(Box.createRigidArea(new Dimension(10, 0)));
			buttonPanel.add(delBtn);
		}
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		return buttonPanel;
	}
}
