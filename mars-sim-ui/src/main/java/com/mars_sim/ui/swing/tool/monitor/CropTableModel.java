/*
 * Mars Simulation Project
 * CropTableModel.java
 * @date 2022-06-28
 * @author Manny Kung
 */
package com.mars_sim.ui.swing.tool.monitor;

import java.util.ArrayList;
import java.util.List;

import com.mars_sim.core.Unit;
import com.mars_sim.core.UnitEvent;
import com.mars_sim.core.UnitEventType;
import com.mars_sim.core.UnitType;
import com.mars_sim.core.structure.Settlement;
import com.mars_sim.core.structure.building.Building;
import com.mars_sim.core.structure.building.function.FunctionType;
import com.mars_sim.core.structure.building.function.farming.Crop;
import com.mars_sim.core.structure.building.function.farming.CropCategory;
import com.mars_sim.core.structure.building.function.farming.Farming;
import com.mars_sim.tools.Msg;

/**
 * The CropTableModel keeps track of the quantity of the growing crops in each greenhouse by categories.
 */
public class CropTableModel extends UnitTableModel<Building> {

	// Column indexes
	private static final int GREENHOUSE_NAME = 0;
	private static final int INITIAL_COLS = 1;

	private static final int FIRST_CROP_CAT = INITIAL_COLS + 1;
	
	/** The total number of available crop category. */
	private static int numCropCat = CropCategory.values().length;
	
	/** The number of Columns. */
	private static int columnCount = numCropCat + FIRST_CROP_CAT;

	/** Names of Columns. */
	private static final ColumnSpec[] COLUMNS;

	static {
		COLUMNS = new ColumnSpec[columnCount];
		COLUMNS[GREENHOUSE_NAME] = new ColumnSpec("Name of Greenhouse", String.class);
		COLUMNS[INITIAL_COLS] = new ColumnSpec("# Crops", Integer.class);

		for (CropCategory cat : CropCategory.values()) {
			int idx = FIRST_CROP_CAT + cat.ordinal();
			COLUMNS[idx] = new ColumnSpec(cat.getName(), Integer.class);
		}
	};

	// Data members
	/**
	 * A list of crop categories.
	 */
	private List<CropCategory> cropCategoryList;

	public CropTableModel(Settlement settlement) {
		super (UnitType.BUILDING, Msg.getString("CropTableModel.tabName"), //$NON-NLS-1$
				"CropTableModel.countingCrops", COLUMNS);
		cropCategoryList = new ArrayList<>(List.of(CropCategory.values()));

		// Cache all crop categories
		setCachedColumns(INITIAL_COLS, FIRST_CROP_CAT + CropCategory.values().length);
		setSettlementFilter(settlement);

		listenForUnits();
	}

	/**
	 * Filter the Greenhouses according to a Settlement
	 */
	@Override
	public boolean setSettlementFilter(Settlement filter) {
		resetEntities(filter.getBuildingManager().getBuildingSet(FunctionType.FARMING));

		return true;
	}

	/**
	 * Gives the position number for a particular crop group.
	 *
	 * @param String cropCat
	 * @return a position number
	 */
	private int getCategoryNum(String cat) {
		return CropCategory.valueOf(cat.toUpperCase()).ordinal();
	}

	/**
	 * Gets the total number of crop in a crop group from cropMap or cropCache.
	 *
	 * @param return a number
	 */
	private Object getValueAtCropCat(Building greenhouse, int cropColumn) {
		CropCategory cropCat = cropCategoryList.get(cropColumn - FIRST_CROP_CAT);

		int num = 0;
		for(Crop k : greenhouse.getFarming().getCrops()) {
			CropCategory cat = k.getCropSpec().getCropCategory();
			if (cat.equals(cropCat)) {
				num++;
			}
		}
		return num;
	}

	/**
	 * Return the value of a Cell.
	 *
	 * @param rowIndex    Row index of the cell.
	 * @param columnIndex Column index of the cell.
	 */
	@Override
	public Object getEntityValue(Building greenhouse, int columnIndex) {
		Object result = null;

		switch (columnIndex) {
			case GREENHOUSE_NAME: 
				result = greenhouse.getNickName();
				break;
			case INITIAL_COLS: 
				result = getTotalNumOfAllCrops(greenhouse);
				break;
			default: 
				result = getValueAtCropCat(greenhouse, columnIndex);
				break;
		}

		return result;
	}


	/**
	 * Gets the total numbers of all crops in a greenhouse building
	 *
	 * @param b Building
	 * @return total num of crops
	 */
	private int getTotalNumOfAllCrops(Building b) {
		if (!b.getFarming().getCrops().isEmpty())
			return b.getFarming().getCrops().size();
		return 0;
	}

	/**
	 * Catch unit update event.
	 *
	 * @param event the unit event.
	 */
	@Override
	public void unitUpdate(UnitEvent event) {
		Unit unit = (Unit) event.getSource();
		UnitEventType eventType = event.getType();
		Object target = event.getTarget();

		int columnNum = -1;
		if (eventType == UnitEventType.ADD_BUILDING_EVENT) {
			if (target instanceof Farming)
				columnNum = GREENHOUSE_NAME; // = 1
		}

		else if (eventType == UnitEventType.CROP_EVENT) {
			Crop crop = (Crop) target;
			CropCategory cat = crop.getCropSpec().getCropCategory();
			columnNum = getCategoryNum(cat.getName());
		}
		if (columnNum > -1) {
			entityValueUpdated((Building) unit, columnNum, columnNum);
		}
	}
}