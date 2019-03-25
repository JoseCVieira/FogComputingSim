package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.gui.core.ApplicationGui;
import org.fog.gui.core.SpringUtilities;
import org.fog.utils.Config;
import org.fog.utils.Util;

public class AddAppModule extends JDialog {
	private static final long serialVersionUID = -511667786177319577L;
	
	private final AppModule module;
	private final ApplicationGui app;
	
	private JTextField moduleName;
	private JTextField moduleMips;
	private JTextField moduleRam;
	private JTextField moduleSize;
	private JTextField moduleBw;
	
	public AddAppModule(final JFrame frame, final ApplicationGui app, final AppModule module) {
		this.app = app;
		this.module = module;
		setLayout(new BorderLayout());

		add(createInputPanelArea(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(module == null ? "  Add Application Module" : "  Edit Application Module");
		setModal(true);
		setPreferredSize(new Dimension(500, 300));
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
				double mips_ = -1;
				int ram_ = -1;
				long size_ = -1, bw_ = -1;
				
				if (Util.validString(moduleName.getText())) {
					if(module == null || (module != null && !module.getName().equals(moduleName.getText()))) {		
						for(AppModule appModule : app.getModules())
							if(appModule.getName().equalsIgnoreCase(moduleName.getText()))
								error_msg += "Name already exists\n";
					}
				}else
					error_msg += "Missing name\n";
				
				if(moduleName.getText().contains(" "))
					error_msg += "Name cannot contain spaces\n";
				
				if (!Util.validString(moduleMips.getText())) error_msg += "Missing Mips\n";
				if (!Util.validString(moduleRam.getText())) error_msg += "Missing Ram\n";
				if (!Util.validString(moduleSize.getText())) error_msg += "Missing Mem\n";
				if (!Util.validString(moduleBw.getText())) error_msg += "Missing Bw\n";

				name_ = moduleName.getText();
				if((mips_ = Util.stringToDouble(moduleMips.getText())) < 0) error_msg += "\nMips should be a positive number";
				if((ram_ = Util.stringToInt(moduleRam.getText())) < 0) error_msg += "\nRam should be a positive number";
				if((size_ = Util.stringToLong(moduleSize.getText())) < 0) error_msg += "\nMem should be a positive number";
				if((bw_ = Util.stringToLong(moduleBw.getText())) < 0) error_msg += "\nBw should be a positive number";
				
				if(error_msg == ""){
					if(module != null)
						module.setValues(name_, mips_, ram_, size_, bw_);
					else 
						app.addAppModule(name_, mips_, ram_, size_, bw_);
					setVisible(false);
				}else
					Util.prompt(AddAppModule.this, error_msg, "Error");
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

	private JPanel createInputPanelArea() {
        JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		
        moduleName = Util.createInput(springPanel, moduleName, "Name: ", module == null ? "" : module.getName());
        moduleMips = Util.createInput(springPanel, moduleMips, "Mips: ", module == null ? Double.toString(Config.MODULE_MIPS) : Double.toString(module.getMips()));
        moduleRam = Util.createInput(springPanel, moduleRam, "Ram: ", module == null ? Integer.toString(Config.MODULE_RAM) : Integer.toString(module.getRam()));
        moduleSize = Util.createInput(springPanel, moduleSize, "Mem: ", module == null ? Long.toString(Config.MODULE_SIZE) : Long.toString(module.getSize()));
        moduleBw = Util.createInput(springPanel, moduleBw, "Bw: ", module == null ? Long.toString(Config.MODULE_BW) : Long.toString(module.getBw()));

		//rows, cols, initX, initY, xPad, yPad
        SpringUtilities.makeCompactGrid(springPanel, 5, 2, 6, 6, 6, 6);
		return springPanel;
	}
}
