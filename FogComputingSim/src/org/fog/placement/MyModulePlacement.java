package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;

public class MyModulePlacement extends ModulePlacement{
	
	// Stores the current mapping of application modules to fog devices
	private Map<Integer, List<String>> currentModuleMap;
	private ModuleMapping moduleMapping;
	
	public MyModulePlacement(List<FogDevice> fogDevices, Application application,
			ModuleMapping moduleMapping){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		setCurrentModuleMap(new HashMap<Integer, List<String>>());
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		
		for(FogDevice dev : getFogDevices())
			getCurrentModuleMap().put(dev.getId(), new ArrayList<String>());
		
		mapModules();
	}
	
	@Override
	protected void mapModules() {
		for(String deviceName : getModuleMapping().getModuleMapping().keySet()){
			for(String moduleName : getModuleMapping().getModuleMapping().get(deviceName)){
				
				int deviceId = -1;
				for(FogDevice dev : getFogDevices())
					if(dev.getName().equals(deviceName))
						deviceId = dev.getId();
				
				getCurrentModuleMap().get(deviceId).add(moduleName);
			}
		}

		getApplication().setPaths(MygetLeafToRootPaths());
		printPaths(getApplication().getPaths());

		for(int deviceId : getCurrentModuleMap().keySet())
			for(String module : getCurrentModuleMap().get(deviceId))
				if(!createModuleInstanceOnDevice(getApplication().getModuleByName(module),
						getFogDeviceById(deviceId)))
					CloudSim.abruptallyTerminate();
	}
	
	protected List<List<Integer>> MygetLeafToRootPaths(){
		List<Integer> listInner = new ArrayList<>();
		
		// All paths without brothers
		int cloudId = -1; //TODO Neste momento só é permitido ter 1 cloud...
		for(FogDevice device : getFogDevices())
			if(device.getParentsIds().size() == 1 && device.getParentsIds().get(0) == -1)
				cloudId = device.getId();
		
		List<List<Integer>> pathList = getPaths(cloudId);
		List<List<Integer>> pathBrotherList = new ArrayList<List<Integer>>();
		
		// All paths with brothers
		for(FogDevice device : getFogDevices()) {
			for(int brotherId : device.getBrothersIds()) {				
				List<List<Integer>> brothersTopPathsList = MygetPathsFromTo(brotherId, cloudId, pathList);
				List<List<Integer>> parentBottomPathsList = MygetPathsFromTo(-1, device.getId(), pathList);
				
				for(List<Integer> parentPath : parentBottomPathsList) {
					for(List<Integer> brotherPath : brothersTopPathsList) {
						
						listInner = new ArrayList<>();
						
						for(int fogId : parentPath)
							listInner.add(fogId);
							
						for(int fogId : brotherPath)
							listInner.add(fogId);
						
						if(!pathBrotherList.contains(listInner))
							pathBrotherList.add(listInner);
					}
				}
			}
		}
		
		for(List<Integer> path : pathBrotherList)
			pathList.add(path);
		
		return pathList;
	}
	
	@SuppressWarnings("serial")
	protected List<List<Integer>> getPaths(final int fogDeviceId){
		FogDevice device = (FogDevice)CloudSim.getEntity(fogDeviceId);
		
		if(device.getChildrenIds().size() == 0){		
			final List<Integer> path =  (new ArrayList<Integer>(){{add(fogDeviceId);}});
			List<List<Integer>> paths = (new ArrayList<List<Integer>>(){{add(path);}});
			return paths;
		}
		
		List<List<Integer>> paths = new ArrayList<List<Integer>>();
		for(int childId : device.getChildrenIds()){
			List<List<Integer>> childPaths = getPaths(childId);
			for(List<Integer> childPath : childPaths)
				childPath.add(fogDeviceId);
			paths.addAll(childPaths);
		}
		return paths;
	}
	
	@SuppressWarnings("serial")
	protected List<List<Integer>> MygetPathsFromTo(final int fromFogDeviceId, final int toFogDeviceId,
			List<List<Integer>> pathsList){
		
		if(pathsList.size() == 0){
			final List<Integer> path = new ArrayList<Integer>(){{add(toFogDeviceId);}};
			List<List<Integer>> paths = new ArrayList<List<Integer>>(){{add(path);}};
			return paths;
		}
		
		List<List<Integer>> pathList = new ArrayList<>();
		List<Integer> listInner = new ArrayList<>();
		
		if(fromFogDeviceId != -1) {
			for(List<Integer> path : pathsList) {
				if(path.contains(fromFogDeviceId) && path.contains(toFogDeviceId)) {
					listInner = new ArrayList<>();
					
					boolean first = false, second = false;
					for(int i = 0; i < path.size(); i++) {
						if(path.get(i) == fromFogDeviceId)
							first = true;
						
						if(first && !second)
							listInner.add(path.get(i));
						
						if(path.get(i) == toFogDeviceId)
							second = true;
					}
					
					if(!pathList.contains(listInner))
						pathList.add(listInner);
				}
			}
		}else {
			for(List<Integer> path : pathsList) {
				if(path.contains(toFogDeviceId)) {
					listInner = new ArrayList<>();
					
					boolean canBeAdded = true;
					for(int i = 0; i < path.size(); i++) {
						if(canBeAdded)
							listInner.add(path.get(i));
						
						if(canBeAdded && path.get(i) == toFogDeviceId)
							canBeAdded = false;
					}
					
					if(!pathList.contains(listInner))
						pathList.add(listInner);
				}
			}
		}
		return pathList;
	}
	
	private void printPaths(List<List<Integer>> paths){
		System.out.println("\nPath list:\n");
		
		for(List<Integer> path : paths) {
			for(int fogId : path)
				System.out.print(getFogDeviceById(fogId).getName() + "   ");
			System.out.println();
		}
		
		System.out.println("\n");
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
