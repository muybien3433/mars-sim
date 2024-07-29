/*
 * Mars Simulation Project
 * MissionTab.java
 * @date 2021-12-07
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.tool.monitor;

import java.util.List;

import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.mapdata.location.Coordinates;
import com.mars_sim.ui.swing.tool.mission.MissionWindow;

/**
 * This class represents a mission table displayed within the Monitor Window.
 */
@SuppressWarnings("serial")
public class MissionTab extends TableTab {

	/**
	 * Constructor.
	 * 
	 * @throws Exception
	 */
	public MissionTab(final MonitorWindow window) {
		// Use TableTab constructor
		super(window, new MissionTableModel(window.getDesktop().getSimulation()), true, true, MissionWindow.ICON);
		
		adjustColumnWidth(table);

		setEntityDriven(true);
		setNavigatable(true);
	}


	/**
	 * Get the Coordinates of the selected Mission
	 * @return Cooridnates, maybe null
	 */
	@Override
    public Coordinates getSelectedCoordinates() {
		List<?> rows = getSelection();
		if (!rows.isEmpty() && (rows.get(0) instanceof Mission m)) {
			return m.getCurrentMissionLocation();
		}
		return null;
    }
}
