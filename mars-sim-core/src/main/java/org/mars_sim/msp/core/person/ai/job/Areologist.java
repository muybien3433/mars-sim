/*
 * Mars Simulation Project
 * Areologist.java
 * @date 2021-09-27
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.job;

import java.util.Iterator;
import java.util.List;

import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.NaturalAttributeManager;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.RoverMission;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.Lab;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.Research;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.Vehicle;

/**
 * The Areologist class represents a job for an areologist, one who studies the
 * rocks and landforms of Mars.
 */
class Areologist extends Job {
	
	/**
	 * Constructor.
	 */
	public Areologist() {
		// Use Job constructor
		super(JobType.AREOLOGIST,  Job.buildRoleMap(5.0, 10.0, 5.0, 5.0, 20.0, 25.0, 10.0, 30.0));
	}

	/**
	 * Gets a person's capability to perform this job.
	 * 
	 * @param person the person to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Person person) {

		double result = 1D;

		int areologySkill = person.getSkillManager().getSkillLevel(SkillType.AREOLOGY);
		result = areologySkill;

		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);
		int experienceAptitude = attributes.getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);
		double averageAptitude = (academicAptitude + experienceAptitude) / 2D;
		result += result * ((averageAptitude - 100D) / 100D);

		if (person.getPhysicalCondition().hasSeriousMedicalProblems())
			result = 0D;

		return result;
	}

	/**
	 * Gets the base settlement need for this job.
	 * 
	 * @param settlement the settlement in need.
	 * @return the base need >= 0
	 */
	public double getSettlementNeed(Settlement settlement) {
		double result = .1;

		int population = settlement.getNumCitizens();
		
		// Add (labspace * tech level / 2) for all labs with areology specialties.
		List<Building> laboratoryBuildings = settlement.getBuildingManager().getBuildings(FunctionType.RESEARCH);
		Iterator<Building> i = laboratoryBuildings.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Research lab = building.getResearch();
			if (lab.hasSpecialty(ScienceType.AREOLOGY)) {
				result += (lab.getLaboratorySize() * lab.getTechnologyLevel() / 10D);
			}
		}

		// Add (labspace * tech level / 2) for all parked rover labs with areology
		// specialties.
		Iterator<Vehicle> j = settlement.getParkedVehicles().iterator();
		while (j.hasNext()) {
			Vehicle vehicle = j.next();
			if (vehicle instanceof Rover) {
				Rover rover = (Rover) vehicle;
				if (rover.hasLab()) {
					Lab lab = rover.getLab();
					if (lab.hasSpecialty(ScienceType.AREOLOGY)) {
						result += (lab.getLaboratorySize() * lab.getTechnologyLevel() / 12D);
					}
				}
			}
		}

		// Add (labspace * tech level / 2) for all labs with areology specialties in
		// rovers out on missions.
		// MissionManager missionManager = Simulation.instance().getMissionManager();
		Iterator<Mission> k = missionManager.getMissionsForSettlement(settlement).iterator();
		while (k.hasNext()) {
			Mission mission = k.next();
			if (mission instanceof RoverMission) {
				Rover rover = ((RoverMission) mission).getRover();
				if ((rover != null) && !settlement.getParkedVehicles().contains(rover)) {
					if (rover.hasLab()) {
						Lab lab = rover.getLab();
						if (lab.hasSpecialty(ScienceType.AREOLOGY)) {
							result += (lab.getLaboratorySize() * lab.getTechnologyLevel() / 8D);
						}
					}
				}
			}
		}

		result = (result + population / 10D) / 2.0;
				
		return result;
	}
}
