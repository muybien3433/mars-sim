/*
 * Mars Simulation Project
 * Mining.java
 * @date 2021-12-21
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.mars_sim.msp.core.Coordinates;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.UnitType;
import org.mars_sim.msp.core.environment.ExploredLocation;
import org.mars_sim.msp.core.equipment.EquipmentType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.JobType;
import org.mars_sim.msp.core.person.ai.task.CollectMinedMinerals;
import org.mars_sim.msp.core.person.ai.task.EVAOperation;
import org.mars_sim.msp.core.person.ai.task.MineSite;
import org.mars_sim.msp.core.person.ai.task.utils.Task;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsClock;
import org.mars_sim.msp.core.tool.RandomUtil;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.StatusType;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.core.vehicle.VehicleType;

/**
 * Mission for mining mineral concentrations at an explored site.
 */
public class Mining extends RoverMission
	implements SiteMission {

	private static final Set<JobType> PREFERRED_JOBS = Set.of(JobType.AREOLOGIST, JobType.ASTRONOMER, JobType.PILOT);

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static SimLogger logger = SimLogger.getLogger(Mining.class.getName());
	
	/** Default description. */
	private static final String DEFAULT_DESCRIPTION = Msg.getString("Mission.description.mining");
	
	/** Mission phases */
	private static final MissionPhase MINING_SITE = new MissionPhase("Mission.phase.miningSite");

	/** Number of large bags needed for mission. */
	public static final int NUMBER_OF_LARGE_BAGS = 20;

	/** Base amount (kg) of a type of mineral at a site. */
	static final double MINERAL_BASE_AMOUNT = 2500D;

	/** Amount of time(millisols) to spend at the mining site. */
	private static final double MINING_SITE_TIME = 4000D;

	/** Minimum amount (kg) of an excavated mineral that can be collected. */
	private static final double MINIMUM_COLLECT_AMOUNT = 10D;

	/**
	 * The minimum number of mineral concentration estimation improvements for an
	 * exploration site for it to be considered mature enough to mine.
	 */
	public static final int MATURE_ESTIMATE_NUM = 10;

	// Data members
	private boolean endMiningSite;
	
	private ExploredLocation miningSite;
	private LightUtilityVehicle luv;
	
	private Map<AmountResource, Double> excavatedMinerals;
	private Map<AmountResource, Double> totalExcavatedMinerals;

	/**
	 * Constructor
	 * 
	 * @param startingPerson the person starting the mission.
	 * @throws MissionException if error creating mission.
	 */
	public Mining(Person startingPerson) {

		// Use RoverMission constructor.
		super(DEFAULT_DESCRIPTION, MissionType.MINING, startingPerson);
		
		if (!isDone()) {
			// Initialize data members.
			excavatedMinerals = new HashMap<>(1);
			totalExcavatedMinerals = new HashMap<>(1);

			// Recruit additional members to mission.
			if (!recruitMembersForMission(startingPerson, MIN_GOING_MEMBERS))
				return;

			Settlement s = getStartingSettlement();
			
			// Determine mining site.
			try {
				if (hasVehicle()) {
					miningSite = determineBestMiningSite(getRover(), s);
					miningSite.setReserved(true);
					addNavpoint(miningSite.getLocation(), "mining site");
				}
			} catch (Exception e) {
				logger.severe(startingPerson, "Mining site could not be determined.", e);
				endMission(MissionStatus.MINING_SITE_NOT_BE_DETERMINED);
			}

			// Add home settlement
			addNavpoint(new NavPoint(s.getCoordinates(), s, s.getName()));

			// Check if vehicle can carry enough supplies for the mission.
			if (hasVehicle() && !isVehicleLoadable()) {
				endMission(MissionStatus.CANNOT_LOAD_RESOURCES);
			}

			if (!isDone()) {
				// Reserve light utility vehicle.
				luv = reserveLightUtilityVehicle();
				if (luv == null) {
					endMission(MissionStatus.LUV_NOT_AVAILABLE);
				}
			}
		}

		// Set initial mission phase.
		setPhase(REVIEWING, null);
	}

	/**
	 * Constructor with explicit data.
	 * 
	 * @param members            collection of mission members.
	 * @param miningSite         the site to mine.
	 * @param rover              the rover to use.
	 * @param description        the mission's description.
	 */
	public Mining(Collection<MissionMember> members, ExploredLocation miningSite,
			Rover rover, LightUtilityVehicle luv, String description) {

		// Use RoverMission constructor.
		super(description, MissionType.MINING, (MissionMember) members.toArray()[0], rover);
		
		// Initialize data members.
		this.miningSite = miningSite;
		miningSite.setReserved(true);
		excavatedMinerals = new HashMap<>(1);
		totalExcavatedMinerals = new HashMap<>(1);

		addMembers(members, false);

		// Add mining site nav point.
		addNavpoint(miningSite.getLocation(), "mining site");

		// Add home settlement
		Settlement s = getStartingSettlement();
		addNavpoint(s);

		// Check if vehicle can carry enough supplies for the mission.
		if (hasVehicle() && !isVehicleLoadable()) {
			endMission(MissionStatus.CANNOT_LOAD_RESOURCES);
		}

		// Reserve light utility vehicle.
		this.luv = luv;
		if (luv == null) {
			logger.warning("Light utility vehicle not available.");
			endMission(MissionStatus.LUV_NOT_AVAILABLE);
		} else {
			luv.setReservedForMission(true);
		}

		// Set initial mission phase.
		setPhase(EMBARKING, s.getName());
	}

	/**
	 * Checks if a light utility vehicle (LUV) is available for the mission.
	 * 
	 * @param settlement the settlement to check.
	 * @return true if LUV available.
	 */
	public static boolean isLUVAvailable(Settlement settlement) {
		boolean result = false;

		Iterator<Vehicle> i = settlement.getParkedVehicles().iterator();
		while (i.hasNext()) {
			Vehicle vehicle = i.next();
			if (vehicle.getVehicleType() == VehicleType.LUV) {
				boolean usable = !vehicle.isReserved();
                usable = vehicle.isVehicleReady();

				if (((Crewable) vehicle).getCrewNum() > 0 || ((Crewable) vehicle).getRobotCrewNum() > 0)
					usable = false;

				if (usable)
					result = true;

			}
		}

		return result;
	}

	/**
	 * Checks if the required attachment parts are available.
	 * 
	 * @param settlement the settlement to check.
	 * @return true if available attachment parts.
	 */
	public static boolean areAvailableAttachmentParts(Settlement settlement) {
		boolean result = true;

		try {
			if (!settlement.getItemResourceIDs().contains(ItemResourceUtil.pneumaticDrillID)) {
				result = false;
			}
			if (!settlement.getItemResourceIDs().contains(ItemResourceUtil.backhoeID)) {
				result = false;
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error in getting parts.");
		}

		return result;
	}

	@Override
	protected boolean determineNewPhase() {
		boolean handled = true;
		if (!super.determineNewPhase()) {
			if (TRAVELLING.equals(getPhase())) {
				if (getCurrentNavpoint().isSettlementAtNavpoint()) {
					startDisembarkingPhase();
				}
				else {
					setPhase(MINING_SITE, getCurrentNavpoint().getDescription());
				}
			} 
			
			else if (MINING_SITE.equals(getPhase())) {
				startTravellingPhase();
			} 
			else {
				handled = false;
			}
		}
		return handled;
	}

	@Override
	protected void performPhase(MissionMember member) {
		super.performPhase(member);
		if (MINING_SITE.equals(getPhase())) {
			miningPhase(member);
		}
	}


	@Override
	protected void performEmbarkFromSettlementPhase(MissionMember member) {
		super.performEmbarkFromSettlementPhase(member);
		performEmbarkFrom();
	}

	private void performEmbarkFrom() {
		// Attach light utility vehicle for towing.
		if (!isDone() && (getRover().getTowedVehicle() == null)) {

			Settlement settlement = getStartingSettlement();

			getRover().setTowedVehicle(luv);
			luv.setTowingVehicle(getRover());
			settlement.removeParkedVehicle(luv);

			if (!settlement.hasItemResource(ItemResourceUtil.pneumaticDrillID)
					|| !settlement.hasItemResource(ItemResourceUtil.backhoeID)) {
				logger.warning(luv, 
						"Could not load LUV and/or its attachment parts for mission " + getName());
				endMission(MissionStatus.LUV_ATTACHMENT_PARTS_NOT_LOADABLE);
				return;
			}
				
			try {
				// Load light utility vehicle with attachment parts.
				settlement.retrieveItemResource(ItemResourceUtil.pneumaticDrillID, 1);
				luv.storeItemResource(ItemResourceUtil.pneumaticDrillID, 1);

				settlement.retrieveItemResource(ItemResourceUtil.backhoeID, 1);
				luv.storeItemResource(ItemResourceUtil.backhoeID, 1);
			} catch (Exception e) {
				logger.severe(luv, 
						"Problem loading attachments", e);
				endMission(MissionStatus.LUV_ATTACHMENT_PARTS_NOT_LOADABLE);
			}
		}
	}

	@Override
	protected void performDisembarkToSettlementPhase(MissionMember member, Settlement disembarkSettlement) {
		performDisembarkTo();
		super.performDisembarkToSettlementPhase(member, disembarkSettlement);
	}

	protected void performDisembarkTo() {
		// Unload towed light utility vehicle.
		if (!isDone() && (getRover().getTowedVehicle() != null)) {
			try {
				Settlement settlement = getStartingSettlement();
				
				getRover().setTowedVehicle(null);
				luv.setTowingVehicle(null);
				settlement.removeParkedVehicle(luv);
				luv.findNewParkingLoc();

				// Unload attachment parts.
				luv.retrieveItemResource(ItemResourceUtil.pneumaticDrillID, 1);
				settlement.storeItemResource(ItemResourceUtil.pneumaticDrillID, 1);

				luv.retrieveItemResource(ItemResourceUtil.backhoeID, 1);
				settlement.storeItemResource(ItemResourceUtil.backhoeID, 1);
			} catch (Exception e) {
				logger.log(Level.SEVERE, "Error unloading light utility vehicle and attachment parts.");
				endMission(MissionStatus.LUV_ATTACHMENT_PARTS_NOT_LOADABLE);
			}
		}
	}

	/**
	 * Perform the mining phase.
	 * 
	 * @param member the mission member performing the mining phase.
	 * @throws MissionException if error performing the mining phase.
	 */
	private void miningPhase(MissionMember member) {

		// Detach towed light utility vehicle if necessary.
		if (getRover().getTowedVehicle() != null) {
			getRover().setTowedVehicle(null);
			luv.setTowingVehicle(null);
		}

		// Check if crew has been at site for more than three sols.
		boolean timeExpired = getPhaseDuration() >= MINING_SITE_TIME;

        if (isEveryoneInRover()) {

			// Check if end mining flag is set.
			if (endMiningSite) {
				endMiningSite = false;
				setPhaseEnded(true);
			}

			// Check if crew has been at site for more than three sols, then end this phase.
			if (timeExpired) {
				setPhaseEnded(true);
			}

			// Determine if no one can start the mine site or collect resources tasks.
			boolean nobodyMineOrCollect = true;
			Iterator<MissionMember> j = getMembers().iterator();
			while (j.hasNext()) {
				MissionMember tempMember = j.next();
				if (MineSite.canMineSite(tempMember, getRover())) {
					nobodyMineOrCollect = false;
				}
				if (canCollectExcavatedMinerals(tempMember)) {
					nobodyMineOrCollect = false;
				}
			}

			// If no one can mine minerals at the site or is low light level (except in dark polar region)
			// night time, end the mining phase.
			boolean inDarkPolarRegion = surfaceFeatures.inDarkPolarRegion(getCurrentMissionLocation());
			double sunlight = surfaceFeatures.getSolarIrradiance(getCurrentMissionLocation());
			if (nobodyMineOrCollect 
					|| ((sunlight < 12) && !inDarkPolarRegion)) {
				setPhaseEnded(true);
			}

			// Anyone in the crew or a single person at the home settlement has a dangerous
			// illness, end phase.
			if (hasEmergency()) {
				setPhaseEnded(true);
			}

			// Check if enough resources for remaining trip. false = not using margin.
			if (!hasEnoughResourcesForRemainingMission(false)) {
				// If not, determine an emergency destination.
				determineEmergencyDestination(member);
				setPhaseEnded(true);
			}
		} else {
			// If mining time has expired for the site, have everyone end their
			// mining and collection tasks.
			if (timeExpired) {
				Iterator<MissionMember> i = getMembers().iterator();
				while (i.hasNext()) {
					MissionMember tempMember = i.next();
					if (member.getUnitType() == UnitType.PERSON) {
						Person tempPerson = (Person) tempMember;

						Task task = tempPerson.getMind().getTaskManager().getTask();
						if (task instanceof MineSite) {
							((MineSite) task).endEVA();
						}
						if (task instanceof CollectMinedMinerals) {
							((CollectMinedMinerals) task).endEVA();
						}
					}
				}
			}
		}

		if (!getPhaseEnded()) {
			// 75% chance of assigning task, otherwise allow break.
			if (RandomUtil.lessThanRandPercent(75D)
				// If mining is still needed at site, assign tasks.
				&& !endMiningSite && !timeExpired
				// If person can collect minerals the site, start that task.
				&& canCollectExcavatedMinerals(member)
				&& member.getUnitType() == UnitType.PERSON) {
					Person person = (Person) member;
					AmountResource mineralToCollect = getMineralToCollect(person);
					assignTask(person, new CollectMinedMinerals(person, getRover(), mineralToCollect));
			}
		} else {
			// Mark site as mined.
			miningSite.setMined(true);

			// Attach light utility vehicle for towing.
			getRover().setTowedVehicle(luv);
			luv.setTowingVehicle(getRover());
		}
	}


	/**
	 * Checks if a person can collect minerals from the excavation pile.
	 * 
	 * @param member the member collecting.
	 * @return true if can collect minerals.
	 */
	private boolean canCollectExcavatedMinerals(MissionMember member) {
		boolean result = false;

		Iterator<AmountResource> i = excavatedMinerals.keySet().iterator();
		while (i.hasNext()) {
			AmountResource resource = i.next();
			if ((excavatedMinerals.get(resource) >= MINIMUM_COLLECT_AMOUNT)
					&& CollectMinedMinerals.canCollectMinerals(member, getRover(), resource)) {
				result = true;
			}
		}

		return result;
	}

	/**
	 * Gets the mineral resource to collect from the excavation pile.
	 * 
	 * @param person the person collecting.
	 * @return mineral
	 */
	private AmountResource getMineralToCollect(Person person) {
		AmountResource result = null;
		double largestAmount = 0D;

		Iterator<AmountResource> i = excavatedMinerals.keySet().iterator();
		while (i.hasNext()) {
			AmountResource resource = i.next();
			if ((excavatedMinerals.get(resource) >= MINIMUM_COLLECT_AMOUNT)
					&& CollectMinedMinerals.canCollectMinerals(person, getRover(), resource)) {
				double amount = excavatedMinerals.get(resource);
				if (amount > largestAmount) {
					result = resource;
					largestAmount = amount;
				}
			}
		}

		return result;
	}

	/**
	 * Ends mining at a site.
	 */
	@Override
	public void abortPhase() {
		if (MINING_SITE.equals(getPhase())) {

			logger.log(Level.INFO, "Mining site phase ended due to external trigger.");
			endMiningSite = true;

			endAllEVA();
		}
		else
			super.abortPhase();
	}

	/**
	 * Determines the best available mining site.
	 * 
	 * @param rover          the mission rover.
	 * @param homeSettlement the mission home settlement.
	 * @return best explored location for mining, or null if none found.
	 */
	public static ExploredLocation determineBestMiningSite(Rover rover, Settlement homeSettlement) {

		ExploredLocation result = null;
		double bestValue = 0D;

		try {
			double roverRange = rover.getRange(MissionType.MINING);
			double tripTimeLimit = getTotalTripTimeLimit(rover, rover.getCrewCapacity(), true);
			double tripRange = getTripTimeRange(tripTimeLimit, rover.getBaseSpeed() / 2D);
			double range = roverRange;
			if (tripRange < range) {
				range = tripRange;
			}

			Iterator<ExploredLocation> i = surfaceFeatures.getExploredLocations()
					.iterator();
			while (i.hasNext()) {
				ExploredLocation site = i.next();

				boolean isMature = (site.getNumEstimationImprovement() >= MATURE_ESTIMATE_NUM);

				if (!site.isMined() && !site.isReserved() && site.isExplored() && isMature) {
					// Only mine from sites explored from home settlement.
					if (homeSettlement.equals(site.getSettlement())) {
						Coordinates siteLocation = site.getLocation();
						Coordinates homeLocation = homeSettlement.getCoordinates();
						if (Coordinates.computeDistance(homeLocation, siteLocation) <= (range / 2D)) {
							double value = getMiningSiteValue(site, homeSettlement);
							if (value > bestValue) {
								result = site;
								bestValue = value;
							}
						}
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error determining best mining site.");
		}

		return result;
	}

	/**
	 * Gets the estimated mineral value of a mining site.
	 * 
	 * @param site       the mining site.
	 * @param settlement the settlement valuing the minerals.
	 * @return estimated value of the minerals at the site (VP).
	 * @throws MissionException if error determining the value.
	 */
	public static double getMiningSiteValue(ExploredLocation site, Settlement settlement) {

		double result = 0D;

		Map<String, Double> concentrations = site.getEstimatedMineralConcentrations();
		Iterator<String> i = concentrations.keySet().iterator();
		while (i.hasNext()) {
			String mineralType = i.next();
			int mineralResource = ResourceUtil.findIDbyAmountResourceName(mineralType);
			double mineralValue = settlement.getGoodsManager().getGoodValuePerItem(mineralResource);
			double concentration = concentrations.get(mineralType);
			double mineralAmount = (concentration / 100D) * MINERAL_BASE_AMOUNT;
			result += mineralValue * mineralAmount;
		}
			
		result = Math.min(100, result);
		return result;
	}

	@Override
	protected Map<Integer, Integer> getEquipmentNeededForRemainingMission(boolean useBuffer) {
		Map<Integer, Integer> result = new HashMap<>();

		// Include required number of bags.
		result.put(EquipmentType.getResourceID(EquipmentType.LARGE_BAG), NUMBER_OF_LARGE_BAGS);

		return result;
	}

	@Override
	public Settlement getAssociatedSettlement() {
		return getStartingSettlement();
	}

	@Override
	protected double getEstimatedRemainingMissionTime(boolean useBuffer) {
		double result = super.getEstimatedRemainingMissionTime(useBuffer);
		result += getEstimatedRemainingMiningSiteTime();
		return result;
	}

	/**
	 * Gets the estimated time remaining at mining site in the mission.
	 * 
	 * @return time (millisols)
	 */
	private double getEstimatedRemainingMiningSiteTime() {
		double result = 0D;

		// Use estimated remaining mining time at site if still there.
		if (MINING_SITE.equals(getPhase())) {
			double remainingTime = MINING_SITE_TIME - getPhaseDuration();
			if (remainingTime > 0D) {
				result = remainingTime;
			}
		} else {
			// If mission hasn't reached mining site yet, use estimated mining site time.
			result = MINING_SITE_TIME;
		}

		return result;
	}

	@Override
	protected Map<Integer, Number> getResourcesNeededForRemainingMission(boolean useBuffer) {
		Map<Integer, Number> result = super.getResourcesNeededForRemainingMission(useBuffer);

		double miningSiteTime = getEstimatedRemainingMiningSiteTime();
		double timeSols = miningSiteTime / 1000D;

		int crewNum = getPeopleNumber();

		// Determine life support supplies needed for mining activity.
		addLifeSupportResources(result, crewNum, timeSols, useBuffer);
		return result;
	}

	/**
	 * Gets the range of a trip based on its time limit and mining site.
	 * 
	 * @param tripTimeLimit time (millisols) limit of trip.
	 * @param averageSpeed  the average speed of the vehicle.
	 * @return range (km) limit.
	 */
	private static double getTripTimeRange(double tripTimeLimit, double averageSpeed) {
		double tripTimeTravellingLimit = tripTimeLimit - MINING_SITE_TIME;
		double averageSpeedMillisol = averageSpeed / MarsClock.MILLISOLS_PER_HOUR;
		return tripTimeTravellingLimit * averageSpeedMillisol;
	}

	/**
	 * Gets the mission mining site.
	 * 
	 * @return mining site.
	 */
	public ExploredLocation getMiningSite() {
		return miningSite;
	}

	@Override
	protected void endMission(MissionStatus endStatus) {
		super.endMission(endStatus);

		if (miningSite != null) {
			miningSite.setReserved(false);
		}
		if (luv != null) {
			luv.setReservedForMission(false);
		}
	}

	/**
	 * Reserves a light utility vehicle for the mission.
	 * 
	 * @return reserved light utility vehicle or null if none.
	 */
	private LightUtilityVehicle reserveLightUtilityVehicle() {
		LightUtilityVehicle result = null;

		Iterator<Vehicle> i = getStartingSettlement().getParkedVehicles().iterator();
		while (i.hasNext() && (result == null)) {
			Vehicle vehicle = i.next();
			if (vehicle.getVehicleType() == VehicleType.LUV) {
				LightUtilityVehicle luvTemp = (LightUtilityVehicle) vehicle;
				if (((luvTemp.getPrimaryStatus() == StatusType.PARKED) || (luvTemp.getPrimaryStatus() == StatusType.GARAGED))
						&& !luvTemp.isReserved() && (luvTemp.getCrewNum() == 0) && (luvTemp.getRobotCrewNum() == 0)) {
					result = luvTemp;
					luvTemp.setReservedForMission(true);
				}
			}
		}

		return result;
	}

	/**
	 * Gets the mission's light utility vehicle.
	 * 
	 * @return light utility vehicle.
	 */
	public LightUtilityVehicle getLightUtilityVehicle() {
		return luv;
	}

	/**
	 * Gets the amount of a mineral currently excavated.
	 * 
	 * @param mineral the mineral resource.
	 * @return amount (kg)
	 */
	public double getMineralExcavationAmount(AmountResource mineral) {
		return excavatedMinerals.getOrDefault(mineral, 0D);
	}

	/**
	 * Gets the total amount of a mineral that has been excavated so far.
	 * 
	 * @param mineral the mineral resource.
	 * @return amount (kg)
	 */
	public double getTotalMineralExcavatedAmount(AmountResource mineral) {
		return totalExcavatedMinerals.getOrDefault(mineral, 0D);
	}

	/**
	 * Excavates an amount of a mineral.
	 * 
	 * @param mineral the mineral resource.
	 * @param amount  the amount (kg)
	 */
	public void excavateMineral(AmountResource mineral, double amount) {
		double currentExcavated = amount;
		if (excavatedMinerals.containsKey(mineral)) {
			currentExcavated += excavatedMinerals.get(mineral);
		}
		excavatedMinerals.put(mineral, currentExcavated);

		double totalExcavated = amount;
		if (totalExcavatedMinerals.containsKey(mineral)) {
			totalExcavated += totalExcavatedMinerals.get(mineral);
		}
		totalExcavatedMinerals.put(mineral, totalExcavated);

		fireMissionUpdate(MissionEventType.EXCAVATE_MINERALS_EVENT);
	}

	/**
	 * Collects an amount of a mineral.
	 * 
	 * @param mineral the mineral resource.
	 * @param amount  the amount (kg)
	 * @throws Exception if error collecting mineral.
	 */
	public void collectMineral(AmountResource mineral, double amount) {
		double currentExcavated = 0D;
		if (excavatedMinerals.containsKey(mineral)) {
			currentExcavated = excavatedMinerals.get(mineral);
		}
		if (currentExcavated >= amount) {
			excavatedMinerals.put(mineral, (currentExcavated - amount));
		} else {
			throw new IllegalStateException(
					mineral.getName() + " amount: " + amount + " more than currently excavated.");
		}
		fireMissionUpdate(MissionEventType.COLLECT_MINERALS_EVENT);
	}

	@Override
	protected Map<Integer, Number> getSparePartsForTrip(double distance) {
		// Load the standard parts from VehicleMission.
		Map<Integer, Number> result = super.getSparePartsForTrip(distance);

		// Determine repair parts for EVA Suits.
		double evaTime = getEstimatedRemainingMiningSiteTime();
		double numberAccidents = evaTime * getPeopleNumber() * EVAOperation.BASE_ACCIDENT_CHANCE;

		// Assume the average number malfunctions per accident is 1.5.
		double numberMalfunctions = numberAccidents * VehicleMission.AVERAGE_EVA_MALFUNCTION;

		result.putAll(super.getEVASparePartsForTrip(numberMalfunctions));

		return result;
	}
	
	@Override
	protected Set<JobType> getPreferredPersonJobs() {
		return PREFERRED_JOBS;
	}

	@Override
	public void destroy() {
		super.destroy();

		miningSite = null;
		if (excavatedMinerals != null) {
			excavatedMinerals.clear();
		}
		excavatedMinerals = null;
		if (totalExcavatedMinerals != null) {
			totalExcavatedMinerals.clear();
		}
		totalExcavatedMinerals = null;
		luv = null;
	}

	@Override
	public double getTotalSiteScore(Settlement reviewerSettlement) {
		return getMiningSiteValue(miningSite, reviewerSettlement);
	}
}
