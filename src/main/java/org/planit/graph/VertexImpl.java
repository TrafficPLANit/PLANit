package org.planit.graph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.opengis.geometry.DirectPosition;
import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;

/**
 * vertex representation connected to one or more entry and exit edges
 *
 * @author markr
 *
 */
public class VertexImpl implements Vertex {

  /** generated UID */
  private static final long serialVersionUID = 2165199386965239623L;

  // Protected

  /**
   * generate unique node id
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @return nodeId
   */
  protected static int generateVertexId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Vertex.class);
  }

  /**
   * Unique internal identifier
   */
  protected final long id;

  /**
   * External identifier used in input files
   */
  protected Object externalId;

  /**
   * generic input property storage
   */
  protected Map<String, Object> inputProperties = null;

  /**
   * Centre point geometry which is coordinate reference system aware
   */
  protected DirectPosition centrePointGeometry;

  /**
   * Edges of this vertex
   */
  protected final Set<Edge> edges = new TreeSet<Edge>();

  /**
   * Entry edge segments which connect to this vertex
   */
  protected final Set<EdgeSegment> entryEdgeSegments = new TreeSet<EdgeSegment>();

  /**
   * Exit edge segments which connect to this vertex
   */
  protected final Set<EdgeSegment> exitEdgeSegments = new TreeSet<EdgeSegment>();

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected VertexImpl(final IdGroupingToken groupId) {
    this.id = generateVertexId(groupId);
  }

  // Public

  /**
   * Collect the geometry of the point location of this vertex
   * 
   * @return direct position reflecting point location
   */
  public DirectPosition getCentrePointGeometry() {
    return centrePointGeometry;
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public void setCentrePointGeometry(final DirectPosition centrePointGeometry) {
    this.centrePointGeometry = centrePointGeometry;
  }

  // Getters-Setters

  /**
   * #{@inheritDoc}
   */
  @Override
  public long getId() {
    return id;
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
  public void setExternalId(final Object externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasExternalId() {
    return (externalId != null);
  }

  /**
   * Add a property from the original input that is not part of the readily available members
   *
   * @param key   key (name) of the input property
   * @param value value of the input property
   */
  public void addInputProperty(final String key, final Object value) {
    if (inputProperties == null) {
      inputProperties = new HashMap<String, Object>();
    }
    inputProperties.put(key, value);
  }

  /**
   * Get input property by its key
   *
   * @param key the key of the input property
   * @return value the value of the input property
   */
  public Object getInputProperty(final String key) {
    return inputProperties.get(key);
  }

  /**
   * Compare vertices by their id
   *
   * @param o Vertex object to be compared to this one
   * @return result of comparison
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(final Vertex o) {
    return Long.valueOf(id).compareTo(o.getId());
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public boolean addEdge(final Edge edge) {
    return edges.add(edge);
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public boolean removeEdge(final Edge edge) {
    return edges.remove(edge);
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public Set<Edge> getEdges() {
    return edges;
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public Set<EdgeSegment> getEntryEdgeSegments() {
    return this.entryEdgeSegments;
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public Set<EdgeSegment> getExitEdgeSegments() {
    return this.exitEdgeSegments;
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public boolean addEdgeSegment(final EdgeSegment edgeSegment) throws PlanItException {
    if (edgeSegment.getUpstreamVertex().getId() == getId()) {
      return exitEdgeSegments.add(edgeSegment);
    } else if (edgeSegment.getDownstreamVertex().getId() == getId()) {
      return entryEdgeSegments.add(edgeSegment);
    }
    throw new PlanItException(String.format("edge segment %s (id:%d) does not have this vertex %s (%d) on either end", edgeSegment.getExternalId(), edgeSegment.getId()));
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public boolean removeEdgeSegment(final EdgeSegment edgeSegment) throws PlanItException {
    if (edgeSegment.getUpstreamVertex().getId() == getId()) {
      return exitEdgeSegments.remove(edgeSegment);
    } else if (edgeSegment.getDownstreamVertex().getId() == getId()) {
      return entryEdgeSegments.remove(edgeSegment);
    }
    throw new PlanItException(String.format("edge segment %s (id:%d) does not have this vertex %s (%d) on either end", edgeSegment.getExternalId(), edgeSegment.getId()));
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public boolean hasExitEdgeSegments() {
    return exitEdgeSegments.isEmpty();
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public boolean hasEntryEdgeSegments() {
    return entryEdgeSegments.isEmpty();
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public int getNumberOfEdges() {
    return edges.size();
  }

}
