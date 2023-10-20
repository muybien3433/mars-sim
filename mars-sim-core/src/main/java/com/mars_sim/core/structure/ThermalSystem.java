/*
 * Mars Simulation Project
 * ThermalSystem.java
 * @date 2023-08-26
 * @author Manny Kung
 */
package com.mars_sim.core.structure;

import java.io.Serializable;
import java.util.Iterator;

import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingException;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.ThermalGeneration;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.Temporal;

/**
 * This class is the settlement's Thermal Control, Distribution and Storage Subsystem.
 */
public class ThermalSystem
implements Serializable, Temporal {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	// May add back SimLogger logger = SimLogger.getLogger(ThermalSystem.class.getName());

	// Data members
	private double powerGeneratedCache;
	
	private double heatGeneratedCache;

	private double heatStored;

	private double heatRequired;
	
	private double heatValue;

	private Settlement settlement;

	private BuildingManager manager;
	
	/**
	 * Constructor.
	 */
	public ThermalSystem(Settlement settlement) {

		this.settlement = settlement;
		this.manager = settlement.getBuildingManager();
		
		heatGeneratedCache = 0D;
		heatStored = 0D;

		heatRequired = 0D;
	}

	
	/**
	 * Gets the total max possible generated heat in the heating system.
	 * 
	 * @return heat in kW
	 */
	public double getGeneratedHeat() {
		return heatGeneratedCache;
	}

	/**
	 * Gets the total max possible generated heat in the heating system.
	 * 
	 * @return heat in kW
	 */
	public double getGeneratedPower() {
		return powerGeneratedCache;
	}


	/**
	 * Sets the new amount of generated heat in the heating system.
	 * 
	 * @param newGeneratedHeat the new generated heat kW
	 */
	private void setGeneratedHeat(double newGeneratedHeat) {
		if (heatGeneratedCache != newGeneratedHeat) {
			heatGeneratedCache = newGeneratedHeat;
			settlement.fireUnitUpdate(UnitEventType.GENERATED_HEAT_EVENT);
		}
	}

	/**
	 * Sets the new amount of generated power in the heating system.
	 * 
	 * @param newGeneratedHeat the new generated power kW
	 */
	private void setGeneratedPower(double newGeneratedPower) {
		if (powerGeneratedCache != newGeneratedPower) {
			powerGeneratedCache = newGeneratedPower;
			settlement.fireUnitUpdate(UnitEventType.GENERATED_POWER_EVENT);
		}
	}

	/**
	 * Gets the heat required from the heating system.
	 * 
	 * @return heat in kW
	 */
	// NOT USED FOR THE TIME BEING. always return ZERO
	public double getRequiredHeat() {
		return heatRequired;
	}

	/**
	 * Time passing for heating system.
	 * 
	 * @param time amount of time passing (in millisols)
	 */
	@Override
	public boolean timePassing(ClockPulse pulse) {

		// update the total heat generated in the heating system.
		updateTotalHeatGenerated();

		updateTotalPowerGenerated();

		// Update heat value.
		determineHeatValue();

		return true;
	}

	/**
	 * Updates the total heat generated in the heating system.
	 * 
	 * @throws BuildingException if error determining total heat generated.
	 */
	private void updateTotalHeatGenerated() {
		double heat = 0D;

		// Add the heat generated by all heat generation buildings.
		Iterator<Building> iHeat = manager.getBuildingSet(FunctionType.THERMAL_GENERATION).iterator();
		while (iHeat.hasNext()) {
			ThermalGeneration gen = iHeat.next().getThermalGeneration();
			if (gen != null)
				heat += gen.getGeneratedHeat();
		}
		setGeneratedHeat(heat);

	}

	/**
	 * Updates the total power generated by the heat engine system.
	 * 
	 * @throws BuildingException if error determining total heat generated.
	 */
	private void updateTotalPowerGenerated() {
		double power = 0D;

		// Add the heat generated by all heat generation buildings.
		Iterator<Building> i = manager.getBuildingSet(FunctionType.POWER_GENERATION).iterator();
		while (i.hasNext()) {
			ThermalGeneration gen = i.next().getThermalGeneration();
			if (gen != null)
				power += gen.getGeneratedPower();
		}
		setGeneratedPower(power);
	}

	/**
	 * Determines the value of heat energy at the settlement.
	 */
	private void determineHeatValue() {
		double demand = heatRequired;
		double supply = heatGeneratedCache + (heatStored / 2D);

		double newHeatValue = demand / (supply + 1.0D);

		if (newHeatValue != heatValue) {
			heatValue = newHeatValue;
			settlement.fireUnitUpdate(UnitEventType.HEAT_VALUE_EVENT);
		}
	}

	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		manager = null;
		settlement = null;
	}
}