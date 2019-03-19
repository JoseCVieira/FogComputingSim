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
import org.fog.placement.ModulePlacementMapping;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.DeterministicDistribution;

public class TwoApps {
	static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	static List<FogDevice> mobiles = new ArrayList<FogDevice>();
	static List<Sensor> sensors = new ArrayList<Sensor>();
	static List<Actuator> actuators = new ArrayList<Actuator>();
	
	static int numOfDepts = 1;
	static int numOfMobilesPerDept = 4;
	static double EEG_TRANSMISSION_TIME = 5.1;
	
	public static void main(String[] args) {

		Log.printLine("Starting TwoApps...");

		try {
			Log.disable();
			int num_user = 1;
			Calendar calendar = Calendar.getInstance();
			boolean trace_flag = false;

			CloudSim.init(num_user, calendar, trace_flag);

			String appId0 = "vr_game_0";
			String appId1 = "vr_game_1";
			
			FogBroker broker0 = new FogBroker("broker_0");
			FogBroker broker1 = new FogBroker("broker_1");
			
			
			Application application0 = createApplication0(appId0, broker0.getId());
			Application application1 = createApplication1(appId1, broker1.getId());
			application0.setUserId(broker0.getId());
			application1.setUserId(broker1.getId());
			
			createFogDevices();
			
			createEdgeDevices0(broker0.getId(), appId0);
			createEdgeDevices1(broker1.getId(), appId1);
			
			ModuleMapping moduleMapping_0 = ModuleMapping.createModuleMapping();
			ModuleMapping moduleMapping_1 = ModuleMapping.createModuleMapping();
			
			moduleMapping_0.addModuleToDevice("connector", "cloud");
			moduleMapping_0.addModuleToDevice("concentration_calculator", "cloud");
			moduleMapping_1.addModuleToDevice("connector_1", "cloud");
			moduleMapping_1.addModuleToDevice("concentration_calculator_1", "cloud");
			for(FogDevice device : fogDevices){
				if(device.getName().startsWith("m")){
					moduleMapping_0.addModuleToDevice("client", device.getName());
					moduleMapping_1.addModuleToDevice("client_1", device.getName());
				}
			}
			
			Controller controller = new Controller("master-controller", fogDevices, sensors, 
					actuators);
			
			for(FogDevice fogDevice : fogDevices)
				fogDevice.setController(controller);
			
			controller.submitApplication(application0, new ModulePlacementMapping(fogDevices, application0, moduleMapping_0));
			controller.submitApplication(application1, 1000, new ModulePlacementMapping(fogDevices, application1, moduleMapping_1));

			TimeKeeper.getInstance().setSimulationStartTime(Calendar.getInstance().getTimeInMillis());

			CloudSim.startSimulation();

			CloudSim.stopSimulation();

			Log.printLine("VRGame finished!");
		} catch (Exception e) {
			e.printStackTrace();
			Log.printLine("Unwanted errors happen");
		}
	}

	private static void createEdgeDevices0(int userId, String appId) {
		for(FogDevice mobile : mobiles){
			String id = mobile.getName();
			Sensor eegSensor = new Sensor("s-"+appId+"-"+id, "EEG", userId, appId, new DeterministicDistribution(EEG_TRANSMISSION_TIME));
			sensors.add(eegSensor);
			Actuator display = new Actuator("a-"+appId+"-"+id, userId, appId, "DISPLAY");
			actuators.add(display);
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0);
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0);
		}
	}
	
	private static void createEdgeDevices1(int userId, String appId) {
		for(FogDevice mobile : mobiles){
			String id = mobile.getName();
			
			Sensor eegSensor = new Sensor("s-"+appId+"-"+id, "EEG_1", userId, appId, new DeterministicDistribution(EEG_TRANSMISSION_TIME));
			sensors.add(eegSensor);
			
			Actuator display = new Actuator("a-"+appId+"-"+id, userId, appId, "DISPLAY_1");
			actuators.add(display);
			
			eegSensor.setGatewayDeviceId(mobile.getId());
			eegSensor.setLatency(6.0);
			display.setGatewayDeviceId(mobile.getId());
			display.setLatency(1.0);
		}
	}

	private static void createFogDevices() {
		FogDevice cloud = createFogDevice("cloud", 44800, 40000, 100, 10000, 0, 0.01, 16*103, 16*83.25);
		cloud.getParentsIds().add(-1);
		
		FogDevice proxy = createFogDevice("proxy-server", 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		proxy.getParentsIds().add(cloud.getId());
		proxy.getUpStreamLatencyMap().put(cloud.getId(), 100.0);
		
		fogDevices.add(cloud);
		fogDevices.add(proxy);
		
		for(int i=0;i<numOfDepts;i++)
			addGw(i+"", proxy.getId());
	}

	private static FogDevice addGw(String id, int parentId){
		FogDevice dept = createFogDevice("d-"+id, 2800, 4000, 10000, 10000, 1, 0.0, 107.339, 83.4333);
		fogDevices.add(dept);
		dept.getParentsIds().add(parentId);
		dept.getUpStreamLatencyMap().put(parentId, 4.0);
		for(int i=0;i<numOfMobilesPerDept;i++){
			String mobileId = id+"-"+i;
			FogDevice mobile = addMobile(mobileId, dept.getId());
			
			mobile.getUpStreamLatencyMap().put(dept.getId(), 2.0);
			fogDevices.add(mobile);
		}
		return dept;
	}
	
	private static FogDevice addMobile(String id, int parentId){
		FogDevice mobile = createFogDevice("m-"+id, 1000, 1000, 10000, 270, 3, 0, 87.53, 82.44);
		mobile.getParentsIds().add(parentId);
		mobiles.add(mobile);
		return mobile;
	}

	private static FogDevice createFogDevice(String nodeName, long mips,
			int ram, long upBw, long downBw, int level, double ratePerMips, double busyPower, double idlePower) {
		
		List<Pe> peList = new ArrayList<Pe>();

		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000;
		int bw = 10000;

		PowerHost host = new PowerHost(
				hostId,
				new RamProvisionerSimple(ram),
				new BwProvisionerOverbooking(bw),
				storage,
				peList,
				new StreamOperatorScheduler(peList),
				new FogLinearPowerModel(busyPower, idlePower)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		String arch = "x86";
		String os = "Linux";
		String vmm = "Xen";
		double time_zone = 10.0;
		double cost = 3.0;
		double costPerMem = 0.05;
		double costPerStorage = 0.001;
		double costPerBw = 0.0;
		LinkedList<Storage> storageList = new LinkedList<Storage>();

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(
				arch, os, vmm, host, time_zone, cost, costPerMem,
				costPerStorage, costPerBw);

		FogDevice fogdevice = null;
		try {
			fogdevice = new FogDevice(nodeName, characteristics, 
					new AppModuleAllocationPolicy(hostList), storageList, 10, upBw, downBw, ratePerMips);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return fogdevice;
	}

	@SuppressWarnings({"serial" })
	private static Application createApplication0(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId);

		application.addAppModule("client", 10);
		application.addAppModule("concentration_calculator", 10);
		application.addAppModule("connector", 10);

		application.addAppEdge("EEG", "client", 3000, 500, "EEG", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client", "concentration_calculator", 3500, 500, "_SENSOR", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("concentration_calculator", "connector", 100, 1000, 1000, "PLAYER_GAME_STATE", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("concentration_calculator", "client", 14, 500, "CONCENTRATION", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("connector", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", Tuple.DOWN, AppEdge.ACTUATOR);

		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9));
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0));
		application.addTupleMapping("concentration_calculator", "_SENSOR", "CONCENTRATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0));

		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("EEG");add("client");add("concentration_calculator");add("client");add("DISPLAY");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		
		return application;
	}
	
	@SuppressWarnings({"serial" })
	private static Application createApplication1(String appId, int userId){
		
		Application application = Application.createApplication(appId, userId); 

		application.addAppModule("client_1", 10);
		application.addAppModule("concentration_calculator_1", 10);
		application.addAppModule("connector_1", 10);

		application.addAppEdge("EEG_1", "client_1", 3000, 500, "EEG_1", Tuple.UP, AppEdge.SENSOR);
		application.addAppEdge("client_1", "concentration_calculator_1", 3500, 500, "_SENSOR_1", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("concentration_calculator_1", "connector_1", 100, 1000, 1000, "PLAYER_GAME_STATE_1", Tuple.UP, AppEdge.MODULE);
		application.addAppEdge("concentration_calculator_1", "client_1", 14, 500, "CONCENTRATION_1", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("connector_1", "client_1", 100, 28, 1000, "GLOBAL_GAME_STATE_1", Tuple.DOWN, AppEdge.MODULE);
		application.addAppEdge("client_1", "DISPLAY_1", 1000, 500, "SELF_STATE_UPDATE_1", Tuple.DOWN, AppEdge.ACTUATOR);
		application.addAppEdge("client_1", "DISPLAY_1", 1000, 500, "GLOBAL_STATE_UPDATE_1", Tuple.DOWN, AppEdge.ACTUATOR);

		application.addTupleMapping("client_1", "EEG_1", "_SENSOR_1", new FractionalSelectivity(0.9));
		application.addTupleMapping("client_1", "CONCENTRATION_1", "SELF_STATE_UPDATE_1", new FractionalSelectivity(1.0));
		application.addTupleMapping("concentration_calculator_1", "_SENSOR_1", "CONCENTRATION_1", new FractionalSelectivity(1.0));
		application.addTupleMapping("client_1", "GLOBAL_GAME_STATE_1", "GLOBAL_STATE_UPDATE_1", new FractionalSelectivity(1.0));

		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("EEG_1");add("client_1");add("concentration_calculator_1");add("client_1");add("DISPLAY_1");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		
		return application;
	}
}
