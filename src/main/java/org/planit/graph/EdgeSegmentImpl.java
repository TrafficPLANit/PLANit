package org.planit.graph;

import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.Edge;
import org.planit.utils.network.EdgeSegment;
import org.planit.utils.network.Vertex;

/**
 * EdgeSegment represents an edge in a particular (single) direction. Each edge
 * has either one or two edge segments where each edge segment may have a more
 * detailed geography than its parent link (which represents both directions via
 * a centreline)
 * 
 * This class is now abstract.  It is extended by LinkSegment (physical links) and Connectoid (virtual links).
 * 
 * @author markr
 *
 */
public abstract class EdgeSegmentImpl implements EdgeSegment {

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
	 * The external Id for this link segment type
	 */
	protected long externalId;

	/**
	 * Flag to indicate whether an external Id has been set for this edge segment
	 */
	protected boolean externalIdSet;
	
   /**
     * Generate unique edge segment id
     * 
     * @return id id of this EdgeSegment
     */
    protected static int generateEdgeSegmentId() {
        return IdGenerator.generateId(EdgeSegment.class);
    }

    // Public

    /**
     * Constructor
     * 
     * @param parentEdge  parent edge of segment
     * @param directionAB  direction of travel
     */
    protected EdgeSegmentImpl(Edge parentEdge, boolean directionAB) {
        this.id = generateEdgeSegmentId();
        this.parentEdge = parentEdge;
        this.upstreamVertex = directionAB ? parentEdge.getVertexA() : parentEdge.getVertexB();
        this.downstreamVertex = directionAB ? parentEdge.getVertexB() : parentEdge.getVertexA();
        externalIdSet = false;
    }
    
    // Public

    /**
     * Get the segment's upstream vertex
     * 
     * @return upstream vertex
     */
    @Override
    public Vertex getUpstreamVertex() {
        return upstreamVertex;
    }

    /**
     * Get the segment's downstream vertex
     * 
     * @return downstream vertex
     */
    @Override
    public Vertex getDownstreamVertex() {
        return downstreamVertex;
    }

    // Getter - Setters

    /**
     * Unique id of the edge segment
     * 
     * @return id
     */
    @Override
    public long getId() {
        return this.id;
    }

    /**
     * parent edge of the segment
     * 
     * @return parentEdge
     */
    @Override
    public Edge getParentEdge() {
        return this.parentEdge;
    }
    
    /**
     * set external id of the instance. Note that this id need not be unique (unlike regular id)
     * @param externalId
     */
    @Override
	public void setExternalId(long externalId) {
		this.externalId = externalId;
		externalIdSet = true;
	}
	
    /**
     * Does the instance have an external id
     * @return true when available, false otherwise
     */
    @Override
	public boolean hasExternalId() {
		return externalIdSet;
	}
	
    /**
     * Get external id of the instance. Note that this id need not be unique (unlike regular id)
     * @return externalId
     */
    @Override
	public long getExternalId() {
		return externalId;
	}

   /**
     * compare based on edge segment id
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(EdgeSegment o) {
        return (int) (id - o.getId());
    }        

}