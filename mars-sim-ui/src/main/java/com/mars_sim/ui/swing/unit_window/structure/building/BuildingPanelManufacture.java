/*
 * Mars Simulation Project
 * BuildingPanelManufacture.java
 * @date 2022-07-10
 * @author Scott Davis
 */
package com.mars_sim.ui.swing.unit_window.structure.building;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;

import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import com.mars_sim.core.Unit;
import com.mars_sim.core.logging.SimLogger;
import com.mars_sim.core.manufacture.ManufactureProcess;
import com.mars_sim.core.manufacture.ManufactureProcessInfo;
import com.mars_sim.core.manufacture.ManufactureUtil;
import com.mars_sim.core.manufacture.SalvageProcess;
import com.mars_sim.core.manufacture.SalvageProcessInfo;
import com.mars_sim.core.person.Person;
import com.mars_sim.core.person.ai.SkillManager;
import com.mars_sim.core.person.ai.SkillType;
import com.mars_sim.core.robot.Robot;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.Manufacture;
import com.mars_sim.core.tool.Msg;
import com.mars_sim.ui.swing.ImageLoader;
import com.mars_sim.ui.swing.MainDesktopPane;
import com.mars_sim.ui.swing.unit_window.structure.ManufacturePanel;
import com.mars_sim.ui.swing.unit_window.structure.SalvagePanel;
import com.mars_sim.ui.swing.utils.AttributePanel;
import com.mars_sim.ui.swing.utils.ProcessInfoRenderer;



/**
 * A building panel displaying the manufacture building function.
 */
@SuppressWarnings("serial")
public class BuildingPanelManufacture extends BuildingFunctionPanel {

	/** Default logger. */
	private static final SimLogger logger = SimLogger.getLogger(BuildingPanelManufacture.class.getName());

	private static final String MANU_ICON = "manufacture";
	private static final String BUTTON_TEXT = Msg.getString("TabPanelManufacture.button.createNewProcess"); // $NON-NLS-1$
	private static final String QUEUE_TEXT = "Queue This Process";
	
	/** Is UI constructed. */
	private boolean uiDone = false;
	
	private static int processStringWidth = 170;

	/** The manufacture building. */
	private Manufacture workshop;
	/** Panel for displaying process panels. */
	private JPanel processListPane;
	/** The scroll panel for the process list. */
	private JScrollPane scrollPanel;
	/** List of manufacture processes in building. */
	private List<ManufactureProcess> processCache;
	/** List of salvage processes in building. */
	private List<SalvageProcess> salvageCache;
	/** Process selector. */
	private JComboBox<Object> processComboBox;
	
	private DefaultComboBoxModel<Object> processModel;
	/** List of available processes. */
	private transient List<Object> newProcessCache;
	/** Process selection button. */
	private JButton newProcessButton;

	private JLabel printersUsed;

	/**
	 * Constructor.
	 * 
	 * @param workshop the manufacturing building function.
	 * @param desktop  the main desktop.
	 */
	public BuildingPanelManufacture(Manufacture workshop, MainDesktopPane desktop) {
		// Use BuildingFunctionPanel constructor.
		super(
			Msg.getString("BuildingPanelManufacture.title"),
			ImageLoader.getIconByName(MANU_ICON), 
			workshop.getBuilding(), 
			desktop
		);

		// Initialize data model.
		this.workshop = workshop;
	}
	
	/**
	 * Builds the UI.
	 */
	@Override
	protected void buildUI(JPanel center) {

		// Prepare label panel
		AttributePanel labelPanel = new AttributePanel(3);
		center.add(labelPanel, BorderLayout.NORTH);

		// Prepare tech level label
		labelPanel.addTextField("Tech Level", Integer.toString(workshop.getTechLevel()), null);

		// Prepare processCapacity label
		labelPanel.addTextField("Process Capacity", Integer.toString(workshop.getMaxProcesses()), null);

		// Prepare processCapacity label
		printersUsed = labelPanel.addTextField("# of Printers In Use",
								Integer.toString(workshop.getNumPrintersInUse()), null);
			
		// Create scroll pane for manufacturing processes
		scrollPanel = new JScrollPane();
		scrollPanel.setPreferredSize(new Dimension(170, 90));
		center.add(scrollPanel, BorderLayout.CENTER);

		// Create process list main panel
		JPanel processListMainPane = new JPanel(new BorderLayout(0, 0));
		scrollPanel.setViewportView(processListMainPane);

		// Create process list panel
		processListPane = new JPanel(new FlowLayout(10, 10 ,10));
		processListPane.setLayout(new BoxLayout(processListPane, BoxLayout.Y_AXIS));
		processListMainPane.add(processListPane, BorderLayout.NORTH);

		// Create process panels
		processCache = new ArrayList<>(workshop.getProcesses());
		Iterator<ManufactureProcess> i = processCache.iterator();
		while (i.hasNext())
			processListPane.add(new ManufacturePanel(i.next(), false, processStringWidth));

		// Create salvage panels.
		salvageCache = new ArrayList<>(workshop.getSalvageProcesses());
		Iterator<SalvageProcess> j = salvageCache.iterator();
		while (j.hasNext())
			processListPane.add(new SalvagePanel(j.next(), false, processStringWidth));

		// Create interaction panel.
		JPanel interactionPanel = new JPanel(new GridLayout(2, 1, 10, 10));
		center.add(interactionPanel, BorderLayout.SOUTH);
		
		// Create new manufacture process selection.
		processModel = new DefaultComboBoxModel<>();
		processComboBox = new JComboBox<>(processModel);
		processComboBox.setRenderer(new ManufactureSelectionListCellRenderer());
		processComboBox.setToolTipText("Select an Available Manufacturing Process");
		interactionPanel.add(processComboBox);

		// Create new process button.
		JPanel btnPanel = new JPanel(new FlowLayout());
		newProcessButton = new JButton(BUTTON_TEXT);
		btnPanel.add(newProcessButton);
		newProcessButton.setToolTipText("Create a New Manufacturing Process or Salvage a Process");
		newProcessButton.addActionListener(event -> {
			try {
				Object selectedItem = processComboBox.getSelectedItem();
				if (selectedItem != null) {
					if (selectedItem instanceof ManufactureProcessInfo selectedProcess) {

						if (ManufactureUtil.canProcessBeStarted(selectedProcess, workshop)) {
							getWorkshop().addProcess(new ManufactureProcess(selectedProcess, workshop));
							
							update();

							logger.log(workshop.getBuilding(), Level.CONFIG, 0, "Player selected the manufacturing process '" 
									+ selectedProcess.getName() + "'.");
						}
						
						else if (ManufactureUtil.canProcessBeQueued(selectedProcess, workshop)) {
							workshop.addToManuQueue(new ManufactureProcess(selectedProcess, workshop));
							
							update();

							logger.log(workshop.getBuilding(), Level.CONFIG, 0, "Player queued the manufacturing process '" 
									+ selectedProcess.getName() + "'.");
						}					
					} 
					
					else if (selectedItem instanceof SalvageProcessInfo selectedSalvage) {

						if (ManufactureUtil.canSalvageProcessBeStarted(selectedSalvage, workshop)) {
							Unit salvagedUnit = ManufactureUtil.findUnitForSalvage(selectedSalvage, workshop.getBuilding().getSettlement());
							workshop.addSalvageProcess(
									new SalvageProcess(selectedSalvage, workshop, salvagedUnit));
							update();

							logger.log(workshop.getBuilding(), Level.CONFIG, 0, "Player selected the salvaging process '" 
									+ salvagedUnit.getName() + "'.");
						}
						else if (ManufactureUtil.canSalvageProcessBeQueued(selectedSalvage, workshop)) {
							Unit salvagedUnit = ManufactureUtil.findUnitForSalvage(selectedSalvage, workshop.getBuilding().getSettlement());
							workshop.addToSalvageQueue(
									new SalvageProcess(selectedSalvage, workshop, salvagedUnit));
							update();

							logger.log(workshop.getBuilding(), Level.CONFIG, 0, "Player queued the salvaging process '" 
									+ salvagedUnit.getName() + "'.");
						}
					}
				}
			} catch (Exception e) {
				logger.severe(workshop.getBuilding(), 0, "new process button", e);
			}
		});
		interactionPanel.add(btnPanel);		

		// Add available salvage processes.
		populateAvailableProcesses();
	}

	/**
	 * Gets all the manufacture processes at the settlement.
	 * 
	 * @return list of manufacture processes.
	 */
	private List<ManufactureProcess> getManufactureProcesses() {
		List<ManufactureProcess> result = new ArrayList<>();

		result.addAll(workshop.getProcesses());

		if (!workshop.isFull()) {
			Iterator<ManufactureProcess> j = workshop.getQueueManuProcesses().iterator();
			while (j.hasNext()) {
				ManufactureProcess process = j.next();
				result.add(process);
				workshop.loadFromManuQueue(process);
				// Add only one at a time to ensure it's not full
				break;
			}
		}

		return result;
	}

	/**
	 * Gets all the salvage processes at the settlement.
	 * 
	 * @return list of salvage processes.
	 */
	private List<SalvageProcess> getSalvageProcesses() {
		List<SalvageProcess> result = new ArrayList<>();

		result.addAll(workshop.getSalvageProcesses());
			
		if (!workshop.isFull()) {
			Iterator<SalvageProcess> j = workshop.getQueueSalvageProcesses().iterator();
			while (j.hasNext()) {
				SalvageProcess process = j.next();
				result.add(process);
				workshop.loadFromSalvageQueue(process);
				// Add only one at a time to ensure it's not full
				break;
			}	
		}

		return result;
	}
	
	@Override
	public void update() {	
		if (!uiDone)
			initializeUI();

		List<ManufactureProcess> processes = getManufactureProcesses();
		List<SalvageProcess> salvages = getSalvageProcesses();
		
		if (!processCache.equals(processes) || !salvageCache.equals(salvages)) {

			// Add process panels for new processes.
			for (ManufactureProcess process : processes) {
				if (!processCache.contains(process))
					processListPane.add(new ManufacturePanel(process, false, processStringWidth));
			}

			// Add salvage panels for new salvage processes.
			for (SalvageProcess salvage : salvages) {
				if (!salvageCache.contains(salvage))
					processListPane.add(new SalvagePanel(salvage, false, processStringWidth));
			}

			// Remove process panels for old processes.
			for (ManufactureProcess process : processCache) {
				if (!processes.contains(process)) {
					ManufacturePanel panel = getManufacturePanel(process);
					if (panel != null)
						processListPane.remove(panel);
				}
			}

			// Remove salvage panels for old salvages.
			for (SalvageProcess salvage : salvageCache) {
				if (!salvages.contains(salvage)) {
					SalvagePanel panel = getSalvagePanel(salvage);
					if (panel != null)
						processListPane.remove(panel);
				}
			}

			// Update processCache
			processCache.clear();
			processCache.addAll(processes);

			// Update salvageCache
			salvageCache.clear();
			salvageCache.addAll(salvages);

			scrollPanel.validate();
		}

		// Update all process panels.
		for (ManufactureProcess mp : processes) {
			ManufacturePanel panel = getManufacturePanel(mp);
			if (panel != null)
				panel.update();
		}

		// Update all salvage panels.
		for (SalvageProcess sp : salvages) {
			SalvagePanel panel = getSalvagePanel(sp);
			if (panel != null)
				panel.update();
		}

		printersUsed.setText(Integer.toString(workshop.getNumPrintersInUse()));
		populateAvailableProcesses();
	}

	/**
	 * Loads the Combo according to what can be actually run not just what is defined.
	 */
	private void populateAvailableProcesses() {
		List<Object> newProcesses = new ArrayList<>();

		if (workshop.getProcesses().size() < workshop.getNumPrintersInUse()) {
			// Find list of doable manufacturing processes
			newProcesses.addAll(getAvailableProcesses());
			newProcesses.addAll(getAvailableSalvageProcesses());
		}

		// Compare to last time
		if ((newProcessCache == null) || !newProcessCache.equals(newProcesses)) {
			Object currentSelection = processComboBox.getSelectedItem();
			newProcessCache = newProcesses;

			// Update the items
			processModel.removeAllElements();
			processModel.addAll(newProcessCache);

			if ((currentSelection != null) && newProcessCache.contains(currentSelection)) {
				processComboBox.setSelectedItem(currentSelection);
			}
			
			// Update new process button.
//			newProcessButton.setEnabled(processComboBox.getItemCount() > 0);

			if (workshop.isFull()) {
				newProcessButton.setText(QUEUE_TEXT);
			}
			else {
				newProcessButton.setText(BUTTON_TEXT);
			}		
		}
	}

	/**
	 * Gets the panel for a manufacture process.
	 * 
	 * @param process the manufacture process.
	 * @return manufacture panel or null if none.
	 */
	private ManufacturePanel getManufacturePanel(ManufactureProcess process) {
		ManufacturePanel result = null;

		for (int x = 0; x < processListPane.getComponentCount(); x++) {
			Component component = processListPane.getComponent(x);
			if ((component instanceof ManufacturePanel panel) 
					&& panel.getManufactureProcess().equals(process)) {
				result = panel;
			}
		}

		return result;
	}

	/**
	 * Gets the panel for a salvage process.
	 * 
	 * @param process the salvage process.
	 * @return the salvage panel or null if none.
	 */
	private SalvagePanel getSalvagePanel(SalvageProcess process) {
		SalvagePanel result = null;

		for (int x = 0; x < processListPane.getComponentCount(); x++) {
			Component component = processListPane.getComponent(x);
			if ((component instanceof SalvagePanel panel) 
				&& panel.getSalvageProcess().equals(process)) {
				result = panel;
			}
		}

		return result;
	}

	/**
	 * Gets all manufacturing processes available at the workshop.
	 * 
	 * @return vector of processes.
	 */
	private List<ManufactureProcessInfo> getAvailableProcesses() {
		List<ManufactureProcessInfo> result = new ArrayList<>();

		// Determine highest materials science skill level at settlement.
		Settlement settlement = workshop.getBuilding().getSettlement();
		int highestSkillLevel = 0;
		for(Person tempPerson : settlement.getAllAssociatedPeople()) {
			SkillManager skillManager = tempPerson.getSkillManager();
			int skill = skillManager.getSkillLevel(SkillType.MATERIALS_SCIENCE);
			highestSkillLevel = (skill > highestSkillLevel ? skill : highestSkillLevel);
		}

		for(Robot r : settlement.getAllAssociatedRobots()) {
			SkillManager skillManager = r.getSkillManager();
			int skill = skillManager.getSkillLevel(SkillType.MATERIALS_SCIENCE);
			if (skill > highestSkillLevel) {
				highestSkillLevel = skill;
			}
		}
		
		// What can be done with the skill
		for(ManufactureProcessInfo process : ManufactureUtil
						.getManufactureProcessesForTechSkillLevel(workshop.getTechLevel(), highestSkillLevel)) {
			if (ManufactureUtil.canProcessBeStarted(process, workshop))
				result.add(process);
		}
		
		// Enable Collections.sorts by implementing Comparable<>
		Collections.sort(result);
		return result;
	}

	/**
	 * Gets all salvage processes available at the workshop.
	 * 
	 * @return vector of salvage processes.
	 */
	private List<SalvageProcessInfo> getAvailableSalvageProcesses() {
		List<SalvageProcessInfo> result = new ArrayList<>();

		for(SalvageProcessInfo process : ManufactureUtil.getSalvageProcessesForTechLevel(workshop.getTechLevel())) { 
			if (ManufactureUtil.canSalvageProcessBeStarted(process, workshop))
				result.add(process);
		}
		
		// Enable Collections.sorts by implementing Comparable<>
		Collections.sort(result);
		return result;
	}

	/**
	 * Gets the workshop for this panel.
	 * 
	 * @return workshop
	 */
	private Manufacture getWorkshop() {
		return workshop;
	}

	/**
	 * Gets the building of the workshop for this panel.
	 * 
	 * @return building
	 */
	private Building getBuilding() {
		return workshop.getBuilding();
	}
	
	/**
	 * Inner class for the manufacture selection list cell renderer.
	 */
	private static class ManufactureSelectionListCellRenderer extends DefaultListCellRenderer {

		@Override
		public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected,
				boolean cellHasFocus) {
			Component result = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
			if (value instanceof ManufactureProcessInfo mpinfo) {
				// Capitalized processName
				String processName = mpinfo.getName();
				if (processName.length() > processStringWidth)
					processName = processName.substring(0, processStringWidth) + "...";
				((JLabel) result).setText(processName);
				((JComponent) result).setToolTipText(ProcessInfoRenderer.getToolTipString(mpinfo));
			} else if (value instanceof SalvageProcessInfo spinfo) {
				// Capitalized processName
				String processName = spinfo.getName();
				if (processName.length() > processStringWidth)
					processName = processName.substring(0, processStringWidth) + "...";
				((JLabel) result).setText(processName);
				((JComponent) result).setToolTipText(SalvagePanel.getToolTipString(null, spinfo, null));
			}
			return result;
		}
	}

	/**
	 * Prepare object for garbage collection.
	 */
	@Override
	public void destroy() {
		super.destroy();
		
		// take care to avoid null exceptions
		workshop = null;
		processListPane = null;
		scrollPanel = null;
		processComboBox = null;
		newProcessCache = null;
		newProcessButton = null;
	}
}
