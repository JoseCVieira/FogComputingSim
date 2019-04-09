package org.fog.placement.algorithms.placement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.FogDeviceCharacteristics;
import org.fog.entities.Sensor;
import org.fog.placement.algorithms.routing.DijkstraAlgorithm;
import org.fog.placement.algorithms.routing.Edge;
import org.fog.placement.algorithms.routing.Graph;
import org.fog.placement.algorithms.routing.Vertex;

public abstract class Algorithm {
	private static final boolean PRINT_DETAILS = true;
	
	private double fMipsPrice[];
	private double fRamPrice[];
	private double fMemPrice[];
	private double fBwPrice[];
	
	private int fId[];
	private String fName[];
	private double fMips[];
	private double fRam[];
	private double fMem[];
	private double fBw[];
	private PowerModel fPwModel[];
	
	private String mName[];
	private double mMips[];
	private double mRam[];
	private double mMem[];
	private double mBw[];
	
	private double[][] latencyMap;
	private double[][] dependencyMap;
	
	Map<Map<Integer, Integer>, List<Integer>> paths = new HashMap<Map<Integer,Integer>, List<Integer>>();
	
	public Algorithm(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) throws IllegalArgumentException {
		
		if(applications == null || sensors == null || actuators == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		extractFogCharacteristics(fogDevices, sensors, actuators);
		computeLatencyMap(fogDevices, sensors, actuators);
		
		extractAppCharacteristics(applications);
		computeDependencyMap(applications, sensors, actuators);
	}
	
	private void extractFogCharacteristics (final List<FogDevice> fogDevices,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		
		ArrayList<Integer> id = new ArrayList<Integer>();
		ArrayList<String> name = new ArrayList<String>();
		ArrayList<Double> mips = new ArrayList<Double>();
		ArrayList<Double> ram = new ArrayList<Double>();
		ArrayList<Double> mem = new ArrayList<Double>();
		ArrayList<Double> bw = new ArrayList<Double>();
		ArrayList<PowerModel> pwModel = new ArrayList<PowerModel>();
		
		ArrayList<Double> mipsPrice = new ArrayList<Double>();
		ArrayList<Double> ramPrice = new ArrayList<Double>();
		ArrayList<Double> memPrice = new ArrayList<Double>();
		ArrayList<Double> bwPrice = new ArrayList<Double>();
		
		for(FogDevice fogDevice : fogDevices) {
			FogDeviceCharacteristics characteristics =
					(FogDeviceCharacteristics) fogDevice.getCharacteristics();
			
			double aux = 0;
			for(Pe pe : fogDevice.getHost().getPeList())
				aux += pe.getMips();
			
			id.add(fogDevice.getId());
			name.add(fogDevice.getName());
			mips.add(aux);
			ram.add((double) fogDevice.getHost().getRam());
			mem.add((double) fogDevice.getHost().getStorage());
			bw.add((double) fogDevice.getHost().getBw());
			pwModel.add(fogDevice.getHost().getPowerModel());
			
			mipsPrice.add(characteristics.getCostPerMips());
			ramPrice.add(characteristics.getCostPerMem());
			memPrice.add(characteristics.getCostPerStorage());
			bwPrice.add(characteristics.getCostPerBw());
		}
		
		// sensors and actuators are added to compute tuples latency
		for(Sensor sensor : sensors) {
			id.add(sensor.getId());
			name.add(sensor.getName());
			mips.add(0.0);
			ram.add(0.0);
			mem.add(0.0);
			bw.add(0.0);
			pwModel.add(null);
			mipsPrice.add(0.0);
			ramPrice.add(0.0);
			memPrice.add(0.0);
			bwPrice.add(0.0);
		}
		
		for(Actuator actuator : actuators) {
			id.add(actuator.getId());
			name.add(actuator.getName());
			mips.add(0.0);
			ram.add(0.0);
			mem.add(0.0);
			bw.add(0.0);
			pwModel.add(null);
			mipsPrice.add(0.0);
			ramPrice.add(0.0);
			memPrice.add(0.0);
			bwPrice.add(0.0);
		}
		
		fName = name.toArray(new String[name.size()]);
		fPwModel = convertPowerModels(pwModel);
		
		fId = convertIntegers(id);
		fMips = convertDoubles(mips);
		fRam = convertDoubles(ram);
		fMem = convertDoubles(mem);
		fBw = convertDoubles(bw);
		
		fMipsPrice = convertDoubles(mipsPrice);
		fRamPrice = convertDoubles(ramPrice);
		fMemPrice = convertDoubles(memPrice);
		fBwPrice = convertDoubles(bwPrice);
		
		if(PRINT_DETAILS) {
			for(int i = 0; i < fogDevices.size(); i++) {
				FogDevice fDevice = null;
				
				for(FogDevice fogDevice : fogDevices)
					if(fogDevice.getName().equals(getfName()[i]))
						fDevice = fogDevice;
				
				System.out.println("Id: " + fDevice.getId() + " fName: " + getfName()[i]);
				System.out.println("fMips: " + getfMips()[i]);
				System.out.println("fRam: " + getfRam()[i]);
				System.out.println("fMem: " + getfMem()[i]);
				System.out.println("fBw: " + getfBw()[i]);
				System.out.println("fMipsPrice: " + getfMipsPrice()[i]);
				System.out.println("fRamPrice: " + getfRamPrice()[i]);
				System.out.println("fMemPrice: " + getfMemPrice()[i]);
				System.out.println("fBwPrice: " + getfBwPrice()[i]);
				System.out.println("Neighbors: " +  fDevice.getNeighborsIds());
				System.out.println("LatencymMap: " + fDevice.getLatencyMap() + "\n");
			}
		}
	}
	
	private void extractAppCharacteristics(final List<Application> applications) {		
		ArrayList<String> name = new ArrayList<String>(); 
		ArrayList<Double> mips = new ArrayList<Double>(); 
		ArrayList<Double> ram = new ArrayList<Double>(); 
		ArrayList<Double> mem = new ArrayList<Double>(); 
		ArrayList<Double> bw = new ArrayList<Double>(); 
		
		for(Application application : applications) {
			for(AppModule module : application.getModules()) {
				name.add(module.getName());
				mips.add(module.getMips());
				ram.add((double) module.getRam());
				mem.add((double) module.getSize());
				bw.add((double) module.getBw());
			}
			
			// sensors and actuators are added to compute tuples latency
			for(AppEdge appEdge : application.getEdges()) {
				if(!name.contains(appEdge.getSource())) {
					name.add(appEdge.getSource());
					mips.add(0.0);
					ram.add(0.0);
					mem.add(0.0);
					bw.add(0.0);
				}
				
				if(!name.contains(appEdge.getDestination())) {
					name.add(appEdge.getDestination());
					mips.add(0.0);
					ram.add(0.0);
					mem.add(0.0);
					bw.add(0.0);
				}
			}
		}
		
		mName = name.toArray(new String[name.size()]);
		mMips = convertDoubles(mips);
		mRam = convertDoubles(ram);
		mMem = convertDoubles(mem);
		mBw = convertDoubles(bw);
		
		if(PRINT_DETAILS) {
			for(int i = 0; i < mName.length; i++) {
				System.out.println("mName: " + getmName()[i]);
				System.out.println("mMips: " + getmMips()[i]);
				System.out.println("mRam: " + getmRam()[i]);
				System.out.println("mMem: " + getmMem()[i]);
				System.out.println("mBw: " + getmBw()[i] + "\n");
			}
		}
	}
	
	private void computeLatencyMap(final List<FogDevice> fogDevices,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		Map<Integer, Vertex> mapNodes = new HashMap<Integer, Vertex>();
		List<Edge> edges = new ArrayList<Edge>();
		
		for(FogDevice fogDevice : fogDevices) {
			Vertex vertex = new Vertex(Integer.toString(fogDevice.getId()));
			mapNodes.put(fogDevice.getId(), vertex);
		}
		
		// sensors and actuators are added to compute tuples latency
		for(Sensor sensor : sensors) {
			Vertex vertex = new Vertex(Integer.toString(sensor.getId()));
			mapNodes.put(sensor.getId(), vertex);
		}
		
		for(Actuator actuator : actuators) {
			Vertex vertex = new Vertex(Integer.toString(actuator.getId()));
			mapNodes.put(actuator.getId(), vertex);
		}
		
		for(FogDevice fogDevice : fogDevices) {
			int dId = fogDevice.getId();
			
			for(int neighborId : fogDevice.getNeighborsIds()) {
				int lat = fogDevice.getLatencyMap().get(neighborId).intValue();
				edges.add(new Edge(mapNodes.get(dId), mapNodes.get(neighborId), lat));
				edges.add(new Edge(mapNodes.get(neighborId), mapNodes.get(dId), lat));
			}
		}
		
		for(Sensor sensor : sensors) {
			int id1 = sensor.getId();
			int id2 = sensor.getGatewayDeviceId();
			double lat = sensor.getLatency();
			
			edges.add(new Edge(mapNodes.get(id1), mapNodes.get(id2), lat));
			edges.add(new Edge(mapNodes.get(id2), mapNodes.get(id1), lat));
		}
		
		for(Actuator actuator : actuators) {
			int id1 = actuator.getId();
			int id2 = actuator.getGatewayDeviceId();
			double lat = actuator.getLatency();
			
			edges.add(new Edge(mapNodes.get(id1), mapNodes.get(id2), lat));
			edges.add(new Edge(mapNodes.get(id2), mapNodes.get(id1), lat));
		}

		Graph graph = new Graph(new ArrayList<Vertex>(mapNodes.values()), edges);
		DijkstraAlgorithm dijkstra = new DijkstraAlgorithm(graph);
		
		latencyMap = new double[fName.length][fName.length];
		for(Vertex v1 : dijkstra.getNodes()) {
			for(Vertex v2 : dijkstra.getNodes()) {
				dijkstra.execute(v1);
				
				LinkedList<Vertex> path = dijkstra.getPath(v2);
				
				double latency = 0;
				int iter = 0;
				if(path != null) {
					for(Vertex v : path) {
						boolean found = false;
						
						for(FogDevice fogDevice : fogDevices) {
							if(Integer.parseInt(v.getName()) == fogDevice.getId()) {
								for(Integer neighborId : fogDevice.getNeighborsIds()) {
									if(iter < path.size() - 1 &&
											Integer.parseInt(path.get(iter+1).getName()) == neighborId) {
										latency += (double) fogDevice.getLatencyMap().get(neighborId);
										found = true;
										break;
									}
								}
								break;
							}
						}
						
						if(!found) {
							for(Sensor sensor : sensors) {
								if(Integer.parseInt(v.getName()) == sensor.getId()) {
									found = true;
									latency += sensor.getLatency();
									break;
								}
							}
						}
						
						if(!found) {
							for(Actuator actuator : actuators) {
								if(Integer.parseInt(v.getName()) == actuator.getId()) {
									found = true;
									latency += actuator.getLatency();
									break;
								}
							}
						}
						iter++;
					}
				}
				
				int col = 0;
				int row = 0;
				for(int i = 0; i < fId.length; i++) {
					if(fId[i] == Integer.parseInt(v1.getName()))
						col = i;
					
					if(fId[i] == Integer.parseInt(v2.getName()))
						row = i;
				}
				
				latencyMap[col][row] = latency;
			}
		}
		
		if(PRINT_DETAILS) {
			System.out.println("\nLATENCY MAP:");
			
			final String[][] table = new String[fId.length+1][fId.length+1];
			
			table[0][0] = " ";
			for(int i = 0; i < fId.length; i++)
				table[0][i+1] = fName[i];
			
			for(int i = 0; i < fId.length; i++) {
				table[i+1][0] = fName[i];
				
				for(int j = 0; j < fId.length; j++)
					table[i+1][j+1] = Double.toString(latencyMap[i][j]);
			}
			
			String repeated = repeate(fId.length+1, "%13s");
			
			for (final Object[] row : table)
			    System.out.format(repeated + "\n", row);
		}
		
		System.exit(0);
	}
	
	private void computeDependencyMap(final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {
		
		/*for(Application application : applications) {
			for(AppEdge appEdge : application.getEdges()) {
				dependencyMap.put()
				
				
			}
		}
		
		for(Map<Integer, Integer> innerMap : latencyMap.keySet()) {
			
		}*/
	}
	
	private static double[] convertDoubles(List<Double> d) {
		double[] ret = new double[d.size()];
		
	    for (int i=0; i < ret.length; i++)
	        ret[i] = d.get(i).doubleValue();
	    
	    return ret;
	}
	
	private static int[] convertIntegers(List<Integer> ints) {
		int[] ret = new int[ints.size()];
		
	    for (int i=0; i < ret.length; i++)
	        ret[i] = ints.get(i).intValue();
	    
	    return ret;
	}
	
	private static PowerModel[] convertPowerModels(List<PowerModel> pwM) {
		PowerModel[] ret = new PowerModel[pwM.size()];
		
	    for (int i=0; i < ret.length; i++)
	        ret[i] = pwM.get(i);
	    
	    return ret;
	}
	
	private static String repeate(int i, String s) {
	    StringBuilder sb = new StringBuilder();
	    for (int j = 0; j < i; j++)
	      sb.append(s);
	    return sb.toString();
	  }
	
	
	public abstract Map<String, List<String>> execute();

	public double[] getfMipsPrice() {
		return fMipsPrice;
	}

	public double[] getfRamPrice() {
		return fRamPrice;
	}

	public double[] getfMemPrice() {
		return fMemPrice;
	}

	public double[] getfBwPrice() {
		return fBwPrice;
	}

	public String[] getfName() {
		return fName;
	}

	public double[] getfMips() {
		return fMips;
	}

	public double[] getfRam() {
		return fRam;
	}

	public double[] getfMem() {
		return fMem;
	}

	public double[] getfBw() {
		return fBw;
	}

	public String[] getmName() {
		return mName;
	}

	public double[] getmMips() {
		return mMips;
	}

	public double[] getmRam() {
		return mRam;
	}

	public double[] getmMem() {
		return mMem;
	}

	public double[] getmBw() {
		return mBw;
	}

	public PowerModel[] getfPwModel() {
		return fPwModel;
	}
	
}
