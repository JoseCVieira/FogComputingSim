package org.fog.test;

import java.util.ArrayList;
import java.util.List;

import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.application.selectivity.FractionalSelectivity;

/**
 * Class which defines several example applications to test the simulator.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class ApplicationsExample {
	private static List<Application> exampleApplications = new ArrayList<Application>();
	
	/**
	 * Creates several example applications to test the simulator.
	 */
	@SuppressWarnings("serial")
	public static void createExampleApplications() {
		Application application = new Application("VRGame");
		application.addAppModule("client", 100, 25, true, false);
		application.addAppModule("calculator", 100, 30, false, false);
		application.addAppModule("connector", 100, 30, false, false);
		
		application.addAppEdge("EEG", "client", 3000, 500, "EEG", AppEdge.SENSOR);
		application.addAppEdge("client", "calculator", 3500, 500, "_SENSOR", AppEdge.MODULE);
		application.addAppEdge("calculator", "connector", 100, 1000, 1000, "PLAYER_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("calculator", "client", 14, 500, "CONCENTRATION", AppEdge.MODULE);
		application.addAppEdge("connector", "client", 100, 28, 1000, "GLOBAL_GAME_STATE", AppEdge.MODULE);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "SELF_STATE_UPDATE", AppEdge.ACTUATOR);
		application.addAppEdge("client", "DISPLAY", 1000, 500, "GLOBAL_STATE_UPDATE", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client", "EEG", "_SENSOR", new FractionalSelectivity(0.9));
		application.addTupleMapping("client", "CONCENTRATION", "SELF_STATE_UPDATE", new FractionalSelectivity(1.0));
		application.addTupleMapping("calculator", "_SENSOR", "CONCENTRATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "GLOBAL_GAME_STATE", "GLOBAL_STATE_UPDATE", new FractionalSelectivity(1.0));
		
		final AppLoop loop1 = new AppLoop(new ArrayList<String>(){{add("EEG");add("client");add("calculator");add("client");add("DISPLAY");}}, 15);
		List<AppLoop> loops = new ArrayList<AppLoop>(){{add(loop1);}};
		application.setLoops(loops);
		exampleApplications.add(application);
		
		application = new Application("VRGame_MP");
		application.addAppModule("client_MP", 100, 25, true, false);
		application.addAppModule("calculator_MP", 100, 30, false, false);
		application.addAppModule("connector_MP", 100, 30, false, true);
		
		application.addAppEdge("EEG_MP", "client_MP", 3000, 500, "EEG_MP", AppEdge.SENSOR);
		application.addAppEdge("client_MP", "calculator_MP", 3500, 500, "_SENSOR_MP", AppEdge.MODULE);
		application.addAppEdge("calculator_MP", "connector_MP", 100, 1000, 1000, "PLAYER_GAME_STATE_MP", AppEdge.MODULE);
		application.addAppEdge("calculator_MP", "client_MP", 14, 500, "CONCENTRATION_MP", AppEdge.MODULE);
		application.addAppEdge("connector_MP", "client_MP", 100, 28, 1000, "GLOBAL_GAME_STATE_MP", AppEdge.MODULE);
		application.addAppEdge("client_MP", "DISPLAY_MP", 1000, 500, "SELF_STATE_UPDATE_MP", AppEdge.ACTUATOR);
		application.addAppEdge("client_MP", "DISPLAY_MP", 1000, 500, "GLOBAL_STATE_UPDATE_MP", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client_MP", "EEG_MP", "_SENSOR_MP", new FractionalSelectivity(0.9));
		application.addTupleMapping("client_MP", "CONCENTRATION_MP", "SELF_STATE_UPDATE_MP", new FractionalSelectivity(1.0));
		application.addTupleMapping("calculator_MP", "_SENSOR_MP", "CONCENTRATION_MP", new FractionalSelectivity(1.0));
		application.addTupleMapping("client_MP", "GLOBAL_GAME_STATE_MP", "GLOBAL_STATE_UPDATE_MP", new FractionalSelectivity(1.0));
		
		final AppLoop loop2 = new AppLoop(new ArrayList<String>(){{add("EEG_MP");add("client_MP");add("calculator_MP");add("client_MP");add("DISPLAY_MP");}}, 15);
		loops = new ArrayList<AppLoop>(){{add(loop2);}};
		application.setLoops(loops);
		exampleApplications.add(application);
		
		application = new Application("DCNS");
		application.addAppModule("object_detector", 100, 30, false, false);
		application.addAppModule("motion_detector", 100, 30, false, false);
		application.addAppModule("object_tracker", 100, 30, false, false);
		application.addAppModule("user_interface", 100, 25, true, false);
		
		application.addAppEdge("CAMERA", "motion_detector", 1000, 20000, "CAMERA", AppEdge.SENSOR);
		application.addAppEdge("motion_detector", "object_detector", 2000, 2000, "MOTION_VIDEO_STREAM", AppEdge.MODULE);
		application.addAppEdge("object_detector", "user_interface", 500, 2000, "DETECTED_OBJECT", AppEdge.MODULE);
		application.addAppEdge("object_detector", "object_tracker", 1000, 100, "OBJECT_LOCATION", AppEdge.MODULE);
		application.addAppEdge("object_tracker", "PTZ_CONTROL", 100, 28, 100, "PTZ_PARAMS", AppEdge.ACTUATOR);
		
		application.addTupleMapping("motion_detector", "CAMERA", "MOTION_VIDEO_STREAM", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "OBJECT_LOCATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("object_detector", "MOTION_VIDEO_STREAM", "DETECTED_OBJECT", new FractionalSelectivity(0.05));
		
		final AppLoop loop3 = new AppLoop(new ArrayList<String>(){{add("motion_detector");add("object_detector");add("object_tracker");}}, 10);
		final AppLoop loop4 = new AppLoop(new ArrayList<String>(){{add("object_tracker");add("PTZ_CONTROL");}}, 10);
		loops = new ArrayList<AppLoop>(){{add(loop3);add(loop4);}};
		application.setLoops(loops);
		exampleApplications.add(application);
		
		application = new Application("TEMP");
		application.addAppModule("client", 100, 25, false, false);
		application.addAppModule("classifier", 100, 30, false, false);
		application.addAppModule("tuner", 100, 30, false, false);
	
		application.addAppEdge("TEMP", "client", 1000, 100, "TEMP", AppEdge.SENSOR);
		application.addAppEdge("client", "classifier", 8000, 100, "_SENSOR", AppEdge.MODULE);
		application.addAppEdge("classifier", "tuner", 1000000, 100, "HISTORY", AppEdge.MODULE);
		application.addAppEdge("classifier", "client", 1000, 100, "CLASSIFICATION", AppEdge.MODULE);
		application.addAppEdge("tuner", "classifier", 1000, 100, "TUNING_PARAMS", AppEdge.MODULE);
		application.addAppEdge("client", "MOTOR", 1000, 100, "ACTUATOR", AppEdge.ACTUATOR);
		
		application.addTupleMapping("client", "TEMP", "_SENSOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("client", "CLASSIFICATION", "ACTUATOR", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "CLASSIFICATION", new FractionalSelectivity(1.0));
		application.addTupleMapping("classifier", "_SENSOR", "HISTORY", new FractionalSelectivity(0.1));
		application.addTupleMapping("tuner", "HISTORY", "TUNING_PARAMS", new FractionalSelectivity(1.0));
		
		final AppLoop loop5 = new AppLoop(new ArrayList<String>(){{add("TEMP");add("client");add("classifier");add("client");add("MOTOR");}}, 10);
		final AppLoop loop6 = new AppLoop(new ArrayList<String>(){{add("classifier");add("tuner");add("classifier");}}, 10);
		loops = new ArrayList<AppLoop>(){{add(loop5);add(loop6);}};
		application.setLoops(loops);
		exampleApplications.add(application);
	}
	
	/**
	 * Gets a specific application example based on its name.
	 * 
	 * @param name the name of the application
	 * @return the application example
	 */
	public static Application getAppExampleByName(String name) {
		for(Application app : exampleApplications)
			if(app.getAppId().equals(name))
				return app;
		return null;
	}
	
	public static Application getAppExampleByIndex(int index) {
		if(index < 0 || index >= exampleApplications.size()) return null;
		return exampleApplications.get(index);
	}
	
	/**
	 * Adds a new application to the example applications list.
	 * 
	 * @return the application
	 */
	public static void addApplicationExample(Application application) {
		exampleApplications.add(application);
	}
	
	/**
	 * Gets the size of example applications list.
	 * 
	 * @return the size of example applications list
	 */
	public static int getNumberOfApplicationsExample() {
		return exampleApplications.size();
	}
	
	/**
	 * Removes all applications.
	 */
	public static void clean() {
		exampleApplications.clear();
	}
	
}
