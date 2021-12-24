package org.goplanit.graph;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.Vertex;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.CloneUtils;
import org.locationtech.jts.geom.Point;

/**
 * Vertex representation connected to one or more entry and exit edges
 *
 * @author markr
 *
 */
public class VertexImpl extends GraphEntityImpl implements Vertex {

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
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected VertexImpl(final IdGroupingToken groupId) {
    super(groupId, VERTEX_ID_CLASS);
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
      for (var entry : vertexImpl.inputProperties.entrySet()) {
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
  public boolean removeEdge(final long edgeId) {
    return edges.remove(edgeId) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<? extends Edge> getEdges() {
    return Collections.unmodifiableCollection(edges.values());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VertexImpl clone() {
    return new VertexImpl(this);
  }

}
