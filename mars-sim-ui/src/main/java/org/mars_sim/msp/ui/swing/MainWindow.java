/*
 * Mars Simulation Project
 * MainWindow.java
 * @date 2021-08-15
 * @author Scott Davis
 */
package org.mars_sim.msp.ui.swing;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLayer;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.WindowConstants;
import javax.swing.plaf.metal.MetalLookAndFeel;

import org.mars.sim.console.InteractiveTerm;
import org.mars_sim.msp.core.GameManager;
import org.mars_sim.msp.core.GameManager.GameMode;
import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Simulation;
import org.mars_sim.msp.core.SimulationFiles;
import org.mars_sim.msp.core.SimulationListener;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.time.ClockListener;
import org.mars_sim.msp.core.time.ClockPulse;
import org.mars_sim.msp.core.time.MasterClock;
import org.mars_sim.msp.ui.astroarts.OrbitViewer;
import org.mars_sim.msp.ui.swing.tool.JStatusBar;
import org.mars_sim.msp.ui.swing.tool.WaitLayerUIPanel;
import org.mars_sim.msp.ui.swing.tool.svg.SVGIcon;
import org.mars_sim.msp.ui.swing.utils.MSPIconManager;

import com.alee.extended.button.WebSwitch;
import com.alee.extended.label.WebStyledLabel;
import com.alee.extended.memorybar.WebMemoryBar;
import com.alee.extended.overlay.FillOverlay;
import com.alee.extended.overlay.WebOverlay;
import com.alee.laf.WebLookAndFeel;
import com.alee.laf.button.WebButton;
import com.alee.managers.UIManagers;
import com.alee.managers.icon.LazyIcon;
import com.alee.managers.style.StyleId;
import com.alee.utils.swing.NoOpKeyListener;
import com.alee.utils.swing.NoOpMouseListener;

/**
 * The MainWindow class is the primary UI frame for the project. It contains the
 * main desktop pane window are, status bar and tool bars.
 */
public class MainWindow
extends JComponent implements ClockListener {

	private static final long serialVersionUID = 1L;

	private static final Logger logger = Logger.getLogger(MainWindow.class.getName());

	/** Icon image filename for frame */
	public static final String LANDER_PNG = "landerhab16.png";//"/images/LanderHab.png";
	public static final String LANDER_SVG = "/svg/icons/lander_hab.svg";

	public static final String INFO_RED_SVG = "/svg/icons/info_red.svg";
	public static final String PAUSE_ORANGE_SVG = "/svg/icons/pause_orange.svg";
	public static final String MARS_CALENDAR_SVG = "/svg/icons/calendar_mars.svg";

	public static final String INFO_SVG = "/svg/icons/info.svg";
	public static final String EDIT_SVG = "/svg/icons/edit.svg";
	public static final String LEFT_SVG = "/svg/icons/left_rotate.svg";
	public static final String RIGHT_SVG = "/svg/icons/right_rotate.svg";
	public static final String CENTER_SVG = "/svg/icons/center.svg";
	public static final String STACK_SVG = "/svg/icons/stack.svg";

	public static final String SAND_SVG = Msg.getString("img.svg.sand");//$NON-NLS-1$
	public static final String HAZY_SVG = Msg.getString("img.svg.hazy");//$NON-NLS-1$

	public static final String SANDSTORM_SVG = Msg.getString("img.svg.sandstorm"); //$NON-NLS-1$
	public static final String DUST_DEVIL_SVG = Msg.getString("img.svg.dust_devil");//$NON-NLS-1$

	public static final String COLD_WIND_SVG = Msg.getString("img.svg.cold_wind");//$NON-NLS-1$
	public static final String FROST_WIND_SVG = Msg.getString("img.svg.frost_wind");//$NON-NLS-1$

	public static final String SUN_SVG = Msg.getString("img.svg.sun"); //$NON-NLS-1$
	public static final String DESERT_SUN_SVG = Msg.getString("img.svg.desert_sun");//$NON-NLS-1$
	public static final String CLOUDY_SVG = Msg.getString("img.svg.cloudy");//$NON-NLS-1$
	public static final String SNOWFLAKE_SVG = Msg.getString("img.svg.snowflake");//$NON-NLS-1$
	public static final String ICE_SVG = Msg.getString("img.svg.ice");//$NON-NLS-1$
	public static final String MARS_SVG = Msg.getString("img.svg.mars");//$NON-NLS-1$
	public static final String TELESCOPE_SVG = Msg.getString("img.svg.telescope");//$NON-NLS-1$
	public static final String OS = System.getProperty("os.name").toLowerCase(); // e.g. 'linux', 'mac os x'

	/** The size of the weather icons */
	public static final int WEATHER_ICON_SIZE = 64;


	/** The main window frame. */
	private static JFrame frame;
	/** The Telescope icon. */
	private static Icon telescopeIcon;

	private static SplashWindow splashWindow;

	private static InteractiveTerm interactiveTerm;

	private static MSPIconManager iconManager;
	
	/** The four types of theme types. */
	public enum ThemeType {
		SYSTEM, NIMBUS, NIMROD, WEBLAF, METAL
	}
	/** The default ThemeType enum. */
	public ThemeType defaultThemeType = ThemeType.NIMBUS;

	// Data members
	private boolean isIconified = false;

	private boolean useDefault = false;

	private String lookAndFeelTheme;

	/** The unit tool bar. */
	private UnitToolBar unitToolbar;
	/** The tool bar. */
	private ToolToolBar toolToolbar;
	/** The main desktop. */
	private MainDesktopPane desktop;

	private MainWindowMenu mainWindowMenu;

	private Timer delayTimer1;

	private OrbitViewer orbitViewer;

	private JStatusBar statusBar;

	/** WebSwitch for the control of play or pause the simulation*/
	private WebSwitch pauseSwitch;
	private WebButton increaseSpeed;
	private WebButton decreaseSpeed;

	private JCheckBox overlayCheckBox;

	private WebOverlay overlay;

	private WebStyledLabel blockingOverlay;

	private WebMemoryBar memoryBar;

	private JPanel bottomPane;
	private JPanel mainPane;

	/** Arial font. */
	private Font ARIAL_FONT = new Font("Arial", Font.PLAIN, 14);

	private JLayer<JPanel> jlayer;
	private WaitLayerUIPanel layerUI = new WaitLayerUIPanel();

	private Dimension selectedSize;

	private Simulation sim;
	private MasterClock masterClock;

	/**
	 * Constructor 1.
	 *
	 * @param cleanUI true if window should display a clean UI.
	 */
	public MainWindow(boolean cleanUI, Simulation sim) {
		this.sim = sim;
				
		if (GameManager.getGameMode() == GameMode.COMMAND) {
			logger.log(Level.CONFIG, "Running mars-sim in Command Mode.");
		} else {
			logger.log(Level.CONFIG, "Running mars-sim in Sandbox Mode.");
		}

		// Start the wait layer
		layerUI.start();

		// this.cleanUI = cleanUI;
		// Set up the look and feel library to be used
		initializeTheme();

		// Set up the frame
		frame = new JFrame();
		frame.setResizable(true);
		frame.setMinimumSize(new Dimension(640, 640));
		
		GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice[] gs = ge.getScreenDevices();
		GraphicsDevice graphicsDevice = null;

		if (gs.length == 1) {
			logger.log(Level.CONFIG, "Detecting only one screen.");
			logger.config("1 screen detected.");	
		}
		else if (gs.length == 0) {
			throw new RuntimeException("No Screens Found.");
			// NOTE: what about the future server version of mars-sim in which no screen is needed.
		}
		else {
			logger.config(gs.length + " screens detected.");	
		}
		
		graphicsDevice = gs[0];
		int screenWidth = graphicsDevice.getDisplayMode().getWidth();
		int screenHeight = graphicsDevice.getDisplayMode().getHeight();

		if (cleanUI) {
			askScreenConfig(screenWidth, screenHeight);
		}
		else {
			chooseScreenSize(screenWidth, screenHeight);
		}	

		// Set up MainDesktopPane
		desktop = new MainDesktopPane(this, sim);

		// Set up other elements
		masterClock = sim.getMasterClock();
		init();

		// Show frame
		frame.setVisible(true);

		// Stop the wait indicator layer
		layerUI.stop();

		// Dispose the Splash Window
		disposeSplash();

		// Open all initial windows.
		desktop.openInitialWindows();
	}

	/**
	 * Asks if the player wants to use last saved screen configuration.
	 * 
	 * @param screenWidth
	 * @param screenHeight
	 */
	private void askScreenConfig(int screenWidth, int screenHeight) {

		logger.config("Do you want to use the last saved screen configuration ?");
		logger.config("To proceed, please choose 'Yes' or 'No' button in the dialog box.");
		
		int reply = JOptionPane.showConfirmDialog(frame,
				"Do you want to use the last saved screen configuration", 
				"Screen Configuration", 
				JOptionPane.YES_NO_OPTION);
        if (reply == JOptionPane.YES_OPTION) {
        	
			logger.config("You choose Yes to loading last saved screen configuration.");	
			
    		// Load previous UI configuration.
			UIConfig.INSTANCE.parseFile();
			
			// Set the UI configuration
			useDefault = UIConfig.INSTANCE.useUIDefault();
			logger.config("useDefault is: " + useDefault);
		
			if (useDefault) {
				logger.config("Will calculate screen size for default display instead.");
				setUpCalculatedScreen(screenWidth, screenHeight);
			}
			else {
				setUpSavedScreen();
			}
        }
        
        // No. use the new default setting
        else {
			logger.config("Will calculate screen size for default display instead.");
			setUpCalculatedScreen(screenWidth, screenHeight);
        }
	}
	
	private void setUpSavedScreen() {
		selectedSize = UIConfig.INSTANCE.getMainWindowDimension();
		
		// Set frame size
		frame.setSize(selectedSize);
		logger.config("The last saved window dimension is "	
			+ selectedSize.width
			+ " x "
			+ selectedSize.height
			+ ".");
		
		// Display screen at a certain location
		frame.setLocation(UIConfig.INSTANCE.getMainWindowLocation());
		logger.config("The last saved frame starts at (" 
				+ UIConfig.INSTANCE.getMainWindowLocation().x
				+ ", "
				+ UIConfig.INSTANCE.getMainWindowLocation().y
				+ ").");
	}
	
	private void setUpCalculatedScreen(int screenWidth, int screenHeight) {
		selectedSize = calculatedScreenSize(screenWidth, screenHeight);
//		selectedSize = new Dimension(screenWidth, screenHeight);
		
		// Set frame size
		frame.setSize(selectedSize);
		
		logger.config("The default window dimension is "
				+ selectedSize.width
				+ " x "
				+ selectedSize.height
				+ ".");

		frame.setLocation(
			((screenWidth - selectedSize.width) / 2),
			((screenHeight - selectedSize.height) / 2)
		);
		
		logger.config("Use default configuration to set frame to the center of the screen.");	
		logger.config("The window frame is centered and starts at (" 
				+ (screenWidth - selectedSize.width) / 2 
				+ ", "
				+ (screenHeight - selectedSize.height) / 2
				+ ").");
	}
	
	/**
	 * Chooses screen size.
	 * 
	 * @param screenWidth
	 * @param screenHeight
	 */
	private void chooseScreenSize(int screenWidth, int screenHeight) {
		// Load previous UI configuration.
		UIConfig.INSTANCE.parseFile();
		
		// Set the UI configuration
		useDefault = UIConfig.INSTANCE.useUIDefault();
		logger.config("useDefault is: " + useDefault);
	
		if (useDefault) {
			logger.config("Will calculate screen size for default display instead.");
			setUpCalculatedScreen(screenWidth, screenHeight);
		}
		else {
			setUpSavedScreen();
		}
	}
	
	/**
	 * Calculates the screen size.
	 * 
	 * @param screenWidth
	 * @param screenHeight
	 * @return
	 */
	private Dimension calculatedScreenSize(int screenWidth, int screenHeight) {
		logger.config("Current screen size is " + screenWidth + " x " + screenHeight);
		logger.config("useDefault is: " + useDefault);
		
		Dimension frameSize = null;
		if (useDefault) {
			frameSize = interactiveTerm.getSelectedScreen();
			logger.config("Use default screen configuration.");
			logger.config("Selected screen size is " + frameSize.width + " x " + frameSize.height);
		}
		else {
			// Use any stored size
			frameSize = UIConfig.INSTANCE.getMainWindowDimension();
			logger.config("Use last saved window size " + frameSize.width + " x " + frameSize.height);	
		}
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		if (screenSize != null) {
			logger.config("Current toolkit screen size is " + screenSize.width + " x " + screenSize.height);

			if (frameSize != null) {
				// Check selected is not bigger than the screen
				if (frameSize.width > screenSize.width
						|| frameSize.height > screenSize.height) {
					logger.warning("Selected screen size cannot be larger than physical screen size.");
					frameSize = null;
				}
//				else {
//					// proceed to the next
//				}
			}
			

			if (frameSize == null) {
				// Make frame size 80% of screen size.
				if (screenSize.width > 800) {
					frameSize = new Dimension(
						(int) Math.round(screenSize.getWidth() * .8),
						(int) Math.round(screenSize.getHeight() * .8)
					);
					logger.config("New window size is " + frameSize.width + " x " + frameSize.height);
				}
				else {
					frameSize = new Dimension(screenSize);
					logger.config("New window size is " + frameSize.width + " x " + frameSize.height);
				}
			}
		}
 
		return frameSize;
	}

	/**
	 * Get the selected screen size for the main window.
	 * @return
	 */
	public Dimension getSelectedSize() {
		return selectedSize;
	}

	public void stopLayerUI() {
		layerUI.stop();
	}

	public static void initIconManager() {
		// Set up an icon set for use throughout mars-sim
		iconManager = new MSPIconManager();

		int size = 24;

		iconManager.addSVGIcon("info_red", INFO_RED_SVG, 12, 12);
		iconManager.addSVGIcon("pause_orange", PAUSE_ORANGE_SVG, 300, 300);

		iconManager.addSVGIcon(
		        "calendar_mars",
		        MARS_CALENDAR_SVG,16, 16);

		iconManager.addSVGIcon(
		        "lander",
		        LANDER_SVG,16, 16);

		iconManager.addSVGIcon(
		        "info",
		        INFO_SVG,size, size);

		iconManager.addSVGIcon(
		        "edit",
		        EDIT_SVG,size, size);

		iconManager.addSVGIcon(
		        "left",
		        LEFT_SVG,size, size);

		iconManager.addSVGIcon(
		        "right",
		        RIGHT_SVG,size, size);

		iconManager.addSVGIcon(
		        "center",
		        CENTER_SVG,size, size);

		iconManager.addSVGIcon(
		        "stack",
		        STACK_SVG,size, size);

		/////////////////////////////////////////////////////////

		iconManager.addSVGIcon(
		        "sandstorm",
		        SANDSTORM_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		iconManager.addSVGIcon(
		        "dustDevil",
		        DUST_DEVIL_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		////////////////////

		iconManager.addSVGIcon(
		        "frost_wind",
		        FROST_WIND_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		iconManager.addSVGIcon(
		        "cold_wind",
		        COLD_WIND_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		////////////////////

		iconManager.addSVGIcon(
		        "sun",
		        SUN_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		iconManager.addSVGIcon(
		        "desert_sun",
		        DESERT_SUN_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		iconManager.addSVGIcon(
		        "cloudy",
		        CLOUDY_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		iconManager.addSVGIcon(
		        "snowflake",
		        SNOWFLAKE_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		iconManager.addSVGIcon(
		        "ice",
		        ICE_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		////////////////////

		iconManager.addSVGIcon(
		        "sand",
		        SAND_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		iconManager.addSVGIcon(
		        "hazy",
		        HAZY_SVG,WEATHER_ICON_SIZE, WEATHER_ICON_SIZE);

		////////////////////

		iconManager.addSVGIcon(
		      "mars",
		      MARS_SVG, 18, 18);

		iconManager.addSVGIcon(
			      "telescope",
			      TELESCOPE_SVG,14, 14);

	}

	/**
	 * Initializes UI elements for the frame
	 */
	@SuppressWarnings("serial")
	private void init() {

		frame.addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent event) {
				// Save simulation and UI configuration when window is closed.
				exitSimulation();
			}
		});

		frame.addWindowStateListener(new WindowStateListener() {
			   public void windowStateChanged(WindowEvent e) {
				   int state = e.getNewState();
                   isIconified = (state == Frame.ICONIFIED);
				   if (state == Frame.MAXIMIZED_HORIZ
						   || state == Frame.MAXIMIZED_VERT)
//					   frame.update(getGraphics());
						logger.log(Level.CONFIG, "MainWindow set to maximum."); //$NON-NLS-1$
					repaint();
			   }
		});

    	frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

		changeTitle(false);

		frame.setIconImage(getIconImage());

		// Set up the main pane
		mainPane = new JPanel(new BorderLayout());
		frame.add(mainPane);

		// Set up the jlayer pane
		JPanel jlayerPane = new JPanel(new BorderLayout());
		jlayerPane.add(desktop);
		// Set up the glassy wait layer for pausing
		jlayer = new JLayer<>(jlayerPane, layerUI);

		// Set up the overlay pane
		JPanel overlayPane = new JPanel(new BorderLayout());

		// Create a pause overlay
		createOverlay(overlayPane);

		// Add desktop to the overlay pane
		overlayPane.add(jlayer, BorderLayout.CENTER);

		// Add overlay
		mainPane.add(overlay, BorderLayout.CENTER);

		// Prepare tool toolbar
		toolToolbar = new ToolToolBar(this);

		// Add toolToolbar to mainPane
		overlayPane.add(toolToolbar, BorderLayout.NORTH);

		// Add bottomPane for holding unitToolbar and statusBar
		bottomPane = new JPanel(new BorderLayout());

		// Prepare unit toolbar
		unitToolbar = new UnitToolBar(this) {
			@Override
			protected JButton createActionComponent(Action a) {
				return super.createActionComponent(a);
			}
		};

		unitToolbar.setBorder(new MarsPanelBorder());
		// Remove the toolbar border, to blend into figure contents
		unitToolbar.setBorderPainted(true);

		mainPane.add(bottomPane, BorderLayout.SOUTH);
		bottomPane.add(unitToolbar, BorderLayout.CENTER);

		// set the visibility of tool and unit bars from preferences
		unitToolbar.setVisible(UIConfig.INSTANCE.showUnitBar());
		toolToolbar.setVisible(UIConfig.INSTANCE.showToolBar());

		// Prepare menu
		mainWindowMenu = new MainWindowMenu(this, desktop);
		frame.setJMenuBar(mainWindowMenu);

		// Close the unit bar when starting up
		unitToolbar.setVisible(false);

		// Create the status bar
		statusBar = new JStatusBar(1, 1, 28);

		// Create speed buttons
		createSpeedButtons();
		// Add the decrease speed button
		statusBar.addLeftComponent(decreaseSpeed, false);

		// Create pause switch
		createPauseSwitch();
		statusBar.addLeftComponent(pauseSwitch, false);

		// Add the increase speed button
		statusBar.addLeftComponent(increaseSpeed, false);

		// Create overlay button
		createOverlayCheckBox();
		statusBar.addLeftComponent(overlayCheckBox, false);

		// Create memory bar
		createMemoryBar();
		statusBar.addRightComponent(memoryBar, true);

		statusBar.addRightCorner();

		bottomPane.add(statusBar, BorderLayout.SOUTH);

		// Add this class to the master clock's listener
		masterClock.addClockListener(this, 1000L);
	}

	/**
	 * Sets up the pause overlay
	 *
	 * @param overlayPane
	 */
	public void createOverlay(JPanel overlayPane) {
		// Add overlayPane to overlay
		overlay = new WebOverlay(StyleId.overlay, overlayPane);

		Icon pauseIcon = new LazyIcon("pause_orange").getIcon();

        blockingOverlay = new WebStyledLabel(
        		pauseIcon,
                SwingConstants.CENTER
        );
        NoOpMouseListener.install(blockingOverlay);
        NoOpKeyListener.install(blockingOverlay);
	}

	public void createOverlayCheckBox() {
		overlayCheckBox = new JCheckBox("{Pause Overlay On/Off:b}", false);
		overlayCheckBox.putClientProperty(StyleId.STYLE_PROPERTY, StyleId.checkboxLink);
		overlayCheckBox.setToolTipText("Turn on/off pause overlay in desktop");

		overlayCheckBox.addItemListener(new ItemListener() {
		    @Override
		    public void itemStateChanged(ItemEvent e) {
		        if(e.getStateChange() == ItemEvent.SELECTED) {
		        	// Checkbox has been selected
		        	overlay.addOverlay(new FillOverlay(blockingOverlay));
		        } else {
		        	// Checkbox has been unselected
	                if (blockingOverlay.isShowing()) {
	                    overlay.removeOverlay(blockingOverlay);
	                }
		        };
		    }
		});
		// Disable the overlay check box at start of the sim
		overlayCheckBox.setEnabled(false);
	}

	public void createPauseSwitch() {
		pauseSwitch = new WebSwitch(true);
		pauseSwitch.setSwitchComponents(
				ImageLoader.getIcon(Msg.getString("img.speed.play")),
				ImageLoader.getIcon(Msg.getString("img.speed.pause")));
		pauseSwitch.setToolTipText("Pause or Resume the Simulation");

		pauseSwitch.addActionListener(e -> 
                masterClock.setPaused(!pauseSwitch.isSelected(), false)
		);
	}

	/**
	 * Open orbit viewer
	 */
	public void openOrbitViewer() {
		if (orbitViewer == null) {
			orbitViewer = new OrbitViewer(desktop);
			return;
		}
        orbitViewer.setVisible(!orbitViewer.isVisible());
	}


	public void setOrbitViewer(OrbitViewer orbitViewer) {
		this.orbitViewer = orbitViewer;
	}
	
	public Icon getTelescopeIcon() {
		if (telescopeIcon == null) {
			telescopeIcon = new LazyIcon("telescope").getIcon();
			telescopeIcon = ImageLoader.getNewIcon(Msg.getString("icon.telescope"));
		}
		return telescopeIcon;
	}

	public void createSpeedButtons() {
		increaseSpeed = new WebButton();
		increaseSpeed.setIcon(ImageLoader.getIcon(Msg.getString("img.speed.increase"))); //$NON-NLS-1$
		increaseSpeed.setToolTipText("Increase the sim speed (aka time ratio)");

		increaseSpeed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!masterClock.isPaused()) {
					masterClock.increaseSpeed();
				}
			};
		});

		decreaseSpeed = new WebButton();
		decreaseSpeed.setIcon(ImageLoader.getIcon(Msg.getString("img.speed.decrease"))); //$NON-NLS-1$
		decreaseSpeed.setToolTipText("Decrease the sim speed (aka time ratio)");

		decreaseSpeed.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!masterClock.isPaused()) {
					masterClock.decreaseSpeed();
				}
			};
		});

	}

	public void createMemoryBar() {
		memoryBar = new WebMemoryBar();
		memoryBar.setPreferredWidth(180);
		memoryBar.setRefreshRate(3000);
		memoryBar.setFont(ARIAL_FONT);
		memoryBar.setForeground(Color.DARK_GRAY);
	}

	public WebMemoryBar getMemoryBar() {
		return memoryBar;
	}


	/**
	 * Set up the timer for caching settlement windows
	 */
	public void setupSettlementWindowTimer() {
		delayTimer1 = new Timer();
		delayTimer1.schedule(new CacheLoadingSettlementTimerTask(), 2000);
	}

	/**
	 * Defines the delay timer class
	 */
	class CacheLoadingSettlementTimerTask extends TimerTask {
		public void run() {
			// Cache each settlement unit window
			SwingUtilities.invokeLater(() -> desktop.cacheSettlementUnitWindow());
		}
	}

	public JPanel getBottomPane() {
		return bottomPane;
	}


	/**
	 * Get the window's frame.
	 *
	 * @return the frame.
	 */
	public JFrame getFrame() {
		return frame;
	}

	/**
	 * Gets the main desktop panel.
	 *
	 * @return desktop
	 */
	public MainDesktopPane getDesktop() {
		return desktop;
	}

	/**
	 * Gets the Main Window Menu.
	 *
	 * @return mainWindowMenu
	 */
	public MainWindowMenu getMainWindowMenu() {
		return mainWindowMenu;
	}

	/**
	 * Performs the process of saving a simulation.
	 * Note: if defaultFile is false, displays a FileChooser to select the
	 * location and new filename to save the simulation.
	 *
	 * @param defaultFile is the default.sim file be used
	 */
	public void saveSimulation(boolean defaultFile) {
		File fileLocn = null;
		if (!defaultFile) {
			JFileChooser chooser = new JFileChooser(SimulationFiles.getSaveDir());
			chooser.setDialogTitle(Msg.getString("MainWindow.dialogSaveSim")); //$NON-NLS-1$
			if (chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION) {
				fileLocn = chooser.getSelectedFile();
			}
			else {
				return;
			}
		}

		// Request the save
		sim.requestSave(fileLocn, action -> {
			if (SimulationListener.SAVE_COMPLETED.equals(action)) {
				// Save the current main window ui config
				UIConfig.INSTANCE.saveFile(this);
//				logger.log(Level.CONFIG, "Done calling saveSimulation().");
			}
		});

		logger.log(Level.CONFIG, "Save requested"); 
	}

	/**
	 * Pauses the simulation and opens an announcement window.
	 */
	public void pauseSimulation() {
		desktop.openAnnouncementWindow("  " + Msg.getString("MainWindow.pausingSim") + "  "); //$NON-NLS-1$
		masterClock.setPaused(true, false);
	}

	/**
	 * Closes the announcement window and unpauses the simulation.
	 */
	public void unpauseSimulation() {
		masterClock.setPaused(false, false);
		desktop.disposeAnnouncementWindow();
	}

	/**
	 * Create a new unit button in toolbar.
	 *
	 * @param unit the unit the button is for.
	 */
	public void createUnitButton(Unit unit) {
		unitToolbar.createUnitButton(unit);
	}

	/**
	 * Disposes a unit button in toolbar.
	 *
	 * @param unit the unit to dispose.
	 */
	public void disposeUnitButton(Unit unit) {
		unitToolbar.disposeUnitButton(unit);
	}

	/**
	 * Exit the simulation for running and exit.
	 */
	public void exitSimulation() {
		if (!masterClock.isPaused() && !sim.isSavePending()) {
			int reply = JOptionPane.showConfirmDialog(frame,
					"Are you sure you want to exit?", "Exiting the Simulation", JOptionPane.YES_NO_CANCEL_OPTION);
	        if (reply == JOptionPane.YES_OPTION) {

	        	frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

	        	endSimulation();
	    		// Save the UI configuration.
	    		UIConfig.INSTANCE.saveFile(this);
	    		masterClock.exitProgram();
	    		frame.dispose();
	    		destroy();
	    		System.exit(0);
	        }

	        else {
	        	frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
	        }
		}
	}

	/**
	 * Ends the current simulation, closes the JavaFX stage of MainScene but leaves
	 * the main menu running
	 */
	private void endSimulation() {
		sim.endSimulation();
	}

	/**
	 * Sets the theme skin after calling stage.show() at the start of the sim
	 */
	public void initializeTheme() {
		setLookAndFeel(defaultThemeType);
	}

	/**
	 * Initialize weblaf them
	 */
	public void initializeWeblaf() {

		try {
			// use the weblaf skin
			WebLookAndFeel.install();
			UIManagers.initialize();
			// Start the weblaf icon manager
			if (iconManager == null)
				initIconManager();
		} catch (Exception e) {
			logger.log(Level.WARNING, Msg.getString("MainWindow.log.lookAndFeelError"), e); //$NON-NLS-1$
		}

	}

	/**
	 * Sets the look and feel of the UI
	 *
	 * @param choice
	 */
	public void setLookAndFeel(ThemeType choice1) {
		boolean changed = false;

		if (choice1 == ThemeType.METAL) {

			try {
				UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
				changed = true;
			} catch (Exception e) {
				logger.log(Level.WARNING, Msg.getString("MainWindow.log.lookAndFeelError"), e); //$NON-NLS-1$
			}

			initializeTheme();

		}

		else if (choice1 == ThemeType.SYSTEM) {
			try {

				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());

				changed = true;
			} catch (Exception e) {
				logger.log(Level.WARNING, Msg.getString("MainWindow.log.lookAndFeelError"), e); //$NON-NLS-1$
			}

			initializeWeblaf();
		}

		else if (choice1 == ThemeType.NIMBUS) {

			try {
				boolean foundNimbus = false;
				for (LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
					if (info.getName().equals("Nimbus")) {
						// Set Nimbus look & feel if found in JVM.

						// see https://docs.oracle.com/javase/tutorial/uiswing/lookandfeel/color.html
						UIManager.setLookAndFeel(info.getClassName());
						foundNimbus = true;
						// themeSkin = "nimbus";
						changed = true;
						 break;
					}
				}

				// Metal Look & Feel fallback if Nimbus not present.
				if (!foundNimbus) {
					logger.log(Level.WARNING, Msg.getString("MainWindow.log.nimbusError")); //$NON-NLS-1$
					UIManager.setLookAndFeel(new MetalLookAndFeel());

					changed = true;
				}
			} catch (Exception e) {
				logger.log(Level.WARNING, Msg.getString("MainWindow.log.lookAndFeelError"), e); //$NON-NLS-1$
			}

			initializeWeblaf();
		}

		// if (changed && (desktop != null)) {
		// 	desktop.updateToolWindowLF();
		// 	desktop.updateUnitWindowLF();
		// }
	}

	/**
	 * Gets the unit toolbar.
	 *
	 * @return unit toolbar.
	 */
	public UnitToolBar getUnitToolBar() {
		return unitToolbar;
	}

	/**
	 * Gets the tool toolbar.
	 *
	 * @return tool toolbar.
	 */
	public ToolToolBar getToolToolBar() {
		return toolToolbar;
	}

	public String getLookAndFeelTheme() {
		return lookAndFeelTheme;
	}

	/**
	 * Gets the main pane instance
	 *
	 * @return
	 */
	public JPanel getMainPane() {
		return mainPane;
	}

	
	/**
	 * Gets the icon instance
	 *
	 * @return
	 */
	public static Icon getIcon(String id) {
		if (iconManager == null) {
			initIconManager();
		}
		return iconManager.getIcon(id);
	}

	/**
	 * Gets the lander hab icon instance
	 *
	 * @return
	 */
	public static Icon getLanderIcon() {
		return getIcon("lander");
	}

	public static Image getIconImage() {
		// Not great
		return ((SVGIcon) getIcon("lander")).getImage();
	}


	/**
	 * Starts the splash window frame
	 */
	public static void startSplash() {
        // Create a splash window
		if (splashWindow == null) {
			splashWindow = new SplashWindow();
		}

		splashWindow.setIconImage();
        splashWindow.display();
        splashWindow.getJFrame().setCursor(new Cursor(java.awt.Cursor.WAIT_CURSOR));
        //SwingUtilities.windowForComponent(splashWindow);
        
//        splashWindow.getJFrame().setAlwaysOnTop(true);
	}

	/**
	 * Disposes the splash window frame
	 */
	public static void disposeSplash() {
		if (splashWindow != null) {
			splashWindow.remove();
		}
		splashWindow = null;
	}

	private void changeTitle(boolean isPaused) {
		if (GameManager.getGameMode() == GameMode.COMMAND) {
			if (isPaused) {
				frame.setTitle(Simulation.TITLE + "  -  Command Mode" + "  -  [ P A U S E ]");
			} else {
				frame.setTitle(Simulation.TITLE + "  -  Command Mode");
			}
		} else {
			if (isPaused) {
				frame.setTitle(Simulation.TITLE + "  -  Sandbox Mode" + "  -  [ P A U S E ]");
			} else {
				frame.setTitle(Simulation.TITLE + "  -  Sandbox Mode");
			}
		}
	}

	public boolean isIconified() {
		return isIconified;
	}

	/**
	 * Create background tile when MainDesktopPane is first displayed. Center
	 * logoLabel on MainWindow and set backgroundLabel to the size of
	 * MainDesktopPane.
	 *
	 * @param e the component event
	 */
	public void componentResized(ComponentEvent e) {
		desktop.componentResized(e);
	}
	
	/** 
	 * Get the active simualation.
	 */
	public Simulation getSimulation() {
		return sim;
	}

	@Override
	public void clockPulse(ClockPulse pulse) {
		if (pulse.getElapsed() > 0 && !isIconified) {
			// Increments the Earth and Mars clock labels.
			toolToolbar.incrementClocks(pulse.getMasterClock(), pulse.isNewSol());

			// Cascade the pulse
			desktop.clockPulse(pulse);
		}
	}

	/**
	 * Change the pause status. Called by Masterclock's firePauseChange() since
	 * TimeWindow is on clocklistener.
	 *
	 * @param isPaused true if set to pause
	 * @param showPane true if the pane will show up
	 */
	@Override
	public void pauseChange(boolean isPaused, boolean showPane) {
		changeTitle(isPaused);
		// Update pause/resume webswitch buttons, based on masterclock's pause state.
		if (isPaused) { // if it needs to pause
			// if the web switch is at the play position
			if (pauseSwitch.isSelected()) {
				// then switch it to the pause position and animate the change
				pauseSwitch.setSelected(false, true);
			}
			// Enable the overlay check box
			overlayCheckBox.setEnabled(true);
			overlayCheckBox.setSelected(true);
		}

		else { // if it needs to resume playing
			// if the web switch is at the pause position
			if (!pauseSwitch.isSelected()) {
				// then switch it to the play position and animate the change
				pauseSwitch.setSelected(true, true);
			}
			// Disable the overlay check box
			overlayCheckBox.setSelected(false);
			overlayCheckBox.setEnabled(false);
		}
	}

	public static void setInteractiveTerm(InteractiveTerm i) {
		interactiveTerm = i;
	}

	/**
	 * Prepares the panel for deletion.
	 */
	public void destroy() {
		frame = null;
		unitToolbar = null;
		toolToolbar = null;
		desktop.destroy();
		desktop = null;
		mainWindowMenu = null;
		statusBar = null;
		bottomPane = null;
		mainPane = null;
	}
}
