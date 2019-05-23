package org.fog.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

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
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.gui.GuiConfig;
import org.fog.placement.Controller;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.distribution.Distribution;

public abstract class FogTest {
	protected static List<Application> exampleApplications = new ArrayList<Application>();
	protected static List<Application> applications = new ArrayList<Application>();
	protected static List<FogBroker> fogBrokers = new ArrayList<FogBroker>();
	protected static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	protected static List<Actuator> actuators = new ArrayList<Actuator>();
	protected static List<Sensor> sensors = new ArrayList<Sensor>();
	protected static Map<String, List<String>> appToFogMap = new HashMap<String, List<String>>();
	protected static Controller controller = null;
	
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
	
	protected static void createClient(FogDevice fogDevice, String sensorName, Distribution distribution, double sensorLat,
			String actuatorName, double actuatorLat) {
		
		FogBroker broker = null;
		try {
			broker = new FogBroker(fogDevice.getName());
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Unwanted errors happen\nFogComputingSim will terminate abruptally.\n");
			System.exit(0);
		}
		
		int appIndex = new Random().nextInt(exampleApplications.size());
		int gatewayDeviceId = fogDevice.getId();
		String clientName = fogDevice.getName();
		int userId = broker.getId();
		
		String appName = exampleApplications.get(appIndex).getAppId();
		String sensorType = "", actuatorType = "";
		
		for(AppEdge appEdge : exampleApplications.get(appIndex).getEdges()) {
			if(appEdge.getEdgeType() == AppEdge.SENSOR)
				sensorType = appEdge.getSource();
			else if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
				actuatorType = appEdge.getDestination();
		}
		
		sensors.add(new Sensor(sensorName + clientName, sensorType + "_" + userId, userId, appName,
				distribution, gatewayDeviceId, sensorLat));

		actuators.add(new Actuator(actuatorName + clientName, userId, appName,
				gatewayDeviceId, actuatorLat, actuatorType + "_" + userId));
		
		if(!appToFogMap.containsKey(fogDevice.getName())) {
			List<String> appList = new ArrayList<String>();
			appList.add(appName);
			appToFogMap.put(fogDevice.getName(), appList);
		}else {
			appToFogMap.get(fogDevice.getName()).add(appName);
		}
		
		fogBrokers.add(broker);
	}
	
	protected static void createApplications() {
		for(FogDevice fogDevice : fogDevices) {
			FogBroker broker = getFogBrokerByName(fogDevice.getName());
			
			if(appToFogMap.containsKey(fogDevice.getName())) {
				for(String app : appToFogMap.get(fogDevice.getName())) {
					Application application = createApplication(app, broker.getId());
					applications.add(application);
				}
			}
		}
	}
	
	private static Application createApplication(String appId, int userId) {
		Application appExample = null;
		
		for(Application app : exampleApplications) {
			if(app.getAppId().equals(appId)) {
				appExample = app;
			}
		}
		
		if(appExample == null) return null;
		
		Application application = new Application(appId + "_" + userId, userId);
		
		List<AppModule> globalModules = new ArrayList<AppModule>();
		
		for(AppModule appModule : appExample.getModules()) {
			if(!appModule.isGlobalModule()) {
				application.addAppModule(appModule);
				continue;
				
			// If it is a global module verify if is already in other application
			}else {
				globalModules.add(appModule);
				
				boolean found = false;
				for(Application app : applications) {
					if(app.getModuleByName(appModule.getName()) != null) {
						found = true;
					}
				}
				
				if(!found) {
					application.addAppModule(appModule);
				}
			}
		}
		
		System.out.println();
		
		for(AppEdge appEdge : appExample.getEdges()) {
			application.addAppEdge(appEdge, globalModules);
		}
		
		System.out.println("\n\n");
		
		for(AppModule appModule : appExample.getModules()) {
			for(Pair<String, String> pair : appModule.getSelectivityMap().keySet()) {
				FractionalSelectivity fractionalSelectivity = ((FractionalSelectivity)appModule.getSelectivityMap().get(pair));
				application.addTupleMapping(appModule.getName(), pair, fractionalSelectivity.getSelectivity());
			}
		}
		
		List<AppLoop> loops = new ArrayList<AppLoop>();
		for(AppLoop loop : appExample.getLoops()) {
			ArrayList<String> l = new ArrayList<String>();
			for(String name : loop.getModules())
				l.add(name + "_" + userId);
			loops.add(new AppLoop(l));
		}
		
		application.setLoops(loops);
		return application;
	}
	
	protected static void createController() {
		controller = new Controller("master-controller", fogDevices, sensors, actuators);
		
		for(FogDevice fogDevice : fogDevices)
			fogDevice.setController(controller);
	}
	
	protected static FogBroker getFogBrokerByName(String name) {
		for(FogBroker fogBroker : fogBrokers)
			if(fogBroker.getName().equals(name))
				return fogBroker;
		return null;
	}
	
	public List<Application> getApplications() {
		return applications;
	}
	
	public List<FogBroker> getFogBrokers() {
		return fogBrokers;
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
	
	public Map<String, List<String>> getAppToFogMap() {
		return appToFogMap;
	}
}