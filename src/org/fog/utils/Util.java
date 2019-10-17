package org.fog.utils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

import org.apache.commons.lang3.ArrayUtils;
import org.fog.core.FogComputingSim;

/**
 * Class which defines some utility methods used along the program.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class Util {
	/**
	 * Verifies whether a given string is valid.
	 * 
	 * @param value the string
	 * @return true if its not null nor empty, otherwise false
	 */
	public static boolean validString(final String value) {
		if(value == null || value.length() < 1)
			return false;
		return true;
	}
	
	/**
	 * Transforms a string into a integer number.
	 * 
	 * @param value the string to be transformed
	 * @return the integer value
	 */
	public static int stringToInt(final String value) {
		int v;
		
		try {
			BigDecimal bd = new BigDecimal(value);
			v = bd.intValue();
		} catch (NumberFormatException e1) {
			v = -1;
		}
		
	    return v;
	}
	
	/**
	 * Transforms a string into a double number.
	 * 
	 * @param value the string to be transformed
	 * @return the double value
	 */
	public static double stringToDouble(final String value) {
		double v;
		
		try {
			v = Double.parseDouble(value);
		} catch (NumberFormatException e1) {
			v = -1;
		}
		
	    return v;
	}
	
	/**
	 * Transforms a string into a long number.
	 * 
	 * @param value the string to be transformed
	 * @return the long value
	 */
	public static long stringToLong(final String value) {
		long v;
		
		try {
			v = Long.parseLong(value);
		} catch (NumberFormatException e1) {
			v = -1;
		}
		
	    return v;
	}
	
	/**
	 * Transforms a string into a probability.
	 * 
	 * @param value the string to be transformed
	 * @return the probability value
	 */
	public static double stringToProbability(final String value) {
		double v;
		
		try {
			v = Double.parseDouble(value);
		} catch (NumberFormatException e1) {
			v = -1;
		}
		
		if(v > 1)
			v = -1;
		
	    return v;
	}
	
	/**
	 * Verifies whether a given array contains a given integer number from index 0 until a given index (exclusive).
	 * 
	 * @param array the array
	 * @param index the index
	 * @param key the value
	 * @return true if the array contains the value; 0, otherwise
	 */
	public static boolean contains(final int[] array, final int index, final int key) {
		ArrayList<Integer> p = new ArrayList<Integer>();
		
		for(int i = 0; i < index; i++) {
			p.add(array[i]);
			
		}
		
		return p.contains(key);
	}
	
	/**
	 * Verifies whether a given array contains a given integer number.
	 * 
	 * @param array the array
	 * @param key the value
	 * @return true if the array contains the value; 0, otherwise
	 */
	public static boolean contains(final int[] array, final int key) {     
	    return ArrayUtils.contains(array, key);
	}
	
	/**
	 * Gets a random integer number in the range of the parameters.
	 * 
	 * @param min the minimum random number
	 * @param max the maximum random number
	 * @return the random number
	 */
	public static int rand(final int min, final int max) {
        Random r = new Random();
        return min + r.nextInt(max - min + 1);
    }
	
	/**
	 * Gets a random double number in the range of the parameters.
	 * 
	 * @param min the minimum random number
	 * @param max the maximum random number
	 * @return the random number
	 */
	public static double rand(final double min, final double max) {
		Random r = new Random();
		return min + (max - min) * r.nextDouble();
	}
	
	/**
	 * Gets a random positive number in the range of the normal distribution.
	 * 
	 * @param mean the normal mean
	 * @param dev the normal deviation
	 * @return the random number
	 */
	public static double normalRand(final double mean, final double dev) {
		Random r = new Random();
		double randomNumber = -1;
		int counter = 1;
		
		while(randomNumber < 0) {
			randomNumber = r.nextGaussian()*dev + mean;
			
			if(++counter > 100)
				FogComputingSim.err("It looks like the normal random number generator method is running in an infinite loop");
		}
		
		return randomNumber;
	}
	
	/**
	 * Creates a copy of a matrix.
	 * 
	 * @param input the matrix to be copied
	 * @return the copy of the matrix
	 */
	public static int[][] copy(final int[][] input) {
		int r = input.length;
		int c = input[0].length;
		
		int[][] output = new int[r][c];
		
		for(int i = 0; i < r ;i++)
			for(int j = 0; j < c ; j++)
				output[i][j] = input[i][j];
				
		return output;
	}
	
	/**
	 * Creates a copy of a vector.
	 * 
	 * @param input the vector to be copied
	 * @return the copy of the vector
	 */
	public static int[] copy(final int[] input) {
		int[] output = new int[input.length];
		
		for(int i = 0; i < input.length ;i++)
			output[i] = input[i];
				
		return output;
	}
	
	/**
	 * Creates a string center aligned with a given width.
	 * 
	 * @param width the width of the string
	 * @param s the content of the string
	 * @return the string center aligned with a given width
	 */
	public static String centerString(final int width, final String s) {
	    return String.format("%-" + width  + "s", String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
	}
	
	/**
	 * Creates a string left aligned with a given width.
	 * 
	 * @param width the width of the string
	 * @param s the content of the string
	 * @return the string center aligned with a given width
	 */
	public static String leftString(final int width, final String s) {
		return String.format("%-" + width + "s", s);
	}
	
	/**
	 * Prompts the user to press a enter a given input.
	 * 
	 * @param string the message to be displayed
	 */
	@SuppressWarnings("resource")
	public static void promptEnterKey(final String string){
	   System.out.println(string);
	   Scanner scanner = new Scanner(System.in);
	   scanner.nextLine();
	}	
	
}
