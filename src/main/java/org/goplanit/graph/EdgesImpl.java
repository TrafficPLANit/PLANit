package org.goplanit.graph;

import org.goplanit.graph.directed.EdgeSegmentsImpl;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.EdgeFactory;
import org.goplanit.utils.graph.Edges;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;

import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Implementation of Edges interface
 * 
 * @author markr
 */
public class EdgesImpl extends GraphEntitiesImpl<Edge> implements Edges {

  /** factory to create edge instances */
  private final EdgeFactory edgeFactory;

  /**
   * Copy constructor, also creates a new factory with reference to this container
   *
   * @param edgesImpl to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param biConsumer when deepCopy applied to each original and copy, may be null
   */
  protected EdgesImpl(EdgesImpl edgesImpl, boolean deepCopy, BiConsumer<Edge, Edge> biConsumer) {
    super(edgesImpl, deepCopy, biConsumer);
    this.edgeFactory = new EdgeFactoryImpl(edgesImpl.edgeFactory.getIdGroupingToken(), this);
  }

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public EdgesImpl(final IdGroupingToken groupId) {
    super(Edge::getId);
    this.edgeFactory = new EdgeFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param edgeFactory to use
   */
  public EdgesImpl(EdgeFactory edgeFactory) {
    super(Edge::getId);
    this.edgeFactory = edgeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeFactory getFactory() {
    return edgeFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgesImpl shallowClone() {
    return new EdgesImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgesImpl deepClone() {
    return new EdgesImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgesImpl deepCloneWithMapping(BiConsumer<Edge, Edge> mapper) {
    return new EdgesImpl(this, true, mapper);
  }

}
