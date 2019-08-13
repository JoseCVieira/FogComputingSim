package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

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
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Util;

/**
 * Class which allows to add or edit an application module.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AddAppModule extends JDialog {
	private static final long serialVersionUID = -511667786177319577L;
	private static final String[] booleanOp = {"YES", "NO"};
	
	/** Object which contains the module to be edited or null if its a new one */
	private final AppModule module;
	
	/** Application of the module */
	private final Application app;
	
	/** Name of the module */
	private JTextField moduleName;
	
	/** Ram needed to support the module */
	private JTextField moduleRam;
	
	/**
	 * Denotes if the application module is a client module.
	 * Client modules only run inside the client device (e.g., Graphical User Interface)
	 */
	private JComboBox<String> clientModule;
	
	/**
	 * Denotes if the application module is a global module.
	 * Global modules are used by all clients running the same application (e.g., Multiplayer Games)
	 */
	private JComboBox<String> globalModule;
	
	/**
	 * Creates or edits an application module.
	 * 
	 * @param frame the current context
	 * @param app the application
	 * @param module the application module be edited; can be null when a new application module is to be added
	 */
	public AddAppModule(final JFrame frame, final Application app, final AppModule module) {
		this.app = app;
		this.module = module;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(module == null ? "  Add Application Module" : "  Edit Application Module");
		setModal(true);
		setPreferredSize(new Dimension(500, 250));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
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
			ArrayList<AppEdge> edgesToRemove = new ArrayList<AppEdge>();
			
			delBtn.addActionListener(new ActionListener() {
	            public void actionPerformed(ActionEvent event) {
	            	for(AppEdge appEdge : app.getEdges()) {	            		
	            		if(appEdge.getSource().equals(module.getName()) ||
	            				appEdge.getDestination().equals(module.getName())) {
	            			for(AppModule appModule : app.getModules())
	    	            		for(Pair<String, String> pair : appModule.getSelectivityMap().keySet())
	    	            			if(pair.getFirst().equals(appModule.getName()) ||
	    	            					pair.getSecond().equals(appModule.getName()))
	    	            				appModule.getSelectivityMap().remove(pair);
	            			edgesToRemove.add(appEdge);
	            		}
	            	}
	            	
	            	for(AppEdge appEdge : edgesToRemove)
	            		app.getEdges().remove(appEdge);
	            	app.getModules().remove(module);
	                setVisible(false);
	            }
	        });
		}

		okBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String error_msg = "", name_ = "";
				int ram_ = -1;
				boolean clientModule_ = false, globalModule_ = false;
				
				if (Util.validString(moduleName.getText())) {
					if(module == null || (module != null && !module.getName().equals(moduleName.getText()))) {		
						for(AppModule appModule : app.getModules())
							if(appModule.getName().equalsIgnoreCase(moduleName.getText()))
								error_msg += "Name already exists\n";
					}
				}else
					error_msg += GuiMsg.errMissing("Name");
				
				if(moduleName.getText().contains(" "))
					error_msg += "Name cannot contain spaces\n";
				
				if (!Util.validString(moduleRam.getText())) error_msg += "Missing Ram\n";
				if (!Util.validString((String) clientModule.getSelectedItem())) error_msg += GuiMsg.errMissing("Client module");
				if (!Util.validString((String) globalModule.getSelectedItem())) error_msg += GuiMsg.errMissing("Global module");
				
				name_ = moduleName.getText();
				if((ram_ = Util.stringToInt(moduleRam.getText())) < 0) error_msg += GuiMsg.errFormat("Ram");
				
				if(error_msg == "") {
					clientModule_ = ((String) clientModule.getSelectedItem()).equals("YES") ? true : false;
					globalModule_ = ((String) globalModule.getSelectedItem()).equals("YES") ? true : false;
					
					if(clientModule_ && globalModule_)
						error_msg += "Modules cannot be of global and client types at the same time";
				}
				
				if(error_msg == ""){
					if(module != null)
						module.setValues(name_, ram_, clientModule_, globalModule_);
					else
						app.addAppModule(name_, ram_, clientModule_, globalModule_);
					setVisible(false);
				}else
					GuiUtils.prompt(AddAppModule.this, error_msg, "Error");
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
	
	/**
	 * Creates all the inputs that users need to fill up.
	 * 
	 * @return the panel containing the inputs
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JPanel createInputPanelArea() {
        JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
        String nameOp = module == null ? "" : module.getName();
        String ramOp = module == null ? Integer.toString(GuiConstants.MODULE_RAM) : Integer.toString(module.getRam());
        String clientOp = "", globalOp = "";
        
        if(module != null) {
        	clientOp = module.isClientModule() ? "Yes" : "No";
        	globalOp = module.isGlobalModule() ? "Yes" : "No";
        }
        
        ComboBoxModel<String> clientModuleModel = new DefaultComboBoxModel(booleanOp);
        ComboBoxModel<String> globalModuleModel = new DefaultComboBoxModel(booleanOp);
        
        moduleName = GuiUtils.createInput(springPanel, moduleName, "Name: ", nameOp, GuiMsg.TipModName);
        moduleRam = GuiUtils.createInput(springPanel, moduleRam, "Ram [Bytes]: ", ramOp, GuiMsg.TipModRam);
        clientModule = GuiUtils.createDropDown(springPanel, clientModule, "Client module: ", clientModuleModel, clientOp, GuiMsg.TipModClient);
        globalModule = GuiUtils.createDropDown(springPanel, globalModule, "Global module: ", globalModuleModel, globalOp, GuiMsg.TipModGlobal);        
        
		//rows, cols, initX, initY, xPad, yPad
        SpringUtilities.makeCompactGrid(springPanel, 4, 2, 6, 6, 6, 6);
		return springPanel;
	}
}
