package org.fog.gui.core;

import java.io.Serializable;

import org.fog.core.Config;

public class ActuatorGui extends Node implements Serializable{
	private static final long serialVersionUID = 4087896123649020073L;

	private String name;
	private String actuatorType;
	
	public ActuatorGui(String name){
		super(name, Config.ACTUATOR_TYPE);
		setName(name);
		setActuatorType("");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getActuatorType() {
		return actuatorType;
	}

	public void setActuatorType(String actuatorType) {
		this.actuatorType = actuatorType;
	}
	
	@Override
	public String toString() {
		return "Actuator [name=" + name + " type=" + actuatorType + "]";
	}
}