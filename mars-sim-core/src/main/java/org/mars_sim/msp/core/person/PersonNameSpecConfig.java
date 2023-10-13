/*
 * Mars Simulation Project
 * PersonNameSpecConfig.java
 * @date 2023-07-23
 * @author Barry Evans
 */
package org.mars_sim.msp.core.person;

import java.util.ArrayList;
import java.util.List;

import org.jdom2.Document;
import org.jdom2.Element;
import org.mars_sim.msp.core.configuration.UserConfigurableConfig;
import org.mars_sim.msp.core.reportingAuthority.Nation;

/**
 * Configuration class to load person naming schemes unique to each country.
 */
public class PersonNameSpecConfig extends UserConfigurableConfig<PersonNameSpec> {

	private final String COUNTRY_XSD = "country.xsd";
	
	private static final String COUNTRY = "country";

	private final String MALE = "male";
	private final String FEMALE = "female";
	private final String LAST_NAME_LIST = "last-name-list";
	private final String FIRST_NAME_LIST = "first-name-list";
	private final String LAST_NAME = "last-name";
	private final String FIRST_NAME = "first-name";
	private final String GENDER = "gender";
    private final String NAME = "name";
    private final String VALUE = "value";

    // Note: each of the predefined country below has a xml file
    private String[] COUNTRIES = {
    					"Austria",  "Belgium", "Brazil", 
    					"Canada", "China", "Czech Republic",
                        "Denmark", "Estonia", "Finland", "France", 
                        "Germany", "Greece",
                        "Hungary", "India", "Ireland", "Italy", 
                        "Japan", "Luxembourg",
                        "Norway", "Poland", "Portugal", 
                        "Romania", "Russia",
                        "Saudi Arabia",
                        "South Korea", "Spain", "Sweden", "Switzerland", 
                        "Netherlands",
                        "United Arab Emirates",
                        "United Kingdom", "United States"};

	private static List<Nation> nations = new ArrayList<>();
	
    public PersonNameSpecConfig() {
        super(COUNTRY);

        setXSDName(COUNTRY_XSD);

        for (String name: COUNTRIES) {
        	Nation nation = new Nation(name);
        	nations.add(nation);
        }
        
        loadDefaults(COUNTRIES);  
    }

    @Override
    protected Document createItemDoc(PersonNameSpec item) {
        throw new UnsupportedOperationException("Unimplemented method 'createItemDoc'");
    }

    @Override
    protected PersonNameSpec parseItemXML(Document doc, boolean predefined) {

		Element countryElement = doc.getRootElement();

		String country = countryElement.getAttributeValue(NAME);
		PersonNameSpec result = new PersonNameSpec(country, predefined);

        // Scan first names
        Element firstNameEl = countryElement.getChild(FIRST_NAME_LIST);
        List<Element> firstNamesList = firstNameEl.getChildren(FIRST_NAME);
        for (Element nameElement : firstNamesList) {

            String gender = nameElement.getAttributeValue(GENDER);
            String name = nameElement.getAttributeValue(VALUE);

            if (gender.equalsIgnoreCase(MALE)) {
                result.addMaleName(name);
            } else if (gender.equalsIgnoreCase(FEMALE)) {
                result.addFemaleName(name);
            }
        }

        // Scan last names
        Element lastNameEl = countryElement.getChild(LAST_NAME_LIST);
        List<Element> lastNamesList = lastNameEl.getChildren(LAST_NAME);
        for (Element nameElement : lastNamesList) {
            result.addLastName(nameElement.getAttributeValue(VALUE));
        }

        return result;
    }
    
    /**
     * Gets a list of nations.
     * 
     * @return
     */
    public static List<Nation> getNations() {
    	return nations;
    }
    
    /**
     * Gets the nation with a particular name.
     * 
     * @param name
     * @return
     */
    public static Nation getNation(String name) {
    	for (Nation n: nations) {
    		if (n.getName().equalsIgnoreCase(name)) {
    			return n;
    		}
    	}
    	return null;
    }
    
}
