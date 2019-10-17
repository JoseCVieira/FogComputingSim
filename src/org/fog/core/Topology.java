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
import org.fog.entities.Client;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.entities.Tuple;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.test.ApplicationsExample;
import org.fog.utils.FogLinearPowerModel;
import org.fog.utils.FogUtils;
import org.fog.utils.movement.Movement;

/**
 * Class which defines the inputs needed to run the FogComputingSim.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public abstract class Topology {
	/** List of all applications needed to be deployed */
	protected static List<Application> applications = new ArrayList<Application>();
	
	/** List of all fog nodes within the physical topology */
	protected static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	
	/** List of all actuators within the physical topology */
	protected static List<Actuator> actuators = new ArrayList<Actuator>();
	
	/** List of all sensors within the physical topology */
	protected static List<Sensor> sensors = new ArrayList<Sensor>();
	
	/** Map containing the key equal to the name of the client device and a value with the list of names of the applications which he want to deploy */
	protected static Map<String, LinkedHashSet<String>> appToFogMap = new HashMap<String, LinkedHashSet<String>>();
	
	protected abstract void createFogDevices();
	protected abstract void createClients();
	
	/**
	 * Create a new empty topology.
	 */
	public Topology() { }
	
	/**
	 * Create a topology and print it's name.
	 * 
	 * @param toPrint
	 */
	public Topology(String toPrint) {
		if(Config.PRINT_DETAILS)
			System.out.println(toPrint);
		
		ApplicationsExample.createExampleApplications();
		createFogDevices();
		createClients();
		deployApplications();
	}
	
	/**
	 * Create a new fog device.
	 * 
	 * @param name the name of the node
	 * @param mips the processing resource units available at the node
	 * @param ram the memory resource units available at the node
	 * @param strg the storage resource units available at the node
	 * @param bPw the busy power value (power consumption while using the full processing capacity of the node)
	 * @param iPw the idle power value (power consumption while using no processing resources at the node)
	 * @param costPerMips the monetary cost [€] of processing resources usage
	 * @param costPerMem the monetary cost [€] of memory usage
	 * @param costPerStorage the monetary cost [€] of storage usage
	 * @param costPerBw the monetary cost [€] of bandwidth usage in at any link which the node is the source
	 * @param costPerEnergy the monetary cost [€] of energy spent at the node
	 * @param movement the movement of the node
	 * @return the fog device
	 */
	protected static FogDevice createFogDevice(String name, double mips, int ram, long strg, double bPw, double iPw, double costPerMips,
			double costPerMem, double costPerStorage, double costPerBw, double costPerEnergy, Movement movement) {
		List<Pe> processingElementsList = new ArrayList<Pe>();
		processingElementsList.add(new Pe(0, new PeProvisioner(mips)));

		PowerHost host = new PowerHost(
				FogUtils.generateEntityId(),
				new RamProvisioner(ram),
				new BwProvisioner(Long.MAX_VALUE),
				strg,
				processingElementsList,
				new VmSchedulerTimeSharedOverbookingEnergy(processingElementsList),
				new FogLinearPowerModel(bPw, iPw)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(Constants.FOG_DEVICE_ARCH,
				Constants.FOG_DEVICE_OS, Constants.FOG_DEVICE_VMM, host, Constants.FOG_DEVICE_TIMEZONE,
				costPerMips, costPerMem, costPerStorage, costPerBw, costPerEnergy);
		
		try {
			return new FogDevice(name, characteristics, new AppModuleAllocationPolicy(hostList), new LinkedList<Storage>(),
					Constants.SCHEDULING_INTERVAL, movement);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Create a new client device.
	 * 
	 * @param name the name of the node
	 * @param mips the processing resource units available at the node
	 * @param ram the memory resource units available at the node
	 * @param strg the storage resource units available at the node
	 * @param bw the network resource units available at the node
	 * @param movement the movement of the node
	 * @return the client device
	 */
	protected static FogDevice createClientDevice(String name, double mips, int ram, long strg, Movement movement) {
		List<Pe> processingElementsList = new ArrayList<Pe>();
		processingElementsList.add(new Pe(0, new PeProvisioner(mips)));

		PowerHost host = new PowerHost(
				FogUtils.generateEntityId(),
				new RamProvisioner(ram),
				new BwProvisioner(Long.MAX_VALUE),
				strg,
				processingElementsList,
				new VmSchedulerTimeSharedOverbookingEnergy(processingElementsList),
				new FogLinearPowerModel(0, 0)
			);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(Constants.FOG_DEVICE_ARCH,
				Constants.FOG_DEVICE_OS, Constants.FOG_DEVICE_VMM, host, Constants.FOG_DEVICE_TIMEZONE,
				0, 0, 0, 0, 0);
		
		try {
			return new Client(name, characteristics, new AppModuleAllocationPolicy(hostList), new LinkedList<Storage>(),
					Constants.SCHEDULING_INTERVAL, movement);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}
	
	/**
	 * Creats a connection (link) between two nodes.
	 * 
	 * @param fog1 the first node
	 * @param fog2 the second node
	 * @param latUp the latency from the first to the second node
	 * @param latDown the latency from the second to the first node
	 * @param bwUp the bandwidth from the first to the second node
	 * @param bwDown the bandwidth from the second to the first node
	 */
	protected static void connectFogDevices(FogDevice fog1, FogDevice fog2, double latUp, double latDown, double bwUp, double bwDown) {
		fog1.getLatencyMap().put(fog2.getId(), latUp);
		fog2.getLatencyMap().put(fog1.getId(), latDown);
		
		fog1.getBandwidthMap().put(fog2.getId(), bwUp);
		fog2.getBandwidthMap().put(fog1.getId(), bwDown);
		
		fog1.getTupleQueue().put(fog2.getId(), new LinkedList<Pair<Tuple, Integer>>());
		fog2.getTupleQueue().put(fog1.getId(), new LinkedList<Pair<Tuple, Integer>>());
		
		fog1.getTupleLinkBusy().put(fog2.getId(), false);
		fog2.getTupleLinkBusy().put(fog1.getId(), false);
	}
	
	/**
	 * Adds the applications to the list of applications and assert them to the respective nodes.
	 */
	protected void deployApplications() {
		List<AppModule> globalModules = new ArrayList<AppModule>();
		
		for(String fogName : appToFogMap.keySet()) {
			FogDevice fogDevice = getFogDeviceByName(fogName);
			
			for(String appName : appToFogMap.get(fogDevice.getName())) {
				Application appToDeploy = ApplicationsExample.getAppExampleByName(appName);
				Application application = getApplicationByName(appName);
				
				if(application == null) {
					application = new Application(appName);
					applications.add(application);
				}
				
				// Terminate if there are repeated either modules or edges with repeated names
				if(!isValid(appToDeploy, fogDevice))
					FogComputingSim.err("It is mandatory that both applications' modules and edges running inside each client to have unique names");
				
				for(AppModule appModule : appToDeploy.getModules()) {
					
					// Global module
					if(getModuleByName(application, appModule.getName()) == null) {
						application.addAppModule(appModule, fogDevice.getId());
					}
						
					if(appModule.isGlobalModule() && !globalModules.contains(appModule)) {
						globalModules.add(appModule);
					}
				}
				
				// Add the applications edges
				for(AppEdge appEdge : appToDeploy.getEdges()) {
					application.addAppEdge(appEdge, globalModules, fogDevice.getId());
				}
				
				// Add the application modules
				for(AppModule appModule : appToDeploy.getModules()) {
					for(Pair<String, String> pair : appModule.getSelectivityMap().keySet()) {
						FractionalSelectivity fractionalSelectivity = ((FractionalSelectivity)appModule.getSelectivityMap().get(pair));
						application.addTupleMapping(appModule.getName(), pair, fractionalSelectivity.getSelectivity(), fogDevice.getId());
					}
				}
				
				// Add the application loops
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
					loops.add(new AppLoop(l, loop.getDeadline()));
				}
				
				if(application.getLoops() == null)
					application.setLoops(loops);
				else
					application.getLoops().addAll(loops);
			}
		}
	}
	
	/**
	 * Checks if any module or edge has a repeated name.  Inside the same client, modules need to have unique name as well as edges.
	 * 
	 * @param appToDeploy the application to be deployed
	 * @param fogDevice the fog device
	 * @return true if there are no repeated names related to modules or edges, otherwise false
	 */
	private boolean isValid(Application appToDeploy, FogDevice fogDevice) {
		for(Application app : applications) {
			for(AppModule appModule : appToDeploy.getModules()) {
				
				// Do not deploy application if there are repeated module's names inside the same client
				if(getModuleByName(app, appModule.getName() + "_"  + fogDevice.getId()) != null) {
					return false;
				}
			}
			
			for(AppEdge appEdge : appToDeploy.getEdges()) {
				String source = appEdge.getSource() + "_"  + fogDevice.getId();
				String dest = appEdge.getDestination()  + "_"  + fogDevice.getId();
				String type = appEdge.getTupleType() + "_"  + fogDevice.getId();
				
				for(AppEdge edge : app.getEdges()) {
					if(source.equals(edge.getSource()) || dest.equals(edge.getDestination()) || type.equals(edge.getTupleType())) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	/**
	 * Gets a module by it's name.
	 * 
	 * @param application the applications name
	 * @param name the name of the application module
	 * @return the module; can be null
	 */
	protected static AppModule getModuleByName(Application application, String name) {
		for(AppModule appModule : application.getModules())
			if(appModule.getName().equals(name))
				return appModule;
		return null;
	}
	
	/**
	 * Gets an application by it's name.
	 * 
	 * @param name the name of the application
	 * @return the application; can be null
	 */
	protected static Application getApplicationByName(String name) {
		for(Application app : applications)
			if(app.getAppId().equals(name))
				return app;
		return null;
	}
	
	
	/**
	 * Gets a fog device by it's name.
	 * 
	 * @param name the name of the fog device
	 * @return the fog device; can be null
	 */
	protected static FogDevice getFogDeviceByName(String name) {
		for(FogDevice fogDevice : fogDevices)
			if(fogDevice.getName().equals(name))
				return fogDevice;
		return null;
	}
	
	/**
	 * Gets the list of applications.
	 * 
	 * @return the list of applications
	 */
	public List<Application> getApplications() {
		return applications;
	}
	
	
	/**
	 * Gets the list of fog devices.
	 * 
	 * @return the list of fog devices
	 */
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}
	
	
	/**
	 * Gets the list of actuators.
	 * 
	 * @return the list of actuators
	 */
	public List<Actuator> getActuators() {
		return actuators;
	}
	
	
	/**
	 * Gets the list of sensors.
	 * 
	 * @return the list os sensors
	 */
	public List<Sensor> getSensors() {
		return sensors;
	}
	
}
