package org.fog.application;

/**
 * Class which represents application edges which connect modules together and represent data dependency between them.
 * 
 * @author Harshit Gupta
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class AppEdge {
	public static final int SENSOR = 1; // Application edge originates from a sensor
	public static final int ACTUATOR = 2; // Application edge leads to an actuator
	public static final int MODULE = 3; // Application edge is between application modules
	
	/** Name of source application module */
	private String source;
	
	/** Name of destination application module */
	private String destination;
	
	/** CPU length (in MIPS) of tuples carried by the application edge */
	private double tupleCpuLength;
	
	/** Network length (in kilobytes) of tuples carried by the application edge */
	private double tupleNwLength;
	
	/** Type of tuples carried by the application edge */
	private String tupleType;
	
	/** Origin or the destination of the edge */
	private int edgeType;
	
	/** Periodicity of the application edge (in case it is periodic) */
	private double periodicity;
	
	/** Denotes if the application edge is a periodic edge */
	private boolean isPeriodic;
	
	/**
	 * Creates a new non periodic application edge.
	 * 
	 * @param source the name of source application module
	 * @param destination the name of destination application module
	 * @param tupleCpuLength the CPU length (in MIPS) of tuples carried by the application edge
	 * @param tupleNwLength the network length (in bytes) of tuples carried by the application edge
	 * @param tupleType the type of tuples carried by the application edge
	 * @param edgeType the origin or the destination of the edge
	 */
	public AppEdge(String source, String destination, double tupleCpuLength, 
			double tupleNwLength, String tupleType, int edgeType){
		setSource(source);
		setDestination(destination);
		setTupleCpuLength(tupleCpuLength);
		setTupleNwLength(tupleNwLength);
		setTupleType(tupleType);
		setEdgeType(edgeType);
		setPeriodic(false);
	}
	
	/**
	 * Creates a new periodic application edge.
	 * 
	 * @param source the name of source application module
	 * @param destination the name of destination application module
	 * @param periodicity the periodicity of the application edge
	 * @param tupleCpuLength the CPU length (in MIPS) of tuples carried by the application edge
	 * @param tupleNwLength the network length (in bytes) of tuples carried by the application edge
	 * @param tupleType the type of tuples carried by the application edge
	 * @param edgeType the origin or the destination of the edge
	 */
	public AppEdge(String source, String destination, double periodicity, double tupleCpuLength,
			double tupleNwLength, String tupleType, int edgeType){
		setSource(source);
		setDestination(destination);
		setTupleCpuLength(tupleCpuLength);
		setTupleNwLength(tupleNwLength);
		setTupleType(tupleType);
		setEdgeType(edgeType);
		setPeriodic(true);
		setPeriodicity(periodicity);
	}
	
	/**
	 * Updates all values of the non periodic application edge.
	 * 
	 * @param source the name of source application module
	 * @param destination the name of destination application module
	 * @param tupleCpuLength the CPU length (in MIPS) of tuples carried by the application edge
	 * @param tupleNwLength the network length (in bytes) of tuples carried by the application edge
	 * @param tupleType the type of tuples carried by the application edge
	 * @param edgeType the origin or the destination of the edge
	 */
	public void setValues(String source, String destination, double tupleCpuLength,  double tupleNwLength,
			String tupleType, int edgeType){
		setSource(source);
		setDestination(destination);
		setTupleCpuLength(tupleCpuLength);
		setTupleNwLength(tupleNwLength);
		setTupleType(tupleType);
		setEdgeType(edgeType);
		setPeriodic(false);
	}
	
	/**
	 * Updates all values of the periodic application edge.
	 * 
	 * @param source the name of source application module
	 * @param destination the name of destination application module
	 * @param periodicity the periodicity of the application edge
	 * @param tupleCpuLength the CPU length (in MIPS) of tuples carried by the application edge
	 * @param tupleNwLength the network length (in bytes) of tuples carried by the application edge
	 * @param tupleType the type of tuples carried by the application edge
	 * @param edgeType the origin or the destination of the edge
	 */
	public void setValues(String source, String destination, double periodicity, double tupleCpuLength, 
			double tupleNwLength, String tupleType, int edgeType){
		setSource(source);
		setDestination(destination);
		setTupleCpuLength(tupleCpuLength);
		setTupleNwLength(tupleNwLength);
		setTupleType(tupleType);
		setEdgeType(edgeType);
		setPeriodic(true);
		setPeriodicity(periodicity);
	}
	
	/**
	 * Gets the source application module.
	 * 
	 * @return the source application module
	 */
	public String getSource() {
		return source;
	}
	
	/**
	 * Sets the source application module.
	 * 
	 * @param source the source application module
	 */
	public void setSource(String source) {
		this.source = source;
	}
	
	/**
	 * Gets the name of destination application module.
	 * 
	 * @return the name of destination application module
	 */
	public String getDestination() {
		return destination;
	}
	
	/**
	 * Sets the name of destination application module.
	 * 
	 * @param destination the name of destination application module
	 */
	public void setDestination(String destination) {
		this.destination = destination;
	}
	
	/**
	 * Gets the CPU length (in MIPS) of tuples carried by the application edge.
	 * 
	 * @return CPU length (in MIPS) of tuples carried by the application edge
	 */
	public double getTupleCpuLength() {
		return tupleCpuLength;
	}
	
	/**
	 * Sets the CPU length (in MIPS) of tuples carried by the application edge.
	 * 
	 * @param tupleCpuLength the CPU length (in MIPS) of tuples carried by the application edge
	 */
	public void setTupleCpuLength(double tupleCpuLength) {
		this.tupleCpuLength = tupleCpuLength;
	}
	
	/**
	 * Gets the network length (in kilobytes) of tuples carried by the application edge.
	 * 
	 * @return the network length (in kilobytes) of tuples carried by the application edge
	 */
	public double getTupleNwLength() {
		return tupleNwLength;
	}
	
	/**
	 * Sets the network length (in kilobytes) of tuples carried by the application edge.
	 * 
	 * @param tupleNwLength the network length (in kilobytes) of tuples carried by the application edge
	 */
	public void setTupleNwLength(double tupleNwLength) {
		this.tupleNwLength = tupleNwLength;
	}
	
	/**
	 * Gets the type of tuples carried by the application edge.
	 * 
	 * @return the type of tuples carried by the application edge
	 */
	public String getTupleType() {
		return tupleType;
	}
	
	
	/**
	 * Sets the type of tuples carried by the application edge.
	 * 
	 * @param tupleType the type of tuples carried by the application edge
	 */
	public void setTupleType(String tupleType) {
		this.tupleType = tupleType;
	}

	
	/**
	 * Gets the origin or the destination of the edge.
	 * 
	 * @return  Origin or the destination of the edge
	 */
	public int getEdgeType() {
		return edgeType;
	}

	/**
	 * Sets the origin or the destination of the edge.
	 * @param edgeType the origin or the destination of the edge
	 */
	public void setEdgeType(int edgeType) {
		this.edgeType = edgeType;
	}
	
	/**
	 * Gets the periodicity of the application edge.
	 * 
	 * @return the periodicity of the application edge
	 */
	public double getPeriodicity() {
		return periodicity;
	}

	/**
	 * Sets the periodicity of the application edge.
	 * 
	 * @param periodicity the periodicity of the application edge
	 */
	public void setPeriodicity(double periodicity) {
		this.periodicity = periodicity;
	}

	
	/**
	 * Check whether the application edge is a periodic edge.
	 * 
	 * @return true if it is, otherwise false
	 */
	public boolean isPeriodic() {
		return isPeriodic;
	}

	
	/**
	 * Defines whether the application edge is a periodic edge.
	 * 
	 * @param isPeriodic if the application edge is a periodic edge
	 */
	public void setPeriodic(boolean isPeriodic) {
		this.isPeriodic = isPeriodic;
	}

}
