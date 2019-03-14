package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.gui.core.ActuatorGui;
import org.fog.gui.core.ApplicationGui;
import org.fog.gui.core.Edge;
import org.fog.gui.core.FogDeviceGui;
import org.fog.gui.core.Graph;
import org.fog.gui.core.Node;
import org.fog.gui.core.SensorGui;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.MyModulePlacement;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.Config;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;

public class RunSim extends JDialog {
	private static final long serialVersionUID = -8313194085507492462L;
	public static final boolean MY_PLACEMENT = true;
	
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	
	private JTextArea outputArea;
	private JLabel imageLabel;
	private JComponent space;
	private JScrollPane pane;
	private JLabel msgLabel;
	private JPanel panel;
	private Graph graph;

	public RunSim(final Graph graph, final JFrame frame){
		this.graph = graph;
		setLayout(new BorderLayout());
		
        //initUI();
        run();
        
        //add(panel, BorderLayout.CENTER);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle(" Run Simulation");
		setModal(true);
		setPreferredSize(new Dimension(900, 600));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}
	
	private void initUI(){
		imageLabel = new JLabel(new ImageIcon(this.getClass().getResource("/images/run.gif")));
		imageLabel.setAlignmentX(CENTER_ALIGNMENT);
		msgLabel = new JLabel("Simulation is executing");
		msgLabel.setAlignmentX(CENTER_ALIGNMENT);
		space = (JComponent)Box.createRigidArea(new Dimension(0, 200));
		
		panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
		panel.add(space);
		panel.add(msgLabel);
        panel.add(imageLabel);

        pane = new JScrollPane();
        outputArea = new JTextArea();

        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        outputArea.setEditable(false);

        pane.getViewport().add(outputArea);
        panel.add(pane);
        pane.setVisible(false);
	}
	
	private void run(){
		//try {
			Log.disable();
			CloudSim.init(1, Calendar.getInstance(), false);
			
			for(Node node : graph.getDevicesList().keySet()) {
				if(node.getType().equals(Config.FOG_TYPE)) {
					FogDeviceGui fog = (FogDeviceGui)node;
					
	    			if(fog.getApplication().length() > 0) {
	    				FogBroker broker = null;
						try {
							broker = new FogBroker(fog.getName());
						} catch (Exception e) {
							e.printStackTrace();
						}
	    				Application application = createApplication(graph,
	    						fog.getApplication(), broker.getId());
	    				
	    				if(application == null) continue;
	    				
	    				application.setUserId(broker.getId());
	    				
	    				createFogDevices(graph);
	    				createSensorActuator(graph, fog.getName(), broker.getId(), fog.getApplication());
	    				
	    				printDetails(application);
	    				System.exit(0);
	    				
	    				ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
	    				
	    				Controller controller = new Controller("master-controller",
	    						fogDevices, sensors, actuators);
	    				
	    				for(FogDevice fogDevice : fogDevices)
	    					fogDevice.setController(controller);
	    				
	    				if(MY_PLACEMENT)
	    					controller.submitApplication(application, 0,
	    							new MyModulePlacement(fogDevices, sensors, actuators,
	    							application, moduleMapping));
	    				else
	    					controller.submitApplication(application, 0,
	    							new ModulePlacementEdgewards(fogDevices, sensors, actuators,
	    							application, moduleMapping));
	    			}
				}
			}
			
			System.exit(0);
			
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			CloudSim.startSimulation();
			CloudSim.stopSimulation();
			
			Log.printLine("MyApp finished!");
	}
	
	private void append(String content){
		outputArea.append(content+"\n");
	}

	@SuppressWarnings({"serial"})
	private static Application createApplication(Graph graph, String appId, int userId){
		ApplicationGui applicationGui = null;
		
		for(ApplicationGui app : graph.getAppList())
			if(app.getAppId().equals(appId))
				applicationGui = app;
		
		if(applicationGui == null) return null;
		
		Application application = Application.createApplication(appId, userId);
		
		for(AppModule appModule : applicationGui.getModules()) {
			application.addAppModule(appModule);
			
			for(Pair<String, String> pair : appModule.getSelectivityMap().keySet())
				application.addTupleMapping(appModule.getName(), pair,
						((FractionalSelectivity)appModule.getSelectivityMap().get(pair)).getSelectivity());
		}
		
		for(AppEdge appEdge : applicationGui.getEdges())
			application.addAppEdge(appEdge);
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{
			add("EEG");
			add("client");
			add("c_calculator");
			add("client");
			add("DISPLAY");
		}});
		
		List<AppLoop> loops = new ArrayList<AppLoop>(){{
			add(loop1);
		}};
		
		application.setLoops(loops);
		application.setLoops(null);
		return application;
	}
	
	private static void createFogDevices(Graph graph) {
		for(Node node : graph.getDevicesList().keySet())
			if(node.getType().equals(Config.FOG_TYPE))
				fogDevices.add(createFogDevice((FogDeviceGui)node));
		
		for (Entry<Node, List<Edge>> entry : graph.getDevicesList().entrySet()) {
			if(entry.getKey().getType().equals(Config.FOG_TYPE)) {
				FogDeviceGui fog1 = (FogDeviceGui)entry.getKey();
				FogDevice f1 = getFogDeviceByName(fog1.getName());
				
				for (Edge edge : entry.getValue()) {
					if(edge.getNode().getType().equals(Config.FOG_TYPE)){
						FogDeviceGui fog2 = (FogDeviceGui)edge.getNode();
						FogDevice f2 = getFogDeviceByName(fog2.getName());
						
						if(fog1.getLevel() > fog2.getLevel()) {
							f1.getParentsIds().add(f2.getId());
							f1.getUpStreamLatencyMap().put(f2.getId(), edge.getLatency());
						}else if(fog1.getLevel() < fog2.getLevel()) {
							f2.getParentsIds().add(f1.getId());
							f2.getUpStreamLatencyMap().put(f1.getId(), edge.getLatency());
						}else {
							f2.getBrothersIds().add(f1.getId());
							f2.getUpStreamLatencyMap().put(f1.getId(), edge.getLatency());
							f1.getBrothersIds().add(f2.getId());
							f1.getUpStreamLatencyMap().put(f2.getId(), edge.getLatency());
						}
					}
				}
			}
		}
		
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getParentsIds().size() == 0)
				fogDevice.getParentsIds().add(-1);
	}
	
	private static void createSensorActuator(Graph graph, String clientName, int userId, String appId) {
		FogDeviceGui client = null;
		SensorGui sensor = null;
		ActuatorGui actuator = null;
		String tupleType = "";
		String actuatorType = "";
		double sensorLat = -1;
		double actuatorLat = -1;
		
		for(Node node : graph.getDevicesList().keySet())
			if(node.getName().equals(clientName))
				client = (FogDeviceGui) node;
		
		for (Entry<Node, List<Edge>> entry : graph.getDevicesList().entrySet()) {
			for (Edge edge : entry.getValue()) {
				if(entry.getKey().equals(client)){
					if(edge.getNode().getType().equals(Config.SENSOR_TYPE)) {
						sensor = (SensorGui)edge.getNode();
						sensorLat = edge.getLatency();
					}else if(edge.getNode().getType().equals(Config.ACTUATOR_TYPE)) {
						actuator = (ActuatorGui)edge.getNode();
						actuatorLat = edge.getLatency();
					}
				}else if(entry.getKey().getType().equals(Config.SENSOR_TYPE) && edge.getNode().equals(client)) {
					sensor = (SensorGui)entry.getKey();
					sensorLat = edge.getLatency();
				}else if(entry.getKey().getType().equals(Config.ACTUATOR_TYPE) && edge.getNode().equals(client)) {
					actuator = (ActuatorGui)entry.getKey();
					actuatorLat = edge.getLatency();
				}
				
				if(sensor != null && actuator != null)
					break;
			}
			if(sensor != null && actuator != null)
				break;
		}
		
		for(ApplicationGui applicationGui : graph.getAppList()) {
			if(applicationGui.getAppId().equals(appId)) {
				for(AppEdge appEdge : applicationGui.getEdges()) {
					if(appEdge.getEdgeType() == AppEdge.SENSOR)
						tupleType = appEdge.getSource();
					else if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
						actuatorType = appEdge.getDestination();
				}
			}
		}
		
		Sensor newSensor = new Sensor(sensor.getName(), tupleType, userId, appId, sensor.getDistribution());
		sensors.add(newSensor);
		newSensor.setGatewayDeviceId(getFogDeviceByName(clientName).getId());
		newSensor.setLatency(sensorLat);

		Actuator display = new Actuator(actuator.getName(), userId, appId, actuatorType);
		actuators.add(display);
		display.setGatewayDeviceId(getFogDeviceByName(clientName).getId());
		display.setLatency(actuatorLat);
		
	}
	
	private static FogDevice createFogDevice(FogDeviceGui fog) {
		List<Pe> peList = new ArrayList<Pe>();

		// Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(fog.getMips()))); // need to store Pe id and MIPS Rating

		PowerHost host = new PowerHost(
			FogUtils.generateEntityId(),
			new RamProvisionerSimple((int)fog.getRam()),
			new BwProvisionerOverbooking((long)fog.getDownBw()),
			fog.getStorage(),
			peList,
			new StreamOperatorScheduler(peList),
			new FogLinearPowerModel(87.53, 82.44/*busyPower, idlePower*/)
		);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		double time_zone = 10.0; // time zone this resource located
		double cost = 3.0; // the cost of using processing in this resource
		double costPerMem = 0.05; // the cost of using memory in this resource
		double costPerStorage = 0.001; // the cost of using storage in this resource
		double costPerBw = 0.1; // the cost of using bw in this resource
		LinkedList<Storage> storageList = new LinkedList<Storage>(); // we are not adding SAN devices by now

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				"x86", "Linux", "Xen", host, time_zone, cost, costPerMem, costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(fog.getName(), characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, fog.getUpBw(),
					fog.getDownBw(), fog.getRateMips());
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return fogdevice;
	}
	
	private static FogDevice getFogDeviceByName(String name) {
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getName().equals(name))
				return fogDevice;
		return null;
	}
	
	private static void printDetails(Application application) {
		System.out.println("Fog Devices: " + fogDevices + "\n\n");
		System.out.println("Actuators: " + actuators + "\n\n");
		System.out.println("Sensors: " + sensors + "\n\n");
		
		for(AppEdge appEdge : application.getEdges())
			System.out.println("AppEdge: " + appEdge + "\n");
		
		for(AppModule appModule : application.getModules()) {
			System.out.println("AppModule" + appModule);
			System.out.println("SelectivityMap" + appModule.getSelectivityMap() + "\n\n\n");
		}
	}
}
