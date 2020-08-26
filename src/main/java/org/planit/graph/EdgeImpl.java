package org.planit.graph;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * Edge class connecting two vertices via some geometry. Each edge has one or two underlying edge segments in a particular direction which may carry additional information for each
 * particular direction of the edge.
 *
 * @author markr
 *
 */
public class EdgeImpl implements Edge {

  // Protected

  /** generated UID */
  private static final long serialVersionUID = -3061186642253968991L;

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
   * @param groupId, contiguous id generation within this group for instances of this class
   * @return id of this Edge object
   */
  protected static long generateEdgeId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Edge.class);
  }

  // Public

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   * @param length   length of the link
   * @throws PlanItException thrown if there is an error
   */
  protected EdgeImpl(final IdGroupingToken groupId, final Vertex vertexA, final Vertex vertexB, final double length) throws PlanItException {
    this.id = generateEdgeId(groupId);
    this.vertexA = vertexA;
    this.vertexB = vertexB;
    this.length = length;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment registerEdgeSegment(final EdgeSegment edgeSegment, final boolean directionAB) throws PlanItException {
    PlanItException.throwIf(edgeSegment.getParentEdge().getId() != getId(), "inconsistency between link segment parent link and link it is being registered on");

    final EdgeSegment currentEdgeSegment = directionAB ? edgeSegmentAB : edgeSegmentBA;
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
  public void addInputProperty(final String key, final Object value) {
    if (inputProperties == null) {
      inputProperties = new HashMap<String, Object>();
    }
    inputProperties.put(key, value);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getInputProperty(final String key) {
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

  /**
   * {@inheritDoc}
   */
  @Override  
  public void setName(final String name) {
    this.name = name;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getEdgeSegmentAb() {
    return edgeSegmentAB;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegment getEdgeSegmentBa() {
    return edgeSegmentBA;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int compareTo(final Edge other) {
    return Long.valueOf(id).compareTo(other.getId());
  }

}
