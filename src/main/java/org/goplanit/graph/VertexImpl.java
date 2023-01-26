package org.goplanit.graph;

import java.util.ArrayList;
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
public class VertexImpl<E extends Edge> extends GraphEntityImpl implements Vertex {

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
   * Edges of this vertex. List used to ensure fixed order in iterating and minimal memory overhead
   */
  protected final ArrayList<E> edges = new ArrayList<>(2);

  /**
   * Constructor
   * 
   * @param groupId, contiguous id generation within this group for instances of this class
   */
  protected VertexImpl(final IdGroupingToken groupId) {
    super(groupId, VERTEX_ID_CLASS);
  }

  /**
   * Constructor
   * 
   * @param groupId contiguous id generation within this group for instances of this class
   * @param vertexIdClass to use
   */
  protected VertexImpl(final IdGroupingToken groupId, final Class<? extends Vertex> vertexIdClass) {
    super(groupId, vertexIdClass);
  }

  /**
   * Constructor. Only to be used when not reliant on contiguous id generation within enclosing container
   * 
   * @param id to use
   */
  protected VertexImpl(long id) {
    super(id);
  }

  /**
   * Copy constructor.
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  protected VertexImpl(VertexImpl<E> other, boolean deepCopy) {
    super(other, deepCopy);
    edges.addAll(other.edges); // edges not owned, so not deep copied

    setPosition(deepCopy ? (Point) other.getPosition().copy() : other.getPosition());
    if (other.inputProperties != null && !other.inputProperties.isEmpty()) {
      if(deepCopy) {
        CloneUtils.deepCloneFromTo(other.inputProperties, this.inputProperties);
      }else{
        this.inputProperties.putAll(other.inputProperties);
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
      inputProperties = new HashMap<>();
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
  @SuppressWarnings("unchecked")
  @Override
  public boolean addEdge(final Edge edge) {
    if (edges.contains(edge)) {
      return false;
    }

    edges.add((E) edge);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean removeEdge(final long edgeId) {
    return edges.removeIf(e -> e.getId() == edgeId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeAllEdges(){
    edges.clear();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Collection<? extends E> getEdges() {
    return Collections.unmodifiableCollection(edges);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VertexImpl<E> clone() {
    return new VertexImpl<>(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public VertexImpl<E> deepClone() {
    return new VertexImpl<>(this, true);
  }
}
