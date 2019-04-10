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
	protected static final boolean PRINT_DETAILS = true;
	
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
	private double[][] nwSizeMap;
	private double[] mCpuSize;
	private double[][] mandatoryMap;
	private double[][][] bandwidthMap;
	
	public Algorithm(final List<FogDevice> fogDevices, final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) throws IllegalArgumentException {
		
		if(applications == null || sensors == null || actuators == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		extractFogCharacteristics(fogDevices, sensors, actuators);
		extractAppCharacteristics(applications, sensors, actuators);
		
		computeLatencyMap(fogDevices, sensors, actuators);
		computeDependencyMap(applications);
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
			mips.add(1.0); // its value is irrelevant but needs to be different from 0
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
			mips.add(1.0); // its value is irrelevant but needs to be different from 0
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
			System.out.println("\n*******************************************************");
			System.out.println("\t\tFOG NODES CHARACTERISTICS:");
			System.out.println("*******************************************************\n");
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
				System.out.println("LatencymMap: " + fDevice.getLatencyMap());
				
				if(i < fogDevices.size() -1)
					System.out.println();
			}
		}
	}
	
	private void extractAppCharacteristics(final List<Application> applications,
			final List<Sensor> sensors, final List<Actuator> actuators) {		
		ArrayList<String> name = new ArrayList<String>(); 
		ArrayList<Double> mips = new ArrayList<Double>(); 
		ArrayList<Double> ram = new ArrayList<Double>(); 
		ArrayList<Double> mem = new ArrayList<Double>(); 
		ArrayList<Double> bw = new ArrayList<Double>(); 
		Map<String, String> mPositioning = new HashMap<String, String>();
		
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
					
					for(Sensor sensor : sensors)
						if(sensor.getAppId().equals(application.getAppId()))
							mPositioning.put(sensor.getName(), appEdge.getSource());
				}
				
				if(!name.contains(appEdge.getDestination())) {
					name.add(appEdge.getDestination());
					mips.add(0.0);
					ram.add(0.0);
					mem.add(0.0);
					bw.add(0.0);
					
					for(Actuator actuator : actuators)
						if(actuator.getAppId().equals(application.getAppId()))
							mPositioning.put(actuator.getName(), appEdge.getDestination());
				}
			}
		}
		
		mName = name.toArray(new String[name.size()]);
		mMips = convertDoubles(mips);
		mRam = convertDoubles(ram);
		mMem = convertDoubles(mem);
		mBw = convertDoubles(bw);
		mandatoryMap = new double[fName.length][mName.length];
		
		for(String deviceName : mPositioning.keySet()) {
			
			int col = -1, row = -1;
			for(int i = 0; i < mName.length; i++)
				if(mName[i].equals(mPositioning.get(deviceName)))
					col = i;
			
			for(int i = 0; i < fName.length; i++)
				if(fName[i].equals(deviceName))
					row = i;
			
			if(col != -1 && row != -1)
				mandatoryMap[row][col] = 1;
		}
		
		mCpuSize = new double[mName.length];
		for(Application application : applications) {
			for(AppEdge appEdge : application.getEdges()) {
				
				int aux = 0;
				for(int i = 0; i < mName.length; i++)
					if(mName[i].equals(appEdge.getDestination()))
						aux = i;
					
				mCpuSize[aux] += appEdge.getTupleCpuLength();
			}
		}
		
		if(PRINT_DETAILS) {
			System.out.println("\n*******************************************************");
			System.out.println("\t\tAPP MODULES CHARACTERISTICS:");
			System.out.println("*******************************************************\n");
			
			for(int i = 0; i < mName.length; i++) {
				System.out.println("mName: " + getmName()[i]);
				System.out.println("mMips: " + getmMips()[i]);
				System.out.println("mRam: " + getmRam()[i]);
				System.out.println("mMem: " + getmMem()[i]);
				System.out.println("mBw: " + getmBw()[i]);
				System.out.println("mCpuSize: " + getmCpuSize()[i]);
				
				if(i < mName.length -1)
					System.out.println();
			}
			
			System.out.println("\n*******************************************************");
			System.out.println("\t\tMANDATORY POSITIONING:");
			System.out.println("*******************************************************\n");
			
			final String[][] table = new String[fName.length+1][mName.length+1];
			
			table[0][0] = " ";
			for(int i = 0; i < mName.length; i++)
				table[0][i+1] = mName[i];
			
			for(int i = 0; i < fName.length; i++) {
				table[i+1][0] = fName[i];
				
				for(int j = 0; j < mName.length; j++)
					table[i+1][j+1] = Double.toString(mandatoryMap[i][j]);
			}
			
			String repeated = repeate(mName.length, "%17s");
			
			for (final Object[] row : table)
			    System.out.format("%23s" + repeated + "\n", row);
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
		bandwidthMap = new double[fName.length-1][fName.length][fName.length];
		int iter = 0, row = 0, col = 0;
		for(Vertex v1 : dijkstra.getNodes()) {
			iter = 0;
			for(int id : fId) {
				if(Integer.parseInt(v1.getName()) == id) {
					row = iter;
					break;
				}
				iter++;
			}
			
			for(Vertex v2 : dijkstra.getNodes()) {
				iter = 0;
				for(int id : fId) {
					if(Integer.parseInt(v2.getName()) == id) {
						col = iter;
						break;
					}
					iter++;
				}
				
				dijkstra.execute(v1);
				LinkedList<Vertex> path = dijkstra.getPath(v2);
				
				double latency = 0;
				iter = 0;
				if(path != null) {
					for(Vertex v : path) {
						double bandwidth = 0;
						boolean found = false;
						
						for(FogDevice fogDevice : fogDevices) {
							if(Integer.parseInt(v.getName()) == fogDevice.getId()) {
								for(Integer neighborId : fogDevice.getNeighborsIds()) {
									if(iter < path.size() - 1 &&
											Integer.parseInt(path.get(iter+1).getName()) == neighborId) {
										latency += (double) fogDevice.getLatencyMap().get(neighborId);
										bandwidth = (double) 1/fogDevice.getHost().getBw();
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
									bandwidth = 0;
									break;
								}
							}
						}
						
						if(!found) {
							for(Actuator actuator : actuators) {
								if(Integer.parseInt(v.getName()) == actuator.getId()) {
									found = true;
									latency += actuator.getLatency();
									bandwidth = 0;
									break;
								}
							}
						}
						
						if(iter < path.size()-1)
							bandwidthMap[iter][row][col] = bandwidth;
						iter++;
					}
				}
				
				int c = 0;
				int r = 0;
				for(int i = 0; i < fId.length; i++) {
					if(fId[i] == Integer.parseInt(v1.getName()))
						c = i;
					
					if(fId[i] == Integer.parseInt(v2.getName()))
						r = i;
				}
				
				latencyMap[c][r] = latency;
			}
		}
		
		if(PRINT_DETAILS) {
			System.out.println("\n*******************************************************");
			System.out.println("\t\tLATENCY MAP:");
			System.out.println("*******************************************************\n");
			
			String[][] table = new String[fId.length+1][fId.length+1];
			
			table[0][0] = " ";
			for(int i = 0; i < fId.length; i++)
				table[0][i+1] = fName[i];
			
			for(int i = 0; i < fId.length; i++) {
				table[i+1][0] = fName[i];
				
				for(int j = 0; j < fId.length; j++)
					table[i+1][j+1] = Double.toString(latencyMap[i][j]);
			}
			
			String repeated = repeate(fId.length, "%13s");
			
			for (final Object[] r : table)
			    System.out.format("%23s" + repeated + "\n", r);
			
			
			System.out.println("\n*******************************************************");
			System.out.println("\t\tBANDWIDTH MAP:");
			System.out.println("*******************************************************\n");
			
			for(iter = 0; iter < fId.length-1; iter++) {
				table = new String[fId.length+1][fId.length+1];
				
				table[0][0] = " ";
				for(int i = 0; i < fId.length; i++)
					table[0][i+1] = fName[i];
				
				for(int i = 0; i < fId.length; i++) {
					table[i+1][0] = fName[i];
					
					for(int j = 0; j < fId.length; j++)
						table[i+1][j+1] = Double.toString(bandwidthMap[iter][i][j]);
				}
				
				repeated = repeate(fId.length, "%13s");
				
				for (final Object[] r : table)
				    System.out.format("%23s" + repeated + "\n", r);
				
				if(iter < fId.length-2)
					System.out.println();
			}
		}
	}
	
	private void computeDependencyMap(final List<Application> applications) {
		nwSizeMap = new double[mName.length][mName.length];	
		dependencyMap = new double[mName.length][mName.length];
		for(Application application : applications) {
			for(AppEdge appEdge : application.getEdges()) {
				
				int col = 0, row = 0;
				for(int i = 0; i < mName.length; i++) {
					if(mName[i].equals(appEdge.getSource()))
						col = i;
					
					if(mName[i].equals(appEdge.getDestination()))
						row = i;
				}
				
				dependencyMap[col][row] = 1;
				nwSizeMap[col][row] = appEdge.getTupleNwLength();
			}
		}
		
		if(PRINT_DETAILS) {
			System.out.println("\n*******************************************************");
			System.out.println("\t\tDEPENDENCY MAP:");
			System.out.println("*******************************************************\n");
			
			String[][] table = new String[mName.length+1][mName.length+1];
			
			table[0][0] = " ";
			for(int i = 0; i < mName.length; i++)
				table[0][i+1] = mName[i];
			
			for(int i = 0; i < mName.length; i++) {
				table[i+1][0] = mName[i];
				
				for(int j = 0; j < mName.length; j++)
					table[i+1][j+1] = Double.toString(dependencyMap[i][j]);
			}
			
			String repeated = repeate(mName.length, "%17s");
			
			for (final Object[] row : table)
			    System.out.format("%23s" + repeated + "\n", row);
			
			System.out.println("\n*******************************************************");
			System.out.println("\t\tNW SIZE MAP:");
			System.out.println("*******************************************************\n");
			
			table = new String[mName.length+1][mName.length+1];
			
			table[0][0] = " ";
			for(int i = 0; i < mName.length; i++)
				table[0][i+1] = mName[i];
			
			for(int i = 0; i < mName.length; i++) {
				table[i+1][0] = mName[i];
				
				for(int j = 0; j < mName.length; j++)
					table[i+1][j+1] = Double.toString(nwSizeMap[i][j]);
			}
			
			repeated = repeate(mName.length, "%17s");
			
			for (final Object[] row : table)
			    System.out.format("%23s" + repeated + "\n", row);
		}
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
	
	protected static String repeate(int i, String s) {
		StringBuilder sb = new StringBuilder();
		for (int j = 0; j < i; j++)
			sb.append(s);
		return sb.toString();
    }
	
	public int moduleHasMandatoryPositioning(int col) {
		for(int i = 0; i < mandatoryMap.length; i++)
			if(mandatoryMap[i][col] == 1)
				return i;
		return -1;
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
	
	public double[] getmCpuSize(){
		return mCpuSize;
	}

	public PowerModel[] getfPwModel() {
		return fPwModel;
	}
	
	public double[][] getLatencyMap() {
		return latencyMap;
	}
	
	public double[][] getDependencyMap() {
		return dependencyMap;
	}
	
	public double[][] getMandatoryMap() {
		return mandatoryMap;
	}
	
	public double[][] getBandwidthMap(int iter){
		return bandwidthMap[iter];
	}
	
	public double[][] getNwSizeMap(){
		return nwSizeMap;
	}
	
}
