package org.fog.entities;

import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.utils.FogEvents;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;

/**
 * Class representing actuators (there needs to be one actuator per user's application).
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Actuator extends SimEntity{
	/** Gateway device id (client node) */
	private int gatewayDeviceId;
	
	/** Link latency between him and the gateway (it's always 0) */
	private double latency;
	
	/** Application id in which it is actuating */
	private String appId;
	
	/** User id */
	private int userId;
	
	/** Actuator type */
	private String actuatorType;
	
	/** Application where it is actuating */
	private Application app;
	
	/**
	 * Creates a new actuator.
	 * 
	 * @param name the name of the actuator
	 * @param userId the user id of the actuator
	 * @param appId the application id of the actuator
	 * @param gatewayDeviceId the gateway device id (client node) of the actuator
	 * @param actuatorType the type of actuator
	 */
	public Actuator(String name, int userId, String appId, int gatewayDeviceId, String actuatorType) {
		super(name);
		this.setAppId(appId);
		this.gatewayDeviceId = gatewayDeviceId;
		setUserId(userId);
		setActuatorType(actuatorType);
	}
	
	/**
	 * Informs the client node that a new actuator has joined after its criation.
	 */
	@Override
	public void startEntity() {
		sendNow(gatewayDeviceId, FogEvents.ACTUATOR_JOINED, getLatency());
	}
	
	/**
	 * Processes the events that can occur in an actuator.
	 */
	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.TUPLE_ARRIVAL:
			processTupleArrival(ev);
			break;
		}		
	}
	
	/**
	 * Processes the event of tuple arrival.
	 * 
	 * @param ev the event that just occurred
	 */
	private void processTupleArrival(SimEvent ev) {
		Tuple tuple = (Tuple)ev.getData();
		Logger.debug(getName(), "Received tuple " + tuple.getCloudletId() + " on " + tuple.getDestModuleName());
		String srcModule = tuple.getSrcModuleName();
		String destModule = tuple.getDestModuleName();
		TimeKeeper.getInstance().receivedTuple(tuple);
		
		for(AppLoop loop : app.getLoops()){
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
	
	@Override
	public void shutdownEntity() {
		
	}
	
	/**
	 * Gets the gateway device id.
	 * 
	 * @return the gateway device id
	 */
	public int getGatewayDeviceId() {
		return gatewayDeviceId;
	}
	
	/**
	 * Sets the gateway device id.
	 * 
	 * @param gatewayDeviceId the gateway device id
	 */
	public void setGatewayDeviceId(int gatewayDeviceId) {
		this.gatewayDeviceId = gatewayDeviceId;
	}
	
	/**
	 * Gets the user id.
	 * 
	 * @return the user id
	 */
	public int getUserId() {
		return userId;
	}
	
	/**
	 * Sets the user id.
	 * 
	 * @param userId the user id
	 */
	public void setUserId(int userId) {
		this.userId = userId;
	}
	
	/**
	 * Gets the application id
	 * 
	 * @return the application id
	 */
	public String getAppId() {
		return appId;
	}
	
	/**
	 * Sets the application id.
	 * 
	 * @param appId the application id
	 */
	public void setAppId(String appId) {
		this.appId = appId;
	}
	
	/**
	 * Gets the actuator type.
	 * 
	 * @return the actuator type
	 */
	public String getActuatorType() {
		return actuatorType;
	}
	
	/**
	 * Sets the the actuator type.
	 * 
	 * @param actuatorType the actuator type
	 */
	public void setActuatorType(String actuatorType) {
		this.actuatorType = actuatorType;
	}
	
	/**
	 * Gets the application.
	 * 
	 * @return the application
	 */
	public Application getApp() {
		return app;
	}
	
	/**
	 * Sets the application.
	 * 
	 * @param app the application
	 */
	public void setApp(Application app) {
		this.app = app;
	}
	
	/**
	 * Gets the latency.
	 * 
	 * @return the latency
	 */
	public double getLatency() {
		return latency;
	}
	
	/**
	 * Sets the latency.
	 * 
	 * @param latency the latency
	 */
	public void setLatency(double latency) {
		this.latency = latency;
	}

}
