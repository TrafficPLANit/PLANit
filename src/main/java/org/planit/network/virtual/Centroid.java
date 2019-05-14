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
 * Row/column of the demand matrix which this centroid/zone represents
 */ 
    protected final long odPos;

    /**
     * the zone this centroid represents
     */
    protected final Zone parentZone;
            
    // Public
    
/**
 * Constructor
 * 
 * @param odPos     row/column in the OD matrix which this centroid/zone corresponds to
 */
    public Centroid(Zone parentZone, long odPos) {
        super();
        this.parentZone = parentZone;
        this.odPos = odPos;
    }
    
    // Getters-Setters
    
    public long getOdPos() {
        return odPos;
    }
    
/**
 * Return the parent zone of this centroid
 * 
 * @return        parent zone of this centroid
 */
    public Zone getParentZone() {
        return this.parentZone;
    }
    
}