package org.fog.placement;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppLoop;
import org.fog.application.AppModule;
import org.fog.application.Application;
import org.fog.core.Constants;
import org.fog.core.FogComputingSim;
import org.fog.entities.Actuator;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.utils.FogEvents;
import org.fog.utils.NetworkUsageMonitor;
import org.fog.utils.TimeKeeper;
import org.fog.utils.Util;

public class Controller extends SimEntity{
	private static final int MAX_COLUMN_SIZE = 50;
	
	private Map<String, ModulePlacement> appModulePlacementPolicy;
	private Map<String, Application> applications;
	private Map<String, Integer> appLaunchDelays;
	
	private List<FogDevice> fogDevices;
	private List<Sensor> sensors;
	private List<Actuator> actuators;
	
	private Util u;
	
	public Controller(String name, List<FogDevice> fogDevices, List<Sensor> sensors, List<Actuator> actuators) {
		super(name);
		this.u = new Util();
		this.applications = new HashMap<String, Application>();
		setAppLaunchDelays(new HashMap<String, Integer>());
		setAppModulePlacementPolicy(new HashMap<String, ModulePlacement>());
		setFogDevices(fogDevices);
		setActuators(actuators);
		setSensors(sensors);
	}

	public FogDevice getFogDeviceById(int id){
		for(FogDevice fogDevice : getFogDevices())
			if(id==fogDevice.getId())
				return fogDevice;
		return null;
	}
	
	@Override
	public void startEntity() {
		for(String appId : applications.keySet()){
			if(getAppLaunchDelays().get(appId)==0)
				processAppSubmit(applications.get(appId));
			else
				send(getId(), getAppLaunchDelays().get(appId), FogEvents.APP_SUBMIT, applications.get(appId));
		}

		send(getId(), Constants.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
		send(getId(), Constants.MAX_SIMULATION_TIME, FogEvents.STOP_SIMULATION);
		
		for(FogDevice dev : getFogDevices())
			sendNow(dev.getId(), FogEvents.RESOURCE_MGMT);
	}

	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.APP_SUBMIT:
			processAppSubmit(ev);
			break;
		case FogEvents.TUPLE_FINISHED:
			processTupleFinished(ev);
			break;
		case FogEvents.CONTROLLER_RESOURCE_MANAGE:
			manageResources();
			break;
		case FogEvents.STOP_SIMULATION:
			CloudSim.stopSimulation();
			printTimeDetails();
			printPowerDetails();
			printCostDetails();
			printNetworkUsageDetails();
			
			if(FogComputingSim.isDisplayingPlot)
				Util.promptEnterKey("Press \"ENTER\" to exit...");
			
			System.exit(0);
			break;
		}
	}

	private String getStringForLoopId(int loopId){
		for(String appId : getApplications().keySet()){
			Application app = getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules().toString();
			}
		}
		return null;
	}

	protected void manageResources(){
		send(getId(), Constants.RESOURCE_MANAGE_INTERVAL, FogEvents.CONTROLLER_RESOURCE_MANAGE);
	}
	
	private void processTupleFinished(SimEvent ev) {
	}
	
	@Override
	public void shutdownEntity() {	
	}
	
	public void submitApplication(Application application, int delay, ModulePlacement modulePlacement){
		getApplications().put(application.getAppId(), application);
		getAppLaunchDelays().put(application.getAppId(), delay);
		getAppModulePlacementPolicy().put(application.getAppId(), modulePlacement);
		
		for(Sensor sensor : sensors)
			sensor.setApp(getApplications().get(sensor.getAppId()));
			
		for(Actuator ac : actuators)
			ac.setApp(getApplications().get(ac.getAppId()));
	}
	
	public void submitApplication(Application application, ModulePlacement modulePlacement){
		submitApplication(application, 0, modulePlacement);
	}
	
	private void processAppSubmit(SimEvent ev){
		Application app = (Application) ev.getData();
		processAppSubmit(app);
	}
	
	private void processAppSubmit(Application application){
		System.out.println("Submitted application " + application.getAppId() + " at time= " + CloudSim.clock());
		getApplications().put(application.getAppId(), application);
		
		ModulePlacement modulePlacement = getAppModulePlacementPolicy().get(application.getAppId());
		
		Map<Integer, List<AppModule>> deviceToModuleMap = modulePlacement.getDeviceToModuleMap();
		for(Integer deviceId : deviceToModuleMap.keySet()){
			for(AppModule module : deviceToModuleMap.get(deviceId)){
				sendNow(deviceId, FogEvents.APP_SUBMIT, application);
				sendNow(deviceId, FogEvents.LAUNCH_MODULE, module);
			}
		}
	}
	
	private void printTimeDetails() {
		DecimalFormat df = new DecimalFormat("0.00");
		
		System.out.println("\n\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "EXECUTION TIME") + "|");
		newDetailsField(2, '-');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), String.valueOf(Calendar.getInstance().getTimeInMillis() -
				TimeKeeper.getInstance().getSimulationStartTime())) + "|");
		newDetailsField(2, '=');
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "APPLICATION LOOP DELAYS") + "|");
		newDetailsField(2, '-');
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
			System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), getStringForLoopId(loopId) + " ---> "+
					df.format(TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId)).toString()) + "|");
		}
		newDetailsField(2, '=');

		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "TUPLE CPU EXECUTION DELAY") + "|");
		newDetailsField(2, '-');
		for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet())
			System.out.print("|" + u.centerString(MAX_COLUMN_SIZE, tupleType) + "|" +
					u.centerString(MAX_COLUMN_SIZE,
							df.format(TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType)).toString()) + "|\n");
		newDetailsField(2, '=');
	}
	
	private void printNetworkUsageDetails() {
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL NETWORK USAGE") + "|");
		newDetailsField(2, '-');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "" +
				NetworkUsageMonitor.getNetworkUsage()/Constants.MAX_SIMULATION_TIME) + "|");
		newDetailsField(2, '=');
	}
	
	private void printCostDetails(){
		DecimalFormat df = new DecimalFormat("0.00"); 
		Double aux = 0.0;
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "COST OF EXECUTION") + "|");
		newDetailsField(2, '-');
		System.out.print("|" + u.centerString(MAX_COLUMN_SIZE/5-1, "ID") + "|" +
				u.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, "NAME") + "|" +
				u.centerString(MAX_COLUMN_SIZE, "VALUE") + "|\n");
		newDetailsField(2, '-');
		for(FogDevice fogDevice : getFogDevices()) {
			aux += fogDevice.getTotalCost();
			System.out.print("|" + u.centerString(MAX_COLUMN_SIZE/5-1, String.valueOf(fogDevice.getId())) + "|" +
					u.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, fogDevice.getName()) + "|" +
					u.centerString(MAX_COLUMN_SIZE, String.valueOf(df.format(fogDevice.getTotalCost()))) + "|\n");
		}
		newDetailsField(2, '-');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL = " + df.format(aux)) + "|");
		newDetailsField(2, '=');
	}
	
	private void printPowerDetails() {
		DecimalFormat df = new DecimalFormat("0.00");
		Double aux = 0.0;
		
		System.out.println("\n");
		newDetailsField(2, '=');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "ENERGY CONSUMED") + "|");
		newDetailsField(2, '-');
		System.out.print("|" + u.centerString(MAX_COLUMN_SIZE/5-1, "ID") + "|" +
				u.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, "NAME") + "|" +
				u.centerString(MAX_COLUMN_SIZE, "VALUE") + "|\n");
		newDetailsField(2, '-');
		for(FogDevice fogDevice : getFogDevices()) {
			aux += fogDevice.getEnergyConsumption();
			System.out.print("|" + u.centerString(MAX_COLUMN_SIZE/5-1, String.valueOf(fogDevice.getId())) + "|" +
					u.centerString(MAX_COLUMN_SIZE-MAX_COLUMN_SIZE/5, fogDevice.getName()) + "|" +
					u.centerString(MAX_COLUMN_SIZE, String.valueOf(df.format(fogDevice.getEnergyConsumption()))) + "|\n");
		}
		newDetailsField(2, '-');
		System.out.println("|" + u.centerString((MAX_COLUMN_SIZE*2+1), "TOTAL = " + df.format(aux)) + "|");
		newDetailsField(2, '=');
	}
	
	private static void newDetailsField(int nrColumn, char character) {
		for (int i=0; i<MAX_COLUMN_SIZE*nrColumn+(nrColumn); i++)
		    System.out.print(character);
		System.out.println(character);
	}
	
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}

	public void setFogDevices(List<FogDevice> fogDevices) {
		this.fogDevices = fogDevices;
	}

	public Map<String, Integer> getAppLaunchDelays() {
		return appLaunchDelays;
	}

	public void setAppLaunchDelays(Map<String, Integer> appLaunchDelays) {
		this.appLaunchDelays = appLaunchDelays;
	}

	public Map<String, Application> getApplications() {
		return applications;
	}

	public void setApplications(Map<String, Application> applications) {
		this.applications = applications;
	}

	public List<Sensor> getSensors() {
		return sensors;
	}

	public void setSensors(List<Sensor> sensors) {
		for(Sensor sensor : sensors)
			sensor.setControllerId(getId());
		this.sensors = sensors;
	}

	public List<Actuator> getActuators() {
		return actuators;
	}

	public void setActuators(List<Actuator> actuators) {
		this.actuators = actuators;
	}

	public Map<String, ModulePlacement> getAppModulePlacementPolicy() {
		return appModulePlacementPolicy;
	}

	public void setAppModulePlacementPolicy(Map<String, ModulePlacement> appModulePlacementPolicy) {
		this.appModulePlacementPolicy = appModulePlacementPolicy;
	}
}