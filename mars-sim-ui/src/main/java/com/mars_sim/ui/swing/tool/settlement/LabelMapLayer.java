/**
 * Mars Simulation Project
 * LabelMapLayer.java
 * @date 2022-07-01
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.tool.settlement;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.mars_sim.core.person.GenderType;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.mission.Mission;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.construction.ConstructionSite;
import com.mars_sim.core.structure.construction.ConstructionStage;
import com.mars_sim.core.vehicle.Vehicle;
import com.mars_sim.core.vehicle.VehicleType;
import com.mars_sim.mapdata.location.LocalPosition;
import com.mars_sim.tools.Msg;

/**
 * A settlement map layer for displaying labels for map objects.
 */
public class LabelMapLayer
implements SettlementMapLayer {
 
	// Static members
	private final Color HALLWAY_LABEL_COLOR = Color.gray;; //Color.blue;//new Color (79, 108, 44); // dull sage green
	private final Color BUILDING_LABEL_COLOR = Color.gray.darker(); // Color(0, 0, 255);; //dark bright blue //Color.blue;//new Color (79, 108, 44); // dull sage green

	private final Color BLACK_LABEL_OUTLINE_COLOR = new Color(0, 0, 0, 190); //new Color(0, 0, 0, 150);
	private final Color WHITE_LABEL_OUTLINE_COLOR = new Color(255, 255, 255, 190);

	private final Color SHOP_LABEL_COLOR = new Color (195, 176, 145); // khaki: 146, 112, 255; // pale purple
	private final Color LAB_LABEL_COLOR = new Color (51, 102, 153); // dull dark blue: 51, 102, 153 //sky magenta; 40, 54, 95); // navy blue
	private final Color HAB_LABEL_COLOR = new Color (255, 102, 102); // Very light red: 255, 102, 102 // Maroon: 184, 134, 11 //147, 197, 114 // pistachio: 48, 213, 200; turquoise: 244, 164, 96; sandy brown: 92, 23, 0; // BURGUNDY
	private final Color REACTOR_LABEL_COLOR = new Color (174, 198, 207); // pastel blue: 100, 60, 60; // pale red: Color.red.darker(); // red
	private final Color GARAGE_LABEL_COLOR = Color.yellow;//new Color (255, 222, 122); // pale yellow
	private final Color GREENHOUSE_LABEL_COLOR = new Color (133, 187, 101);// pale green; // 62, 180, 137); // mint; (153, 234, 37) is bright green; (79, 108, 44) is dull sage green //(69, 92, 0) is dark sage //  // new Color(0, 255, 64); //bright green;//
	private final Color MEDICAL_LABEL_COLOR = new Color (51, 204, 255); // very light blue: 51, 204, 255 // dull dark blue : 0, 69, 92
	private final Color LIVING_LABEL_COLOR = new Color (236, 121, 154); // salmon: 236, 121, 154 // pastel orange: 255, 179, 71 //  harvest gold: 225, 179, 120:
	private final Color RESOURCE_LABEL_COLOR = new Color (182, 201, 255); // pale blue
	private final Color EVA_LABEL_COLOR = new Color (236, 255, 179); // pale yellow
	private final Color ERV_LABEL_COLOR = new Color (83, 83, 83); // pale grey
	
	private final Color CONSTRUCTION_SITE_LABEL_COLOR = new Color(237, 114, 38); //greyish orange
	private final Color CONSTRUCTION_SITE_LABEL_OUTLINE_COLOR = new Color(0, 0, 0, 150);

	private final Color VEHICLE_LABEL_COLOR = Color.YELLOW.darker(); // new Color(249, 134, 134); // light-red //127, 0, 127); // magenta-purple
	private final Color VEHICLE_LABEL_OUTLINE_COLOR = new Color(0, 0, 0, 150);//(255, 255, 255, 190);

	// (159, 7, 118) pinkish red purle
	// (255, 153, 225) light pink
	static final Color FEMALE_COLOR = Color.MAGENTA.brighter();
	static final Color FEMALE_OUTLINE_COLOR = new Color(255, 153, 225);
	static final Color FEMALE_SELECTED_COLOR = Color.MAGENTA.darker();
	static final Color FEMALE_SELECTED_OUTLINE_COLOR = FEMALE_COLOR.darker();
	
	// (154, 204, 255) pale light blue
	// ((210, 210, 210) light grey
	static final Color MALE_COLOR = new Color(154, 204, 255);
	static final Color MALE_OUTLINE_COLOR = MALE_COLOR.darker();
	static final Color MALE_SELECTED_COLOR = Color.cyan; 
	static final Color MALE_SELECTED_OUTLINE_COLOR = Color.BLUE; 

	
	// 186, 129, 145 manila pink
	static final Color ROBOT_COLOR = new Color(156, 126, 9); 
	static final Color ROBOT_OUTLINE_COLOR = Color.ORANGE; 
	static final Color ROBOT_SELECTED_COLOR = new Color(186, 129, 145);
	static final Color ROBOT_SELECTED_OUTLINE_COLOR = ROBOT_SELECTED_COLOR.darker();

	// Data members
	private SettlementMapPanel mapPanel;
	
	private Map<String, BufferedImage> labelImageCache;

	/**
	 * Constructor.
	 * @param mapPanel the settlement map panel.
	 */
	public LabelMapLayer(SettlementMapPanel mapPanel) {
		// Initialize data members.
		this.mapPanel = mapPanel;
		labelImageCache = new HashMap<String, BufferedImage>();
	}

	@Override
	public void displayLayer(
		Graphics2D g2d, Settlement settlement, Building building,
		double xPos, double yPos, int mapWidth, int mapHeight,
		double rotation, double scale
	) {

		// Save original graphics transforms.
		AffineTransform saveTransform = g2d.getTransform();

		// Get the map center point.
		double mapCenterX = mapWidth / 2D;
		double mapCenterY = mapHeight / 2D;

		// Translate map from settlement center point.
		g2d.translate(mapCenterX + (xPos * scale), mapCenterY + (yPos * scale));

		// Rotate map from North.
		g2d.rotate(rotation, 0D - (xPos * scale), 0D - (yPos * scale));

		// Draw all building labels.
		if (mapPanel.isShowBuildingLabels()) {
			//mapPanel.getSettlementTransparentPanel().getBuildingLabelMenuItem().setState(true);
			drawBuildingLabels(g2d, settlement);
		}

		// Draw all construction site labels.
		if (mapPanel.isShowConstructionLabels()) {
			drawConstructionSiteLabels(g2d, settlement);
		}

		// Draw all vehicle labels.
		if (mapPanel.isShowVehicleLabels()) {
			drawVehicleLabels(g2d, settlement);
		}

		// Draw all people labels.
		drawPersonLabels(g2d, settlement, mapPanel.isShowPersonLabels());

		// Draw all people labels.
		drawRobotLabels(g2d, settlement, mapPanel.isShowRobotLabels());

		// Restore original graphic transforms.
		g2d.setTransform(saveTransform);
	}

	private int spaceCount(String s) {
		int c = 0;
        for(int i = 0; i < s.length(); i++)  {
            char ch = s.charAt(i);
            if(ch == ' ')
            c++;
        }
        return c;
    }
    
	/**
	 * Draws labels for all of the buildings in the settlement.
	 * 
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 */
	private void drawBuildingLabels(Graphics2D g2d, Settlement settlement) {
		if (settlement != null) {
			
			double scale = mapPanel.getScale();
			int size = (int)(scale / 2.0);
			size = Math.max(size, 12);
			double yDiff = scale / 2.5;
			int yOffset = (int)yDiff;
			
			Iterator<Building> i = settlement.getBuildingManager().getBuildingSet().iterator();
			while (i.hasNext()) {
				Building building = i.next();
				String name = building.getNickName();

				int num = spaceCount(name);
				String words[] = name.split(" ");
				int s = words.length;
				
				Color frontColor = BUILDING_LABEL_COLOR;
				Color outlineColor = WHITE_LABEL_OUTLINE_COLOR;
				switch(building.getCategory()) {
					case WORKSHOP:
						frontColor = SHOP_LABEL_COLOR;
						outlineColor = BLACK_LABEL_OUTLINE_COLOR;
						break;
					case LABORATORY:
						frontColor = LAB_LABEL_COLOR;
						outlineColor = WHITE_LABEL_OUTLINE_COLOR;
						break;
					case LIVING:
						frontColor = LIVING_LABEL_COLOR;
						outlineColor = BLACK_LABEL_OUTLINE_COLOR;
						break;
					case MEDICAL:
						frontColor = MEDICAL_LABEL_COLOR;
						outlineColor = WHITE_LABEL_OUTLINE_COLOR;
						break;
					case COMMAND:
						frontColor = HAB_LABEL_COLOR;
						outlineColor = BLACK_LABEL_OUTLINE_COLOR;
						break;
					case VEHICLE: 
						frontColor = GARAGE_LABEL_COLOR;
						outlineColor = BLACK_LABEL_OUTLINE_COLOR;
						break;
					case HALLWAY: 
						frontColor = HALLWAY_LABEL_COLOR;
						outlineColor = WHITE_LABEL_OUTLINE_COLOR;
						break;
					case FARMING:
						frontColor = GREENHOUSE_LABEL_COLOR;
						outlineColor = BLACK_LABEL_OUTLINE_COLOR;
						break;
					case PROCESSING:
						frontColor = RESOURCE_LABEL_COLOR;
						outlineColor = BLACK_LABEL_OUTLINE_COLOR;
						break;
					case POWER:
						frontColor = REACTOR_LABEL_COLOR;
						outlineColor = BLACK_LABEL_OUTLINE_COLOR;
						break;
					case EVA_AIRLOCK:
						frontColor = EVA_LABEL_COLOR;
						outlineColor = BLACK_LABEL_OUTLINE_COLOR;
						break;
					case ERV:
						frontColor = ERV_LABEL_COLOR;
						outlineColor = WHITE_LABEL_OUTLINE_COLOR;
						break;
					default:
						break;
				}
						
				if (num == 1) {
					// if (type.equalsIgnoreCase(Building.HALLWAY)) {
					// 	// Shrink the size of a hallway label.
					// 	//e.g. Turned "Hallway 12 " into "H12"
					// 	String newName = "H " + words[1];
					// 	drawStructureLabel(g2d, newName, building.getPosition(),
					// 	frontColor, outlineColor, 0);
					// }
					// else if (type.equalsIgnoreCase(Building.TUNNEL)) {
					// 	// Shrink the size of a hallway label.
					// 	//e.g. Turned "Hallway 12 " into "H12"
					// 	String newName = "T " + words[1];
					// 	drawStructureLabel(g2d, newName, building.getPosition(),
					// 	frontColor, outlineColor, 0);
					// }
					// else {
			
						drawStructureLabel(g2d, name, building.getPosition(),
								frontColor, outlineColor, 0);
					//}
				}
				
				else { // more than one whitespace
					String last_1 = words[s-1];
					String last_2 = words[s-2];		
					words[s-2] = last_2 + " " + last_1;
					s = s-1;
						
					// Split up the name into multiple lines
					for (int j = 0; j < s; j++) {
						
						int y = 0;
						
						if (s == 2) {

							if (j == 0)
								y = - yOffset;
							else
								y = yOffset;
						}
						else { //if (s == 3) {
							if (j == 0)
								y = - (int)(yOffset * 2);
							else if (j == 1)
								y = 0;
							else
								y = (int)(yOffset * 2);	
						}
						
						drawStructureLabel(g2d, words[j], building.getPosition(),
								frontColor, outlineColor, y);
					}
				}
			}
		}
	}
 
	/**
	 * Draws labels for all of the construction sites in the settlement.
	 * 
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 */
	private void drawConstructionSiteLabels(Graphics2D g2d, Settlement settlement) {
		if (settlement != null) {
			
			double scale = mapPanel.getScale();
			float yOffset = (float)(scale / 2.0);
			yOffset = Math.max(yOffset, 12);

			Iterator<ConstructionSite> i = settlement
				.getConstructionManager()
				.getConstructionSites()
				.iterator();
			
			while (i.hasNext()) {
				ConstructionSite site = i.next();
				String siteLabel = getConstructionLabel(site);
				// Split up the name into multiple lines except with the whitespace after character 'm' or 'x'
				// e.g. foundation name="subsurface foundation 5m x 10m x 3m"
				String words[] = siteLabel.split(" ");
				int s = words.length;

				// more than one whitespace
				String last_1 = words[s-1];
				String last_2 = words[s-2];		
				words[s-2] = last_2 + " " + last_1;
				s = s-1;
					
				// Split up the name into multiple lines
				for (int j = 0; j < s; j++) {
					
					float y = 0;
					
					if (s == 2) {

						if (j == 0)
							y = - yOffset;
						else
							y = yOffset;
					}
					else { //if (s == 3) {
						if (j == 0)
							y = - (int)(yOffset * 2);
						else if (j == 1)
							y = 0;
						else
							y = (int)(yOffset * 2);	
					}
					
					drawStructureLabel(g2d, words[j], site.getPosition(),
							CONSTRUCTION_SITE_LABEL_COLOR, CONSTRUCTION_SITE_LABEL_OUTLINE_COLOR,
							y);
				}
			}
		}
	}

	/**
	 * Gets the label for a construction site.
	 * 
	 * @param site the construction site.
	 * @return the construction label.
	 */
	public static String getConstructionLabel(ConstructionSite site) {
		String label = ""; //$NON-NLS-1$
		ConstructionStage stage = site.getCurrentConstructionStage();
		if (stage != null) {
			if (site.isUndergoingConstruction()) {
				label = Msg.getString("LabelMapLayer.constructing", stage.getInfo().getName()); //$NON-NLS-1$
			} else if (site.isUndergoingSalvage()) {
				label = Msg.getString("LabelMapLayer.salvaging", stage.getInfo().getName()); //$NON-NLS-1$
			} else if (site.hasUnfinishedStage()) {
				if (stage.isSalvaging()) {
					label = Msg.getString("LabelMapLayer.salvagingUnfinished", stage.getInfo().getName()); //$NON-NLS-1$
				} else {
					label = Msg.getString("LabelMapLayer.constructingUnfinished", stage.getInfo().getName()); //$NON-NLS-1$
				}
			} else {
				label = Msg.getString("LabelMapLayer.completed", stage.getInfo().getName()); //$NON-NLS-1$
			}
		} else {
			label = Msg.getString("LabelMapLayer.noConstruction"); //$NON-NLS-1$
		}
		return label;
	}

	/**
	 * Draws labels for all of the vehicles parked at the settlement.
	 * 
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 */
	private void drawVehicleLabels(Graphics2D g2d, Settlement settlement) {
		if (settlement != null) {
			double scale = mapPanel.getScale();
			int size = (int)(scale / 2.0);
			size = Math.max(size, 12);
					
			// Draw all vehicles that are at the settlement location.
			Iterator<Vehicle> i = settlement.getParkedVehicles().iterator();
			while (i.hasNext()) {
				Vehicle vehicle = i.next();
				if (vehicle.getVehicleType() == VehicleType.LUV) {
					drawStructureLabel(g2d,vehicle.getName(), vehicle.getPosition(),
						VEHICLE_LABEL_COLOR, VEHICLE_LABEL_OUTLINE_COLOR, 0);
				}
				
				else {
					// Split up the name into multiple lines
					String words[] = vehicle.getName().split(" ");
					int s = words.length;
					for (int j = 0; j < s; j++) {
						drawStructureLabel(g2d, words[j], vehicle.getPosition(),
							VEHICLE_LABEL_COLOR, VEHICLE_LABEL_OUTLINE_COLOR, j * size);
					}
				}
			}
		}
	}

	/**
	 * Draws labels for all people at the settlement.
	 * 
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 * @param showNonSelectedPeople true if showing non-selected person labels.
	 */
	private void drawPersonLabels(
		Graphics2D g2d, Settlement settlement,
		boolean showNonSelectedPeople
	) {

		Collection<Person> people = settlement.getPeopleInVicinity();
		Person selectedPerson = mapPanel.getSelectedPerson();
		int xoffset = 5;
		double scale = mapPanel.getScale();
		float yoffset = (float)(scale / 2.0);
		yoffset = Math.max(yoffset, 12);

		Color sColor = FEMALE_SELECTED_COLOR;
		Color soColor = FEMALE_SELECTED_OUTLINE_COLOR;
		
		if (selectedPerson != null) {
			
			if (selectedPerson.getGender().equals(GenderType.MALE)) {
				sColor = MALE_SELECTED_COLOR;
				soColor = MALE_SELECTED_OUTLINE_COLOR;
			}
		
			// Draw selected person.
			if (people.contains(selectedPerson)) {
				float originalFontSize = 10f;
				// Draw person name.
				drawPersonRobotLabel(g2d, selectedPerson.getName(), selectedPerson.getPosition(), sColor.darker(), soColor,
						originalFontSize, xoffset, 0);

				originalFontSize = 8f;
				
				// Draw task.
				String taskString = "- " + Msg.getString("LabelMapLayer.activity", selectedPerson.getMind().getTaskManager().getTaskDescription(false)); //$NON-NLS-1$
				if (taskString != null && !taskString.equals("")) {
					drawPersonRobotLabel(
						g2d, taskString, selectedPerson.getPosition(), sColor.darker(), soColor,
						originalFontSize, xoffset, yoffset);
				}
				
				// Draw mission.
				Mission mission = selectedPerson.getMind().getMission();
				if (mission != null) {
					String missionString = "-- " + Msg.getString("LabelMapLayer.mission", mission.getName(), mission.getPhaseDescription()); //$NON-NLS-1$
					if (missionString != null && !missionString.equals("")) {
						drawPersonRobotLabel(
							g2d, missionString, selectedPerson.getPosition(), sColor.darker(), soColor,
							originalFontSize, xoffset, 1.8f * yoffset);
					}
				}
			}
		}

		// Draw all people except selected person.
		if (showNonSelectedPeople) {
			Iterator<Person> i = people.iterator();
			while (i.hasNext()) {
				Person person = i.next();
				
				if (!person.equals(selectedPerson)) {
					
					// Split up the name into 2 lines
					String words[] = person.getName().split(" ");
					int s = words.length;
					String n = "";
					for (int j = 0; j < s; j++) {
						if (j == 0) n = words[0];
						else n += " " + words[j].substring(0, 1) + ".";
					}	
					boolean male = person.getGender() == GenderType.MALE;
					float originalFontSize = 10f;
					drawPersonRobotLabel(g2d, n, person.getPosition(),
								(male ? MALE_COLOR : FEMALE_COLOR),
								(male ? MALE_OUTLINE_COLOR : FEMALE_OUTLINE_COLOR), 
								originalFontSize,
								xoffset, 0);
				}
			}
		}
	}


	/**
	 * Draws labels for all robots at the settlement.
	 * 
	 * @param g2d the graphics context.
	 * @param settlement the settlement.
	 * @param showNonSelectedRobots true if showing non-selected robot labels.
	 */
	private void drawRobotLabels(
		Graphics2D g2d, Settlement settlement,
		boolean showNonSelectedRobots
	) {

		List<Robot> robots = RobotMapLayer.getRobotsToDisplay(settlement);
		Robot selectedRobot = mapPanel.getSelectedRobot();
		float xoffset = 10L;
		double scale = mapPanel.getScale();
		float yoffset = (float)(scale / 2.0);
		yoffset = Math.max(yoffset, 12);
		
		// Draw all robots except selected robot.
		if (showNonSelectedRobots) {
			Iterator<Robot> i = robots.iterator();
			while (i.hasNext()) {
				Robot robot = i.next();
				float originalFontSize = 10f;
				if (!robot.equals(selectedRobot)) {
					drawPersonRobotLabel(g2d, robot.getName(), robot.getPosition(),
							ROBOT_COLOR, ROBOT_OUTLINE_COLOR, 
							originalFontSize, xoffset, 0);
				}
			}
		}

		// Draw selected robot.
		if (selectedRobot != null) { //robots.contains(selectedRobot)) {
			float originalFontSize = 10f;
			// Draw robot name.
			drawPersonRobotLabel(
				g2d, selectedRobot.getName(), selectedRobot.getPosition(),
				ROBOT_COLOR, ROBOT_SELECTED_OUTLINE_COLOR,
				originalFontSize, xoffset, 0);

			originalFontSize = 8f;
			// Draw task.
			String taskString = "- " + Msg.getString("LabelMapLayer.activity", selectedRobot.getBotMind().getBotTaskManager().getTaskDescription(false)); //$NON-NLS-1$
			if (taskString != null && !taskString.equals(""))
				drawPersonRobotLabel(
					g2d, taskString, selectedRobot.getPosition(),
					ROBOT_COLOR, ROBOT_SELECTED_OUTLINE_COLOR,
					originalFontSize, xoffset, yoffset);

			// Draw mission.
			Mission mission = selectedRobot.getBotMind().getMission();
			if (mission != null) {
				String missionString = "-- " + Msg.getString("LabelMapLayer.mission", mission.getName(), mission.getPhaseDescription()); //$NON-NLS-1$
				if (missionString != null && !missionString.equals(""))
					drawPersonRobotLabel(
						g2d, missionString, selectedRobot.getPosition(),
						ROBOT_SELECTED_COLOR.darker(), ROBOT_SELECTED_OUTLINE_COLOR,
						originalFontSize, xoffset, 1.8f * yoffset);
			}
		}
	}
	
	/**
	 * Draws a label centered at the X, Y location.
	 * 
	 * @param g2d the graphics 2D context.
	 * @param label the label string.
	 * @param loc the location from center of settlement (meters).
	 * @param labelColor the color of the label.
	 * @param labelOutlineColor the color of the outline of the label.
	 */
	private void drawStructureLabel(
		Graphics2D g2d, String label, LocalPosition loc,
		Color labelColor, Color labelOutlineColor, float yOffset
	) {
		double scale = mapPanel.getScale();
		float fontSize = Math.round(scale / 1.2);
		float size = (float) Math.max(fontSize / 30.0, 1.2);
		
		// If the scale is smaller than 5, then 
		// there is no need of using labelOutlineColor 
		if (scale <= 5)
			labelOutlineColor = labelColor;
		
		// yDiff cause the label to shift upward
//		double yDiff = scale / 30.0;
//		if (yOffset == -1)
//			yDiff = 0;
		
		// Save original graphics transforms.
		AffineTransform saveTransform = g2d.getTransform();
		Font saveFont = g2d.getFont();

		// Get the label image.
		Font font = g2d.getFont().deriveFont(Font.BOLD, fontSize);
		g2d.setFont(font);
		
		BufferedImage labelImage = getLabelImage(
			label, font, g2d.getFontRenderContext(),
			labelColor, labelOutlineColor
		);

		// Determine transform information.
		double centerX = labelImage.getWidth() / 2D;
		double centerY = labelImage.getHeight() / 2D;
		double translationX = (-1D * loc.getX() * mapPanel.getScale()) - centerX;
		double translationY = (-1D * loc.getY() * mapPanel.getScale()) - centerY;

		// Apply graphic transforms for label.
		AffineTransform newTransform = new AffineTransform(saveTransform);
		newTransform.translate(translationX, translationY);
		newTransform.rotate(mapPanel.getRotation() * -1D, centerX, centerY);
		g2d.setTransform(newTransform);

		// Draw image label with yOffset
		g2d.drawImage(labelImage, 0, Math.round(yOffset * size), mapPanel);
		
		// Restore original graphic transforms.
		g2d.setTransform(saveTransform);
		g2d.setFont(saveFont);
	}

	/**
	 * Draws a label to the right of an X, Y location.
	 * 
	 * @param g2d the graphics 2D context.
	 * @param label the label string.
	 * @param loc the location from center of settlement (meters).
	 * @param labelColor the color of the label.
	 * @param labelOutlineColor the color of the outline of the label.
	 * @param xOffset the X pixel offset from the center point.
	 * @param yOffset the Y pixel offset from the center point.
	 */
	private void drawPersonRobotLabel(
		Graphics2D g2d, String label, LocalPosition loc,
		Color labelColor, Color labelOutlineColor, float originalFontSize, float xOffset, float yOffset
	) {

		double scale = mapPanel.getScale();
		float fontSize = Math.round(scale / 2.5);
		float size = (float)(Math.max(fontSize / 30.0, 1.25));
		
		// Save original graphics transforms.
		AffineTransform saveTransform = g2d.getTransform();
		Font saveFont = g2d.getFont();

		// Get the label image.
		Font font = g2d.getFont().deriveFont(Font.PLAIN, originalFontSize + fontSize);
		g2d.setFont(font);
		
		BufferedImage labelImage = getLabelImage(
			label, font, g2d.getFontRenderContext(),
			labelColor, labelOutlineColor
		);

		// Determine transform information.
		double centerX = labelImage.getWidth() / 2D ;
		double centerY = labelImage.getHeight() / 2D ;
		double translationX = (-1D * loc.getX() * scale) - centerX;
		double translationY = (-1D * loc.getY() * scale) - centerY;

		// Apply graphic transforms for label.
		AffineTransform newTransform = new AffineTransform(saveTransform);
		newTransform.translate(translationX, translationY);
		newTransform.rotate(mapPanel.getRotation() * -1D, centerX, centerY);
		g2d.setTransform(newTransform);

		// Draw image label.
		int widthOffset =  (int)Math.round((centerX + fontSize) + xOffset);
		int heightOffset = (int)Math.round((centerY + fontSize) + yOffset * size);
		g2d.drawImage(labelImage, widthOffset, heightOffset, mapPanel);

		// Restore original graphic transforms.
		g2d.setTransform(saveTransform);
		g2d.setFont(saveFont);
	}

	/**
	 * Gets an image of the label from cache or creates one if it doesn't exist.
	 * 
	 * @param label the label string.
	 * @param font the font to use.
	 * @param fontRenderContext the font render context to use.
	 * @param labelColor the color of the label.
	 * @param labelOutlineColor the color of the outline of the label.
	 * @return buffered image of label.
	 */
	private BufferedImage getLabelImage(
		String label, Font font, FontRenderContext fontRenderContext, Color labelColor,
		Color labelOutlineColor
	) { 
		BufferedImage labelImage = null;
		String labelId = label + font.toString() + labelColor.toString() + labelOutlineColor.toString();
		if (labelImageCache.containsKey(labelId)) {
			labelImage = labelImageCache.get(labelId);
		} else {
			labelImage = createLabelImage(label, font, fontRenderContext, labelColor, labelOutlineColor);
			labelImageCache.put(labelId, labelImage);
		}
		return labelImage;
	}

//	/**
//	 * Creates a label image.
//	 * @param label the label string.
//	 * @param font the font to use.
//	 * @param fontRenderContext the font render context to use.
//	 * @param labelColor the color of the label.
//	 * @param labelOutlineColor the color of the outline of the label.
//	 * @return buffered image of label.
//   */
//	private BufferedImage createLabelImage(
//		String label, Font font, FontRenderContext fontRenderContext, Color labelColor,
//		Color labelOutlineColor) {
//
//		// Determine bounds.
//		TextLayout textLayout1 = new TextLayout(label, font, fontRenderContext);
//		Rectangle2D bounds1 = textLayout1.getBounds();
//
//		// Get label shape.
//		Shape labelShape = textLayout1.getOutline(null);
//
//		// Create buffered image for label.
//		int width = (int) (bounds1.getWidth() + bounds1.getX()) + 4;
//		int height = (int) (bounds1.getHeight()) + 4;
//		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
//
//		// Get graphics context from buffered image.
//		Graphics2D g2d = (Graphics2D) bufferedImage.getGraphics();
//		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
//		g2d.translate(2D - bounds1.getX(), 2D - bounds1.getY());
//
//		// Draw label outline.
//		Stroke saveStroke = g2d.getStroke();
//		g2d.setColor(labelOutlineColor);
//		g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
//		g2d.draw(labelShape);
//		g2d.setStroke(saveStroke);
//
//		// Fill label
//		g2d.setColor(labelColor);
//		g2d.fill(labelShape);
//
//		// Dispose of image graphics context.
//		g2d.dispose();
//
//		return bufferedImage;
//	}


	/**
	 * Creates a label image.
	 * 
	 * @param label the label string.
	 * @param font the font to use.
	 * @param fontRenderContext the font render context to use.
	 * @param labelColor the color of the label.
	 * @param labelOutlineColor the color of the outline of the label.
	 * @return buffered image of label.
	 */
	private BufferedImage createLabelImage(
		String label, Font font, FontRenderContext fontRenderContext, Color labelColor,
		Color labelOutlineColor) {

		// Determine bounds.
		TextLayout textLayout1 = new TextLayout(label, font, fontRenderContext);
		Rectangle2D bounds1 = textLayout1.getBounds();

		// Get label shape.
		Shape labelShape = textLayout1.getOutline(null);

		// Create buffered image for label.
		int width = (int) (bounds1.getWidth() + bounds1.getX()) + 4;
		int height = (int) (bounds1.getHeight()) + 4;
		BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		// Get graphics context from buffered image.
		Graphics2D g2d = (Graphics2D) bufferedImage.getGraphics();
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g2d.translate(2D - bounds1.getX(), 2D - bounds1.getY());

		// Draw label outline.
		Stroke saveStroke = g2d.getStroke();
		g2d.setColor(labelOutlineColor);
		g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

		// Draw outline
		g2d.draw(labelShape);

		// Restore stroke
		g2d.setStroke(saveStroke);
		
		g2d.setColor(labelColor);
		// Fill label
		g2d.fill(labelShape);

		// Dispose of image graphics context.
		g2d.dispose();

		return bufferedImage;
	}


	@Override
	public void destroy() {
		// Clear label image cache.
		labelImageCache.clear();
	}
}