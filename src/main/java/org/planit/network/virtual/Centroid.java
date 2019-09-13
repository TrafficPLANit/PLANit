package org.planit.network.virtual;

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
     * the zone this centroid represents
     */
    protected final Zone parentZone;

    // Public

    /**
     * Constructor
     * 
     * @param parentZone
     *            the parent zone of this Centroid
     */
    public Centroid(Zone parentZone) {
        super();
        this.parentZone = parentZone;
    }
    
    // Getters-Setters

    /**
     * Return the parent zone of this centroid
     * 
     * @return parent zone of this centroid
     */
    public Zone getParentZone() {
        return this.parentZone;
    }

}