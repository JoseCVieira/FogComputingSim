package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.placement.algorithms.routing.DijkstraAlgorithm;
import org.fog.placement.algorithms.routing.Edge;
import org.fog.placement.algorithms.routing.Graph;
import org.fog.placement.algorithms.routing.Vertex;

public class ModulePlacementMapping extends ModulePlacement{
	
	// Stores the current mapping of application modules to fog devices
	private Map<Integer, List<String>> currentModuleMap;
	private ModuleMapping moduleMapping;
	
	public ModulePlacementMapping(List<FogDevice> fogDevices, Application application,
			ModuleMapping moduleMapping){
		this.setFogDevices(fogDevices);
		this.setApplication(application);
		this.setModuleMapping(moduleMapping);
		setCurrentModuleMap(new HashMap<Integer, List<String>>());
		this.setModuleToDeviceMap(new HashMap<String, Integer>());
		this.setDeviceToModuleMap(new HashMap<Integer, List<AppModule>>());
		
		for(FogDevice dev : getFogDevices())
			getCurrentModuleMap().put(dev.getId(), new ArrayList<String>());
		
		mapModules();
		createGraph();
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
		
		for(int deviceId : getCurrentModuleMap().keySet())
			for(String module : getCurrentModuleMap().get(deviceId))
				createModuleInstanceOnDevice(getApplication().getModuleByName(module),
						getFogDeviceById(deviceId));
	}
	
	private void createGraph() {
		Map<Integer, Vertex> mapNodes = new HashMap<Integer, Vertex>();
		List<Edge> edges = new ArrayList<Edge>();
		
		for(FogDevice fDev : getFogDevices()) {
			Vertex vertex = new Vertex(Integer.toString(fDev.getId()));
			mapNodes.put(fDev.getId(), vertex);
		}
		
		for(FogDevice fDev : getFogDevices()) {
			int dId = fDev.getId();
			
			for(int neighborId : fDev.getNeighborsIds()) {
				int lat = fDev.getLatencyMap().get(neighborId).intValue();
				edges.add(new Edge(mapNodes.get(dId), mapNodes.get(neighborId), lat));
				edges.add(new Edge(mapNodes.get(neighborId), mapNodes.get(dId), lat));
			}
		}

		Graph graph = new Graph(new ArrayList<Vertex>(mapNodes.values()), edges);
        getApplication().setDijkstraAlgorithm(new DijkstraAlgorithm(graph));
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
