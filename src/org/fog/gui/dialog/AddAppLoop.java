package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.core.Constants;
import org.fog.gui.GuiMsg;
import org.fog.gui.GuiUtils;
import org.fog.utils.Util;

/**
 * Class which allows to add an application loop.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AddAppLoop extends JDialog {
	private static final long serialVersionUID = 4794808969864918000L;
	private static final int WIDTH = 800;
	private static final int HEIGHT = 800;
	private static final String[] columnNames = {"No", "Name", "Remove"};
	
	/** Object which holds the content of the table */
	private DefaultTableModel dtm;
	
	/** Application of the loop */
	private final Application app;
	
	/** The context */
	private final JFrame frame;
	
	/** List which contains all the module names of the loop */
	private List<String> loop;
	
	/** Loop deadline (time acceptable by the user for the loop execution in the worst case scenario) */
	private JTextField deadline;
	
	/**
	 * Creats a new application loop.
	 * 
	 * @param frame the current context
	 * @param app the application of the loop
	 */
	public AddAppLoop(final JFrame frame, final Application app) {
		this.app = app;
		this.frame = frame;
		setLayout(new BorderLayout());
		loop = new ArrayList<String>();

		add(createInputPanel(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle("  Add Application Loop");
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
	private JPanel createInputPanel() {
        dtm = new DefaultTableModel(getLoop(), columnNames);
        JTable jtable = new JTable(dtm) {
			private static final long serialVersionUID = 1L;

			public boolean isCellEditable(int row, int column){
				return false;
        	}
    	};
        
		JPanel inputPanelWrapper = new JPanel();
		inputPanelWrapper.setLayout(new BoxLayout(inputPanelWrapper, BoxLayout.PAGE_AXIS));

		JButton okBtn = new JButton("Add");
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new AddLoop(frame, app, loop);
				dtm.setDataVector(getLoop(), columnNames);
				configureTable(jtable);
			}
		});
		
		JPanel jPanel = new JPanel();
		jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		deadline = GuiUtils.createInput(jPanel, deadline, "Loop deadline [s]: ", Double.toString(Constants.INF), GuiMsg.TipLoopDeadline);	
		
		jPanel = new JPanel();
		jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		jPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		jPanel.add(okBtn);
		
		inputPanelWrapper.add(jPanel);
        
    	jtable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    JTable table = (JTable)e.getSource();
			    int rowAtPoint = table.rowAtPoint(e.getPoint());
			    int columnAtPoint = table.columnAtPoint(e.getPoint());
			    
			    if(columnAtPoint == 2) {
			    	if(GuiUtils.confirm(AddAppLoop.this, "Do you really want to remove " +
			    			table.getValueAt(rowAtPoint, 1) + " ?") == JOptionPane.YES_OPTION) {
			    		loop.remove(table.getValueAt(rowAtPoint, 1).toString());
			    		dtm.setDataVector(getLoop(), columnNames);
				    	configureTable(jtable);
			    	}
			    }
			}
        });
    	
    	configureTable(jtable);
        JScrollPane jScrollPane = new JScrollPane(jtable);
        jScrollPane.setPreferredSize(new Dimension(WIDTH - 40, HEIGHT - 100));
        inputPanelWrapper.add(jScrollPane);
        
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
		
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
		
		JButton okBtn = new JButton("Close");
		
		okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {            	
            	String error_msg = "";
				double value;
				
				if (!Util.validString(deadline.getText()))	error_msg += GuiMsg.errMissing("Deadline value");
				if((value = Util.stringToDouble(deadline.getText())) < 0) error_msg += GuiMsg.errFormat("Deadline");
				
				if(error_msg == "") {					
					if(!loop.isEmpty())
	            		app.getLoops().add(new AppLoop(loop, value));
					
					setVisible(false);
				}else
					GuiUtils.prompt(AddAppLoop.this, error_msg, "Error");
            	
            }
        });
		
		JPanel buttons = new JPanel(new GridBagLayout());
        buttons.add(okBtn, gbc);
        
        gbc.weighty = 1;
		buttonPanel.add(buttons, gbc);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

		return buttonPanel;
	}
	
	/**
	 * Gets the content of the application loop table.
	 * 
	 * @return the content of the application loop table
	 */
	private String[][] getLoop() {
		if(loop.isEmpty())
			return null;
		
		String[][] lists = new String[loop.size()][];
		int index = 0;
		
		for(String name : loop) {
			String[] list = new String[3];
			
			list[0] = Integer.toString(index);
			list[1] = name;
			list[2] = "✘";
			lists[index++] = list;
		}
		return lists;
	}
	
	/**
	 * Configures the sizes of the columns within a given table.
	 * 
	 * @param jtable the table with the columns sizes configured
	 */
	private void configureTable(JTable jtable) {
		jtable.getColumn("Remove").setCellRenderer(new GuiUtils.ButtonRenderer());
		jtable.getColumnModel().getColumn(0).setPreferredWidth(75);
		jtable.getColumnModel().getColumn(1).setPreferredWidth(WIDTH - 175);
		jtable.getColumnModel().getColumn(2).setPreferredWidth(100);
		jtable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
	}
	
}
