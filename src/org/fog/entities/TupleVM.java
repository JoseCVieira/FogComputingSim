package org.fog.entities;

import org.cloudbus.cloudsim.UtilizationModel;
import org.fog.application.AppModule;
import org.fog.application.Application;

public class TupleVM extends Tuple {
	private AppModule vm;
	private Application application;
	private int destId;
	private int srcId;

	public TupleVM(String appId, int cloudletId, long cloudletLength, int pesNumber, long cloudletFileSize,
			long cloudletOutputSize, UtilizationModel utilizationModelCpu, UtilizationModel utilizationModelRam,
			UtilizationModel utilizationModelBw, AppModule vm, Application application, int srcId, int destId) {
		super(appId, cloudletId, cloudletLength, pesNumber, cloudletFileSize, cloudletOutputSize, utilizationModelCpu,
				utilizationModelRam, utilizationModelBw);
		
		setVm(vm);
		setApplication(application);
		setSrcId(srcId);
		setDestId(destId);
	}

	public AppModule getVm() {
		return vm;
	}

	public void setVm(AppModule vm) {
		this.vm = vm;
	}

	public Application getApplication() {
		return application;
	}

	public void setApplication(Application application) {
		this.application = application;
	}

	public int getDestId() {
		return destId;
	}

	public void setDestId(int destId) {
		this.destId = destId;
	}

	public int getSrcId() {
		return srcId;
	}

	public void setSrcId(int srcId) {
		this.srcId = srcId;
	}

}
