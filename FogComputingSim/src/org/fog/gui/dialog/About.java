package org.fog.gui.dialog;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;

import org.fog.gui.core.SpringUtilities;

public class About extends JDialog {
	private static final long serialVersionUID = 1L;

	public About(final JFrame frame) {
    	setLayout(new BorderLayout());

		add(initUI(), BorderLayout.CENTER);
		add(createButtonPanel(), BorderLayout.PAGE_END);

		setTitle(" About FogComputingSim");
		setModal(true);
		setPreferredSize(new Dimension(500, 500));
		setResizable(false);
		pack();
		setLocationRelativeTo(frame);
		setVisible(true);
    }
	
	private JPanel initUI() {
        JPanel springPanel = new JPanel(new SpringLayout());
        springPanel.setBorder(BorderFactory.createEmptyBorder(70, 40, 70, 40));

        JLabel name = new JLabel("FogComputingSim", SwingConstants.CENTER);
        name.setFont(new Font("Serif", Font.BOLD, 25));
        name.setAlignmentX(0.5f);
        springPanel.add(name);
        
        springPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        name = new JLabel("Unregistered", SwingConstants.CENTER);
        name.setFont(new Font("Serif", Font.BOLD, 18));
        name.setAlignmentX(0.5f);
        springPanel.add(name);
        
        springPanel.add(Box.createRigidArea(new Dimension(0, 75)));
        name = new JLabel("Copyright © 2018-2024, FogComputingSim corp", SwingConstants.CENTER);
        name.setFont(new Font("Serif", Font.BOLD, 15));
        name.setAlignmentX(0.5f);
        springPanel.add(name);
        
        springPanel.add(Box.createRigidArea(new Dimension(0, 20)));
        name = new JLabel("Version 3.1.1, Build 3176", SwingConstants.CENTER);
        name.setFont(new Font("Serif", Font.BOLD, 15));
        name.setAlignmentX(0.5f);
        springPanel.add(name);

		//rows, cols, initX, initY, xPad, yPad
        SpringUtilities.makeCompactGrid(springPanel, 7, 1, 10, 10, 10, 10);
		return springPanel;
	}
    
    private JPanel createButtonPanel() {
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.LINE_AXIS));
		
		GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = GridBagConstraints.REMAINDER;
        gbc.anchor = GridBagConstraints.NORTH;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.HORIZONTAL;
		
		JButton okBtn = new JButton("Close");
		
		okBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
            	setVisible(false);
            }
        });
		
		JPanel buttons = new JPanel(new GridBagLayout());
        buttons.add(okBtn, gbc);
        
        gbc.weighty = 1;

		buttonPanel.add(buttons, gbc);
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));

		return buttonPanel;
	}
}
