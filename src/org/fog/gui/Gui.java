package org.fog.gui;

import java.awt.AWTEvent;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Toolkit;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fog.core.Constants;
import org.fog.gui.core.Bridge;
import org.fog.gui.core.FogDeviceGui;
import org.fog.gui.core.Graph;
import org.fog.gui.core.GraphView;
import org.fog.gui.core.Link;
import org.fog.gui.core.Node;
import org.fog.gui.core.RunGUI;
import org.fog.gui.dialog.About;
import org.fog.gui.dialog.AddActuator;
import org.fog.gui.dialog.DisplayApplications;
import org.fog.utils.Util;
import org.fog.gui.dialog.AddFogDevice;
import org.fog.gui.dialog.AddLink;
import org.fog.gui.dialog.AddSensor;

public class Gui extends JFrame {
	private static final long serialVersionUID = -2238414769964738933L;
	
	private JPanel contentPane;
	private JPanel panel;
	private JPanel graph;
	
	private static Graph physicalGraph;
	private static GraphView physicalCanvas;
	
	private static JButton btnRun;
	private DisplayMode dm;
	
	private RunGUI runGUI;
	
	public Gui() {
		 //UIManager.setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel");
         //UIManager.setLookAndFeel("com.jtattoo.plaf.mint.MintLookAndFeel");
		
		// Get only the first screen (for multiple screens)
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
	    GraphicsDevice[] gs = ge.getScreenDevices();
	    dm = gs[0].getDisplayMode();
		
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setPreferredSize(new Dimension(dm.getWidth(), dm.getHeight()));
        setLocationRelativeTo(null);
        
        setTitle("Fog Computing Simulator - FogComputingSim");
        ImageIcon img = new ImageIcon(getClass().getResource("/images/icon.png"));
        setIconImage(img.getImage());

        contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout());

		initUI();
		initGraph();
		
		pack();
		setVisible(true);
		setResizable(true);
		
		Toolkit.getDefaultToolkit().addAWTEventListener(new Listener(Gui.this), AWTEvent.MOUSE_EVENT_MASK);
	}
	
	private final void initUI() {
		setUIFont(new javax.swing.plaf.FontUIResource("Serif", Font.BOLD,18));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        graph = new JPanel(new java.awt.GridLayout(1, 2));
        
		initBar();
		this.setLocation(0, 0);
	}
	
	private void initGraph(){
    	physicalGraph = new Graph();
    	physicalCanvas = new GraphView(physicalGraph);
    	
		graph.add(physicalCanvas);
		contentPane.add(graph, BorderLayout.CENTER);
    }
	
    private final void initBar() {
		ActionListener addFogDeviceListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddFogDeviceDialog();
		    	verifyRun();
		    }
		};
		
		ActionListener addLinkListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddLinkDialog();
		    	verifyRun();
		    }
		};
		
		ActionListener addAppListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddAppDialog();
		    }
		};
		
		ActionListener addActuatorListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddActuatorDialog();
		    	verifyRun();
		    }
		};
		
		ActionListener addSensorListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	openAddSensorDialog();
		    	verifyRun();
		    }
		};
		
		ActionListener importPhyTopoListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	String fileName = importFile("json");
		    	
		    	if(fileName != null && fileName.length() != 0) {
		    		try {
		    			Graph phyGraph= Bridge.jsonToGraph(fileName);
				    	physicalGraph = phyGraph;
				    	physicalCanvas.setGraph(physicalGraph);
				    	physicalCanvas.repaint();
					} catch (Exception e1) {
						Util.prompt(Gui.this, "Invalid File", "Error");
					}
		    	}
		    	verifyRun();
		    }
		};
		
		ActionListener savePhyTopoListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	try {
					saveFile("json", physicalGraph);
				} catch (IOException e1) {
					Util.prompt(Gui.this, "Something went wrong...", "Error");
				}
		    }
		};
		
		ActionListener runListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	runGUI = new RunGUI(physicalGraph);
            	Gui.this.setVisible(false);
            }
        };
        
        ActionListener helpListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	openAboutDialog();
            }
        };
		
		ActionListener exitListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        };
    	
        JToolBar toolbar = new JToolBar();
        ImageIcon iSensor = new ImageIcon(getClass().getResource("/images/sensor.png"));
        ImageIcon iActuator = new ImageIcon(getClass().getResource("/images/actuator.png"));
        ImageIcon iFogDevice = new ImageIcon(getClass().getResource("/images/fog.png"));
        ImageIcon iLink = new ImageIcon(getClass().getResource("/images/link.png"));
        ImageIcon iApp = new ImageIcon(getClass().getResource("/images/app.png"));
        ImageIcon iHOpen = new ImageIcon(getClass().getResource("/images/load.png"));
        ImageIcon iHSave = new ImageIcon(getClass().getResource("/images/save.png"));
        ImageIcon run = new ImageIcon(getClass().getResource("/images/play.png"));
        ImageIcon help = new ImageIcon(getClass().getResource("/images/help.png"));
        ImageIcon exit = new ImageIcon(getClass().getResource("/images/exit.png"));

        final JButton btnSensor = new JButton(iSensor);
        btnSensor.setToolTipText("Add Sensor");
        final JButton btnActuator = new JButton(iActuator);
        btnActuator.setToolTipText("Add Actuator");
        final JButton btnFogDevice = new JButton(iFogDevice);
        btnFogDevice.setToolTipText("Add Fog Device");
        final JButton btnLink = new JButton(iLink);
        btnLink.setToolTipText("Add Link");
        final JButton btnApp = new JButton(iApp);
        btnApp.setToolTipText("Add Application");
        final JButton btnHopen = new JButton(iHOpen);
        btnHopen.setToolTipText("Open Scenario");
        final JButton btnHsave = new JButton(iHSave);
        btnHsave.setToolTipText("Save Scenario");
        
        btnRun = new JButton(run);
        btnRun.setToolTipText("Start simulation");
        
        JButton btnHelp = new JButton(help);
        btnHelp.setToolTipText("About FogComputingSim");
        toolbar.setAlignmentX(0);
        
        JButton btnExit = new JButton(exit);
        btnExit.setToolTipText("Exit FogComputingSim");
        toolbar.setAlignmentX(0);
        
        btnSensor.addActionListener(addSensorListener);
        btnActuator.addActionListener(addActuatorListener);
        btnFogDevice.addActionListener(addFogDeviceListener);
        btnLink.addActionListener(addLinkListener);
        btnApp.addActionListener(addAppListener);
        btnHopen.addActionListener(importPhyTopoListener);
        btnHsave.addActionListener(savePhyTopoListener);
        btnRun.addActionListener(runListener);
        btnHelp.addActionListener(helpListener);
        btnExit.addActionListener(exitListener);

        toolbar.add(btnSensor);
        toolbar.add(btnActuator);
        toolbar.add(btnFogDevice);
        toolbar.add(btnLink);
        toolbar.add(btnApp);
        
        toolbar.addSeparator(new Dimension(dm.getWidth()/3, 0));
        toolbar.add(btnHopen);
        toolbar.add(btnHsave);
        toolbar.add(btnRun);
        
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(btnHelp);
        toolbar.add(btnExit);

        panel.add(toolbar);
        contentPane.add(panel, BorderLayout.NORTH);
    	btnRun.setEnabled(false);
    }

    private void openAddActuatorDialog() {
		new AddActuator(physicalGraph, Gui.this, null);
		physicalCanvas.repaint();
	}
	
	private void openAddSensorDialog() {
		new AddSensor(physicalGraph, Gui.this, null);
		physicalCanvas.repaint();
	}

	private void openAddFogDeviceDialog() {
		new AddFogDevice(physicalGraph, Gui.this, null);
    	physicalCanvas.repaint();
	}
	
	private void openAddLinkDialog() {
		new AddLink(physicalGraph, Gui.this);
    	physicalCanvas.repaint();
	}

	private void openAddAppDialog() {
		new DisplayApplications(physicalGraph, Gui.this);
    	physicalCanvas.repaint();
	}
	
	private void openAboutDialog() {
		new About(Gui.this);
    	physicalCanvas.repaint();
	}
    
    private String importFile(String type) {
        JFileChooser fileopen = new JFileChooser();
        File workingDirectory = new File(System.getProperty("user.dir"));
        fileopen.setCurrentDirectory(workingDirectory);
        
        FileFilter filter = new FileNameExtensionFilter(type.toUpperCase() + " Files", type);
        fileopen.addChoosableFileFilter(filter);

        int ret = fileopen.showDialog(panel, " Import file");

        if (ret == JFileChooser.APPROVE_OPTION) {
            File file = fileopen.getSelectedFile();
            return file.getPath();
        }
        return "";
    }
    
    private void saveFile(String type, Graph graph) throws IOException {
    	JFileChooser fileopen = new JFileChooser();
    	File workingDirectory = new File(System.getProperty("user.dir"));
        fileopen.setCurrentDirectory(workingDirectory);
        
        FileFilter filter = new FileNameExtensionFilter(type.toUpperCase() + " Files", type);
        fileopen.addChoosableFileFilter(filter);

        int ret = fileopen.showSaveDialog(panel);

        if (ret == JFileChooser.APPROVE_OPTION) {
        	String jsonText = graph.toJsonString();
            String path = fileopen.getSelectedFile().toString();
            File file = new File(path);
    		FileOutputStream out = new FileOutputStream(file);
			out.write(jsonText.getBytes());
			out.close();
        }
    }
    
    private static void setUIFont(javax.swing.plaf.FontUIResource f) {
        @SuppressWarnings("rawtypes")
		java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
          Object key = keys.nextElement();
          Object value = UIManager.get (key);
          if (value != null && value instanceof javax.swing.plaf.FontUIResource)
            UIManager.put (key, f);
          }
    }
    
    private static class Listener implements AWTEventListener {
    	private JFrame frame;
    	
    	public Listener(final JFrame frame) {
    		this.frame = frame;
		}
    	
        @Override
		public void eventDispatched(AWTEvent event) {
        	MouseEvent mv = (MouseEvent)event;
        	if (mv.getID() == MouseEvent.MOUSE_CLICKED && mv.getClickCount() > 1)
        		physicalCanvas.openDeviceDetails(frame, physicalCanvas, physicalGraph,
        				new Point(mv.getX(), mv.getY()));
		}
    }
    
    public static void verifyRun() {
    	btnRun.setEnabled(false);
    	
    	ArrayList<Node> list = new ArrayList<Node>();
    	for(Node node : physicalGraph.getDevicesList().keySet()) {
    		if(node.getType().equals(Constants.FOG_TYPE))
    			if(((FogDeviceGui)node).getApplication().length() > 0)
    				list.add(node);
    		
    		boolean isConnected = false;
    		for(Node node1 : physicalGraph.getDevicesList().keySet())
				for(Link edge : physicalGraph.getDevicesList().get(node1))
					if(edge.getNode().getName().equals(node.getName()) || node1.getName().equals(node.getName()))
						isConnected =  true;
    		
			if(!isConnected)
				return;
    	}
    	
    	if(list.size() > 0)
    		btnRun.setEnabled(true);
    	else
    		return;
    	
    	for(Node fogNode : list) {
    		if(!hasSensorActuator(fogNode)) {
    			btnRun.setEnabled(false);
    			return;
    		}
    	}
    }
    
    public static boolean hasSensorActuator(Node fogNode) {
    	boolean sensor = false, actuator = false;
		
		for(Node node : physicalGraph.getDevicesList().keySet()) {
			if(fogNode.equals(node)) {
				for(Link edge : physicalGraph.getDevicesList().get(node)) {
	    			if(!sensor && edge.getNode().getType().equals(Constants.SENSOR_TYPE))
	    				sensor = true;
	    			
	    			if(!actuator && edge.getNode().getType().equals(Constants.ACTUATOR_TYPE))
	    				actuator = true;
	    			
	    			if(actuator && sensor)
	    				break;
	    		}
			}else if(node.getType().equals(Constants.SENSOR_TYPE) || node.getType().equals(Constants.ACTUATOR_TYPE)) {
				for(Link edge : physicalGraph.getDevicesList().get(node)) {    					
					if(!sensor && edge.getNode().equals(fogNode) && node.getType().equals(Constants.SENSOR_TYPE))
	    				sensor = true;
				
					if(!actuator && edge.getNode().equals(fogNode) && node.getType().equals(Constants.ACTUATOR_TYPE))
						actuator = true;
				
					if(actuator && sensor)
	    				break;
				}
			}
		}
		
		if(!actuator || !sensor)
			return false;
		return true;
    }
    
    public Graph getPhysicalGraph() {
    	return physicalGraph;
    }
    
    public RunGUI getRunGUI() {
    	return runGUI;
    }

}
