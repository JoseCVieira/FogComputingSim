package org.cloudbus.cloudsim.provisioners;

import org.cloudbus.cloudsim.Vm;

public abstract class ResourceProvisioner {
	
	public abstract boolean allocateResourcesForVm(Vm vm, Number value);
	public abstract Number getAllocatedResourcesForVm(Vm vm);
	public abstract void deallocateResourcesForVm(Vm vm);
	public abstract void deallocateResourcesForAllVms();
	public abstract boolean isSuitableForVm(Vm vm, Number value);
}