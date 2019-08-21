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
import java.awt.event.*;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.fog.gui.dialog.DisplayApplications;
import org.fog.gui.dialog.DisplayRandom;
import org.fog.gui.dialog.DisplaySettings;
import org.fog.gui.core.Bridge;
import org.fog.gui.core.Node;
import org.fog.gui.core.Graph;
import org.fog.gui.core.GraphView;
import org.fog.gui.core.Link;
import org.fog.gui.core.RunGUI;
import org.fog.gui.dialog.AddFogDevice;
import org.fog.gui.dialog.AddLink;

/**
 * Class which is responsible for running the Graphical User Interface.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Gui extends JFrame {
	private static final long serialVersionUID = -2238414769964738933L;
	
	private JPanel contentPane;
	private JPanel panel;
	private JPanel graph;
	
	/** The current topology */
	private static Graph physicalGraph;
	
	/** Panel that displays a graph */
	private static GraphView physicalCanvas;
	
	/** Button which allows to run the current topology */
	private static JButton btnRun;
	
	/** Object which hold the current topology */
	private RunGUI runGUI;
	
	/**
	 * Creates a new GUI object.
	 */
	public Gui() {
		 //UIManager.setLookAndFeel("com.jtattoo.plaf.hifi.HiFiLookAndFeel");
         //UIManager.setLookAndFeel("com.jtattoo.plaf.mint.MintLookAndFeel");
		
	    setExtendedState(JFrame.MAXIMIZED_BOTH); 
	    setUndecorated(true);
		
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        //setPreferredSize(new Dimension(dm.getWidth(), dm.getHeight()));
        setLocationRelativeTo(null);
        
        setTitle("Fog Computing Simulator - FogComputingSim");
        ImageIcon img = new ImageIcon(getClass().getResource("/images/icon.png"));
        setIconImage(img.getImage());

        contentPane = new JPanel();
		setContentPane(contentPane);
		contentPane.setLayout(new BorderLayout());

		initUI();
		
		pack();
		setVisible(true);
		setResizable(true);
		
		Toolkit.getDefaultToolkit().addAWTEventListener(new Listener(Gui.this), AWTEvent.MOUSE_EVENT_MASK);
	}
	
	/**
	 * Initializes the GUI.
	 */
	private final void initUI() {
		setUIFont(new javax.swing.plaf.FontUIResource("Serif", Font.BOLD,18));

        panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        
        graph = new JPanel(new java.awt.GridLayout(1, 2));
        
		initBar();
		this.setLocation(0, 0);
		
		physicalGraph = new Graph();
    	physicalCanvas = new GraphView(physicalGraph);
    	
		graph.add(physicalCanvas);
		contentPane.add(graph, BorderLayout.CENTER);
	}
	
	/**
	 * Creates menu bar.
	 */
    private final void initBar() {
    	
    	// Create button listeners
		ActionListener addFogDeviceListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	new AddFogDevice(physicalGraph, Gui.this, null);
		    	physicalCanvas.repaint();
		    	verifyRun();
		    }
		};
		
		ActionListener addLinkListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	new AddLink(physicalGraph, Gui.this);
		    	physicalCanvas.repaint();
		    	verifyRun();
		    }
		};
		
		ActionListener addAppListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	new DisplayApplications(physicalGraph, Gui.this);
		    	physicalCanvas.repaint();
		    }
		};
		
		ActionListener randomListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	new DisplayRandom(physicalGraph, Gui.this);
		    	physicalCanvas.repaint();
		    	verifyRun();
		    }
		};
		
		ActionListener importPhyTopoListener = new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
		    	String fileName = importFile("json");
		    	
		    	if(fileName != null && fileName.length() != 0) {
		    		try {
				    	physicalGraph = Bridge.jsonToGraph(fileName);
				    	physicalCanvas.setGraph(physicalGraph);
				    	physicalCanvas.repaint();
					} catch (Exception e1) {
						GuiUtils.prompt(Gui.this, "Invalid File", "Error");
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
					GuiUtils.prompt(Gui.this, "Something went wrong...", "Error");
				}
		    }
		};
		
		ActionListener runListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	runGUI = new RunGUI(physicalGraph);
            	Gui.this.setVisible(false);
            }
        };
        
        ActionListener settingsListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	new DisplaySettings(Gui.this);
		    	physicalCanvas.repaint();
            }
        };
		
		ActionListener exitListener = new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                System.exit(0);
            }
        };
    	
        // Create the buttons
        JToolBar toolbar = new JToolBar();
        ImageIcon iFogDevice = new ImageIcon(getClass().getResource("/images/fog.png"));
        ImageIcon iLink = new ImageIcon(getClass().getResource("/images/link.png"));
        ImageIcon iApp = new ImageIcon(getClass().getResource("/images/app.png"));
        ImageIcon random = new ImageIcon(getClass().getResource("/images/random.png"));
        ImageIcon iHOpen = new ImageIcon(getClass().getResource("/images/load.png"));
        ImageIcon iHSave = new ImageIcon(getClass().getResource("/images/save.png"));
        ImageIcon run = new ImageIcon(getClass().getResource("/images/play.png"));
        ImageIcon settings = new ImageIcon(getClass().getResource("/images/settings.png"));
        ImageIcon exit = new ImageIcon(getClass().getResource("/images/exit.png"));
        
        final JButton btnFogDevice = new JButton(iFogDevice);
        btnFogDevice.setToolTipText("Add Fog Device");
        final JButton btnLink = new JButton(iLink);
        btnLink.setToolTipText("Add Link");
        final JButton btnApp = new JButton(iApp);
        btnApp.setToolTipText("Add Application");
        final JButton btnRandom = new JButton(random);
        btnRandom.setToolTipText("Random topology");
        final JButton btnHopen = new JButton(iHOpen);
        btnHopen.setToolTipText("Open topology");
        final JButton btnHsave = new JButton(iHSave);
        btnHsave.setToolTipText("Save topology");
        final JButton btnSettings  = new JButton(settings);
        btnSettings.setToolTipText("Simulation settings");
        
        btnRun = new JButton(run);
        btnRun.setToolTipText("Start simulation");
        
        JButton btnExit = new JButton(exit);
        btnExit.setToolTipText("Exit FogComputingSim");
        toolbar.setAlignmentX(0);
        
        // Define which button listener
        btnFogDevice.addActionListener(addFogDeviceListener);
        btnLink.addActionListener(addLinkListener);
        btnApp.addActionListener(addAppListener);
        btnRandom.addActionListener(randomListener);
        btnHopen.addActionListener(importPhyTopoListener);
        btnHsave.addActionListener(savePhyTopoListener);
        btnRun.addActionListener(runListener);
        btnSettings.addActionListener(settingsListener);
        btnExit.addActionListener(exitListener);
        
        toolbar.add(btnFogDevice);
        toolbar.add(btnLink);
        toolbar.add(btnApp);
        
        // Get only the first screen (for multiple screens)
 		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
 	    GraphicsDevice[] gs = ge.getScreenDevices();
 	    DisplayMode dm = gs[0].getDisplayMode();
        
        toolbar.addSeparator(new Dimension((int) (dm.getWidth()/3.2), 0));
        toolbar.add(btnRandom);
        toolbar.add(btnHopen);
        toolbar.add(btnHsave);
        toolbar.add(btnRun);
        toolbar.add(btnSettings);
        
        toolbar.add(Box.createHorizontalGlue());
        toolbar.add(btnExit);
        
        panel.add(toolbar);
        contentPane.add(panel, BorderLayout.NORTH);
    	btnRun.setEnabled(false);
    }
    
    /**
     * Opens user current directory to allow to choose a topology written in a given file (e.g., JSON).
     * 
     * @param type type of file
     * @return return the file path; can be empty
     */
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
    
    /**
     * Saves the current topology in a given file.
     * 
     * @param type type of file
     * @param graph the object which holds the current topology
     * @throws IOException if file does not exists
     */
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
    
    /**
     * Sets the type of font to be displayed within the GUI.
     * 
     * @param f the type of font
     */
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
    
    /**
     * Verifies if the topology is correct in order to simulate it. If it is, it enables the run button, otherwise disable it.
     */
    public static void verifyRun() {
    	btnRun.setEnabled(false);
    	
    	// Its required more than one node in the physical topology
    	if(physicalGraph.getDevicesList().size() <= 1) return;
    	
    	boolean application = false;
    	boolean fixedTopology = false;
    	for(Node node : physicalGraph.getDevicesList().keySet()) {
    		if(!node.getApplication().isEmpty()) {
    			application = true;
    		}
    		
    		for(Node node1 : physicalGraph.getDevicesList().keySet()) {
				for(Link edge : physicalGraph.getDevicesList().get(node1)) {
					if(edge.getNode().getName().equals(node.getName()))
						fixedTopology =  true;
				}
    		}
    	}
    	
    	// Its required at least one application
    	if(!application) return;
    	
    	// Its required at least two fixed nodes (mobile nodes may not exit)
    	if(!fixedTopology) return;
    	
    	btnRun.setEnabled(true);
    }
    
    /**
     * Gets the object which hold the topology.
     * 
     * @return the topology
     */
    public RunGUI getRunGUI() {
    	return runGUI;
    }

}
