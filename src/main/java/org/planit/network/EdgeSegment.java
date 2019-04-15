package org.planit.network;

import org.planit.utils.IdGenerator;

/** EdgeSegment represents an edge in a particular (single) direction. Each edge has either one or two edge segments
 * where each edge segment may have a more detailed geography than its parent link (which represents both directions via a centreline)
 * @author markr
 *
 */
public class EdgeSegment implements Comparable<EdgeSegment> {

	/**
	 * unique internal identifier 
	 */
	protected final long id;
	
	/**
	 * segment's parent edge
	 */
	protected final Edge parentEdge;
	
	/**
	 * the upstreamVertex of the edge segment
	 */
	protected final Vertex upstreamVertex;
	
	/**
	 * The downstream vertex of this edge segment
	 */
	protected final Vertex downstreamVertex;
	
/** 
 * Generate unique edge segment id
 * 
 * @return id      id of this EdgeSegment
 */
	protected static int generateEdgeSegmentId() {
		return IdGenerator.generateId(EdgeSegment.class);
	}		
	
	// Public
	
/**
 * Constructor
 * 
 * @param parentEdge        parent edge of segment
 * @param directionAB        direction of travel
 */
	protected EdgeSegment(Edge parentEdge, boolean directionAB)
	{
		this.id = generateEdgeSegmentId();
		this.parentEdge = parentEdge;
		this.upstreamVertex = directionAB ? parentEdge.getVertexA() : parentEdge.getVertexB();
		this.downstreamVertex = directionAB ? parentEdge.getVertexB() : parentEdge.getVertexA();
	}
		
	// Public 
		
/** 
 * Get the segment's upstream vertex
 * 
 * @return              upstream vertex
 */
	public Vertex getUpstreamVertex(){
		return upstreamVertex;
	}
	
/** 
 * Get the segment's downstream vertex
 * 
 * @return                                 downstream vertex
 */
	public Vertex getDownstreamVertex(){
		return downstreamVertex;
	}	
	
	// Getter - Setters

	public long getId() {
		return this.id;
	}		
		
	public Edge getParentEdge() {
		return this.parentEdge;
	}
	
	/** compare based on edge segment id
	 * @see java.lang.Comparable#compareTo(java.lang.Object)
	 */
	@Override
	public int compareTo(EdgeSegment o) {
		return (int) (id - o.id);
	}
}
