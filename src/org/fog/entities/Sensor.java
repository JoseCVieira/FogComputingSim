package org.fog.entities;

import java.util.ArrayList;

import org.cloudbus.cloudsim.UtilizationModelFull;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.utils.FogEvents;
import org.fog.utils.FogUtils;
import org.fog.utils.Logger;
import org.fog.utils.TimeKeeper;
import org.fog.utils.distribution.Distribution;

/**
 * Class representing sensors (there needs to be one sensor per user's application).
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Sensor extends SimEntity{
	/** File size (in byte) of this cloudlet AFTER finish executing by a PowerDatacenter */
	private static final long OUTPUT_SIZE = 3;
	
	/** Application id in which it is sensing */
	private String appId;
	
	/** Tuple type */
	private String tupleType;
	
	/** Sensor name */
	private String sensorName;
	
	/** Destiny application module name */
	private String destModuleName;
	
	/** User id */
	private int userId;
	
	/** Gateway device id (client node) */
	private int gatewayDeviceId;
	
	/** Link latency between him and the gateway (it's always 0) */
	private double latency;
	
	/** Application where it is sensing */
	private Application app;
	
	/** Tuple generation distribution of the sensor */
	private Distribution transmitDistribution;
	
	/**
	 * Creates a new sensor.
	 * 
	 * @param name the name of the sensor
	 * @param tupleType the tuple type of the sensor
	 * @param userId the user id of the sensor
	 * @param appId the application id of the sensor
	 * @param transmitDistribution the tuple generation distribution of the sensor
	 * @param gatewayDeviceId
	 */
	public Sensor(String name, String tupleType, int userId, String appId, Distribution transmitDistribution, int gatewayDeviceId) {
		super(name);
		this.setAppId(appId);
		this.setTransmitDistribution(transmitDistribution);
		setTupleType(tupleType);
		setSensorName(tupleType);
		setUserId(userId);
		setGatewayDeviceId(gatewayDeviceId);
		setLatency(latency);
	}
	
	/**
	 * Starts the tuple generation and transmission after its creation.
	 */
	@Override
	public void startEntity() {
		send(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE);
	}
	
	/**
	 * Processes the events that can occur in an sensor.
	 */
	@Override
	public void processEvent(SimEvent ev) {
		switch(ev.getTag()){
		case FogEvents.EMIT_TUPLE:
			transmit();
			send(getId(), getTransmitDistribution().getNextValue(), FogEvents.EMIT_TUPLE);
			break;
		}
			
	}
	
	/**
	 * Transmit a new tuple to the client node (gateway device).
	 */
	public void transmit(){
		AppEdge _edge = null;
		for(AppEdge edge : getApp().getEdges())
			if(edge.getSource().equals(getTupleType()))
				_edge = edge;

		long cpuLength = (long) _edge.getTupleCpuLength();
		long nwLength = (long) _edge.getTupleNwLength();
		
		Tuple tuple = new Tuple(getAppId(), FogUtils.generateTupleId(), cpuLength, 1,
				nwLength, OUTPUT_SIZE, new UtilizationModelFull(), new UtilizationModelFull(),
				new UtilizationModelFull());
		
		tuple.setUserId(getUserId());
		tuple.setTupleType(getTupleType());
		tuple.setDestModuleName(_edge.getDestination());
		tuple.setSrcModuleName(getSensorName());
		tuple.setActualTupleId(updateTimings(getSensorName(), tuple.getDestModuleName()));
		
		Logger.debug(getName(), "Sending tuple with tupleId = " + tuple.getCloudletId());
		send(gatewayDeviceId, getLatency(), FogEvents.TUPLE_ARRIVAL, tuple);
	}
	
	/**
	 * Update timings on that application edge.
	 * 
	 * @param src source module of the application edge
	 * @param dest destination module of the application edge
	 * @return >= 0 if it has found the edge, otherwise -1
	 */
	private int updateTimings(String src, String dest){
		Application application = getApp();
		for(AppLoop loop : application.getLoops()){
			if(loop.hasEdge(src, dest)){
				int tupleId = TimeKeeper.getInstance().getUniqueId();
				
				if(!TimeKeeper.getInstance().getLoopIdToTupleIds().containsKey(loop.getLoopId()))
					TimeKeeper.getInstance().getLoopIdToTupleIds().put(loop.getLoopId(), new ArrayList<Integer>());
				TimeKeeper.getInstance().getLoopIdToTupleIds().get(loop.getLoopId()).add(tupleId);
				TimeKeeper.getInstance().getEmitTimes().put(tupleId, CloudSim.clock());
				return tupleId;
			}
		}
		
		return -1;
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
	 * Gets the tuple type.
	 * 
	 * @return the tuple type
	 */
	public String getTupleType() {
		return tupleType;
	}
	
	/**
	 * Sets the tuple type.
	 * 
	 * @param tupleType the tuple type
	 */
	public void setTupleType(String tupleType) {
		this.tupleType = tupleType;
	}
	
	/**
	 * Gets the sensor name.
	 * 
	 * @return the sensor name
	 */
	public String getSensorName() {
		return sensorName;
	}
	
	/**
	 * Sets the sensor name.
	 * 
	 * @param sensorName the sensor name
	 */
	public void setSensorName(String sensorName) {
		this.sensorName = sensorName;
	}
	
	/**
	 * Gets the application id.
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
	 * Gets the destination application module name.
	 * 
	 * @return the destination application module name
	 */
	public String getDestModuleName() {
		return destModuleName;
	}
	
	/**
	 * Sets the destination application module name.
	 * 
	 * @param destModuleName the destination application module name
	 */
	public void setDestModuleName(String destModuleName) {
		this.destModuleName = destModuleName;
	}
	
	/**
	 * Gets the transmit distribution.
	 * 
	 * @return the transmit distribution
	 */
	public Distribution getTransmitDistribution() {
		return transmitDistribution;
	}
	
	/**
	 * Sets the transmit distribution.
	 * 
	 * @param transmitDistribution the transmit distribution
	 */
	public void setTransmitDistribution(Distribution transmitDistribution) {
		this.transmitDistribution = transmitDistribution;
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
