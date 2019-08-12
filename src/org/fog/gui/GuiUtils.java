package org.fog.gui;

import java.awt.Component;

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
 * Class which holds some utility methods used within the GUI.
 * 
 * @author José Carlos Ribeiro Vieira @ Instituto Superior Técnico (IST), Lisbon-Portugal
 * @since  July, 2019
 */
public class GuiUtils {	
	/**
	 * Creates a dialog message in the GUI with a given message.
	 * 
	 * @param component the graphical representation that can interact with the user
	 * @param msg the message to be displayed
	 * @param title the title to be displayed
	 */
	public static void prompt(final Component component, final String msg, final String title){
		JOptionPane.showMessageDialog(component, msg, title, JOptionPane.ERROR_MESSAGE);
	}
	
	/**
	 * Creates a dialog message in the GUI with the options Yes, No and Cancel.
	 * 
	 * @param component the graphical representation that can interact with the user
	 * @param msg the message to be displayed
	 * @return the integer indicating the option selected by the user
	 */
	public static int confirm(final Component component, final String msg){
		return JOptionPane.showConfirmDialog(component, msg);
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
	public static JTextField createInput(final JPanel jPanel, JTextField jTextField, final String label, final String value) {
		JLabel jLabel = new JLabel(label);
		jPanel.add(jLabel);
		jTextField = new JTextField();
		jTextField.setText(value);
		jLabel.setLabelFor(jTextField);
		jPanel.add(jTextField);
		
		return jTextField;
	}
	
	/**
	 * Creates an input field in the GUI with tool tip text.
	 * 
	 * @param jPanel the generic lightweight container
	 * @param jTextField the input field
	 * @param label the text to be displayed before the text field
	 * @param value the value of the text field
	 * @param tip the tip text to be displayed
	 * @return the input field
	 */
	public static JTextField createInput(final JPanel jPanel, JTextField jTextField, final String label, final String value, String tip) {
		JLabel jLabel = new JLabel(label);
		jLabel.setToolTipText(tip);
		jPanel.add(jLabel);
		jTextField = new JTextField();
		jTextField.setText(value);
		jLabel.setLabelFor(jTextField);
		jPanel.add(jTextField);
		jTextField.setToolTipText(tip);
		
		return jTextField;
	}
	
	/**
	 * Creates a drop-down list in the GUI.
	 * 
	 * @param jPanel the generic lightweight container
	 * @param jComboBox the component that combines a button or editable field and a drop-down list
	 * @param label the text to be displayed before the drop-down list
	 * @param model the list containing the name of the items
	 * @param option the name of the item to be selected
	 * @return the drop-down list
	 */
	public static JComboBox<String> createDropDown(final JPanel jPanel, JComboBox<String> jComboBox, final String label,
			final ComboBoxModel<String> model, final String option) {
		JLabel jLabel = new JLabel(label);
		jPanel.add(jLabel);
		jLabel.setLabelFor(jComboBox);
		model.setSelectedItem(option);
		jPanel.add(jComboBox);
		return jComboBox;
	}
	
	/**
	 * Creates a drop-down list in the GUI with tool tip text.
	 * 
	 * @param jPanel the generic lightweight container
	 * @param jComboBox the component that combines a button or editable field and a drop-down list
	 * @param label the text to be displayed before the drop-down list
	 * @param model the list containing the name of the items
	 * @param option the name of the item to be selected
	 * @param tip the tip text to be displayed
	 * @return the drop-down list
	 */
	public static JComboBox<String> createDropDown(final JPanel jPanel, JComboBox<String> jComboBox, final String label,
			final ComboBoxModel<String> model, final String option, String tip) {
		jComboBox = new JComboBox<>(model);
		JLabel jLabel = new JLabel(label);
		jLabel.setToolTipText(tip);
		jPanel.add(jLabel);
		jLabel.setLabelFor(jComboBox);
		model.setSelectedItem(option);
		jPanel.add(jComboBox);
		jComboBox.setToolTipText(tip);
		
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
