/*
 * Mars Simulation Project
 * TabPanelGeneralRobot.java
 * @date 2024-07-18
 * @author Barry Evans
 */
package com.mars_sim.ui.swing.unit_window.robot;

import java.awt.BorderLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;

import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.robot.SystemCondition;
import com.mars_sim.tools.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.StyleManager;
import com.mars_sim.ui.swing.unit_window.TabPanel;
import com.mars_sim.ui.swing.utils.AttributePanel;

/**
 * This tab shows the general details of the Robot type.
 */
@SuppressWarnings("serial")
public class TabPanelGeneralRobot extends TabPanel {

	private static final String ID_ICON = "info";
	
	private Robot r;

	private JLabel statePercent;
	private JLabel cap;
	private JLabel ampHours;
	private JLabel maxCapNameplate;
	private JLabel kWhStored;
	private JLabel tVolt;
	private JLabel health;
	private JLabel degradPercent;
	private JLabel maxCRating;
	private JLabel cycles;
	
	/**
	 * Constructor.
	 */
	public TabPanelGeneralRobot(Robot r, MainDesktopPane desktop) {
		super(
			Msg.getString("BuildingPanelGeneral.title"),
			ImageLoader.getIconByName(ID_ICON), 
			Msg.getString("BuildingPanelGeneral.title"),
			desktop);
		this.r = r;
	}

	/**
	 * Build the UI elements
	 */
	@Override
	protected void buildUI(JPanel center) {
		SystemCondition sc = r.getSystemCondition();

		JPanel topPanel = new JPanel(new BorderLayout());
		center.add(topPanel, BorderLayout.NORTH);

		// Prepare spring layout info panel.
		AttributePanel infoPanel = new AttributePanel(3);
		topPanel.add(infoPanel, BorderLayout.NORTH);

		infoPanel.addRow("Type", r.getRobotType().getName());
		infoPanel.addRow("Model", r.getModel());
		infoPanel.addRow("Base Mass", StyleManager.DECIMAL_KG.format(r.getBaseMass()), "The base mass of this unit");
		
		JPanel dataPanel = new JPanel(new BorderLayout(10, 10));
		topPanel.add(dataPanel, BorderLayout.CENTER);
        addBorder(dataPanel, "Battery Condition");
		AttributePanel battPanel = new AttributePanel(10);
		dataPanel.add(battPanel, BorderLayout.NORTH);
        
		statePercent = battPanel.addRow("Battery Level", StyleManager.DECIMAL_PERC2.format(sc.getBatteryLevel()), 
				"The state of the battery is kWh stored / energy storage capacity * 100 percent");
		kWhStored = battPanel.addRow("kWh Stored", StyleManager.DECIMAL_KWH.format(sc.getkWattHourStored()));
		cap = battPanel.addRow("Energy Storage Capacity", StyleManager.DECIMAL_KWH.format(sc.getEnergyStorageCapacity()));
		maxCapNameplate = battPanel.addRow("Nameplate Capacity", StyleManager.DECIMAL_KWH.format(sc.getMaxCapNameplate()));	
		maxCRating = battPanel.addRow("Max C-Rating", StyleManager.DECIMAL_PLACES1.format(sc.getMaxCRating()));		
		
		ampHours = battPanel.addRow("Amp Hour", StyleManager.DECIMAL_AH.format(sc.getAmpHourRating()));
		tVolt = battPanel.addRow("Terminal Voltage", StyleManager.DECIMAL_V.format(sc.getTerminalVoltage()));
		health = battPanel.addRow("Health", StyleManager.DECIMAL_PERC2.format(sc.getHealth() * 100));
		degradPercent = battPanel.addRow("Degradation", StyleManager.DECIMAL_PERC2.format(sc.getPercentDegrade())
				+ " per sol");
		cycles = battPanel.addRow("Charge Cycles", sc.getNumCycles() + "");
	}

	@Override
	public void update() {
		SystemCondition sc = r.getSystemCondition();
		
		statePercent.setText(StyleManager.DECIMAL_PERC.format(sc.getBatteryLevel()));
		cap.setText(StyleManager.DECIMAL_KWH.format(sc.getEnergyStorageCapacity()));
		maxCapNameplate.setText(StyleManager.DECIMAL_KWH.format(sc.getMaxCapNameplate()));
		kWhStored.setText(StyleManager.DECIMAL_KWH.format(sc.getkWattHourStored()));
		ampHours.setText(StyleManager.DECIMAL_AH.format(sc.getAmpHourRating()));
		
		tVolt.setText(StyleManager.DECIMAL_V.format(sc.getTerminalVoltage()));
		health.setText(StyleManager.DECIMAL_PERC2.format(sc.getHealth() * 100));
		degradPercent.setText(StyleManager.DECIMAL_PERC2.format(sc.getPercentDegrade()) + " per sol");
		maxCRating.setText(StyleManager.DECIMAL_PLACES1.format(sc.getMaxCRating()));
		cycles.setText(sc.getNumCycles() + "");
	}
}