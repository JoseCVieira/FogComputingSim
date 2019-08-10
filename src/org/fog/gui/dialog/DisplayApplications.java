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

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.fog.core.Constants;
import org.fog.gui.GuiUtils;
import org.fog.gui.core.ApplicationGui;
import org.fog.gui.core.FogDeviceGui;
import org.fog.gui.core.Graph;
import org.fog.gui.core.Node;

/** A dialog to view applications */
public class DisplayApplications extends JDialog {
	private static final long serialVersionUID = 4794808969864918000L;
	private static final int WIDTH = 800;
	private static final int HEIGHT = 800;
	
	private final Graph graph;
	private final JFrame frame;
	private DefaultTableModel dtm;
	
	public DisplayApplications(final Graph graph, final JFrame frame) {
		this.graph = graph;
		this.frame = frame;
		setLayout(new BorderLayout());

		add(createInputPanel(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(" Applications");
		setModal(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}

	private JPanel createInputPanel() {
		String[] columnNames = {"Name", "Edit", "Remove"};
        
        dtm = new DefaultTableModel(getApplications(), columnNames);
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
				new AddApplication(graph, frame, null);
				dtm.setDataVector(getApplications(), columnNames);
				configureTable(jtable);
			}
		});
		
		JPanel jPanel = new JPanel();
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
			    
			    if(columnAtPoint == 1) {
			    	new AddApplication(graph, frame, graph.getAppList().get(rowAtPoint));
			    	dtm.setDataVector(getApplications(), columnNames);
			    	configureTable(jtable);
			    }else if(columnAtPoint == 2) {
			    	if(GuiUtils.confirm(DisplayApplications.this, "Do you really want to remove " +
			    			table.getValueAt(rowAtPoint, 0)+ " ?") == JOptionPane.YES_OPTION) {
			    		
			    		ApplicationGui appToRemove = null;
			    		for(ApplicationGui applicationGui : graph.getAppList())
			    			if(applicationGui.getAppId().equals(table.getValueAt(rowAtPoint, 0)))
			    				appToRemove = applicationGui;
			    		
			    		for(Node node : graph.getDevicesList().keySet())
			    			if(node.getType().equals(Constants.FOG_TYPE))
			    				if(((FogDeviceGui)node).getApplication().equals(appToRemove.getAppId()))
			    					((FogDeviceGui)node).setApplication("");
			    		
			    		graph.removeApp(appToRemove);
			    		dtm.setDataVector(getApplications(), columnNames);
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
	
	/* Miscellaneous methods */
	private String[][] getApplications() {
		if(graph.getAppList() == null)
			return null;
		
		String[][] lists = new String[graph.getAppList().size()][];
		int index = 0;
		
		for(ApplicationGui app : graph.getAppList()) {
			String[] list = new String[3];
				
			list[0] = app.getAppId();
			list[1] = "✎";
			list[2] = "✘";
			lists[index++] = list;
		}
		return lists;
	}
	
	private void configureTable(JTable jtable) {
		jtable.getColumn("Remove").setCellRenderer(new GuiUtils.ButtonRenderer());
		jtable.getColumn("Edit").setCellRenderer(new GuiUtils.ButtonRenderer());
		jtable.getColumnModel().getColumn(0).setPreferredWidth(WIDTH - 200);
		jtable.getColumnModel().getColumn(1).setPreferredWidth(200);
		jtable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
	}
	
}
