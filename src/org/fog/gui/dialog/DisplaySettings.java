package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;

import org.fog.core.Config;
import org.fog.gui.GuiMsg;

/**
 * Class which allows to display and edit the simulation settings.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class DisplaySettings extends JDialog {
	private static final long serialVersionUID = 4794808969864918000L;
	private static final int WIDTH = 400;
	private static final int HEIGHT = 350;
	
	/**
	 * Creates a dialog to display and edit the simulation settings.
	 * 
	 * @param frame the current context 
	 */
	public DisplaySettings(final JFrame frame) {
		setLayout(new BorderLayout());

		add(createInputPanel(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(" Settings");
		setModal(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}

	/**
	 * Creates the editable settings.
	 * 
	 * @return the panel containing the input settings
	 */
	private JPanel createInputPanel() {
		JPanel inputPanelWrapper = new JPanel();
		inputPanelWrapper.setLayout(new BoxLayout(inputPanelWrapper, BoxLayout.PAGE_AXIS));
		
	    JCheckBox checkbox1 = new JCheckBox("Print algorithm best iterations");
	    checkbox1.setToolTipText(GuiMsg.TipSettPrintAlgIter);
	    checkbox1.setSelected(Config.PRINT_ALGORITHM_BEST_ITER);
	    
	    
	    JCheckBox checkbox2 = new JCheckBox("Print algorithm iteration contraints");
	    checkbox2.setToolTipText(GuiMsg.TipSettPrintAlgConst);
	    checkbox2.setSelected(Config.PRINT_ALGORITHM_CONSTRAINTS);
	    
	    JCheckBox checkbox3 = new JCheckBox("Print algorithm results");
	    checkbox3.setToolTipText(GuiMsg.TipSettPrintAlgRes);
	    checkbox3.setSelected(Config.PRINT_ALGORITHM_RESULTS);
	    
	    JCheckBox checkbox4 = new JCheckBox("Plot algorithm results");
	    checkbox4.setToolTipText(GuiMsg.TipSettPlotAlgRes);
	    checkbox4.setSelected(Config.PLOT_ALGORITHM_RESULTS);
	    
	    JCheckBox checkbox5 = new JCheckBox("Export results to excel");
	    checkbox5.setToolTipText(GuiMsg.TipSettExcel);
	    checkbox5.setSelected(Config.EXPORT_RESULTS_EXCEL);
	    
	    JCheckBox checkbox6 = new JCheckBox("Debug mode");
	    checkbox6.setToolTipText(GuiMsg.TipSettDebug);
	    checkbox6.setSelected(Config.DEBUG_MODE);
	    
	    JCheckBox checkbox7 = new JCheckBox("Print details");
	    checkbox7.setToolTipText(GuiMsg.TipSettDetails);
	    checkbox7.setSelected(Config.PRINT_DETAILS);
	    
	    JCheckBox checkbox8 = new JCheckBox("Print cost details");
	    checkbox8.setToolTipText(GuiMsg.TipSettCost);
	    checkbox8.setSelected(Config.PRINT_COST_DETAILS);
	    
	    JCheckBox checkbox9 = new JCheckBox("Dynamic simulation");
	    checkbox9.setToolTipText(GuiMsg.TipSettDynamic);
	    checkbox9.setSelected(Config.DYNAMIC_SIMULATION);
	    
	    JCheckBox checkbox10 = new JCheckBox("Allow migration");
	    checkbox10.setToolTipText(GuiMsg.TipSettMigration);
	    checkbox10.setSelected(Config.ALLOW_MIGRATION);
	    
	    checkbox1.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.PRINT_ALGORITHM_BEST_ITER = e.getStateChange() == 1 ? true : false;
            }
	    });
	    
	    checkbox2.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.PRINT_ALGORITHM_CONSTRAINTS = e.getStateChange() == 1 ? true : false;
            }
	    });
	    
	    checkbox3.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.PRINT_ALGORITHM_RESULTS = e.getStateChange() == 1 ? true : false;
            }
        });
	    
	    checkbox4.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.PLOT_ALGORITHM_RESULTS = e.getStateChange() == 1 ? true : false;
            }
        });
	    
	    checkbox5.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.EXPORT_RESULTS_EXCEL = e.getStateChange() == 1 ? true : false;
            }
        });
	    
	    checkbox6.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.DEBUG_MODE = e.getStateChange() == 1 ? true : false;
            }
        });
	    
	    checkbox7.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.PRINT_DETAILS = e.getStateChange() == 1 ? true : false;
            }
        });
	    
	    checkbox8.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.PRINT_COST_DETAILS = e.getStateChange() == 1 ? true : false;
            }
        });
	    
	    checkbox9.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.DYNAMIC_SIMULATION = e.getStateChange() == 1 ? true : false;
            }
        });
	    
	    checkbox10.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
            	Config.ALLOW_MIGRATION = e.getStateChange() == 1 ? true : false;
            }
        });
	    
        inputPanelWrapper.add(checkbox1);
        inputPanelWrapper.add(checkbox2);
        inputPanelWrapper.add(checkbox3);
        inputPanelWrapper.add(checkbox4);
        inputPanelWrapper.add(checkbox5);
        inputPanelWrapper.add(checkbox6);
        inputPanelWrapper.add(checkbox7);
        inputPanelWrapper.add(checkbox8);
        inputPanelWrapper.add(checkbox9);
        inputPanelWrapper.add(checkbox10);
        
		return inputPanelWrapper;
	}
	
	/**
	 * Creates the button panel (i.e., close) and defines its behavior upon being clicked.
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
		
		JButton okBtn = new JButton("Close");
		
		okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	setVisible(false);
            }
        });
		
		JPanel buttons = new JPanel(new GridBagLayout());
        buttons.add(okBtn, gbc);
        
        gbc.weighty = 1;
		buttonPanel.add(buttons, gbc);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		return buttonPanel;
	}
	
}
