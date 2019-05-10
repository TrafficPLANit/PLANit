package org.planit.network.virtual;

import java.util.logging.Logger;
import javax.annotation.Nonnull;

import org.opengis.geometry.DirectPosition;
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
	 * the zone this centroid represents
	 */
	protected Zone parentZone;
	
	/**
	 * Location of the Centroid
	 */
	protected DirectPosition location = null;
			
	// Public
	
    /**
     * Constructor
     * 
    * @param parentZone      zone containing this centroid
    * @param externalId      external Id of link connecting to this centroid
     */
	public Centroid(@Nonnull Zone parentZone) {
		super();
		this.parentZone = parentZone;
	}	
	
    /**
     * Constructor
     * 
    * @param parentZone      zone containing this centroid
    * @param externalId      external Id of link connecting to this centroid
     */
    public Centroid(@Nonnull Zone parentZone, DirectPosition location) {
        super();
        this.parentZone = parentZone;
        this.location = location;
    }   	
	
		
	// Getters-Setters
	
    /**
     * Return the parent zone of this centroid
     * 
     * @return        parent zone of this centroid
     */
	public Zone getParentZone() {
		return this.parentZone;
	}
	
	/**
	 * When we want to obtain the internal origin/destination id that relates to the centroid collect the zone id and
	 * not the centroid's internal id (as thi is a vertex id shared across all vertices (nodes and centroids).
	 * @return
	 */
	public long getZoneId() {
	    return getParentZone().getId();
	}
	
    /**
     * When we want to obtain the external origin/destination id that relates to the centroid collect the external zone id
     * @return
     */
    public long getExternalZoneId() {
        return getParentZone().getExternalId();
    }	
	
    public DirectPosition getLocation() {
        return location;
    }

    public void setLocation(DirectPosition location) {
        this.location = location;
    }	
		

}
