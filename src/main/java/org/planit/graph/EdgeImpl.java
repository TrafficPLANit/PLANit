package org.planit.graph;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.misc.IdGenerator;

/**
 * Edge class connecting two vertices via some geometry. Each edge has one or
 * two underlying edge segments in a particular direction which may carry
 * additional information for each particular direction of the edge.
 * 
 * @author markr
 *
 */
public class EdgeImpl implements Edge {

    // Protected

    /**
     * Unique internal identifier
     */
    protected final long id;
    
    /**
     * Generic input property storage
     */
    protected Map<String, Object> inputProperties = null;
    
    /**
     * Name of the edge
     */
    protected String name = null;
    
    /**
     * Vertex A
     */
    protected Vertex vertexA = null;
    
    /**
     * Vertex B
     */
    protected Vertex vertexB = null;
    
    /**
     * Length of edge
     */
    protected double length;

    /**
     * Edge segment A to B direction
     */
    protected EdgeSegment edgeSegmentAB = null;
    /**
     * Edge segment B to A direction
     */
    protected EdgeSegment edgeSegmentBA = null;

    /**
     * Generate edge id
     * 
     * @return id of this Edge object
     */
    protected static long generateEdgeId() {
        return IdGenerator.generateId(Edge.class);
    }

    // Public

    /**
     * Constructor which injects link lengths directly
     * 
     * @param vertexA first vertex in the link
     * @param vertexB second vertex in the link
     * @param length length of the link
     * @throws PlanItException thrown if there is an error
     */
    protected EdgeImpl(@Nonnull Vertex vertexA, @Nonnull Vertex vertexB, double length) throws PlanItException {
        this.id = generateEdgeId();
        this.vertexA = vertexA;
        this.vertexB = vertexB;
        this.length = length;
    }

    /**
     * Register EdgeSegment.
     * 
     * If there already exists an edgeSegment for that direction it is replaced and
     * returned
     * 
     * @param edgeSegment EdgeSegment to be registered
     * @param directionAB direction of travel
     * @return replaced LinkSegment
     * @throws PlanItException thrown if there is an error
     */
    protected EdgeSegment registerEdgeSegment(EdgeSegment edgeSegment, boolean directionAB) throws PlanItException {
        if (edgeSegment.getParentEdge().getId() != getId()) {
            throw new PlanItException(
                    "Inconsistency between link segment parent link and link it is being registered on");
        }
        EdgeSegment currentEdgeSegment = directionAB ? edgeSegmentAB : edgeSegmentBA;
        if (directionAB) {
            this.edgeSegmentAB = edgeSegment;
        } else {
            this.edgeSegmentBA = edgeSegment;
        }
        return currentEdgeSegment;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addInputProperty(String key, Object value) {
        if (inputProperties == null) {
            inputProperties = new HashMap<String, Object>();
        }
        inputProperties.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getInputProperty(String key) {
        return inputProperties.get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getLength() {
        return length;
    }

    // Getters-Setters


    /**
     * {@inheritDoc}
     */
    @Override
    public long getId() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vertex getVertexA() {
        return vertexA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Vertex getVertexB() {
        return vertexB;
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String getName() {
        return name;
    }
    
	/** set the name of this edge
	 * @param name the name of this edge
	 */
	public void setName(String name) {
		this.name = name;
	}

    /**
     * {@inheritDoc}
     */
    @Override
    public EdgeSegment getEdgeSegmentAB() {
        return edgeSegmentAB;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EdgeSegment getEdgeSegmentBA() {
        return edgeSegmentBA;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int compareTo(Edge o) {
        return Long.valueOf(id).compareTo(o.getId());
    }
}
