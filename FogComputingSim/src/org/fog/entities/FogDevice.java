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
import org.fog.placement.ModulePlacement;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.Logger;
import org.fog.utils.ModuleLaunchConfig;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.dijkstra.DijkstraAlgorithm;
import org.fog.utils.dijkstra.Vertex;

public class FogDevice extends PowerDatacenter {
	private static final boolean PRINT_COMMUNICATION_DETAILS = false;
	
	protected Map<String, Map<String, Integer>> moduleInstanceCount;
	protected List<Pair<Integer, Double>> associatedActuatorIds;
	protected Map<String, List<String>> appToModulesMap;
	protected List<String> activeApplications;
	
	private Queue<Pair<Tuple, Integer>> tupleQueue;
	private boolean tupleLinkBusy;
	
	protected double lastMipsUtilization;
	protected double lastRamUtilization; // RAM ---- added
	protected double lastMemUtilization; // MEM ---- added
	protected double lastBwUtilization; // BW ---- added
	
	private List<Integer> neighborsIds;
	private Controller controller; // Added
	
	protected Map<Integer, Double> latencyMap; // Modified
	
	private double lastUtilizationUpdateTime;
	protected double energyConsumption;
	protected double totalCost;
	
	public FogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		
		setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
		setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
		appToModulesMap = new HashMap<String, List<String>>();
		setActiveApplications(new ArrayList<String>());
		
		setTupleQueue(new LinkedList<Pair<Tuple, Integer>>());
		setTupleLinkBusy(false);
		
		this.lastMipsUtilization = 0;
		this.lastRamUtilization = 0;
		this.lastMemUtilization = 0;
		this.lastBwUtilization = 0;
		
		setNeighborsIds(new ArrayList<Integer>());
		setLatencyMap(new HashMap<Integer, Double>());
		
		setVmAllocationPolicy(vmAllocationPolicy);
		setSchedulingInterval(schedulingInterval);
		setCharacteristics(characteristics);
		setVmList(new ArrayList<Vm>());
		setStorageList(storageList);
		
		for (Host host : getCharacteristics().getHostList())
			host.setDatacenter(this);
		
		// If this resource doesn't have any PEs then no useful at all
		if (getCharacteristics().getNumberOfPes() == 0)
			throw new Exception(super.getName() + " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
		
		// stores id of this class
		getCharacteristics().setId(super.getId());
		
		setLastProcessTime(0.0);
		lastUtilizationUpdateTime = 0;
		setEnergyConsumption(0);
		setTotalCost(0);
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
		case FogEvents.SENSOR_JOINED:
			processSensorJoining(ev);
			break;
		case FogEvents.SEND_PERIODIC_TUPLE:
			sendPeriodicTuple(ev);
			break;
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.UPDATE_TUPLE_QUEUE:
			updateTupleQueue();
			break;
		case FogEvents.ACTIVE_APP_UPDATE:
			updateActiveApplications(ev);
			break;
		case FogEvents.ACTUATOR_JOINED:
			processActuatorJoined(ev);
			break;
		case FogEvents.LAUNCH_MODULE_INSTANCE:
			updateModuleInstanceCount(ev);
			break;
		case FogEvents.RESOURCE_MGMT:
			manageResources(ev);
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
		send(getId(), Config.RESOURCE_MGMT_INTERVAL, FogEvents.RESOURCE_MGMT);
	}

	/**
	 * Updating the number of modules of an application module on this device
	 * @param ev instance of SimEvent containing the module and no of instances 
	 */
	private void updateModuleInstanceCount(SimEvent ev) {
		ModuleLaunchConfig config = (ModuleLaunchConfig)ev.getData();
		String appId = config.getModule().getAppId();
		if(!moduleInstanceCount.containsKey(appId))
			moduleInstanceCount.put(appId, new HashMap<String, Integer>());
		moduleInstanceCount.get(appId).put(config.getModule().getName(), config.getInstanceCount());
		System.out.println(getName() + " Creating "+config.getInstanceCount() +
				" instances of module " + config.getModule().getName());
	}

	private AppModule getModuleByName(String moduleName){
		AppModule module = null;
		for(Vm vm : getHost().getVmList()){
			if(((AppModule)vm).getName().equals(moduleName)){
				module=(AppModule)vm;
				break;
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
		String srcModule = edge.getSource();
		AppModule module = getModuleByName(srcModule);
		
		if(module == null) return;
		
		Tuple tuple = controller.getApplications().get(module.getAppId()).createTuple(edge, module.getId());
		updateTimingsOnSending(tuple);
		sendToSelf(tuple);
		
		send(getId(), edge.getPeriodicity(), FogEvents.SEND_PERIODIC_TUPLE, edge);
	}

	protected void processActuatorJoined(SimEvent ev) {
		int actuatorId = ev.getSource();
		double delay = (double)ev.getData();
		getAssociatedActuatorIds().add(new Pair<Integer, Double>(actuatorId, delay));
	}

	
	protected void updateActiveApplications(SimEvent ev) {
		Application app = (Application)ev.getData();
		getActiveApplications().add(app.getAppId());
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
						Logger.debug(getName(), "Completed execution of tuple "+tuple.getCloudletId() +
								"on " + tuple.getDestModuleName());
						List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(),
								tuple, vm.getId());
						
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
	
	// Modified
	private void updateEnergyConsumption() {
		double totalMipsAllocated = 0;
		for(final Vm vm : getHost().getVmList()){
			AppModule operator = (AppModule)vm;
			operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
					.getAllocatedMipsForVm(operator));
			totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
		}
		
		double timeNow = CloudSim.clock();
		double timeDif = timeNow-lastUtilizationUpdateTime;
		setEnergyConsumption(getEnergyConsumption() + timeDif*getHost().getPowerModel().getPower(lastMipsUtilization));
		FogDeviceCharacteristics characteristics = (FogDeviceCharacteristics) getCharacteristics();
		setTotalCost(getTotalCost() + timeDif*lastMipsUtilization*getHost().getTotalMips()*characteristics.getCostPerMips());
		
		lastMipsUtilization = totalMipsAllocated/getHost().getTotalMips();
		lastUtilizationUpdateTime = timeNow;
	}

	protected void processAppSubmit(SimEvent ev) {
		Application app = (Application)ev.getData();
		controller.getApplications().put(app.getAppId(), app);
	}
	
	protected void sendTupleToActuator(Tuple tuple){
		for(Pair<Integer, Double> actuatorAssociation : getAssociatedActuatorIds()){
			int actuatorId = actuatorAssociation.getFirst();
			double delay = actuatorAssociation.getSecond();
			String actuatorType = ((Actuator)CloudSim.getEntity(actuatorId)).getActuatorType();
			
			if(tuple.getDestModuleName().equals(actuatorType)){
				send(actuatorId, delay, FogEvents.TUPLE_ARRIVAL, tuple);
				return;
			}
		}
		
		if(PRINT_COMMUNICATION_DETAILS) printCommunication(tuple);
		sendTo(tuple, findNextHopCommunication(tuple));
	}

	protected void processTupleArrival(SimEvent ev){
		Tuple tuple = (Tuple)ev.getData();
		
		Logger.debug(getName(), "Received tuple " + tuple.getCloudletId() + " with tupleType = " +
				tuple.getTupleType() + " Source : " + CloudSim.getEntityName(ev.getSource())+" Dest : " +
				CloudSim.getEntityName(ev.getDestination()));
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);		
		
		if(tuple.getDirection() == Tuple.ACTUATOR){
			sendTupleToActuator(tuple);
			return;
		}
		
		if(getHost().getVmList().size() > 0){
			final AppModule operator = (AppModule)getHost().getVmList().get(0);
			
			if(CloudSim.clock() > 0){
				getHost().getVmScheduler().deallocatePesForVm(operator);
				getHost().getVmScheduler().allocatePesForVm(operator, new ArrayList<Double>(){
					protected static final long serialVersionUID = 1L;
				{add((double) getHost().getTotalMips());}});
			}
		}
		
		if(appToModulesMap.containsKey(tuple.getAppId())){
			if(appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())){
				int vmId = -1;
				
				for(Vm vm : getHost().getVmList())
					if(((AppModule)vm).getName().equals(tuple.getDestModuleName()))
						vmId = vm.getId();
					
				if(vmId < 0 || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) && 
						tuple.getModuleCopyMap().get(tuple.getDestModuleName()) != vmId))
					return;

				tuple.setVmId(vmId);
				updateTimingsOnReceipt(tuple);
				
				executeTuple(ev, tuple.getDestModuleName());
				
			}else if(tuple.getDestModuleName() != null){
				if(PRINT_COMMUNICATION_DETAILS) printCommunication(tuple);
				sendTo(tuple, findNextHopCommunication(tuple));
			}
		}else{
			if(PRINT_COMMUNICATION_DETAILS) printCommunication(tuple);
			sendTo(tuple, findNextHopCommunication(tuple));
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

	protected void processSensorJoining(SimEvent ev){
		send(ev.getSource(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ACK);
	}
	
	protected void executeTuple(SimEvent ev, String moduleName){
		Logger.debug(getName(), "Executing tuple on module " + moduleName);
		Tuple tuple = (Tuple)ev.getData();
		
		TimeKeeper.getInstance().tupleStartedExecution(tuple);
		updateAllocatedMips(moduleName);
		processCloudletSubmit(ev, false);
		updateAllocatedMips(moduleName);
	}
	
	protected void processModuleArrival(SimEvent ev){
		AppModule module = (AppModule)ev.getData();
		String appId = module.getAppId();
		
		if(!appToModulesMap.containsKey(appId))
			appToModulesMap.put(appId, new ArrayList<String>());

		appToModulesMap.get(appId).add(module.getName());

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
	
	protected void updateTupleQueue(){
		if(!getTupleQueue().isEmpty()){
			Pair<Tuple, Integer> pair = getTupleQueue().poll();
			sendFreeLink(pair.getFirst(), pair.getSecond());
		}else
			setTupleLinkBusy(false);
	}
	
	protected void sendFreeLink(Tuple tuple, int destId){
		double networkDelay = tuple.getCloudletFileSize()/getHost().getBw();
		
		setTupleLinkBusy(true);
		
		double latency = getLatencyMap().get(destId);
		send(getId(), networkDelay, FogEvents.UPDATE_TUPLE_QUEUE);
		send(destId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
	}
	
	protected void sendTo(Tuple tuple, int id){
		if(!isTupleLinkBusy())
			sendFreeLink(tuple, id);
		else
			tupleQueue.add(new Pair<Tuple, Integer>(tuple, id));
	}

	protected void sendToSelf(Tuple tuple){
		send(getId(), CloudSim.getMinTimeBetweenEvents(), FogEvents.TUPLE_ARRIVAL, tuple);
	}

	/**
	 * Find next hop based on delay
	 * @param tuple
	 * @return
	 */
	private int findNextHopCommunication(Tuple tuple) {
		Application app = controller.getApplications().get(tuple.getAppId());
		ModulePlacement modulePlacement = controller.getAppModulePlacementPolicy().get(app.getAppId());
		
		String destModule = tuple.getDestModuleName();
		
		int destId;
		try {
			destId = modulePlacement.getModuleToDeviceMap().get(destModule);
		} catch (Exception e) { // Actuator tuple
			destId = tuple.getClientId();
		}
		
		
		int initId = getId();
		
		DijkstraAlgorithm dijkstra = app.getDijkstraAlgorithm();
		Vertex initNode = null;
		Vertex destNode = null;
		
		for(Vertex vertex : dijkstra.getNodes()) {
			if(vertex.getName().equals(Integer.toString(initId)))
				initNode = vertex;
			
			if(vertex.getName().equals(Integer.toString(destId)))
				destNode = vertex;
		}
		
		dijkstra.execute(initNode);
		LinkedList<Vertex> path = dijkstra.getPath(destNode);
		return Integer.parseInt(path.get(1).getName());
	}
	
	// Added for debug
	private void printCommunication(Tuple tuple){
		System.out.println("Tuple" + tuple);
		System.out.println("From: " + getId());
		System.out.println("To: " + findNextHopCommunication(tuple) + "\n\n");
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
	
	public Queue<Pair<Tuple, Integer>> getTupleQueue() {
		return tupleQueue;
	}

	public void setTupleQueue(Queue<Pair<Tuple, Integer>> tupleQueue) {
		this.tupleQueue = tupleQueue;
	}

	public boolean isTupleLinkBusy() {
		return tupleLinkBusy;
	}

	public void setTupleLinkBusy(boolean tupleLinkBusy) {
		this.tupleLinkBusy = tupleLinkBusy;
	}
	
	public List<String> getActiveApplications() {
		return activeApplications;
	}
	
	public void setActiveApplications(List<String> activeApplications) {
		this.activeApplications = activeApplications;
	}

	public List<Pair<Integer, Double>> getAssociatedActuatorIds() {
		return associatedActuatorIds;
	}

	public void setAssociatedActuatorIds(List<Pair<Integer, Double>> associatedActuatorIds) {
		this.associatedActuatorIds = associatedActuatorIds;
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
	
	public double getTotalCost() {
		return totalCost;
	}

	public void setTotalCost(double totalCost) {
		this.totalCost = totalCost;
	}

	public Map<String, Map<String, Integer>> getModuleInstanceCount() {
		return moduleInstanceCount;
	}

	public void setModuleInstanceCount(
			Map<String, Map<String, Integer>> moduleInstanceCount) {
		this.moduleInstanceCount = moduleInstanceCount;
	}
	
	public void setController(Controller controller) {
		this.controller = controller;
	}
	
	public Controller getController() {
		return controller;
	}
	
	@Override
	public String toString() {
		String str = "";
		
		str = "\nID: " + getId() + " Name: " + getName() + "\n"+
		"NeighborsIds: " + neighborsIds + "\n"+
		"MIPS: " + getHost().getTotalMips() + "\n"+
		"RAM: " + getHost().getRam() + "\n"+
		"MEM: " + getHost().getStorage() + "\n"+
		"BW: " + getHost().getBw() + "\n"+
		"LatencyMap: " + latencyMap + "\n\n";
		return str;
	}
	
}
