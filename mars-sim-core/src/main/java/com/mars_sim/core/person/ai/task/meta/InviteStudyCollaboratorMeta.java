/**
 * Mars Simulation Project
 * InviteStudyCollaboratorMeta.java
 * @version 3.2.0 2021-06-20
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task.meta;

import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.fav.FavoriteType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.person.ai.task.InviteStudyCollaborator;
import com.mars_sim.core.person.ai.task.util.FactoryMetaTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskTrait;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.science.ScientificStudyUtil;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.tools.Msg;

/**
 * Meta task for the InviteStudyCollaborator task.
 */
public class InviteStudyCollaboratorMeta extends FactoryMetaTask {

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.inviteStudyCollaborator"); //$NON-NLS-1$
    /** default logger. */
    private static SimLogger logger = SimLogger.getLogger(InviteStudyCollaboratorMeta.class.getName());
    
    public InviteStudyCollaboratorMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.WORK_HOUR);
		setFavorite(FavoriteType.RESEARCH);
		setTrait(TaskTrait.LEADERSHIP);
		setPreferredJob(JobType.ACADEMICS);
		setPreferredRole(RoleType.CHIEF_OF_SCIENCE, RoleType.SCIENCE_SPECIALIST);
	}

    @Override
    public Task constructInstance(Person person) {
        return new InviteStudyCollaborator(person);
    }

    @Override
    public double getProbability(Person person) {

        double result = 0D;
        
        if (person.isInside()) {

            if (!person.getPhysicalCondition().isFitByLevel(1000, 70, 1000))
            	return 0;

            // Check if study is in invitation phase.
            ScientificStudy study = person.getStudy();
            if (study == null)
            	return 0;
            		
            else if (study.getPhase().equals(ScientificStudy.INVITATION_PHASE)) {

                // Check that there isn't a full set of open invitations already sent out.
                int collabNum = study.getCollaborativeResearchers().size();
                int openInvites = study.getNumOpenResearchInvitations();
                if ((openInvites + collabNum) < study.getMaxCollaborators()) {

                    // Check that there's scientists available for invitation.
                    if (ScientificStudyUtil.getAvailableCollaboratorsForInvite(study).isEmpty()) {
                    	logger.warning(person, 30_000L, "Can not find anyone to invite for " + study.getName());
                    }
                    else {

                    	// Once a proposal is finished get the invites out quickly
                        result += 100D;

                        if (person.isInVehicle()) {	
                	        // Check if person is in a moving rover.
                	        if (Vehicle.inMovingRover(person)) {
                		        // the bonus for proposing scientific study inside a vehicle, 
                	        	// rather than having nothing to do if a person is not driving
                	        	result += 20;
                	        } 	       
                	        else
                		        // the bonus for proposing scientific study inside a vehicle, 
                	        	// rather than having nothing to do if a person is not driving
                	        	result += 5;
                        }
                        
                        // Crowding modifier
                        Building adminBuilding = BuildingManager.getAvailableFunctionTypeBuilding(person, FunctionType.ADMINISTRATION);
                        result *= getBuildingModifier(adminBuilding, person);


                        // Increase probability if person's current job is related to study's science.
                        JobType job = person.getMind().getJob();
                        ScienceType science = study.getScience();
                        if (science == ScienceType.getJobScience(job)) {
                            result *= 2D;
                        }
                        result *= person.getAssociatedSettlement().getGoodsManager().getResearchFactor();

                        result *= getPersonModifier(person);
	                }
	            }
	        }
        }

        return result;
    }
}