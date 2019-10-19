package org.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.fog.application.AppEdge;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.placement.Controller;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.NetworkMonitor;
import org.fog.utils.ProcessorMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.communication.MobilePathLossModel;
import org.fog.utils.movement.Movement;
import org.fog.utils.movement.StaticMovement;

/**
 * Class representing fog devices.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class FogDevice extends PowerDatacenter {
	
	/** List containing all deployed modules within it */
	private List<String> deployedModules;
	
	/** List of all tuples queues (one per connection/link) */
	private Map<Integer, Queue<Pair<Tuple, Integer>>> tupleQueue;
	
	/** List which holds if the links are available or not (one per connection/link) */
	private Map<Integer, Boolean> tupleLinkBusy;
	
	/** Percentage of processing resources that were in use in the previous resource usage analyze */
	private double lastMipsUtilization;
	
	/** Percentage of memory resources that were in use in the previous resource usage analyze */
	private double lastRamUtilization;
	
	/** Percentage of storage resources that were in use in the previous resource usage analyze */
	private double lastStorageUtilization;
	
	/** Percentage of network resources that were in use in the previous resource usage analyze */
	private double lastBwUtilization;
	
	/** List which holds all its fixed neighbors (i.e., connected nodes with fixed communications). For mobile nodes this list is empty */
	private List<Integer> fixedNeighborsIds;
	
	/** List which holds the latencies to all its neighbors (i.e., connected nodes with any type of communication) */
	private Map<Integer, Double> latencyMap;
	
	/** List which holds the bandwidth to all its neighbors (i.e., connected nodes with any type of communication) */
	private Map<Integer, Double> bandwidthMap;
	
	/** Map which holds a pair of source/destination module name and the next hop to where the tuple will be forwarded */
	private Map<Map<String, String>, Integer> tupleRoutingTable;
	
	/** Map which holds the module name (virtual machine name) and the next hop to where it will be forwarded */
	private Map<String, Integer> vmRoutingTable;
	
	/** Time at it was performed the last resource usage analyze */
	private double lastUtilizationUpdateTime;
	
	/** The total energy consumption during the simulation */
	private double energyConsumption;
	
	/** The total monetary cost during the simulation */
	private double totalCost;
	
	/** Object representing the master controller */
	private Controller controller;
	
	/** Object representing its movement */
	private Movement movement;
	
	/** Object which holds the CPU tuple queue as well as the counters for the ordered and processed MIs */
	private ProcessorMonitor processorMonitor;
	
	/** Queue (FIFO) which holds all VMs to be migrated */
	private Queue<SimEvent> scheduleMigrationList;

	/**
	 * Creates a new fog device.
	 * 
	 * @param name the name of the fog device
	 * @param characteristics the characteristics of the fog device (e.g., its resources and prices)
	 * @param vmAllocationPolicy the virtual machine allocation policy of the fog device
	 * @param storageList the storage list of the fog device
	 * @param schedulingInterval the scheduling interval of the fog device
	 * @param movement the movement of the fog device (i.e., position, velocity and direction)
	 * @throws Exception if the total number of PEs is zero
	 * 
	 */
	public FogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
			double schedulingInterval, Movement movement) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		
		deployedModules = new ArrayList<String>();
		setFixedNeighborsIds(new ArrayList<Integer>());
		setLatencyMap(new HashMap<Integer, Double>());
		setBandwidthMap(new HashMap<Integer, Double>());
		setTupleRoutingTable(new HashMap<Map<String,String>, Integer>());
		setVmRoutingTable(new HashMap<String, Integer>());
		setTupleQueue(new HashMap<Integer, Queue<Pair<Tuple,Integer>>>());
		setTupleLinkBusy(new HashMap<Integer, Boolean>());
		setProcessorMonitor(new ProcessorMonitor());
		scheduleMigrationList = new LinkedList<SimEvent>();
		
		setVmAllocationPolicy(vmAllocationPolicy);
		setSchedulingInterval(schedulingInterval);
		setCharacteristics(characteristics);
		setVmList(new ArrayList<Vm>());
		setStorageList(storageList);
		
		setMovement(movement);
		
		for (Host host : getCharacteristics().getHostList())
			host.setDatacenter(this);
		
		// If this resource doesn't have any PEs then no useful at all
		if (getCharacteristics().getNumberOfPes() == 0)
			throw new Exception(super.getName() + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
		
		// stores id of this class
		getCharacteristics().setId(super.getId());
	}
	
	/**
	 * Processes the events that can occur in a fog node.
	 */
	@Override
	protected void processOtherEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ARRIVAL:
			processTupleArrival(ev);
			break;
		case FogEvents.LAUNCH_MODULE:
			processModuleArrival(ev);
			break;
		case FogEvents.SEND_PERIODIC_TUPLE:
			sendPeriodicTuple(ev);
			break;
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.UPDATE_TUPLE_QUEUE:
			updateTupleQueue(ev);
			break;
		case FogEvents.RESOURCE_MGMT:
			manageResources(ev);
			break;
		case FogEvents.UPDATE_PERIODIC_MOVEMENT:
			updatePeriodicMovement();
			break;
		case FogEvents.CONNECTION_LOST:
			removeLink((Integer) ev.getData());
			break;
		case FogEvents.MIGRATION:
			scheduleMigration(ev);
			break;
		case FogEvents.FINISH_MIGRATION:
			finishMigration(ev);
			break;
		case FogEvents.FINISH_SETUP_MIGRATION:
			finishSetupMigration(ev);
			break;
		default:
			break;
		}
	}
	
	/**
	 * Perform miscellaneous resource management tasks.
	 * 
	 * @param ev the event that just occurred
	 */
	private void manageResources(SimEvent ev) {
		updateEnergyConsumption();
		send(getId(), Constants.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
	}
	
	/**
	 * Sending periodic tuple for an application edge. Note that for multiple instances of a single source module, number of tuples are sent.
	 * 
	 * @param ev SimEvent instance containing the edge to send tuple on
	 */
	protected void sendPeriodicTuple(SimEvent ev) {
		AppEdge edge = (AppEdge)ev.getData();
		String srcModuleName = edge.getSource();
		String dstModuleName = edge.getDestination();
		AppModule srcModule = getModuleByName(srcModuleName);
		AppModule dstModule = getModuleByName(dstModuleName);
		
		if(srcModule == null || dstModule == null) return;
		
		boolean found = false;
		for(Map<String, String> map : tupleRoutingTable.keySet()) {
			if(map.containsValue(edge.getDestination()) || map.containsValue(edge.getSource())) {
				found = true;
			}
		}
		
		if(!deployedModules.contains(dstModuleName) && !found) {
			return;
		}
		
		Tuple tuple = controller.getApplications().get(srcModule.getAppId()).createTuple(edge, dstModule.getUserId());
		
		// If it is the source a given application loop
		List<String> path = new ArrayList<String>();
		path.add(edge.getSource());
		
		if(controller.getApplications().get(srcModule.getAppId()).isLoop(path, edge.getDestination())) {
			tuple.getPathMap().put(path, CloudSim.clock());
		}
		
		sendToSelf(tuple);
		TimeKeeper.getInstance().tupleStartedTransmission(tuple);
		
		send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
	}
	
	/**
	 * Update cloudet processing without scheduling future events.
	 * 
	 * @return the double
	 */
	protected double updateCloudetProcessingWithoutSchedulingFutureEventsForce() {
		double currentTime = CloudSim.clock();
		double minTime = Double.MAX_VALUE;

		for (PowerHost host : this.<PowerHost> getHostList()) {
			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
			
			if (time < minTime)
				minTime = time;
		}
		
		checkCloudletCompletion();
		setLastProcessTime(currentTime);
		return minTime;
	}
	
	/**
	 * Checks whether the cloudlet has already finished the execution of the tuple.
	 */
	protected void checkCloudletCompletion() {
		boolean cloudletCompleted = false;
		List<? extends Host> list = getVmAllocationPolicy().getHostList();
		for (int i = 0; i < list.size(); i++) {
			Host host = list.get(i);
			
			List<Vm> vms = new ArrayList<Vm>();
			vms.addAll(host.getVmList());
			
			for (Vm vm : vms) {
				while (vm.getCloudletScheduler().isFinishedCloudlets()) {
					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
					if (cl != null) {
						cloudletCompleted = true;
						Tuple tuple = (Tuple) cl;
						
						if(Config.PRINT_DETAILS)
							FogComputingSim.print("[" + getName() + "] Completed execution of tuple w/ tupleId: " + tuple.getCloudletId() + " on " + tuple.getDestModuleName());
						
						TimeKeeper.getInstance().tupleEndedExecution(tuple);
						Application application = controller.getApplications().get(tuple.getAppId());
						List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, vm.getId());
						for(Tuple resTuple : resultantTuples) {
							resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
							resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
							
							for(List<String> path : tuple.getPathMap().keySet()) {								
								// The application module which has processed the tuple belongs to the loop path
								if(application.isLoop(path, tuple.getDestModuleName())) {
									double initalTime = tuple.getPathMap().get(path);
									
									List<String> newPath = new ArrayList<String>();
									newPath.addAll(path);
									newPath.add(tuple.getDestModuleName());
									
									resTuple.getPathMap().put(newPath, initalTime);
									
									// If it is the last module in the loop
									if(application.finalLoop(newPath) != -1) {
										TimeKeeper.getInstance().finishedLoop(newPath, CloudSim.clock() - initalTime);
									}
								}
							}
							
							List<String> path = new ArrayList<String>();
							path.add(tuple.getDestModuleName());
							
							// If it is the source a given application loop
							if(application.isLoop(path, resTuple.getDestModuleName())) {
								resTuple.getPathMap().put(path, CloudSim.clock());
							}
							
							sendToSelf(resTuple);
							TimeKeeper.getInstance().tupleStartedTransmission(resTuple);
						}
						
						if(resultantTuples == null || resultantTuples.isEmpty()) {
							for(List<String> path : tuple.getPathMap().keySet()) {
								List<String> newPath = new ArrayList<String>();
								newPath.addAll(path);
								newPath.add(tuple.getDestModuleName());
								
								double initalTime = tuple.getPathMap().get(path);
								
								if(application.finalLoop(newPath) != -1) {
									TimeKeeper.getInstance().finishedLoop(newPath, CloudSim.clock() - initalTime);
								}
							}
						}
						
						sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
					}
				}
			}
		}
		
		if(cloudletCompleted) {
			updateAllocatedMips(null);
			updateCPUTupleQueue(null);
		}
	}
	
	/**
	 * Updates the allocated processing resources.
	 * 
	 * @param incomingOperator the application module name
	 */
	private void updateAllocatedMips(String incomingOperator) {
		updateEnergyConsumption();
		
		getHost().getVmScheduler().deallocatePesForAllVms();
		for(final Vm vm : getHost().getVmList()) {
			if(vm.getCloudletScheduler().runningCloudlets() > 0 || ((AppModule)vm).getName().equals(incomingOperator)){
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) getHost().getTotalMips());}});
			}else{
				getHost().getVmScheduler().allocatePesForVm(vm, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add(0.0);}});
			}
		}
		
		updateEnergyConsumption();
	}
	
	/**
	 * Updates the energy consumption and the monetary cost at the node.
	 */
	private void updateEnergyConsumption() {
		double totalMipsAllocated = 0;
		double totalRamAllocated = 0;
		double totalStorageAllocated = 0;
		double totalBwAllocated = 0;
		double totalBwAvailable = 0;
		
		for(final Vm vm : getHost().getVmList()){
			AppModule operator = (AppModule)vm;
			operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler().getAllocatedMipsForVm(operator));
			
			double allocatedMipsForVm = getHost().getTotalAllocatedMipsForVm(vm);
			
			if(Config.PRINT_DETAILS)
				FogComputingSim.print("[" + getName() + "] number of allocated mips for vm: " + operator.getName() + " is " + allocatedMipsForVm);
			
			totalMipsAllocated += allocatedMipsForVm;
			totalStorageAllocated += ((AppModule)vm).getSize();
			
			if(allocatedMipsForVm != 0)
				totalRamAllocated += ((AppModule)vm).getCurrentAllocatedRam();
		}
		
		for(int neighborId : tupleLinkBusy.keySet()) {			
			totalBwAvailable += getBandwidthMap().get(neighborId);
			
			if(!tupleLinkBusy.get(neighborId)) continue;
			totalBwAllocated += getBandwidthMap().get(neighborId);
		}
		
		double timeNow = CloudSim.clock();
		double timeDif = timeNow-lastUtilizationUpdateTime;
		processorMonitor.addProcessedMI((long) (timeDif*lastMipsUtilization*getHost().getTotalMips()));
		
		if(processorMonitor.getProcessedMI() > processorMonitor.getOrderedMI()) {
			FogComputingSim.err("[" + getName() + "] Processed MI is greater than Ordered MI");
		}
		
		double energyConsumption = timeDif*getHost().getPowerModel().getPower(lastMipsUtilization);
		
		// If its a mobile node, we apply the mobile energy consumption model
		if(!isStaticNode()) {
			for(int neighborId : tupleLinkBusy.keySet()) {
				if(!tupleLinkBusy.get(neighborId)) continue;
				energyConsumption += timeDif*MobilePathLossModel.TX_POWER;
			}
		}
		
		setEnergyConsumption(getEnergyConsumption() + energyConsumption);
		
		FogDeviceCharacteristics characteristics = (FogDeviceCharacteristics) getCharacteristics();
		
		double newcost = getTotalCost();
		newcost += timeDif*lastMipsUtilization*getHost().getTotalMips()*characteristics.getCostPerMips();
		newcost += timeDif*lastRamUtilization*getHost().getRam()*characteristics.getCostPerMem();
		newcost += timeDif*lastStorageUtilization*getHost().getStorage()*characteristics.getCostPerStorage();
		newcost += timeDif*lastBwUtilization*totalBwAvailable*characteristics.getCostPerBw();
		newcost += energyConsumption*characteristics.getCostPerPower();
		newcost += timeDif*characteristics.getCostPerSecond();
		setTotalCost(newcost);
		
		lastRamUtilization = totalRamAllocated/getHost().getRam();
		lastBwUtilization = totalBwAllocated/totalBwAvailable;
		lastStorageUtilization = totalStorageAllocated/getHost().getStorage();
		lastMipsUtilization = totalMipsAllocated/getHost().getTotalMips();
		lastUtilizationUpdateTime = timeNow;
		
		if(Config.PRINT_COST_DETAILS) printCost();
	}
	
	/**
	 * Processes the event of application submit.
	 * 
	 * @param ev the event that just occurred
	 */
	private void processAppSubmit(SimEvent ev) {
		Application app = (Application)ev.getData();
		controller.getApplications().put(app.getAppId(), app);
	}
	
	/**
	 * Processes the arrival of a tuple.
	 * 
	 * @param ev the event that just occurred containing the tuple
	 */
	protected void processTupleArrival(SimEvent ev) {
		Tuple tuple = (Tuple)ev.getData();
		
		if(tuple instanceof TupleVM) {
			Map<AppModule, Integer> vmPosition = new HashMap<AppModule, Integer>();
			
			TupleVM tmp = (TupleVM) tuple;
			Integer nextHopId = vmRoutingTable.get(tmp.getVm().getName());
			
			// Final node
			if(nextHopId == null) {
				Map<Application, AppModule> map = new HashMap<Application, AppModule>();
				map.put(tmp.getApplication(), tmp.getVm());
				
				send(getId(), 0, FogEvents.FINISH_MIGRATION, tmp.getVm());
				TimeKeeper.getInstance().receivedTuple(tuple);
			}else {
				if(Config.PRINT_DETAILS)
					FogComputingSim.print("[" + getName() + "] received and is forwarding the vm: " + tmp.getVm().getName() + " to: " + vmRoutingTable.get(tmp.getVm().getName()));
				
				vmPosition.put(tmp.getVm(), vmRoutingTable.get(tmp.getVm().getName()));
				sendNow(controller.getId(), FogEvents.UPDATE_VM_POSITION, vmPosition);
				
				sendTo(tmp, vmRoutingTable.get(tmp.getVm().getName()));
			}
			return;
		}
		
		if(Config.PRINT_DETAILS)
			FogComputingSim.print("[" + getName() + "] received tuple w/ destiny module: " + tuple.getDestModuleName());
		
		Map<String, String> communication = new HashMap<String, String>();
		communication.put(tuple.getSrcModuleName(), tuple.getDestModuleName());
		
		// It can be null after some handover is completed. This can happen because, some connections were removed and routing
		// tables were updated. Thus, once there still can exist some old tuples, they will be lost because we already don't
		// know where to forward it.		
		if((!tupleRoutingTable.containsKey(communication) && !deployedModules.contains(tuple.getDestModuleName())) || moduleIsUnavailable(tuple.getDestModuleName())) {
			if(Config.PRINT_DETAILS)
				FogComputingSim.print("[" + getName() + "] Is rejecting tuple w/ destiny: " + tuple.getDestModuleName());
			
			NetworkMonitor.incrementPacketDrop();
			TimeKeeper.getInstance().lostTuple(tuple);
			return;
		}		

		if(deployedModules.contains(tuple.getDestModuleName())) {
			int vmId = -1;			
			for(Vm vm : getHost().getVmList()) {
				if(((AppModule)vm).getName().equals(tuple.getDestModuleName())) {
					vmId = vm.getId();
				}
			}
				
			if(vmId < 0 || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) && 
					tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId))
				return;
			
			tuple.setVmId(vmId);
			updateCPUTupleQueue(ev);
			NetworkMonitor.incrementPacketSuccess();
			TimeKeeper.getInstance().receivedTuple(tuple);
		}else {
			communication = new HashMap<String, String>();
			communication.put(tuple.getSrcModuleName(), tuple.getDestModuleName());
			
			Integer nexHopId = tupleRoutingTable.get(communication);
			
			if(Config.PRINT_DETAILS)
				FogComputingSim.print("[" + getName() + "] Is forwarding tuple w/ destiny module: " + tuple.getDestModuleName() +
						" to: " + controller.getFogDeviceById(nexHopId).getName() + " w/ tupleId: " + tuple.getCloudletId());
			
			sendTo(tuple, nexHopId);
		}
	}
	
	/**
	 * Processes the event of module arrival.
	 * 
	 * @param ev the event that just occurred containing the application module
	 */
	private void processModuleArrival(SimEvent ev) {
		AppModule module = (AppModule)ev.getData();
		
		deployedModules.add(module.getName());
		processVmCreate(ev, false);
		FogComputingSim.print("Creating " + module.getName() + " on device " + getName());
		module.setBeingInstantiated(false);
		initializePeriodicTuples(module);
		
		getHost().getVmScheduler().allocatePesForVm(module, new ArrayList<Double>(){
			protected static final long serialVersionUID = 1L;
		{add(0.0);}});
	}
	
	/**
	 * Initializes periodic tuples between different application modules with a given application module source name.
	 * 
	 * @param module the application module name
	 */
	private void initializePeriodicTuples(AppModule module) {
		String appId = module.getAppId();
		Application app = controller.getApplications().get(appId);
		List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
		
		for(AppEdge edge : periodicEdges) {
			send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
		}
	}
	
	/**
	 * Updates the state of the tuple queue. If there still exist some tuple inside the waiting list, sent it.
	 * Otherwise, just update the energy consumption and resource usage.
	 * 
	 * @param ev the event that just occurred containing the link destination id
	 */
	private void updateTupleQueue(SimEvent ev){
		Integer destId = (Integer)ev.getData();
		
		// It can be null after some handover is completed. This can happen because, some connections were removed and routing
		// tables were updated. Thus, once there still can exist some old tuples, they will be lost because we already don't
		// know where to forward it.
		if(getTupleQueue().get(destId) != null && !getTupleQueue().get(destId).isEmpty()){
			Pair<Tuple, Integer> pair = getTupleQueue().get(destId).poll();
			sendFreeLink(pair.getFirst(), pair.getSecond());
		}else {
			if(getTupleLinkBusy().containsKey(destId)) {
				getTupleLinkBusy().put(destId, false);
				updateEnergyConsumption();
			}
		}
	}
	
	/**
	 * Sends a new tuple into the link with a given link destination id.
	 * 
	 * @param tuple the tuple to be sent
	 * @param destId the link destination id
	 */
	private void sendFreeLink(Tuple tuple, int destId){		
		updateEnergyConsumption();
		getTupleLinkBusy().put(destId, true);
		
		double latency = getLatencyMap().get(destId);
		double bandwidth = getBandwidthMap().get(destId);
		double networkDelay = (double)tuple.getCloudletFileSize()/bandwidth;
		
		send(getId(), networkDelay, FogEvents.UPDATE_TUPLE_QUEUE, destId);
		send(destId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
		
		updateEnergyConsumption();
		NetworkMonitor.sendingTuple(tuple, getId(), destId);
	}
	
	/**
	 * Checks whether a given link is free. If it is, the send the tuple, otherwise put it in the waiting list.
	 * 
	 * @param tuple the tuple to be sent
	 * @param destId the link destination id
	 */
	protected void sendTo(Tuple tuple, int id) {
		if(!getTupleLinkBusy().get(id))
			sendFreeLink(tuple, id);
		else
			getTupleQueue().get(id).add(new Pair<Tuple, Integer>(tuple, id));
	}

	/**
	 * Sends a tuple to it self.
	 * 
	 * @param tuple the tuple to be sent
	 */
	protected void sendToSelf(Tuple tuple){
		sendNow(getId(), FogEvents.TUPLE_ARRIVAL, tuple);
	}
	
	/**
	 * Executes the next tuple in the list if the CPU is not busy.
	 * 
	 * @param ev the event that just occurred containing the tuple
	 * @param moduleName the module name (virtual machine) where it will be processed
	 */
	private void updateCPUTupleQueue(SimEvent ev) {
		if(ev != null) {
			Tuple tuple = (Tuple) ev.getData();
			processorMonitor.addTupleToQueue(ev);
			TimeKeeper.getInstance().tupleStartedExecution(tuple);
		}else {
			processorMonitor.setCPUBusy(false);
			performScheduledMigrations();
		}
		
		if(!processorMonitor.isCPUBusy() && !processorMonitor.isEmptyTupleQueue()){
			processorMonitor.setCPUBusy(true);
			
			ev = processorMonitor.getPopTupleFromQueue();
			Tuple tuple = (Tuple) ev.getData();
			
			if(!deployedModules.contains(tuple.getDestModuleName())) {
				if(Config.PRINT_DETAILS)
					System.out.println("["+getName()+"] is rejecting tuple to module: " + tuple.getDestModuleName());
				
				NetworkMonitor.incrementPacketDrop();
				TimeKeeper.getInstance().lostTuple(tuple);
				updateCPUTupleQueue(null);
				return;
			}
			
			if(Config.PRINT_DETAILS)
				FogComputingSim.print("[" + getName() + "] Started execution of tuple w/ tupleId: " + tuple.getCloudletId() + " on " + tuple.getDestModuleName() + " size: " + tuple.getCloudletLength());
			
			processorMonitor.addOrderedMI(((Cloudlet)ev.getData()).getCloudletLength());
			
			AppModule dstModule = getModuleByName(tuple.getDestModuleName());
			tuple.setUserId(dstModule.getUserId());
			
			updateAllocatedMips(tuple.getDestModuleName());
			processCloudletSubmit(ev, false);
			updateAllocatedMips(tuple.getDestModuleName());
			
		}else if(processorMonitor.isEmptyTupleQueue()) {
			processorMonitor.setCPUBusy(false);
		}
	}
	
	/**
	 * Updates its position following its defined movement.
	 */
	private void updatePeriodicMovement() {
		movement.updateLocation();
		send(getId(), Config.PERIODIC_MOVEMENT_UPDATE, FogEvents.UPDATE_PERIODIC_MOVEMENT);
	}
	
	/**
	 * Removes a connection to a node.
	 * 
	 * @param id the id which the connection will be removed
	 */
	private void removeLink(int id) {
		getLatencyMap().remove(id);
		getBandwidthMap().remove(id);
		getTupleQueue().remove(id);
		getTupleLinkBusy().remove(id);
		
		if(Config.PRINT_DETAILS)
			FogComputingSim.print("Removing connection between: " + getName() +  " -> " + controller.getFogDeviceById(id).getName());

		// Clean unused entries from routing table
		Map<Map<String, String>, Integer> newTable = new HashMap<Map<String,String>, Integer>();
		
		for(Map<String, String> map : tupleRoutingTable.keySet()) {
			if(tupleRoutingTable.get(map) != id) {
				newTable.put(map, tupleRoutingTable.get(map));
			}
		}
		
		tupleRoutingTable = newTable;
		
		NetworkMonitor.removeConnection(getId(), id);
	}
	
	/**
	 * Adds a new VM migration to the queue.
	 * 
	 * @param ev the event which contains the migration information
	 */
	private void scheduleMigration(SimEvent ev) {
		scheduleMigrationList.add(ev);
		if(processorMonitor.isCPUBusy()) return;
		performScheduledMigrations();
	}
	
	/**
	 * Performs all migration inside the queue.
	 */
	@SuppressWarnings("unchecked")
	private void performScheduledMigrations() {
		while(!scheduleMigrationList.isEmpty()) {
			Map<FogDevice, Map<Application, AppModule>> map = (Map<FogDevice, Map<Application, AppModule>>)scheduleMigrationList.poll().getData();
			Map<Application, AppModule> appMap = map.entrySet().iterator().next().getValue();
			Application application = appMap.entrySet().iterator().next().getKey();
			AppModule vm = appMap.entrySet().iterator().next().getValue();
			FogDevice to = map.entrySet().iterator().next().getKey();
			
			if(vmRoutingTable.containsKey(vm.getName())) {
				Map<String, Object> migrate = new HashMap<String, Object>();
				migrate.put("vm", vm);
				migrate.put("host", to.getHost());
				
				double totalSize = vm.getRam() + vm.getSize();
				
				vm.setInMigration(true);
				getVmAllocationPolicy().deallocateHostForVm(vm);
				getHost().getVmList().remove(vm);
				getVmList().remove(vm);
				deployedModules.remove(vm.getName());
				
				TupleVM tuple = new TupleVM(application.getAppId(), FogUtils.generateTupleId(), 0, 1, (long) totalSize, 0,
						new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull(), vm, application);
				tuple.setActualTupleId(TimeKeeper.getInstance().getUniqueId());
				tuple.setTupleType(vm.getName());
				
				sendTo(tuple, vmRoutingTable.get(vm.getName()));
				TimeKeeper.getInstance().tupleStartedTransmission(tuple);
				
				if(Config.PRINT_DETAILS)
					FogComputingSim.print("[" + getName() + "] started the migration of the vm: " + vm.getName() + " toward the machine: " + to.getName());
				
				Map<AppModule, Integer> vmPosition = new HashMap<AppModule, Integer>();
				vmPosition.put(vm, vmRoutingTable.get(vm.getName()));
				sendNow(controller.getId(), FogEvents.UPDATE_VM_POSITION, vmPosition);
				
				updateEnergyConsumption();
			}
		}
	}
	
	/**
	 * Indicates the end of the application module migration.
	 * 
	 * @param ev the event that just occurred containing the application module
	 */
	private void finishMigration(SimEvent ev) {
		AppModule vm = (AppModule)ev.getData();
		getHost().removeMigratingInVm(vm);
		
		if (!getVmAllocationPolicy().allocateHostForVm(vm, getHost()))
			FogComputingSim.err("VM allocation to the destination host failed");
		
		deployedModules.add(vm.getName());
		vm.setInMigration(false);
		vm.setBeingInstantiated(true);
		getHost().deallocatePesForVm(vm);
		
		if(Config.PRINT_DETAILS)
			FogComputingSim.print("[" + getName() + "] received and is deploying vm: " + vm.getName());
		
		send(getId(), Config.SETUP_VM_TIME, FogEvents.FINISH_SETUP_MIGRATION, vm);
	}
	
	/**
	 * Indicates the end of the application module setup.
	 * 
	 * @param ev the event that just occurred containing the application module
	 */
	private void finishSetupMigration(SimEvent ev) {
		AppModule vm = (AppModule)ev.getData();
		
		// If VM was not migrated again during the setup time, set it available. Otherwise, do nothing.
		if(deployedModules.contains(vm.getName())) {
			vm.setBeingInstantiated(false);
			initializePeriodicTuples(vm);
			
			if(Config.PRINT_DETAILS)
				FogComputingSim.print("[" + getName() + "] just fished the setup of the vm: " + vm.getName() + " and its now available");
		}
	}
	
	/**
	 * Checks whether the module is available or not.
	 * 
	 * @param name the application module name
	 * @return true if it is unavailable (its in neither migration or setup mode), otherwise false
	 */
	private boolean moduleIsUnavailable(String name) {
		for(Vm vm : getHost().getVmList()) {
			if(((AppModule)vm).getName().equals(name)) {
				if(vm.isInMigration() || vm.isBeingInstantiated())
					return true;
				else
					return false;
			}
		}
		return false;
	}
	
	/**
	 * Gets application module by its name.
	 * 
	 * @param moduleName the name of the application module
	 * @return the application module; can be null
	 */
	protected AppModule getModuleByName(String moduleName) {
		AppModule module = null;
		for(FogDevice fogDevice : controller.getFogDevices()) {
			for(Vm vm : fogDevice.getHost().getVmList()){
				if(((AppModule)vm).getName().equals(moduleName)) {
					module = (AppModule)vm;
					break;
				}
			}
		}
		
		return module;
	}
	
	/**
	 * Prints a detailed report related to the current resource usage and costs.
	 */
	private void printCost() {
		System.out.println("\n================================================================================");
		System.out.println("Resource usage report of " + getName() + ":\n");
		System.out.println("MIPS   	(%): " + String.format( "%.3f", lastMipsUtilization));
		System.out.println("RAM    	(%): " + String.format( "%.3f", lastRamUtilization));
		System.out.println("STRG   	(%): " + String.format( "%.3f", lastStorageUtilization));
		System.out.println("BW     	(%): " + String.format( "%.3f", lastBwUtilization));
		System.out.println("Cost    (€): " + String.format( "%.3f", getTotalCost()));
		System.out.println("Energy 	(W): " + String.format( "%.3f", getEnergyConsumption()));
		System.out.println("================================================================================\n");
	}
	
	/**
	 * Gets the host.
	 * 
	 * @return the host
	 */
	public PowerHost getHost() {
		return (PowerHost) getHostList().get(0);
	}
	
	/**
	 * Gets the energy consumption value.
	 * 
	 * @return the energy consumption value
	 */
	public double getEnergyConsumption() {
		return energyConsumption;
	}

	/**
	 * Sets the energy consumption value.
	 * 
	 * @param energyConsumption the energy consumption value
	 */
	public void setEnergyConsumption(double energyConsumption) {
		this.energyConsumption = energyConsumption;
	}
	
	/**
	 * Gets the latency map.
	 * 
	 * @return the latency map
	 */
	public Map<Integer, Double> getLatencyMap() {
		return latencyMap;
	}
	
	/**
	 * Sets the latency map.
	 * 
	 * @param latencyMap the latency map
	 */
	public void setLatencyMap(Map<Integer, Double> latencyMap) {
		this.latencyMap = latencyMap;
	}
	
	/**
	 * Gets the bandwidth map.
	 * 
	 * @return the bandwidth map
	 */
	public Map<Integer, Double> getBandwidthMap() {
		return bandwidthMap;
	}
	
	/**
	 * Sets the bandwidth map.
	 * 
	 * @param bandwidthMap the bandwidth map
	 */
	public void setBandwidthMap(Map<Integer, Double> bandwidthMap) {
		this.bandwidthMap = bandwidthMap;
	}
	
	/**
	 * Gets the total cost.
	 * 
	 * @return the total cost
	 */
	public double getTotalCost() {
		return totalCost;
	}
	
	/**
	 * Sets the total cost.
	 * 
	 * @param totalCost the total cost
	 */
	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}
	
	/**
	 * Sets the controller.
	 * 
	 * @param controller the controller
	 */
	public void setController(Controller controller) {
		this.controller = controller;
		
		for(int neighborId : latencyMap.keySet()) {
			getFixedNeighborsIds().add(neighborId);
		}
		
		if(isStaticNode() && !(movement instanceof StaticMovement))
			FogComputingSim.err("Node cannot have wired connections and have a dynamic movement - use static movement instead");
		
		if(Config.DYNAMIC_SIMULATION)
			updatePeriodicMovement();
	}
	
	/**
	 * Gets the controller.
	 * 
	 * @return the controller
	 */
	public Controller getController() {
		return controller;
	}
	
	/**
	 * Gets the tuple routing table.
	 * 
	 * @return the tuple routing table
	 */
	public Map<Map<String, String>, Integer> getTupleRoutingTable() {
		return tupleRoutingTable;
	}
	
	/**
	 * Sets the tuple routing table.
	 * 
	 * @param tupleRoutingTable the tuple routing table
	 */
	public void setTupleRoutingTable(Map<Map<String, String>, Integer> tupleRoutingTable) {
		this.tupleRoutingTable = tupleRoutingTable;
	}
	
	/**
	 * Gets the tuple queue.
	 * 
	 * @return the tuple queue
	 */
	public Map<Integer, Queue<Pair<Tuple, Integer>>> getTupleQueue() {
		return tupleQueue;
	}
	
	/**
	 * Sets the tuple queue.
	 * 
	 * @param tupleQueue the tuple queue
	 */
	public void setTupleQueue(Map<Integer, Queue<Pair<Tuple, Integer>>> tupleQueue) {
		this.tupleQueue = tupleQueue;
	}
	
	/**
	 * Gets the tuple link busy.
	 * 
	 * @return the tuple link busy
	 */
	public Map<Integer, Boolean> getTupleLinkBusy() {
		return tupleLinkBusy;
	}
	
	/**
	 * Sets the tuple link busy.
	 * 
	 * @param tupleLinkBusy the tuple link busy
	 */
	public void setTupleLinkBusy(Map<Integer, Boolean> tupleLinkBusy) {
		this.tupleLinkBusy = tupleLinkBusy;
	}
	
	/**
	 * Gets the movement.
	 * 
	 * @return the movement
	 */
	public Movement getMovement() {
		return movement;
	}
	
	/**
	 * Sets the movement.
	 * 
	 * @param movement the movement
	 */
	public void setMovement(Movement movement) {
		this.movement = movement;
	}
	
	/**
	 * Gets the fixed neighbors ids.
	 * 
	 * @return the fixed neighbors ids
	 */
	public List<Integer> getFixedNeighborsIds() {
		return fixedNeighborsIds;
	}
	
	/**
	 * Sets the fixed neighbors ids.
	 * 
	 * @param fixedNeighborsIds the fixed neighbors ids
	 */
	public void setFixedNeighborsIds(List<Integer> fixedNeighborsIds) {
		this.fixedNeighborsIds = fixedNeighborsIds;
	}
	
	/**
	 * Gets the virtual machine routing table.
	 * 
	 * @return the virtual machine routing table
	 */
	public Map<String, Integer> getVmRoutingTable() {
		return vmRoutingTable;
	}
	
	/**
	 * Sets the virtual machine routing table.
	 * 
	 * @param vmRoutingTable the virtual machine routing table
	 */
	public void setVmRoutingTable(Map<String, Integer> vmRoutingTable) {
		this.vmRoutingTable = vmRoutingTable;
	}
	
	/**
	 * Checks whether its a static node.
	 * 
	 * @return true if it is, otherwise false
	 */
	public boolean isStaticNode() {
		return !getFixedNeighborsIds().isEmpty();
	}
	
	/**
	 * Gets the object which holds the CPU tuple queue as well as the counters for the ordered and processed MIs.
	 * 
	 * @return the object which holds the CPU tuple queue as well as the counters for the ordered and processed MIs
	 */
	public ProcessorMonitor getProcessorMonitor() {
		return processorMonitor;
	}
	
	/**
	 * Sets the object which holds the CPU tuple queue as well as the counters for the ordered and processed MIs.
	 * 
	 * @param processorMonitor the object which holds the CPU tuple queue as well as the counters for the ordered and processed MIs
	 */
	public void setProcessorMonitor(ProcessorMonitor processorMonitor) {
		this.processorMonitor = processorMonitor;
	}
	
}