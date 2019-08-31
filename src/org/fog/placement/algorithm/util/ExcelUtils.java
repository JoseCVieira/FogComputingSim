package org.fog.placement.algorithm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.fog.application.AppEdge;
import org.fog.application.AppLoop;
import org.fog.application.Application;
import org.fog.core.Config;
import org.fog.entities.FogDevice;
import org.fog.placement.Controller;
import org.fog.placement.algorithm.Job;
import org.fog.placement.algorithm.MultiObjectiveJob;
import org.fog.utils.NetworkMonitor;
import org.fog.utils.TimeKeeper;

/**
 * Class which is responsible for exporting both the algorithm and simulation results to the output excel file.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class ExcelUtils {
	private static final int DEFAULT_LENGTH = 36;
	private static final int START = 1;
	private static final Path path = FileSystems.getDefault().getPath(".");
	private static final String filePath = path + "/output/output.xlsx";
	private static final String ALGORITHM_SHEET = "Algorithm";
	private static final String RESULTS_SHEET = "Results";
	
	/** The identifier of the simulation */
	private static int SIMULATION_ID = -1;
	
	/**
	 * Writes a given solution to the output excel file.
	 * 
	 * @param solution the solution
	 * @param algName the name of the algorithm used
	 * @throws IOException if the file does not exists
	 */
	public static void writeExcel(Job solution, String algName) throws IOException {
		File file = new File(filePath);
		file.createNewFile(); // If the named file does not exist create it
		
	    int rowIndex = getFirstEmptyRowIndex(filePath, ALGORITHM_SHEET);
		
	    Workbook workbook;
	    Sheet sheet;
	    
	    // Create a new sheet
		if(rowIndex == START) {
			workbook = new HSSFWorkbook();
			workbook.createSheet(ALGORITHM_SHEET);
			workbook.createSheet(RESULTS_SHEET);
			sheet = workbook.getSheet(ALGORITHM_SHEET);
			
			Row row = sheet.createRow(rowIndex++);
		    
		    int cellIndex = START;
		    
		    createTitleCell(sheet, row, cellIndex++, 65, "Simul. Id");
		    createTitleCell(sheet, row, cellIndex++, 240, "Name");
		    
		    for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
		    	createTitleCell(sheet, row, cellIndex++, 70, Config.objectiveNames[i] + " prio.");
		    }
		    
		    for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
		    	createTitleCell(sheet, row, cellIndex++, 85, Config.objectiveNames[i] + " value");
		    }
		    
		    createTitleCell(sheet, row, cellIndex++, 100, "Total");
		    createTitleCell(sheet, row, cellIndex++, 150, "Time stamp");
		    
		    if(SIMULATION_ID == -1) {
		    	SIMULATION_ID = 0;
		    }
		    
		    writeSolution(sheet, rowIndex, solution, algName);
		    
		// Edit the current sheet
		}else {
			FileInputStream fileInputStream = new FileInputStream(new File(filePath));
            workbook = new HSSFWorkbook(fileInputStream);
            sheet = workbook.getSheet(ALGORITHM_SHEET);
            
            if(SIMULATION_ID == -1) {
            	SIMULATION_ID = (int) Double.parseDouble(sheet.getRow(rowIndex-1).getCell(1).toString()) + 1;
            	
            	// Create a separator
            	Row row = sheet.createRow(rowIndex++);
            	CellStyle cellStyle = row.getSheet().getWorkbook().createCellStyle();
        	    cellStyle.setAlignment(HorizontalAlignment.CENTER);
        	    Cell cell = row.createCell(1);
        	    cell.setCellValue("-");
        	    cell.setCellStyle(cellStyle);
            }
            
            writeSolution(sheet, rowIndex, solution, algName);

            fileInputStream.close();
		}
		
		try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
	        workbook.write(outputStream);
	    }
		
		workbook.close();
	}
	
	/**
	 * Writes a given simulation result to the output excel file.
	 * 
	 * @param controller the controller used in the simulation
	 * @throws IOException if the file does not exists
	 */
	public static void writeExcel(Controller controller) throws IOException {
		File file = new File(filePath);
		file.createNewFile(); // If the named file does not exist create it
		
	    int rowIndex = getFirstEmptyRowIndex(filePath, RESULTS_SHEET);
		
	    FileInputStream fileInputStream = new FileInputStream(new File(filePath));
	    Workbook workbook = new HSSFWorkbook(fileInputStream);
	    Sheet sheet = workbook.getSheet(RESULTS_SHEET);
	    
	    // Create the clomun names
	    if(rowIndex == START) {
	    	int cellIndex = START;
	    	Row row = sheet.createRow(rowIndex++);
	    	
	    	createTitleCell(sheet, row, cellIndex++, 65, "Simul. Id");
	    	createTitleCell(sheet, row, cellIndex++, 140, "Execution time [s]");
		    createTitleCell(sheet, row, cellIndex++, 125, "CPU delay [s]");
		    createTitleCell(sheet, row, cellIndex++, 125, "LAT delay [s]");
		    createTitleCell(sheet, row, cellIndex++, 125, "BW delay [s]");
		    createTitleCell(sheet, row, cellIndex++, 125, "Total delay [s]");
		    createTitleCell(sheet, row, cellIndex++, 125, "Energy [W]");
		    createTitleCell(sheet, row, cellIndex++, 125, "Ordered MI [MI]");
		    createTitleCell(sheet, row, cellIndex++, 125, "Processed MI [MI]");
		    createTitleCell(sheet, row, cellIndex++, 125, "Cost [€]");
		    createTitleCell(sheet, row, cellIndex++, 125, "Network usage [s]");
		    createTitleCell(sheet, row, cellIndex++, 90, "# pkt success");
		    createTitleCell(sheet, row, cellIndex++, 90, "# pkt drop");
		    createTitleCell(sheet, row, cellIndex++, 90, "# handover");
		    createTitleCell(sheet, row, cellIndex++, 90, "# migration");
		    
		// Create a separator
	    }else {
	    	Row row = sheet.createRow(rowIndex++);
	    	CellStyle cellStyle = row.getSheet().getWorkbook().createCellStyle();
		    cellStyle.setAlignment(HorizontalAlignment.CENTER);
		    Cell cell = row.createCell(1);
		    cell.setCellValue("-");
		    cell.setCellStyle(cellStyle);
	    }
    	    
	    writeResult(sheet, rowIndex, controller);

        fileInputStream.close();
		
	    try (FileOutputStream outputStream = new FileOutputStream(filePath)) {
	        workbook.write(outputStream);
	    }
	    
		workbook.close();
	}
	
	/**
	 * Parses and writes the solution in the output excel file.
	 * 
	 * @param sheet the excel page
	 * @param rowIndex the row index
	 * @param solution the solution to be written
	 * @param algName the algorithm name used
	 */
	private static void writeSolution(Sheet sheet, int rowIndex, Job solution, String algName) {
		Row row = sheet.createRow(rowIndex);
		CellStyle cellStyle = row.getSheet().getWorkbook().createCellStyle();
	    cellStyle.setAlignment(HorizontalAlignment.CENTER);
	    
	    int cellIndex = START;
	    
	    createCell(sheet, row, cellIndex++, Integer.toString(SIMULATION_ID), HorizontalAlignment.CENTER);
	    createCell(sheet, row, cellIndex++, algName, HorizontalAlignment.CENTER);
	    
	    DecimalFormat df = new DecimalFormat("0.00000");
	    if(solution instanceof MultiObjectiveJob) {
	    	MultiObjectiveJob sol = (MultiObjectiveJob) solution;
		    for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
		    	 createCell(sheet, row, cellIndex++, Integer.toString(Config.priorities[i]), HorizontalAlignment.CENTER);
		    }
		    
		    for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
		    	createCell(sheet, row, cellIndex++, df.format(sol.getDetailedCost(i)), HorizontalAlignment.RIGHT);
		    }
		    
		    createCell(sheet, row, cellIndex++, "NA", HorizontalAlignment.CENTER);
	    }else {
	    	for(int i = 0; i < Config.NR_OBJECTIVES*2; i++) {
	    		createCell(sheet, row, cellIndex++, "NA", HorizontalAlignment.CENTER);
	    	}
	    	
	    	createCell(sheet, row, cellIndex++, df.format(solution.getCost()), HorizontalAlignment.RIGHT);
	    }
	    
	    String timeStamp = new SimpleDateFormat("HH:mm:ss @ dd.MM.yyyy", Locale.US).format(new Date());
	    createCell(sheet, row, cellIndex++, timeStamp, HorizontalAlignment.CENTER);
	}
	
	/**
	 * Parses and writes the result of the simulation in the output excel file.
	 * 
	 * @param sheet the excel page
	 * @param rowIndex the row index
	 * @param controller the controller used in the simulation
	 */
	private static void writeResult(Sheet sheet, int rowIndex, Controller controller) {
		Row row = sheet.createRow(rowIndex);
		CellStyle cellStyle = row.getSheet().getWorkbook().createCellStyle();
	    cellStyle.setAlignment(HorizontalAlignment.CENTER);
	    DecimalFormat df = new DecimalFormat("0.000");
	    
		int cellIndex = START;
		
		// ID
		createCell(sheet, row, cellIndex++, Integer.toString(SIMULATION_ID), HorizontalAlignment.CENTER);
		
		// Elapsed time
		String value = Long.toString(Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime());
		createCell(sheet, row, cellIndex++, value, HorizontalAlignment.CENTER);
	    
		// Loops delays
		double cpu = 0;
		double lat = 0;
		double bw = 0;
		double total = 0;
		for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
			List<String> modules = getListForLoopId(controller, loopId);
			
			for(int i = 0; i < modules.size()-1; i++) {
				String startModule = modules.get(i);
				String destModule = modules.get(i+1);
				
				for(String tupleType : getTupleTypeForDependency(controller, startModule, destModule)) {
					if(TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().containsKey(tupleType))
						cpu += TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType);
				}
			}
			
			for(int i = 0; i < modules.size()-1; i++) {
				String startModule = modules.get(i);
				String destModule = modules.get(i+1);
				
				for(String tupleType : getTupleTypeForDependency(controller, startModule, destModule)) {
					if(TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().containsKey(tupleType)) {
						Map<Double, Integer> map = TimeKeeper.getInstance().getLoopIdToCurrentNwLatAverage().get(tupleType);
						double totalLat = map.entrySet().iterator().next().getKey();
						int counter = map.entrySet().iterator().next().getValue();
						lat += totalLat/counter;
						
						map = TimeKeeper.getInstance().getLoopIdToCurrentNwBwAverage().get(tupleType);
						double totalBw = map.entrySet().iterator().next().getKey();
						bw += totalBw/counter;
					}
				}
			}
			
			total += TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
		}
		
		df = new DecimalFormat("0.######E0");
		String bwf = df.format(bw);
		df = new DecimalFormat("0.000");
		
		createCell(sheet, row, cellIndex++, df.format(cpu), HorizontalAlignment.RIGHT);
		createCell(sheet, row, cellIndex++, df.format(lat), HorizontalAlignment.RIGHT);
		createCell(sheet, row, cellIndex++, bwf, HorizontalAlignment.RIGHT);
		createCell(sheet, row, cellIndex++, df.format(total), HorizontalAlignment.RIGHT);
	    
	    // Energy
	    total = 0;
	    for(FogDevice fogDevice : controller.getFogDevices()) {
	    	total += fogDevice.getEnergyConsumption();
		}
	    createCell(sheet, row, cellIndex++, df.format(total), HorizontalAlignment.RIGHT);
	    
	    // Ordered MIs
	    total = 0;
	    for(FogDevice fogDevice : controller.getFogDevices()) {
	    	total += fogDevice.getProcessorMonitor().getOrderedMI();
		}
	    createCell(sheet, row, cellIndex++, df.format(total), HorizontalAlignment.RIGHT);
	    
	    // Processed MIs
	    total = 0;
	    for(FogDevice fogDevice : controller.getFogDevices()) {
	    	total += fogDevice.getProcessorMonitor().getProcessedMI();
		}
	    createCell(sheet, row, cellIndex++, df.format(total), HorizontalAlignment.RIGHT);
	    
	    // Cost
	    total = 0;
	    for(FogDevice fogDevice : controller.getFogDevices()) {
	    	total += fogDevice.getTotalCost();
		}
	    createCell(sheet, row, cellIndex++, df.format(total), HorizontalAlignment.RIGHT);
	    
	    // Network details
	    createCell(sheet, row, cellIndex++, df.format(NetworkMonitor.getNetworkUsage()), HorizontalAlignment.RIGHT);
	    createCell(sheet, row, cellIndex++, Integer.toString(NetworkMonitor.getPacketSuccess()), HorizontalAlignment.RIGHT);
	    createCell(sheet, row, cellIndex++, Integer.toString(NetworkMonitor.getPacketDrop()), HorizontalAlignment.RIGHT);
	    createCell(sheet, row, cellIndex++, Integer.toString(controller.getNrHandovers()), HorizontalAlignment.RIGHT);
	    createCell(sheet, row, cellIndex++, Integer.toString(controller.getNrMigrations()), HorizontalAlignment.RIGHT);
	}
	
	
	/**
	 * Creates a cell inside the title row.
	 * 
	 * @param sheet the excel page
	 * @param row the row index
	 * @param index the cell index
	 * @param size the size of the column
	 * @param value the text to be written inside the cell
	 */
	private static void createTitleCell(Sheet sheet, Row row, int index, int size, String value) {
		CellStyle cellStyle = row.getSheet().getWorkbook().createCellStyle();
	    cellStyle.setAlignment(HorizontalAlignment.CENTER);
	    
	    Cell cell = row.createCell(index);
	    cell.setCellValue(value);
	    sheet.setColumnWidth(index, size*DEFAULT_LENGTH);
	    cell.setCellStyle(cellStyle);
	}
	
	/**
	 * Creates a cell inside a given row.
	 * 
	 * @param sheet the excel page
	 * @param row the row index
	 * @param index the cell index
	 * @param value the text to be written inside the cell
	 */
	private static void createCell(Sheet sheet, Row row, int index, String value, HorizontalAlignment h) {
		CellStyle cellStyle = row.getSheet().getWorkbook().createCellStyle();
	    cellStyle.setAlignment(h);
	    
	    Cell cell = row.createCell(index);
	    cell.setCellValue(value);
	    cell.setCellStyle(cellStyle);
	}
	
	/**
	 * Gets the first empty row inside a given excel page which is located in a given path.
	 * 
	 * @param filePath the path of the excel file
	 * @param sheetName the name of the excel page
	 * @return the first empty row inside the page
	 */
	private static int getFirstEmptyRowIndex(String filePath, String sheetName) {
		int index = 1;
		
		try {
			FileInputStream excelFile = new FileInputStream(new File(filePath));
			Workbook workbook = new HSSFWorkbook(excelFile);
			Sheet datatypeSheet = workbook.getSheet(sheetName);
			
			Iterator<Row> iterator = datatypeSheet.iterator();
			while (iterator.hasNext()) {
				iterator.next();
				index++;
			}
			
			workbook.close();
		} catch (Exception e) {
			// Do nothing
		}
		
		return index;
	}
	
	private static List<String> getListForLoopId(Controller controller, int loopId) {
		for(String appId : controller.getApplications().keySet()){
			Application app = controller.getApplications().get(appId);
			for(AppLoop loop : app.getLoops()){
				if(loop.getLoopId() == loopId)
					return loop.getModules();
			}
		}
		return null;
	}
	
	private static List<String> getTupleTypeForDependency(Controller controller, String startModule, String destModule) {
		List<String> tupleTypes = new ArrayList<String>();
		
		for(String appId : controller.getApplications().keySet()){
			Application app = controller.getApplications().get(appId);
			for(AppEdge appEdge : app.getEdges()){
				if(!appEdge.getSource().equals(startModule) || !appEdge.getDestination().equals(destModule)) continue;
				if(tupleTypes.contains(appEdge.getTupleType())) continue;
				tupleTypes.add(appEdge.getTupleType());
			}
		}
		return tupleTypes;
	}
	
}
