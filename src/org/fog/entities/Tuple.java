package org.fog.entities;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cloudbus.cloudsim.Cloudlet;
import org.cloudbus.cloudsim.UtilizationModel;

/**
 * Class representing tuple (messages exchanged between application modules).
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Tuple extends Cloudlet{
	/** If the module has not an actuator as its destination */
	private static final int NOT_ACTUATOR = 1;
	
	/** If the module has an actuator as its destination */
	public static final int ACTUATOR = 2;
	
	/** If the application id */
	private String appId;
	
	/** Tuple type */
	private String tupleType;
	
	/** Destination application module name */
	private String destModuleName;
	
	/** Source application module name */
	private String srcModuleName;
	
	/** Id of the tuple */
	private int actualTupleId;
	
	/** If the tuple has its has as its destination an actuator */
	private int direction;
	
	/**
	 * Map containing the module paths followed and the corresponding time in which the initial
	 * tuple was created (used to compute the loop E2E latency).
	 */
	private Map<List<String>, Double> pathMap;

	/**
	 * Map to keep track of which module instances has a tuple traversed.
	 * Map from moduleName to vmId of a module instance
	 */
	private Map<String, Integer> moduleCopyMap;
	
	/**
	 * Creates a new tuple.
	 * 
	 * @param appId the application id
	 * @param cloudletId the unique ID of this Cloudlet
	 * @param loudletLength the length or size (in MI) of this cloudlet to be executed in a PowerDatacenter
	 * @param pesNumber the pes number
	 * @param cloudletFileSize the file size [Byte] of this cloudlet BEFORE submitting to a PowerDatacenter
	 * @param cloudletOutputSize the file size [Byte] of this cloudlet AFTER finish executing by a PowerDatacenter
	 * @param utilizationModelCpu the utilization model cpu
	 * @param utilizationModelRam the utilization model ram
	 * @param utilizationModelBw the utilization model bw
	 */
	public Tuple(String appId, int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw) {
		
		super(cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize,
				utilizationModelCpu, utilizationModelRam, utilizationModelBw);
		
		setAppId(appId);
		setDirection(NOT_ACTUATOR);
		setModuleCopyMap(new HashMap<String, Integer>());
		setPathMap(new HashMap<List<String>, Double>());
	}
	
	/**
	 * Gets the id of the tuple.
	 * 
	 * @return the id of the tuple
	 */
	public int getActualTupleId() {
		return actualTupleId;
	}
	
	/**
	 * Sets the id of the tuple.
	 * 
	 * @param actualTupleId the id of the tuple
	 */
	public void setActualTupleId(int actualTupleId) {
		this.actualTupleId = actualTupleId;
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
	 * Gets the destination application module.
	 * 
	 * @return the destination application module
	 */
	public String getDestModuleName() {
		return destModuleName;
	}
	
	/**
	 * Sets the destination application module.
	 * 
	 * @param destModuleName the destination application module
	 */
	public void setDestModuleName(String destModuleName) {
		this.destModuleName = destModuleName;
	}
	
	/**
	 * Gets the source application module.
	 * 
	 * @return the source application module
	 */
	public String getSrcModuleName() {
		return srcModuleName;
	}
	
	/**
	 * Sets the source application module.
	 * 
	 * @param srcModuleName the source application module
	 */
	public void setSrcModuleName(String srcModuleName) {
		this.srcModuleName = srcModuleName;
	}
	
	/**
	 * Gets the direction of the tuple (has as its destination an actuator or not).
	 * 
	 * @return the direction of the tuple
	 */
	public int getDirection() {
		return direction;
	}
	
	/**
	 * Sets the direction of the tuple (has as its destination an actuator or not).
	 * 
	 * @param direction the direction of the tuple
	 */
	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	/**
	 * Gets the module copy map.
	 * 
	 * @return the module copy map
	 */
	public Map<String, Integer> getModuleCopyMap() {
		return moduleCopyMap;
	}
	
	/**
	 * Sets the module copy map.
	 * 
	 * @param moduleCopyMap the module copy map
	 */
	public void setModuleCopyMap(Map<String, Integer> moduleCopyMap) {
		this.moduleCopyMap = moduleCopyMap;
	}
	
	/**
	 * Gets the map containing the module paths followed and the corresponding time
	 * in which the initial tuple was created.
	 * 
	 * @return the map containing the module paths followed and the corresponding time
	 * in which the initial tuple was created
	 */
	public Map<List<String>, Double> getPathMap() {
		return pathMap;
	}
	
	/**
	 * Sets the map containing the module paths followed and the corresponding time
	 * in which the initial tuple was created.
	 * 
	 * @param pathMap the map containing the module paths followed and the corresponding time
	 * in which the initial tuple was created
	 */
	public void setPathMap(Map<List<String>, Double> pathMap) {
		this.pathMap = pathMap;
	}
	
	@Override
	public String toString() {
		String str = "\nappId: " + appId + "\n"+
		"tupleType: " + tupleType + "\n"+
		"TupleCpuLength: " + getCloudletLength() + "\n"+
		"TupleNWLength: " + getCloudletFileSize() + "\n"+
		"destModuleName: " + destModuleName + "\n"+
		"srcModuleName: " + srcModuleName + "\n"+
		"actualTupleId: " + actualTupleId + "\n"+
		"direction: " + direction;
		return str;
	}
	
}
