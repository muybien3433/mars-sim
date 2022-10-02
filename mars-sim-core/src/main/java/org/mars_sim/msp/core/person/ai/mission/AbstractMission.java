/*
 * Mars Simulation Project
 * Mission.java
 * @date 2022-08-10
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.stream.Collectors;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.UnitManager;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.data.UnitSet;
import org.mars_sim.msp.core.environment.SurfaceFeatures;
import org.mars_sim.msp.core.events.HistoricalEvent;
import org.mars_sim.msp.core.events.HistoricalEventManager;
import org.mars_sim.msp.core.logging.Loggable;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.EventType;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.PersonConfig;
import org.mars_sim.msp.core.person.ai.NaturalAttributeType;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.job.util.ShiftType;
import org.mars_sim.msp.core.person.ai.social.RelationshipUtil;
import org.mars_sim.msp.core.person.ai.task.util.Task;
import org.mars_sim.msp.core.person.ai.task.util.Worker;
import org.mars_sim.msp.core.robot.Robot;
import org.mars_sim.msp.core.robot.ai.job.RobotJob;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.time.Temporal;
import org.mars_sim.msp.core.tool.Conversion;
import org.mars_sim.msp.core.tool.RandomUtil;


/**
 * The Mission class represents a large multi-person task There is at most one
 * instance of a mission per person. A Mission may have one or more people
 * associated with it.
 */
public abstract class AbstractMission implements Mission, Temporal {

	// Plain POJO to help score potential mission members
	private static final class MemberScore {
		Person candidate;
		double score;

		private MemberScore(Person candidate, double personValue) {
			super();
			this.candidate = candidate;
			this.score = personValue;
		}

		public double getScore() {
			return score;
		}

		@Override
		public String toString() {
			return "MemberScore [candidate=" + candidate + ", score=" + score + "]";
		}
	}

	/** default serial id. */
	private static final long serialVersionUID = 1L;
	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(AbstractMission.class.getName());

	private static final String OUTSIDE = "Outside";

	private static final int MAX_CAP = 8;

	private static final MissionPhase COMPLETED_PHASE = new MissionPhase("completed", MissionPhase.Stage.CLOSEDOWN);
	private static final MissionPhase ABORTED_PHASE = new MissionPhase("aborted", MissionPhase.Stage.CLOSEDOWN);
	protected static final MissionPhase REVIEWING = new MissionPhase("reviewing", MissionPhase.Stage.PREPARATION);


	protected static final MissionStatus NOT_ENOUGH_MEMBERS = new MissionStatus("Mission.status.noMembers");
	private static final MissionStatus MISSION_NOT_APPROVED = new MissionStatus("Mission.status.notApproved");
	private static final MissionStatus MISSION_ACCOMPLISHED = new MissionStatus("Mission.status.accomplished");
	private static final MissionStatus MISSION_ABORTED = new MissionStatus("Mission.status.aborted");

	private static final String INTERNAL_PROBLEM = "Mission.status.internalProblem";


	// Data members
	/** The number of people that can be in the mission. */
	private int missionCapacity;
	/** The mission priority (between 1 and 5, with 1 the lowest, 5 the highest) */
	private int priority = 2;

	
	/** Has the current phase ended? */
	private boolean phaseEnded;
	/** True if mission is completed. */
	private boolean done = false;
	private boolean aborted = false;


	/** The Name of this mission. */
	private String missionName;
	/** Unique identifer  */
	private int identifier;
	/** The full mission designation. */
	private String fullMissionDesignation = "";
	
	/** The mission type enum. */
	private MissionType missionType;

	/** A list of mission status. */
	private Set<MissionStatus> missionStatus;
	/** The current phase of the mission. */
	private MissionPhase phase;
	/** The description of the current phase of operation. */
	private String phaseDescription;
	/** Time the phase started */
	private MarsClock phaseStartTime;
	/** Log of mission activity	 */
	private MissionLog log = new MissionLog();

	/** The name of the starting member */
	private Worker startingMember;
	/** The mission plan. */
	private MissionPlanning plan;

	/** 
	 * A set of those who sign up for this mission. 
	 * After the mission is over. It will be retained and will not be deleted.
	 */
	private Set<Worker> signUp;

	/** 
	 * A collection of those who are actually went on the mission.
	 * After the mission is over. All members will be removed 
	 * and the collection will become empty.
	 */
	private Set<Worker> members;
	
	// transient members
	/** Mission listeners. */
	private transient List<MissionListener> listeners;

	// Static members
	protected static UnitManager unitManager;
	protected static HistoricalEventManager eventManager;
	protected static MissionManager missionManager;
	protected static SurfaceFeatures surfaceFeatures;
	protected static PersonConfig personConfig;
	protected static MarsClock marsClock;

	/**
	 * Constructor.
	 *
	 * @param missionType
	 * @param startingMember
	 */
	protected AbstractMission(MissionType missionType, Worker startingMember) {
		// Initialize data members

		this.identifier = missionManager.getNextIdentifier();
		
		this.missionName = missionType.getName() + " #" + identifier;
		this.missionType = missionType;
		this.startingMember = startingMember;

		missionStatus = new HashSet<>();
		members = new UnitSet<>();
		done = false;
		phase = null;
		phaseDescription = "";
		phaseEnded = false;
		missionCapacity = MAX_CAP;
		
		signUp = new UnitSet<>();

		Person person = (Person) startingMember;

		if (person.isInSettlement()) {

			// Created mission starting event.
			registerHistoricalEvent(person, EventType.MISSION_START, "Mission Starting");

			// Log mission starting.
			int n = members.size();
			String appendStr = "";
			if (n == 0)
				appendStr = ".";
			else if (n == 1)
				appendStr = "' with 1 other.";
			else
				appendStr = "' with " + n + " others.";

			String article = "a ";

			String missionStr = missionName;

			if (!missionStr.toLowerCase().contains("mission"))
				missionStr = missionName + " mission";

			if(Conversion.isVowel(missionName))
				article = "an ";

			logger.log(startingMember, Level.INFO, 0,
					"Began organizing " + article + missionStr + appendStr);

			// Add starting member to mission.
			startingMember.setMission(this);

			// Note: do NOT set his shift to ON_CALL yet.
			// let the mission lead have more sleep before departing
		}

	}

	/**
	 * Adds a listener.
	 *
	 * @param newListener the listener to add.
	 */
	@Override
	public final void addMissionListener(MissionListener newListener) {
		if (listeners == null) {
			listeners = new CopyOnWriteArrayList<>();
		}
		synchronized (listeners) {
			if (!listeners.contains(newListener)) {
				listeners.add(newListener);
			}
		}
	}

	/**
	 * Removes a listener.
	 *
	 * @param oldListener the listener to remove.
	 */
	@Override
	public final void removeMissionListener(MissionListener oldListener) {
		if ((listeners != null) && listeners.contains(oldListener)) {
			synchronized (listeners) {
				listeners.remove(oldListener);
			}
		}
	}

	/**
	 * Fires a mission update event.
	 *
	 * @param updateType the update type.
	 */
	protected final void fireMissionUpdate(MissionEventType updateType) {
		fireMissionUpdate(updateType, this);
	}

	/**
	 * Fires a mission update event.
	 *
	 * @param addMemberEvent the update type.
	 * @param target         the event target or null if none.
	 */
	protected final void fireMissionUpdate(MissionEventType addMemberEvent, Object target) {
		if (listeners != null) {
			synchronized (listeners) {
				for (MissionListener l : listeners) {
					l.missionUpdate(new MissionEvent(this, addMemberEvent, target));
				}
			}
		}
	}

	/**
	 * Gets the string representation of this mission.
	 */
	public String toString() {
		return missionName;
	}

	/**
	 * Adds a member.
	 * 
	 * @param member
	 */
	@Override
	public void addMember(Worker member) {
		if (!members.contains(member)) {
			members.add(member);

			signUp.add(member);
			if (UnitType.ROBOT == member.getUnitType()) {
				registerHistoricalEvent((Robot) member, EventType.MISSION_JOINING,
						"Adding a member");
			}
			else
				registerHistoricalEvent((Person) member, EventType.MISSION_JOINING,
									"Adding a member");
	
			fireMissionUpdate(MissionEventType.ADD_MEMBER_EVENT, member);

			logger.log(member, Level.FINER, 0, "Just got added to " + missionName + ".");
		}
	}

	/**
	 * Registers this historical event.
	 * 
	 * @param person
	 * @param type
	 * @param message
	 */
	private void registerHistoricalEvent(Worker member, EventType type, String message) {
		String container = null;
		String hometown = null;
		String coordinates = null;
		if (member.isInSettlement()) {
			container = member.getBuildingLocation().getNickName();
			hometown = member.getAssociatedSettlement().getName();
			coordinates = member.getAssociatedSettlement().getCoordinates().getCoordinateString();
		} else if (member.isInVehicle()) {
			container = member.getVehicle().getName();
			hometown = member.getVehicle().getCoordinates().toString();
			coordinates = member.getVehicle().getCoordinates().getCoordinateString();
		} else {
			container = OUTSIDE;
			hometown = member.getAssociatedSettlement().getName();
			coordinates = member.getCoordinates().toString();
		}

		// Creating mission joining event.
		HistoricalEvent newEvent = new MissionHistoricalEvent(type, this,
				message, missionName, member.getName(), 
				container, hometown, coordinates);
		eventManager.registerNewEvent(newEvent);
	}

	public final void adjustShift(Worker member) {
		// Added codes in reassigning a work shift
		if (member.getUnitType() == UnitType.PERSON) {
			Person person = (Person) member;
			person.getMind().stopMission();
			member.setMission(null);

			ShiftType shift = null;
			if (person.getSettlement() != null) {
				shift = person.getSettlement().getAnEmptyWorkShift(-1);
				person.setShiftType(shift);
			}
			else if ((person.getVehicle() != null) && (person.getVehicle().getSettlement() != null)) {
					shift = person.getVehicle().getSettlement().getAnEmptyWorkShift(-1);
					person.setShiftType(shift);
			}

			registerHistoricalEvent(person, EventType.MISSION_FINISH, "Removing a member");
			fireMissionUpdate(MissionEventType.REMOVE_MEMBER_EVENT, member);
		}
	}
	
	/**
	 * Removes a member from the mission.
	 *
	 * @param member to be removed
	 */
	@Override
	public void removeMember(Worker member) {
		if (members.contains(member)) {
			members.remove(member);
			// Adjust the work shift
			adjustShift(member);
		}
	}


	/**
	 * Gets a collection of the members in the mission.
	 *
	 * @return collection of members
	 */
	@Override
	public final Set<Worker> getMembers() {
		return members;
	}

	/**
	 * Returns a list of people and robots who have signed up for this mission.
	 * 
	 * @return
	 */
	@Override
	public Set<Worker> getSignup() {
		return signUp;
	}
	
	/**
	 * Adds these members to the mission.
	 * 
	 * @param newMembers Members to add
	 * @param allowRobots Are Robots allowed
	 */
	protected void addMembers(Collection<Worker> newMembers, boolean allowRobots) {
		for(Worker member : newMembers) {
			if (member.getUnitType() == UnitType.PERSON) {
				((Person) member).getMind().setMission(this);
			}
			else {
				if (!allowRobots) {
					throw new IllegalStateException("Mission does not supprot robots");
				}
				((Robot) member).getBotMind().setMission(this);
			}
		}
	}

	/**
	 * Determines if mission is completed.
	 *
	 * @return true if mission is completed
	 */
	@Override
	public final boolean isDone() {
		return done;
	}

	/**
	 * Gets the name of the mission.
	 *
	 * @return name of mission
	 */
	@Override
	public final String getName() {
		return missionName;
	}

	/**
	 * Updates the mission name.
	 * 
	 * @param newName
	 */
    public void setName(String newName) {
		this.missionName = newName;
    }

	/**
	 * Gets the mission type enum.
	 *
	 * @return
	 */
	@Override
	public MissionType getMissionType() {
		return missionType;
	}

	/**
	 * Gets the current phase of the mission.
	 *
	 * @return phase
	 */
	@Override
	public MissionPhase getPhase() {
		return phase;
	}

	/**
	 * Sets the mission phase and the current description.
	 *
	 * @param newPhase the new mission phase.
	 * @param subjectOfPhase This is the subject of the phase
	 * @throws MissionException if newPhase is not in the mission's collection of
	 *                          phases.
	 */
	protected final void setPhase(MissionPhase newPhase, String subjectOfPhase) {
		if (newPhase == null) {
			throw new IllegalArgumentException("newPhase is null");
		}

		// Move phase on
 		phase = newPhase;
		setPhaseEnded(false);
		phaseStartTime = new MarsClock(marsClock);

		String template = newPhase.getDescriptionTemplate();
		if (template != null) {
			phaseDescription = MessageFormat.format(template, subjectOfPhase);
		}
		else {
			phaseDescription = "";
		}

		// Add entry to the log
		addMissionLog(newPhase.getName());

		fireMissionUpdate(MissionEventType.PHASE_EVENT, newPhase);
	}

	protected void addMissionLog(String entry) {
		log.addEntry(entry);
	}

	/**
	 * Gets the mission log.
	 */
	@Override
	public MissionLog getLog() {
		return log;
	}

	/**
	 * Time that the current phases started
	 */
	@Override
	public MarsClock getPhaseStartTime() {
		return phaseStartTime;
	}

	/**
	 * Gets duration of current Phase.
	 */
	protected double getPhaseDuration() {
		return MarsClock.getTimeDiff(marsClock, phaseStartTime);
	}

	/**
	 * Gets the description of the current phase.
	 *
	 * @return phase description.
	 */
	@Override
	public final String getPhaseDescription() {
		if (phaseDescription != null && !phaseDescription.equals("")) {
			return phaseDescription;
		} else if (phase != null) {
			return phase.toString();
		} else
			return "";
	}

	/**
	 * Sets the description of the current phase.
	 *
	 * @param description the phase description.
	 */
	protected final void setPhaseDescription(String description) {
		phaseDescription = description;
		fireMissionUpdate(MissionEventType.PHASE_DESCRIPTION_EVENT, description);
	}

	/**
	 * Performs the mission.
	 *
	 * @param member the member performing the mission.
	 * @return Can the work participate
	 */
	@Override
	public boolean performMission(Worker member) {
		if (!canParticipate(member)) {
			return false;
		}

		// If current phase is over, decide what to do next.
		if (phaseEnded && !determineNewPhase()) {
			logger.warning(member, "New phase for " + getName()
							+ " cannot be determined for " + phase.getName());
		}

		// Perform phase.
		if (!done) {
			performPhase(member);
		}
		return true;
	}

	/**
	 * Determines a new phase for the mission when the current phase has ended.
	 *
	 * @return Has the new phase been identified
	 * @throws MissionException if problem setting a new phase.
	 */
	protected abstract boolean determineNewPhase();

	/**
	 * The member performs the current phase of the mission.
	 *
	 * @param member the member performing the phase.
	 */
	protected void performPhase(Worker member) {
		if (phase == null) {
			endMissionProblem(member, "Current phase null");
		}

		if (REVIEWING.equals(getPhase())) {
			requestReviewPhase(member);
		}
	}

	/**
	 * Gets the mission capacity for participating people.
	 *
	 * @return mission capacity
	 */
	@Override
	public final int getMissionCapacity() {
		return missionCapacity;
	}

	/**
	 * Sets the mission capacity to a given value.
	 *
	 * @param newCapacity the new mission capacity
	 */
	protected final void setMissionCapacity(int newCapacity) {
		missionCapacity = newCapacity;
		fireMissionUpdate(MissionEventType.CAPACITY_EVENT, newCapacity);
	}

	/** 
	 * Abort the current phase; nothign on the base class
	 */
	public void abortPhase() {
		// Do nothing
	}

	/**
	 * Abort the mission by the user. Will stop currnet phase.
	 */
	@Override
	public final void abortMission() {
		abortMission(MISSION_ABORTED, null);
	}

	/**
	 * Mission is being aborted because of a problem.
	 * @param reason Reason to abort
	 * @param event Optional type of event to create
	 */
	protected void abortMission(MissionStatus reason, EventType event) {
		aborted = true;
	}

	/**
	 * Computes the mission experience score.
	 *
	 * @param reason
	 */
	private void addMissionScore() {
		for (Worker member : members) {
			if (member.getUnitType() == UnitType.PERSON) {
				Person person = (Person) member;

				if (!person.isDeclaredDead()) {
					if (person.getPhysicalCondition().hasSeriousMedicalProblems()) {
						// Note : there is a minor penalty for those who are sick
						// and thus unable to fully function during the mission
						person.addMissionExperience(missionType, 2);
					}
					else if (person.equals(startingMember)) {
						// The mission lead receive extra bonus
						person.addMissionExperience(missionType, 6);

						// Add a leadership point to the mission lead
						person.getNaturalAttributeManager().adjustAttribute(NaturalAttributeType.LEADERSHIP, 1);
					}
					else
						person.addMissionExperience(missionType, 3);
				}
			}
		}
	}

	/**
	 * Finalizes the mission. Reason for ending mission. Mission can
	 * override this to perform necessary finalizing operations.
	 *
	 * @param endStatus A status to add for the end of Mission
	 *
	 */
	protected void endMission(MissionStatus endStatus) {
		if (done) {
			logger.warning(startingMember, "Mission " + getName() + " is already ended.");
			return;
		}

		// Ended with a status
		if (endStatus != null) {
			missionStatus.add(endStatus);
		}

		// If no mission flags have been added then it was accomplised
		String listOfStatuses = missionStatus.stream().map(MissionStatus::getName).collect(Collectors.joining(", "));
		MissionPhase finalPhase;
		if (missionStatus.isEmpty() && !aborted) {
			missionStatus.add(MISSION_ACCOMPLISHED);
			addMissionScore();
			finalPhase = COMPLETED_PHASE;
		}
		else {
			finalPhase = ABORTED_PHASE;
		}
		setPhase(finalPhase, listOfStatuses);
		log.setDone();
		done = true; 
		
		StringBuilder status = new StringBuilder();
		status.append("Ended the ")
			.append(getName())
			.append(" with the status flag(s): ")
			.append(listOfStatuses);
		logger.info(startingMember, status.toString());

		// Disband the members
		if (members != null && !members.isEmpty()) {
			String listOfMembers = members.stream().map(Worker::getName).collect(Collectors.joining(", "));
			logger.info(startingMember, "Disbanding mission member(s): " + listOfMembers);
			
			for(Worker member : members) {
				adjustShift(member);
			}	
			members.clear();
		}
	}

	/**
	 * Assigns a particular pending task for the worker
	 * 
	 * @param worker
	 * @param task string
	 * @return
	 */
	protected boolean assignPendingTask(Worker worker, String task) {
		return worker.getTaskManager().addAPendingTask(task, false);
	}
	
	/**
	 * Checks if a person has any issues in starting a new task.
	 *
	 * @param person the person to assign to the task
	 * @param task   the new task to be assigned
	 * @return true if task can be performed.
	 */
	protected boolean assignTask(Person person, Task task) {
		boolean canPerformTask = !task.isEffortDriven() || (person.getPerformanceRating() != 0D);

		// If task is effort-driven and person too ill, do not assign task.

        if (canPerformTask) {
			canPerformTask = person.getMind().getTaskManager().addTask(task);
		}

		return canPerformTask;
	}

	/**
	 * Adds a new task for a robot in the mission. Task may be not assigned if the
	 * robot has a malfunction.
	 *
	 * @param robot the robot to assign to the task
	 * @param task  the new task to be assigned
	 * @return true if task can be performed.
	 */
	protected boolean assignTask(Robot robot, Task task) {

		// If robot is malfunctioning, it cannot perform task.
		if (robot.getMalfunctionManager().hasMalfunction()) {
			return false;
		}

		if (!robot.getSystemCondition().isBatteryAbove(5))
			return false;

		return robot.getBotMind().getBotTaskManager().addTask(task);
	}

	/**
	 * Checks to see if any of the people in the mission have any dangerous medical
	 * problems that require treatment at a settlement. Also any environmental
	 * problems, such as suffocation.
	 *
	 * @return true if dangerous medical problems
	 */
	private final boolean hasDangerousMedicalProblems() {
		Person patient = null;
		for (Worker member : members) {
			if (member.getUnitType() == UnitType.PERSON) {
				if (((Person) member).getPhysicalCondition().hasSeriousMedicalProblems()) {
					patient = (Person) member;
				}
			}
		}

		if (patient != null) {
			// Abort the mission and return home
			abortMission(new MissionStatus("Mission.status.medicalEmergency", patient.getName()),
						 EventType.MISSION_MEDICAL_EMERGENCY);
		}
		return patient != null;
	}

	/**
	 * Checks to see if any of the people in the mission have any potential medical
	 * problems due to low fitness level that will soon degrade into illness.
	 *
	 * @return true if potential medical problems exist
	 */
	protected final boolean hasAnyPotentialMedicalProblems() {
		for (Worker member : members) {
			if (member.getUnitType() == UnitType.PERSON) {
				if (((Person) member).getPhysicalCondition().computeFitnessLevel() < 2) {
					return true;
				}
			}
		}

		return false;
	}

	/**
	 * Checks to see if all of the people in the mission have any dangerous medical
	 * problems that require treatment at a settlement. Also any environmental
	 * problems, such as suffocation.
	 *
	 * @return true if all have dangerous medical problems
	 */
	public final boolean hasDangerousMedicalProblemsAllCrew() {
		boolean result = true;
		for (Worker member : members) {
			if (member.getUnitType() == UnitType.PERSON) {
				if (!((Person) member).getPhysicalCondition().hasSeriousMedicalProblems()) {
					result = false;
				}
			}
		}
		return result;
	}

	/**
	 * Checks if the mission has an emergency situation.
	 *
	 * @return true if emergency.
	 */
	protected boolean hasEmergency() {
		return hasDangerousMedicalProblems();
	}

	/**
	 * Checks if the mission has an emergency situation affecting all the crew.
	 *
	 * @return true if emergency affecting all.
	 */
	public boolean hasEmergencyAllCrew() {
		return hasDangerousMedicalProblemsAllCrew();
	}

	/**
	 * Recruits new members into the mission.
	 *
	 * @param startingMember the mission member starting the mission.
	 * @param sameSettlement do members have to be at the same Settlement as the starting Member
	 * @param minMembers Minimum number of members requried
	 */
	protected boolean recruitMembersForMission(Worker startingMember, boolean sameSettlement, int minMembers) {

		// Get all people qualified for the mission.
		Collection<Person> possibles;
		if (sameSettlement) {
			possibles = startingMember.getAssociatedSettlement().getAllAssociatedPeople();
		}
		else {
			possibles = unitManager.getPeople();
		}

		List<MemberScore> qualifiedPeople = new ArrayList<>();
		for(Person person : possibles) {
			if (isCapableOfMission(person)) {
				// Determine the person's mission qualification.
				double qualification = getMissionQualification(person) * 100D;

				// Determine how much the recruiter likes the person.
				double likability = 50D;
				if (startingMember.getUnitType() == UnitType.PERSON) {
					likability = RelationshipUtil.getOpinionOfPerson((Person) startingMember, person);
				}

				// Check if person is the best recruit.
				double personValue = (qualification + likability) / 2D;
				qualifiedPeople.add(new MemberScore(person, personValue));
			}
		}

		int pop = startingMember.getAssociatedSettlement().getNumCitizens();
		int max = 0;

		if (pop < 4)
			max = 1;
		else if (pop >= 4 && pop < 7)
			max = 2;
		else if (pop >= 7 && pop < 10)
			max = 3;
		else if (pop >= 10 && pop < 14)
			max = 4;
		else if (pop >= 14 && pop < 18)
			max = 5;
		else if (pop >= 18 && pop < 23)
			max = 6;
		else if (pop >= 23 && pop < 29)
			max = 7;
		else if (pop >= 29)
			max = 8;

		// 50% tendency to have 1 less person
		int rand = RandomUtil.getRandomInt(1);
		if (rand == 1) {
			if (max >= 5)
				max--;
		}

		// Max can not bigger than mission capacity
		max = Math.min(max, missionCapacity);

		// Recruit the most qualified and most liked people first.
		qualifiedPeople.sort(Comparator.comparing(MemberScore::getScore, Comparator.reverseOrder()));
		while (!qualifiedPeople.isEmpty() && (members.size() < max)) {

			// Try to recruit best person available to the mission.
			MemberScore next = qualifiedPeople.remove(0);
			recruitPerson(startingMember, next.candidate);
		}

		if (members.size() < minMembers) {
			endMission(NOT_ENOUGH_MEMBERS);
			return false;
		}

		return true;
	}

	/**
	 * Attempts to recruit a new person into the mission.
	 *
	 * @param recruiter the mission member doing the recruiting.
	 * @param recruitee the person being recruited.
	 */
	private void recruitPerson(Worker recruiter, Person recruitee) {
		if (isCapableOfMission(recruitee)) {
			// Get mission qualification modifier.
			double qualification = getMissionQualification(recruitee) * 100D;
			// Get the recruitee's social opinion of the recruiter.
			double recruiterLikability = 50D;
			if (recruiter.getUnitType() == UnitType.PERSON) {
				recruiterLikability = RelationshipUtil.getOpinionOfPerson(recruitee, (Person) recruiter);
			}

			// Get the recruitee's average opinion of all the current mission members.
			List<Person> people = new ArrayList<>();
			Iterator<Worker> i = members.iterator();
			while (i.hasNext()) {
				Worker member = i.next();
				if (member.getUnitType() == UnitType.PERSON) {
					people.add((Person) member);
				}
			}
			double groupLikability = RelationshipUtil.getAverageOpinionOfPeople(recruitee, people);

			double recruitmentChance = (qualification + recruiterLikability + groupLikability) / 3D;
			if (recruitmentChance > 100D) {
				recruitmentChance = 100D;
			} else if (recruitmentChance < 0D) {
				recruitmentChance = 0D;
			}

			if (RandomUtil.lessThanRandPercent(recruitmentChance)) {
				recruitee.setMission(this);

				// NOTE: do not set his shift to ON_CALL until after the mission plan has been approved
			}
		}
	}

	/**
	 * Checks to see if a member is capable of joining a mission.
	 *
	 * @param member the member to check.
	 * @return true if member could join mission.
	 */
	protected boolean isCapableOfMission(Worker member) {
		boolean result = false;

		if (member == null) {
			throw new IllegalArgumentException("member is null");
		}

		if (member.getUnitType() == UnitType.PERSON) {
			Person person = (Person) member;

			// Make sure person isn't already on a mission.
			boolean onMission = (person.getMind().getMission() != null);

			// Make sure person doesn't have any serious health problems.
			boolean healthProblem = person.getPhysicalCondition().hasSeriousMedicalProblems();

			// Check if person is qualified to join the mission.
			boolean isQualified = (getMissionQualification(person) > 0D);

			if (!onMission && !healthProblem && isQualified) {
				result = true;
			}
		}

		return result;
	}

	/**
	 * Gets the mission qualification value for the member. Member is qualified in
	 * joining the mission if the value is larger than 0. The larger the
	 * qualification value, the more likely the member will be picked for the
	 * mission.
	 *
	 * @param member the member to check.
	 * @return mission qualification value.
	 */
	@Override
	public double getMissionQualification(Worker member) {

		double result = 0D;

		if (member.getUnitType() == UnitType.PERSON) {
			Person person = (Person) member;
			result = Math.max(5,  person.getMissionExperience(missionType));

			// Get base result for job modifier.
			Set<JobType> prefered = getPreferredPersonJobs();
			JobType job = person.getMind().getJob();
			double jobModifier;
			if ((prefered != null) && prefered.contains(job)) {
				jobModifier = 1D;
			}
			else {
				jobModifier = 0.5D;
			}

			result = result + 2 * result * jobModifier;
		}
		else {
			Robot robot = (Robot) member;

			// Get base result for job modifier.
			RobotJob job = robot.getBotMind().getRobotJob();
			if (job != null) {
				result = job.getJoinMissionProbabilityModifier(this.getClass());
			}
		}

		return result;
	}

	/**
	 * Gets the preferred Job types.
	 * 
	 * @return
	 */
	protected Set<JobType> getPreferredPersonJobs() {
		return Collections.emptySet();
	}

	/**
	 * Checks if the current phase has ended or not.
	 *
	 * @return true if phase has ended
	 */
	public final boolean getPhaseEnded() {
		return phaseEnded;
	}

	/**
	 * Sets if the current phase has ended or not.
	 *
	 * @param phaseEnded true if phase has ended
	 */
	protected final void setPhaseEnded(boolean phaseEnded) {
		this.phaseEnded = phaseEnded;
	}

	/**
	 * Gets the number and amounts of resources needed for the mission.
	 *
	 * @param useBuffer use time buffers in estimation if true.
	 * @return map of amount and item resources and their Double amount or Integer
	 *         number.
	 */
	protected abstract Map<Integer, Number> getResourcesNeededForRemainingMission(boolean useBuffer);

	/**
	 * Gets the number and types of equipment needed for the mission.
	 *
	 * @param useBuffer use time buffers in estimation if true.
	 * @return map of equipment types and number.
	 */
	protected Map<Integer, Integer> getEquipmentNeededForRemainingMission(boolean useBuffer) {
		return new HashMap<>();
	}

	/**
	 * Time passing for mission.
	 *
	 * @param time the amount of time passing (in millisols)
	 * @throws Exception if error during time passing.
	 */
	public boolean timePassing(ClockPulse pulse) {
		return true;
	}


	/**
	 * Gets the current location of the mission.
	 *
	 * @return coordinate location.
	 * @throws MissionException if error determining location.
	 */
	public Coordinates getCurrentMissionLocation() {

		Coordinates result = null;

		if (startingMember != null)	{
			Person p = (Person)startingMember;
			Settlement s = p.getSettlement();
			if (s != null)
				return s.getCoordinates();
			if (p.isInVehicle())
				return p.getVehicle().getCoordinates();
			else
				return p.getCoordinates();

		}
		else {
			logger.severe(getName() + ":No starting member");
		}

		return result;
	}

	/**
	 * Requests review for the mission.
	 *
	 * @param member the mission lead.
	 */
	private void requestReviewPhase(Worker member) {
		Person p = (Person)member;

		if (plan == null) {
			throw new IllegalStateException("No Mission plan");
		}

		switch(plan.getStatus()) {
			case NOT_APPROVED:
				endMission(MISSION_NOT_APPROVED);
				break;
			
			case APPROVED:
				createFullDesignation();

				logger.log(p, Level.INFO, 0, "Mission plan for " + getName() + " was approved.");

				if (!(this instanceof VehicleMission)) {
					// Set the members' work shift to on-call to get ready
					for (Worker m : members) {
						((Person) m).setShiftType(ShiftType.ON_CALL);
					}
				}
				setPhaseEnded(true);
				break;
			default:
				// Nothing to do yet
		}
	}

	/**
	 * Start reviewing this Mission
	 */
	protected void startReview() {
		setPhase(REVIEWING, null);
		plan = new MissionPlanning(this);
	}
	/**
	 * Returns the mission plan.
	 *
	 * @return {@link MissionPlanning}
	 */
	@Override
	public MissionPlanning getPlan() {
		return plan;
	}

	/**
	 * Returns the starting person.
	 *
	 * @return {@link Person}
	 */
	@Override
	public Person getStartingPerson() {
		if (startingMember.getUnitType() == UnitType.PERSON)
			return (Person)startingMember;
		else
			return null;
	}

	/**
	 * Sets the starting member.
	 *
	 * @param member the new starting member
	 */
	protected final void setStartingMember(Worker member) {
		this.startingMember = member;
		fireMissionUpdate(MissionEventType.STARTING_SETTLEMENT_EVENT);
	}

	/**
	 * Mission designation code. Only defined once mission has started.
	 */
	@Override
	public String getFullMissionDesignation() {
		return fullMissionDesignation;
	}

	/**
	 * Creates the mission designation string for this mission.
	 *
	 * @return
	 */
	protected void createFullDesignation() {
		StringBuilder buffer = new StringBuilder();
		buffer.append(Conversion.getInitials(missionType.getName().replace("with", "").trim()))
			  .append(" ")
			  .append(Conversion.getInitials(getAssociatedSettlement().getName()))
			  .append('-')
			  .append(String.format("%03d", identifier));
		fullMissionDesignation = buffer.toString();

		fireMissionUpdate(MissionEventType.DESIGNATION_EVENT, fullMissionDesignation);
	}

	/**
	 * An internal problem has happened so end the mission.
	 */
	protected void endMissionProblem(Loggable source, String reason) {
		MissionStatus status = new MissionStatus(INTERNAL_PROBLEM, reason);
		logger.severe(source, getName() + ": " + status.getName());
		endMission(status);
	}

	@Override
	public Set<MissionStatus> getMissionStatus() {
		return missionStatus;
	}

	/**
	 * Adds a new mission status.
	 *
	 * @param status
	 */
	protected boolean addMissionStatus(MissionStatus status) {
		boolean newStatus = missionStatus.add(status);
		if (newStatus) {
			addMissionLog(status.getName());
		}
		return newStatus;
	}

	@Override
	public int getPriority() {
		return priority;
	}

	/**
	 * Checks if this worker can participate.
	 * 
	 * @param worker This maybe used by overridding methods
	 * @return
	 */
	protected boolean canParticipate(Worker worker) {
		return true;
	}
	
	/**
	 * Compares if this object equals this instance of mission.
	 */
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null) return false;
		if (this.getClass() != obj.getClass()) return false;
		AbstractMission m = (AbstractMission) obj;
		return this.identifier == m.identifier;
	}

	/**
	 * Gets the hash code for this object.
	 *
	 * @return hash code.
	 */
	public int hashCode() {
		return (1 + identifier) % 64; 
	}

	/**
	 * Reloads instances after loading from a saved sim.
	 *
	 * @param si {@link Simulation}
	 * @param c {@link MarsClock}
	 * @param e {@link HistoricalEventManager}
	 * @param u {@link UnitManager}
	 * @param sf {@link SurfaceFeatures}
	 * @param m {@link MissionManager}
	 */
	public static void initializeInstances(Simulation si, MarsClock c, HistoricalEventManager e,
			UnitManager u, SurfaceFeatures sf, 
			MissionManager m, PersonConfig pc) {
		marsClock = c;
		eventManager = e;
		unitManager = u;
		surfaceFeatures = sf;
		missionManager = m;
		personConfig = pc;

		MissionLog.initialise(c);
		MissionUtil.initializeInstances(u, m);
	}
}
