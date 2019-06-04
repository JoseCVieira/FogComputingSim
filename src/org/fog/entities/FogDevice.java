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
import org.fog.utils.FogEvents;
import org.fog.utils.Location;
import org.fog.utils.Logger;
import org.fog.utils.Movement;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

public class FogDevice extends PowerDatacenter {
	private List<String> deployedModules;
	
	private Map<Integer, Queue<Pair<Tuple, Integer>>> tupleQueue;
	private Map<Integer, Boolean> tupleLinkBusy;
	
	private double lastMipsUtilization;
	private double lastRamUtilization;
	private double lastMemUtilization;
	private double lastBwUtilization;
	
	private List<Integer> neighborsIds;
	private Controller controller;
	
	private Map<Integer, Double> latencyMap;
	private Map<Integer, Double> bandwidthMap;
	private Map<Map<String, String>, Integer> routingTable;
	
	private double lastUtilizationUpdateTime;
	private double energyConsumption;
	private double totalCost;
	
	private Movement movement;
	
	public FogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy, List<Storage> storageList,
			double schedulingInterval, Movement movement) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		
		deployedModules = new ArrayList<String>();
		setNeighborsIds(new ArrayList<Integer>());
		setLatencyMap(new HashMap<Integer, Double>());
		setBandwidthMap(new HashMap<Integer, Double>());
		setRoutingTable(new HashMap<Map<String,String>, Integer>());
		
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
		
		Tuple tuple = controller.getApplications().get(srcModule.getAppId()).createTuple(edge, srcModule.getId(), dstModule.getUserId());
		updateTimingsOnSending(tuple);
		sendToSelf(tuple);
		
		send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
	}
	
	public String getOperatorName(int vmId){
		for(Vm vm : this.getHost().getVmList())
			if(vm.getId() == vmId)
				return ((AppModule)vm).getName();

		return null;
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
		
		double currentEnergyConsumption = getEnergyConsumption();
		double newEnergyConsumption = currentEnergyConsumption +
				timeDif*getHost().getPowerModel().getPower(lastMipsUtilization);
		setEnergyConsumption(newEnergyConsumption);
		
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

	protected void processTupleArrival(SimEvent ev){
		Tuple tuple = (Tuple)ev.getData();
		
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
			
			Map<String, String> communication = new HashMap<String, String>();
			communication.put(tuple.getSrcModuleName(), tuple.getDestModuleName());
			sendTo(tuple, routingTable.get(communication));
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
		System.out.println("Creating " + module.getName() + " on device " + getName());
		
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
		
		if(!getTupleQueue().get(destId).isEmpty()){
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
	
	protected void sendTo(Tuple tuple, int id){
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
		System.out.println("To: " + routingTable.get(communication) + "\n\n");
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
		
		if(movement.getVelocity() != 0) {
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
		
		System.out.println(this + "\n\n");
	}
	
	public PowerHost getHost(){
		return (PowerHost) getHostList().get(0);
	}
	
	public List<Integer> getNeighborsIds() {
		return neighborsIds;
	}
	
	public void setNeighborsIds(List<Integer> neighborsIds) {
		this.neighborsIds = neighborsIds;
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
	}
	
	public Controller getController() {
		return controller;
	}
	
	public Map<Map<String, String>, Integer> getRoutingTable() {
		return routingTable;
	}

	public void setRoutingTable(Map<Map<String, String>, Integer> routingTable) {
		this.routingTable = routingTable;
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

	@Override
	public String toString() {
		return "FogDevice [\nName=" + getName() + "\nId=" + getId() + "\ndeployedModules=" + deployedModules
				+ "\nneighborsIds=" + neighborsIds + "\nlatencyMap=" + latencyMap + "\nbandwidthMap=" + bandwidthMap
				+ "\nroutingTable=" + routingTable + "\nmovement=" + movement + "\nMIPS=" + getHost().getTotalMips()
				+ "\nRAM=" + getHost().getRam() + "\nMEM=" + getHost().getStorage() + "\nBW=" + getHost().getBw() + "]";
	}
	
}