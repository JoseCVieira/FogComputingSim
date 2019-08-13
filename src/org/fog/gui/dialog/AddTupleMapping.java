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
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.gui.GuiMsg;
import org.fog.gui.GuiUtils;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Util;

/**
 * Class which allows to add or edit an application tuple mapping.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AddTupleMapping extends JDialog {
	private static final long serialVersionUID = -511667786177319577L;
	private static final int WIDTH = 500;
	private static final int HEIGHT = 260;
	
	/** Object which contains the module of the tuple mapping to be edited or null if its a new one */
	private final AppModule module;
	
	/** Application of the module */
	private final Application app;
	
	/** Pair containing both the input and output tuple names/labels */
	private final Pair<String, String> pair;
	
	/** Probability of generating the output tuple from upon the processing of the input tuple */
	private JTextField probability;
	
	/** Module name of the tuple mapping */
	private JComboBox<String> moduleName;
	
	/** Input tuple name/label of the tuple mapping */
	private JComboBox<String> inputTuple;
	
	/** Output tuple name/label of the tuple mapping */
	private JComboBox<String> outputTuple;
	
	/**
	 * Creates or edits an application tuple mapping.
	 * 
	 * @param frame the current context
	 * @param app the application of the tuple mapping
	 * @param module the application module of the tuple mapping
	 * @param pair the pair containing both the input and output tuple names/labels
	 */
	public AddTupleMapping(final JFrame frame, final Application app, final AppModule module, final Pair<String, String> pair) {
		this.app = app;
		this.pair = pair;
		this.module = module;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(module == null ? "  Add Tuple Mapping" : "  Edit Tuple Mapping");
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
        
        AppModule moduleOp = module == null ? null : module;
		String inputOp = module == null ? null : pair.getFirst();
		String outputOp = module == null ? null : pair.getSecond();
		String probOp = module == null ? "" : Double.toString(((FractionalSelectivity)module.getSelectivityMap().get(pair)).getSelectivity());
        
		ArrayList<String> inTmp = new ArrayList<String>();
		for(AppEdge appEdge : app.getEdges()) {
			if(!inTmp.contains(appEdge.getTupleType()))
				inTmp.add(appEdge.getTupleType());
		}
		
		ArrayList<String> outTmp = new ArrayList<String>();
		if(module != null) {
			for(AppEdge appEdge : app.getEdges()) {
				if(!outTmp.contains(appEdge.getTupleType()) && !(inputOp).equals(appEdge.getTupleType()))
					outTmp.add(appEdge.getTupleType());
			}
		}
		
		ComboBoxModel<String> nameModel = new DefaultComboBoxModel(app.getModules().toArray());
		ComboBoxModel<String> inputModel = new DefaultComboBoxModel(inTmp.toArray());
		ComboBoxModel<String> outputModel = new DefaultComboBoxModel(outTmp.toArray());
		
		moduleName = GuiUtils.createDropDown(springPanel, moduleName, "Module name: ", nameModel, moduleOp, GuiMsg.TipTupleMod);
		inputTuple = GuiUtils.createDropDown(springPanel, inputTuple, "Input Tuple: ", inputModel, inputOp, GuiMsg.TipTupleIn);
		outputTuple = GuiUtils.createDropDown(springPanel, outputTuple, "Output Tuple: ", outputModel, outputOp, GuiMsg.TipTupleOut);
		probability = GuiUtils.createInput(springPanel, probability, "Probability: ", probOp, GuiMsg.TipTupleProb);
		moduleName.setRenderer(new GuiUtils.AppModulesCellRenderer());
		
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
										if(selectedNode.equals(p.getFirst()) && (appEdge.getTupleType()).equals(p.getSecond()))
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
				
				ComboBoxModel<String> outputModel = new DefaultComboBoxModel(tuplesToDisplay.toArray());
				outputTuple.setModel(outputModel);
			}
		});
		
		SpringUtilities.makeCompactGrid(springPanel, 4, 2, 6, 6, 6, 6);
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
				else error_msg +=  GuiMsg.errMissing("Module name");
				
				String input = (String)inputTuple.getSelectedItem();
				if(input != null) inputTuple_ = input;
				else error_msg += GuiMsg.errMissing("Input tuple");
				
				String output = (String)outputTuple.getSelectedItem();
				if(output != null) outputTuple_ = output;
				else error_msg += GuiMsg.errMissing("Output tuple");
				
				if (!Util.validString(probability.getText())) error_msg += "Missing Probability\n";
				if((probability_ = Util.stringToProbability(probability.getText())) < 0) error_msg +=
						"\nProbability should be a number between [0.0; 1.0]";
				
				// Add/edit(override) the new tuple mapping
				if(error_msg == ""){
					app.addTupleMapping(moduleName_, inputTuple_, outputTuple_, new FractionalSelectivity(probability_));
					setVisible(false);
				}else
					GuiUtils.prompt(AddTupleMapping.this, error_msg, "Error");
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
