package org.fog.utils;

import java.awt.Component;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.UIManager;
import javax.swing.table.TableCellRenderer;

import org.fog.application.AppModule;
import org.fog.gui.core.Node;

public class Util {
	
	public Util() {
		
	}
	
	public String centerString(int width, String s) {
	    return String.format("%-" + width  + "s", String.format("%" + (s.length() + (width - s.length()) / 2) + "s", s));
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
	
	public static void prompt(Component parentComponent, String msg, String type){
		JOptionPane.showMessageDialog(parentComponent, msg, type, JOptionPane.ERROR_MESSAGE);
	}
	
	public static int confirm(Component parentComponent, String msg){
		return JOptionPane.showConfirmDialog(parentComponent, msg);
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
