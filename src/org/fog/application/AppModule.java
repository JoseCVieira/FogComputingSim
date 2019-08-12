package org.fog.application;

import java.util.Map;

import org.apache.commons.math3.util.Pair;
import org.cloudbus.cloudsim.CloudletScheduler;
import org.cloudbus.cloudsim.CloudletSchedulerTimeShared;
import org.cloudbus.cloudsim.power.PowerVm;
import org.fog.application.selectivity.SelectivityModel;
import org.fog.utils.FogUtils;

/**
 * Class representing application modules (Virtual Machines), the processing elements of the application model.
 * 
 * @author Harshit Gupta
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AppModule extends PowerVm {
	/** Name of the application module */
	private String name;
	
	/** Id of the application */
	private String appId;
	
	/**
	 * Denotes if the application module is a client module.
	 * Client modules only run inside the client device (e.g., Graphical User Interface)
	 */
	private boolean clientModule;
	
	/**
	 * Denotes if the application module is a global module.
	 * Global modules are used by all clients running the same application (e.g., Multiplayer Games)
	 */
	private boolean globalModule;
	
	/** Map of the selectivity models governing the relation between the incoming and outgoing edges */
	private Map<Pair<String, String>, SelectivityModel> selectivityMap;
	
	/**
	 * Creates a new application module.
	 * 
	 * @param id the application module id
	 * @param name the application module name
	 * @param appId the application id
	 * @param userId the user id
	 * @param mips the processing resource units necessary to the application module
	 * @param ram the memory resource units necessary to the application module
	 * @param bw the network resource units necessary to the application module
	 * @param size the storage resource units necessary to the application module
	 * @param vmm the virtual machine monitor
	 * @param cloudletScheduler the cloudletScheduler policy for cloudlets
	 * @param selectivityMap the map of the selectivity models governing the relation between the incoming and outgoing edges
	 * @param clientModule if the application module is a global module
	 * @param glogbalModule if the application global is a global module
	 */
	public AppModule(int id, String name, String appId, int userId, double mips, int ram, long bw, long size,
			String vmm, CloudletScheduler cloudletScheduler, Map<Pair<String, String>, SelectivityModel> selectivityMap,
			boolean clientModule, boolean glogbalModule) {
		super(id, userId, mips, 1, ram, bw, size, 1, vmm, cloudletScheduler, 300);
		
		setName(name);
		setAppId(appId);
		setUid(getUid(userId, id));
		setBeingInstantiated(true);
		setSelectivityMap(selectivityMap);
		setClientModule(clientModule);
		setGlobalModule(glogbalModule);
	}
	
	/**
	 * Creates a new application module from another application module.
	 * 
	 * @param operator the application module
	 */
	public AppModule(AppModule operator) {
		super(FogUtils.generateEntityId(), operator.getUserId(), operator.getMips(), 1, operator.getRam(), operator.getBw(),
				operator.getSize(), 1, operator.getVmm(), new CloudletSchedulerTimeShared(), operator.getSchedulingInterval());
		
		setName(operator.getName());
		setAppId(operator.getAppId());
		setBeingInstantiated(true);
		setSelectivityMap(operator.getSelectivityMap());
		setClientModule(operator.isClientModule());
		setGlobalModule(operator.isGlobalModule());
	}
	
	/**
	 * Modifies the values of the application module.
	 * 
	 * @param name the application module name
	 * @param ram the memory resource units necessary to the application module
	 * @param clientModule if the application module is a global module
	 * @param glogbalModule if the application global is a global module
	 */
	public void setValues(String name, int ram, boolean clientModule, boolean glogbalModule) {
		setName(name);
		setRam(ram);
		setClientModule(clientModule);
		setGlobalModule(glogbalModule);
	}
	
	/**
	 * Gets the name of the application module.
	 * 
	 * @return the name of the application module
	 */
	public String getName() {
		return name;
	}
	
	/**
	 * Sets the name of the application module.
	 * 
	 * @param name the name of the application module
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * Gets the map of the selectivity models governing the relation between the incoming and outgoing edges.
	 * 
	 * @return the map of the selectivity models governing the relation between the incoming and outgoing edges
	 */
	public Map<Pair<String, String>, SelectivityModel> getSelectivityMap() {
		return selectivityMap;
	}
	
	/**
	 * Sets the map of the selectivity models governing the relation between the incoming and outgoing edges.
	 * 
	 * @param selectivityMap the map of the selectivity models governing the relation between the incoming and outgoing edges
	 */
	public void setSelectivityMap(Map<Pair<String, String>, SelectivityModel> selectivityMap) {
		this.selectivityMap = selectivityMap;
	}
	
	/**
	 * Gets the id of the application.
	 * 
	 * @return the id of the application
	 */
	public String getAppId() {
		return appId;
	}
	
	/**
	 * Sets the id of the application.
	 * 
	 * @param appId the id of the application
	 */
	public void setAppId(String appId) {
		this.appId = appId;
	}

	/**
	 * Check whether the application module is a client edge.
	 * 
	 * @return true if it is, otherwise false
	 */
	public boolean isClientModule() {
		return clientModule;
	}

	/**
	 * Defines whether the application module is a client edge.
	 * 
	 * @param isPeriodic if the application module is a client edge
	 */
	public void setClientModule(boolean clientModule) {
		this.clientModule = clientModule;
	}

	/**
	 * Check whether the application module is a global edge.
	 * 
	 * @return true if it is, otherwise false
	 */
	public boolean isGlobalModule() {
		return globalModule;
	}

	/**
	 * Defines whether the application module is a global edge.
	 * 
	 * @param isPeriodic if the application module is a global edge
	 */
	public void setGlobalModule(boolean globalModule) {
		this.globalModule = globalModule;
	}
	
	@Override
	public String toString() {
		return "AppModule [name=" + name + ", appId=" + appId +"]";
	}
	
}