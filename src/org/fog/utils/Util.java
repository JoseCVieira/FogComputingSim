package org.fog.utils;

import java.awt.Component;
import java.util.Random;
import java.util.Scanner;

import javax.swing.ComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.fog.application.AppModule;
import org.fog.gui.core.Node;

public class Util {	
	public Util() {
		
	}
	
	public static boolean validString(String value) {
		if(value == null || value.length() < 1)
			return false;
		return true;
	}
	
	public static int stringToInt(String value) {
		int v;
		
		try {
			v = Integer.parseInt(value);
		} catch (NumberFormatException e1) {
			v = -1;
		}
		
	    return v;
	}
	
	public static double stringToDouble(String value) {
		double v;
		
		try {
			v = Double.parseDouble(value);
		} catch (NumberFormatException e1) {
			v = -1;
		}
		
	    return v;
	}
	
	public static long stringToLong(String value) {
		long v;
		
		try {
			v = Long.parseLong(value);
		} catch (NumberFormatException e1) {
			v = -1;
		}
		
	    return v;
	}
	
	public static double stringToProbability(String value) {
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
	
	public static int rand(int min, int max) {
        Random r = new java.util.Random();
        return min + r.nextInt(max - min + 1);
    }
	
	public static double normalRand(double mean, double dev) {
		Random r = new Random();
		double randomNumber = -1;
		while(randomNumber < 0) randomNumber = r.nextGaussian()*dev + mean;
		return randomNumber;
	}
	
	public static int[][] copy(int[][] input) {
		int r = input.length;
		int c = input[0].length;
		
		int[][] output = new int[r][c];
		
		for(int i = 0; i < r ;i++)
			for(int j = 0; j < c ; j++)
				output[i][j] = input[i][j];
				
		return output;
	}
	
	public static int[] copy(int[] input) {
		int[] output = new int[input.length];
		
		for(int i = 0; i < input.length ;i++)
			output[i] = input[i];
				
		return output;
	}
	
	public String centerString(int width, String s) {
	    return String.format("%-" + width  + "s", String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
	}
	
	public static void prompt(Component parentComponent, String msg, String type){
		JOptionPane.showMessageDialog(parentComponent, msg, type, JOptionPane.ERROR_MESSAGE);
	}
	
	public static int confirm(Component parentComponent, String msg){
		return JOptionPane.showConfirmDialog(parentComponent, msg);
	}
	
	@SuppressWarnings("resource")
	public static void promptEnterKey(){
	   System.out.println("Press \"ENTER\" to continue...");
	   Scanner scanner = new Scanner(System.in);
	   scanner.nextLine();
	}
	
	public static JTextField createInput(JPanel jPanel, JTextField jTextField, String label, String value) {
		JLabel jLabel = new JLabel(label);
		jPanel.add(jLabel);
		jTextField = new JTextField();
		jTextField.setText(value);
		jLabel.setLabelFor(jTextField);
		jPanel.add(jTextField);
		
		return jTextField;
	}
	
	public static JComboBox<String> createDropDown(JPanel jPanel, JComboBox<String> jComboBox, String label, ComboBoxModel<String> periodicModel, String option) {
		JLabel jLabel = new JLabel(label);
		jPanel.add(jLabel);
		jLabel.setLabelFor(jComboBox);
		periodicModel.setSelectedItem(option);
		jPanel.add(jComboBox);
		return jComboBox;
	}
	
	public static class ButtonRenderer extends JButton implements TableCellRenderer {
		private static final long serialVersionUID = 1L;
	
		public ButtonRenderer() {
			setOpaque(true);
		}
	
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			
			if (isSelected) {
		    	setForeground(table.getSelectionForeground());
		    	setBackground(table.getSelectionBackground());
		    }else {
		    	setForeground(table.getForeground());
		    	setBackground(UIManager.getColor("Button.background"));
		    }
			
			setText((value == null) ? "" : value.toString());
			return this;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static class NodeCellRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 6021697923766790099L;

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Node node = (Node) value;
			JLabel label = new JLabel();

			if (node != null && node.getName() != null)
				label.setText(node.getName());

			return label;
		}
	}
	
	@SuppressWarnings("rawtypes")
	public static class AppModulesCellRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 6021697923766790099L;

		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			AppModule appModule = (AppModule) value;
			JLabel label = new JLabel();

			if (appModule != null && appModule.getName() != null)
				label.setText(appModule.getName());

			return label;
		}
	}
	
}
