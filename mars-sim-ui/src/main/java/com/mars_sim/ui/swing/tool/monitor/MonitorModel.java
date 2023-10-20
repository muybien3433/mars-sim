/**
 * Mars Simulation Project
 * MonitorModel.java
 * @version 3.2.0 2021-06-20
 * @author Manny Kung
 */

package com.mars_sim.ui.swing.tool.monitor;

import javax.swing.table.TableModel;

import com.mars_sim.core.structure.Settlement;

/**
 * This defines a table model for use in the Monitor tool.
 * The subclasses on this model could provide data on any Entity within the
 * Simulation. This interface defines simple extra method that provide a richer
 * interface for the Monitor window to be based upon.
 */
interface MonitorModel extends TableModel {

	/**
	 * Get the name of this model. The name will be a description helping
	 * the user understand the contents.
	 * @return Descriptive name.
	 */
	public String getName();


	/**
	 * Return the object at the specified row indexes.
	 * @param row Index of the row object.
	 * @return Object at the specified row.
	 */
	public Object getObject(int row);

	/**
	 * Prepares the model for deletion.
	 */
	public void destroy();

	/**
	 * Gets the model count string.
	 */
	public String getCountString();

	/**
	 * Set the Settlement as a filter
	 * @param filter Settlement
	 * @return 
	 */
	public boolean setSettlementFilter(Settlement filter);

	/**
	 * Set whether the changes to the Entities should be monitor for change.
	 * @param activate 
	 */
    public void setMonitorEntites(boolean activate);

	/**
	 * Get a tooltip representation of a cell. Most cells with return null.
	 * @param rowIndex
	 * @param colIndex
	 * @return
	 */
    public String getToolTipAt(int rowIndex, int colIndex);
}