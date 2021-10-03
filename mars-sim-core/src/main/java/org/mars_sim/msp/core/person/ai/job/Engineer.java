/*
 * Mars Simulation Project
 * Engineer.java
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
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.function.FunctionType;
import org.mars_sim.msp.core.structure.building.function.Manufacture;
import org.mars_sim.msp.core.structure.building.function.Research;

/**
 * The Engineer class represents an engineer job focusing on repair and
 * maintenance of buildings and vehicles.
 */
class Engineer extends Job {
	
	/** Constructor. */
	public Engineer() {
		// Use Job constructor
		super(JobType.ENGINEER, Job.buildRoleMap(5.0, 20.0, 30.0, 10.0, 10.0, 15.0, 10.0, 20.0));


	}

	/**
	 * Gets a person's capability to perform this job.
	 * 
	 * @param person the person to check.
	 * @return capability (min 0.0).
	 */
	public double getCapability(Person person) {

		double result = 0D;

		int materialsScienceSkill = person.getSkillManager().getSkillLevel(SkillType.MATERIALS_SCIENCE);
		int mechanicSkill = person.getSkillManager().getSkillLevel(SkillType.MECHANICS);
		result = mechanicSkill *.25 + materialsScienceSkill * .75;
		
		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);

		int experienceAptitude = attributes.getAttribute(NaturalAttributeType.EXPERIENCE_APTITUDE);
		result += result * ((academicAptitude + experienceAptitude - 100) / 200D);
	
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
		
		// Get population
		int population = settlement.getNumCitizens();
		
		// Add (tech level * process number / 2) for all manufacture buildings.
		List<Building> manufactureBuildings = settlement.getBuildingManager().getBuildings(FunctionType.MANUFACTURE);

		Iterator<Building> i = manufactureBuildings.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Manufacture workshop = building.getManufacture();
			result += (workshop.getTechLevel() + 1) * workshop.getMaxProcesses() / 10D;
		}
		
		List<Building> laboratoryBuildings = settlement.getBuildingManager().getBuildings(FunctionType.RESEARCH);
		Iterator<Building> ii = laboratoryBuildings.iterator();
		while (ii.hasNext()) {
			Building building = ii.next();
			Research lab = building.getResearch();
			if (lab.hasSpecialty(ScienceType.ENGINEERING)) {
				result += (lab.getLaboratorySize() * lab.getTechnologyLevel() / 12D);
			}
		}
		
		result = (result + population / 8D) / 2.0;
			
		return result;
	}
}
