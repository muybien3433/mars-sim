/**
 * Mars Simulation Project
 * MissionTab.java
 * @date 2021-12-07
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.tool.monitor;

import java.util.List;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.tool.mission.MissionWindow;

/**
 * This class represents a mission table displayed within the Monitor Window.
 */
@SuppressWarnings("serial")
public class MissionTab extends TableTab {

	/**
	 * Constructor.
	 * @throws Exception
	 */
	public MissionTab(final MonitorWindow window) {
		// Use TableTab constructor
		super(window, new MissionTableModel(), true, true, MonitorWindow.MISSION_ICON);
		
		super.adjustColumnWidth(table);

		setEntityDriven(true);
		setNavigatable(true);
	}

	/**
	 * Display details for selected Missions
	 */
	@Override
	public void displayDetails(MainDesktopPane desktop) {
		List<?> rows = getSelection();
		for(Object o : rows) {
			if (o instanceof Mission u) {
				((MissionWindow)desktop.openToolWindow(MissionWindow.NAME)).openMission(u);
			}
		}
	}

	/**
	 * Get the Coordinates of the selected Mission
	 * @return Cooridnates, maybe null
	 */
    public Coordinates getSelectedCoordinates() {
		List<?> rows = getSelection();
		if (!rows.isEmpty() && (rows.get(0) instanceof Mission m)) {
			return m.getCurrentMissionLocation();
		}
		return null;
    }
}
