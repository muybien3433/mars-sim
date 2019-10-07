/**
 * Mars Simulation Project
 * VehicleChatUtils.java
 * @version 3.1.0 2019-09-03
 * @author Manny Kung
 */

package org.mars.sim.console;

import org.mars_sim.msp.core.person.ai.mission.Mission;
import org.mars_sim.msp.core.person.ai.mission.VehicleMission;
import org.mars_sim.msp.core.resource.ResourceUtil;
import org.mars_sim.msp.core.science.ScienceType;
import org.mars_sim.msp.core.tool.Conversion;
import org.mars_sim.msp.core.vehicle.LightUtilityVehicle;
import org.mars_sim.msp.core.vehicle.Rover;

public class VehicleChatUtils extends ChatUtils {

//	private static Logger logger = Logger.getLogger(VehicleChatUtils.class.getName());

	/**
	 * Asks the vehicle when the input is a string
	 * 
	 * @param text the input string
	 * @param name the input name of the vehicle
	 * @return the response string[]
	 */
	public static String[] askVehicle(String text, String name) {
		
		ChatUtils.personCache = null;
		ChatUtils.robotCache = null;
		ChatUtils.settlementCache = null;
//		ChatUtils.vehicleCache = null;
		
		String questionText = "";
		StringBuffer responseText = new StringBuffer();

		if (text.toLowerCase().contains("where")) {
			questionText = YOU_PROMPT + "Where are you located ?"; 
			responseText.append("I'm located at ");
			responseText.append(Conversion.capitalize(vehicleCache.getLocationTag().getQuickLocation()));
		}
		
		else if (text.equalsIgnoreCase("status")) {
			questionText = YOU_PROMPT + "What is your status ?";
			int max = 28;

			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Status : ", max) + vehicleCache.getStatus().getName());

			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Associated Settlement : ", max)
					+ vehicleCache.getAssociatedSettlement().getName());

			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Location : ", max) + vehicleCache.getImmediateLocation());

			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Locale : ", max) + vehicleCache.getLocale());

			String reserve = "Yes";
			if (!vehicleCache.isReservedForMission())
				reserve = "No";
			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Reserved : ", max) + reserve);

			String missionStr = "None";
			if (missionManager.getMissionForVehicle(vehicleCache) != null) {
				Mission m = missionManager.getMissionForVehicle(vehicleCache);
				String lead = m.getStartingMember().getName();
				missionStr = "Yes. " + m.getName();
				responseText.append(System.lineSeparator());
				responseText.append(addWhiteSpacesRightName("On a Mission : ", max) + missionStr);
				responseText.append(System.lineSeparator());
				responseText.append(addWhiteSpacesRightName("Mission Lead : ", max) + lead);
				responseText.append(System.lineSeparator());

				double dist = 0;
				double trav = 0;

				if (m instanceof VehicleMission) {
					dist = Math.round(((VehicleMission) m).getProposedRouteTotalDistance() * 10.0) / 10.0;
					trav = Math.round(((VehicleMission) m).getActualTotalDistanceTravelled() * 10.0) / 10.0;
					responseText.append(addWhiteSpacesRightName("Proposed Dist. : ", max) + (dist + " km"));
					responseText.append(System.lineSeparator());
					responseText.append(addWhiteSpacesRightName("Travelled : ", max) + (trav + " km"));
					responseText.append(System.lineSeparator());
				}

			} else {
				responseText.append(System.lineSeparator());
				responseText.append(addWhiteSpacesRightName("Not on a Mission", max));
			}

			if (vehicleCache instanceof Rover) {
				String towed = "No";
				if (vehicleCache.isBeingTowed() && vehicleCache.getTowingVehicle().getName() != null) {
					towed = "Yes. Towed by " + vehicleCache.getTowingVehicle().getName();
				}
				responseText.append(System.lineSeparator());
				responseText.append(addWhiteSpacesRightName("Being Towed : ", max) + towed);

				String towing = "No";
				if (((Rover) vehicleCache).isTowingAVehicle()
						&& ((Rover) vehicleCache).getTowedVehicle().getName() != null) {
					towing = "Yes. Towing " + ((Rover) vehicleCache).getTowedVehicle().getName();
				}
				responseText.append(System.lineSeparator());
				responseText.append(addWhiteSpacesRightName("Towing : ", max) + towing);
			}

		}

		else if (text.equalsIgnoreCase("specs")) {
			questionText = YOU_PROMPT + "Can you show me the specifications ?";

			int max = 28;
			responseText.append(System.lineSeparator());
//			responseText.append(SYSTEM_PROMPT);
//			responseText.append("Specifications :");
			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Name : ", max) + vehicleCache.getName());
			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Type : ", max) + vehicleCache.getVehicleType());
			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Description : ", max) + vehicleCache.getVehicleType());
			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Base Mass : ", max)).append(vehicleCache.getBaseMass())
					.append(" kg");
			responseText.append(System.lineSeparator());

//			System.out.println("next is speed : " + responseText.toString());

			responseText.append(addWhiteSpacesRightName("Base Speed : ", max)).append(vehicleCache.getBaseSpeed())
					.append(" km/h");
			responseText.append(System.lineSeparator());
			responseText.append(addWhiteSpacesRightName("Drivetrain Efficiency : ", max))
					.append(vehicleCache.getDrivetrainEfficiency()).append(" kWh/km");
//			responseText.append(System.lineSeparator());
//			responseText.append(addhiteSpacesName("Travel per sol : ", max) + vehicleCache.getEstimatedTravelDistancePerSol() + " km (Estimated)");
			responseText.append(System.lineSeparator());

//			System.out.println("next is SOFC : " + responseText.toString());

			String fuel = "Electrical Battery";
			if (vehicleCache instanceof Rover) {
				int id = ((Rover) vehicleCache).getFuelType();
				String fuelName = ResourceUtil.findAmountResourceName(id);
				fuel = Conversion.capitalize(fuelName) + " (Solid Oxide Fuel Cell)";

				responseText.append(addWhiteSpacesRightName("Power Source : ", max) + fuel);
				responseText.append(System.lineSeparator());

				responseText.append(addWhiteSpacesRightName("Fuel Capacity : ", max))
						.append(vehicleCache.getFuelCapacity() + " kg");
				responseText.append(System.lineSeparator());

				responseText.append(addWhiteSpacesRightName("Base Range : ", max))
						.append(Math.round(vehicleCache.getBaseRange() * 100.0) / 100.0 + " km (Estimated)");
				responseText.append(System.lineSeparator());

				responseText.append(addWhiteSpacesRightName("Base Fuel Consumption : ", max)).append(
						Math.round(vehicleCache.getBaseFuelConsumption() * 100.0) / 100.0 + " km/kg (Estimated)");
				responseText.append(System.lineSeparator());
			} else {
				responseText.append(addWhiteSpacesRightName("Power Source : ", max) + fuel);
				responseText.append(System.lineSeparator());
			}

//			System.out.println("next is getCrewCapacity : " + responseText.toString());

			int crewSize = 0;
			if (vehicleCache instanceof Rover) {
				crewSize = ((Rover) vehicleCache).getCrewCapacity();
			} else if (vehicleCache instanceof LightUtilityVehicle) {
				crewSize = ((LightUtilityVehicle) vehicleCache).getCrewCapacity();
			}
			responseText.append(addWhiteSpacesRightName("Crew Size : ", max)).append(crewSize);
			responseText.append(System.lineSeparator());

			double cargo = 0;
			if (vehicleCache instanceof Rover) {
				cargo = ((Rover) vehicleCache).getCargoCapacity();
				responseText.append(addWhiteSpacesRightName("Cargo Capacity : ", max)).append(cargo + " kg");
				responseText.append(System.lineSeparator());
			}

//			System.out.println("next is sick bay : " + responseText.toString());				

			String hasSickBayStr = "No";
			if (vehicleCache instanceof Rover) {

				boolean hasSickBay = ((Rover) vehicleCache).hasSickBay();
				if (hasSickBay) {
					hasSickBayStr = "Yes";

					responseText.append(addWhiteSpacesRightName("Has Sick Bay : ", max) + hasSickBayStr);
					responseText.append(System.lineSeparator());

					int bed = ((Rover) vehicleCache).getSickBay().getSickBedNum();
					responseText.append(addWhiteSpacesRightName("# Beds (Sick Bay) : ", max)).append(bed);
					responseText.append(System.lineSeparator());

					int lvl = ((Rover) vehicleCache).getSickBay().getTreatmentLevel();
					responseText.append(addWhiteSpacesRightName("Tech Level (Sick Bay) : ", max)).append(lvl);
					responseText.append(System.lineSeparator());

				} else {
					responseText.append(addWhiteSpacesRightName("Has Sick Bay : ", max) + hasSickBayStr);
					responseText.append(System.lineSeparator());
				}

			}

//			System.out.println("next is lab : " + responseText.toString());

			String hasLabStr = "No";
			if (vehicleCache instanceof Rover) {

				boolean hasLab = ((Rover) vehicleCache).hasLab();
				if (hasLab) {
					hasLabStr = "Yes";

					responseText.append(addWhiteSpacesRightName("Has Lab : ", max) + hasLabStr);
					responseText.append(System.lineSeparator());

					int lvl = ((Rover) vehicleCache).getLab().getTechnologyLevel();
					responseText.append(addWhiteSpacesRightName("Tech Level (Lab) : ", max)).append(lvl);
					responseText.append(System.lineSeparator());

					int size = ((Rover) vehicleCache).getLab().getLaboratorySize();
					responseText.append(addWhiteSpacesRightName("Lab Size : ", max)).append(size);
					responseText.append(System.lineSeparator());

					ScienceType[] types = ((Rover) vehicleCache).getLab().getTechSpecialties();
					String names = "";
					for (ScienceType t : types) {
						names += t.getName() + ", ";
					}
					names = names.substring(0, names.length() - 2);

					responseText.append(addWhiteSpacesRightName("Lab Specialties : ", max) + names);
					responseText.append(System.lineSeparator());

				} else {
					responseText.append(addWhiteSpacesRightName("Has Lab: ", max) + hasSickBayStr);
					responseText.append(System.lineSeparator());
				}

			}

//			if (vehicleCache instanceof LightUtilityVehicle) {
//
//				Collection<Part> parts = ((LightUtilityVehicle)vehicleCache).getPossibleAttachmentParts();
//
//				String partNames = "";
//				for (Part p: parts) {
//					partNames += p.getName() + ", ";
//				}
//				partNames = partNames.substring(0, partNames.length()-2);
//				
//				System.out.println("[partNames" + partNames + "]");
//				
//				responseText.append(addhiteSpacesName("Attachment Parts : ", max) + partNames);
//				responseText.append(System.lineSeparator());
//					
//			}

		} else if (text.equalsIgnoreCase("key") || text.equalsIgnoreCase("keys") || text.equalsIgnoreCase("keyword")
				|| text.equalsIgnoreCase("keywords") || text.equalsIgnoreCase("/k")) {

//			help = true;
			questionText = REQUEST_KEYS;
			if (connectionMode == 0) {
				keywordText = VEHICLE_KEYWORDS;
			} else {
				keywordText = VEHICLE_KEYWORDS + KEYWORDS_HEIGHT;
			}
			// responseText.append(System.lineSeparator());
			responseText.append(keywordText);

		} else if (text.equalsIgnoreCase("help") || text.equalsIgnoreCase("/h") || text.equalsIgnoreCase("/?")
				|| text.equalsIgnoreCase("?")) {

//			help = true;
			questionText = REQUEST_HELP;
			if (connectionMode == 0) {
				helpText = HELP_TEXT;
			} else {
				helpText = HELP_TEXT + HELP_HEIGHT;
			}
			// responseText.append(System.lineSeparator());
			responseText.append(helpText);

		} else {
			String[] txt = clarify(name, text);
			questionText = txt[0];
			responseText.append(txt[1]);
		}

		return new String[] { questionText, responseText.toString() };
	}

}