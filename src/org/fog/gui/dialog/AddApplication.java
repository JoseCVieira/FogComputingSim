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
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.gui.GuiUtils;
import org.fog.gui.core.Graph;

/**
 * Class which allows to add or edit an application.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AddApplication extends JDialog {
	private static final long serialVersionUID = 4794808969864918000L;
	private static final int WIDTH = 1500;
	private static final int HEIGHT = 1000;
	private static final String[] COLUMN_MODULES = {"Name", "Ram [Byte]", "Mig. deadline [s]", "Client Module", "Global Module", "Edit"};
	private static final String[] COLUMN_EDGES = {"Source", "Destination", "Tuple CPU [MI]", "Tuple NW [Byte]",
			"Tuple Type", "Edge Type", "Periodicity [s]", "Edit"};
	private static final String[] COLUMN_TUPLES = {"Module Name", "Input Tuple Type", "Output Tuple Type",
			"Selectivity", "Edit"};
	private static final String[] COLUMN_LOOPS = {"Loop", "Deadline [s]", "Edit", "Remove"};
	
	/** Object which contains the application to be edited or null if its a new one */
	private Application app;
	
	/** Object which holds the current topology */
	private final Graph graph;
	
	/** The context */
	private final JFrame frame;
	
	/** Table which holds and displays the application modules */
	private JTable jtableModules;
	
	/** Table which holds and displays the application edges */
	private JTable jtableEdges;
	
	/** Table which holds and displays the application tuple mapping */
	private JTable jtableTuples;
	
	/** Table which holds and displays the application loops */
	private JTable jtableLoops;
	
	/** Object which holds the content of the application modules table */
	private DefaultTableModel dtmModules;
	
	/** Object which holds the content of the application edges table */
	private DefaultTableModel dtmEdges;
	
	/** Object which holds the content of the application tuple mapping table */
	private DefaultTableModel dtmTuples;
	
	/** Object which holds the content of the application loops table */
	private DefaultTableModel dtmLoops;
	
	/** Button to allow to add a new application module */
	private JButton addModuleBtn;
	
	/** Button to allow to add a new application edge */
	private JButton addEdgeBtn;
	
	/** Button to allow to add a new application tuple mapping */
	private JButton addTupleBtn;
	
	/** Button to allow to add a new application loop */
	private JButton addLoopBtn;
	
	/** Name of the application (AppId) */
	private JTextField tfName;
	
	/** Tabbed pane to switch between tables */
	private JTabbedPane tp;
	
	/**
	 * Creates or edits an application.
	 * 
	 * @param graph the current topology
	 * @param frame the current context
	 * @param app the application be edited; can be null when a new application is to be added
	 */
	public AddApplication(final Graph graph, final JFrame frame, Application app) {
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
	
	/**
	 * Creates the tabs needed.
	 * 
	 * @return the panel containing the tabs
	 */
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
	
	/**
	 * Creates the input text field to edit the application name.
	 * 
	 * @return the panel containing the input text field to edit the application name
	 */
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
				
				for(Application applicationGui : graph.getAppList()) {
					if(app == null || (app != null && !app.getAppId().equals(applicationGui.getAppId()))) {
						if(name.equals(applicationGui.getAppId())) {
							canBeChanged = false;
							break;
						}
					}
				}
				
				if(tfName.getText().contains(" "))
					GuiUtils.prompt(AddApplication.this, "Name cannot contain spaces", "Error");
				else {
					if(canBeChanged) {
						if(app == null) {
							tp.setEnabled(true);
							addModuleBtn.setEnabled(true);
							app = new Application(name);
							graph.getAppList().add(app);
						}else
							app.setAppId(name);
					}else
						GuiUtils.prompt(AddApplication.this, name + " already exists", "Error");
				}
			}
		});

		jPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		jPanel.add(okBtn);
		
		return jPanel;
	}
	
	/**
	 * Creates the module tab which contains both the table displaying the modules, its characteristics and the edit button,
	 * as well as the button to add a new application module.
	 * 
	 * @return the panel containing the application modules tab
	 */
	private JPanel createModules() {
		dtmModules = new DefaultTableModel(getAppModules(), COLUMN_MODULES);
		jtableModules = createTable(jtableModules, dtmModules);
		jtableModules.getColumn("Edit").setCellRenderer(new GuiUtils.ButtonRenderer());
		
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
	
	/**
	 * Creates the edge tab which contains both the table displaying the edges, its characteristics and the edit button,
	 * as well as the button to add a new application edge.
	 * 
	 * @return the panel containing the application edges tab
	 */
	private JPanel createEdges() {
		dtmEdges = new DefaultTableModel(getAppEdges(), COLUMN_EDGES);
		jtableEdges = createTable(jtableEdges, dtmEdges);
		jtableEdges.getColumn("Edit").setCellRenderer(new GuiUtils.ButtonRenderer());
		
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
	
	/**
	 * Creates the tuple mapping tab which contains both the table displaying the tuple mapping, its characteristics and the edit button,
	 * as well as the button to add a new application tuple mapping.
	 * 
	 * @return the panel containing the application tuple mapping tab
	 */
	private JPanel createTuples() {
		dtmTuples = new DefaultTableModel(getTuples(), COLUMN_TUPLES);
		jtableTuples = createTable(jtableTuples, dtmTuples);
		jtableTuples.getColumn("Edit").setCellRenderer(new GuiUtils.ButtonRenderer());

    	JPanel jPanel = new JPanel();
		jPanel.setLayout(new BoxLayout(jPanel, BoxLayout.PAGE_AXIS));
		jPanel.add(Box.createRigidArea(new Dimension(100, 0)));
		
		addTupleBtn = new JButton("Add Tuple");
		addTupleBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				new AddTupleMapping(frame, app, null, null);
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
			    	
			    	new AddTupleMapping(frame, app, appModule, pair);
			    	updateTable(dtmTuples, jtableTuples, getTuples(), COLUMN_TUPLES);
			    }
			}
        });
		
	    jPanel.add(createJScrollPane(jtableTuples));
		return jPanel;
	}
	
	/**
	 * Creates the loop tab which contains both the table displaying the loops, its characteristics and the edit button,
	 * as well as the button to add a new application loop.
	 * 
	 * @return the panel containing the application loops tab
	 */
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
				new AddAppLoop(frame, app, -1);
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
			    
			    if(columnAtPoint == 2) {
			    	new AddAppLoop(frame, app, rowAtPoint);
					dtmLoops.setDataVector(getLoops(), COLUMN_LOOPS);
					configureTable(jtableLoops);
			    } else if(columnAtPoint == 3) {
			    	if(GuiUtils.confirm(AddApplication.this, "Do you really want to remove " +
			    			table.getValueAt(rowAtPoint, 0) + " ?") == JOptionPane.YES_OPTION) {
			    		
			    		String[] parts = table.getValueAt(rowAtPoint, 0).toString().split(" -> ");
			    		double deadline = Double.parseDouble(table.getValueAt(rowAtPoint, 1).toString());
			    		
			    		List<String> lparts = new ArrayList<String>();
			    		for(String p : parts)
			    			lparts.add(p);
			    		
			    		AppLoop toRemove = null;
			    		for(AppLoop appLoop : app.getLoops()) {
			    			if(appLoop.getModules().equals(lparts) && deadline == appLoop.getDeadline()) {
			    				toRemove = appLoop;
			    				break;
			    			}
			    		}
			    		
			    		app.getLoops().remove(toRemove);
			    		
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
	
	/**
	 * Gets the content of the application modules table.
	 * 
	 * @return the content of the application modules table
	 */
	private String[][] getAppModules() {
		if(app == null)
			return null;
		
		String[][] lists = new String[app.getModules().size()][];
		int index = 0;
		
		for(AppModule appModule : app.getModules()) {
			String[] list = new String[6];
				
			list[0] = appModule.getName();
			list[1] = Integer.toString(appModule.getRam());
			list[2] = Double.toString(appModule.getMigrationDeadline());
			list[3] = appModule.isClientModule() ? "Yes" : "No";
			list[4] = appModule.isGlobalModule() ? "Yes" : "No";
			list[5] = "✎";
			lists[index++] = list;
		}
		return lists;
	}
	
	
	/**
	 * Gets the content of the application edges table.
	 * 
	 * @return the content of the application edges table
	 */
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
	
	/**
	 * Gets the content of the application tuple mapping table.
	 * 
	 * @return the content of the application tuple mapping table
	 */
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
	
	/**
	 * Gets the content of the application loops table.
	 * 
	 * @return the content of the application loops table
	 */
	private String[][] getLoops() {
		if(app == null)
			return null;
		
		String[][] lists = new String[app.getLoops().size()][];
		int index = 0;
		
		for(AppLoop loop : app.getLoops()) {
			String[] list = new String[4];
			list[0] = "";
			
			int i = 0;
			for(String name : loop.getModules()) {
				list[0] += name;
				if(i++ < loop.getModules().size() - 1)
					list[0] += " -> ";
			}
			
			list[1] = Double.toString(loop.getDeadline());
			list[2] = "✎";
			list[3] = "✘";
			lists[index++] = list;
		}
		return lists;
	}
	
	/**
	 * Creates the button panel (i.e., Ok, Cancel, Delete) and defines its behavior upon being clicked.
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
	
	/**
	 * Render the edit button within a given table.
	 * 
	 * @param model the object which holds the content of the table
	 * @param jTable the table itself
	 * @param data the data to fill up the table
	 * @param columns the name of the columns
	 */
	private void updateTable(DefaultTableModel model, JTable jTable, String[][] data, String[] columns) {
		model.setDataVector(data, columns);
		jTable.getColumn("Edit").setCellRenderer(new GuiUtils.ButtonRenderer());
	}
	
	/**
	 * Creates a table with non editable cells.
	 * 
	 * @param jTable the table itself
	 * @param model the object which holds the content of the table
	 * @return the table with non editable cells
	 */
	private JTable createTable(JTable jTable, DefaultTableModel model) {
		jTable = new JTable(model) {
			private static final long serialVersionUID = 1L;

			public boolean isCellEditable(int row, int column){
				return false;
        	}
    	};
    	
    	return jTable;
	}
	
	/**
	 * Creates a scrollable pane inside a given table.
	 * 
	 * @param jTable the table itself
	 * @return the table with a scrollable pane within it
	 */
	private JScrollPane createJScrollPane(JTable jTable) {
		JScrollPane jScrollPane = new JScrollPane(jTable);
        jScrollPane.setMaximumSize(new Dimension(WIDTH - 40, HEIGHT - 250));
        jScrollPane.setMinimumSize(new Dimension(WIDTH - 40, HEIGHT - 250));
        jScrollPane.setPreferredSize(new Dimension(WIDTH - 40, HEIGHT - 250));
        return jScrollPane;
	}
	
	/**
	 * Configures the sizes of the columns within a given table.
	 * 
	 * @param jtable the table with the columns sizes configured
	 */
	private void configureTable(JTable jtable) {
		jtable.getColumn("Edit").setCellRenderer(new GuiUtils.ButtonRenderer());
		jtable.getColumn("Remove").setCellRenderer(new GuiUtils.ButtonRenderer());
		
		jtable.getColumnModel().getColumn(0).setPreferredWidth(WIDTH - 350);
		jtable.getColumnModel().getColumn(1).setPreferredWidth(150);
		jtable.getColumnModel().getColumn(2).setPreferredWidth(100);
		jtable.getColumnModel().getColumn(3).setPreferredWidth(100);
		
		jtable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
	}
	
}
