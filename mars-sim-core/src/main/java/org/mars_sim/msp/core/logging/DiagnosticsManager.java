/**
 * Mars Simulation Project
 * DiagnosticsManager.java
 * @date 2023-08-02
 * @author Barry Evans
 */
package org.mars_sim.msp.core.logging;

import java.io.FileNotFoundException;

import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.data.RatingLog;

public class DiagnosticsManager {

	private static final String UNIT_MODULE = "unit";
	
	// List of modules supporting diagnostics
	public static final String [] MODULE_NAMES = {
			UNIT_MODULE
	};
	
	/**
	 * Prevent creation of instance to enforce static helper
	 * */
	private DiagnosticsManager() {}

	/**
	 * Set the diagnostics for one of the modules
	 * @param module
	 * @param enabled
	 * @return
	 * @throws FileNotFoundException 
	 */
	public static boolean setDiagnostics(String module, boolean enabled) throws FileNotFoundException {
		if (module.equals(UNIT_MODULE)) {
			Unit.setDiagnostics(enabled);
		}
		else {
			RatingLog.setDiagnostics(module, enabled);
		}
		return true;
	}
}
