package org.fog.placement.algorithms.placement;

public class AlgorithmMathUtils {

	public static double[][] multiplyMatrices(double[][] firstMatrix, double[][] secondMatrix)
			throws IllegalArgumentException {
		
		if(firstMatrix == null || secondMatrix == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		int r1 = firstMatrix.length;
		int c1 = firstMatrix[0].length;
		int r2 = secondMatrix.length;
		int c2 = secondMatrix[0].length;
		
		if(c1 != r2)
			throw new IllegalArgumentException("Impossible to preform the required matrix multiplication");
		
		double[][] product = new double[r1][c2];
        
        for(int i = 0; i < r1; i++)
            for (int j = 0; j < c2; j++)
                for (int k = 0; k < c1; k++)
                    product[i][j] += firstMatrix[i][k] * secondMatrix[k][j];
        
        return product;
    }
	
	public static double[][] transposeMatrix(double [][] matrix) throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("Invalid argument");
		
		double[][] temp = new double[matrix[0].length][matrix.length];
        for (int i = 0; i < matrix.length; i++)
            for (int j = 0; j < matrix[0].length; j++)
                temp[j][i] = matrix[i][j];
        return temp;
    }
	
	public static double[][] dotProductMatrices(double[][] firstMatrix, double[][] secondMatrix)
			throws IllegalArgumentException {
		
		if(firstMatrix == null || secondMatrix == null)
			throw new IllegalArgumentException("Some of the received arguments are null");
		
		int r1 = firstMatrix.length;
		int c1 = firstMatrix[0].length;
		int r2 = secondMatrix.length;
		int c2 = secondMatrix[0].length;
		
		if(r1 != r2 || c1 != c2)
			throw new IllegalArgumentException("Impossible to preform the required dot product");
		
		double[][] product = new double[r1][c1];
        
        for(int i = 0; i < r1; i++)
            for (int j = 0; j < c1; j++)
                    product[i][j] = firstMatrix[i][j] * secondMatrix[i][j];
        
        return product;
	}
	
	public static double sumAllElementsMatrix(double[][] matrix) throws IllegalArgumentException {
		if(matrix == null)
			throw new IllegalArgumentException("Invalid argument");
		
		int r = matrix.length;
		int c = matrix[0].length;
        double ret = 0;
		
        for(int i = 0; i < r; i++)
            for (int j = 0; j < c; j++)
            	ret += matrix[i][j];
        
        return ret;
	}
	
	public static void printMatrix(double[][] matrix) {		
		int r = matrix.length;
		int c = matrix[0].length;
		
        for(int i = 0; i < r; i++) {
            for (int j = 0; j < c; j++)
            	System.out.print(matrix[i][j] + " ");
            System.out.println();
        }
        
        System.out.println("\n");
	}
	
}
