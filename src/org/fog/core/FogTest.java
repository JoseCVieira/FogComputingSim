package org.fog.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.PeProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;
import org.cloudbus.cloudsim.sdn.overbooking.VmSchedulerTimeSharedOverbookingEnergy;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.gui.GuiConfig;
import org.fog.placement.Controller;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;

public abstract class FogTest {
	protected static List<Application> exampleApplications = new ArrayList<Application>();
	protected static List<Application> applications = new ArrayList<Application>();
	protected static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	protected static List<Actuator> actuators = new ArrayList<Actuator>();
	protected static List<Sensor> sensors = new ArrayList<Sensor>();
	protected static Map<String, LinkedHashSet<String>> appToFogMap = new HashMap<String, LinkedHashSet<String>>();
	protected static Controller controller = null;
	
	protected abstract void createFogDevices();
	protected abstract void createClients();
	
	public FogTest() {
		
	}
	
	public FogTest(String toPrint) {
		System.out.println(toPrint);
		
		createExampleApplications();
		createFogDevices();
		createController();
		createClients();
		deployApplications();
	}
	
	protected static FogDevice createFogDevice(String name, double mips, int ram, long strg, long bw, double bPw,
			double iPw, double costPerMips, double costPerMem, double costPerStorage, double costPerBw) {
		List<Pe> processingElementsList = new ArrayList<Pe>();
		processingElementsList.add(new Pe(0, new PeProvisioner(mips)));

		PowerHost host = new PowerHost(
				FogUtils.generateEntityId(),
				new RamProvisioner(ram),
				new BwProvisioner(bw),
				strg,
				processingElementsList,
				new VmSchedulerTimeSharedOverbookingEnergy(processingElementsList),
				new FogLinearPowerModel(bPw, iPw)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(Constants.FOG_DEVICE_ARCH,
				Constants.FOG_DEVICE_OS, Constants.FOG_DEVICE_VMM, host, Constants.FOG_DEVICE_TIMEZONE,
				GuiConfig.COST_PER_SEC, costPerMips, costPerMem, costPerStorage, costPerBw);
		
		try {
			return new FogDevice(name, characteristics, new AppModuleAllocationPolicy(hostList),
					new LinkedList<Storage>(), Constants.SCHEDULING_INTERVAL);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	protected static void connectFogDevices(FogDevice fog1, FogDevice fog2, double latUp, double latDown, double bwUp, double bwDown) {
		fog1.getNeighborsIds().add(fog2.getId());
		fog2.getNeighborsIds().add(fog1.getId());
		
		fog1.getLatencyMap().put(fog2.getId(), latUp);
		fog2.getLatencyMap().put(fog1.getId(), latDown);
		
		fog1.getBandwidthMap().put(fog2.getId(), bwUp);
		fog2.getBandwidthMap().put(fog1.getId(), bwDown);
		
		fog1.getTupleQueue().put(fog2.getId(), new LinkedList<Pair<Tuple, Integer>>());
		fog2.getTupleQueue().put(fog1.getId(), new LinkedList<Pair<Tuple, Integer>>());
		
		fog1.getTupleLinkBusy().put(fog2.getId(), false);
		fog2.getTupleLinkBusy().put(fog1.getId(), false);
	}
	
	protected static void createController() {
		controller = new Controller("master-controller", fogDevices, sensors, actuators);
		
		for(FogDevice fogDevice : fogDevices)
			fogDevice.setController(controller);
	}
	
	protected void deployApplications() {
		List<AppModule> globalModules = new ArrayList<AppModule>();
		
		for(String fogName : appToFogMap.keySet()) {
			FogDevice fogDevice = getFogDeviceByName(fogName);
			
			for(String appName : appToFogMap.get(fogDevice.getName())) {
				Application appToDeploy = getAppExampleByName(appName);
				Application application = getApplicationByName(appName);
				
				if(application == null) {
					application = new Application(appName);
					applications.add(application);
				}
				
				boolean repeatedApp = false;
				
				// Check if module has a repeated name. Inside the same client, modules need to have unique name. Thus check for repeated modules.
				for(AppModule appModule : appToDeploy.getModules()) {
					if(getModuleByName(application, appModule.getName() + "_"  + fogDevice.getId()) != null) {
						repeatedApp = true;
						break;
					}
				}
				
				// Do not deploy application if there are repeated module's names inside the same client
				if(repeatedApp)
					continue;
				
				for(AppModule appModule : appToDeploy.getModules()) {
					
					// Global module
					if(getModuleByName(application, appModule.getName()) == null) {
						application.addAppModule(appModule, fogDevice.getId());
					}
						
					if(appModule.isGlobalModule() && !globalModules.contains(appModule)) {
						globalModules.add(appModule);
					}
				}
				
				for(AppEdge appEdge : appToDeploy.getEdges()) {
					application.addAppEdge(appEdge, globalModules, fogDevice.getId());
				}
				
				for(AppModule appModule : appToDeploy.getModules()) {
					for(Pair<String, String> pair : appModule.getSelectivityMap().keySet()) {
						FractionalSelectivity fractionalSelectivity = ((FractionalSelectivity)appModule.getSelectivityMap().get(pair));
						application.addTupleMapping(appModule.getName(), pair, fractionalSelectivity.getSelectivity(), fogDevice.getId());
					}
				}
				
				List<AppLoop> loops = new ArrayList<AppLoop>();
				for(AppLoop loop : appToDeploy.getLoops()) {
					ArrayList<String> l = new ArrayList<String>();
					for(String name : loop.getModules()) {
						
						boolean found = false;
						for(AppModule gModule : globalModules) {
							if(gModule.getName().equals(name)) {
								found = true;
								break;
							}
						}
						
						if(found) {
							l.add(name);
						}else {
							l.add(name + "_" + fogDevice.getId());
						}
						
					}
					loops.add(new AppLoop(l));
				}
				
				if(application.getLoops() == null)
					application.setLoops(loops);
				else
					application.getLoops().addAll(loops);
			}
		}
	}
	
	@SuppressWarnings("serial")
	protected void createExampleApplications() {		
		Application application = new Application("VRGame");
		application.addAppModule("client", 100, true, false);
		application.addAppModule("calculator", 100, false, false);
		application.addAppModule("connector", 100, false, false);
		
		application.addAppEdge("EEG", "client", 3000, 500, "EEG", AppEdge.SENSOR);
		application.addAppEdge("client", "calculator", 3500, 500, "_SENSOR", AppEdge.MODULE);
		application.addAppEdge("calculator", "connector", 100, 1000, 1000, "PLAYER_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("calculator", "client", 14, 500, "CONCENTRATION", AppEdge.MODULE);
		application.addAppEdge("connector", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", AppEdge.ACTUATOR);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9));
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0));
		application.addTupleMapping("calculator", "_SENSOR", "CONCENTRATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0));
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("EEG");add("client");add("concentration_calculator");add("client");add("DISPLAY");}});
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		exampleApplications.add(application);
		
		application = new Application("VRGame-MP");
		application.addAppModule("client", 100, true, false);
		application.addAppModule("calculator", 100, false, false);
		application.addAppModule("connector", 100, false, true);
		
		application.addAppEdge("EEG", "client", 3000, 500, "EEG", AppEdge.SENSOR);
		application.addAppEdge("client", "calculator", 3500, 500, "_SENSOR", AppEdge.MODULE);
		application.addAppEdge("calculator", "connector", 100, 1000, 1000, "PLAYER_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("calculator", "client", 14, 500, "CONCENTRATION", AppEdge.MODULE);
		application.addAppEdge("connector", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", AppEdge.ACTUATOR);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9));
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0));
		application.addTupleMapping("calculator", "_SENSOR", "CONCENTRATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0));
		
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("EEG");add("client");add("concentration_calculator");add("client");add("DISPLAY");}});
		loops = new ArrayList<AppLoop>(){{add(loop2);}};
		application.setLoops(loops);
		exampleApplications.add(application);
		
		application = new Application("DCNS");
		application.addAppModule("object_detector", 100, false, false);
		application.addAppModule("motion_detector", 100, true, false);
		application.addAppModule("object_tracker", 100, false, false);
		application.addAppModule("user_interface", 100, false, false);
		
		application.addAppEdge("CAMERA", "motion_detector", 1000, 20000, "CAMERA", AppEdge.SENSOR);
		application.addAppEdge("motion_detector", "object_detector", 2000, 2000, "MOTION_VIDEO_STREAM", AppEdge.MODULE);
		application.addAppEdge("object_detector", "user_interface", 500, 2000, "DETECTED_OBJECT", AppEdge.MODULE);
		application.addAppEdge("object_detector", "object_tracker", 1000, 100, "OBJECT_LOCATION", AppEdge.MODULE);
		application.addAppEdge("object_tracker", "PTZ_CONTROL", 100, 28, 100, "PTZ_PARAMS", AppEdge.ACTUATOR);
		
		application.addTupleMapping("motion_detector", "CAMERA", "MOTION_VIDEO_STREAM", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "OBJECT_LOCATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "DETECTED_OBJECT", new FractionalSelectivity(0.05));
		
		final AppLoop loop3 = new AppLoop(new ArrayList<String>(){{add("motion_detector");add("object_detector");add("object_tracker");}});
		final AppLoop loop4 = new AppLoop(new ArrayList<String>(){{add("object_tracker");add("PTZ_CONTROL");}});
		loops = new ArrayList<AppLoop>(){{add(loop3);add(loop4);}};
		exampleApplications.add(application);
		
		application = new Application("TEMP");
		application.addAppModule("client", 100, false, false);
		application.addAppModule("classifier", 100, false, false);
		application.addAppModule("tuner", 100, false, false);
	
		application.addAppEdge("TEMP", "client", 1000, 100, "TEMP", AppEdge.SENSOR);
		application.addAppEdge("client", "classifier", 8000, 100, "_SENSOR", AppEdge.MODULE);
		application.addAppEdge("classifier", "tuner", 1000000, 100, "HISTORY", AppEdge.MODULE);
		application.addAppEdge("classifier", "client", 1000, 100, "CLASSIFICATION", AppEdge.MODULE);
		application.addAppEdge("tuner", "classifier", 1000, 100, "TUNING_PARAMS", AppEdge.MODULE);
		application.addAppEdge("client", "MOTOR", 1000, 100, "ACTUATOR", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client", "TEMP", "_SENSOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "CLASSIFICATION", "ACTUATOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "CLASSIFICATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "HISTORY", new FractionalSelectivity(0.1));
		application.addTupleMapping("tuner", "HISTORY", "TUNING_PARAMS", new FractionalSelectivity(1.0));
		
		final AppLoop loop5 = new AppLoop(new ArrayList<String>(){{add("TEMP");add("client");add("classifier");add("client");add("MOTOR");}});
		final AppLoop loop6 = new AppLoop(new ArrayList<String>(){{add("classifier");add("tuner");add("classifier");}});
		loops = new ArrayList<AppLoop>(){{add(loop5);add(loop6);}};
		application.setLoops(loops);
		exampleApplications.add(application);
	}
	
	protected static AppModule getModuleByName(Application application, String name) {
		for(AppModule appModule : application.getModules())
			if(appModule.getName().equals(name))
				return appModule;
		return null;
	}
	
	protected static Application getAppExampleByName(String name) {
		for(Application app : exampleApplications)
			if(app.getAppId().equals(name))
				return app;
		return null;
	}
	
	protected static Application getApplicationByName(String name) {
		for(Application app : applications)
			if(app.getAppId().equals(name))
				return app;
		return null;
	}
	
	protected static FogDevice getFogDeviceByName(String name) {
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getName().equals(name))
				return fogDevice;
		return null;
	}
	
	public List<Application> getApplications() {
		return applications;
	}
	
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}
	
	public List<Actuator> getActuators() {
		return actuators;
	}
	
	public List<Sensor> getSensors() {
		return sensors;
	}
	
	public Controller getController() {
		return controller;
	}
	
	public Map<String, LinkedHashSet<String>> getAppToFogMap() {
		return appToFogMap;
	}
}