/*
 * Mars Simulation Project
 * OptimizeSystem.java
 * @date 2023-07-29
 * @author Manny Kung
 */
package com.mars_sim.core.person.ai.task.meta;

import java.util.logging.Level;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.NaturalAttributeType;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.person.ai.fav.FavoriteType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.person.ai.task.OptimizeSystem;
import com.mars_sim.core.person.ai.task.util.FactoryMetaTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskTrait;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * Meta task for the OptimizeSystem task.
 */
public class OptimizeSystemMeta extends FactoryMetaTask {

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(OptimizeSystemMeta.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.optimizeSystem"); //$NON-NLS-1$

	private static final int FACTOR = 20;
	
    public OptimizeSystemMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);
		setFavorite(FavoriteType.OPERATION, FavoriteType.TINKERING);
		setTrait(TaskTrait.ACADEMIC);
		setPreferredJob(JobType.SOFTWARE);
    }
    
	@Override
	public Task constructInstance(Person person) {
		return new OptimizeSystem(person);
	}

	@Override
	public double getProbability(Person person) {
		double result = 0D;
        
		if (person.isInSettlement()) {
            
			try {
				
				// Compute total entropy
				result = person.getSettlement().getBuildingManager().
						getTotalEntropy();
						
				if (result < 0.01)
					result = 0.01;
				
				double org = person.getNaturalAttributeManager().getAttribute(NaturalAttributeType.ORGANIZATION);
				double com = 0;
				
				if (person.getSkillManager().getSkill(SkillType.COMPUTING) != null) {
					com = person.getSkillManager().getSkill(SkillType.COMPUTING).getCumuativeExperience();
				}

				result = RandomUtil.getRandomDouble(result + org + com);
				
				if (JobType.COMPUTER_SCIENTIST == person.getMind().getJob())
	            	result *= 4;
				else if (JobType.ENGINEER == person.getMind().getJob())
	            	result *= 3;
				
	            if (RoleType.COMPUTING_SPECIALIST == person.getRole().getType())
	            	result *= 3;
	            else if (RoleType.ENGINEERING_SPECIALIST == person.getRole().getType())
	            	result *= 2;
	            else if (RoleType.CHIEF_OF_COMPUTING == person.getRole().getType())
	            	result *= 2.5;
	            else if (RoleType.CHIEF_OF_ENGINEERING == person.getRole().getType())
	            	result *= 1.5;
		        
			} catch (Exception e) {
				logger.log(Level.SEVERE, "getProbability()", e);
			}

			result *= getPersonModifier(person);
		}

//		logger.info(person, "OptimizeSystemMeta: " + Math.round(result * 10.0)/10.0);
		return result;
	}
}