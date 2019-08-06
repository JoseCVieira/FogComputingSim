package org.fog.entities;

import org.cloudbus.cloudsim.UtilizationModel;
import org.fog.application.AppModule;
import org.fog.application.Application;

/**
 * Class representing application modules (virtual machines) migration tuples.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class TupleVM extends Tuple {
	/** Application module (virtual machine) to be migrated */
	private AppModule vm;
	
	/** Application of the application module */
	private Application application;
	
	/**
	 * Creates a new application module (virtual machine) migration tuple.
	 * 
	 * @param appId the application id
	 * @param cloudletId the unique ID of this Cloudlet
	 * @param loudletLength the length or size (in MI) of this cloudlet to be executed in a PowerDatacenter
	 * @param pesNumber the pes number
	 * @param cloudletFileSize the file size (in byte) of this cloudlet BEFORE submitting to a PowerDatacenter
	 * @param cloudletOutputSize the file size (in byte) of this cloudlet AFTER finish executing by a PowerDatacenter
	 * @param utilizationModelCpu the utilization model cpu
	 * @param utilizationModelRam the utilization model ram
	 * @param utilizationModelBw the utilization model bw
	 * @param vm the application module (virtual machine)
	 * @param application the application
	 */
	public TupleVM(String appId, int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, AppModule vm, Application application) {
		super(appId, cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
		
		setVm(vm);
		setApplication(application);
	}
	
	/**
	 * Gets the application module.
	 * 
	 * @return the application module
	 */
	public AppModule getVm() {
		return vm;
	}
	
	/**
	 * Sets the application module.
	 * 
	 * @param vm the application module
	 */
	public void setVm(AppModule vm) {
		this.vm = vm;
	}
	
	/**
	 * Gets the application.
	 * 
	 * @return the application
	 */
	public Application getApplication() {
		return application;
	}
	
	/**
	 * Sets the application.
	 * 
	 * @param application the application
	 */
	public void setApplication(Application application) {
		this.application = application;
	}

}
