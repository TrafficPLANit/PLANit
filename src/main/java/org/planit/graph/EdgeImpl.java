package org.planit.graph;

import java.util.HashMap;
import java.util.Map;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
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
  private long id;

  /**
   * Vertex A
   */
  private Vertex vertexA = null;

  /**
   * Vertex B
   */
  private Vertex vertexB = null;

  /**
   * External Id of the physical link
   */
  protected Object externalId;

  /**
   * Generic input property storage
   */
  protected Map<String, Object> inputProperties = null;

  /**
   * Name of the edge
   */
  protected String name = null;

  /**
   * Length of edge
   */
  protected double lengthInKm;

  /**
   * Generate edge id
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @return id of this Edge object
   */
  protected static long generateEdgeId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Edge.class);
  }

  /**
   * set id on this edge
   * 
   * @param id to set
   */
  protected void setId(long id) {
    this.id = id;
  }

  /**
   * set vertex B
   * 
   * @param vertexB to set
   */
  protected void setVertexB(Vertex vertexB) {
    this.vertexB = vertexB;
  }

  /**
   * set vertex A
   * 
   * @param vertexA to set
   */
  protected void setVertexA(Vertex vertexA) {
    this.vertexA = vertexA;
  }

  /**
   * Constructor which injects link lengths directly
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @param vertexA  first vertex in the link
   * @param vertexB  second vertex in the link
   * @param lengthKm length of the link
   * @throws PlanItException thrown if there is an error
   */
  protected EdgeImpl(final IdGroupingToken groupId, final Vertex vertexA, final Vertex vertexB, final double lengthKm) throws PlanItException {
    setId(generateEdgeId(groupId));
    setVertexA(vertexA);
    setVertexB(vertexB);
    setLengthKm(lengthKm);
  }

  /**
   * Copy constructor
   * 
   * @param edgeImpl to copy
   */
  protected EdgeImpl(EdgeImpl edgeImpl) {
    setId(edgeImpl.getId());
    setVertexA(edgeImpl.getVertexA());
    setVertexB(edgeImpl.getVertexB());
    setLengthKm(edgeImpl.getLengthKm());
    setName(edgeImpl.getName());
    if (inputProperties != null) {
      inputProperties = new HashMap<String, Object>(inputProperties);
    }
  }

  // Public

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExternalId(final Object externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Object getExternalId() {
    return externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasExternalId() {
    return (externalId != null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeVertex(Vertex vertex) {
    if (vertex != null) {
      if (getVertexA() != null && getVertexA().getId() == vertex.getId()) {
        return removeVertexA();
      } else if (getVertexB() != null && getVertexB().getId() == vertex.getId()) {
        return removeVertexB();
      }
    }
    return false;
  }

  /**
   * remove vertex B by setting it to null
   * 
   * @return true
   */
  public boolean removeVertexB() {
    setVertexB(null);
    return true;
  }

  /**
   * remove vertex A by setting it to null
   * 
   * @return true
   */
  public boolean removeVertexA() {
    setVertexA(null);
    return true;
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
  public double getLengthKm() {
    return lengthInKm;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setLengthKm(double lengthInKm) {
    this.lengthInKm = lengthInKm;
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
  public int compareTo(final Edge other) {
    return Long.valueOf(id).compareTo(other.getId());
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  public boolean replace(final Vertex vertexToReplace, final Vertex vertexToReplaceWith, boolean updateVertexEdges) throws PlanItException {
    boolean vertexReplaced = false;

    /* replace vertices on edge */
    if (vertexToReplaceWith != null) {
      if (getVertexA() != null && vertexToReplace.getId() == getVertexA().getId()) {
        removeVertex(vertexToReplace);
        setVertexA(vertexToReplaceWith);
        vertexReplaced = true;
      } else if (getVertexB() != null && vertexToReplace.getId() == getVertexB().getId()) {
        removeVertex(vertexToReplace);
        setVertexB(vertexToReplaceWith);
        vertexReplaced = true;
      }
    }

    /* replace edge on vertices */
    if (vertexReplaced && updateVertexEdges) {
      vertexToReplace.removeEdge(this);
      vertexToReplaceWith.addEdge(this);
    }

    return vertexReplaced;
  }

  /**
   * {@inheritDoc}
   * 
   */
  @Override
  protected Edge clone() throws CloneNotSupportedException {
    return new EdgeImpl(this);
  }

}
