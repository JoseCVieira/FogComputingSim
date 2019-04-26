package org.fog.placement.algorithms.placement;

public class AlgorithmMathUtils {
	
	/*
	 *  Scalar/dot multiplication
	 */
	public static <T extends Number, K extends Number> double[] scalarMultiplication(T[] vector, K constant)
			throws IllegalArgumentException {
		if(vector == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		double[] result = new double[vector.length];
		
		for(int i = 0; i < vector.length; i++)
			result[i] = vector[i].doubleValue() * constant.doubleValue();
		
		return result;
	}
	
	public static <T extends Number, K extends Number> double[][] scalarMultiplication(T[][] matrix, K constant)
			throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int r = matrix.length;
		int c = matrix[0].length;
		
		double[][] result = new double[r][c];
		
		for(int i = 0; i < r ;i++)
			for(int j = 0; j < c ;j++)
				result[i][j] = matrix[i][j].doubleValue() * constant.doubleValue();
				
		return result;	
	}
	
	public static <T extends Number, K extends Number> double[] scalarMultiplication(T[] vector1, K[] vector2)
			throws IllegalArgumentException {
		if(vector1 == null || vector2 == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int s1 = vector1.length;
		int s2 = vector2.length;
		
		if(s1 != s2)
			throw new IllegalArgumentException("AlgorithmUtils Err: Impossible to preform the required multiplication.");
		
		double[] result = new double[vector1.length];
		
		for(int i = 0; i < vector1.length; i++)
			result[i] = vector1[i].doubleValue() * vector2[i].doubleValue();
		
		return result;
	}
	
	public static <T extends Number, K extends Number> double[][] scalarMultiplication(T[][] matrix1, K[][] matrix2)
			throws IllegalArgumentException {
		if(matrix1 == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int r1 = matrix1.length;
		int c1 = matrix1[0].length;
		int r2 = matrix2.length;
		int c2 = matrix2[0].length;
		
		if(c1 != c2 || r1 !=r2)
			throw new IllegalArgumentException("AlgorithmUtils Err: Impossible to preform the required multiplication.");
		
		double[][] result = new double[r1][c1];
		
		for(int i = 0; i < r1 ;i++)
			for(int j = 0; j < c1 ;j++)
				result[i][j] = matrix1[i][j].doubleValue() * matrix2[i][j].doubleValue();
				
		return result;	
	}
	
	/*
	 *  Scalar/dot division
	 */
	public static <T extends Number, K extends Number> double[] scalarDivision(T[] vector, K constant)
			throws IllegalArgumentException {
		if(vector == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		double[] result = new double[vector.length];
		
		for(int i = 0; i < vector.length; i++)
			result[i] = vector[i].doubleValue() / constant.doubleValue();
		
		return result;
	}
	
	public static <T extends Number, K extends Number> double[][] scalarDivision(T[][] matrix, K constant)
			throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int r = matrix.length;
		int c = matrix[0].length;
		
		double[][] result = new double[r][c];
		
		for(int i = 0; i < r ;i++)
			for(int j = 0; j < c ;j++)
				result[i][j] = matrix[i][j].doubleValue() / constant.doubleValue();
				
		return result;	
	}
	
	public static <T extends Number, K extends Number> double[] scalarDivision(T[] vector1, K[] vector2)
			throws IllegalArgumentException {
		if(vector1 == null || vector2 == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int s1 = vector1.length;
		int s2 = vector2.length;
		
		if(s1 != s2)
			throw new IllegalArgumentException("AlgorithmUtils Err: Impossible to preform the required multiplication.");
		
		double[] result = new double[vector1.length];
		
		for(int i = 0; i < vector1.length; i++)
			result[i] = vector1[i].doubleValue() / vector2[i].doubleValue();
		
		return result;
	}
	
	public static <T extends Number, K extends Number> double[][] scalarDivision(T[][] matrix1, K[][] matrix2)
			throws IllegalArgumentException {
		if(matrix1 == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int r1 = matrix1.length;
		int c1 = matrix1[0].length;
		int r2 = matrix2.length;
		int c2 = matrix2[0].length;
		
		if(c1 != c2 || r1 !=r2)
			throw new IllegalArgumentException("AlgorithmUtils Err: Impossible to preform the required multiplication.");
		
		double[][] result = new double[r1][c1];
		
		for(int i = 0; i < r1 ;i++)
			for(int j = 0; j < c1 ;j++)
				result[i][j] = matrix1[i][j].doubleValue() / matrix2[i][j].doubleValue();
				
		return result;	
	}
	
	/*
	 *  Multiplication
	 */	
	public static <T extends Number, K extends Number> double[][] multiply(T[][] matrix1, K[][] matrix2)
			throws IllegalArgumentException {
		if(matrix1 == null || matrix2 == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Some of the received arguments are null.");
		
		int r1 = matrix1.length;
		int c1 = matrix1[0].length;
		int r2 = matrix2.length;
		int c2 = matrix2[0].length;
		
		if(c1 != r2)
			throw new IllegalArgumentException("AlgorithmUtils Err: Impossible to preform the required multiplication.");
		
		double[][] result = new double[r1][c2];
        
        for(int i = 0; i < r1; i++)
            for (int j = 0; j < c2; j++)
                for (int k = 0; k < c1; k++)
                	result[i][j] += matrix1[i][k].doubleValue() * matrix2[k][j].doubleValue();
        
        return result;
	}
	
	/**
	 * Sum
	 */
	public static <T extends Number, K extends Number> double[] sum(T[] vector1, K[] vector2)
			throws IllegalArgumentException {
		if(vector1 == null || vector2 == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int s1 = vector1.length;
		int s2 = vector2.length;
		
		if(s1 != s2)
			throw new IllegalArgumentException("AlgorithmUtils Err: Impossible to preform the required multiplication.");
		
		double[] result = new double[vector1.length];
		
		for(int i = 0; i < vector1.length; i++)
			result[i] = vector1[i].doubleValue() + vector2[i].doubleValue();
		
		return result;
	}
	
	public static <T extends Number, K extends Number> double[][] sum(T[][] matrix1, K[][] matrix2)
			throws IllegalArgumentException {
		if(matrix1 == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int r1 = matrix1.length;
		int c1 = matrix1[0].length;
		int r2 = matrix2.length;
		int c2 = matrix2[0].length;
		
		if(c1 != c2 || r1 !=r2)
			throw new IllegalArgumentException("AlgorithmUtils Err: Impossible to preform the required multiplication.");
		
		double[][] result = new double[r1][c1];
		
		for(int i = 0; i < r1 ;i++)
			for(int j = 0; j < c1 ;j++)
				result[i][j] = matrix1[i][j].doubleValue() + matrix2[i][j].doubleValue();
				
		return result;	
	}
	
	/**
	 * Subtract
	 */
	public static <T extends Number, K extends Number> double[] subtract(T[] vector1, K[] vector2)
			throws IllegalArgumentException {
		if(vector1 == null || vector2 == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int s1 = vector1.length;
		int s2 = vector2.length;
		
		if(s1 != s2)
			throw new IllegalArgumentException("AlgorithmUtils Err: Impossible to preform the required multiplication.");
		
		double[] result = new double[vector1.length];
		
		for(int i = 0; i < vector1.length; i++)
			result[i] = vector1[i].doubleValue() - vector2[i].doubleValue();
		
		return result;
	}
	
	public static <T extends Number, K extends Number> double[][] subtract(T[][] matrix1, K[][] matrix2)
			throws IllegalArgumentException {
		if(matrix1 == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int r1 = matrix1.length;
		int c1 = matrix1[0].length;
		int r2 = matrix2.length;
		int c2 = matrix2[0].length;
		
		if(c1 != c2 || r1 !=r2)
			throw new IllegalArgumentException("AlgorithmUtils Err: Impossible to preform the required multiplication.");
		
		double[][] result = new double[r1][c1];
		
		for(int i = 0; i < r1 ;i++)
			for(int j = 0; j < c1 ;j++)
				result[i][j] = matrix1[i][j].doubleValue() - matrix2[i][j].doubleValue();
				
		return result;	
	}
	
	/**
	 * Sum
	 */
	public static <T extends Number> double sumAll(T[] vector)
			throws IllegalArgumentException {
		if(vector == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		double result = 0;
		for(int i = 0; i < vector.length; i++)
			result += vector[i].doubleValue();
		
		return result;
	}
	
	public static <T extends Number> double sumAll(T[][] matrix)
			throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		double result = 0;
		for(int i = 0; i < matrix.length; i++)
			for(int j = 0; j < matrix[0].length ;j++)
				result += matrix[i][j].doubleValue();
		
		return result;
	}
	
	/**
	 * Transpose
	 */
	public static <T extends Number> double[][] transpose(T[][] matrix)
			throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		double[][] result = new double[matrix[0].length][matrix.length];
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix[0].length; j++)
            	result[j][i] = matrix[i][j].doubleValue();
        
        return result;
	}
	
	/**
	 * Number conversions
	 */
	
	public static int[] toInt(double[] vector) throws IllegalArgumentException {
		if(vector == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int[] result = new int[vector.length];
		
		for(int i = 0; i < vector.length; i++)
			result[i] = (int) vector[i];
				
		return result;	
	}
	
	public static int[][] toInt(double[][] matrix) throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int r = matrix.length;
		int c = matrix[0].length;
		
		int[][] result = new int[r][c];
		
		for(int i = 0; i < r ;i++)
			for(int j = 0; j < c ;j++)
				result[i][j] = (int) matrix[i][j];
				
		return result;	
	}
	
	public static Number[] toNumber(int[] vector) throws IllegalArgumentException {
		if(vector == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		Number[] result = new Number[vector.length];
		
		for(int i = 0; i < vector.length; i++)
			result[i] = vector[i];
				
		return result;	
	}
	
	public static Number[][] toNumber(int[][] matrix) throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int r = matrix.length;
		int c = matrix[0].length;
		
		Number[][] result = new Number[r][c];
		
		for(int i = 0; i < r ;i++)
			for(int j = 0; j < c ;j++)
				result[i][j] = matrix[i][j];
				
		return result;	
	}
	
	public static Number[] toNumber(double[] vector) throws IllegalArgumentException {
		if(vector == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		Number[] result = new Number[vector.length];
		
		for(int i = 0; i < vector.length; i++)
			result[i] = vector[i];
				
		return result;	
	}
	
	public static Number[][] toNumber(double[][] matrix) throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("AlgorithmUtils Err: Invalid argument.");
		
		int r = matrix.length;
		int c = matrix[0].length;
		
		Number[][] result = new Number[r][c];
		
		for(int i = 0; i < r ;i++)
			for(int j = 0; j < c ;j++)
				result[i][j] = matrix[i][j];
				
		return result;	
	}
	
}
