package org.fog.core;

import java.util.ArrayList;
import java.util.List;

import org.fog.application.Application;
import org.fog.entities.Actuator;
import org.fog.entities.FogBroker;
import org.fog.entities.FogDevice;
import org.fog.entities.Sensor;
import org.fog.placement.Controller;

public abstract class FogTest {
	protected static List<Application> applications = new ArrayList<Application>();
	protected static List<FogBroker> fogBrokers = new ArrayList<FogBroker>();
	protected static List<FogDevice> fogDevices = new ArrayList<FogDevice>();
	protected static List<Actuator> actuators = new ArrayList<Actuator>();
	protected static List<Sensor> sensors = new ArrayList<Sensor>();
	protected static Controller controller = null;
	
	public List<Application> getApplications() {
		return applications;
	}
	
	public List<FogBroker> getFogBrokers() {
		return fogBrokers;
	}
	
	public List<FogDevice> getFogDevices() {
		return fogDevices;
	}
	
	public List<Actuator> getActuators() {
		return actuators;
	}
	
	public List<Sensor> getSensors() {
		return sensors;
	}
	
	public Controller getController() {
		return controller;
	}
}
