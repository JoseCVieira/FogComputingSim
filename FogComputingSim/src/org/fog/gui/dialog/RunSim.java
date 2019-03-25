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
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.Config;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;

public class RunSim extends JDialog {
	private static final long serialVersionUID = -8313194085507492462L;
	private static final boolean DEBUG_MODE = false;
	
	private static List<FogBroker> fogBrokers = new ArrayList<FogBroker>();
	private static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	private static List<Actuator> actuators = new ArrayList<Actuator>();
	private static List<Sensor> sensors = new ArrayList<Sensor>();
	private static Graph graph;

	public RunSim(final Graph graph, final JFrame frame){
		RunSim.graph = graph;
		setLayout(new BorderLayout());
		
        add(initUI(), BorderLayout.CENTER);
        
        Thread thread = new Run();
        thread.start();

		setTitle(" Run Simulation");
		setModal(true);
		setPreferredSize(new Dimension(900, 600));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
	}
	
	private JPanel initUI(){
		JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        
		panel.add((JComponent)Box.createRigidArea(new Dimension(0, 200)));
		
		JLabel msgLabel = new JLabel(" Simulation is executing");
		msgLabel.setAlignmentX(CENTER_ALIGNMENT);
		panel.add(msgLabel);
		
		JLabel imageLabel = new JLabel(new ImageIcon(this.getClass().getResource("/images/run.gif")));
		imageLabel.setAlignmentX(CENTER_ALIGNMENT);
        panel.add(imageLabel);
        
        JTextArea outputArea = new JTextArea();
        outputArea.setLineWrap(true);
        outputArea.setWrapStyleWord(true);
        outputArea.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        outputArea.setEditable(false);

        JScrollPane pane = new JScrollPane();
        pane.getViewport().add(outputArea);
        panel.add(pane);
        pane.setVisible(false);
        return panel;
	}
	
	public class Run extends Thread {
		public Run(){
		}
		
		public void run(){
			try {
    			if(DEBUG_MODE) {
    				Logger.setLogLevel(Logger.DEBUG);
    				Logger.setEnabled(true);
    			}else
    				Log.disable();
    			
    			CloudSim.init(Calendar.getInstance());
    			createFogDevices(RunSim.graph);
    			
    			ArrayList<FogDeviceGui> clients = getClients();
    			
    			for(FogDeviceGui fog : clients) {
    				FogBroker broker = new FogBroker(fog.getName());
    				createSensorActuator(graph, fog.getName(), broker.getId(), fog.getApplication());
    				fogBrokers.add(broker);
    			}
    			
    			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
    			
    			for(FogDevice fogDevice : fogDevices)
    				fogDevice.setController(controller);
    			
    			for(FogDeviceGui fog : clients) {
    				FogBroker broker = getFogBrokerByName(fog.getName());
    				Application application = createApplication(graph, fog.getApplication(), broker.getId());
    				application.setClientId(getFogDeviceByName(fog.getName()).getId());
    				
    				ModuleMapping moduleMapping = ModuleMapping.createModuleMapping();
    				
    				if(broker.getId() == 8) {
    					/*moduleMapping.addModuleToDevice("client_7", "Proxy");
    					moduleMapping.addModuleToDevice("concentration_calculator_7", "Client1");
    					moduleMapping.addModuleToDevice("connector_7", "FogNode1");*/
    					
    					moduleMapping.addModuleToDevice("client_8", "Cloud");
    					moduleMapping.addModuleToDevice("concentration_calculator_8", "Cloud");
    					moduleMapping.addModuleToDevice("connector_8", "Cloud");
    				}else {
    					/*moduleMapping.addModuleToDevice("client_10", "Proxy");
    					moduleMapping.addModuleToDevice("concentration_calculator_10", "Client2");
    					moduleMapping.addModuleToDevice("connector_10", "FogNode2");*/
    					
    					moduleMapping.addModuleToDevice("client_11", "Cloud");
    					moduleMapping.addModuleToDevice("concentration_calculator_11", "Cloud");
    					moduleMapping.addModuleToDevice("connector_11", "Cloud");
    				}
    				
					controller.submitApplication(application, new ModulePlacementMapping(fogDevices, application, moduleMapping));
					
					if(DEBUG_MODE)
						printDetails(application);
    			}
    			
    			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
    			CloudSim.startSimulation();
    			CloudSim.stopSimulation();
    			System.exit(0);
    		} catch (Exception e) {
    			e.printStackTrace();
    			Log.printLine("Unwanted errors happen");
    		}
		}
		
		private ArrayList<FogDeviceGui> getClients(){
			ArrayList<FogDeviceGui> clients = new ArrayList<FogDeviceGui>();
			
			for(Node node : graph.getDevicesList().keySet())
				if(node.getType().equals(Config.FOG_TYPE))
	    			if(((FogDeviceGui)node).getApplication().length() > 0)
	    				clients.add((FogDeviceGui)node);
			
			return clients;
		}
		
		private void createFogDevices(Graph graph) {
			for(Node node : graph.getDevicesList().keySet())
				if(node.getType().equals(Config.FOG_TYPE))
					fogDevices.add(createFogDevice((FogDeviceGui)node));
			
			for (Entry<Node, List<Edge>> entry : graph.getDevicesList().entrySet()) {
				if(!entry.getKey().getType().equals(Config.FOG_TYPE))
					continue;
				
				FogDeviceGui fog1 = (FogDeviceGui)entry.getKey();
				FogDevice f1 = getFogDeviceByName(fog1.getName());
				
				for (Edge edge : entry.getValue()) {
					if(!edge.getNode().getType().equals(Config.FOG_TYPE))
						continue;						
						
					FogDeviceGui fog2 = (FogDeviceGui)edge.getNode();
					FogDevice f2 = getFogDeviceByName(fog2.getName());
					
					if(fog1.getLevel() > fog2.getLevel()) {
						f1.getParentsIds().add(f2.getId());
						f2.getChildrenIds().add(f1.getId());
					}else if(fog1.getLevel() < fog2.getLevel()) {
						f2.getParentsIds().add(f1.getId());
						f1.getChildrenIds().add(f2.getId());
					}else {
						f2.getBrothersIds().add(f1.getId());
						f1.getBrothersIds().add(f2.getId());
					}
					
					f2.getUpStreamLatencyMap().put(f1.getId(), edge.getLatency());
					f2.getDownStreamLatencyMap().put(f1.getId(), edge.getLatency());
					f1.getUpStreamLatencyMap().put(f2.getId(), edge.getLatency());
					f1.getDownStreamLatencyMap().put(f2.getId(), edge.getLatency());
				}
			}
			
			for (Entry<Node, List<Edge>> entry : graph.getDevicesList().entrySet()) {
				if(!entry.getKey().getType().equals(Config.FOG_TYPE))
					continue;
				
				FogDeviceGui fog1 = (FogDeviceGui)entry.getKey();
				FogDevice f1 = getFogDeviceByName(fog1.getName());
				
				System.out.println("id: " + f1.getId() + " Name: " + fog1.getName());
				
				System.out.println("Parents: " +  f1.getParentsIds());
				System.out.println("Children: " + f1.getChildrenIds());
				System.out.println("Brothers: " + f1.getBrothersIds());
				
				System.out.println("UpStreamMap: " + f1.getUpStreamLatencyMap());
				System.out.println("DownStreamMap: " + f1.getDownStreamLatencyMap());
				
				System.out.println("\n\n\n");
			}
			
			for(FogDevice fogDevice : fogDevices)
				if(fogDevice.getParentsIds().size() == 0)
					fogDevice.getParentsIds().add(-1);
		}
		
		private FogDevice createFogDevice(FogDeviceGui fog) {
			List<Pe> peList = new ArrayList<Pe>();
			peList.add(new Pe(0, new PeProvisionerOverbooking(fog.getMips())));

			PowerHost host = new PowerHost(
					FogUtils.generateEntityId(),
					new RamProvisionerSimple((int)fog.getRam()),
					new BwProvisionerOverbooking((long)fog.getUpBw()*1024),
					fog.getStorage(),
					peList,
					new StreamOperatorScheduler(peList),
					new FogLinearPowerModel(fog.getBusyPower(), fog.getIdlePower())
				);

			List<Host> hostList = new ArrayList<Host>();
			hostList.add(host);

			LinkedList<Storage> storageList = new LinkedList<Storage>();

			FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics("x86", "Linux", "Xen",
					host, 10.0, fog.getCostPerSec(), fog.getRateMips(), fog.getRateRam(), fog.getRateStorage(), fog.getRateBwUp());
			
			try {
				return new FogDevice(fog.getName(), characteristics, new AppModuleAllocationPolicy(hostList),
						storageList, 10, fog.getUpBw(), fog.getDownBw());
			} catch (Exception e) {
				e.printStackTrace();
				return null;
			}
		}
		
		@SuppressWarnings({"serial"})
		private Application createApplication(Graph graph, String appId, int userId){
			ApplicationGui applicationGui = null;
			
			for(ApplicationGui app : graph.getAppList())
				if(app.getAppId().equals(appId))
					applicationGui = app;
			
			if(applicationGui == null) return null;
			
			Application application = new Application(appId, userId);
			
			for(AppModule appModule : applicationGui.getModules())
				application.addAppModule(appModule);
			
			for(AppEdge appEdge : applicationGui.getEdges())
				application.addAppEdge(appEdge);
			
			for(AppModule appModule : applicationGui.getModules())
				for(Pair<String, String> pair : appModule.getSelectivityMap().keySet())
					application.addTupleMapping(appModule.getName(), pair,
							((FractionalSelectivity)appModule.getSelectivityMap().get(pair)).getSelectivity());
			
			List<AppLoop> loops = new ArrayList<AppLoop>(){{
				add(new AppLoop(new ArrayList<String>(){{
						add("EEG_" + userId);
						add("client_" + userId);
						add("concentration_calculator_" + userId);
						add("client_" + userId);
						add("DISPLAY_" + userId);
					}}
				));
			}};
			
			application.setLoops(loops);
			return application;
		}
		
		private void createSensorActuator(Graph graph, String clientName, int userId, String appId) {
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
			
			sensors.add(new Sensor(sensor.getName(), tupleType + "_" + userId, userId, appId + "_" + userId,
					sensor.getDistribution(), getFogDeviceByName(clientName).getId(), sensorLat));

			actuators.add(new Actuator(actuator.getName(), userId, appId + "_" + userId,
					getFogDeviceByName(clientName).getId(), actuatorLat, actuatorType + "_" + userId));
		}
		
		private FogDevice getFogDeviceByName(String name) {
			for(FogDevice fogDevice : fogDevices)
				if(fogDevice.getName().equals(name))
					return fogDevice;
			return null;
		}
		
		private FogBroker getFogBrokerByName(String name) {
			for(FogBroker fogBroker : fogBrokers)
				if(fogBroker.getName().equals(name))
					return fogBroker;
			return null;
		}
		
		private void printDetails(Application application) {
			System.out.println("\n[FOG DEVICES]:\n");
			for(FogDevice fd : fogDevices)
				System.out.println(fd);
			
			System.out.println("\n[ACTUATORS]:\n");
			for(Actuator act : actuators)
				System.out.println(act);
			
			System.out.println("\n[SENSORS]:\n");
			for(Sensor s : sensors)
				System.out.println(s);
			
			System.out.println("\n[APP MODULES]:\n");
			for(AppModule appModule : application.getModules())
				System.out.println(appModule);
			
			System.out.println("\n[APP EDGES]:\n");
			for(AppEdge appEdge : application.getEdges())
				System.out.println(appEdge);
			
			System.out.println("\n[APP TUPLES]:\n");
			for(AppModule appModule : application.getModules())
				for(Pair<String, String> pair : appModule.getSelectivityMap().keySet())
					System.out.println("From: " + pair.getFirst() + " To: " + pair.getSecond() +
							" Value: " + pair.getValue());
			
			System.out.println("\n[APPLICATION]:\n" + application);
		}
	}
	
}
