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

/**
 * Class which defines some utility methods used along the program.
 * 
 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since   July, 2019
 */
public class Util {	
	public static boolean validString(String value) {
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
	public static int stringToInt(String value) {
		int v;
		
		try {
			v = Integer.parseInt(value);
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
	public static double stringToDouble(String value) {
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
	public static long stringToLong(String value) {
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
	
	/**
	 * Gets a random number in the range of the parameters.
	 * 
	 * @param min the minimum random number
	 * @param max the maximum random number
	 * @return the random number
	 */
	public static int rand(int min, int max) {
        Random r = new Random();
        return min + r.nextInt(max - min + 1);
    }
	
	/**
	 * Gets a random number in the range of the normal distribution.
	 * 
	 * @param mean the normal mean
	 * @param dev the normal deviation
	 * @return the random number
	 */
	public static double normalRand(double mean, double dev) {
		Random r = new Random();
		double randomNumber = -1;
		while(randomNumber < 0) randomNumber = r.nextGaussian()*dev + mean;
		return randomNumber;
	}
	
	/**
	 * Creates a copy of a matrix.
	 * 
	 * @param input the matrix to be copied
	 * @return the copy of the matrix
	 */
	public static int[][] copy(int[][] input) {
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
	public static int[] copy(int[] input) {
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
	public String centerString(int width, String s) {
	    return String.format("%-" + width  + "s", String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
	}
	
	/**
	 * Creates a dialog message in the GUI with a given message.
	 * 
	 * @param component the graphical representation that can interact with the user
	 * @param msg the message to be displayed
	 * @param title the title to be displayed
	 */
	public static void prompt(Component component, String msg, String title){
		JOptionPane.showMessageDialog(component, msg, title, JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Creates a dialog message in the GUI with the options Yes, No and Cancel.
	 * 
	 * @param component the graphical representation that can interact with the user
	 * @param msg the message to be displayed
	 * @return the integer indicating the option selected by the user
	 */
	public static int confirm(Component component, String msg){
		return JOptionPane.showConfirmDialog(component, msg);
	}
	
	/**
	 * Prompts the user to press a enter a given input.
	 * 
	 * @param string the message to be displayed
	 */
	@SuppressWarnings("resource")
	public static void promptEnterKey(String string){
	   System.out.println(string);
	   Scanner scanner = new Scanner(System.in);
	   scanner.nextLine();
	}
	
	/**
	 * Creates an input field in the GUI.
	 * 
	 * @param jPanel the generic lightweight container
	 * @param jTextField the input field
	 * @param label the label assigned to the input field
	 * @param value the text to be displayed
	 * @return the input field
	 */
	public static JTextField createInput(JPanel jPanel, JTextField jTextField, String label, String value) {
		JLabel jLabel = new JLabel(label);
		jPanel.add(jLabel);
		jTextField = new JTextField();
		jTextField.setText(value);
		jLabel.setLabelFor(jTextField);
		jPanel.add(jTextField);
		
		return jTextField;
	}
	
	/**
	 * Creates a drop-down list in the GUI.
	 * 
	 * @param jPanel the generic lightweight container
	 * @param jComboBox the component that combines a button or editable field and a drop-down list
	 * @param label the label assigned to the drop-down list
	 * @param model the list containing the name of the items
	 * @param option the name of the item to be selected
	 * @return the drop-down list
	 */
	public static JComboBox<String> createDropDown(JPanel jPanel, JComboBox<String> jComboBox, String label,
			ComboBoxModel<String> model, String option) {
		JLabel jLabel = new JLabel(label);
		jPanel.add(jLabel);
		jLabel.setLabelFor(jComboBox);
		model.setSelectedItem(option);
		jPanel.add(jComboBox);
		return jComboBox;
	}
	
	/**
	 * Class which which is responsible to making a JButton clickable inside a JTable.
	 * 
	 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
	 * @see     tecnico.ulisboa.pt
	 * @since   July, 2019
	 */
	public static class ButtonRenderer extends JButton implements TableCellRenderer {
		private static final long serialVersionUID = 1L;
		
		/**
		 * Creates a new button renderer
		 */
		public ButtonRenderer() {
			setOpaque(true);
		}
		
		/**
		 * Gets the cell renderer component.
		 * 
		 * @param table the table that is asking the renderer to draw
		 * @param value the text to be displayed on the button
		 * @param isSelected the variable which represents if the button was selected
		 * @param hasFocus 
		 * @param row the row of the button
		 * @param column the column of the button
		 * @return the graphical representation that can interact with the user
		 */
		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
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
	
	/**
	 * Class which which is responsible to create a drop-down for node objects in the GUI.
	 * 
	 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
	 * @see     tecnico.ulisboa.pt
	 * @since   July, 2019
	 */
	@SuppressWarnings("rawtypes")
	public static class NodeCellRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 6021697923766790099L;

		/**
		 * Returns a component that has been configured to display the specified value. That component's paint method is then called
		 * to "render" the cell. If it is necessary to compute the dimensions of a list because the list cells do not have a fixed
		 * size, this method is called to generate a component on which getPreferredSize can be invoked.
		 * 
		 * @param list The JList we're painting
		 * @param value The value returned by list.getModel().getElementAt(index)
		 * @param index The cells index
		 * @param isSelected True if the specified cell was selected
		 * @param cellHasFocus True if the specified cell has the focus
		 * @return the component whose paint() method will render the specified value
		 */
		@Override
		public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
			Node node = (Node) value;
			JLabel label = new JLabel();

			if (node != null && node.getName() != null)
				label.setText(node.getName());

			return label;
		}
	}
	
	/**
	 * Class which which is responsible to create a drop-down for application modules objects in the GUI.
	 * 
	 * @author  José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST)
	 * @see     tecnico.ulisboa.pt
	 * @since   July, 2019
	 */
	@SuppressWarnings("rawtypes")
	public static class AppModulesCellRenderer extends JLabel implements ListCellRenderer {
		private static final long serialVersionUID = 6021697923766790099L;

		/**
		 * Returns a component that has been configured to display the specified value. That component's paint method is then called
		 * to "render" the cell. If it is necessary to compute the dimensions of a list because the list cells do not have a fixed
		 * size, this method is called to generate a component on which getPreferredSize can be invoked.
		 * 
		 * @param list The JList we're painting
		 * @param value The value returned by list.getModel().getElementAt(index)
		 * @param index The cells index
		 * @param isSelected True if the specified cell was selected
		 * @param cellHasFocus True if the specified cell has the focus
		 * @return the component whose paint() method will render the specified value
		 */
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
