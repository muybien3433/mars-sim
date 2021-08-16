package org.mars_sim.msp.core.configuration;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.XMLConstants;

import org.apache.commons.io.FileUtils;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.mars_sim.msp.core.SimulationConfig;
import org.mars_sim.msp.core.SimulationFiles;

/**
 * This is a manager class of a catagory of UserConfigurable class.
 * It provides a means to manage a static collection of them and read/write
 * to file system
 */
public abstract class UserConfigurableConfig<T extends UserConfigurable> {

	private static final Logger logger = Logger.getLogger(UserConfigurableConfig.class.getName());
	private static final String BACKUP = ".bak";
	
	private String itemPrefix;
	private Map<String,T> knownItems = new HashMap<>();

	protected UserConfigurableConfig(String itemPrefix) {
		this.itemPrefix = itemPrefix;
		
		// Scan saved items folder
		File savedDir = new File(SimulationFiles.getSaveDir());
	    String[] list = savedDir.list();
	    for (String userFile : list) {
	    	if (userFile.startsWith(itemPrefix)
	    			&& userFile.endsWith(SimulationConfig.XML_EXTENSION)) {
	    		loadItem(userFile, false);
	    	}
		}
	}

	/**
	 * Load a create from external or bundled XML.
	 * @param name
	 * @return
	 */
	protected void loadItem(String file, boolean predefined) {
		
		Document doc = parseXMLFileAsJDOMDocument(file, predefined);
		if (doc == null) {
			throw new IllegalStateException("Can not find " + file);
		}
		
		T result = parseItemXML(doc, predefined);
		knownItems.put(result.getName(), result);
	}
	
	/**
	 * Get a item by it's name
	 * @param name
	 * @return
	 */
	public T getItem(String name) {
		return knownItems.get(name);
	}
	
	/**
	 * Delete the item.
	 * @param name
	 */
	public void deleteItem(String name) {
		knownItems.remove(name);

		String filename = getItemFilename(name);
		File oldFile = new File(SimulationFiles.getSaveDir(), filename + SimulationConfig.XML_EXTENSION);
		logger.info("Deleting file " + oldFile.getAbsolutePath());
		oldFile.delete();
	}
	

	/**
	 * Gte teh filename for an item based on it's name
	 * @param crewName
	 * @return
	 */
	private String getItemFilename(String name) {
		// Replace spaces 
		return itemPrefix + name.toLowerCase().replace(' ', '_');
	}
	
	/**
	 * Parses an XML file into a DOM document.
	 * 
	 * @param filename the path of the file.
	 * @param useDTD   true if the XML DTD should be used.
	 * @return DOM document
	 * @throws IOException
	 * @throws JDOMException
	 * @throws Exception     if XML could not be parsed or file could not be found.
	 */
	private Document parseXMLFileAsJDOMDocument(String filename, boolean useDTD) {
		SAXBuilder builder = null;
		String path = "";
		
		if (useDTD) { // for alpha crew
			builder = new SAXBuilder();
		    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			path = SimulationFiles.getXMLDir();
			
			// Alpha is a bundled XML so needs to be copied out
			SimulationConfig.instance().getBundledXML(filename);
			filename += SimulationConfig.XML_EXTENSION;
		}
		else { // for beta crew
			builder = new SAXBuilder();
		    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_DTD, "");
		    builder.setProperty(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
			path = SimulationFiles.getSaveDir();
		}

	    Document document = null;
	    
		File f = new File(path, filename);
		if (!f.exists()) {
			return null;
		}
		
		if (f.exists() && f.canRead()) {
	        
	        try {
		        document = builder.build(f);
		    }
		    catch (JDOMException | IOException e)
		    {
		        e.printStackTrace();
		    }
		}
		
	    return document;
	}

	
	/**
	 * Save the XML document for this item.
	 * 
	 * @param item The details to save
	 */
	public void saveItem(T item) {

		String filename = getItemFilename(item.getName());
		File itemFile = new File(SimulationFiles.getSaveDir(), filename + SimulationConfig.XML_EXTENSION);
		
		// Create save directory if it doesn't exist.
		if (!itemFile.getParentFile().exists()) {
			itemFile.getParentFile().mkdirs();
			logger.config(itemFile.getParentFile().getAbsolutePath() + " created successfully."); 
		}
		
		if (itemFile.exists()) {
			File itemBackup = new File(SimulationFiles.getSaveDir(), filename + BACKUP);
			try {
				if (Files.deleteIfExists(itemBackup.toPath())) {
					// Delete the beta_crew.bak
				    logger.config("Old " + itemBackup.getName() + " deleted."); 
				} 
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			try {
				// Back up the previous version of beta_crew.xml as beta_crew.bak
				FileUtils.moveFile(itemFile, itemBackup);
			    logger.config(itemFile.getName() + " --> " + itemBackup.getName()); 
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			
			try {
				if (Files.deleteIfExists(itemFile.toPath())) {
				    logger.config("Old " + itemFile.getName() + " deleted."); 
				} 

			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		if (!itemFile.exists()) {
			Document outputDoc = createItemDoc(item);
			XMLOutputter fmt = new XMLOutputter();
			fmt.setFormat(Format.getPrettyFormat());
				
			try (FileOutputStream stream = new FileOutputStream(itemFile);
				 OutputStreamWriter writer = new OutputStreamWriter(stream, "UTF-8")) {						 
				fmt.output(outputDoc, writer);
			    logger.config("New " + itemFile.getName() + " created and saved."); 
			    stream.close();
			} catch (Exception e) {
				logger.log(Level.SEVERE, e.getMessage());
			}
		}
		
		// Update or register new crew
		knownItems.put(item.getName(), item);
	}
	
	/**
	 * Get the names of the known items.
	 * @return Alphabetically sorted names.
	 */
	public List<String> getItemNames() {
		List<String> names = new ArrayList<>(knownItems.keySet());
		Collections.sort(names);
		return names;
	}
	
	/**
	 * Convert an Item into am XML representation
	 * @param item Item to parse.
	 * @see #parseXML(Document, boolean)
	 * @return
	 */
	protected abstract Document createItemDoc(T item);

	/**
	 * Parse an XML document to create an item instance.
	 * @param doc Document of details
	 * @param predefined Is this item predefined or user defined.
	 * @see #createItemDoc(UserConfigurable)
	 * @return
	 */
	abstract protected T parseItemXML(Document doc, boolean predefined);
}
