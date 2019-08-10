package org.fog.gui.core;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math3.util.Pair;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.selectivity.FractionalSelectivity;
import org.fog.utils.Location;
import org.fog.utils.Movement;
import org.fog.utils.distribution.DeterministicDistribution;
import org.fog.utils.distribution.Distribution;
import org.fog.utils.distribution.NormalDistribution;
import org.fog.utils.distribution.UniformDistribution;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Bridge {
	private static Node getNode(Graph graph, String name){
		for(Node node : graph.getDevicesList().keySet())
			if(node!=null)
				if(node.getName().equals(name))
					return node;
		return null;
	}

	// convert from JSON object to Graph object
	public static Graph jsonToGraph(String fileName){
		Graph graph = new Graph();
		
		try {
			JSONObject doc = (JSONObject) JSONValue.parse(new FileReader(fileName));
    		JSONArray nodes = (JSONArray) doc.get("nodes");
    		@SuppressWarnings("unchecked")
			Iterator<JSONObject> iter = nodes.iterator();
			while(iter.hasNext()){
				JSONObject node = iter.next();
				String nodeName = (String) node.get("name");
				String application = (String) node.get("application");
				int level = new BigDecimal((Long)node.get("level")).intValue();
				double mips = (Double) node.get("mips");
				int ram = new BigDecimal((Long)node.get("ram")).intValue();
				long strg = (Long) node.get("strg");
				double rateMips = (Double) node.get("ratePerMips");
				double rateRam = (Double) node.get("ratePerRam");
				double rateStorage = (Double) node.get("ratePerStrg");
				double rateBw = (Double) node.get("ratePerBw");
				double rateEnergy = (Double) node.get("ratePerEn");
				double idlePower = (Double) node.get("idlePower");
				double busyPower = (Double) node.get("busyPower");
				double posX = (Double) node.get("posx");
				double posY = (Double) node.get("posy");
				int direction = new BigDecimal((Long)node.get("direction")).intValue();
				double velocity = (Double) node.get("velocity");
				
				Movement movement = new Movement(velocity, direction, new Location(posX, posY));
				
				int distType = new BigDecimal((Long)node.get("distribution")).intValue();
				Distribution distribution = null;
				
				if(distType == Distribution.DETERMINISTIC)
					distribution = new DeterministicDistribution(new BigDecimal((Double)node.get("value")).doubleValue());
				else if(distType == Distribution.NORMAL){
					distribution = new NormalDistribution(new BigDecimal((Double)node.get("mean")).doubleValue(), 
							new BigDecimal((Double)node.get("stdDev")).doubleValue());
				} else if(distType == Distribution.UNIFORM){
					distribution = new UniformDistribution(new BigDecimal((Double)node.get("min")).doubleValue(), 
							new BigDecimal((Double)node.get("max")).doubleValue());
				}

				Node fogDevice = new FogDeviceGui(nodeName, level, mips, ram, strg, rateMips, rateRam, rateStorage, rateBw, rateEnergy, idlePower,
						busyPower, movement, application, distribution);
				graph.addNode(fogDevice);
			}
				
			JSONArray links = (JSONArray) doc.get("links");
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> linksIter =links.iterator(); 
			while(linksIter.hasNext()){
				JSONObject link = linksIter.next();
				String src = (String) link.get("source");  
				String dst = (String) link.get("destination");
				double lat = (Double) link.get("latency");
				double bw = (Double) link.get("bw");
				
				Node source = (Node) getNode(graph, src);
				Node target = (Node) getNode(graph, dst);
				
				if(source!=null && target!=null){
					Link edge = new Link(target, lat, bw);
					graph.addEdge(source, edge);
				}
			}
			
			JSONArray applications = (JSONArray) doc.get("applications");
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> appsIter = applications.iterator(); 
			while(appsIter.hasNext()){
				JSONObject app = appsIter.next();				
				graph.addApp(new ApplicationGui((String) app.get("name"), app.get("loops")));
			}
			
			JSONArray modules = (JSONArray) doc.get("modules");
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> modulesIter = modules.iterator(); 
			while(modulesIter.hasNext()){
				JSONObject model = modulesIter.next();
				
				String appId = (String) model.get("appId");
				String name = (String) model.get("name");
				int ram = ((Long) model.get("ram")).intValue();
				long strg = (Long) model.get("strg");
				boolean clientModule = (Boolean) model.get("clientModule");
				boolean globalModule = (Boolean) model.get("globalModule");
				
				for(ApplicationGui app : graph.getAppList()) {
					if(app.getAppId().equals(appId)) {
						app.addAppModule(name, ram, strg, clientModule, globalModule);
						break;
					}
				}
			}
			
			JSONArray edges = (JSONArray) doc.get("edges");
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> edgesIter = edges.iterator(); 
			while(edgesIter.hasNext()){
				JSONObject edge = edgesIter.next();

				String appId = (String) edge.get("appId");
				String source = (String) edge.get("source");
				String destination = (String) edge.get("destination");
				boolean periodic = (Boolean) edge.get("periodic");
				double periodicity = (Double) edge.get("periodicity");
				double tupleCpuLength = (Double) edge.get("tupleCpuLength");
				double tupleNwLength = (Double) edge.get("tupleNwLength");
				String tupleType = (String) edge.get("tupleType");
				int edgeType = ((Long) edge.get("edgeType")).intValue();
				
				for(ApplicationGui app : graph.getAppList()) {
					if(app.getAppId().equals(appId)) {
						if(!periodic)
							app.addAppEdge(source, destination, tupleCpuLength, tupleNwLength, tupleType,
									edgeType);
						else
							app.addAppEdge(source, destination, periodicity,tupleCpuLength, tupleNwLength,
									tupleType, edgeType);
						break;
					}
				}
			}
			
			JSONArray tuples = (JSONArray) doc.get("tuples");
			@SuppressWarnings("unchecked")
			Iterator<JSONObject> tuplesIter = tuples.iterator(); 
			while(tuplesIter.hasNext()){
				JSONObject tuple = tuplesIter.next();
				
				String appId = (String) tuple.get("appId");
				String moduleName = (String) tuple.get("moduleName");
				String inputTuple = (String) tuple.get("inputTuple");
				String outputTuple = (String) tuple.get("outputTuple");
				double selectivity = (Double) tuple.get("selectivity");
				
				for(ApplicationGui app : graph.getAppList()) {
					if(app.getAppId().equals(appId)) {
						app.addTupleMapping(moduleName, inputTuple, outputTuple, new FractionalSelectivity(selectivity));
						break;
					}
				}
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		return graph;
	}
	
	// convert from Graph object to JSON object
	@SuppressWarnings("unchecked")
	public static String graphToJson(Graph graph){
		if(graph.getDevicesList().size() < 1 && graph.getAppList().size() < 1)
			return "Graph is Empty";

		Map<Node, List<Node>> edgeList = new HashMap<Node, List<Node>>();
		
		JSONObject topo = new JSONObject();
		JSONArray nodes = new JSONArray();
		JSONArray links = new JSONArray();
		
		for (Entry<Node, List<Link>> entry : graph.getDevicesList().entrySet()) {
			Node srcNode = entry.getKey();
			
			// add node
			JSONObject jobj = new JSONObject();
			FogDeviceGui fogDevice = (FogDeviceGui)srcNode;
			jobj.put("name", fogDevice.getName());
			jobj.put("mips", fogDevice.getMips());
			jobj.put("ram", fogDevice.getRam());
			jobj.put("strg", fogDevice.getStorage());
			jobj.put("bw", fogDevice.getBw());
			jobj.put("level", fogDevice.getLevel());
			jobj.put("ratePerMips", fogDevice.getRateMips());
			jobj.put("ratePerRam", fogDevice.getRateRam());
			jobj.put("ratePerStrg", fogDevice.getRateStorage());
			jobj.put("ratePerBw", fogDevice.getRateBw());
			jobj.put("ratePerEn", fogDevice.getRateEnergy());
			jobj.put("idlePower", fogDevice.getIdlePower());
			jobj.put("busyPower", fogDevice.getBusyPower());
			jobj.put("posx", fogDevice.getMovement().getLocation().getX());
			jobj.put("posy", fogDevice.getMovement().getLocation().getY());
			jobj.put("direction", fogDevice.getMovement().getDirection());
			jobj.put("velocity", fogDevice.getMovement().getVelocity());
			jobj.put("application", fogDevice.getApplication());
			jobj.put("distribution", fogDevice.getDistributionType());
			
			if(fogDevice.getDistributionType()==Distribution.DETERMINISTIC)
				jobj.put("value", ((DeterministicDistribution)fogDevice.getDistribution()).getValue());
			else if(fogDevice.getDistributionType()==Distribution.NORMAL){
				jobj.put("mean", ((NormalDistribution)fogDevice.getDistribution()).getMean());
				jobj.put("stdDev", ((NormalDistribution)fogDevice.getDistribution()).getStdDev());
			} else if(fogDevice.getDistributionType()==Distribution.UNIFORM){
				jobj.put("min", ((UniformDistribution)fogDevice.getDistribution()).getMin());
				jobj.put("max", ((UniformDistribution)fogDevice.getDistribution()).getMax());
			}
			nodes.add(jobj);
			
			// add edge
			for (Link edge : entry.getValue()) {
				Node destNode = edge.getNode();

				if (edgeList.containsKey(destNode) && edgeList.get(destNode).contains(srcNode))
					continue;
				
				JSONObject jobj2 = new JSONObject();
				jobj2.put("source", srcNode.getName());
				jobj2.put("destination", destNode.getName());
				jobj2.put("latency", edge.getLatency());
				jobj2.put("bw", edge.getBandwidth());
				links.add(jobj2);
				
				// add exist edge to the edgeList
				if (edgeList.containsKey(entry.getKey()))
					edgeList.get(entry.getKey()).add(edge.getNode());
				else {
					List<Node> ns = new ArrayList<Node>();
					ns.add(edge.getNode());
					edgeList.put(entry.getKey(), ns);
				}
			}
		}
		topo.put("nodes", nodes);
		topo.put("links", links);
		
		// add application
		JSONArray applications = new JSONArray();
		JSONArray modules = new JSONArray();
		JSONArray edges = new JSONArray();
		JSONArray tuples = new JSONArray();
		for (ApplicationGui applicationGui : graph.getAppList()) {
			JSONObject jobj = new JSONObject();
			jobj.put("name", applicationGui.getAppId());
			jobj.put("loops", applicationGui.getLoops());
			applications.add(jobj);
			
			for(AppModule appModule : applicationGui.getModules()) {
				jobj = new JSONObject();
				jobj.put("appId", appModule.getAppId());
				jobj.put("name", appModule.getName());
				jobj.put("ram", appModule.getRam());
				jobj.put("strg", appModule.getSize());
				jobj.put("clientModule", appModule.isClientModule());
				jobj.put("globalModule", appModule.isGlobalModule());
				modules.add(jobj);
				
				for(Pair<String, String> pair : appModule.getSelectivityMap().keySet()) {
					jobj = new JSONObject();
					jobj.put("appId", appModule.getAppId());
					jobj.put("moduleName", appModule.getName());
					jobj.put("inputTuple", pair.getFirst());
					jobj.put("outputTuple", pair.getSecond());
					jobj.put("selectivity", ((FractionalSelectivity)appModule.getSelectivityMap().get(pair)).getSelectivity());
					tuples.add(jobj);
				}
			}
			
			for(AppEdge appEdge : applicationGui.getEdges()) {
				jobj = new JSONObject();
				jobj.put("appId", applicationGui.getAppId());
				jobj.put("source", appEdge.getSource());
				jobj.put("destination", appEdge.getDestination());
				jobj.put("periodic", appEdge.isPeriodic());
				jobj.put("periodicity", appEdge.getPeriodicity());
				jobj.put("tupleCpuLength", appEdge.getTupleCpuLength());
				jobj.put("tupleNwLength", appEdge.getTupleNwLength());
				jobj.put("tupleType", appEdge.getTupleType());
				jobj.put("edgeType", appEdge.getEdgeType());
				edges.add(jobj);
			}
		}
		
		topo.put("applications", applications);
		topo.put("modules", modules);
		topo.put("edges", edges);
		topo.put("tuples", tuples);
		
		StringWriter out = new StringWriter();
		String jsonText = "";
		
		try {
			topo.writeJSONString(out);
			jsonText = out.toString();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		//System.out.println(jsonText);
		return jsonText;
	}
}