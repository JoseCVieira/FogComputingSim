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
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableModel;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.gui.core.ApplicationGui;
import org.fog.gui.core.Graph;
import org.fog.utils.Util;

/** A dialog to add a new application */
public class AddApplication extends JDialog {
	private static final long serialVersionUID = 4794808969864918000L;
	private static final int WIDTH = 1500;
	private static final int HEIGHT = 1000;
	
	private static final String[] COLUMN_MODULES = {"Name", "Ram", "Mem", "Client Module", "Global Module", "Edit"};
	private static final String[] COLUMN_EDGES = {"Source", "Destination", "Tuple CPU",
			"Tuple NW", "Tuple Type", "Edge Type", "Periodicity", "Edit"};
	private static final String[] COLUMN_TUPLES = {"Module Name", "Input Tuple Type",
			"Output Tuple Type", "Selectivity", "Edit"};
	private static final String[] COLUMN_LOOPS = {"Loop", "Remove"};
	
	private ApplicationGui app;
	private final Graph graph;
	private final JFrame frame;
	
	private JTable jtableModules;
	private JTable jtableEdges;
	private JTable jtableTuples;
	private JTable jtableLoops;
	
	private DefaultTableModel dtmModules;
	private DefaultTableModel dtmEdges;
	private DefaultTableModel dtmTuples;
	private DefaultTableModel dtmLoops;
	
	private JButton addModuleBtn;
	private JButton addEdgeBtn;
	private JButton addTupleBtn;
	private JButton addLoopBtn;
	
	private JTextField tfName;
	private JTabbedPane tp;
	
	public AddApplication(final Graph graph, final JFrame frame, ApplicationGui app) {
		this.graph = graph;
		this.app = app;
		this.frame = frame;
		setLayout(new BorderLayout());
		
		add(createTabs(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);
		
		if(app == null) {
		    tp.setEnabled(false);
			addModuleBtn.setEnabled(false);
	    }
		
		setTitle(app == null ? " Add application" : " Edit application");
		setModal(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}
	
	private JPanel createTabs() {
		Box.createRigidArea(new Dimension(10, 0));

		JPanel jpanel = new JPanel();
		jpanel.setLayout(new BoxLayout(jpanel, BoxLayout.PAGE_AXIS));
		jpanel.add(changeName(), BorderLayout.CENTER);
	    
	    tp = new JTabbedPane();
	    tp.add("Modules", createModules());
	    tp.add("Edges", createEdges());
	    tp.add("Tuples", createTuples());
	    tp.add("Loops", createLoops());
	    
	    tp.addChangeListener(new ChangeListener() {
	        public void stateChanged(ChangeEvent e) {
	        	if(tp.getSelectedIndex() == 0)
	        		updateTable(dtmModules, jtableModules, getAppModules(), COLUMN_MODULES);
	        	else if(tp.getSelectedIndex() == 1)
	        		updateTable(dtmEdges, jtableEdges, getAppEdges(), COLUMN_EDGES);
	        	else if(tp.getSelectedIndex() == 2)
	        		updateTable(dtmTuples, jtableTuples, getTuples(), COLUMN_TUPLES);
	        	else {
	        		dtmLoops.setDataVector(getLoops(), COLUMN_LOOPS);
					configureTable(jtableLoops);
	        	}
	        }
	    });
	    jpanel.add(tp);
	    
	    return jpanel;
	}
	
	private JPanel changeName() {
		JPanel jPanel = new JPanel();
		tfName = new JTextField();
		
		jPanel.add(new JLabel("Name: "));
		tfName = new JTextField();
		tfName.setMaximumSize(tfName.getPreferredSize());
		tfName.setMinimumSize(new Dimension(300, tfName.getPreferredSize().height));
		tfName.setPreferredSize(new Dimension(300, tfName.getPreferredSize().height));
		jPanel.add(tfName);
		
		if(app != null)
			tfName.setText(app.getAppId());

		JButton okBtn = new JButton("Save name");
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String name = tfName.getText();
				boolean canBeChanged = true;
				
				for(ApplicationGui applicationGui : graph.getAppList()) {
					if(app == null || (app != null && !app.equals(applicationGui))) {
						if(name.equals(applicationGui.getAppId())) {
							canBeChanged = false;
							break;
						}
					}
				}
				
				if(tfName.getText().contains(" "))
					Util.prompt(AddApplication.this, "Name cannot contain spaces", "Error");
				else {
					if(canBeChanged) {
						if(app == null) {
							tp.setEnabled(true);
							addModuleBtn.setEnabled(true);
							app = new ApplicationGui(name, new ArrayList<List<String>>());
							graph.getAppList().add(app);
						}else
							app.setAppId(name);
					}else
						Util.prompt(AddApplication.this, name + " already exists", "Error");
				}
			}
		});

		jPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		jPanel.add(okBtn);
		
		return jPanel;
	}
	
	private JPanel createModules() {		
		dtmModules = new DefaultTableModel(getAppModules(), COLUMN_MODULES);
		jtableModules = createTable(jtableModules, dtmModules);
		jtableModules.getColumn("Edit").setCellRenderer(new Util.ButtonRenderer());
		
		JPanel jPanel = new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
		jPanel.add(Box.createRigidArea(new Dimension(100, 0)));
		
		addModuleBtn = new JButton("Add Module");
		addModuleBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new AddAppModule(frame, app, null);
				updateTable(dtmModules, jtableModules, getAppModules(), COLUMN_MODULES);
			}
		});
		
		jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		jPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		jPanel.add(addModuleBtn);
        
		jtableModules.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		jtableModules.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    JTable table = (JTable)e.getSource();
			    int rowAtPoint = table.rowAtPoint(e.getPoint());
			    int columnAtPoint = table.columnAtPoint(e.getPoint());
			    
			    if(columnAtPoint == 5) {			    	
			    	new AddAppModule(frame, app, app.getModules().get(rowAtPoint));
			    	updateTable(dtmModules, jtableModules, getAppModules(), COLUMN_MODULES);
			    }
			}
        });
        
        jPanel.add(createJScrollPane(jtableModules));
		return jPanel;
	}
	
	private JPanel createEdges() {
		dtmEdges = new DefaultTableModel(getAppEdges(), COLUMN_EDGES);
		jtableEdges = createTable(jtableEdges, dtmEdges);
		jtableEdges.getColumn("Edit").setCellRenderer(new Util.ButtonRenderer());
		
		JPanel jPanel = new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
		jPanel.add(Box.createRigidArea(new Dimension(100, 0)));
		
		addEdgeBtn = new JButton("Add Edge");
		addEdgeBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new AddAppEdge(frame, app, null);
				updateTable(dtmEdges, jtableEdges, getAppEdges(), COLUMN_EDGES);
			}
		});
		
		jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		jPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		jPanel.add(addEdgeBtn);
        
		jtableEdges.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		jtableEdges.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    JTable table = (JTable)e.getSource();
			    int rowAtPoint = table.rowAtPoint(e.getPoint());
			    int columnAtPoint = table.columnAtPoint(e.getPoint());
			    
			    if(columnAtPoint == 7) {			    	
			    	new AddAppEdge(frame, app, app.getEdges().get(rowAtPoint));
			    	updateTable(dtmEdges, jtableEdges, getAppEdges(), COLUMN_EDGES);
			    }
			}
        });
    	
        jPanel.add(createJScrollPane(jtableEdges));
		return jPanel;
	}
	
	private JPanel createTuples() {
		dtmTuples = new DefaultTableModel(getTuples(), COLUMN_TUPLES);
		jtableTuples = createTable(jtableTuples, dtmTuples);
		jtableTuples.getColumn("Edit").setCellRenderer(new Util.ButtonRenderer());

    	JPanel jPanel = new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
		jPanel.add(Box.createRigidArea(new Dimension(100, 0)));
		
		addTupleBtn = new JButton("Add Tuple");
		addTupleBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new AddTuple(frame, app, null, null);
				updateTable(dtmTuples, jtableTuples, getTuples(), COLUMN_TUPLES);
			}
		});
		
		jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		jPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		jPanel.add(addTupleBtn);
    	
		jtableTuples.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		jtableTuples.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    JTable table = (JTable)e.getSource();
			    int rowAtPoint = table.rowAtPoint(e.getPoint());
			    int columnAtPoint = table.columnAtPoint(e.getPoint());
			    
			    if(columnAtPoint == 4) {
			    	AppModule appModule = null;
			    	for(AppModule aModule : app.getModules()) {
			    		if(aModule.getName().equals(table.getValueAt(rowAtPoint, 0))) {
			    			appModule = aModule;
			    			break;
			    		}
			    	}
			    	
			    	Pair<String, String> pair = null;
			    	for(Pair<String, String> p : appModule.getSelectivityMap().keySet()) {
			    		if(p.getFirst().equals(table.getValueAt(rowAtPoint, 1)) && 
			    				p.getSecond().equals(table.getValueAt(rowAtPoint, 2))) {
			    			pair = p;
			    			break;
			    		}
			    	}
			    	
			    	new AddTuple(frame, app, appModule, pair);
			    	updateTable(dtmTuples, jtableTuples, getTuples(), COLUMN_TUPLES);
			    }
			}
        });
		
	    jPanel.add(createJScrollPane(jtableTuples));
		return jPanel;
	}
	
	private JPanel createLoops() {
		dtmLoops = new DefaultTableModel(getLoops(), COLUMN_LOOPS);
		jtableLoops = createTable(jtableLoops, dtmLoops);

    	JPanel jPanel = new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
		jPanel.add(Box.createRigidArea(new Dimension(100, 0)));
		
		addLoopBtn = new JButton("Add Loop");
		addLoopBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new AddAppLoop(frame, app);
				dtmLoops.setDataVector(getLoops(), COLUMN_LOOPS);
				configureTable(jtableLoops);
			}
		});
		
		jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		jPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		jPanel.add(addLoopBtn);
    	
		jtableLoops.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		jtableLoops.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    JTable table = (JTable)e.getSource();
			    int rowAtPoint = table.rowAtPoint(e.getPoint());
			    int columnAtPoint = table.columnAtPoint(e.getPoint());
			    
			    if(columnAtPoint == 1) {			    	
			    	if(Util.confirm(AddApplication.this, "Do you really want to remove " +
			    			table.getValueAt(rowAtPoint, 0)+ " ?") == JOptionPane.YES_OPTION) {
			    		
			    		String[] parts = table.getValueAt(rowAtPoint, 0).toString().split(" -> ");
			    		
			    		List<String> lparts = new ArrayList<String>();
			    		for(String p : parts)
			    			lparts.add(p);
			    		
			    		app.getLoops().remove(lparts);
			    		dtmLoops.setDataVector(getLoops(), COLUMN_LOOPS);
						configureTable(jtableLoops);
			    	}
			    }
			}
        });
		
		configureTable(jtableLoops);
	    jPanel.add(createJScrollPane(jtableLoops));
		return jPanel;
	}
	
	private String[][] getAppModules() {
		if(app == null)
			return null;
		
		String[][] lists = new String[app.getModules().size()][];
		int index = 0;
		
		for(AppModule appModule : app.getModules()) {
			String[] list = new String[6];
				
			list[0] = appModule.getName();
			list[1] = Integer.toString(appModule.getRam());
			list[2] = Long.toString(appModule.getSize());
			list[3] = appModule.isClientModule() ? "Yes" : "No";
			list[4] = appModule.isGlobalModule() ? "Yes" : "No";
			list[5] = "✎";
			lists[index++] = list;
		}
		return lists;
	}
	
	private String[][] getAppEdges() {
		if(app == null)
			return null;
		
		String[][] lists = new String[app.getEdges().size()][];
		int index = 0;
		
		for(AppEdge appEdge : app.getEdges()) {
			String[] list = new String[8];
			
			String eType = "SENSOR";
			if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
				eType = "ACTUATOR";
			else if(appEdge.getEdgeType() == AppEdge.MODULE)
				eType = "MODULE";

			list[0] = appEdge.getSource();
			list[1] = appEdge.getDestination();
			list[2] = Double.toString(appEdge.getTupleCpuLength());
			list[3] = Double.toString(appEdge.getTupleNwLength());
			list[4] = appEdge.getTupleType();
			list[5] = eType;
			list[6] = appEdge.isPeriodic() ? Double.toString(appEdge.getPeriodicity()) : "-";
			list[7] = "✎";
			lists[index++] = list;
		}
		return lists;
	}
	
	private String[][] getTuples() {
		if(app == null)
			return null;
		
		int total = 0;
		for(AppModule appModule : app.getModules())
			total += appModule.getSelectivityMap().keySet().size();
		
		String[][] lists = new String[total][];
		int index = 0;
		
		for(AppModule appModule : app.getModules()) {
			for(Pair<String, String> selectivityMap : appModule.getSelectivityMap().keySet()) {
				String[] list = new String[5];
				
				list[0] = appModule.getName();
				list[1] = selectivityMap.getFirst();
				list[2] = selectivityMap.getSecond();
				list[3] = Double.toString(((FractionalSelectivity)appModule.getSelectivityMap().get(selectivityMap)).getSelectivity());
				list[4] = "✎";
				lists[index++] = list;
			}
		}
		return lists;
	}
	
	private String[][] getLoops() {
		if(app == null)
			return null;
		
		String[][] lists = new String[app.getLoops().size()][];
		int index = 0;
		
		for(List<String> loop : app.getLoops()) {
			String[] list = new String[2];
			list[0] = "";
			
			int i = 0;
			for(String name : loop) {
				list[0] += name;
				if(i++ < loop.size() - 1)
					list[0] += " -> ";
			}
			
			list[1] = "✘";
			lists[index++] = list;
		}
		return lists;
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
	
	private void updateTable(DefaultTableModel model, JTable jTable, String[][] data, String[] columns) {
		model.setDataVector(data, columns);
		jTable.getColumn("Edit").setCellRenderer(new Util.ButtonRenderer());
	}
	
	private JTable createTable(JTable jTable, DefaultTableModel model) {
		jTable = new JTable(model) {
			private static final long serialVersionUID = 1L;

			public boolean isCellEditable(int row, int column){
				return false;
        	}
    	};
    	
    	return jTable;
	}
	
	private JScrollPane createJScrollPane(JTable jTable) {
		JScrollPane jScrollPane = new JScrollPane(jTable);
        jScrollPane.setMaximumSize(new Dimension(WIDTH - 40, HEIGHT - 250));
        jScrollPane.setMinimumSize(new Dimension(WIDTH - 40, HEIGHT - 250));
        jScrollPane.setPreferredSize(new Dimension(WIDTH - 40, HEIGHT - 250));
        return jScrollPane;
	}
	
	private void configureTable(JTable jtable) {
		jtable.getColumn("Remove").setCellRenderer(new Util.ButtonRenderer());
		jtable.getColumnModel().getColumn(0).setPreferredWidth(WIDTH - 100);
		jtable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
	}
}