package org.fog.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.FogDevice;
import org.fog.utils.dijkstra.DijkstraAlgorithm;
import org.fog.utils.dijkstra.Edge;
import org.fog.utils.dijkstra.Graph;
import org.fog.utils.dijkstra.Vertex;

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
				if(!createModuleInstanceOnDevice(getApplication().getModuleByName(module),
						getFogDeviceById(deviceId)))
					CloudSim.abruptallyTerminate();
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
			
			for(int p : fDev.getParentsIds()) {
				if(p == -1)
					continue;
				
				int lat = fDev.getUpStreamLatencyMap().get(p).intValue();
				edges.add(new Edge(mapNodes.get(dId), mapNodes.get(p), lat));
				edges.add(new Edge(mapNodes.get(p), mapNodes.get(dId), lat));
			}
			
			for(int b : fDev.getBrothersIds()) {
				int lat = fDev.getUpStreamLatencyMap().get(b).intValue();
				edges.add(new Edge(mapNodes.get(dId), mapNodes.get(b), lat));
				edges.add(new Edge(mapNodes.get(b), mapNodes.get(dId), lat));
			}
			
			for(int c : fDev.getChildrenIds()) {
				int lat = fDev.getDownStreamLatencyMap().get(c).intValue();
				edges.add(new Edge(mapNodes.get(dId), mapNodes.get(c), lat));
				edges.add(new Edge(mapNodes.get(c), mapNodes.get(dId), lat));
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
