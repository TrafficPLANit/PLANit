package org.planit.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

import org.locationtech.jts.geom.Point;
import org.planit.utils.graph.Edge;
import org.planit.utils.graph.Vertex;
import org.planit.utils.id.ExternalIdAbleImpl;
import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.misc.CloneUtils;

/**
 * vertex representation connected to one or more entry and exit edges
 *
 * @author markr
 *
 */
public class VertexImpl extends ExternalIdAbleImpl implements Vertex {

  /** the logger */
  private static final Logger LOGGER = Logger.getLogger(VertexImpl.class.getCanonicalName());

  /** generated UID */
  private static final long serialVersionUID = -2877566769607366608L;

  // Protected

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
   * generate unique vertex id
   *
   * @param groupId, contiguous id generation within this group for instances of this class
   * @return vertex id generated
   */
  protected static long generateVertexId(final IdGroupingToken groupId) {
    return IdGenerator.generateId(groupId, Vertex.class);
  }

  /**
   * set the internal id and expose to package
   * 
   * @param id to set
   */
  protected void setId(long id) {
    super.setId(id);
  }

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected VertexImpl(final IdGroupingToken groupId) {
    super(generateVertexId(groupId));
  }

  /**
   * Copy constructor. Geometry and input properties are deep copied, edges are not because they are not owned by this class by the vertex.
   * 
   * @param vertexImpl to copy
   */
  protected VertexImpl(VertexImpl vertexImpl) {
    super(vertexImpl);
    setPosition((Point) vertexImpl.getPosition().copy());
    edges.putAll(vertexImpl.edges);
    if (vertexImpl.inputProperties != null && !vertexImpl.inputProperties.isEmpty()) {
      for (Entry<String, Object> entry : vertexImpl.inputProperties.entrySet()) {
        addInputProperty(new String(entry.getKey()), CloneUtils.clone(entry.getValue()));
      }
    }
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
  public Object getInputProperty(final String key) {
    return inputProperties.get(key);
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
    return Collections.unmodifiableCollection(edges.values());
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
