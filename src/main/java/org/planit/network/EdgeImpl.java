package org.planit.network;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nonnull;

import org.planit.exceptions.PlanItException;
import org.planit.utils.misc.IdGenerator;
import org.planit.utils.network.Edge;

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
    protected VertexImpl vertexA = null;
    
    /**
     * Vertex B
     */
    protected VertexImpl vertexB = null;
    
    /**
     * Length of edge
     */
    protected double length;

    /**
     * Edge segment A to B direction
     */
    protected EdgeSegmentImpl edgeSegmentAB = null;
    /**
     * Edge segment B to A direction
     */
    protected EdgeSegmentImpl edgeSegmentBA = null;

    /**
     * Generate edge id
     * 
     * @return id of this Edge object
     */
    protected static long generateEdgeId() {
        return IdGenerator.generateId(EdgeImpl.class);
    }

    // Public

    /**
     * Constructor which injects link lengths directly
     * 
     * @param vertexA
     *            first vertex in the link
     * @param vertexB
     *            second vertex in the link
     * @param length
     *            length of the link
     * @throws PlanItException
     *             thrown if there is an error
     */
    protected EdgeImpl(@Nonnull VertexImpl vertexA, @Nonnull VertexImpl vertexB, double length) throws PlanItException {
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
     * @param edgeSegment
     *            EdgeSegment to be registered
     * @param directionAB
     *            direction of travel
     * @return replaced LinkSegment
     * @throws PlanItException
     *             thrown if there is an error
     */
    protected EdgeSegmentImpl registerEdgeSegment(EdgeSegmentImpl edgeSegment, boolean directionAB) throws PlanItException {
        if (edgeSegment.getParentEdge().getId() != getId()) {
            throw new PlanItException(
                    "Inconsistency between link segment parent link and link it is being registered on");
        }
        EdgeSegmentImpl currentEdgeSegment = directionAB ? edgeSegmentAB : edgeSegmentBA;
        if (directionAB) {
            this.edgeSegmentAB = edgeSegment;
        } else {
            this.edgeSegmentBA = edgeSegment;
        }
        return currentEdgeSegment;
    }

    /**
     * Add a property from the original input that is not part of the readily
     * available link members
     * 
     * @param key
     *            key (name) of input property
     * @param value
     *            value of input property
     */
    public void addInputProperty(String key, Object value) {
        if (inputProperties == null) {
            inputProperties = new HashMap<String, Object>();
        }
        inputProperties.put(key, value);
    }

    /**
     * Get input property by its key
     * 
     * @param key
     *            key of input property
     * @return value retrieved value of input property
     */
    public Object getInputProperty(String key) {
        return inputProperties.get(key);
    }

    /**
     * Return length of this edge
     * 
     * @return length of this edge
     */
    public double getLength() {
        return length;
    }

    // Getters-Setters

    /**
     * Return id of this Edge object
     * 
     * @return id of this Edge object
     */
    public long getId() {
        return id;
    }

    public VertexImpl getVertexA() {
        return vertexA;
    }

    public VertexImpl getVertexB() {
        return vertexB;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public EdgeSegmentImpl getEdgeSegmentAB() {
        return edgeSegmentAB;
    }

    public EdgeSegmentImpl getEdgeSegmentBA() {
        return edgeSegmentBA;
    }

    @Override
    public int compareTo(Edge o) {
        return Long.valueOf(id).compareTo(o.getId());
    }
}
