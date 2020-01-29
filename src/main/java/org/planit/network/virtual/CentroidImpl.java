package org.planit.network.virtual;

import org.planit.graph.VertexImpl;
import org.planit.utils.network.virtual.Centroid;
import org.planit.utils.network.virtual.Zone;

/**
 * Centroid implementation
 * 
 * @author gman6028
 *
 */
public class CentroidImpl extends VertexImpl implements Centroid {
    
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
    public CentroidImpl(Zone parentZone) {
        super();
        this.parentZone = parentZone;
    }
    
    // Getters-Setters

    /**
     * Return the parent zone of this centroid
     * 
     * @return parent zone of this centroid
     */
    @Override
	public Zone getParentZone() {
        return this.parentZone;
    }

}