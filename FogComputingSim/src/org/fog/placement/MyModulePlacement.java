package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.Logger;

public class MyModulePlacement extends ModulePlacement{
	
	private ModuleMapping moduleMapping;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	private Map<Integer, Double> currentCpuLoad;
	
	// Stores the current mapping of application modules to fog devices
	private Map<Integer, List<String>> currentModuleMap;
	private Map<Integer, Map<String, Double>> currentModuleLoadMap;
	private Map<Integer, Map<String, Integer>> currentModuleInstanceNum;
	
	List<String> placedModules = new ArrayList<String>();
	
	public MyModulePlacement(List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators,
			Application application, ModuleMapping moduleMapping){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		this.setModuleToDeviceMap(new HashMap<String, List<Integer>>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		
		setSensors(sensors);
		setActuators(actuators);
		setCurrentCpuLoad(new HashMap<Integer, Double>());
		setCurrentModuleMap(new HashMap<Integer, List<String>>());
		setCurrentModuleLoadMap(new HashMap<Integer, Map<String, Double>>());
		setCurrentModuleInstanceNum(new HashMap<Integer, Map<String, Integer>>());
		
		for(FogDevice dev : getFogDevices()){
			getCurrentCpuLoad().put(dev.getId(), 0.0);
			getCurrentModuleLoadMap().put(dev.getId(), new HashMap<String, Double>());
			getCurrentModuleMap().put(dev.getId(), new ArrayList<String>());
			getCurrentModuleInstanceNum().put(dev.getId(), new HashMap<String, Integer>());
		}
		
		mapModules();
		setModuleInstanceCountMap(getCurrentModuleInstanceNum());
	}
	
	@Override
	protected void mapModules() {
		for(String deviceName : getModuleMapping().getModuleMapping().keySet()){
			for(String moduleName : getModuleMapping().getModuleMapping().get(deviceName)){
				int deviceId = CloudSim.getEntityId(deviceName);
				getCurrentModuleMap().get(deviceId).add(moduleName);
				getCurrentModuleLoadMap().get(deviceId).put(moduleName, 0.0);
				getCurrentModuleInstanceNum().get(deviceId).put(moduleName, 0);
			}
		}

		getApplication().setPaths(MygetLeafToRootPaths());
		printPaths(getApplication().getPaths());
		
		for(List<Integer> path : getApplication().getPaths())
			placeModulesInPath(path);
		
		System.out.println("");
		for(int deviceId : getCurrentModuleMap().keySet())
			for(String module : getCurrentModuleMap().get(deviceId))
				createModuleInstanceOnDevice(getApplication().getModuleByName(module), getFogDeviceById(deviceId));
	}
	
	private void placeModulesInPath(List<Integer> path) {
		if(path.size()==0) return;
		
		Map<AppEdge, Double> appEdgeToRate = new HashMap<AppEdge, Double>();
		
		for(AppEdge edge : getApplication().getEdges())
			if(edge.isPeriodic())
				appEdgeToRate.put(edge, 1/edge.getPeriodicity());
		
		for(Integer deviceId : path){
			FogDevice device = getFogDeviceById(deviceId);
			Map<String, Integer> sensorsAssociated = getAssociatedSensors(device);
			Map<String, Integer> actuatorsAssociated = getAssociatedActuators(device);
			placedModules.addAll(sensorsAssociated.keySet());
			placedModules.addAll(actuatorsAssociated.keySet());
			
			if(sensorsAssociated.keySet().isEmpty() || actuatorsAssociated.keySet().isEmpty())
				continue;
			
			// Setting the rates of application edges emanating from sensors
			for(String sensor : sensorsAssociated.keySet())
				for(AppEdge edge : getApplication().getEdges())
					if(edge.getSource().equals(sensor))
						appEdgeToRate.put(edge, sensorsAssociated.get(sensor)*getRateOfSensor(sensor));
						
			// Updating the AppEdge rates for the entire application based on knowledge so far
			boolean changed = true;
			while(changed){ //Loop runs as long as some new information is added
				changed=false;
				Map<AppEdge, Double> rateMap = new HashMap<AppEdge, Double>(appEdgeToRate);
				for(AppEdge edge : rateMap.keySet()){
					AppModule destModule = getApplication().getModuleByName(edge.getDestination());
					if(destModule == null)continue;
					Map<Pair<String, String>, SelectivityModel> map = destModule.getSelectivityMap();
					for(Pair<String, String> pair : map.keySet()){
						if(pair.getFirst().equals(edge.getTupleType())){
							
							// getting mean rate from SelectivityModel
							double outputRate = appEdgeToRate.get(edge)*map.get(pair).getMeanRate();
							AppEdge outputEdge = getApplication().getEdgeMap().get(pair.getSecond());
							
							// if some new information is available
							if(!appEdgeToRate.containsKey(outputEdge) || appEdgeToRate.get(outputEdge)!=outputRate)
								changed = true;
							appEdgeToRate.put(outputEdge, outputRate);
						}
					}
				}
			}
			
			List<String> modulesToPlace = getModulesToPlace(placedModules);
			
			while(modulesToPlace.size() > 0){
				String moduleName = modulesToPlace.get(0);
				double totalCpuLoad = 0;
				
				// FINDING OUT WHETHER PLACEMENT OF OPERATOR ON DEVICE IS POSSIBLE
				for(AppEdge edge : getApplication().getEdges()){ // take all incoming edges
					if(edge.getDestination().equals(moduleName)){
						double rate = appEdgeToRate.get(edge);
						totalCpuLoad += rate*edge.getTupleCpuLength();
					}
				}
					
				if(totalCpuLoad + getCurrentCpuLoad().get(deviceId) > device.getHost().getTotalMips())
					Logger.debug("ModulePlacementEdgeward", "Placement of operator "+moduleName+ "NOT POSSIBLE on device "+device.getName());
				else{
					changed = true;
					Logger.debug("ModulePlacementEdgeward", "Placement of operator "+moduleName+ " on device "+device.getName() + " successful.");
					getCurrentCpuLoad().put(deviceId, totalCpuLoad + getCurrentCpuLoad().get(deviceId));
					System.out.println("Placement of operator "+moduleName+ " on device "+device.getName() + " successful.");

					if(!currentModuleMap.containsKey(deviceId))
						currentModuleMap.put(deviceId, new ArrayList<String>());
					
					currentModuleMap.get(deviceId).add(moduleName);
					placedModules.add(moduleName);
					modulesToPlace = getModulesToPlace(placedModules);
					getCurrentModuleLoadMap().get(device.getId()).put(moduleName, totalCpuLoad);
					
					int max = 1;
					for(AppEdge edge : getApplication().getEdges()){
						if(edge.getSource().equals(moduleName) && actuatorsAssociated.containsKey(edge.getDestination()))
							max = Math.max(actuatorsAssociated.get(edge.getDestination()), max);
						if(edge.getDestination().equals(moduleName) && sensorsAssociated.containsKey(edge.getSource()))
							max = Math.max(sensorsAssociated.get(edge.getSource()), max);
					}
					getCurrentModuleInstanceNum().get(deviceId).put(moduleName, max);
				}
				modulesToPlace.remove(moduleName);
			}
		}
	}
	
	/**
	 * Get the list of modules that are ready to be placed 
	 * @param placedModules Modules that have already been placed in current path
	 * @return list of modules ready to be placed
	 */
	private List<String> getModulesToPlace(List<String> placedModules){ // Changed---------------------------------
		Application app = getApplication();
		List<String> modulesToPlace = new ArrayList<String>();
		
		for(AppModule module : app.getModules())
			if(!placedModules.contains(module.getName()))
				modulesToPlace.add(module.getName());
		
		return modulesToPlace;
	}
	
	/**
	 * Gets all sensors associated with fog-device <b>device</b>
	 * @param device
	 * @return map from sensor type to number of such sensors
	 */
	private Map<String, Integer> getAssociatedSensors(FogDevice device) {
		Map<String, Integer> endpoints = new HashMap<String, Integer>();
		for(Sensor sensor : getSensors()){
			if(sensor.getGatewayDeviceId()==device.getId()){
				if(!endpoints.containsKey(sensor.getTupleType()))
					endpoints.put(sensor.getTupleType(), 0);
				endpoints.put(sensor.getTupleType(), endpoints.get(sensor.getTupleType())+1);
			}
		}
		return endpoints;
	}
	
	/**
	 * Gets all actuators associated with fog-device <b>device</b>
	 * @param device
	 * @return map from actuator type to number of such sensors
	 */
	private Map<String, Integer> getAssociatedActuators(FogDevice device) {
		Map<String, Integer> endpoints = new HashMap<String, Integer>();
		for(Actuator actuator : getActuators()){
			if(actuator.getGatewayDeviceId()==device.getId()){
				if(!endpoints.containsKey(actuator.getActuatorType()))
					endpoints.put(actuator.getActuatorType(), 0);
				endpoints.put(actuator.getActuatorType(), endpoints.get(actuator.getActuatorType())+1);
			}
		}
		return endpoints;
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
	
	protected double getRateOfSensor(String sensorType){
		for(Sensor sensor : getSensors()){
			if(sensor.getTupleType().equals(sensorType))
				return 1/sensor.getTransmitDistribution().getMeanInterTransmitTime();
		}
		return 0;
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

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public Map<Integer, Double> getCurrentCpuLoad() {
		return currentCpuLoad;
	}

	public void setCurrentCpuLoad(Map<Integer, Double> currentCpuLoad) {
		this.currentCpuLoad= currentCpuLoad;
	}

	public Map<Integer, Map<String, Double>> getCurrentModuleLoadMap() {
		return currentModuleLoadMap;
	}

	public void setCurrentModuleLoadMap(
			Map<Integer, Map<String, Double>> currentModuleLoadMap) {
		this.currentModuleLoadMap = currentModuleLoadMap;
	}

	public Map<Integer, Map<String, Integer>> getCurrentModuleInstanceNum() {
		return currentModuleInstanceNum;
	}

	public void setCurrentModuleInstanceNum(
			Map<Integer, Map<String, Integer>> currentModuleInstanceNum) {
		this.currentModuleInstanceNum = currentModuleInstanceNum;
	}
}
