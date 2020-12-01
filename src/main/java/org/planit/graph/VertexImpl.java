package org.planit.graph;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Point;
import org.planit.utils.graph.Edge;
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

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(VertexImpl.class.getCanonicalName());

  /** generated UID */
  private static final long serialVersionUID = -2877566769607366608L;

  // Protected

  /**
   * Unique internal identifier
   */
  protected long id;

  /**
   * External identifier used in input files
   */
  protected String externalId;

  /**
   * The xml Id for this vertex
   */
  private String xmlId;

  /**
   * generic input property storage
   */
  protected Map<String, Object> inputProperties = null;

  /**
   * Position of the vertex
   */
  protected Point position;

  /**
   * Edges of this vertex
   */
  protected final Map<Long, Edge> edges = new HashMap<Long, Edge>();

  /**
   * generate unique node id
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @return nodeId
   */
  protected static long generateVertexId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Vertex.class);
  }

  /**
   * Set id on vertex
   * 
   * @param id to set
   */
  protected void setId(Long id) {
    this.id = id;
  }

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected VertexImpl(final IdGroupingToken groupId) {
    this.id = generateVertexId(groupId);
  }

  /**
   * Copy constructor (for now input properties are NOT copied, because a shallow copy of contents is dangerous). Geometry is deep copied, edges are not because they are not owned
   * by the vertex.
   * 
   * @param vertexImpl to copy
   */
  protected VertexImpl(VertexImpl vertexImpl) {
    setId(vertexImpl.getId());
    setXmlId(vertexImpl.getXmlId());
    setExternalId(vertexImpl.getExternalId());
    setPosition((Point) vertexImpl.getPosition().clone());
    edges.putAll(vertexImpl.edges);
    inputProperties = null; // not copied, shallow copy of objects is dangerous
  }

  // Public

  /**
   * #{@inheritDoc}
   */
  @Override
  public Point getPosition() {
    return position;
  }

  /**
   * #{@inheritDoc}
   */
  @Override
  public void setPosition(final Point position) {
    this.position = position;
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
  public String getExternalId() {
    return externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setExternalId(final String externalId) {
    this.externalId = externalId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getXmlId() {
    return this.xmlId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setXmlId(final String xmlId) {
    this.xmlId = xmlId;
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
   * {@inheritDoc}
   */
  @Override
  public boolean addEdge(final Edge edge) {
    return edges.put(edge.getId(), edge) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeEdge(final Edge edge) {
    return removeEdge(edge.getId());
  }

  @Override
  public boolean removeEdge(final long edgeId) {
    return edges.remove(edgeId) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<Edge> getEdges() {
    return edges.values();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public int getNumberOfEdges() {
    return edges.size();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Set<Edge> getEdges(Vertex otherVertex) {
    Set<Edge> edges = new HashSet<Edge>();
    for (Edge edge : getEdges()) {
      if (edge.getVertexA().getId() == this.getId() && edge.getVertexB().getId() == otherVertex.getId()) {
        edges.add(edge);
      } else if (edge.getVertexB().getId() == this.getId() && edge.getVertexA().getId() == otherVertex.getId()) {
        edges.add(edge);
      }
    }
    return edges;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VertexImpl clone() {
    return new VertexImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean validate() {
    for (Edge edge : getEdges()) {
      if (!edge.hasVertex(this)) {
        LOGGER
            .warning(String.format("edge (id:%d) does not contain vertex (id:%d externalId:%s) even though the vertex is connected to it", edge.getId(), getId(), getExternalId()));
        return false;
      }
    }
    return true;
  }

}
