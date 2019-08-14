package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.core.FogComputingSim;
import org.fog.entities.FogDevice;

/**
 * Class representing the mapping between the modules of one application and the corresponding fog devices where they were deployed.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class ModulePlacement {
	/** List containing all fog devices */
	protected List<FogDevice> fogDevices;
	
	/** Application corresponding to the module placement */
	protected Application application;
	
	/** Module to device map */
	private Map<String, Integer> moduleToDeviceMap;
	
	/** Device to module map */
	private Map<Integer, List<AppModule>> deviceToModuleMap;
	
	/**
	 * Creates a new module placement map.
	 * 
	 * @param fogDevices the list containing all fog devices
	 * @param application the application corresponding to the module placement
	 * @param moduleMapping the map between the names of the fog devices and the corresponding module names
	 */
	public ModulePlacement(List<FogDevice> fogDevices, Application application, Map<String, List<String>> moduleMapping) {
		this.fogDevices = fogDevices;
		this.application = application;
		
		setModuleToDeviceMap(new HashMap<String, Integer>());
		setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		
		mapModules(moduleMapping);
	}
	
	/**
	 * Maps the module names into the corresponding device IDs and creates the instances within the devices.
	 * 
	 * @param moduleMapping the map between the names of the fog devices and the corresponding module names
	 */
	protected void mapModules(Map<String, List<String>> moduleMapping) {
		for(String deviceName : moduleMapping.keySet()) {
			for(String moduleName : moduleMapping.get(deviceName)) {
				
				int deviceId = -1;
				for(FogDevice dev : fogDevices) {
					if(dev.getName().equals(deviceName)) {
						deviceId = dev.getId();
					}
				}
				
				if(deviceId == -1)
					FogComputingSim.err("Should not happen (ModulePlacement)");
				
				createModuleInstanceOnDevice(application.getModuleByName(moduleName), getFogDeviceById(deviceId));
			}
		}
	}
	
	/**
	 * Creates a given application module instance in a given fog device.
	 * 
	 * @param _module the application module deployed
	 * @param device the fog device where the module were deployed
	 */
	protected void createModuleInstanceOnDevice(AppModule _module, final FogDevice device){
		AppModule module = null;
		
		if(getModuleToDeviceMap().containsKey(_module.getName()))
			module = new AppModule(_module);
		else
			module = _module;
		
		if(!getDeviceToModuleMap().containsKey(device.getId()))
			getDeviceToModuleMap().put(device.getId(), new ArrayList<AppModule>());
		getDeviceToModuleMap().get(device.getId()).add(module);

		getModuleToDeviceMap().put(module.getName(), device.getId());
	}
	
	/**
	 * Gets the fog device with a given entity id.
	 * 
	 * @param fogDeviceId the entity id
	 * @return the corresponding fog device
	 */
	protected FogDevice getFogDeviceById(int fogDeviceId){
		return (FogDevice)CloudSim.getEntity(fogDeviceId);
	}
	
	/**
	 * Gets the module to device map.
	 * 
	 * @return the module to device map
	 */
	public Map<String, Integer> getModuleToDeviceMap() {
		return moduleToDeviceMap;
	}
	
	/**
	 * Sets the module to device map.
	 * 
	 * @param moduleToDeviceMap the module to device map
	 */
	public void setModuleToDeviceMap(Map<String, Integer> moduleToDeviceMap) {
		this.moduleToDeviceMap = moduleToDeviceMap;
	}
	
	/**
	 * Gets the the device to module map.
	 * 
	 * @return the the device to module map
	 */
	public Map<Integer, List<AppModule>> getDeviceToModuleMap() {
		return deviceToModuleMap;
	}
	
	/**
	 * Sets the the device to module map.
	 * 
	 * @param deviceToModuleMap the the device to module map
	 */
	public void setDeviceToModuleMap(Map<Integer, List<AppModule>> deviceToModuleMap) {
		this.deviceToModuleMap = deviceToModuleMap;
	}

}
