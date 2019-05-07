package org.planit.zoning;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import org.planit.network.virtual.Centroid;
import org.planit.utils.IdGenerator;

/**
 * Represents a demand generating zone in the network.
 * 
 * @author markr
 *
 */
public class Zone {
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(Zone.class.getName());
        
	/**
	 * Unique identifier for the zone
	 */
	protected final long id;
		
	/**
	 * generic input property storage
	 */
	protected Map<String, Object> inputProperties = null;	
	
	/**
	 * Centroid of the zone
	 */
	protected final Centroid centroid;
	
/** Generate unique zone id
 * 
 * @return 				zone id
 */
	protected static int generateZoneId() {
		return IdGenerator.generateId(Zone.class);
	}
		
	
	// Public
	
/**
 * Constructor
 * 
 * @param centroid			Centroid of this zone
 */
	public Zone(Centroid centroid){
	    this.id = centroid.getId();
		centroid.setParentZone(this);
		this.centroid = centroid;
	}	
	
/**
 * Returns the id of this zone
 * 	
 * @return 			id of this zone
 */
	public long getId() {
		return this.id;
	}
	
/**
 * Add a property from the original input that is not part of the readily available members
 * 
 * @param key			property key
 * @param value			property value
 */
	public void addInputProperty(String key, Object value) {
		if (inputProperties == null) {
			inputProperties = new HashMap<String, Object>();
		}
		inputProperties.put(key, value);
	}
	
/** 
 * Get input property by its key
 * 
 * @param key			property key
 * @return 					property value
 */
	public Object getInputProperty(String key) {
		return inputProperties.get(key);
	}	
	
/**
 * Returns the centroid of this zone
 * 
 * @return				centroid of this zone
 */
	public Centroid getCentroid() {
		return centroid;
	}
		
}
