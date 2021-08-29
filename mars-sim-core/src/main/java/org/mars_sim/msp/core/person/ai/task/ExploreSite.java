/*
 * Mars Simulation Project
 * ExploreSite.java
 * @date 2021-08-28
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.task;

import java.awt.geom.Point2D;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;

import org.mars_sim.msp.core.Inventory;
import org.mars_sim.msp.core.LocalAreaUtil;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.environment.ExploredLocation;
import org.mars_sim.msp.core.environment.MineralMap;
import org.mars_sim.msp.core.equipment.SpecimenBox;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.SkillType;
import org.mars_sim.msp.core.person.ai.mission.Exploration;
import org.mars_sim.msp.core.person.ai.mission.MissionMember;
import org.mars_sim.msp.core.person.ai.task.utils.TaskPhase;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Rover;

/**
 * A task for the EVA operation of exploring a site.
 */
public class ExploreSite extends EVAOperation implements Serializable {

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(ExploreSite.class.getName());

	/** Task name */
	private static final String NAME = Msg.getString("Task.description.exploreSite"); //$NON-NLS-1$

	/** Task phases. */
	private static final TaskPhase EXPLORING = new TaskPhase(Msg.getString("Task.phase.exploring")); //$NON-NLS-1$

	// Static members
	private static final double AVERAGE_ROCK_SAMPLES_COLLECTED_SITE = 40 + RandomUtil.getRandomDouble(20);
	public static final double AVERAGE_ROCK_SAMPLE_MASS = .5D + RandomUtil.getRandomDouble(.5);
	private static final double ESTIMATE_IMPROVEMENT_FACTOR = 5 + RandomUtil.getRandomDouble(5);

	// Data members
	private double totalCollected = 0;
	private double numSamplesCollected = AVERAGE_ROCK_SAMPLES_COLLECTED_SITE / AVERAGE_ROCK_SAMPLE_MASS;
	private double chance = numSamplesCollected / Exploration.EXPLORING_SITE_TIME;
	
	private ExploredLocation site;
	private Rover rover;
	
	/**
	 * Constructor.
	 * 
	 * @param person the person performing the task.
	 * @param site   the site to explore.
	 * @param rover  the mission rover.
	 * @throws exception if error creating task.
	 */
	public ExploreSite(Person person, ExploredLocation site, Rover rover) {

		// Use EVAOperation parent constructor.
		super(NAME, person, true, RandomUtil.getRandomDouble(50D) + 10D, SkillType.AREOLOGY);

		// Initialize data members.
		this.site = site;
		this.rover = rover;
	
		
		
		// Determine location for field work.
		Point2D exploreLoc = determineExploreLocation();
		setOutsideSiteLocation(exploreLoc.getX(), exploreLoc.getY());

		// Take specimen containers for rock samples.
		if (!hasSpecimenContainer()) {
			takeSpecimenContainer();
		}
		// If specimen containers are not available, end task.
		else {
			logger.warning(person, "No more specimen box for collecting rock samples.");
			endTask();
		}
		
		// Add task phase
		addPhase(EXPLORING);
	}

	/**
	 * Determine location to explore.
	 * 
	 * @return field work X and Y location outside rover.
	 */
	private Point2D determineExploreLocation() {

		Point2D newLocation = null;
		boolean goodLocation = false;
		for (int x = 0; (x < 5) && !goodLocation; x++) {
			for (int y = 0; (y < 10) && !goodLocation; y++) {

				double distance = RandomUtil.getRandomDouble(100D) + (x * 100D) + 50D;
				double radianDirection = RandomUtil.getRandomDouble(Math.PI * 2D);
				double newXLoc = rover.getXLocation() - (distance * Math.sin(radianDirection));
				double newYLoc = rover.getYLocation() + (distance * Math.cos(radianDirection));
				Point2D boundedLocalPoint = new Point2D.Double(newXLoc, newYLoc);

				newLocation = LocalAreaUtil.getLocalRelativeLocation(boundedLocalPoint.getX(), boundedLocalPoint.getY(),
						rover);
				goodLocation = LocalAreaUtil.isLocationCollisionFree(newLocation.getX(), newLocation.getY(),
						person.getCoordinates());
			}
		}

		return newLocation;
	}

	/**
	 * Checks if a person can explore a site.
	 * 
	 * @param member the member
	 * @param rover  the rover
	 * @return true if person can explore a site.
	 */
	public static boolean canExploreSite(MissionMember member, Rover rover) {

		if (member instanceof Person) {
			Person person = (Person) member;

			// Check if person can exit the rover.
			if (!ExitAirlock.canExitAirlock(person, rover.getAirlock()))
				return false;

			if (EVAOperation.isGettingDark(person)) {
				logger.fine(person, "Ended exploring site due to getting dark.");
				return false;
			}

			if (EVAOperation.isHungryAtMealTime(person)) {
				logger.fine(person, "Ended exploring site due to being hungry at meal time.");
				return false;
			}
			
			if (EVAOperation.isExhausted(person)) {
				logger.fine(person, "Ended exploring site due to being exhausted.");
				return false;
			}
			
			if (person.getPhysicalCondition().computeFitnessLevel() < 3)
				return false;
			
			// Check if person's medical condition will not allow task.
            return !(person.getPerformanceRating() < .2D);
		}

		return true;
	}

	@Override
	protected TaskPhase getOutsideSitePhase() {
		return EXPLORING;
	}

	@Override
	protected double performMappedPhase(double time) {

		time = super.performMappedPhase(time);
		if (!isDone()) {
			if (getPhase() == null) {
				throw new IllegalArgumentException("Task phase is null");
			} else if (EXPLORING.equals(getPhase())) {
				time = exploringPhase(time);
			}
		}
		return time;
	}

	/**
	 * Perform the exploring phase of the task.
	 * 
	 * @param time the time available (millisols).
	 * @return remaining time after performing phase (millisols).
	 * @throws Exception if error performing phase.
	 */
	private double exploringPhase(double time) {

		// Check for radiation exposure during the EVA operation.
		if (isDone() || shouldEndEVAOperation() || isRadiationDetected(time)) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return time;
		}
		
		if (person.getPhysicalCondition().computeFitnessLevel() < 3) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return time;
		}
		
		if (totalCollected >= AVERAGE_ROCK_SAMPLES_COLLECTED_SITE) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return time;
		}
		
		int rand = RandomUtil.getRandomInt(1);
		
		if (rand == 0) {
			// Improve mineral concentration estimates.
			improveMineralConcentrationEstimates(time);
		}
		else {
			// Collect rock samples.
			collectRockSamples(time);
		}

		// TODO: Add other site exploration activities later.
	
		// Add experience points
		addExperience(time);

		// Check for an accident during the EVA operation.
		checkForAccident(time);

		// Check if site duration has ended or there is reason to cut the exploring
		// phase short and return to the rover.
		if (addTimeOnSite(time)) {
			if (person.isOutside())
        		setPhase(WALK_BACK_INSIDE);
        	else
        		endTask();
			return 0;
		}
		
		return 0D;
	}

	/**
	 * Collect rock samples if chosen.
	 * 
	 * @param time the amount of time available (millisols).
	 * @throws Exception if error collecting rock samples.
	 */
	private void collectRockSamples(double time) {
		if (hasSpecimenContainer()) {
			double probability = Math.round((1 + site.getNumEstimationImprovement()) * chance * time *100.0)/100.0;
			if (probability > .8)
				probability = .8;
			logger.info(person, 10_000, "collectRockSamples::probability: " + probability);
			
			if (RandomUtil.getRandomDouble(1.0D) <= chance * time) {
				
				int randomRock = ResourceUtil.rockSamplesID;
				
				Inventory pInv = person.getInventory();
		        SpecimenBox box = pInv.findASpecimenBox();
				double mass = RandomUtil.getRandomDouble(AVERAGE_ROCK_SAMPLE_MASS * 2D);
				double cap = box.getAmountResourceRemainingCapacity(randomRock);
				if (mass < cap) {
					double excess = box.storeAmountResource(randomRock, mass);
					totalCollected += mass - excess;
				}
			}
		}
	}

	/**
	 * Improve the mineral concentration estimates of an explored site.
	 * 
	 * @param time the amount of time available (millisols).
	 */
	private void improveMineralConcentrationEstimates(double time) {
		double probability = (time / Exploration.EXPLORING_SITE_TIME) * getEffectiveSkillLevel()
				* ESTIMATE_IMPROVEMENT_FACTOR;
		if (RandomUtil.getRandomDouble(1.0D) <= probability) {
			MineralMap mineralMap = Simulation.instance().getMars().getSurfaceFeatures().getMineralMap();
			Map<String, Double> estimatedMineralConcentrations = site.getEstimatedMineralConcentrations();
			Iterator<String> i = estimatedMineralConcentrations.keySet().iterator();
			while (i.hasNext()) {
				String mineralType = i.next();
				double actualConcentration = mineralMap.getMineralConcentration(mineralType, site.getLocation());
				double estimatedConcentration = estimatedMineralConcentrations.get(mineralType);
				double estimationDiff = Math.abs(actualConcentration - estimatedConcentration);
				double estimationImprovement = RandomUtil.getRandomDouble(1D * getEffectiveSkillLevel());
				if (estimationImprovement > estimationDiff)
					estimationImprovement = estimationDiff;
				if (estimatedConcentration < actualConcentration)
					estimatedConcentration += estimationImprovement;
				else
					estimatedConcentration -= estimationImprovement;
				estimatedMineralConcentrations.put(mineralType, estimatedConcentration);
			}

			// Add to site mineral concentration estimation improvement number.
			site.addEstimationImprovement();
			logger.log(person, Level.INFO, 5_000, 
					"Exploring at site " + site.getLocation().getFormattedString() 
					+ ". Estimation Improvement: "
					+ site.getNumEstimationImprovement() + ".");
		}
	}

	/**
	 * Checks if the person is carrying a specimen container.
	 * 
	 * @return true if carrying container.
	 */
	private boolean hasSpecimenContainer() {
		return person.getInventory().containsUnitClass(SpecimenBox.class);
	}

	/**
	 * Takes the least full specimen container from the rover, if any are available.
	 * 
	 * @throws Exception if error taking container.
	 */
	private void takeSpecimenContainer() {
		Unit container = findLeastFullContainer(rover);
		if (container != null) {
			if (person.getInventory().canStoreUnit(container, false)) {
				container.transfer(rover, person);
			}
		}
	}

	/**
	 * Gets the least full specimen container in the rover.
	 * 
	 * @param rover the rover with the inventory to look in.
	 * @return specimen container or null if none.
	 */
	private static SpecimenBox findLeastFullContainer(Rover rover) {
		SpecimenBox result = null;
		double mostCapacity = 0D;

		Iterator<SpecimenBox> i = rover.getInventory().findAllSpecimenBoxes().iterator();
		while (i.hasNext()) {
			SpecimenBox container = i.next();
			try {
				double remainingCapacity = container.getAmountResourceRemainingCapacity(ResourceUtil.rockSamplesID);

				if (remainingCapacity > mostCapacity) {
					result = container;
					mostCapacity = remainingCapacity;
				}
			} catch (Exception e) {
	          	logger.log(Level.SEVERE, "Problems calling getAmountResourceRemainingCapacity(): "+ e.getMessage());
			}
		}

		return result;
	}

	/**
	 * Transfer the Specimen box to the Vehicle
	 */
	@Override
	protected void clearDown() {
		logger.info(person, 10_000, "clearDown::totalCollected: " + totalCollected);
		// Load specimen container in rover.
		Inventory pInv = person.getInventory();
		if (pInv.containsUnitClass(SpecimenBox.class)) {
			SpecimenBox box = pInv.findASpecimenBox();
			box.transfer(pInv, rover);
		}
	}
}
