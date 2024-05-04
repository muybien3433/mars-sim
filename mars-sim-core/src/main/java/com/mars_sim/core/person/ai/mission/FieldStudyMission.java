/*
 * Mars Simulation Project
 * FieldStudyMission.java
 * @date 2021-08-15
 * @author Scott Davis
 */
package com.mars_sim.core.person.ai.mission;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.job.util.JobType;
import com.mars_sim.core.person.ai.task.ScientificStudyFieldWork;
import com.mars_sim.core.person.ai.task.util.Task;
import com.mars_sim.core.person.ai.task.util.Worker;
import com.mars_sim.core.science.ScienceType;
import com.mars_sim.core.science.ScientificStudy;
import com.mars_sim.core.science.StudyStatus;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.vehicle.Rover;
import com.mars_sim.mapdata.location.Coordinates;
import com.mars_sim.mapdata.location.Direction;
import com.mars_sim.tools.util.RandomUtil;


/**
 * This is an abstract Field Study mission to a remote field location for a scientific
 * study. The concrete classes determine the science that is required.
 */
public abstract class FieldStudyMission extends EVAMission {

	private static final Set<JobType> PREFERRED_JOBS = Set.of(JobType.AREOLOGIST, JobType.ASTRONOMER, JobType.BIOLOGIST, JobType.BOTANIST, JobType.CHEMIST, JobType.METEOROLOGIST, JobType.PILOT);

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** Mission phase. */
	public static final MissionPhase RESEARCH_SITE = new MissionPhase("Mission.phase.researchingFieldSite");
	private static final MissionStatus NO_ONGOING_SCIENTIFIC_STUDY = new MissionStatus("Mission.status.noStudy");

	private static final int MIN_MEMEBRS = 2;

	
	// Data members
	/** The field site location. */
	private Coordinates fieldSite;
	/** Scientific study to research. */
	private ScientificStudy study;

	private double fieldSiteTime;
	private ScienceType science;

	/**
	 * Constructor.
	 * 
	 * @param startingPerson {@link Person} the person starting the mission.
	 * @param needsReview
	 * @throws MissionException if problem constructing mission.
	 */
	protected FieldStudyMission(MissionType missionType,
								Person startingPerson,
								ScienceType science, double fieldSiteTime, boolean needsReview) {

		// Use RoverMission constructor.
		super(missionType, startingPerson, null, RESEARCH_SITE);

		this.science = science;
		this.fieldSiteTime = fieldSiteTime;
		
		Settlement s = startingPerson.getSettlement();

		if (!isDone() && s != null) {
			// Set the lead researcher and study.
			study = determineStudy(science, startingPerson);
			if (study == null) {
				endMission(NO_ONGOING_SCIENTIFIC_STUDY);
			}

			// Recruit additional members to mission.
			if (!recruitMembersForMission(startingPerson, MIN_MEMEBRS))
				return;

			// Determine field site location.
			if (hasVehicle()) {
				double tripTimeLimit = getRover().getTotalTripTimeLimit(true);
				determineFieldSite(getVehicle().getRange(), tripTimeLimit);
			}

			// Add home settlement
			addNavpoint(s);

			// Check if vehicle can carry enough supplies for the mission.
			if (hasVehicle() && !isVehicleLoadable()) {
				endMission(CANNOT_LOAD_RESOURCES);
				return;
			}
		}

		if (!isDone()) {
			setInitialPhase(needsReview);
		}
	}

	/**
	 * Constructor with explicit information.
	 * 
	 * @param members            the mission members.
	 * @param leadResearcher     the lead researcher
	 * @param study              the scientific study.
	 * @param rover              the rover used by the mission.
	 * @param fieldSite          the field site to research.
	 * @param description        the mission description.
	 */
	protected FieldStudyMission(MissionType missionType,
			Person leadResearcher,
			Rover rover, ScientificStudy study, double fieldSiteTime,
			Collection<Worker> members,
			Coordinates fieldSite) {

		// Use RoverMission constructor.
		super(missionType, leadResearcher, rover, RESEARCH_SITE);

		this.study = study;
		this.science = study.getScience();
		this.fieldSite = fieldSite;
		this.fieldSiteTime = fieldSiteTime;
		addNavpoint(fieldSite, "field research site");

		// Add mission members.
		addMembers(members, false);
		
		// Add home settlement
		Settlement s = getStartingSettlement();
		addNavpoint(s);

			
		// Check if vehicle can carry enough supplies for the mission.
		if (hasVehicle() && !isVehicleLoadable()) {
			endMission(CANNOT_LOAD_RESOURCES);
			return;
		}

		// Set initial mission phase.
		setInitialPhase(false);
	}

	/**
	 * Gets the scientific study for the mission.
	 * 
	 * @return scientific study.
	 */
	public ScientificStudy getScientificStudy() {
		return study;
	}

	/**
	 * Gets the lead researcher for the mission.
	 * 
	 * @return the researcher.
	 */
	public Person getLeadResearcher() {
		return getStartingPerson();
	}

	/**
	 * Determine the scientific study used for the mission.
	 * 
	 * @param science Type of science in Mission
	 * @param researcher the science researcher.
	 * @return scientific study or null if none determined.
	 */
	public static ScientificStudy determineStudy(ScienceType science, Person researcher) {
		ScientificStudy result = null;

		List<ScientificStudy> possibleStudies = new ArrayList<>();

		// Add primary study if in research phase.
		ScientificStudy primaryStudy = researcher.getStudy();
		if (primaryStudy != null) {
			if ((StudyStatus.RESEARCH_PHASE == primaryStudy.getPhase())
					&& !primaryStudy.isPrimaryResearchCompleted()
					&& (science == primaryStudy.getScience())) {
				// Primary study added twice to double chance of random selection.
				possibleStudies.add(primaryStudy);
				possibleStudies.add(primaryStudy);
			}
		}

		// Add all collaborative studies in research phase.
		Iterator<ScientificStudy> i = researcher.getCollabStudies().iterator();
		while (i.hasNext()) {
			ScientificStudy collabStudy = i.next();
			if (StudyStatus.RESEARCH_PHASE.equals(collabStudy.getPhase())
					&& !collabStudy.isCollaborativeResearchCompleted(researcher)) {
				if (science == collabStudy.getContribution(researcher)) {
					possibleStudies.add(collabStudy);
				}
			}
		}

		// Randomly select study.
		if (possibleStudies.size() > 0) {
			int selected = RandomUtil.getRandomInt(possibleStudies.size() - 1);
			result = possibleStudies.get(selected);
		}

		return result;
	}

	/**
	 * Determine the location of the research site.
	 * 
	 * @param roverRange    the rover's driving range
	 * @param tripTimeLimit the time limit (millisols) of the trip.
	 * @throws MissionException of site can not be determined.
	 */
	private void determineFieldSite(double roverRange, double tripTimeLimit) {

		// Determining the actual traveling range.
		double range = roverRange;
		double timeRange = getTripTimeRange(tripTimeLimit, 1, true);
		if (timeRange < range) {
			range = timeRange;
		}

		// Get the current location.
		Coordinates startingLocation = getCurrentMissionLocation();

		// Determine the research site.
		Direction direction = new Direction(RandomUtil.getRandomDouble(2 * Math.PI));
		double limit = range / 4D;
		double siteDistance = RandomUtil.getRandomDouble(limit);
		fieldSite = startingLocation.getNewLocation(direction, siteDistance);
		addNavpoint(fieldSite, "field research site");
	}

	@Override
	public double getMissionQualification(Worker member) {
		double result = super.getMissionQualification(member);

		if ((result > 0D) && (member instanceof Person person)) {

			// Add modifier if person is a researcher on the same scientific study.
			if (study != null) {
				if (person.equals(study.getPrimaryResearcher())) {
					result += 2D;

					// Check if study's primary science.
					if (science == study.getScience()) {
						result += 1D;
					}
				} else if (study.getCollaborativeResearchers().contains(person)) {
					result += 1D;

					// Check if study collaboration science
					ScienceType collabScience = study.getContribution(person);
					if (science == collabScience) {
						result += 1D;
					}
				}
			}
		}

		return result;
	}

	/**
	 * Performs the research field site phase of the mission.
	 * 
	 * @param member the mission member currently performing the mission
	 */
	protected boolean performEVA(Person member) {

		// If person can research the site, start that task.
		if (canResearchSite(member)) {
			assignTask(member, createFieldStudyTask(member,
											getStartingPerson(),
											study, (Rover) getVehicle()));
		}

		return true;
	}

	/**
	 * Create a task for a researcher to start a fields study on site
	 * @param researcher
	 * @param leadResearcher
	 * @param study
	 * @param vehicle
	 * @return
	 */
	protected abstract Task createFieldStudyTask(Person person, Person leadResearcher,
												 ScientificStudy study,
												 Rover vehicle);

	/**
	 * Can a researcher to the required field research at a location.
	 * @param researcher
	 * @return
	 */
	protected boolean canResearchSite(Worker researcher) {
		return ScientificStudyFieldWork.canResearchSite(researcher, getRover()); 
	}
	

	@Override
	protected Set<JobType> getPreferredPersonJobs() {
		return PREFERRED_JOBS;
	}

	@Override
	protected double getEstimatedTimeAtEVASite(boolean buffer) {
		return fieldSiteTime;
	}
}
