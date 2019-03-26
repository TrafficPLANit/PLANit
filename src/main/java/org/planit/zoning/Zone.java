package org.planit.zoning;

import java.util.HashMap;
import java.util.Map;

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
	
	/** generate unique link segment id
	 * @return linkId
	 */
	protected static int generateZoneId() {
		return IdGenerator.generateId(Zone.class);
	}
		
	
	// Public
	
	/**
	 * Constructor
	 */
	public Zone(Centroid centroid){
		this.id = generateZoneId();
		centroid.setParentZone(this);
		this.centroid = centroid;
	}	
	
	
	public long getId() {
		return this.id;
	}
	
	/**
	 * Add a property from the original input that is not part of the readily available members
	 * @param key
	 * @param value
	 */
	public void addInputProperty(String key, Object value) {
		if (inputProperties == null) {
			inputProperties = new HashMap<String, Object>();
		}
		inputProperties.put(key, value);
	}
	
	/** Get input property by its key
	 * @param key
	 * @return value
	 */
	public Object getInputProperty(String key) {
		return inputProperties.get(key);
	}	
	
	public Centroid getCentroid() {
		return centroid;
	}
		
}
