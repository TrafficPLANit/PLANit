package org.planit.network.virtual;

import java.util.logging.Logger;
import org.planit.network.Vertex;
import org.planit.zoning.Zone;

/**
 * Centroid object
 * 
 * @author gman6028
 *
 */
public class Centroid extends Vertex {
	// Protected
	
    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(Centroid.class.getName());
        
/**
 * Identifier of parent zone of this centroid
 */	
	protected final long zoneId;

	/**
	 * the zone this centroid represents
	 */
	protected Zone parentZone;
			
	// Public
	
/**
 * Constructor
 * 
 * @param zoneId     id of the parent zone of this centroid
 */
	public Centroid(long zoneId) {
		super();
		this.zoneId = zoneId;
//		this.parentZone = null;
	}
	
	// Getters-Setters
	
	public long getZoneId() {
	    return zoneId;
	}
	
/**
 * Return the parent zone of this centroid
 * 
 * @return        parent zone of this centroid
 */
	public Zone getParentZone() {
		return this.parentZone;
	}
	
/**
 * Set the parent zone of this centroid
 * 
 * @param parentZone         parent zone of this centroid
 */
	public void setParentZone(Zone parentZone) {
		this.parentZone = parentZone;
	}
	
}