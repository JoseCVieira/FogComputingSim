package org.fog.placement.algorithm.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
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
	    	createTitleCell(sheet, row, cellIndex++, 150, "Execution time (s)");
		    createTitleCell(sheet, row, cellIndex++, 190, "Application Loop delays (s)");
		    createTitleCell(sheet, row, cellIndex++, 190, "Tuple CPU execution delay (s)");
		    createTitleCell(sheet, row, cellIndex++, 190, "Energy consumed (W)");
		    createTitleCell(sheet, row, cellIndex++, 190, "Cost of execution (€)");
		    createTitleCell(sheet, row, cellIndex++, 190, "Network usage (%)");
		    createTitleCell(sheet, row, cellIndex++, 125, "# packet success");
		    createTitleCell(sheet, row, cellIndex++, 125, "# packet drop");
		    createTitleCell(sheet, row, cellIndex++, 125, "# handovers");
		    createTitleCell(sheet, row, cellIndex++, 125, "# migrations");
		    
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
	    
	    createCell(sheet, row, cellIndex++, Integer.toString(SIMULATION_ID));
	    createCell(sheet, row, cellIndex++, algName);
	    
	    DecimalFormat df = new DecimalFormat("0.00000");
	    if(solution instanceof MultiObjectiveJob) {
	    	MultiObjectiveJob sol = (MultiObjectiveJob) solution;
		    for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
		    	 createCell(sheet, row, cellIndex++, Integer.toString(Config.priorities[i]));
		    }
		    
		    for(int i = 0; i < Config.NR_OBJECTIVES; i++) {
		    	createCell(sheet, row, cellIndex++, df.format(sol.getDetailedCost(i)));
		    }
		    
		    createCell(sheet, row, cellIndex++, "NA");
	    }else {
	    	for(int i = 0; i < Config.NR_OBJECTIVES*2; i++) {
	    		createCell(sheet, row, cellIndex++, "NA");
	    	}
	    	
	    	createCell(sheet, row, cellIndex++, df.format(solution.getCost()));
	    }
	    
	    String timeStamp = new SimpleDateFormat("HH:mm:ss @ dd.MM.yyyy", Locale.US).format(new Date());
	    createCell(sheet, row, cellIndex++, timeStamp);
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
	    
		int cellIndex = START;
		
		createCell(sheet, row, cellIndex++, Integer.toString(SIMULATION_ID));
		
		String value = Long.toString(Calendar.getInstance().getTimeInMillis() - TimeKeeper.getInstance().getSimulationStartTime());
		createCell(sheet, row, cellIndex++, value);
	    
	    double total = 0;
	    DecimalFormat df = new DecimalFormat("0.00");
	    for(Integer loopId : TimeKeeper.getInstance().getLoopIdToTupleIds().keySet()) {
	    	total += TimeKeeper.getInstance().getLoopIdToCurrentAverage().get(loopId);
		}
	    
	    createCell(sheet, row, cellIndex++, Double.toString(total));
	    
	    total = 0;
	    for(String tupleType : TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().keySet()) {
	    	total += TimeKeeper.getInstance().getTupleTypeToAverageCpuTime().get(tupleType);
		}
	    createCell(sheet, row, cellIndex++, Double.toString(total));
	    
	    total = 0;
	    for(FogDevice fogDevice : controller.getFogDevices()) {
	    	total += fogDevice.getEnergyConsumption();
		}
	    createCell(sheet, row, cellIndex++, Double.toString(total));
	    
	    total = 0;
	    for(FogDevice fogDevice : controller.getFogDevices()) {
	    	total += fogDevice.getTotalCost();
		}
	    createCell(sheet, row, cellIndex++, Double.toString(total));
	    
	    df = new DecimalFormat("0.00000000");
	    createCell(sheet, row, cellIndex++, df.format(NetworkMonitor.getNetworkUsage()/Config.MAX_SIMULATION_TIME));
	    
	    createCell(sheet, row, cellIndex++, Integer.toString(NetworkMonitor.getPacketSuccess()));
	    createCell(sheet, row, cellIndex++, Integer.toString(NetworkMonitor.getPacketDrop()));
	    createCell(sheet, row, cellIndex++, Integer.toString(controller.getNrHandovers()));
	    createCell(sheet, row, cellIndex++, Integer.toString(controller.getNrMigrations()));
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
	private static void createCell(Sheet sheet, Row row, int index, String value) {
		CellStyle cellStyle = row.getSheet().getWorkbook().createCellStyle();
	    cellStyle.setAlignment(HorizontalAlignment.CENTER);
	    
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
	
}
