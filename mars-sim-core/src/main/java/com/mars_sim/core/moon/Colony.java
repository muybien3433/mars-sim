/*
 * Mars Simulation Project
 * Colony.java
 * @date 2023-09-25
 * @author Manny Kung
 */

package com.mars_sim.core.moon;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import com.mars_sim.core.Simulation;
import com.mars_sim.core.Unit;
import com.mars_sim.core.authority.Authority;
import com.mars_sim.core.authority.Nation;
import com.mars_sim.core.authority.Organization;
import com.mars_sim.core.logging.Loggable;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.time.ClockPulse;
import com.mars_sim.core.time.Temporal;
import com.mars_sim.mapdata.location.Coordinates;

public class Colony implements Serializable, Temporal, Loggable, Comparable<Colony> {

	private static final long serialVersionUID = 1L;

	public static final SimLogger logger = SimLogger.getLogger(Colony.class.getName());

	private int id;
	
	private String name;
	
	/** The settlement's ReportingAuthority instance. */
	private Authority sponsor;

	private Coordinates location;
	
	private Population population;
	
	private Simulation sim;
	
	private Nation nation;
	
	Set<Zone> zones = new HashSet<>();
	
	public Colony(int id, String name, Authority sponsor, Coordinates location) {
		this.id = id;
		this.name = name;
		this.sponsor = sponsor;
		this.location = location;
		
		nation = sponsor.getOneNation();
		
		population = new Population(this);
		
		for (ZoneType type: ZoneType.values()) {
			addZone(new Zone(type));
		}
		
	}

	public int getId() {
		return id;
	}
	
	public Nation getNation() {
		return nation;
	}
	
	public void addZone(Zone zone) {
		zones.add(zone);
	}
	
	public Set<Zone> getZones() {
		return zones;
	}
	
	public Organization getOrganization() {
		return sponsor.getOrganization();
	}
	
	@Override
	public boolean timePassing(ClockPulse pulse) {
		
		getOrganization().timePassing(pulse);
		
		population.timePassing(pulse);
		
		for (Zone z: zones) {
			z.timePassing(pulse);
		}
		
		return true;
	}
	
	public Population getPopulation() {
		return population;
	}
	
	public double getTotalArea() {
		double sum = 0;
		for (Zone z: zones) {
			sum += z.getArea();
		}
		return sum;
	}

	/**
	 * Gets the authority.
	 */
	public Authority getReportingAuthority() {
		return sponsor;
	}

	/**
	 * Compares this object with the specified object for order.
	 *
	 * @param o the Object to be compared.
	 * @return a negative integer, zero, or a positive integer as this object is
	 *         less than, equal to, or greater than the specified object.
	 */
	@Override
	public int compareTo(Colony	o) {
		return name.compareToIgnoreCase(o.name);
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public Coordinates getCoordinates() {
		return location;
	}

	@Override
	public Unit getContainerUnit() {
		if (sim == null) {
			sim = Simulation.instance();
		}
		return sim.getUnitManager().getMoon();
	}
}


