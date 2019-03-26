package org.planit.network.virtual;

import javax.annotation.Nonnull;

import org.planit.network.Vertex;
import org.planit.utils.IdGenerator;
import org.planit.zoning.Zone;

public class Centroid extends Vertex {
	// Protected
	
	/** generate unique centroid id
	 * @return nodeId
	 */
	protected static int generateCentroidId() {
		return IdGenerator.generateId(Centroid.class);
	}	
	

	/**
	 * Unique internal identifier 
	 */	
	protected final long centroidId;
	
	protected final long geometryId;
	
	/**
	 * the zone this centroid represents
	 */
	protected Zone parentZone;
			
	// Public
	
	/**
	 * Node constructor
	 * @param id
	 */
	public Centroid(@Nonnull Zone parentZone, long geometryId) {
		super();
		this.centroidId = generateCentroidId();
		this.parentZone = parentZone;
		this.geometryId = geometryId;
	}	
	
	public Centroid(long geometryId) {
		super();
		this.centroidId = generateCentroidId();
		this.parentZone = null;
		this.geometryId = geometryId;
	}
	
	public Centroid() {
		super();
		this.centroidId = generateCentroidId();
		this.parentZone = null;
		this.geometryId = 0;
	}
	
	// Getters-Setters
	public long getCentroidId() {
		return centroidId;
	}
	
	public Zone getParentZone() {
		return this.parentZone;
	}
	
	public void setParentZone(Zone parentZone) {
		this.parentZone = parentZone;
	}
	
	public long getGeometryId() {
		return geometryId;
	}

}
