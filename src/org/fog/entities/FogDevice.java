package org.fog.entities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Random;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
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
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.placement.Controller;
import org.fog.core.Config;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.utils.Analysis;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Location;
import org.fog.utils.MobileBandwidthModel;
import org.fog.utils.MobilePathLossModel;
import org.fog.utils.Movement;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

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
			migration(ev);
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
	private void sendPeriodicTuple(SimEvent ev) {
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
		updateTimingsOnSending(tuple);
		sendToSelf(tuple);
		
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
		double timeDiff = currentTime - getLastProcessTime();
		double timeFrameDatacenterEnergy = 0.0;

		for (PowerHost host : this.<PowerHost> getHostList()) {
			Log.printLine();

			double time = host.updateVmsProcessing(currentTime); // inform VMs to update processing
			
			if (time < minTime)
				minTime = time;

			Log.formatLine("%.2f: [Host #%d] utilization is %.2f%%", currentTime, host.getId(),
					host.getUtilizationOfCpu() * 100);
		}

		if (timeDiff > 0) {
			Log.formatLine("\nEnergy consumption for the last time frame from %.2f to %.2f:",
					getLastProcessTime(), currentTime);

			for (PowerHost host : this.<PowerHost> getHostList()) {
				double previousUtilizationOfCpu = host.getPreviousUtilizationOfCpu();
				double utilizationOfCpu = host.getUtilizationOfCpu();
				double timeFrameHostEnergy = host.getEnergyLinearInterpolation(
						previousUtilizationOfCpu, utilizationOfCpu, timeDiff);
				timeFrameDatacenterEnergy += timeFrameHostEnergy;

				Log.printLine();
				Log.formatLine("%.2f: [Host #%d] utilization at %.2f was %.2f%%, now is %.2f%%",
						currentTime, host.getId(), getLastProcessTime(), previousUtilizationOfCpu * 100,
						utilizationOfCpu * 100);
				Log.formatLine("%.2f: [Host #%d] energy is %.2f W*sec", currentTime,
						host.getId(), timeFrameHostEnergy);
			}

			Log.formatLine("\n%.2f: Data center's energy is %.2f W*sec\n", currentTime, timeFrameDatacenterEnergy);
		}

		setPower(getPower() + timeFrameDatacenterEnergy);
		checkCloudletCompletion();
		Log.printLine();

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
			for (Vm vm : host.getVmList()) {
				while (vm.getCloudletScheduler().isFinishedCloudlets()) {
					Cloudlet cl = vm.getCloudletScheduler().getNextFinishedCloudlet();
					
					if (cl != null) {
						cloudletCompleted = true;
						Tuple tuple = (Tuple)cl;
						TimeKeeper.getInstance().tupleEndedExecution(tuple);
						
						Application application = controller.getApplications().get(tuple.getAppId());
						
						if(Config.PRINT_DETAILS)
							FogComputingSim.print("[" + getName() + "] Completed execution of tuple: " + tuple.getCloudletId() + " on " + tuple.getDestModuleName());
						
						List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, vm.getId());
						
						for(Tuple resTuple : resultantTuples){
							resTuple.setModuleCopyMap(new HashMap<String, Integer>(tuple.getModuleCopyMap()));
							resTuple.getModuleCopyMap().put(((AppModule)vm).getName(), vm.getId());
							updateTimingsOnSending(resTuple);
							sendToSelf(resTuple);
						}
						
						sendNow(cl.getUserId(), CloudSimTags.CLOUDLET_RETURN, cl);
					}
				}
			}
		}
		
		if(cloudletCompleted)
			updateAllocatedMips(null);
	}
	
	/**
	 * Updates the timings on sending.
	 * 
	 * @param resTuple the received tuple
	 */
	protected void updateTimingsOnSending(Tuple resTuple) {
		String srcModule = resTuple.getSrcModuleName();
		String destModule = resTuple.getDestModuleName();
		
		for(AppLoop loop : controller.getApplications().get(resTuple.getAppId()).getLoops()){
			if(loop.hasEdge(srcModule, destModule) && loop.isStartModule(srcModule)){
				int tupleId = TimeKeeper.getInstance().getUniqueId();
				resTuple.setActualTupleId(tupleId);
				
				if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
					TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
				
				TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
				TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());
			}
		}
	}
	
	/**
	 * Updates the allocated processing resources.
	 * 
	 * @param incomingOperator the application module name
	 */
	protected void updateAllocatedMips(String incomingOperator) {
		getHost().getVmScheduler().deallocatePesForAllVms();
		for(final Vm vm : getHost().getVmList()){
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
		
		double energyConsumption = timeDif*getHost().getPowerModel().getPower(lastMipsUtilization);
		
		// If its a mobile node, we apply the energy consumption model: Pr = Pt/(4 π d^(γ))
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
		newcost += energyConsumption*characteristics.getCostPerEnergy();
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
	protected void processAppSubmit(SimEvent ev) {
		Application app = (Application)ev.getData();
		controller.getApplications().put(app.getAppId(), app);
	}

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
			
			Analysis.incrementPacketDrop();
			return;
		}else
			Analysis.incrementPacketSuccess();
		

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
			updateTimingsOnReceipt(tuple);
			executeTuple(ev, tuple.getDestModuleName());
		}else{
			communication = new HashMap<String, String>();
			communication.put(tuple.getSrcModuleName(), tuple.getDestModuleName());
			
			Integer nexHopId = tupleRoutingTable.get(communication);
			
			if(Config.PRINT_DETAILS)
				FogComputingSim.print("[" + getName() + "] Is forwarding tuple w/ destiny module: " + tuple.getDestModuleName() +
						" to: " + controller.getFogDeviceById(nexHopId).getName());
			
			sendTo(tuple, nexHopId);
		}
	}
	
	/**
	 * Updates the timings on receipt.
	 * 
	 * @param tuple the received tuple
	 */
	protected void updateTimingsOnReceipt(Tuple tuple) {
		Application app = controller.getApplications().get(tuple.getAppId());
		String srcModule = tuple.getSrcModuleName();
		String destModule = tuple.getDestModuleName();
		
		List<AppLoop> loops = app.getLoops();
		
		for(AppLoop loop : loops){
			if(loop.hasEdge(srcModule, destModule) && loop.isEndModule(destModule)){
				Double startTime = TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				
				if(startTime == null)
					break;
				
				if(!TimeKeeper.getInstance().getLoopIdToCurrentAverage().containsKey(loop.getLoopId())){
					TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), 0.0);
					TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), 0);
				}
				
				double currentAverage = TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loop.getLoopId());
				int currentCount = TimeKeeper.getInstance().getLoopIdToCurrentNum().get(loop.getLoopId());
				double delay = CloudSim.clock()- TimeKeeper.getInstance().getEmitTimes().get(tuple.getActualTupleId());
				TimeKeeper.getInstance().getEmitTimes().remove(tuple.getActualTupleId());
				double newAverage = (currentAverage*currentCount + delay)/(currentCount+1);
				TimeKeeper.getInstance().getLoopIdToCurrentAverage().put(loop.getLoopId(), newAverage);
				TimeKeeper.getInstance().getLoopIdToCurrentNum().put(loop.getLoopId(), currentCount+1);
				break;
			}
		}
	}
	
	/**
	 * Executes the received tuple.
	 * 
	 * @param ev the event that just occurred containing the tuple
	 * @param moduleName the module name (virtual machine) where it will be processed
	 */
	protected void executeTuple(SimEvent ev, String moduleName) {
		Tuple tuple = (Tuple)ev.getData();
		
		if(Config.PRINT_DETAILS)
			FogComputingSim.print("[" + getName() + "] Started execution of tuple: " + tuple.getCloudletId() + " on " + tuple.getDestModuleName());
		
		AppModule dstModule = getModuleByName(tuple.getDestModuleName());
		tuple.setUserId(dstModule.getUserId());
		
		TimeKeeper.getInstance().tupleStartedExecution(tuple);
		updateAllocatedMips(moduleName);
		processCloudletSubmit(ev, false);
		updateAllocatedMips(moduleName);
	}
	
	/**
	 * Processes the event of module arrival.
	 * 
	 * @param ev the event that just occurred containing the application module
	 */
	protected void processModuleArrival(SimEvent ev){
		AppModule module = (AppModule)ev.getData();
		
		deployedModules.add(module.getName());

		processVmCreate(ev, false);
		
		if(Config.PRINT_DETAILS)
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
	protected void updateTupleQueue(SimEvent ev){
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
	protected void sendFreeLink(Tuple tuple, int destId){		
		updateEnergyConsumption();
		getTupleLinkBusy().put(destId, true);
		
		double latency = getLatencyMap().get(destId);
		double bandwidth = getBandwidthMap().get(destId);
		double networkDelay = (double)tuple.getCloudletFileSize()/bandwidth;
		
		send(getId(), networkDelay, FogEvents.UPDATE_TUPLE_QUEUE, destId);
		send(destId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
		
		NetworkUsageMonitor.sendingTuple(latency, bandwidth, tuple.getCloudletFileSize());
		updateEnergyConsumption();
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
		send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
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
	 * Updates position and randomly change movement characteristics except for static nodes
	 */
	private void updatePeriodicMovement() {
		movement.updateLocation();
		
		// Define next direction and velocity
		// Only updates for mobile nodes. Having fixed connections means that this node is a fixed one
		if(!isStaticNode()) {
			Random random = new Random();
			
			double changeDirProb = Config.PROB_CHANGE_DIRECTION;
			double changeVelProb = Config.PROB_CHANGE_VELOCITY;
			while(true) {
				if(random.nextDouble() < changeDirProb)
					movement.setDirection(random.nextInt(Movement.SOUTHEAST + 1));
				
				if(random.nextDouble() < changeVelProb) {
					double value = random.nextDouble();
					
					if(value < Config.PROB_MIN_VELOCITY) {
						movement.setVelocity(Math.abs(Util.normalRand(Config.MIN_VELOCITY, 1)));
					}else if(value >= Config.PROB_MIN_VELOCITY && value <= Config.PROB_MED_VELOCITY + Config.PROB_MIN_VELOCITY) {
						movement.setVelocity(Math.abs(Util.normalRand(Config.MED_VELOCITY, 1)));
					}else {
						movement.setVelocity(Math.abs(Util.normalRand(Config.MAX_VELOCITY, 1)));
					}
				}
				
				// Change the direction or velocity just to ensure devices are within the defined square for test purposes (it can be removed)
				// The movement model which is defined in the current method can also be modified
				// Compute next position (but does not update; just to check if it will end up within the defined square)
				Location newLocation = movement.computeNextLocation();
				
				if(newLocation.getX() > 0 && newLocation.getX() < Config.SQUARE_SIDE && newLocation.getY() > 0 && newLocation.getY() < Config.SQUARE_SIDE)
					break;
				else {
					changeDirProb = 1;
					changeVelProb = 1;
				}
					
			}
			
			// Update connections bandwidth for mobile connections based on their distance
			for(int neighborId : bandwidthMap.keySet()) {
				FogDevice neighbor = controller.getFogDeviceById(neighborId);
				
				double distance = Location.computeDistance(this, neighbor);
				double rxPower = MobilePathLossModel.computeReceivedPower(distance);
				Map<String, Double> map = MobileBandwidthModel.computeCommunicationBandwidth(1, rxPower);
				
				String modulation = map.entrySet().iterator().next().getKey();
				double bandwidth = map.entrySet().iterator().next().getValue();
				
				if(Config.PRINT_DETAILS) {
					FogComputingSim.print("Communication between " + getName() + " and " + neighbor.getName() + " is using " +
							modulation + " modulation" + " w/ bandwidth = "  + String.format("%.2f", bandwidth/1024/1024) + " MB/s" );
				}
				
				bandwidthMap.put(neighborId, bandwidth);
			}
		}
		
		send(getId(), 1, FogEvents.UPDATE_PERIODIC_MOVEMENT);
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
	}
	
	/**
	 * Performs a migration of an application module (virtual machine).
	 * 
	 * @param ev the event that just occurred containing the application module, the application and the destination node (migration destination)
	 */
	@SuppressWarnings("unchecked")
	private void migration(SimEvent ev) {
		Map<FogDevice, Map<Application, AppModule>> map = (Map<FogDevice, Map<Application, AppModule>>)ev.getData();
		
		Map<Application, AppModule> appMap = null;
		Application application = null;
		AppModule vm = null;
		FogDevice to = null;
		
		for(FogDevice fogDevice : map.keySet()) {
			to = fogDevice;
			appMap = map.get(fogDevice);
			
			for(Application app : appMap.keySet()) {
				application = app;
				vm = appMap.get(app);
			}
		}
		
		Map<String, Object> migrate = new HashMap<String, Object>();
		migrate.put("vm", vm);
		migrate.put("host", to.getHost());
		
		double totalSize = vm.getRam() + vm.getSize();
		
		vm.setInMigration(true);
		
		TupleVM tuple = new TupleVM(application.getAppId(), FogUtils.generateTupleId(), 0, 1, (long) totalSize, 0,
				new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull(), vm, application);
		
		sendTo(tuple, vmRoutingTable.get(vm.getName()));
		
		if(Config.PRINT_DETAILS)
			FogComputingSim.print("[" + getName() + "] started the migration of the vm: " + vm.getName() + " toward the machine: " + to.getName());
		
		getVmAllocationPolicy().deallocateHostForVm(vm);
		getVmList().remove(vm);
		getHost().getVmList().remove(vm);
		deployedModules.remove(vm.getName());
		
		Map<AppModule, Integer> vmPosition = new HashMap<AppModule, Integer>();
		vmPosition.put(vm, vmRoutingTable.get(vm.getName()));
		sendNow(controller.getId(), FogEvents.UPDATE_VM_POSITION, vmPosition);
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
	private AppModule getModuleByName(String moduleName){
		AppModule module = null;
		for(FogDevice fogDevice : controller.getFogDevices()) {
			for(Vm vm : fogDevice.getHost().getVmList()){
				if(((AppModule)vm).getName().equals(moduleName)){
					module=(AppModule)vm;
					break;
				}
			}
		}
		
		return module;
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
	
}