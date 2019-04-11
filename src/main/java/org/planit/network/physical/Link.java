package org.planit.network.physical;

import javax.annotation.Nonnull;

import org.opengis.geometry.coordinate.LineString;
import org.planit.exceptions.PlanItException;
import org.planit.network.Edge;
import org.planit.utils.IdGenerator;

/** 
 * Link class connecting two nodes via some geometry. Each link has one or two underlying link segments in a particular direction
 * which may carry additional information for each particular direction of the link.
 * @author markr
 *
 */
public class Link extends Edge {
	
		
	// Protected
	
	/**
	 * unique internal identifier 
	 */
	protected final long linkId;	
	
	/**
	 * Centre-line geometry of the link, including the end node positions
	 */
	protected LineString centreLineGeometry = null; 	

	
	/** generate unique link id
	 * @return linkId
	 */
	protected static long generateLinkId() {
		return IdGenerator.generateId(Link.class);
	}	
	
	// Public
	
	/**
	 * Constructor which injects link length directly
	 * @throws PlanItException 
	 */
	public Link(@Nonnull Node nodeA,@Nonnull Node nodeB, double length) throws PlanItException
	{
		super(nodeA, nodeB, length);
		this.linkId = generateLinkId();
	}
	
	/** Register linkSegment. If there already exists a linkSegment for that direction it is replaced and returned
	 * @param linkSegment
	 * @param directionAB
	 * @return replacedLinkSegment
	 * @throws PlanItException 
	 */
	public LinkSegment registerLinkSegment(LinkSegment linkSegment, boolean directionAB) throws PlanItException {
		return (LinkSegment) registerEdgeSegment(linkSegment, directionAB);
	}
	
	// Getters-Setters

	public long getLinkId() {
		return linkId;
	}	

	public LineString getCentreLineGeometry() {
		return centreLineGeometry;
	}
	public void setCentreLineGeometry(LineString centreLineGeometry) {
		this.centreLineGeometry = centreLineGeometry;
	}
}
