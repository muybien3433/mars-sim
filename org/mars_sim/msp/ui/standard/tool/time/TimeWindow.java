/**
 * Mars Simulation Project
 * TimeWindow.java
 * @version 2.75 2004-03-08
 * @author Scott Davis
 */

package org.mars_sim.msp.ui.standard.tool.time;
 
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.mars_sim.msp.simulation.EarthClock;
import org.mars_sim.msp.simulation.MarsClock;
import org.mars_sim.msp.simulation.MasterClock;
import org.mars_sim.msp.simulation.SimulationProperties;
import org.mars_sim.msp.simulation.UpTimer;
import org.mars_sim.msp.ui.standard.MainDesktopPane;
import org.mars_sim.msp.ui.standard.tool.ToolWindow;

/** 
 * The TimeWindow is a tool window that displays the current 
 * Martian and Earth time 
 */
public class TimeWindow extends ToolWindow {

    private final static int RATIO_SCALE = 200;

    // Data members
    private MasterClock master;      // Master Clock
    private SimulationProperties properties;  // The simulation properties
    private MarsClock marsTime;      // Martian Clock
    private EarthClock earthTime;    // Earth Clock
    private UpTimer uptimer;         // Uptime Timer
    private MarsCalendarDisplay calendarDisplay;  // Martian calendar panel
    private JLabel martianTimeLabel; // JLabel for Martian time
    private JLabel martianMonthLabel; // JLabel for Martian month
    private JLabel northernSeasonLabel; // JLabel for Northern hemisphere season
    private JLabel southernSeasonLabel; // JLabel for Southern hemisphere season
    private JLabel earthTimeLabel;   // JLabel for Earth time
    private JLabel uptimeLabel;      // JLabel for uptimer
    private JSlider pulseSlider;     // JSlider for pulse

    /** Constructs a TimeWindow object 
     *  @param desktop the desktop pane
     */
    public TimeWindow(MainDesktopPane desktop) {

        // Use TimeWindow constructor
        super("Time Tool", desktop);

        // Initialize data members
        master = desktop.getMainWindow().getMars().getMasterClock();
        properties = desktop.getMainWindow().getMars().getSimulationProperties();
        marsTime = master.getMarsClock();
        earthTime = master.getEarthClock();
        uptimer = master.getUpTimer(); 

        // Get content pane
        JPanel mainPane = new JPanel(new BorderLayout());
        mainPane.setBorder(new EmptyBorder(5, 5, 5, 5));
        setContentPane(mainPane);

        // Create Martian time panel
        JPanel martianTimePane = new JPanel(new BorderLayout());
        martianTimePane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
        mainPane.add(martianTimePane, "North");

        // Create Martian time header label
        JLabel martianTimeHeaderLabel = new JLabel("Martian Time", JLabel.CENTER);
        martianTimePane.add(martianTimeHeaderLabel, "North");

        // Create Martian time label
        martianTimeLabel = new JLabel(marsTime.getTimeStamp(), JLabel.CENTER);
        martianTimePane.add(martianTimeLabel, "South");

        // Create Martian calendar panel
        JPanel martianCalendarPane = new JPanel(new FlowLayout());
        martianCalendarPane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
        mainPane.add(martianCalendarPane, "Center");

        // Create Martian calendar month panel
        JPanel calendarMonthPane = new JPanel(new BorderLayout());
        martianCalendarPane.add(calendarMonthPane);

        // Create martian month label
        martianMonthLabel = new JLabel(marsTime.getMonthName(), JLabel.CENTER);
        calendarMonthPane.add(martianMonthLabel, "North");

        // Create Martian calendar display
        calendarDisplay = new MarsCalendarDisplay(marsTime);
        JPanel innerCalendarPane = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        innerCalendarPane.setBorder(new BevelBorder(BevelBorder.LOWERED));
        innerCalendarPane.add(calendarDisplay);
        calendarMonthPane.add(innerCalendarPane, "Center");        

        JPanel southPane = new JPanel(new BorderLayout());
        mainPane.add(southPane, "South");

        JPanel simulationPane = new JPanel(new BorderLayout());
        southPane.add(simulationPane, "South");

        // Create Martian season panel
        JPanel marsSeasonPane = new JPanel(new BorderLayout());
        marsSeasonPane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
        southPane.add(marsSeasonPane, "North");

        // Create Martian season label
        JLabel marsSeasonLabel = new JLabel("Martian Seasons", JLabel.CENTER);
        marsSeasonPane.add(marsSeasonLabel, "North");

        // Create Northern season label
        northernSeasonLabel = new JLabel("Northern Hemisphere: " + marsTime.getSeason(MarsClock.NORTHERN_HEMISPHERE), JLabel.CENTER);
        marsSeasonPane.add(northernSeasonLabel, "Center");
 
        // Create Southern season label
        southernSeasonLabel = new JLabel("Southern Hemisphere: " + marsTime.getSeason(MarsClock.SOUTHERN_HEMISPHERE), JLabel.CENTER);
        marsSeasonPane.add(southernSeasonLabel, "South");

        // Create Earth time panel
        JPanel earthTimePane = new JPanel(new BorderLayout());
        earthTimePane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
        southPane.add(earthTimePane, "Center");

        // Create Earth time header label
        JLabel earthTimeHeaderLabel = new JLabel("Earth Time", JLabel.CENTER);
        earthTimePane.add(earthTimeHeaderLabel, "North");

        // Create Earth time label
        earthTimeLabel = new JLabel(earthTime.getTimeStamp(), JLabel.CENTER);
        earthTimePane.add(earthTimeLabel, "South");

        // Create uptime panel
        JPanel uptimePane = new JPanel(new BorderLayout());
        uptimePane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
        simulationPane.add(uptimePane, "North");

        // Create uptime header label
        JLabel uptimeHeaderLabel = new JLabel("Simulation Uptime", JLabel.CENTER);
        uptimePane.add(uptimeHeaderLabel, "North");

        // Create uptime label
        uptimeLabel = new JLabel(uptimer.getUptime(), JLabel.CENTER);
        uptimePane.add(uptimeLabel, "Center");

        // Create uptime panel
        JPanel pulsePane = new JPanel(new BorderLayout());
        pulsePane.setBorder(new CompoundBorder(new EtchedBorder(), new EmptyBorder(5, 5, 5, 5)));
        simulationPane.add(pulsePane, "South");

        // Create pulse header label
        JLabel pulseHeaderLabel = new JLabel("Simulation Speed", JLabel.CENTER);
        pulsePane.add(pulseHeaderLabel, "North");

        // Create pulse slider
        double existingRatio = master.getTimeRatio();
        int currentPosition = (int) (existingRatio) / RATIO_SCALE;
        if (currentPosition > 10) currentPosition = 10;
        pulseSlider = new JSlider(0, 10, currentPosition);
        pulseSlider.setMajorTickSpacing(1);
        pulseSlider.setPaintTicks(true);
        pulseSlider.addChangeListener(new ChangeListener() {
                public void stateChanged(ChangeEvent e) {
                    double ratio = (double)(pulseSlider.getValue() * RATIO_SCALE);
                    if (ratio <= 0D) ratio = 1;
                    try {
                    	master.setTimeRatio(ratio);
                    }
                    catch (Exception e2) {
                    	System.out.println(e2.getMessage());
                    }
                }
        });
        pulsePane.add(pulseSlider, "South");

        // Pack window
        pack();

        // Add 10 pixels to packed window width
        Dimension windowSize = getSize();
        setSize(new Dimension((int)windowSize.getWidth() + 10, (int) windowSize.getHeight()));
    }

    /** 
     * Update window 
     */
    public void update() {
       martianTimeLabel.setText(marsTime.getTimeStamp());
       earthTimeLabel.setText(earthTime.getTimeStamp());
       uptimeLabel.setText(uptimer.getUptime());
       martianMonthLabel.setText(marsTime.getMonthName());
       northernSeasonLabel.setText("Northern Hemisphere: " + marsTime.getSeason(MarsClock.NORTHERN_HEMISPHERE));
       southernSeasonLabel.setText("Southern Hemisphere: " + marsTime.getSeason(MarsClock.SOUTHERN_HEMISPHERE)); 
       calendarDisplay.update();
    }
}
