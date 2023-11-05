/*
 * Mars Simulation Project
 * ObserveAstronomicalObjectsMeta.java
 * @date 2022-08-06
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.task.meta;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.mars_sim.core.data.RatingScore;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.fav.FavoriteType;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.role.RoleType;
import com.mars_sim.core.person.ai.task.ObserveAstronomicalObjects;
import com.mars_sim.core.person.ai.task.util.MetaTask;
import com.mars_sim.core.person.ai.task.util.SettlementMetaTask;
import com.mars_sim.core.person.ai.task.util.SettlementTask;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.TaskTrait;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.science.ScientificStudyManager;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.BuildingManager;
import com.mars_sim.core.structure.building.function.AstronomicalObservation;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.tools.Msg;
import com.mars_sim.tools.util.RandomUtil;

/**
 * Meta task for the ObserveAstronomicalObjects task.
 */
public class ObserveAstronomicalObjectsMeta extends MetaTask implements SettlementMetaTask {
     /**
     * Represents a Job needed to Observer Astronomical objects for a science study
     */
    private static class AstronomicalTaskJob extends SettlementTask {

		private static final long serialVersionUID = 1L;

        public AstronomicalTaskJob(SettlementMetaTask owner, ScientificStudy s, RatingScore score) {
            super(owner, "Astronomy Observations", s, score);
        }

        @Override
        public Task createTask(Person person) {
            return new ObserveAstronomicalObjects(person, (ScientificStudy) getFocus());
        }
    }

    /** Task name */
    private static final String NAME = Msg.getString(
            "Task.description.observeAstronomicalObjects"); //$NON-NLS-1$
    private static ScientificStudyManager ssm;

    public ObserveAstronomicalObjectsMeta() {
		super(NAME, WorkerType.PERSON, TaskScope.ANY_HOUR);
		
		setFavorite(FavoriteType.ASTRONOMY, FavoriteType.RESEARCH);
		setTrait(TaskTrait.ACADEMIC, TaskTrait.ARTISTIC);
		setPreferredJob(JobType.ASTRONOMER);
		setPreferredRole(RoleType.CHIEF_OF_SCIENCE, RoleType.SCIENCE_SPECIALIST);
	}
    
    /**
     * Get task for any Scientific study that needs Astronomy observation time.
     * Assessment is based on it getting dark and available Observatory
     * @param target Settlement being checked
     */
    @Override
    public List<SettlementTask> getSettlementTasks(Settlement target) {
        List<SettlementTask> result = new ArrayList<>();
        if (ObserveAstronomicalObjects.areConditionsSuitable(target)
                && !target.getBuildingManager().getBuildingSet(FunctionType.ASTRONOMICAL_OBSERVATION).isEmpty()) {    
            // Any Astro based study active at this Settlement
            for(ScientificStudy s : ssm.getAllStudies(target)) {
                if (isSuitableReseach(s)) {
                    // Suitable study so create tasks for each Observatory
                    RatingScore score = new RatingScore(100);
                    score.addModifier(GOODS_MODIFIER, (target.getGoodsManager().getTourismFactor()
                            + target.getGoodsManager().getResearchFactor())/1.5D);

                    result.add(new AstronomicalTaskJob(this, s, score));
                }
            }      
        }
        return result;
    }

    /**
     * Assess the suitability of a Person do to an Observation task. Based largely on the Study
     * the eprson is performing.
     * @param st Task  on offer
     * @param p Person being assessed
     */
    @Override
    public RatingScore assessPersonSuitability(SettlementTask st, Person p) {
        if (!p.isInSettlement()
            || !p.getPhysicalCondition().isFitByLevel(500, 50, 500)) {
            return RatingScore.ZERO_RATING;
        }

        // Check these is a Observatory usable
        var observatory = determineObservatory(p.getSettlement());
        if (observatory == null) {
            return RatingScore.ZERO_RATING;
        }

        double researchModifier = 0D;

        // Add probability for researcher's primary study (if any).
        ScientificStudy s = (ScientificStudy) st.getFocus();
        if (s.equals(p.getStudy())
            && !s.isPrimaryResearchCompleted()) {
           researchModifier = 1.3D;
        }
        // Add probability for each study researcher is collaborating on.
        if ((ScienceType.ASTRONOMY == s.getContribution(p))
            && !s.isCollaborativeResearchCompleted(p)) {
            researchModifier = 1.1D;
        }

        // Can person contribute
        if (researchModifier == 0D) {
            return RatingScore.ZERO_RATING;
        }

        RatingScore result = super.assessPersonSuitability(st, p);
        result.addModifier("research", researchModifier);

        // If researcher's current job isn't related to astronomy, divide by two.
        JobType job = p.getMind().getJob();
        if (job != null) {
            ScienceType jobScience = ScienceType.getJobScience(job);
            if (ScienceType.ASTRONOMY != jobScience) {
                result.addModifier("science", 1.2D);
            }
        }

        result = assessBuildingSuitability(result, observatory.getBuilding(), p);

        return result;
    }

    /**
     * Is this study suitable for Astronomy. Either primary science type or has a collaboration
     * of Astronomy from at least one collabarator.
     * @param study
     * @return
     */
    private static boolean isSuitableReseach(ScientificStudy study) {
        return (ScientificStudy.RESEARCH_PHASE.equals(study.getPhase())
            && ((ScienceType.ASTRONOMY == study.getScience())
                || study.getCollaborationScience().contains(ScienceType.ASTRONOMY)));
    }

    /**
	 * Gets the preferred local astronomical observatory for an observer.
	 * 
	 * @param observer the observer.
	 * @return observatory or null if none found.
	 */
	public static AstronomicalObservation determineObservatory(Settlement target) {

		BuildingManager manager = target.getBuildingManager();
		Set<Building> observatoryBuildings = manager.getBuildingSet(FunctionType.ASTRONOMICAL_OBSERVATION);
		observatoryBuildings = BuildingManager.getNonMalfunctioningBuildings(observatoryBuildings);
		observatoryBuildings = getObservatoriesWithAvailableSpace(observatoryBuildings);
		observatoryBuildings =  BuildingManager.getLeastCrowdedBuildings(observatoryBuildings);

		Building selected = RandomUtil.getARandSet(observatoryBuildings);
		if (selected == null) {
			return null;
		}
		return selected.getAstronomicalObservation();
	}

    /**
	 * Gets a list of observatory buildings with available research space from a
	 * list of observatory buildings.
	 * 
	 * @param buildingList list of buildings with astronomical observation function.
	 * @return observatory buildings with available observatory space.
	 */
	private static Set<Building> getObservatoriesWithAvailableSpace(Set<Building> buildings) {
		Set<Building> result = new HashSet<>();

		for(Building building : buildings) {
			AstronomicalObservation observatory = building.getAstronomicalObservation();
			if (observatory.getObserverNum() < observatory.getObservatoryCapacity()) {
				result.add(building);
			}
		}

		return result;
	}

    public static void initialiseInstances(ScientificStudyManager scientificStudyManager) {
        ssm = scientificStudyManager;
    }
}
