/*
 * Mars Simulation Project
 * Unit.java
 * @date 2023-05-09
 * @author Scott Davis
 */
package com.mars_sim.core;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

import com.mars_sim.core.environment.Weather;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.ai.mission.MissionManager;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.MasterClock;
import com.mars_sim.core.vehicle.Vehicle;

/**
 * The Unit class is the abstract parent class to all units in the simulation.
 * Units include people, vehicles and settlements. This class provides data
 * members and methods common to all units.
 */
public abstract class Unit implements UnitIdentifer, Comparable<Unit> {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Unit.class.getName());

	public static final int MOON_UNIT_ID = -2;
	public static final int OUTER_SPACE_UNIT_ID = -1;
	public static final int MARS_SURFACE_UNIT_ID = 0;
	public static final Integer UNKNOWN_UNIT_ID = -3;

	// Data members
	/** The unit containing this unit. */
	protected Integer containerID = UNKNOWN_UNIT_ID;

	// Unique Unit identifier
	private int identifier;
	/** The last pulse applied. */
	private long lastPulse = 0;
	
	private String name;
	private String description = "No Description";
	/** Commander's notes on this unit. */
	private String notes = "";

	/** Unit listeners. */
	private transient Set<UnitListener> listeners;

	protected static SimulationConfig simulationConfig = SimulationConfig.instance();

	protected static MasterClock masterClock;

	protected static UnitManager unitManager;
	protected static MissionManager missionManager;

	protected static Weather weather;

	// File for diagnostics output
	private static PrintWriter diagnosticFile = null;

	/**
	 * Enable the detailed diagnostics
	 *
	 * @throws FileNotFoundException
	 */
	public static void setDiagnostics(boolean diagnostics) throws FileNotFoundException {
		if (diagnostics) {
			if (diagnosticFile == null) {
				String filename = SimulationRuntime.getLogDir() + "/unit-create.txt";
				diagnosticFile = new PrintWriter(filename);
				logger.config("Diagnostics enabled to " + filename);
			}
		} else if (diagnosticFile != null) {
			diagnosticFile.close();
			diagnosticFile = null;
		}
	}

	/**
	 * Log the creation of a new Unit
	 *
	 * @param entry
	 */
	private static void logCreation(Unit entry) {
		StringBuilder output = new StringBuilder();
		output.append(masterClock.getMarsTime().getDateTimeStamp()).append(" Id:").append(entry.getIdentifier())
				.append(" Type:").append(entry.getUnitType()).append(" Name:").append(entry.getName());

		synchronized (diagnosticFile) {
			diagnosticFile.println(output.toString());
			diagnosticFile.flush();
		}
	}

	/**
	 * Gets the identifier of this unit.
	 */
	public final int getIdentifier() {
		return identifier;
	}

	/**
	 * Constructor 1: the name and identifier are defined.
	 *
	 * @param name     {@link String} the name of the unit
	 * @param id Unit identifier
	 * @param containerId Identifier of the container
	 */
	protected Unit(String name, int id, int containerId) {
		// Initialize data members from parameters
		this.name = name;
		this.identifier = id;
		this.containerID = containerId;
	}

	/**
	 * Constructor 2: where the name and location are defined.
	 *
	 * @param name     {@link String} the name of the unit
	 */
	protected Unit(String name) {
		// Initialize data members from parameters
		this.name = name;

		if (masterClock != null) {
			// Needed for maven test
			this.lastPulse = masterClock.getNextPulse() - 1;
	
			// Calculate the new Identifier for this type
			identifier = unitManager.generateNewId(getUnitType());
		}

		// Define the default LocationStateType of an unit at the start of the sim
		// Instantiate Inventory as needed. Still needs to be pushed to subclass
		// constructors
		switch (getUnitType()) {
		case BUILDING, CONTAINER, EVA_SUIT, PERSON, ROBOT:
			// Why no containerID ?
			break;
			
		case VEHICLE:
			containerID = MARS_SURFACE_UNIT_ID;
			break;

		case CONSTRUCTION, MARS, SETTLEMENT:
			containerID = MARS_SURFACE_UNIT_ID;
			break;

		case MOON:
			containerID = MOON_UNIT_ID;
			break;
			
		default:
			throw new IllegalStateException("Do not know Unittype " + getUnitType());
		}

		if (diagnosticFile != null) {
			logCreation(this);
		}
	}

	/**
	 * What logical UnitType of this object in terms of the management. This is NOT
	 * a direct mapping to the concrete subclass of Unit since some logical
	 * UnitTypes can have multiple implementation, e.g. Equipment.
	 *
	 * @return
	 */
	public abstract UnitType getUnitType();

	/**
	 * Is this time pulse valid for the Unit. Has it been already applied? The logic
	 * on this method can be commented out later on
	 *
	 * @param pulse Pulse to apply
	 * @return Valid to accept
	 */
	protected boolean isValid(ClockPulse pulse) {
		long newPulse = pulse.getId();
		boolean result = (newPulse > lastPulse);
		if (result) {
			long expectedPulse = lastPulse + 1;
			if (expectedPulse != newPulse) {
				// Pulse out of sequence; maybe missed one
				logger.warning(getName() + " expected pulse #" + expectedPulse + " but received " + newPulse);
			}
			lastPulse = newPulse;
		} else {
			if (newPulse == lastPulse) {
				// This is a newly added unit such as person/vehicle/robot in a resupply transport.
				return true;
			}
			else
				logger.severe(getName() + " rejected pulse #" + newPulse + ", last pulse was " + lastPulse);
		}
		return result;
	}

	/**
	 * Gets the unit's name.
	 *
	 * @return the unit's name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Sets the unit's name.
	 *
	 * @param name new name
	 */
	public void setName(String name) {
		this.name = name;
		fireUnitUpdate(UnitEventType.NAME_EVENT, name);
	}

	/**
	 * Gets the unit's description.
	 *
	 * @return description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * Sets the unit's description.
	 *
	 * @param description new description.
	 */
	protected void setDescription(String description) {
		this.description = description;
		fireUnitUpdate(UnitEventType.DESCRIPTION_EVENT, description);
	}

	/**
	 * Gets the commander's notes on this unit.
	 *
	 * @return notes
	 */
	public String getNotes() {
		return notes;
	}

	/**
	 * Sets the commander's notes on this unit.
	 *
	 * @param notes.
	 */
	public void setNotes(String notes) {
		this.notes = notes;
		fireUnitUpdate(UnitEventType.NOTES_EVENT, notes);
	}

	/**
	 * Gets the unit's container unit. Returns null if unit has no container unit.
	 *
	 * @return the unit's container unit
	 */
	public Unit getContainerUnit() {
		if (unitManager == null) // for maven test
			return null;
		return unitManager.getUnitByID(containerID);
	}

	public int getContainerID() {
		return containerID;
	}

	protected void setContainerID(Integer id) {
		containerID = id;
	}
	
	/**
	 * Checks if it has a unit listener.
	 * 
	 * @param listener
	 * @return
	 */
	public synchronized boolean hasUnitListener(UnitListener listener) {
		if (listeners == null)
			return false;
		return listeners.contains(listener);
	}

	/**
	 * Adds a unit listener.
	 *
	 * @param newListener the listener to add.
	 */
	public final synchronized void addUnitListener(UnitListener newListener) {
		if (newListener == null)
			throw new IllegalArgumentException();
		if (listeners == null)
			listeners = new HashSet<>();

		synchronized(listeners) {	
			listeners.add(newListener);
		}
	}

	/**
	 * Removes a unit listener.
	 *
	 * @param oldListener the listener to remove.
	 */
	public final synchronized void removeUnitListener(UnitListener oldListener) {
		if (oldListener == null)
			throw new IllegalArgumentException();

		if (listeners != null) {
			synchronized(listeners) {
				listeners.remove(oldListener);
			}
		}
	}

	/**
	 * Fires a unit update event.
	 *
	 * @param updateType the update type.
	 */
	public final void fireUnitUpdate(UnitEventType updateType) {
		fireUnitUpdate(updateType, null);
	}

	/**
	 * Fires a unit update event.
	 *
	 * @param updateType the update type.
	 * @param target     the event target object or null if none.
	 */
	public final void fireUnitUpdate(UnitEventType updateType, Object target) {
		if (listeners == null || listeners.isEmpty()) {
			return;
		}
		final UnitEvent ue = new UnitEvent(this, updateType, target);
		synchronized (listeners) {
			for(UnitListener i : listeners) {
				try {
					// Stop listeners breaking the update thread
					i.unitUpdate(ue);
				}
				catch(RuntimeException rte) {
					logger.severe(this, "Problem executing listener " + i + " for event " + ue, rte);
				}
			}
		}
	}

	/**
	 * Gets the building this unit is at.
	 *
	 * @return the building
	 */
	public Building getBuildingLocation() {
		return null;
	}

	/**
	 * Gets the associated settlement this unit is with.
	 *
	 * @return the associated settlement
	 */
	public Settlement getAssociatedSettlement() {
		return null;
	}

	/**
	 * Gets the vehicle this unit is in, null if not in vehicle.
	 *
	 * @return the vehicle
	 */
	public Vehicle getVehicle() {
		return null;
	}

	/**
	 * Is this unit inside a settlement ?
	 *
	 * @return true if the unit is inside a settlement
	 */
	public abstract boolean isInSettlement();

	/**
	 * Is this unit inside a vehicle in a garage ?
	 *
	 * @return true if the unit is in a vehicle inside a garage
	 */
	public boolean isInVehicleInGarage() {
		Unit cu = getContainerUnit();
		if (cu.getUnitType() == UnitType.VEHICLE) {
			// still inside the garage
			return ((Vehicle)cu).isInGarage();
		}
		return false;
	}

	/**
	 * Loads instances.
	 *
	 */
	public static void initializeInstances(MasterClock c0, UnitManager um,
			Weather w, MissionManager mm) {
		masterClock = c0;
		weather = w;
		unitManager = um;
		missionManager = mm;
	}

	/**
	 * Compares this object with the specified object for order.
	 *
	 * @param o the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	@Override
	public int compareTo(Unit o) {
		return name.compareToIgnoreCase(o.name);
	}

	/**
	 * String representation of this Unit.
	 *
	 * @return The units name.
	 */
	@Override
	public String toString() {
		return name;
	}

	/**
	 * Compares if an object is the same as this unit
	 *
	 * @param obj
	 */
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (this.getClass() != obj.getClass())
			return false;
		return this.getIdentifier() == ((Unit) obj).getIdentifier();
	}

	/**
	 * Gets the hash code for this object.
	 *
	 * @return hash code.
	 */
	public int hashCode() {
		return getIdentifier() % 32;
	}

	/**
	 * Prepares object for garbage collection.
	 */
	public void destroy() {
		name = null;
		description = null;
		listeners = null;
	}
}
