/*
 * Mars Simulation Project
 * Mining.java
 * @date 2023-06-30
 * @author Scott Davis
 */
package org.mars_sim.msp.core.person.ai.mission;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import org.mars.sim.tools.util.RandomUtil;
import org.mars_sim.msp.core.environment.ExploredLocation;
import org.mars_sim.msp.core.equipment.EquipmentType;
import org.mars_sim.msp.core.logging.SimLogger;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.job.util.JobType;
import org.mars_sim.msp.core.person.ai.task.CollectMinedMinerals;
import org.mars_sim.msp.core.person.ai.task.MineSite;
import org.mars_sim.msp.core.person.ai.task.util.Worker;
import org.mars_sim.msp.core.resource.AmountResource;
import org.mars_sim.msp.core.resource.ItemResourceUtil;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.structure.ObjectiveType;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.time.MarsTime;
import org.mars_sim.msp.core.vehicle.Crewable;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Rover;
import org.mars_sim.msp.core.vehicle.StatusType;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.core.vehicle.VehicleType;

/**
 * Mission for mining mineral concentrations at an explored site.
 */
public class Mining extends EVAMission
	implements SiteMission {

	private static final Set<JobType> PREFERRED_JOBS = Set.of(JobType.AREOLOGIST, JobType.ASTRONOMER, JobType.PILOT);

	/** default serial id. */
	private static final long serialVersionUID = 1L;

	/** default logger. */
	private static final SimLogger logger = SimLogger.getLogger(Mining.class.getName());
	
	/** Mission phases */
	private static final MissionPhase MINING_SITE = new MissionPhase("Mission.phase.miningSite");
	private static final MissionStatus MINING_SITE_NOT_BE_DETERMINED = new MissionStatus("Mission.status.miningSite");
	private static final MissionStatus LUV_NOT_AVAILABLE = new MissionStatus("Mission.status.noLUV");
	private static final MissionStatus LUV_ATTACHMENT_PARTS_NOT_LOADABLE = new MissionStatus("Mission.status.noLUVAttachments");

	private static final int MAX = 3000;
	
	/** Number of large bags needed for mission. */
	public static final int NUMBER_OF_LARGE_BAGS = 20;

	/** The good value factor of a site. */
	static final double MINERAL_GOOD_VALUE_FACTOR = 500;
	
	/** The averge good value of a site. */
	static final double AVERAGE_RESERVE_GOOD_VALUE = 50_000;

	/** Amount of time(millisols) to spend at the mining site. */
	private static final double MINING_SITE_TIME = 4000D;

	/** Minimum amount (kg) of an excavated mineral that can be collected. */
	private static final double MINIMUM_COLLECT_AMOUNT = .01;


	/**
	 * The minimum number of mineral concentration estimation improvements for an
	 * exploration site for it to be considered mature enough to mine.
	 */
	public static final int MATURE_ESTIMATE_NUM = 50;

	private static final Set<ObjectiveType> OBJECTIVES = Set.of(ObjectiveType.BUILDERS_HAVEN, ObjectiveType.MANUFACTURING_DEPOT);

	
	private ExploredLocation miningSite;
	private LightUtilityVehicle luv;
	
	private Map<AmountResource, Double> detectedMinerals;
	private Map<AmountResource, Double> totalExcavatedMinerals;

	/**
	 * Constructor
	 * 
	 * @param startingPerson the person starting the mission.
	 * @throws MissionException if error creating mission.
	 */
	public Mining(Person startingPerson, boolean needsReview) {

		// Use RoverMission constructor.
		super(MissionType.MINING, startingPerson, null, MINING_SITE);
		setIgnoreSunlight(true);

		if (!isDone()) {
			// Initialize data members.
			detectedMinerals = new HashMap<>(1);
			totalExcavatedMinerals = new HashMap<>(1);
			setEVAEquipment(EquipmentType.LARGE_BAG, NUMBER_OF_LARGE_BAGS);

			// Recruit additional members to mission.
			if (!recruitMembersForMission(startingPerson, MIN_GOING_MEMBERS))
				return;

			Settlement s = getStartingSettlement();
			
			// Determine mining site.
			if (hasVehicle()) {
				miningSite = determineBestMiningSite(getRover(), s);
				if (miningSite == null) {
					logger.severe(startingPerson, "Mining site could not be determined.");
					endMission(MINING_SITE_NOT_BE_DETERMINED);
					return;
				}
				miningSite.setReserved(true);

				addNavpoint(miningSite.getLocation(), "mining site");
				
				setupDetectedMinerals();
			}

			// Add home settlement
			addNavpoint(s);

			// Check if vehicle can carry enough supplies for the mission.
			if (hasVehicle() && !isVehicleLoadable()) {
				endMission(CANNOT_LOAD_RESOURCES);
			}

			if (!isDone()) {
				// Reserve light utility vehicle.
				luv = reserveLightUtilityVehicle();
				if (luv == null) {
					endMission(LUV_NOT_AVAILABLE);
					return;
				}
				setInitialPhase(needsReview);
			}
		}
	}

	/**
	 * Constructor with explicit data.
	 * 
	 * @param members            collection of mission members.
	 * @param miningSite         the site to mine.
	 * @param rover              the rover to use.
	 * @param description        the mission's description.
	 */
	public Mining(Collection<Worker> members, ExploredLocation miningSite,
			Rover rover, LightUtilityVehicle luv) {

		// Use RoverMission constructor.,  
		super(MissionType.MINING, (Worker) members.toArray()[0], rover, MINING_SITE);
		setIgnoreSunlight(true);

		// Initialize data members.
		this.miningSite = miningSite;
		miningSite.setReserved(true);
		detectedMinerals = new HashMap<>(1);
		totalExcavatedMinerals = new HashMap<>(1);
		setEVAEquipment(EquipmentType.LARGE_BAG, NUMBER_OF_LARGE_BAGS);

		addMembers(members, false);

		// Add mining site nav point.
		addNavpoint(miningSite.getLocation(), "mining site");

		setupDetectedMinerals();
		
		// Add home settlement
		Settlement s = getStartingSettlement();
		addNavpoint(s);

		// Check if vehicle can carry enough supplies for the mission.
		if (hasVehicle() && !isVehicleLoadable()) {
			endMission(CANNOT_LOAD_RESOURCES);
		}

		// Reserve light utility vehicle.
		this.luv = luv;
		if (luv == null) {
			logger.warning("Light utility vehicle not available.");
			endMission(LUV_NOT_AVAILABLE);
		} else {
			claimVehicle(luv);
		}

		// Set initial mission phase.
		setInitialPhase(false);

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
	protected void performDepartingFromSettlementPhase(Worker member) {
		super.performDepartingFromSettlementPhase(member);
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
				endMission(LUV_ATTACHMENT_PARTS_NOT_LOADABLE);
				return;
			}
			
			// Load light utility vehicle with attachment parts.
			settlement.retrieveItemResource(ItemResourceUtil.pneumaticDrillID, 1);
			luv.storeItemResource(ItemResourceUtil.pneumaticDrillID, 1);

			settlement.retrieveItemResource(ItemResourceUtil.backhoeID, 1);
			luv.storeItemResource(ItemResourceUtil.backhoeID, 1);
		}
	}

	@Override
	protected void performDisembarkToSettlementPhase(Worker member, Settlement disembarkSettlement) {
		// Disconnect the LUV
		disengageLUV();
		
		super.performDisembarkToSettlementPhase(member, disembarkSettlement);
	}

	/**
	 * Disconnects the LUV and return the attachment parts prior to disembarking.
	 */
	protected void disengageLUV() {
		// Unload towed light utility vehicle.
		if (!isDone() && (getRover().getTowedVehicle() != null)) {
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
		}
	}

	/**
	 * Perform the EVA
	 */
	@Override
	protected boolean performEVA(Person person) {
		// Detach towed light utility vehicle if necessary.
		if (getRover().getTowedVehicle() != null) {
			getRover().setTowedVehicle(null);
			luv.setTowingVehicle(null);
		}

		// Determine if no one can start the mine site or collect resources tasks.
		boolean nobodyMineOrCollect = true;
		for(Worker tempMember : getMembers()) {
			if (MineSite.canMineSite(tempMember, getRover())) {
				nobodyMineOrCollect = false;
			}
			if (canCollectExcavatedMinerals(tempMember)) {
				nobodyMineOrCollect = false;
			}
		}

		// Nobody can do anything so stop
		if (nobodyMineOrCollect) {
			logger.warning(getRover(), "No one can mine sites in " + getName() + ".");
			return false;
		}

		if (canCollectExcavatedMinerals(person)) {
			AmountResource mineralToCollect = getMineralToCollect(person);
			assignTask(person, new CollectMinedMinerals(person, getRover(), mineralToCollect));
		}
		else {
			assignTask(person, new MineSite(person, miningSite.getLocation(), getRover(), luv));
		}

		return true;
	}


	/**
	 * Closes down the mining activities
	 */
	@Override
	protected void endEVATasks() {
		super.endEVATasks();
			
		double remainingMass = miningSite.getRemainingMass();
		if (remainingMass < 100)
			// Mark site as mined.
			miningSite.setMinable(false);

		// Attach light utility vehicle for towing.
		Rover rover = getRover();
		if (!luv.equals(rover.getTowedVehicle())) {
			rover.setTowedVehicle(luv);
			luv.setTowingVehicle(rover);
		}
	}

	private void setupDetectedMinerals() {
		Map<String, Double> concs = miningSite.getEstimatedMineralConcentrations();
		double remainingMass = miningSite.getRemainingMass();

		Iterator<String> i = concs.keySet().iterator();
		while (i.hasNext()) {
			String name = i.next();
			AmountResource resource = ResourceUtil.findAmountResource(name);
			double percent = concs.get(name);
			detectedMinerals.put(resource, remainingMass * percent / 100);
			
			logger.info(getName() + " detected " + Math.round(remainingMass * 100.0)/100.0 + " kg " + resource.getName());
		}
	}
	
	/**
	 * Checks if a person can collect minerals from the excavation pile.
	 * 
	 * @param member the member collecting.
	 * @return true if can collect minerals.
	 */
	private boolean canCollectExcavatedMinerals(Worker member) {
		boolean result = false;

		Iterator<AmountResource> i = detectedMinerals.keySet().iterator();
		while (i.hasNext()) {
			AmountResource resource = i.next();
			if ((detectedMinerals.get(resource) >= MINIMUM_COLLECT_AMOUNT)
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

		Iterator<AmountResource> i = detectedMinerals.keySet().iterator();
		while (i.hasNext()) {
			AmountResource resource = i.next();
			if ((detectedMinerals.get(resource) >= MINIMUM_COLLECT_AMOUNT)
					&& CollectMinedMinerals.canCollectMinerals(person, getRover(), resource)) {
				double amount = detectedMinerals.get(resource);
				if (amount > largestAmount) {
					result = resource;
					largestAmount = amount;
				}
			}
		}

		return result;
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
			double roverRange = rover.getRange();
			double tripTimeLimit = rover.getTotalTripTimeLimit(true);
			double tripRange = getTripTimeRange(tripTimeLimit, rover.getBaseSpeed() / 2D);
			double range = roverRange;
			if (tripRange < range) {
				range = tripRange;
			}

			for(ExploredLocation site : surfaceFeatures.getAllRegionOfInterestLocations()) {
				boolean isMature = (site.getNumEstimationImprovement() >= 
						RandomUtil.getRandomDouble(MATURE_ESTIMATE_NUM/2, MATURE_ESTIMATE_NUM));

				if (site.isMinable() && site.isClaimed() && !site.isReserved() && site.isExplored() && isMature
					// Only mine from sites explored from home settlement.
					&& (site.getSettlement() == null || homeSettlement.equals(site.getSettlement()))
					&& homeSettlement.getCoordinates().getDistance(site.getLocation()) <= range) {
						double value = getMiningSiteValue(site, homeSettlement);
						if (value > bestValue) {
							result = site;
							bestValue = value;
					}
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error determining best mining site.");
		}

		return result;
	}

	/**
	 * Determines the total mature mining sites score.
	 * 
	 * @param rover          the mission rover.
	 * @param homeSettlement the mission home settlement.
	 * @return the total score
	 */
	public static double getMatureMiningSitesTotalScore(Rover rover, Settlement homeSettlement) {

		double total = 0;

		try {
			double roverRange = rover.getRange();
			double tripTimeLimit = rover.getTotalTripTimeLimit(true);
			double tripRange = getTripTimeRange(tripTimeLimit, rover.getBaseSpeed() / 2D);
			double range = roverRange;
			if (tripRange < range) {
				range = tripRange;
			}

			for (ExploredLocation site : surfaceFeatures.getAllRegionOfInterestLocations()) {
				boolean isMature = (site.getNumEstimationImprovement() >= 
						RandomUtil.getRandomDouble(MATURE_ESTIMATE_NUM/2, MATURE_ESTIMATE_NUM));
				if (site.isMinable() && site.isClaimed() && !site.isReserved() && site.isExplored() && isMature
					// Only mine from sites explored from home settlement.
					&& (site.getSettlement() == null || homeSettlement.equals(site.getSettlement()))
					&& homeSettlement.getCoordinates().getDistance(site.getLocation()) <= range) {
						double value = getMiningSiteValue(site, homeSettlement);
						total += value;
				}
			}
		} catch (Exception e) {
			logger.log(Level.SEVERE, "Error determining best mining site.");
		}

		return total;
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

		for (Map.Entry<String, Double> conc : site.getEstimatedMineralConcentrations().entrySet()) {
			int mineralResource = ResourceUtil.findIDbyAmountResourceName(conc.getKey());
			double mineralValue = settlement.getGoodsManager().getGoodValuePoint(mineralResource);
			double reserve = site.getRemainingMass();
			double mineralAmount = (conc.getValue() / 100) * reserve / AVERAGE_RESERVE_GOOD_VALUE * MINERAL_GOOD_VALUE_FACTOR;
			result += mineralValue * mineralAmount;
		}

		result = Math.min(MAX, result);
		
		logger.info(settlement, 30_000L, site.getLocation() 
			+ " has a Mining Value of " + Math.round(result * 100.0)/100.0 + ".");
		
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
		double averageSpeedMillisol = averageSpeed / MarsTime.MILLISOLS_PER_HOUR;
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
			releaseVehicle(luv);
		}
	}

	/**
	 * Reserves a light utility vehicle for the mission.
	 * 
	 * @return reserved light utility vehicle or null if none.
	 */
	private LightUtilityVehicle reserveLightUtilityVehicle() {
		for(Vehicle vehicle : getStartingSettlement().getParkedVehicles()) {
			if (vehicle.getVehicleType() == VehicleType.LUV) {
				LightUtilityVehicle luvTemp = (LightUtilityVehicle) vehicle;
				if (((luvTemp.getPrimaryStatus() == StatusType.PARKED) || (luvTemp.getPrimaryStatus() == StatusType.GARAGED))
						&& !luvTemp.isReserved() && (luvTemp.getCrewNum() == 0) && (luvTemp.getRobotCrewNum() == 0)) {
					claimVehicle(luvTemp);
					return luvTemp;
				}
			}
		}

		return null;
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
		return detectedMinerals.getOrDefault(mineral, 0D);
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
		if (detectedMinerals.containsKey(mineral)) {
			currentExcavated += detectedMinerals.get(mineral);
		}
		detectedMinerals.put(mineral, currentExcavated);

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
		if (detectedMinerals.containsKey(mineral)) {
			currentExcavated = detectedMinerals.get(mineral);
		}
		if (currentExcavated >= amount) {
			// Record the excavated amount
			detectedMinerals.put(mineral, (currentExcavated - amount));
			// Reduce the mass at the site
			getMiningSite().excavateMass((currentExcavated - amount));	
		
		} else {
			throw new IllegalStateException(
					mineral.getName() + " amount: " + amount + " more than currently excavated.");
		}
		fireMissionUpdate(MissionEventType.COLLECT_MINERALS_EVENT);
	}

	@Override
	protected Set<JobType> getPreferredPersonJobs() {
		return PREFERRED_JOBS;
	}

	@Override
	public Set<ObjectiveType> getObjectiveSatisified() {
		return OBJECTIVES;
	}
	
	@Override
	public double getTotalSiteScore(Settlement reviewerSettlement) {
		return getMiningSiteValue(miningSite, reviewerSettlement);
	}

	@Override
	protected double getEstimatedTimeAtEVASite(boolean buffer) {
		return MINING_SITE_TIME;
	}
}
