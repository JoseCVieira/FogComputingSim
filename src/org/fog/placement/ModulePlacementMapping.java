package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;

public class ModulePlacementMapping extends ModulePlacement{
	
	// Stores the current mapping of application modules to fog devices
	private Map<Integer, List<String>> currentModuleMap;
	private ModuleMapping moduleMapping;
	
	public ModulePlacementMapping(List<FogDevice> fogDevices, Application application, ModuleMapping moduleMapping){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		setCurrentModuleMap(new HashMap<Integer, List<String>>());
		setModuleToDeviceMap(new HashMap<String, Integer>());
		setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		
		for(FogDevice dev : getFogDevices()) {
			getCurrentModuleMap().put(dev.getId(), new ArrayList<String>());
		}
		
		mapModules();
	}
	
	@Override
	protected void mapModules() {
		for(String deviceName : getModuleMapping().getModuleMapping().keySet()){
			for(String moduleName : getModuleMapping().getModuleMapping().get(deviceName)){
				
				int deviceId = -1;
				for(FogDevice dev : getFogDevices()) {
					if(dev.getName().equals(deviceName)) {
						deviceId = dev.getId();
					}
				}
				
				getCurrentModuleMap().get(deviceId).add(moduleName);
			}
		}
		
		for(int deviceId : getCurrentModuleMap().keySet()) {
			for(String module : getCurrentModuleMap().get(deviceId)) {
				createModuleInstanceOnDevice(getApplication().getModuleByName(module), getFogDeviceById(deviceId));
			}
		}
	}
	
	public ModuleMapping getModuleMapping() {
		return moduleMapping;
	}

	public void setModuleMapping(ModuleMapping moduleMapping) {
		this.moduleMapping = moduleMapping;
	}

	public Map<Integer, List<String>> getCurrentModuleMap() {
		return currentModuleMap;
	}

	public void setCurrentModuleMap(Map<Integer, List<String>> currentModuleMap) {
		this.currentModuleMap = currentModuleMap;
	}
}
