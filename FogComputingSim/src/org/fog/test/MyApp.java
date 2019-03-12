package org.fog.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;

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
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.placement.ModuleMapping;
import org.fog.placement.ModulePlacementEdgewards;
import org.fog.placement.MyModulePlacement;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

public class MyApp {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	
	private static final double EEG_TRANSMISSION_TIME = 5.1;
	private static final int N_MOBILES_PER_DEPT = 2;
	private static final String APP_ID = "MyApp";
	private static final int NDEPTS = 2;
	
	public static final boolean MY_PLACEMENT = true;
	private static final boolean DEBUG_MODE = false;
	
	public static final String END_DEVICE_NAME = "end-device";
	
	public static void main(String[] args) {
		try {
			if(DEBUG_MODE) {
				Logger.setLogLevel(Logger.DEBUG);
				Logger.setEnabled(true);
			}else
				Log.disable();
			
			CloudSim.init(1, Calendar.getInstance(), false); // 1 user, do not trace events
			
			FogBroker broker = new FogBroker("broker");
			Application application = createApplication(APP_ID, broker.getId());
			application.setUserId(broker.getId());
			
			createFogDevices(broker.getId(), APP_ID);
			
			ModuleMapping moduleMapping = ModuleMapping.createModuleMapping(); // initializing a module mapping
			moduleMapping.addModuleToDevice("connector", "cloud"); // fixing all instances of the Connector module to the Cloud
			
			Controller controller = new Controller("master-controller", fogDevices, sensors, actuators);
			
			for(FogDevice fogDevice : fogDevices)
				fogDevice.setController(controller);
			
			if(MY_PLACEMENT)
				controller.submitApplication(application, 0, new MyModulePlacement(fogDevices, sensors, actuators,
						application, moduleMapping));
			else
				controller.submitApplication(application, 0, new ModulePlacementEdgewards(fogDevices, sensors, actuators,
						application, moduleMapping));
			
			System.out.println(fogDevices);
			
			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());
			CloudSim.startSimulation();
			CloudSim.stopSimulation();
			
			Log.printLine("MyApp finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}
	
	@SuppressWarnings({"serial"})
	private static Application createApplication(String appId, int userId){
		// creates an empty application model (empty directed graph)
		Application application = Application.createApplication(appId, userId);

		// Adding modules (vertices) to the application model (directed graph)
		application.addAppModule("client", 10);
		application.addAppModule("c_calculator", 10);
		application.addAppModule("connector", 10);

		// Connecting the application modules (vertices) in the application model (directed graph) with edges
		// adding edge from EEG to Client module carrying tuples of type EEG
		application.addAppEdge("EEG", "client", 3000, 500, "EEG", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client", "c_calculator", 3500, 500, "_SENSOR", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("c_calculator", "connector", 100, 1000, 1000, "PLAYER_GAME_STATE", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("c_calculator", "client", 14, 500, "CONCENTRATION", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("connector", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);

		// Defining the input-output relationships (represented by selectivity) of the application modules.
		// 0.9 tuples of type _SENSOR are emitted by Client module per incoming tuple of type EEG 
		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9));
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0));
		application.addTupleMapping("c_calculator", "_SENSOR", "CONCENTRATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0)); 
	
		// Defining application loops to monitor the latency of
		// Only one loop for monitoring : EEG(sensor) -> Client -> c_calculator -> Client -> DISPLAY (actuator)
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
		return application;
	}
	
	private static void createFogDevices(int userId, String appId) {
		List<Integer> parentsIds;
		
		// creates the fog device Cloud at the apex of the hierarchy with level=0
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
		parentsIds = new ArrayList<Integer>();
		parentsIds.add(-1);
		cloud.setParentsIds(parentsIds);
		
		// creates the fog device Proxy Server (level=1)
		//FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		FogDevice proxy = createFogDevice("proxy-server", 100, 100, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		parentsIds = new ArrayList<Integer>();
		parentsIds.add(cloud.getId());
		proxy.setParentsIds(parentsIds); // setting Cloud as parent of the Proxy Server
		proxy.getUpStreamLatencyMap().put(cloud.getId(), 100.0); // latency of connection from Proxy Server to the Cloud is 100 ms
		
		fogDevices.add(cloud);
		fogDevices.add(proxy);
		
		FogDevice[] fogDevices = new FogDevice[NDEPTS];
		List<Integer> brotherIds = new ArrayList<Integer>();
		
		// Adding a fog device for every Gateway in physical topology.
		// The parent of each gateway is the Proxy Server
		for(int i = 0; i < NDEPTS; i++) {
			fogDevices[i] = addGw(Integer.toString(i), userId, appId, proxy.getId());
			brotherIds.add(fogDevices[i].getId());
		}
		
		for(int i = 0; i < NDEPTS; i++) {
			fogDevices[i].setBrothersIds(brotherIds);
			
			for(int brotherId : brotherIds)
				if(brotherId != fogDevices[i].getId())
					fogDevices[i].getUpStreamLatencyMap().put(brotherId, 3.0);
		}
	}
	
	private static FogDevice addGw(String id, int userId, String appId, int parentId){
		List<Integer> parentsIds = new ArrayList<Integer>();
		FogDevice dept;
		System.out.println(id);
		System.out.println(Integer.parseInt(id)== 0);
		
		if(Integer.parseInt(id) == 1)
			dept = createFogDevice("fog-device-"+id, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		else
			dept = createFogDevice("fog-device-"+id, 100, 100, 10000, 10000, 1, 0.0, 107.339, 83.4333);
			
		fogDevices.add(dept);
		parentsIds.add(parentId);
		dept.setParentsIds(parentsIds);
		dept.getUpStreamLatencyMap().put(parentId, 4.0); // latency of connection between gateways and proxy server is 4 ms

		// Adding mobiles to the physical topology
		// Smartphones have been modeled as fog devices as well
		for(int i=0; i < N_MOBILES_PER_DEPT; i++){
			String mobileId = id+"-"+i;
			FogDevice mobile = addMobile(mobileId, userId, appId, dept.getId());
			mobile.getUpStreamLatencyMap().put(dept.getId(), 2.0); // latency of connection between the smartphone and proxy server is 4 ms
			fogDevices.add(mobile);
		}
		return dept;
	}
	
	private static FogDevice addMobile(String id, int userId, String appId, int parentId){
		List<Integer> parentsIds = new ArrayList<Integer>();
		
		FogDevice mobile = createFogDevice(END_DEVICE_NAME + "-" + id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
		parentsIds.add(parentId);
		mobile.setParentsIds(parentsIds);
		
		// Inter-transmission time of EEG sensor follows a deterministic distribution
		Sensor eegSensor = new Sensor("SENSOR-"+id, "EEG", userId, appId, new DeterministicDistribution(EEG_TRANSMISSION_TIME));
		sensors.add(eegSensor);
		
		Actuator display = new Actuator("ACTUATOR-"+id, userId, appId, "DISPLAY");
		actuators.add(display);
		
		eegSensor.setGatewayDeviceId(mobile.getId());
		eegSensor.setLatency(6.0);  // latency of connection between EEG sensors and the parent Smartphone is 6 ms
		display.setGatewayDeviceId(mobile.getId());
		display.setLatency(1.0);  // latency of connection between Display actuator and the parent Smartphone is 1 ms
		return mobile;
	}
	
	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		// Create PEs and add these into a list.
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips))); // need to store Pe id and MIPS Rating

		PowerHost host = new PowerHost(
			FogUtils.generateEntityId(), //hostId
			new RamProvisionerSimple(ram),
			new BwProvisionerOverbooking(10000), //bw
			1000000, // host storage
			peList,
			new StreamOperatorScheduler(peList),
			new FogLinearPowerModel(busyPower, idlePower)
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
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, ratePerMips);
			fogdevice.setRatePerMips(0.001);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return fogdevice;
	}
}
