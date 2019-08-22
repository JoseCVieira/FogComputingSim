package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.table.DefaultTableModel;

import org.fog.core.Config;
import org.fog.gui.GuiUtils;
import org.fog.gui.GuiUtils.NodeCellRenderer;
import org.fog.gui.core.Node;
import org.fog.gui.core.Graph;
import org.fog.gui.core.Link;
import org.fog.utils.Util;

/**
 * Class which allows to add and display connections between fog devices. Although connections are unidirectional, for
 * simplicity, by adding one link, there are created both directions with the same characteristics.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AddLink extends JDialog {
	private static final long serialVersionUID = 4794808969864918000L;
	private static final int WIDTH = 1500;
	private static final int HEIGHT = 1000;
	private static final String[] COLUMNS = {"From/To", "From/To", "Latency [s]", "Bandwidth [B/s]", "Remove"};
	
	/** Object which holds the current topology */
	private final Graph graph;
	
	/** One of the fog nodes to be added the connection */
	private JComboBox<String> sourceNode;
	
	/** Another fog node to be added the connection */
	private JComboBox<String> targetNode;
	
	/** Latency which characterizes the connection */
	private JTextField latency;
	
	/** Bandwidth which characterizes the connection */
	private JTextField bandwidth;
	
	/** Object which holds the content of the link table */
	private DefaultTableModel dtm;
	
	/** Table which holds and displays the links */
	private JTable jtable;
	
	/**
	 * Creates a dialog to add, delete and display the connections between fog devices.
	 * 
	 * @param graph the object which holds the current topology
	 * @param frame the current context
	 */
	public AddLink(final Graph graph, final JFrame frame) {
		this.graph = graph;
		setLayout(new BorderLayout());

		add(createInputPanel(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(" Add Link");
		setModal(true);
		setPreferredSize(new Dimension(WIDTH, HEIGHT));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame); // must be called between pack and setVisible to work properly
		setVisible(true);
	}
	
	/**
	 * Creates the link table as well as the add and delete buttons.
	 * 
	 * @return the panel containing the inputs
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private JPanel createInputPanel() {
		Box.createRigidArea(new Dimension(10, 0));

		JPanel inputPanelWrapper = new JPanel();
		inputPanelWrapper.setLayout(new BoxLayout(inputPanelWrapper, BoxLayout.PAGE_AXIS));
		
		NodeCellRenderer renderer = new NodeCellRenderer();
		
		ComboBoxModel<String> sourceNodeModel = new DefaultComboBoxModel(graph.getDevicesList().keySet().toArray());
		ComboBoxModel<String> destNodeModel = new DefaultComboBoxModel();
		
		sourceNodeModel.setSelectedItem(null);
		
		sourceNode = new JComboBox<>(sourceNodeModel);
		targetNode = new JComboBox<>(destNodeModel);
		
		sourceNode.setRenderer(renderer);
		targetNode.setRenderer(renderer);
		
		configureInputDropDown(sourceNode);
		configureInputDropDown(targetNode);
		
		sourceNode.addItemListener(new ItemListener() {
			@Override
			public void itemStateChanged(ItemEvent e) {
				// only display nodes which do not have already an edge
				targetNode.removeAllItems();
				Node selectedNode = (Node) sourceNode.getSelectedItem();

				if (selectedNode == null) return;
				
				List<Node> nodesToDisplay = new ArrayList<Node>();
				Set<Node> allNodes = graph.getDevicesList().keySet();

				// Get edges for selected node and throw out all target nodes where already exists an edge
				List<Link> edgesForSelectedNode = graph.getDevicesList().get(selectedNode);
				Set<Node> nodesInEdges = new HashSet<Node>();
				for (Link edge : edgesForSelectedNode)
					nodesInEdges.add(edge.getNode());
				
				for(Node node : graph.getDevicesList().keySet()) {
					for(Link edge : graph.getDevicesList().get(node)) {
						if(edge.getNode().getName().equals(selectedNode.getName()) && !nodesInEdges.contains(node))
							nodesInEdges.add(node);
					}
				}
				
				for (Node node : allNodes) {
					if (!node.getName().equals(selectedNode.getName()) && !nodesInEdges.contains(node))
						nodesToDisplay.add(node);
				}
				
				ComboBoxModel<String> targetNodeModel = new DefaultComboBoxModel(nodesToDisplay.toArray());
				targetNode.setModel(targetNodeModel);
			}
		});
		
		JPanel jPanel = new JPanel(new GridBagLayout());
		JLabel jLabel = new JLabel(" Add new connection");
		jLabel.setHorizontalAlignment(JLabel.LEFT);
		jPanel.add(jLabel);
		jPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
		inputPanelWrapper.add(jPanel);
		
		jPanel = new JPanel(new GridBagLayout());
		jPanel.add(sourceNode);
		jPanel.add(new JLabel(" <---> "));
		jPanel.add(targetNode);
		jPanel.add(new JLabel("  Latency [s]: "));
		latency = new JTextField();
		configureInput(latency);
		jPanel.add(latency);
		
		jPanel.add(new JLabel("  Bandwidth [B/s]: "));
		bandwidth = new JTextField();
		bandwidth.setText(Double.toString(Config.FIXED_COMMUNICATION_BW));
		configureInput(bandwidth);
		jPanel.add(bandwidth);

		JButton okBtn = new JButton("Add");
		okBtn.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {

				double latency_ = 0;
				double bandwidth_ = 0;
				String error_msg = "";
				
				if((latency_ = Util.stringToDouble(latency.getText())) < 0) error_msg += "\nLatency should be a positive number";
				if((bandwidth_ = Util.stringToDouble(bandwidth.getText())) < 0) error_msg += "\nBandwidth should be a positive number";

				if(error_msg == "") {
					if (sourceNode.getSelectedItem() == null || targetNode.getSelectedItem() == null)
						GuiUtils.prompt(AddLink.this, "Please select node", "Error");
					else {
						Node source = (Node)sourceNode.getSelectedItem();
						Node target = (Node)targetNode.getSelectedItem();

						Link link = new Link(target, latency_, bandwidth_);
						graph.addEdge(source, link);
						dtm.setDataVector(getConnections(), COLUMNS);
						
						ComboBoxModel<String> sourceNodeModel = new DefaultComboBoxModel(graph.getDevicesList().keySet().toArray());
						sourceNode.setModel(sourceNodeModel);
						sourceNodeModel.setSelectedItem(null);
						configureTable(jtable);
					}
				}else
					GuiUtils.prompt(AddLink.this, error_msg, "Error");
			}
		});

		jPanel.add(Box.createRigidArea(new Dimension(20, 0)));
		jPanel.add(okBtn);
		inputPanelWrapper.add(jPanel);
		
		jPanel = new JPanel(new GridBagLayout());
		jLabel = new JLabel(" Connections");
		jLabel.setHorizontalAlignment(JLabel.LEFT);
		jPanel.add(jLabel);
		jPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		inputPanelWrapper.add(jPanel);
        
        dtm = new DefaultTableModel(getConnections(), COLUMNS);
        jtable = new JTable(dtm){
			private static final long serialVersionUID = 1L;

			public boolean isCellEditable(int row, int column){
				if(column != 2)
					return true;
				return false;
        	}
    	};
    	
    	jtable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
			    JTable table = (JTable)e.getSource();
			    int rowAtPoint = table.rowAtPoint(e.getPoint());
			    int columnAtPoint = table.columnAtPoint(e.getPoint());
			    
			    if(columnAtPoint == 4) {
			    	if(GuiUtils.confirm(AddLink.this, "Do you realy want to remove the edge [ " + table.getValueAt(rowAtPoint, 0) +
			    			" ] <---> [ " + table.getValueAt(rowAtPoint, 1) + " ] ?") == JOptionPane.YES_OPTION) {
			    		
			    		for(Node node : graph.getDevicesList().keySet()) {
			    			if(node.getName() == table.getValueAt(rowAtPoint, 0)) {
			    				int index = 0;
			    				for(Link edge : graph.getDevicesList().get(node)) {
			    					if(edge.getNode().getName() == table.getValueAt(rowAtPoint, 1))
			    						break;
			    					index++;
			    				}
			    				graph.getDevicesList().get(node).remove(index);
			    				dtm.removeRow(rowAtPoint);
			    				break;
			    			}
			    		}
			    		
			    		ComboBoxModel<String> sourceNodeModel = new DefaultComboBoxModel(graph.getDevicesList().keySet().toArray());
						sourceNode.setModel(sourceNodeModel);
						sourceNodeModel.setSelectedItem(null);
			    	}	
			    }
			}
        });
    	
    	jtable.getColumn("Remove").setCellRenderer(new GuiUtils.ButtonRenderer());
    	configureTable(jtable);
        
        JScrollPane jScrollPane = new JScrollPane(jtable);
        jScrollPane.setMaximumSize(new Dimension(WIDTH, HEIGHT-250));
        jScrollPane.setMinimumSize(new Dimension(WIDTH, HEIGHT-250));
        jScrollPane.setPreferredSize(new Dimension(WIDTH, HEIGHT-250));
        inputPanelWrapper.add(jScrollPane);
        
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
	
	/**
	 * Gets the content of the link table.
	 * 
	 * @return the content of the link table
	 */
	private String[][] getConnections() {
		int total = 0;		
		for(Node node : graph.getDevicesList().keySet())
			total += graph.getDevicesList().get(node).size();
		
		String[][] lists = new String[total][];
		int index = 0;
		
		for(Node node : graph.getDevicesList().keySet()) {
			for(Link edge : graph.getDevicesList().get(node)) {
				String[] list = new String[5];
				
				list[0] = node.getName();
				list[1] = edge.getNode().getName();
				list[2] = Double.toString(edge.getLatency());
				list[3] = Double.toString(edge.getBandwidth());
				list[4] = "✘";
				lists[index++] = list;
			}
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
		jtable.getColumnModel().getColumn(0).setPreferredWidth((WIDTH - 150)/4);
		jtable.getColumnModel().getColumn(1).setPreferredWidth((WIDTH - 150)/4);
		jtable.getColumnModel().getColumn(2).setPreferredWidth((WIDTH - 150)/4);
		jtable.getColumnModel().getColumn(3).setPreferredWidth((WIDTH - 150)/4);
		jtable.getColumnModel().getColumn(4).setPreferredWidth(150);
	}
	
	private void configureInputDropDown(JComboBox<String> jComboBox) {
		jComboBox.setMaximumSize(jComboBox.getPreferredSize());
		jComboBox.setMinimumSize(new Dimension(150, jComboBox.getPreferredSize().height));
		jComboBox.setPreferredSize(new Dimension(150, jComboBox.getPreferredSize().height));
	}
	
	private void configureInput(JTextField jTextField) {
		jTextField.setMaximumSize(jTextField.getPreferredSize());
		jTextField.setMinimumSize(new Dimension(150, jTextField.getPreferredSize().height));
		jTextField.setPreferredSize(new Dimension(150, jTextField.getPreferredSize().height));
	}
	
}
