/*
 * Mars Simulation Project
 * UnitManagerEvent.java
 * @date 2024-08-10
 * @author Scott Davis
 */
package com.mars_sim.core;

import java.util.EventObject;

public class UnitManagerEvent extends EventObject {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	// Data members
	private UnitManagerEventType eventType;
	private Unit unit;

	/**
	 * Constructor.
	 * 
	 * @param unitManager the unit manager throwing this event.
	 * @param eventType   the type of event.
	 * @param unit        the target of the event.
	 */
	public UnitManagerEvent(UnitManager unitManager, UnitManagerEventType eventType, Unit unit) {
		// User EventObject constructor.
		super(unitManager);
		this.eventType = eventType;
		this.unit = unit;
	}

	/**
	 * Gets the event type.
	 * 
	 * @return type string.
	 */
	public UnitManagerEventType getEventType() {
		return eventType;
	}

	/**
	 * Gets the unit target of this event.
	 * 
	 * @return unit
	 */
	public Unit getUnit() {
		return unit;
	}
}
