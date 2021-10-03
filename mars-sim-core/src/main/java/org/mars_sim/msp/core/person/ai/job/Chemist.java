/*
 * Mars Simulation Project
 * Chemist.java
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
import org.mars_sim.msp.core.structure.building.function.Research;

/**
 * The Chemist class represents a job for a chemist.
 */
public class Chemist extends Job {
	
	/**
	 * Constructor.
	 */
	public Chemist() {
		// Use Job constructor
		super(JobType.CHEMIST, Job.buildRoleMap(20.0, 10.0, 5.0, 5.0, 5.0, 20.0, 15.0, 30.0));
	}

	@Override
	public double getCapability(Person person) {
		double result = 0D;

		int chemistrySkill = person.getSkillManager().getSkillLevel(SkillType.CHEMISTRY);
		result = chemistrySkill;

		NaturalAttributeManager attributes = person.getNaturalAttributeManager();
		int academicAptitude = attributes.getAttribute(NaturalAttributeType.ACADEMIC_APTITUDE);
		result += result * ((academicAptitude - 50D) / 100D);

		if (person.getPhysicalCondition().hasSeriousMedicalProblems())
			result = 0D;

//		System.out.println(person + " chemist : " + Math.round(result*100.0)/100.0);

		return result;
	}

	@Override
	public double getSettlementNeed(Settlement settlement) {
		double result = .1;
		
		int population = settlement.getNumCitizens();
		
		// Add (labspace * tech level / 2) for all labs with chemistry specialties.
		List<Building> laboratoryBuildings = settlement.getBuildingManager().getBuildings(FunctionType.RESEARCH);
		Iterator<Building> i = laboratoryBuildings.iterator();
		while (i.hasNext()) {
			Building building = i.next();
			Research lab = building.getResearch();
			if (lab.hasSpecialty(ScienceType.CHEMISTRY)) {
				result += (lab.getLaboratorySize() * lab.getTechnologyLevel() / 12D);
			}
		}

		result = (result + population / 12D) / 2.0;
				
		return result;
	}
}
