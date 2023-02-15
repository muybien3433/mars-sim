/*
 * Mars Simulation Project
 * TabPanelThermalSystem.java
 * @date 2022-07-31
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.unit_window.structure;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Iterator;
import java.util.List;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumnModel;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.structure.Settlement;
import org.mars_sim.msp.core.structure.ThermalSystem;
import org.mars_sim.msp.core.structure.building.Building;
import org.mars_sim.msp.core.structure.building.BuildingManager;
import org.mars_sim.msp.core.structure.building.function.ElectricHeatSource;
import org.mars_sim.msp.core.structure.building.function.HeatMode;
import org.mars_sim.msp.core.structure.building.function.HeatSource;
import org.mars_sim.msp.core.structure.building.function.SolarHeatSource;
import org.mars_sim.msp.core.structure.building.function.ThermalGeneration;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.StyleManager;
import org.mars_sim.msp.ui.swing.tool.SpringUtilities;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

/**
 * This is a tab panel for settlement's Thermal System .
 */
@SuppressWarnings("serial")
public class TabPanelThermalSystem
extends TabPanel {

	// default logger.
	//private static final Logger logger = Logger.getLogger(TabPanelThermalSystem.class.getName());
	
	private static final String HEAT_ICON = "heat";

	private static final String PERCENT_PER_SOL = " % per sol";
	private static final String PERCENT = " %";
	
	/** The Settlement instance. */
	private Settlement settlement;
	
	/** The cache of total heat generated. */
	private double heatGenCache;
	/** The cache of total power generated by heat source. */	
	private double powerGenCache;
	private double eheatCache;
	private double epowerCache;
	
	private JLabel heatGenLabel;
	private JLabel powerGenLabel;
	private JLabel effSolarHeat;
	private JLabel effElectricHeat;

	private JTable heatTable ;

	/**
	 *
	 */
	private JScrollPane heatScrollPane;
	
	private JCheckBox checkbox;

	private JTextField heatGenTF, powerGenTF, electricEffTF, solarEffTF, cellDegradTF;
	
	/** Table model for heat info. */
	private HeatTableModel heatTableModel;
	/** The settlement's Heating System */
	private ThermalSystem thermalSystem;
	
	private BuildingManager manager;

	private List<HeatSource> heatSources;
	
	private List<Building> buildings;
	
	private MainDesktopPane desktop;

	/**
	 * Constructor.
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelThermalSystem(Unit unit, MainDesktopPane desktop) {
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelThermalSystem.title"), //$NON-NLS-1$
			ImageLoader.getIconByName(HEAT_ICON),
			Msg.getString("TabPanelThermalSystem.title"), //$NON-NLS-1$
			unit, desktop
		);
		this.desktop = desktop;
		settlement = (Settlement) unit;
	}
	
	@Override
	protected void buildUI(JPanel content) {
		
		manager = settlement.getBuildingManager();
		thermalSystem = settlement.getThermalSystem();
		buildings = manager.getBuildingsWithThermal();

		JPanel topContentPanel = new JPanel();
		topContentPanel.setLayout(new BoxLayout(topContentPanel, BoxLayout.Y_AXIS));
		content.add(topContentPanel, BorderLayout.NORTH);
		
		// Prepare heat info panel.
		JPanel heatInfoPanel = new JPanel(new SpringLayout());
		topContentPanel.add(heatInfoPanel);

		// Prepare heat generated label.
		heatGenCache = thermalSystem.getGeneratedHeat();
		heatGenLabel = new JLabel(Msg.getString("TabPanelThermalSystem.totalHeatGen"), JLabel.RIGHT); //$NON-NLS-1$
		heatGenLabel.setToolTipText(Msg.getString("TabPanelThermalSystem.totalHeatGen.tooltip")); //$NON-NLS-1$
		heatInfoPanel.add(heatGenLabel);

		JPanel wrapper1 = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING));
		heatGenTF = new JTextField(StyleManager.DECIMAL_KW.format(heatGenCache));
		heatGenTF.setEditable(false);
		heatGenTF.setPreferredSize(new Dimension(120, 24));
		wrapper1.add(heatGenTF);
		heatInfoPanel.add(wrapper1);

		// Prepare power generated label.
		powerGenCache = thermalSystem.getGeneratedPower();
		powerGenLabel = new JLabel(Msg.getString("TabPanelThermalSystem.totalPowerGen"), JLabel.RIGHT); //$NON-NLS-1$
		powerGenLabel.setToolTipText(Msg.getString("TabPanelThermalSystem.totalPowerGen.tooltip")); //$NON-NLS-1$
		heatInfoPanel.add(powerGenLabel);

		JPanel wrapper2 = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING));
		powerGenTF = new JTextField(StyleManager.DECIMAL_KW.format(powerGenCache));
		powerGenTF.setEditable(false);
		powerGenTF.setPreferredSize(new Dimension(120, 24));//setColumns(20);
		wrapper2.add(powerGenTF);
		heatInfoPanel.add(wrapper2);

		double eff_electric_Heating = getAverageEfficiencyElectricHeat();
		effElectricHeat = new JLabel(Msg.getString("TabPanelThermalSystem.electricHeatingEfficiency"), JLabel.RIGHT); //$NON-NLS-1$
		effElectricHeat.setToolTipText(Msg.getString("TabPanelThermalSystem.electricHeatingEfficiency.tooltip")); //$NON-NLS-1$
		heatInfoPanel.add(effElectricHeat);

		JPanel wrapper3 = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING));
		electricEffTF = new JTextField(StyleManager.DECIMAL_PLACES1.format(eff_electric_Heating*100D) + PERCENT);
		electricEffTF.setEditable(false);
		electricEffTF.setPreferredSize(new Dimension(120, 24));
		wrapper3.add(electricEffTF);
		heatInfoPanel.add(wrapper3);

		double eff_solar_heat =  getAverageEfficiencySolarHeating();
		effSolarHeat = new JLabel(Msg.getString("TabPanelThermalSystem.solarHeatingEfficiency"), JLabel.RIGHT); //$NON-NLS-1$
		effSolarHeat.setToolTipText(Msg.getString("TabPanelThermalSystem.solarHeatingEfficiency.tooltip")); //$NON-NLS-1$		
		heatInfoPanel.add(effSolarHeat);

		JPanel wrapper4 = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING));
		solarEffTF = new JTextField(StyleManager.DECIMAL_PLACES2.format(eff_solar_heat*100D) + PERCENT);
		solarEffTF.setEditable(false);
		solarEffTF.setPreferredSize(new Dimension(120, 24));
		wrapper4.add(solarEffTF);
		heatInfoPanel.add(wrapper4);

		// Prepare degradation rate label.
		double degradRate = SolarHeatSource.DEGRADATION_RATE_PER_SOL;
		JLabel degradRateLabel = new JLabel(Msg.getString("TabPanelThermalSystem.degradRate"), JLabel.RIGHT); //$NON-NLS-1$
		degradRateLabel.setToolTipText(Msg.getString("TabPanelThermalSystem.degradRate.tooltip")); //$NON-NLS-1$	
		heatInfoPanel.add(degradRateLabel);

		JPanel wrapper5 = new JPanel(new FlowLayout(0, 0, FlowLayout.LEADING));
		cellDegradTF = new JTextField(StyleManager.DECIMAL_PLACES2.format(degradRate*100D) + PERCENT_PER_SOL);
		cellDegradTF.setEditable(false);
		cellDegradTF.setPreferredSize(new Dimension(120, 24));//setColumns(20);
		wrapper5.add(cellDegradTF);
		heatInfoPanel.add(wrapper5);

		// Create override check box panel.
		JPanel checkboxPane = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContentPanel.add(checkboxPane, BorderLayout.SOUTH);
		
		// Create override check box.
		checkbox = new JCheckBox(Msg.getString("TabPanelThermalSystem.checkbox.value")); //$NON-NLS-1$
		checkbox.setToolTipText(Msg.getString("TabPanelThermalSystem.checkbox.tooltip")); //$NON-NLS-1$
		checkbox.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				setNonGenerating(checkbox.isSelected());
			}
		});
		checkbox.setSelected(false);
		checkboxPane.add(checkbox);
		
		// Create scroll panel for the outer table panel.
		heatScrollPane = new JScrollPane();
		// increase vertical mousewheel scrolling speed for this one
		heatScrollPane.getVerticalScrollBar().setUnitIncrement(16);
		heatScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		content.add(heatScrollPane,BorderLayout.CENTER);
		
		// Prepare thermal control table model.
		heatTableModel = new HeatTableModel(settlement);
		
		// Prepare thermal control table.
		heatTable = new JTable(heatTableModel);
		// Call up the building window when clicking on a row on the table
		heatTable.addMouseListener(new MouseListener() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && !e.isConsumed()) {
					// Get the mouse-selected row
		            int r = heatTable.getSelectedRow();
		            SwingUtilities.invokeLater(() -> 
		            	desktop.openUnitWindow((Unit)heatTable.getValueAt(r, 1), false));
				}
			}
			@Override
			public void mousePressed(MouseEvent e) {
				// nothing
			}
			@Override
			public void mouseReleased(MouseEvent e) {
				// nothing
			}
			@Override
			public void mouseEntered(MouseEvent e) {
				// nothing
			}
			@Override
			public void mouseExited(MouseEvent e) {
				// nothing
			}
		});
		
		heatTable.setRowSelectionAllowed(true);
		TableColumnModel heatColumns = heatTable.getColumnModel();
		heatColumns.getColumn(0).setPreferredWidth(10);
		heatColumns.getColumn(1).setPreferredWidth(150);
		heatColumns.getColumn(2).setPreferredWidth(30);
		heatColumns.getColumn(3).setPreferredWidth(40);
		heatColumns.getColumn(4).setPreferredWidth(40);
		
		DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
		renderer.setHorizontalAlignment(SwingConstants.RIGHT);
		heatColumns.getColumn(1).setCellRenderer(renderer);
		heatColumns.getColumn(2).setCellRenderer(renderer);
		heatColumns.getColumn(3).setCellRenderer(renderer);
		heatColumns.getColumn(4).setCellRenderer(renderer);
		
		// Resizable automatically when its Panel resizes
		heatTable.setPreferredScrollableViewportSize(new Dimension(225, -1));
		heatTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);

		// Added sorting
		heatTable.setAutoCreateRowSorter(true);

		heatScrollPane.setViewportView(heatTable);

		// Lay out the spring panel.
		SpringUtilities.makeCompactGrid(heatInfoPanel,
		                                5, 2, //rows, cols
		                                20, 10,        //initX, initY
		                                10, 1);       //xPad, yPad
	}

	/**
	 * Sets if non-generating buildings should be shown.
	 * 
	 * @param value true or false.
	 */
	private void setNonGenerating(boolean value) {
		if (value)
			buildings = manager.getSortedBuildings();
		else
			buildings = manager.getBuildingsWithThermal();
		heatTableModel.update();
	}

	public double getAverageEfficiencySolarHeating() {
		double eff_solar_heat = 0;
		int i = 0;
		Iterator<Building> iHeat = manager.getBuildingsWithThermal().iterator();
		while (iHeat.hasNext()) {
			Building building = iHeat.next();
			heatSources = building.getThermalGeneration().getHeatSources();
			Iterator<HeatSource> j = heatSources.iterator();
			while (j.hasNext()) {
				HeatSource heatSource = j.next();
				if (heatSource instanceof SolarHeatSource) {
					i++;
					SolarHeatSource solarHeatSource = (SolarHeatSource) heatSource;
					eff_solar_heat += solarHeatSource.getEfficiencySolarHeat();
				}
			}
		}
		// get the average eff
		if (i > 0) {
			eff_solar_heat = eff_solar_heat / i;
		}
		return eff_solar_heat;
	}

	public double getAverageEfficiencyElectricHeat() {

		double eff_electric_heating = 0;
		int i = 0;
		Iterator<Building> iHeat = manager.getBuildingsWithThermal().iterator();
		while (iHeat.hasNext()) {
			Building building = iHeat.next();
			heatSources = building.getThermalGeneration().getHeatSources();
			Iterator<HeatSource> j = heatSources.iterator();
			while (j.hasNext()) {
				HeatSource heatSource = j.next();
				if (heatSource instanceof ElectricHeatSource) {
					i++;
					ElectricHeatSource electricHeatSource = (ElectricHeatSource) heatSource;
					eff_electric_heating += electricHeatSource.getEfficiency();
				}
			}
		}
		// get the average eff
		if (i > 0) {
			eff_electric_heating = eff_electric_heating / i;
		}
		return eff_electric_heating;
		
	}

	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {

		// NOT working ThermalGeneration heater = (ThermalGeneration) building.getFunction(BuildingFunction.THERMAL_GENERATION);
		// SINCE thermalSystem is a singleton. heatMode always = null not helpful: HeatMode heatMode = building.getHeatMode();
		// Check if the old heatGenCapacityCache is different from the latest .
		double heat = thermalSystem.getGeneratedHeat();
		if (heatGenCache != heat) {
			heatGenCache = heat;
			heatGenTF.setText(
					StyleManager.DECIMAL_KW.format(heatGenCache)
				);
		}

		double power = thermalSystem.getGeneratedPower(); 
		if (powerGenCache != power) {
			powerGenCache = power;
			powerGenTF.setText(
					StyleManager.DECIMAL_KW.format(power)
				);
		}

		double eheat = getAverageEfficiencyElectricHeat()*100D;
		if (eheatCache != eheat) {
			eheatCache = eheat;
			electricEffTF.setText(
					StyleManager.DECIMAL_PLACES2.format(eheat) + PERCENT
				);
		}

		double epower = getAverageEfficiencySolarHeating()*100D;
		if (epowerCache != epower) {
			epowerCache = epower;
			solarEffTF.setText(
					StyleManager.DECIMAL_PLACES2.format(epower) + PERCENT
				);
		}

		// Update thermal control table.
		heatTableModel.update();
	}

	/**
	 * Internal class used as model for the thermal control table.
	 */
	private class HeatTableModel extends AbstractTableModel {

		/** default serial id. */
		private static final long serialVersionUID = 1L;

		private ImageIcon dotRed;
		private ImageIcon dotYellow;
		private ImageIcon dotGreen_full, dotGreen_half, dotGreen_quarter, dotGreen_threeQuarter;

		private HeatTableModel(Settlement settlement) {
			dotRed = ImageLoader.getIcon(Msg.getString("img.dotRed")); //$NON-NLS-1$
			dotYellow = ImageLoader.getIcon(Msg.getString("img.dotYellow")); //$NON-NLS-1$
			dotGreen_full = ImageLoader.getIcon(Msg.getString("img.dotGreen_full")); //$NON-NLS-1$
			dotGreen_half = ImageLoader.getIcon(Msg.getString("img.dotGreen_half")); //$NON-NLS-1$
			dotGreen_quarter = ImageLoader.getIcon(Msg.getString("img.dotGreen_quarter")); //$NON-NLS-1$
			dotGreen_threeQuarter = ImageLoader.getIcon(Msg.getString("img.dotGreen_threeQuarter")); //$NON-NLS-1$
	
		}

		public int getRowCount() {
			return buildings.size();
		}

		public int getColumnCount() {
			return 5;
		}

//		public Building getBuilding(int row) { 
//			return buildings.get(row);
//		}
		
		public Class<?> getColumnClass(int columnIndex) {
			Class<?> dataType = super.getColumnClass(columnIndex);
			if (columnIndex == 0) dataType = ImageIcon.class;
			else if (columnIndex == 1) dataType = Building.class;
			else if (columnIndex == 2) dataType = Double.class;
			else if (columnIndex == 3) dataType = Double.class;
			else if (columnIndex == 4) dataType = Double.class;
			return dataType;
		}

		public String getColumnName(int columnIndex) {
			if (columnIndex == 0) return Msg.getString("TabPanelThermalSystem.column.s"); //$NON-NLS-1$
			else if (columnIndex == 1) return Msg.getString("TabPanelThermalSystem.column.building"); //$NON-NLS-1$
			else if (columnIndex == 2) return Msg.getString("TabPanelThermalSystem.column.temperature"); //$NON-NLS-1$
			else if (columnIndex == 3) return Msg.getString("TabPanelThermalSystem.column.generated"); //$NON-NLS-1$
			else if (columnIndex == 4) return Msg.getString("TabPanelThermalSystem.column.capacity"); //$NON-NLS-1$
			else return null;
		}

		public Object getValueAt(int row, int column) {

			Building building = buildings.get(row);
			HeatMode heatMode = building.getHeatMode();

			// if the building has thermal control system, display columns
				if (column == 0) {
					if (heatMode == HeatMode.HEAT_OFF) {
						return dotYellow; 
					}
					else if (heatMode == HeatMode.ONE_EIGHTH_HEAT) {
						return dotGreen_quarter;
					}
					else if (heatMode == HeatMode.QUARTER_HEAT) {
						return dotGreen_quarter;
					}
					else if (heatMode == HeatMode.HALF_HEAT) {
						return dotGreen_half;
					}
					else if (heatMode == HeatMode.THREE_QUARTER_HEAT) {
						return dotGreen_threeQuarter;
					}
					else if (heatMode == HeatMode.FULL_HEAT) {
						return dotGreen_full;
					}
					else if (heatMode == HeatMode.OFFLINE) {
						return dotRed;
					}
					else return null;
				}
				else if (column == 1)
					return buildings.get(row);
				else if (column == 2)
					// return temperature of the building;
					return  Math.round(building.getCurrentTemperature()*10.0)/10.0;
				else if (column == 3) {
					if (heatMode == HeatMode.HEAT_OFF) {
						return 0.0;
					}
					if (heatMode != HeatMode.FULL_HEAT 
							|| heatMode == HeatMode.THREE_QUARTER_HEAT
							|| heatMode == HeatMode.HALF_HEAT
							|| heatMode == HeatMode.QUARTER_HEAT
							|| heatMode == HeatMode.ONE_EIGHTH_HEAT
							) {
							ThermalGeneration heater = building.getThermalGeneration();
							if (heater != null) {
								return  Math.round(heater.getGeneratedHeat()*100.0)/100.0;
							}
							else
								return 0;
					}
				}
				else if (column == 4) {
					double generatedCapacity = 0.0;
					try {
						generatedCapacity = building.getThermalGeneration().getHeatGenerationCapacity();
					}
					catch (Exception e) {}
					return Math.round(generatedCapacity*100.0)/100.0;
				}
			return null;
		}

		public void update() {
			heatScrollPane.validate();

			fireTableDataChanged();
		}
	}
	
	/**
	 * Prepare object for garbage collection.
	 */
	@Override
	public void destroy() {
		super.destroy();
		
		heatGenLabel = null;	
		powerGenLabel = null;	
		effSolarHeat = null;	
		effElectricHeat = null;
		heatTable = null;
		heatScrollPane = null;	
		checkbox = null;
		heatGenTF = null;
		powerGenTF = null;
		electricEffTF = null;
		solarEffTF = null;
		cellDegradTF = null;
		heatTableModel = null;
		thermalSystem = null;
		settlement = null;
		manager = null;
		heatSources = null;
		buildings = null;
	}
}
