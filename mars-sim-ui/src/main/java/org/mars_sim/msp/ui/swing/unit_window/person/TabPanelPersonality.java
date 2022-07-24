/*
 * Mars Simulation Project
 * TabPanelPersonality.java
 * @date 2022-07-13
 * @author Manny Kung
 */
package org.mars_sim.msp.ui.swing.unit_window.person;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;
import javax.swing.border.TitledBorder;

import org.mars_sim.msp.core.Msg;
import org.mars_sim.msp.core.Unit;
import org.mars_sim.msp.core.person.Person;
import org.mars_sim.msp.core.person.ai.MBTIPersonality;
import org.mars_sim.msp.core.person.ai.PersonalityTraitManager;
import org.mars_sim.msp.core.person.ai.PersonalityTraitType;
import org.mars_sim.msp.ui.swing.ImageLoader;
import org.mars_sim.msp.ui.swing.MainDesktopPane;
import org.mars_sim.msp.ui.swing.MarsPanelBorder;
import org.mars_sim.msp.ui.swing.unit_window.TabPanel;

import com.alee.laf.label.WebLabel;
import com.alee.laf.panel.WebPanel;
import com.alee.laf.text.WebTextArea;
import com.alee.managers.style.StyleId;

/**
 * The TabPanelPersonality is a tab panel for personality information about a person.
 */
@SuppressWarnings("serial")
public class TabPanelPersonality extends TabPanel {

	private static final String PUZZLE_ICON = Msg.getString("icon.puzzle"); //$NON-NLS-1$

	private static final Font RESULT_FONT = new Font("Monospaced", Font.PLAIN, 18);
	private static final Font MONOSPACED_PLAIN_12 = new Font("Monospaced", Font.PLAIN, 12);

	/** The Person instance. */
	private Person person;

	/**
	 * Constructor.
	 * 
	 * @param unit the unit to display.
	 * @param desktop the main desktop.
	 */
	public TabPanelPersonality(Unit unit, MainDesktopPane desktop) {
		
		// Use the TabPanel constructor
		super(
			Msg.getString("TabPanelPersonality.title"), //$NON-NLS-1$
			ImageLoader.getNewIcon(PUZZLE_ICON),		
			Msg.getString("TabPanelPersonality.title"), //$NON-NLS-1$
			unit, desktop
		);

		person = (Person) unit;
	}
	
	@Override
	protected void buildUI(JPanel content) {

		// Prepare spring layout info panel.
		JPanel infoPanel = new JPanel(new SpringLayout());
		content.add(infoPanel, BorderLayout.NORTH);

		JPanel scorePanel = new JPanel(new BorderLayout(1, 1));
		content.add(scorePanel, BorderLayout.CENTER);
		
		// Create the text area for displaying the Big Five scores
		scorePanel.add(createBigFive(), BorderLayout.NORTH);
		// Create the text area for displaying the MBTI scores
		scorePanel.add(createMBTI(person.getMind().getMBTI()), BorderLayout.CENTER);
	}
	
	
	/**
	 * Creates the MBTI text area.
	 * 
	 * @param p an instance of MBTIPersonality
	 * @param content
	 * @return
	 */
	private JPanel createMBTI(MBTIPersonality p) {
		
		int ie = p.getIntrovertExtrovertScore();
		int ns = p.getScores().get(1);
		int ft = p.getScores().get(2);
		int jp = p.getScores().get(3);

		
		String[] types = new String[4];
		int[] scores = new int[4];
		
		// Prepare MBTI text area
		WebTextArea ta = new WebTextArea();
		ta.setAlignmentX(Component.CENTER_ALIGNMENT);
		ta.setAlignmentY(Component.CENTER_ALIGNMENT);
		String tip =  "<html>Introvert (I) / Extrovert  (E) : -50 to 0 / 0 to 50" 
					  + "<br>Intuitive (N) / Sensing    (S) : -50 to 0 / 0 to 50"
					  + "<br>  Feeling (F) / Thinking   (T) : -50 to 0 / 0 to 50"
					  + "<br>  Judging (J) / Perceiving (P) : -50 to 0 / 0 to 50</html>";
		ta.setToolTipText(tip);
		ta.setEditable(false);
		ta.setSelectedTextColor(Color.ORANGE.darker());
		ta.setFont(MONOSPACED_PLAIN_12);
		ta.setColumns(15);
		ta.setBorder(new MarsPanelBorder());
		
		WebPanel mbtiPanel = new WebPanel(new FlowLayout(FlowLayout.CENTER));
		mbtiPanel.add(ta);
		
		WebPanel listPanel = new WebPanel(new BorderLayout(1, 1));
		listPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
		listPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		listPanel.setPreferredSize(110, 90);
		listPanel.setMaximumSize(110, 110);
		listPanel.add(mbtiPanel, BorderLayout.NORTH);
		
		String type = p.getTypeString();
		String descriptor = p.getDescriptor();
		WebLabel descriptorLabel = new WebLabel(StyleId.labelShadow, type + " : " + descriptor, JLabel.CENTER);
		descriptorLabel.setToolTipText(Msg.getString("TabPanelPersonality.mbti.descriptor.tip"));//$NON-NLS-1$
		descriptorLabel.setFont(RESULT_FONT);
		listPanel.add(descriptorLabel, BorderLayout.CENTER);
		
		TitledBorder titledBorder = BorderFactory.createTitledBorder(
				null, " Myers & Briggs Type Indicator",
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
				SUBTITLE_FONT_1, Color.darkGray);
		listPanel.setBorder(titledBorder);
		
		
		if (ie < 0) {
			types[0] = "Introvert (I)";
			scores[0] = ie;
		}
		else {
			types[0] = "Extrovert (E)";
			scores[0] = ie;
		}
		
		if (ns < 0) {
			types[1] = "Intuitive (N)";
			scores[1] = ns;
		}
		else {
			types[1] = "Sensing (S)";
			scores[1] = ns;
		}
		
		if (ft < 0) {
			types[2] = "Feeling (F)";
			scores[2] = ft;
		}
		else {
			types[2] = "Thinking (T)";
			scores[2] = ft;
		}
		
		if (jp < 0) {
			types[3] = "Judging (J)";
			scores[3] = jp;
		}
		else {
			types[3] = "Perceiving (P)";
			scores[3] = jp;
		}
	
		for (int i = 0; i < 4; i++) {
			String s = types[i];
			int size = 14 - s.length();
			while (size > 0) {
				ta.append(" ");
				size--;
			}
			ta.append(" " + s + " : " + scores[i] + "  ");
			if (i < 3)
				//if it's NOT the last one
				ta.append("\n");
		}
		
		return listPanel;
	}

	/**
	 * Creates the text area for the Big Five.
	 * 
	 * @param content
	 * @return
	 */
	private JPanel createBigFive() {
		PersonalityTraitManager p = person.getMind().getTraitManager();
		
		String[] types = new String[5];
		int[] scores = new int[5];
		
    	for (PersonalityTraitType t : PersonalityTraitType.values()) {
    		types[t.ordinal()] = t.getName();
    		scores[t.ordinal()] = p.getPersonalityTrait(t);
    	}
    	
		// Prepare MBTI text area
		WebTextArea ta = new WebTextArea();
		ta.setAlignmentX(Component.CENTER_ALIGNMENT);
		ta.setAlignmentY(Component.CENTER_ALIGNMENT);
		ta.setEditable(false);
		ta.setFont(MONOSPACED_PLAIN_12);
		ta.setSelectedTextColor(Color.ORANGE.darker());
		ta.setColumns(15);
		ta.setBorder(new MarsPanelBorder());
		
		String tip =  "<html>          Openness : 0 to 100" 
				  	  + "<br> Conscientiousness : 0 to 100"
				  	  + "<br>      Extraversion : 0 to 100"
				  	  + "<br>       Neuroticism : 0 to 100</html>";
		ta.setToolTipText(tip);
	
		WebPanel listPanel = new WebPanel(new FlowLayout(FlowLayout.CENTER));
		listPanel.setAlignmentY(Component.CENTER_ALIGNMENT);
		listPanel.setToolTipText(Msg.getString("TabPanelPersonality.bigFive.label"));//$NON-NLS-1$
		listPanel.setPreferredSize(110, 160);
		listPanel.add(ta);

		TitledBorder titledBorder = BorderFactory.createTitledBorder(null, " Big Five Personality Traits",
				TitledBorder.DEFAULT_JUSTIFICATION, TitledBorder.DEFAULT_POSITION,
				SUBTITLE_FONT_1, Color.darkGray);
		listPanel.setBorder(titledBorder);
		
		for (int i = 0; i < 5; i++) {
			String s = types[i];
			int size = 18 - s.length();
			while (size > 0) {
				ta.append(" ");
				size--;
			}
			ta.append(" " + s + " : " + scores[i] + "  ");
			if (i < 4)
				//if it's NOT the last one
				ta.append("\n");
		}
		
		return listPanel;
	}
	
	/**
	 * Updates the info on this panel.
	 */
	@Override
	public void update() {
	}
}