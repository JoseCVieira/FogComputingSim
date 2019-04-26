package org.fog.test;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.core.CloudSim;
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
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.placement.Controller;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.utils.Config;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.Util;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;

public class RandomTopology {	
	private static final String CLOUD_NAME = "Cloud";
	private static final int NR_FOG_DEVICES = 5;
	
	private static final int MAX_CONN_LAT = 100;
	private static final int MAX_CONN_BW = 10000;
	
	private static final double CONNECTION_PROB = 0.4;
	private static final double DEPLOY_APP_PROB = 0.35;
	
	private static final double RESOURCES_DEV = 100;
	private static final double ENERGY_DEV = 5;
	private static final double COST_DEV = 1E-5;
	
	private static List<Application> examplesApplications = new ArrayList<Application>();
	private static List<Application> applications = new ArrayList<Application>();
	private static List<FogBroker> fogBrokers = new ArrayList<FogBroker>();
	private static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	private static List<Actuator> actuators = new ArrayList<Actuator>();
	private static List<Sensor> sensors = new ArrayList<Sensor>();
	private static Controller controller;
	
	public static void main(String[] args) {
		System.out.println("Generating a new random topology...");
		
		Log.disable();
		CloudSim.init(Calendar.getInstance());
		
		createExampleApplications();
		createFogDevices();
		connectFogDevices();
		createClients();
		createController();
		createApplications();
		
		new FogComputingSim(applications, fogBrokers, fogDevices, actuators, sensors, controller);
	}
	
	private static void createFogDevices() {
		FogDevice cloud = createFogDevice(CLOUD_NAME, Short.MAX_VALUE, Short.MAX_VALUE, Short.MAX_VALUE, Short.MAX_VALUE,
				16*Config.BUSY_POWER, 16*Config.IDLE_POWER, Config.COST_PER_SEC, Config.RATE_MIPS, Config.RATE_RAM,
				Config.RATE_MEM, Config.RATE_BW);
		
		fogDevices.add(cloud);
		
		int iter = 1;
		int nrFogNodes = NR_FOG_DEVICES - 1;
		
		while(nrFogNodes > 0) {
			int nr = Util.rand(0, nrFogNodes);
			nrFogNodes -= nr;
			
			for(int i = 0; i < nr; i++) {
				double mips = Util.normalRand(Config.MIPS/iter, RESOURCES_DEV/iter);
				double ram = Util.normalRand(Config.RAM/iter, RESOURCES_DEV/iter);
				double strg = Util.normalRand(Config.MEM/iter, RESOURCES_DEV/iter);
				double bw = Util.normalRand(Config.BW/iter, RESOURCES_DEV/iter);
				
				double bPw = Util.normalRand(Config.BUSY_POWER, ENERGY_DEV);
				double iPw = Util.normalRand(Config.IDLE_POWER, ENERGY_DEV);
				
				double rateMips = Util.normalRand(Config.RATE_MIPS, COST_DEV);
				double rateRam = Util.normalRand(Config.RATE_RAM, COST_DEV);
				double rateStrg = Util.normalRand(Config.RATE_MEM, COST_DEV);
				double rateBw = Util.normalRand(Config.RATE_BW, COST_DEV);
				
				FogDevice fogDevice = createFogDevice("L"+iter+":F"+i, mips, (int) ram, (long) strg, (long) bw, bPw, iPw,
						Config.COST_PER_SEC, rateMips, rateRam, rateStrg, rateBw);
				
				fogDevices.add(fogDevice);			
			}
			
			iter++;
		}
	}
	
	private static FogDevice createFogDevice(String name, double mips, int ram, long strg, long bw, double bPw,
			double iPw, double costPerSec, double costPerMips, double costPerMem, double costPerStorage, double costPerBw) {
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

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(Config.FOG_DEVICE_ARCH,
				Config.FOG_DEVICE_OS, Config.FOG_DEVICE_VMM, host, Config.FOG_DEVICE_TIMEZONE,
				costPerSec, costPerMips, costPerMem, costPerStorage, costPerBw);
		
		try {
			return new FogDevice(name, characteristics, new AppModuleAllocationPolicy(hostList),
					new LinkedList<Storage>(), Config.SCHEDULING_INTERVAL);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private static void connectFogDevices() {
		List<FogDevice> notConnctedDevices = new ArrayList<FogDevice>();
		List<FogDevice> connctedDevices = new ArrayList<FogDevice>();
		
		notConnctedDevices.addAll(fogDevices);
		connctedDevices.add(notConnctedDevices.get(0));
		notConnctedDevices.remove(0);
		
		while(!notConnctedDevices.isEmpty()) {
			FogDevice fogDevice = connctedDevices.get(new Random().nextInt(connctedDevices.size()));
			List<FogDevice> toRemove = new ArrayList<FogDevice>();
			
			for(FogDevice f : notConnctedDevices) {
				if(new Random().nextFloat() < CONNECTION_PROB) {
					toRemove.add(f);
					
					fogDevice.getNeighborsIds().add(f.getId());
					f.getNeighborsIds().add(fogDevice.getId());
					
					fogDevice.getLatencyMap().put(f.getId(), (double) Util.rand(MAX_CONN_LAT/3, MAX_CONN_LAT));
					f.getLatencyMap().put(fogDevice.getId(), (double) Util.rand(MAX_CONN_LAT/3, MAX_CONN_LAT));
					
					fogDevice.getBandwidthMap().put(f.getId(), (double) Util.rand(MAX_CONN_BW/3, MAX_CONN_BW));
					f.getBandwidthMap().put(fogDevice.getId(), (double) Util.rand(MAX_CONN_BW/3, MAX_CONN_BW));
					
					fogDevice.getTupleQueue().put(f.getId(), new LinkedList<Pair<Tuple, Integer>>());
					f.getTupleQueue().put(fogDevice.getId(), new LinkedList<Pair<Tuple, Integer>>());
					
					fogDevice.getTupleLinkBusy().put(f.getId(), false);
					f.getTupleLinkBusy().put(fogDevice.getId(), false);
				}
			}
			
			for(FogDevice f : toRemove) {
				notConnctedDevices.remove(f);
				connctedDevices.add(f);
			}
		}
	}
	
	private static void createClients() {
		int nrApps = 0;
		
		while(nrApps == 0) {
			for(FogDevice fogDevice : fogDevices) {
				if(fogDevice.getName().equals(CLOUD_NAME)) continue;
				
				if(new Random().nextFloat() < DEPLOY_APP_PROB) {
					nrApps++;
					
					FogBroker broker = null;
					try {
						broker = new FogBroker(fogDevice.getName());
					} catch (Exception e) {
						e.printStackTrace();
						System.err.println("Unwanted errors happen\nFogComputingSim will terminate abruptally.\n");
						System.exit(0);
					}
					
					int appIndex = new Random().nextInt(examplesApplications.size());
					int gatewayDeviceId = fogDevice.getId();
					String clientName = fogDevice.getName();
					int userId = broker.getId();
					
					String appName = examplesApplications.get(appIndex).getAppId();
					String sensorType = "", actuatorType = "";
					
					for(AppEdge appEdge : examplesApplications.get(appIndex).getEdges()) {
						if(appEdge.getEdgeType() == AppEdge.SENSOR)
							sensorType = appEdge.getSource();
						else if(appEdge.getEdgeType() == AppEdge.ACTUATOR)
							actuatorType = appEdge.getDestination();
					}
					
					Distribution sensorDist = new DeterministicDistribution(Util.normalRand(Config.SENSOR_DESTRIBUTION, 1.0)); //TODO: test other distributions
					double sensorLat = Util.normalRand(Config.SENSOR_LATENCY, 1);
					double actuatorLat = Util.normalRand(Config.ACTUATOR_LATENCY, 0.1);
					
					sensors.add(new Sensor("Sensor:" + clientName, sensorType + "_" + userId, userId, appName + "_" + userId,
							sensorDist, gatewayDeviceId, sensorLat));
	
					actuators.add(new Actuator("Actuator:" + clientName, userId, appName + "_" + userId,
							gatewayDeviceId, actuatorLat, actuatorType + "_" + userId));
					
					fogDevice.getActiveApplications().add(appName);
					fogBrokers.add(broker);
				}
			}
		}
	}
	
	private static void createController() {
		controller = new Controller("master-controller", fogDevices, sensors, actuators);
		
		for(FogDevice fogDevice : fogDevices)
			fogDevice.setController(controller);
	}
	
	@SuppressWarnings("serial")
	private static void createExampleApplications() {		
		Application application = new Application("VRGame", -1);
		application.addAppModule("client", 100);
		application.addAppModule("calculator", 100);
		application.addAppModule("connector", 100);
		
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
		examplesApplications.add(application);
		
		application = new Application("DCNS", -1);
		application.addAppModule("object_detector", 100);
		application.addAppModule("motion_detector", 100);
		application.addAppModule("object_tracker", 100);
		application.addAppModule("user_interface", 100);
		
		application.addAppEdge("CAMERA", "motion_detector", 1000, 20000, "CAMERA", AppEdge.SENSOR);
		application.addAppEdge("motion_detector", "object_detector", 2000, 2000, "MOTION_VIDEO_STREAM", AppEdge.MODULE);
		application.addAppEdge("object_detector", "user_interface", 500, 2000, "DETECTED_OBJECT", AppEdge.MODULE);
		application.addAppEdge("object_detector", "object_tracker", 1000, 100, "OBJECT_LOCATION", AppEdge.MODULE);
		application.addAppEdge("object_tracker", "PTZ_CONTROL", 100, 28, 100, "PTZ_PARAMS", AppEdge.ACTUATOR);
		
		application.addTupleMapping("motion_detector", "CAMERA", "MOTION_VIDEO_STREAM", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "OBJECT_LOCATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "DETECTED_OBJECT", new FractionalSelectivity(0.05));
		
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("motion_detector");add("object_detector");add("object_tracker");}});
		final AppLoop loop3 = new AppLoop(new ArrayList<String>(){{add("object_tracker");add("PTZ_CONTROL");}});
		loops = new ArrayList<AppLoop>(){{add(loop2);add(loop3);}};
		examplesApplications.add(application);
		
		application = new Application("TEMP", -1);
		application.addAppModule("client", 100);
		application.addAppModule("classifier", 100);
		application.addAppModule("tuner", 100);
	
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
		
		final AppLoop loop4 = new AppLoop(new ArrayList<String>(){{add("TEMP");add("client");add("classifier");add("client");add("MOTOR");}});
		final AppLoop loop5 = new AppLoop(new ArrayList<String>(){{add("classifier");add("tuner");add("classifier");}});
		loops = new ArrayList<AppLoop>(){{add(loop4);add(loop5);}};
		application.setLoops(loops);
		examplesApplications.add(application);
	}
	
	private static void createApplications() {
		for(FogDevice fogDevice : fogDevices) {
			FogBroker broker = getFogBrokerByName(fogDevice.getName());
			
			for(String app : fogDevice.getActiveApplications()) {
				Application application = createApplication(app, broker.getId());
				application.setClientId(fogDevice.getId());
				applications.add(application);
			}
		}
	}
	
	private static Application createApplication(String appId, int userId) {
		Application appExample = null;
		
		for(Application app : examplesApplications)
			if(app.getAppId().equals(appId))
				appExample = app;
		
		if(appExample == null) return null;
		
		Application application = new Application(appId + "_" + userId, userId);

		for(AppModule appModule : appExample.getModules())
			application.addAppModule(appModule);
		
		for(AppEdge appEdge : appExample.getEdges())
			application.addAppEdge(appEdge);
			
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
	
	private static FogBroker getFogBrokerByName(String name) {
		for(FogBroker fogBroker : fogBrokers)
			if(fogBroker.getName().equals(name))
				return fogBroker;
		return null;
	}
	
}