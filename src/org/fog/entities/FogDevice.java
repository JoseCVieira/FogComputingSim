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
import org.fog.utils.Logger;
import org.fog.utils.Movement;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

public class FogDevice extends PowerDatacenter {
	private List<String> deployedModules;
	private List<String> periodicTuplesToCancel;
	
	private Map<Integer, Queue<Pair<Tuple, Integer>>> tupleQueue;
	private Map<Integer, Boolean> tupleLinkBusy;
	
	private double lastMipsUtilization;
	private double lastRamUtilization;
	private double lastMemUtilization;
	private double lastBwUtilization;
	
	private List<Integer> fixedNeighborsIds;
	private Map<Integer, Double> latencyMap;
	private Map<Integer, Double> bandwidthMap;
	private Map<Map<String, String>, Integer> tupleRoutingTable;
	private Map<String, Integer> vmRoutingTable;
	
	private double lastUtilizationUpdateTime;
	private double energyConsumption;
	private double totalCost;
	
	private Controller controller;
	private Movement movement;
	
	public FogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
			double schedulingInterval, Movement movement) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		
		deployedModules = new ArrayList<String>();
		periodicTuplesToCancel = new ArrayList<String>();
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
	
	@Override
	protected void processOtherEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ARRIVAL:
			processTupleArrival(ev);
			break;
		case FogEvents.LAUNCH_MODULE:
			processModuleArrival(ev);
			break;
		case FogEvents.RELEASE_OPERATOR:
			processOperatorRelease(ev);
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
	 * Perform miscellaneous resource management tasks
	 * @param ev
	 */
	private void manageResources(SimEvent ev) {
		updateEnergyConsumption();
		send(getId(), Constants.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
	}

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
	 * Sending periodic tuple for an application edge. Note that for multiple instances of a single source module, number of tuples are sent.
	 * @param ev SimEvent instance containing the edge to send tuple on
	 */
	private void sendPeriodicTuple(SimEvent ev) {
		AppEdge edge = (AppEdge)ev.getData();
		String srcModuleName = edge.getSource();
		String dstModuleName = edge.getDestination();
		AppModule srcModule = getModuleByName(srcModuleName);
		AppModule dstModule = getModuleByName(dstModuleName);
		
		if(srcModule == null || dstModule == null) return;
		
		if(periodicTuplesToCancel.contains(srcModule.getName())) {
			periodicTuplesToCancel.remove(srcModule.getName());
			return;
		}
		
		Tuple tuple = controller.getApplications().get(srcModule.getAppId()).createTuple(edge, srcModule.getId(), dstModule.getUserId());
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
						Logger.debug(getName(), "Completed execution of tuple "+tuple.getCloudletId() + "on " + tuple.getDestModuleName());
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
	
	protected void updateAllocatedMips(String incomingOperator){
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
	
	private void updateEnergyConsumption() {
		double totalMipsAllocated = 0;
		double totalRamAllocated = 0;
		double totalMemAllocated = 0;
		
		for(final Vm vm : getHost().getVmList()){
			AppModule operator = (AppModule)vm;
			operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
					.getAllocatedMipsForVm(operator));
			
			double allocatedMipsForVm = getHost().getTotalAllocatedMipsForVm(vm);
			totalMipsAllocated += allocatedMipsForVm;
			totalMemAllocated += ((AppModule)vm).getSize();
			
			if(allocatedMipsForVm != 0)
				totalRamAllocated += ((AppModule)vm).getCurrentAllocatedRam();
		}
		
		/*double totalBwAllocated = 0;
		for(int destId : getTupleLinkBusy().keySet())
			if(getTupleLinkBusy().get(destId))
				totalBwAllocated += getBandwidthMap().get(destId);*/
		
		double timeNow = CloudSim.clock();
		double timeDif = timeNow-lastUtilizationUpdateTime;
		
		setEnergyConsumption(getEnergyConsumption() + timeDif*getHost().getPowerModel().getPower(lastMipsUtilization));
		
		FogDeviceCharacteristics characteristics = (FogDeviceCharacteristics) getCharacteristics();
		
		double newcost = getTotalCost();
		newcost += timeDif*lastMipsUtilization*getHost().getTotalMips()*characteristics.getCostPerMips();
		newcost += timeDif*lastRamUtilization*getHost().getRam()*characteristics.getCostPerMem();
		newcost += timeDif*lastMemUtilization*getHost().getStorage()*characteristics.getCostPerStorage();
		//newcost += timeDif*lastBwUtilization*getHost().getBw()*characteristics.getCostPerBw();
		newcost += timeDif*characteristics.getCostPerSecond();
		setTotalCost(newcost);
		
		lastRamUtilization = totalRamAllocated/getHost().getRam();
		//lastBwUtilization = totalBwAllocated/getHost().getBw();
		lastMemUtilization = totalMemAllocated/getHost().getStorage();
		lastMipsUtilization = totalMipsAllocated/getHost().getTotalMips();
		
		lastUtilizationUpdateTime = timeNow;
		
		if(Config.PRINT_COST_DETAILS) printCost();
	}

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
				
				FogComputingSim.print(getName() + " received and is deploying vm: " + tmp.getVm().getName());
			}else {
				FogComputingSim.print(getName() + " received and is forwarding the vm: " + tmp.getVm().getName() + " to: " + vmRoutingTable.get(tmp.getVm().getName()));
				
				vmPosition.put(tmp.getVm(), vmRoutingTable.get(tmp.getVm().getName()));
				sendNow(controller.getId(), FogEvents.UPDATE_VM_POSITION, vmPosition);
				
				sendTo(tmp, vmRoutingTable.get(tmp.getVm().getName()));
			}
			return;
		}
		
		Map<String, String> communication = new HashMap<String, String>();
		communication.put(tuple.getSrcModuleName(), tuple.getDestModuleName());
		
		// It can be null after some handover is completed. This can happen because, some connections were removed and routing
		// tables were updated. Thus, once there still can exist some old tuples, they will be lost because we already don't
		// know where to forward it.		
		if((!tupleRoutingTable.containsKey(communication) && !deployedModules.contains(tuple.getDestModuleName())) || moduleInMigration(tuple.getDestModuleName())) {
			FogComputingSim.print("[" + getName() + "] Is rejecting tuple with destiny: " + tuple.getDestModuleName());
			
			Analysis.incrementPacketDrop();
			return;
		}else
			Analysis.incrementPacketSuccess();
		
		if(getHost().getVmList().size() > 0){
			final AppModule operator = (AppModule)getHost().getVmList().get(0);
			
			if(CloudSim.clock() > 0){
				getHost().getVmScheduler().deallocatePesForVm(operator);
				getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) getHost().getTotalMips());}});
			}
		}

		if(deployedModules.contains(tuple.getDestModuleName())){
			int vmId = -1;
			for(Vm vm : getHost().getVmList()) {
				if(((AppModule)vm).getName().equals(tuple.getDestModuleName())) {
					vmId = vm.getId();
					
					if(Config.PRINT_DETAILS)
						FogComputingSim.print("[" + getName() + "] Is executing tuple on module: " + ((AppModule)vm).getName());
				}
			}
				
			if(vmId < 0 || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) && 
					tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId))
				return;

			tuple.setVmId(vmId);
			updateTimingsOnReceipt(tuple);
			executeTuple(ev, tuple.getDestModuleName());
		}else{
			if(Config.PRINT_COMMUNICATION_DETAILS) printCommunication(tuple);
			
			communication = new HashMap<String, String>();
			communication.put(tuple.getSrcModuleName(), tuple.getDestModuleName());
			
			Integer nexHopId = tupleRoutingTable.get(communication);
			sendTo(tuple, nexHopId);
		}
	}

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
	
	protected void executeTuple(SimEvent ev, String moduleName){
		Logger.debug(getName(), "Executing tuple on module " + moduleName);
		Tuple tuple = (Tuple)ev.getData();
		
		TimeKeeper.getInstance().tupleStartedExecution(tuple);
		updateAllocatedMips(moduleName);
		
		AppModule dstModule = getModuleByName(tuple.getDestModuleName());
		tuple.setUserId(dstModule.getUserId());
		
		processCloudletSubmit(ev, false);
		updateAllocatedMips(moduleName);
	}
	
	protected void processModuleArrival(SimEvent ev){
		AppModule module = (AppModule)ev.getData();
		
		deployedModules.add(module.getName());

		processVmCreate(ev, false);
		FogComputingSim.print("Creating " + module.getName() + " on device " + getName());
		
		if (module.isBeingInstantiated())
			module.setBeingInstantiated(false);
		
		initializePeriodicTuples(module);
		
		module.updateVmProcessing(CloudSim.clock(),
				getVmAllocationPolicy().getHost(module).getVmScheduler().getAllocatedMipsForVm(module));
	}
	
	private void initializePeriodicTuples(AppModule module) {
		String appId = module.getAppId();
		Application app = controller.getApplications().get(appId);
		List<AppEdge> periodicEdges = app.getPeriodicEdges(module.getName());
		
		for(AppEdge edge : periodicEdges)
			send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
	}

	protected void processOperatorRelease(SimEvent ev){
		this.processVmMigrate(ev, false);
	}
	
	protected void updateTupleQueue(SimEvent ev){
		Integer destId = (Integer)ev.getData();
		
		// It can be null after some handover is completed. This can happen because, some connections were removed and routing
		// tables were updated. Thus, once there still can exist some old tuples, they will be lost because we already don't
		// know where to forward it.
		if(getTupleQueue().get(destId) != null && !getTupleQueue().get(destId).isEmpty()){
			Pair<Tuple, Integer> pair = getTupleQueue().get(destId).poll();
			sendFreeLink(pair.getFirst(), pair.getSecond());
		}else {
			getTupleLinkBusy().put(destId, false);
			updateEnergyConsumption();
		}
	}
	
	protected void sendFreeLink(Tuple tuple, int destId){		
		updateEnergyConsumption();
		getTupleLinkBusy().put(destId, true);
		
		double latency = getLatencyMap().get(destId);
		double networkDelay = (double)tuple.getCloudletFileSize()/getBandwidthMap().get(destId);
		
		send(getId(), networkDelay, FogEvents.UPDATE_TUPLE_QUEUE, destId);
		send(destId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
		updateEnergyConsumption();
	}
	
	protected void sendTo(Tuple tuple, int id) {
		if(!getTupleLinkBusy().get(id))
			sendFreeLink(tuple, id);
		else
			getTupleQueue().get(id).add(new Pair<Tuple, Integer>(tuple, id));
	}

	protected void sendToSelf(Tuple tuple){
		send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
	}
	
	protected void printCommunication(Tuple tuple){
		Map<String, String> communication = new HashMap<String, String>();
		communication.put(tuple.getSrcModuleName(), tuple.getDestModuleName());
		
		System.out.println("\n\nTuple:" + tuple);
		System.out.println("From: " + getId());
		System.out.println("To: " + tupleRoutingTable.get(communication) + "\n\n");
	}
	
	private void printCost() {
		System.out.println("\n\nName: " + getName());
		System.out.println("lastMipsUtilization: " + lastMipsUtilization);
		System.out.println("lastRamUtilization: " + lastRamUtilization);
		System.out.println("lastMemUtilization: " + lastMemUtilization);
		System.out.println("lastBwUtilization: " + lastBwUtilization);
	}
	
	// Update position and randomly change movement characteristics except for static nodes
	// which are characterized by 0 velocity
	private void updatePeriodicMovement() {
		movement.updateLocation();
		
		// Define next direction and velocity
		// Only updates for mobile nodes. Having fixed connections means that this node is a fixed one
		if(getFixedNeighborsIds().isEmpty()) {
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
		}			
		
		send(getId(), 1, FogEvents.UPDATE_PERIODIC_MOVEMENT);
		//System.out.println(this + "\n\n");
	}
	
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
				new UtilizationModelFull(), new UtilizationModelFull(), new UtilizationModelFull(), vm, application, getId());
		
		System.out.println("Name: " + getName());
		System.out.println("vm.getName(): " + vm.getName());
		
		sendTo(tuple, vmRoutingTable.get(vm.getName()));
		
		getVmAllocationPolicy().deallocateHostForVm(vm);
		getVmList().remove(vm);
		getHost().getVmList().remove(vm);
		deployedModules.remove(vm.getName());
		periodicTuplesToCancel.add(vm.getName());
		
		Map<AppModule, Integer> vmPosition = new HashMap<AppModule, Integer>();
		vmPosition.put(vm, vmRoutingTable.get(vm.getName()));
		sendNow(controller.getId(), FogEvents.UPDATE_VM_POSITION, vmPosition);
	}
	
	private void finishMigration(SimEvent ev) {
		AppModule vm = (AppModule)ev.getData();
		
		getHost().removeMigratingInVm(vm);
		
		if (!getVmAllocationPolicy().allocateHostForVm(vm, getHost()))
			FogComputingSim.err("VM allocation to the destination host failed");
		
		deployedModules.add(vm.getName());
		
		vm.setInMigration(false);
		vm.setBeingInstantiated(false);
		initializePeriodicTuples(vm);
		
		//send(getId(), Config.SETUP_VM_TIME, FogEvents.FINISH_SETUP_MIGRATION, vm);
	}
	
	private void finishSetupMigration(SimEvent ev) {
		AppModule vm = (AppModule)ev.getData();

		getHost().removeMigratingInVm(vm);
		
		if (!getVmAllocationPolicy().allocateHostForVm(vm, getHost()))
			FogComputingSim.err("VM allocation to the destination host failed");
		
		deployedModules.add(vm.getName());
		
		vm.setInMigration(false);
		vm.setBeingInstantiated(false);
		initializePeriodicTuples(vm);
		
		FogComputingSim.print("[" + getName() + "] just fished the setup of the vm: " + vm.getName() + " and its now available");
	}
	
	private boolean moduleInMigration(String name) {
		for(Vm vm : getHost().getVmList()) {
			if(((AppModule)vm).getName().equals(name)) {
				if(vm.isInMigration())
					return true;
				else
					return false;
			}
		}
		return false;
	}
	
	public PowerHost getHost() {
		return (PowerHost) getHostList().get(0);
	}
	
	public double getEnergyConsumption() {
		return energyConsumption;
	}

	public void setEnergyConsumption(double energyConsumption) {
		this.energyConsumption = energyConsumption;
	}
	
	public Map<Integer, Double> getLatencyMap() {
		return latencyMap;
	}

	public void setLatencyMap(Map<Integer, Double> latencyMap) {
		this.latencyMap = latencyMap;
	}
	
	public Map<Integer, Double> getBandwidthMap() {
		return bandwidthMap;
	}

	public void setBandwidthMap(Map<Integer, Double> bandwidthMap) {
		this.bandwidthMap = bandwidthMap;
	}
	
	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}
	
	public void setController(Controller controller) {
		this.controller = controller;
		
		// Strength signal of the fixed communications is 1 (maximum)
		for(int neighborId : latencyMap.keySet()) {
			getFixedNeighborsIds().add(neighborId);
		}
		
		updatePeriodicMovement();
	}
	
	public Controller getController() {
		return controller;
	}
	
	public Map<Map<String, String>, Integer> getTupleRoutingTable() {
		return tupleRoutingTable;
	}

	public void setTupleRoutingTable(Map<Map<String, String>, Integer> tupleRoutingTable) {
		this.tupleRoutingTable = tupleRoutingTable;
	}
	
	public Map<Integer, Queue<Pair<Tuple, Integer>>> getTupleQueue() {
		return tupleQueue;
	}

	public void setTupleQueue(Map<Integer, Queue<Pair<Tuple, Integer>>> tupleQueue) {
		this.tupleQueue = tupleQueue;
	}

	public Map<Integer, Boolean> getTupleLinkBusy() {
		return tupleLinkBusy;
	}

	public void setTupleLinkBusy(Map<Integer, Boolean> tupleLinkBusy) {
		this.tupleLinkBusy = tupleLinkBusy;
	}
	
	public Movement getMovement() {
		return movement;
	}

	public void setMovement(Movement movement) {
		this.movement = movement;
	}
	
	public List<Integer> getFixedNeighborsIds() {
		return fixedNeighborsIds;
	}

	public void setFixedNeighborsIds(List<Integer> fixedNeighborsIds) {
		this.fixedNeighborsIds = fixedNeighborsIds;
	}
	
	public Map<String, Integer> getVmRoutingTable() {
		return vmRoutingTable;
	}

	public void setVmRoutingTable(Map<String, Integer> vmRoutingTable) {
		this.vmRoutingTable = vmRoutingTable;
	}

	@Override
	public String toString() {
		return "FogDevice [\nName=" + getName() + "\nId=" + getId() + "\ndeployedModules=" + deployedModules
				+ "\nlatencyMap=" + latencyMap + "\nbandwidthMap=" + bandwidthMap + "\nroutingTable=" + tupleRoutingTable
				+ "\nmovement=" + movement + "\nMIPS=" + getHost().getTotalMips() + "\nRAM=" + getHost().getRam()
				+ "\nMEM=" + getHost().getStorage() + "\nBW=" + getHost().getBw() + "\nVm List=" + getHost().getVmList()
				+ "\nfixedNeighborsIds=" + fixedNeighborsIds + "]";
	}
	
}