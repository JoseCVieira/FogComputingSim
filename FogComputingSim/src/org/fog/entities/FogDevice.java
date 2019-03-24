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
import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.Storage;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;
import org.cloudbus.cloudsim.sdn.overbooking.BwProvisionerOverbooking;
import org.cloudbus.cloudsim.sdn.overbooking.PeProvisionerOverbooking;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.placement.Controller;
import org.fog.placement.ModulePlacement;
import org.fog.policy.AppModuleAllocationPolicy;
import org.fog.scheduler.StreamOperatorScheduler;
import org.fog.utils.Config;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.ModuleLaunchConfig;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.dijkstra.DijkstraAlgorithm;
import org.fog.utils.dijkstra.Vertex;

public class FogDevice extends PowerDatacenter {
	private static final boolean PRINT_COMMUNICATION_DETAILS = true;
	
	protected Map<String, Map<String, Integer>> moduleInstanceCount;
	protected List<Pair<Integer, Double>> associatedActuatorIds;
	protected Map<String, List<String>> appToModulesMap;
	protected Map<Integer, Integer> cloudTrafficMap;
	protected List<String> activeApplications;
	
	protected Queue<Pair<Tuple, Integer>> southTupleQueue;
	protected Queue<Pair<Tuple, Integer>> northTupleQueue; // Modified
	protected boolean isSouthLinkBusy;
	protected boolean isNorthLinkBusy;
	protected double downlinkBandwidth;
	protected double uplinkBandwidth;
	
	protected double lastMipsUtilization;
	protected double lastRamUtilization; // RAM ---- added
	protected double lastMemUtilization; // MEM ---- added
	protected double lastBwUtilization; // BW ---- added
	
	private List<Integer> childrenIds;
	private List<Integer> brothersIds; // Added
	private List<Integer> parentsIds; // Modified
	private Controller controller; // Added
	
	protected Map<Integer, Double> downStreamLatencyMap; // Modified
	protected Map<Integer, Double> upStreamLatencyMap; // Modified
	
	private double lastUtilizationUpdateTime;
	protected double energyConsumption;
	protected double ratePerMips;
	protected double totalCost;
	
	public FogDevice(String name, FogDeviceCharacteristics characteristics, VmAllocationPolicy vmAllocationPolicy,
			List<Storage> storageList, double schedulingInterval, double uplinkBandwidth, double downlinkBandwidth,
			double ratePerMips) throws Exception {
		super(name, characteristics, vmAllocationPolicy, storageList, schedulingInterval);
		
		setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
		setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
		appToModulesMap = new HashMap<String, List<String>>();
		this.cloudTrafficMap = new HashMap<Integer, Integer>();
		setActiveApplications(new ArrayList<String>());
		
		southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		northTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		setNorthLinkBusy(false);
		setSouthLinkBusy(false);
		setDownlinkBandwidth(downlinkBandwidth);
		setUplinkBandwidth(uplinkBandwidth);
		
		this.lastMipsUtilization = 0;
		this.lastRamUtilization = 0;
		this.lastMemUtilization = 0;
		this.lastBwUtilization = 0;
		
		setParentsIds(new ArrayList<Integer>());
		setBrothersIds(new ArrayList<Integer>());
		setChildrenIds(new ArrayList<Integer>());
		
		setDownStreamLatencyMap(new HashMap<Integer, Double>());
		setUpStreamLatencyMap(new HashMap<Integer, Double>());
		
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
		setRatePerMips(ratePerMips);
		setTotalCost(0);
	}

	public FogDevice(String name, long mips, int ram, double uplinkBandwidth, double downlinkBandwidth,
			double ratePerMips, PowerModel powerModel) throws Exception {
		
		super(name, null, null, new LinkedList<Storage>(), 0);
		
		List<Pe> peList = new ArrayList<Pe>();

		// Create PEs and add these into a list.
		// need to store Pe id and MIPS Rating
		peList.add(new Pe(0, new PeProvisionerOverbooking(mips)));

		int hostId = FogUtils.generateEntityId();
		long storage = 1000000; // host storage
		int bw = 10000;

		PowerHost host = new PowerHost(
			hostId,
			new RamProvisionerSimple(ram),
			new BwProvisionerOverbooking(bw),
			storage,
			peList,
			new StreamOperatorScheduler(peList),
			powerModel
		);

		List<Host> hostList = new ArrayList<Host>();
		hostList.add(host);

		setVmAllocationPolicy(new AppModuleAllocationPolicy(hostList));
		
		String arch = Config.FOG_DEVICE_ARCH; 
		String os = Config.FOG_DEVICE_OS; 
		String vmm = Config.FOG_DEVICE_VMM;
		double time_zone = Config.FOG_DEVICE_TIMEZONE;
		double cost = Config.FOG_DEVICE_COST; 
		double costPerMem = Config.FOG_DEVICE_COST_PER_MEMORY;
		double costPerStorage = Config.FOG_DEVICE_COST_PER_STORAGE;
		double costPerBw = Config.FOG_DEVICE_COST_PER_BW;

		FogDeviceCharacteristics characteristics = new FogDeviceCharacteristics(arch, os, vmm, host,
				time_zone, cost, costPerMem, costPerStorage, costPerBw);

		setCharacteristics(characteristics);
		
		setLastProcessTime(0.0);
		setVmList(new ArrayList<Vm>());
		setUplinkBandwidth(uplinkBandwidth);
		setDownlinkBandwidth(downlinkBandwidth);
		setAssociatedActuatorIds(new ArrayList<Pair<Integer, Double>>());
		
		for (Host host1 : getCharacteristics().getHostList())
			host1.setDatacenter(this);

		setActiveApplications(new ArrayList<String>());
		if (getCharacteristics().getNumberOfPes() == 0)
			throw new Exception(super.getName()
					+ " : Error - this entity has no PEs. Therefore, can't process any Cloudlets.");
		
		getCharacteristics().setId(super.getId());
		
		appToModulesMap = new HashMap<String, List<String>>();
		northTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		southTupleQueue = new LinkedList<Pair<Tuple, Integer>>();
		setNorthLinkBusy(false);
		setSouthLinkBusy(false);
		
		
		setChildrenIds(new ArrayList<Integer>());
		
		this.cloudTrafficMap = new HashMap<Integer, Integer>();
		
		this.energyConsumption = 0;
		this.lastRamUtilization = 0;
		this.lastMemUtilization = 0;
		this.lastBwUtilization = 0;
		this.lastMipsUtilization = 0;
		setTotalCost(0);
		setDownStreamLatencyMap(new HashMap<Integer, Double>());
		setUpStreamLatencyMap(new HashMap<Integer, Double>());
		setModuleInstanceCount(new HashMap<String, Map<String, Integer>>());
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
		case FogEvents.UPDATE_NORTH_TUPLE_QUEUE:
			updateNorthTupleQueue();
			break;
		case FogEvents.UPDATE_SOUTH_TUPLE_QUEUE:
			updateSouthTupleQueue();
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
		
		for(int i = 0; i < module.getNumInstances(); i++){
			Tuple tuple = controller.getApplications().get(module.getAppId()).createTuple(edge, module.getId());
			updateTimingsOnSending(tuple);
			sendToSelf(tuple);			
		}
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

			Log.formatLine(
					"\n%.2f: Data center's energy is %.2f W*sec\n",
					currentTime,
					timeFrameDatacenterEnergy);
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
						Logger.debug(getName(), "Completed execution of tuple "+tuple.getCloudletId()+"on "+tuple.getDestModuleName());
						List<Tuple> resultantTuples = application.getResultantTuples(tuple.getDestModuleName(), tuple, getId(), vm.getId());
						
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
		double totalRamAllocated = 0;
		double totalMemAllocated = 0;
		double totalBwAllocated = 0;
		double newcost = 0;
		
		for(final Vm vm : getHost().getVmList()){
			AppModule operator = (AppModule)vm;
			operator.updateVmProcessing(CloudSim.clock(), getVmAllocationPolicy().getHost(operator).getVmScheduler()
					.getAllocatedMipsForVm(operator));
			totalMipsAllocated += getHost().getTotalAllocatedMipsForVm(vm);
			totalRamAllocated += ((AppModule)vm).getCurrentAllocatedRam();
			totalBwAllocated += ((AppModule)vm).getCurrentAllocatedBw();
			totalMemAllocated += ((AppModule)vm).getSize();
		}
		double totalMem = totalMemAllocated + getHost().getStorage();
		
		double timeNow = CloudSim.clock();
		double time_def = timeNow-lastUtilizationUpdateTime;
		
		double currentEnergyConsumption = getEnergyConsumption();
		double newEnergyConsumption = currentEnergyConsumption + time_def*getHost().getPowerModel().getPower(lastMipsUtilization);
		setEnergyConsumption(newEnergyConsumption);
		
		newcost = getTotalCost();
		newcost += time_def*lastMipsUtilization*getHost().getTotalMips()*getRatePerMips();
		newcost += time_def*lastRamUtilization*getHost().getRam()*getCharacteristics().getCostPerMem();
		newcost += time_def*lastMemUtilization*totalMem*getCharacteristics().getCostPerStorage();
		newcost += time_def*lastBwUtilization*getHost().getBw()*getCharacteristics().getCostPerBw();
		newcost += time_def*getCharacteristics().getCostPerSecond();

		/*System.out.println("\n\n" + getName());
		System.out.println(lastUtilization + " " + getHost().getTotalMips() + " " + getRatePerMips());
		System.out.println(lastRamUtilization + " " + getHost().getRam() + " " + getCharacteristics().getCostPerMem());
		System.out.println(lastMemUtilization + " " + totalMem + " " + getCharacteristics().getCostPerStorage());
		System.out.println(lastBwUtilization + " " + getHost().getBw() + " " + getCharacteristics().getCostPerBw());*/
		setTotalCost(newcost);
		
		lastMipsUtilization = Math.min(1, (float)totalMipsAllocated/getHost().getTotalMips());
		lastRamUtilization = Math.min(1, (float)totalRamAllocated/getHost().getRam());
		lastBwUtilization = Math.min(1, (float)totalBwAllocated/getHost().getBw());
		lastMemUtilization = Math.min(1, (float)totalMemAllocated/totalMem);
		lastUtilizationUpdateTime = timeNow;
	}

	protected void processAppSubmit(SimEvent ev) {
		Application app = (Application)ev.getData();
		controller.getApplications().put(app.getAppId(), app);
	}
	
	protected void updateCloudTraffic(){
		int time = (int)CloudSim.clock()/1000;
		if(!cloudTrafficMap.containsKey(time))
			cloudTrafficMap.put(time, 0);
		
		cloudTrafficMap.put(time, cloudTrafficMap.get(time)+1);
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
		
		if(PRINT_COMMUNICATION_DETAILS) printCommunication(tuple, "DOWN");
		sendDown(tuple, findNextHopCommunication(tuple));
	}

	protected void processTupleArrival(SimEvent ev){
		Tuple tuple = (Tuple)ev.getData();
		
		if(getName().equals("cloud"))
			updateCloudTraffic();
		
		Logger.debug(getName(), "Received tuple " + tuple.getCloudletId() +
				"with tupleType = "+tuple.getTupleType() + "\t| Source : " +
				CloudSim.getEntityName(ev.getSource())+"|Dest : " +
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
		
		
		if(getName().equals("cloud") && tuple.getDestModuleName()==null)
			sendNow(controller.getId(), FogEvents.TUPLE_FINISHED, null);
		
		if(appToModulesMap.containsKey(tuple.getAppId())){
			if(appToModulesMap.get(tuple.getAppId()).contains(tuple.getDestModuleName())){
				int vmId = -1;
				
				for(Vm vm : getHost().getVmList())
					if(((AppModule)vm).getName().equals(tuple.getDestModuleName()))
						vmId = vm.getId();
					
				if(vmId < 0 || (tuple.getModuleCopyMap().containsKey(tuple.getDestModuleName()) && 
						tuple.getModuleCopyMap().get(tuple.getDestModuleName())!=vmId ))
					return;

				tuple.setVmId(vmId);
				updateTimingsOnReceipt(tuple);
				
				executeTuple(ev, tuple.getDestModuleName());
				
			}else if(tuple.getDestModuleName()!=null){
				if(tuple.getDirection() == Tuple.UP) {
					if(PRINT_COMMUNICATION_DETAILS) printCommunication(tuple, "UP");
						sendUp(tuple, findNextHopCommunication(tuple));
				}else if(tuple.getDirection() == Tuple.DOWN) {
					if(PRINT_COMMUNICATION_DETAILS) printCommunication(tuple, "DOWN");
						sendDown(tuple, findNextHopCommunication(tuple));
				}
			}else {
				if(PRINT_COMMUNICATION_DETAILS) printCommunication(tuple, "UP");
					sendUp(tuple, findNextHopCommunication(tuple));
			}
		}else{
			if(tuple.getDirection() == Tuple.UP) {
				if(PRINT_COMMUNICATION_DETAILS) printCommunication(tuple, "UP");
					sendUp(tuple, findNextHopCommunication(tuple));
			}else if(tuple.getDirection() == Tuple.DOWN) {
				if(PRINT_COMMUNICATION_DETAILS) printCommunication(tuple, "DOWN");
					sendDown(tuple, findNextHopCommunication(tuple));
			}
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
				
				if(startTime==null)
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
		Logger.debug(getName(), "Executing tuple on module "+moduleName);
		Tuple tuple = (Tuple)ev.getData();
		
		AppModule module = getModuleByName(moduleName);
		
		if(tuple.getDirection() == Tuple.UP){
			String srcModule = tuple.getSrcModuleName();
			if(!module.getDownInstanceIdsMaps().containsKey(srcModule))
				module.getDownInstanceIdsMaps().put(srcModule, new ArrayList<Integer>());
			if(!module.getDownInstanceIdsMaps().get(srcModule).contains(tuple.getSourceModuleId()))
				module.getDownInstanceIdsMaps().get(srcModule).add(tuple.getSourceModuleId());
			
			int instances = -1;
			for(String _moduleName : module.getDownInstanceIdsMaps().keySet())
				instances = Math.max(module.getDownInstanceIdsMaps().get(_moduleName).size(), instances);
			module.setNumInstances(instances);
		}
		
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
	
	
	protected void updateNorthTupleQueue(){
		if(!getNorthTupleQueue().isEmpty()){
			Pair<Tuple, Integer> pair = getNorthTupleQueue().poll();
			sendUpFreeLink(pair.getFirst(), pair.getSecond());
		}else
			setNorthLinkBusy(false);
	}
	
	protected void sendUpFreeLink(Tuple tuple, int parentId){
		double networkDelay = tuple.getCloudletFileSize()/getUplinkBandwidth();
		setNorthLinkBusy(true);
		
		double latency = getUpStreamLatencyMap().get(parentId);
		send(getId(), networkDelay, FogEvents.UPDATE_NORTH_TUPLE_QUEUE);
		send(parentId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
	}
	
	protected void sendUp(Tuple tuple, int parentId){
		if(parentId > 0 && (getParentsIds().contains(parentId) || getBrothersIds().contains(parentId))){
			if(!isNorthLinkBusy())
				sendUpFreeLink(tuple, parentId);
			else
				northTupleQueue.add(new Pair<Tuple, Integer>(tuple, parentId));
		}
	}
	
	protected void updateSouthTupleQueue(){
		if(!getSouthTupleQueue().isEmpty()){
			Pair<Tuple, Integer> pair = getSouthTupleQueue().poll(); 
			sendDownFreeLink(pair.getFirst(), pair.getSecond());
		}else
			setSouthLinkBusy(false);
	}
	
	protected void sendDownFreeLink(Tuple tuple, int childId){
		double networkDelay = tuple.getCloudletFileSize()/getDownlinkBandwidth();
		setSouthLinkBusy(true);
		
		double latency = getDownStreamLatencyMap().get(childId);
		send(getId(), networkDelay, FogEvents.UPDATE_SOUTH_TUPLE_QUEUE);
		send(childId, networkDelay + latency, FogEvents.TUPLE_ARRIVAL, tuple);
		NetworkUsageMonitor.sendingTuple(latency, tuple.getCloudletFileSize());
	}
	
	protected void sendDown(Tuple tuple, int childId){
		if(getChildrenIds().contains(childId) || getBrothersIds().contains(childId)){
			if(!isSouthLinkBusy())
				sendDownFreeLink(tuple, childId);
			else
				southTupleQueue.add(new Pair<Tuple, Integer>(tuple, childId));
		}
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
		int destId = modulePlacement.getModuleToDeviceMap().get(destModule);
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
	private void printCommunication(Tuple tuple, String direction){
		System.out.println("\nDirection: " + direction);
		System.out.println(tuple);
		System.out.println("From: " + getId());
		System.out.println("To: " + findNextHopCommunication(tuple));
	}

	public PowerHost getHost(){
		return (PowerHost) getHostList().get(0);
	}
	
	public List<Integer> getParentsIds() {
		return parentsIds;
	}
	
	public void setParentsIds(List<Integer> parentsId) {
		this.parentsIds = parentsId;
	}
	
	public List<Integer> getChildrenIds() {
		return childrenIds;
	}
	
	public void setChildrenIds(List<Integer> childrenIds) {
		this.childrenIds = childrenIds;
	}
	public double getUplinkBandwidth() {
		return uplinkBandwidth;
	}
	
	public void setUplinkBandwidth(double uplinkBandwidth) {
		this.uplinkBandwidth = uplinkBandwidth;
	}
	
	public boolean isSouthLinkBusy() {
		return isSouthLinkBusy;
	}
	
	public boolean isNorthLinkBusy() {
		return isNorthLinkBusy;
	}
	
	public void setSouthLinkBusy(boolean isSouthLinkBusy) {
		this.isSouthLinkBusy = isSouthLinkBusy;
	}
	
	public void setNorthLinkBusy(boolean isNorthLinkBusy) {
		this.isNorthLinkBusy = isNorthLinkBusy;
	}
	
	public List<String> getActiveApplications() {
		return activeApplications;
	}
	
	public void setActiveApplications(List<String> activeApplications) {
		this.activeApplications = activeApplications;
	}

	public Queue<Pair<Tuple, Integer>> getNorthTupleQueue() {
		return northTupleQueue;
	}

	public void setNorthTupleQueue(Queue<Pair<Tuple, Integer>> northTupleQueue) {
		this.northTupleQueue = northTupleQueue;
	}

	public Queue<Pair<Tuple, Integer>> getSouthTupleQueue() {
		return southTupleQueue;
	}

	public void setSouthTupleQueue(Queue<Pair<Tuple, Integer>> southTupleQueue) {
		this.southTupleQueue = southTupleQueue;
	}

	public double getDownlinkBandwidth() {
		return downlinkBandwidth;
	}

	public void setDownlinkBandwidth(double downlinkBandwidth) {
		this.downlinkBandwidth = downlinkBandwidth;
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
	
	public Map<Integer, Double> getDownStreamLatencyMap() {
		return downStreamLatencyMap;
	}

	public void setDownStreamLatencyMap(Map<Integer, Double> downStreamLatencyMap) {
		this.downStreamLatencyMap = downStreamLatencyMap;
	}
	
	public Map<Integer, Double> getUpStreamLatencyMap() {
		return upStreamLatencyMap;
	}
	
	public void setUpStreamLatencyMap(Map<Integer, Double> upStreamLatencyMap) {
		this.upStreamLatencyMap = upStreamLatencyMap;
	}

	public double getRatePerMips() {
		return ratePerMips;
	}

	public void setRatePerMips(double ratePerMips) {
		this.ratePerMips = ratePerMips;
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
	
	public List<Integer> getBrothersIds() {
		return brothersIds;
	}

	public void setBrothersIds(List<Integer> brothersIds) {
		this.brothersIds = brothersIds;
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
		"ParentsIds: " + parentsIds + "\n"+
		"BrodersIds: " + brothersIds + "\n"+
		"ChildrenIds: " + childrenIds + "\n"+
		"MIPS: " + getHost().getTotalMips() + "\n"+
		"RAM: " + getHost().getRam() + "\n"+
		"MEM: " + getHost().getStorage() + "\n"+
		"BW: " + getHost().getBw() + "\n"+
		"DownlinkBandwidth: " + downlinkBandwidth + "\n"+
		"UplinkBandwidth: " + uplinkBandwidth + "\n"+
		"upStreamLatencyMap: " + upStreamLatencyMap + "\n"+
		"downStreamLatencyMap: " + downStreamLatencyMap + "\n"+
		"RatePerMips: " + ratePerMips + "\n\n";
		return str;
	}
	
}
