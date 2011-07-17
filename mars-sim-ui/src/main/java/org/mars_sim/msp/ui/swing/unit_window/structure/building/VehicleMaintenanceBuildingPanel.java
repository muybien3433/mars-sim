/**
 * Mars Simulation Project
 * VehicleMaintenanceBuildingPanel.java
 * @version 3.01 2011-07-17
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing.unit_window.structure.building;

import org.apache.commons.collections.CollectionUtils;
import org.mars_sim.msp.core.structure.building.function.VehicleMaintenance;
import org.mars_sim.msp.core.vehicle.Vehicle;
import org.mars_sim.msp.ui.swing.MainDesktopPane;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

/**
 * The VehicleMaintenanceBuildingPanel class is a building function panel representing 
 * the vehicle maintenance capabilities of the building.
 */
public class VehicleMaintenanceBuildingPanel extends BuildingFunctionPanel implements MouseListener {

	private VehicleMaintenance garage;
	private JLabel vehicleNumberLabel;
	private int vehicleNumberCache = 0;
	private JList vehicleList;
	private DefaultListModel vehicleListModel;
	private Collection<Vehicle> vehicleCache;

	/**
	 * Constructor
	 * @param garage the vehicle maintenance function
	 * @param desktop the main desktop
	 */
	public VehicleMaintenanceBuildingPanel(VehicleMaintenance garage, MainDesktopPane desktop) {
		
		// Use BuildingFunctionPanel constructor
		super(garage.getBuilding(), desktop);
		
		// Initialize data members
		this.garage = garage;
		
		// Set panel layout
		setLayout(new BorderLayout());
		
		// Create label panel
		JPanel labelPanel = new JPanel(new GridLayout(3, 1, 0, 0));
		add(labelPanel, BorderLayout.NORTH);
        
		// Create vehicle maintenance label
		JLabel vehicleMaintenanceLabel = new JLabel("Vehicle Loading/Maintenance", JLabel.CENTER);
		labelPanel.add(vehicleMaintenanceLabel);
        
		// Create vehicle number label
		vehicleNumberCache = garage.getCurrentVehicleNumber();
		vehicleNumberLabel = new JLabel("Vehicle Number: " + vehicleNumberCache, JLabel.CENTER);
		labelPanel.add(vehicleNumberLabel);
        
		// Create vehicle capacity label
		int vehicleCapacity = garage.getVehicleCapacity();
		JLabel vehicleCapacityLabel = new JLabel("Vehicle Capacity: " + vehicleCapacity, JLabel.CENTER);
		labelPanel.add(vehicleCapacityLabel);	
		
		// Create vehicle list panel
		JPanel vehicleListPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		add(vehicleListPanel, BorderLayout.CENTER);
        
		// Create scroll panel for vehicle list
		JScrollPane vehicleScrollPanel = new JScrollPane();
		vehicleScrollPanel.setPreferredSize(new Dimension(160, 60));
		vehicleListPanel.add(vehicleScrollPanel);
        
		// Create vehicle list model
		vehicleListModel = new DefaultListModel();
		vehicleCache = new ArrayList<Vehicle>(garage.getVehicles());
		Iterator<Vehicle> i = vehicleCache.iterator();
		while (i.hasNext()) vehicleListModel.addElement(i.next());
        
		// Create vehicle list
		vehicleList = new JList(vehicleListModel);
		vehicleList.addMouseListener(this);
		vehicleScrollPanel.setViewportView(vehicleList);
	}
	
	/**
	 * Update this panel
	 */
	public void update() {
		// Update vehicle list and vehicle mass label
	    
	    if (!CollectionUtils.isEqualCollection(vehicleCache, garage.getVehicles())) {
			vehicleCache = new ArrayList<Vehicle>(garage.getVehicles());
			vehicleListModel.clear();
			Iterator<Vehicle> i = vehicleCache.iterator();
			while (i.hasNext()) vehicleListModel.addElement(i.next());
            
            vehicleNumberCache = garage.getCurrentVehicleNumber();
			vehicleNumberLabel.setText("Vehicle Number: " + vehicleNumberCache);
		}
	}
	
	/** 
	 * Mouse clicked event occurs.
	 * @param event the mouse event
	 */
	public void mouseClicked(MouseEvent event) {
		// If double-click, open vehicle window.
		if (event.getClickCount() >= 2) {
		    Vehicle vehicle = (Vehicle) vehicleList.getSelectedValue();
		    if (vehicle != null) {
		        desktop.openUnitWindow(vehicle, false);
		    }
		}
	}

	public void mousePressed(MouseEvent arg0) {}
	public void mouseReleased(MouseEvent arg0) {}
	public void mouseEntered(MouseEvent arg0) {}
	public void mouseExited(MouseEvent arg0) {}
}