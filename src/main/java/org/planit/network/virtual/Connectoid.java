package org.planit.network.virtual;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.geo.utils.PlanitGeoUtils;
import org.planit.network.Edge;
import org.planit.network.physical.Node;
import org.planit.utils.IdGenerator;

/** 
 * connectoid connecting a zone to the physical road network, carrying two connectoid segments in one or two directions
 * which may carry additional information for each particular direction of the connectoid.
 * @author markr
 *
 */
public class Connectoid extends Edge {
	
		
	// Protected
	
	/**
	 * unique internal identifier 
	 */
	protected final long connectoidId;	
	
	/** generate unique link id
	 * @return linkId
	 */
	protected static int generateConnectoidId() {
		return IdGenerator.generateId(Connectoid.class);
	}	
	
	// Public
	
	/**
	 * Constructor
	 * @throws PlanItException 
	 */
	public Connectoid(@Nonnull Centroid centroidA, @Nonnull Node nodeB, PlanitGeoUtils planitGeoUtils) throws PlanItException
	{
		super(centroidA, nodeB, planitGeoUtils);
		this.connectoidId = generateConnectoidId();
	}
	
	public Connectoid(@Nonnull Centroid centroidA, @Nonnull Node nodeB, double length) throws PlanItException {
		super(centroidA, nodeB, length);
		this.connectoidId = generateConnectoidId();
	}
		
	
	/** Register connectoidSegment. If there already exists a connectoidSegment for that direction it is replaced and returned
	 * @param connectoidSegment
	 * @param directionAB
	 * @return replacedConnectoidSegment
	 * @throws PlanItException 
	 */
	public ConnectoidSegment registerConnectoidSegment(ConnectoidSegment connectoidSegment, boolean directionAB) throws PlanItException {
		return (ConnectoidSegment) registerEdgeSegment(connectoidSegment, directionAB);
	}
			
	// Getters-Setters

	public long getConnectoidId() {
		return connectoidId;
	}	
}
