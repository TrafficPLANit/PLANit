package org.planit.network.virtual;

import java.util.logging.Logger;
import javax.annotation.Nonnull;
import org.planit.network.Vertex;
import org.planit.utils.IdGenerator;
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
 * Generate unique centroid id
 * 
 * @return             id of this Centroid
 */
	protected static int generateCentroidId() {
		return IdGenerator.generateId(Centroid.class);
	}	
	

/**
 * Unique internal identifier 
 */	
	protected final long centroidId;

/**
 * External identifier for this centroid
 */
	protected final long externalId;
	
	/**
	 * the zone this centroid represents
	 */
	protected Zone parentZone;
			
	// Public
	
/**
 * Constructor
 * 
* @param parentZone      zone containing this centroid
* @param externalId         external Id of link connecting to this centroid
 */
	public Centroid(@Nonnull Zone parentZone, long externalId) {
		super();
		this.centroidId = parentZone.getId();
		this.parentZone = parentZone;
		this.externalId = externalId;
	}	
	
/**
 * Constructor
 * 
 * @param externalId     external Id of link connecting to this centroid
 */
	public Centroid(long externalId) {
		super();
		this.centroidId = generateCentroidId();
		this.parentZone = null;
		this.externalId = externalId;
	}
	
/**
 * Constructor
 */
	public Centroid() {
		super();
		this.centroidId = generateCentroidId();
		this.parentZone = null;
		this.externalId = 0;
	}
	
	// Getters-Setters
	
/**
 * Return the id of this centroid
 * 
 * @return     id of this centroid
 */
	public long getCentroidId() {
		return centroidId;
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
	
/**
 * Return external Id of link connecting to this centroid
 * 
 * @return      external id of link connecting to this centroid
 */
	public long getExternalId() {
		return externalId;
	}

}
