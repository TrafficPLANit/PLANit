package org.goplanit.graph.directed;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Logger;

import org.goplanit.graph.GraphEntitiesImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.Edge;
import org.goplanit.utils.graph.GraphEntityDeepCopyMapper;
import org.goplanit.utils.graph.directed.*;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.misc.Pair;

/**
 * Implementation of EdgeSegments interface.
 * 
 * @author markr
 *
 */
public class EdgeSegmentsImpl extends GraphEntitiesImpl<EdgeSegment> implements EdgeSegments {

  /** the logger */
  @SuppressWarnings("unused")
  private static final Logger LOGGER = Logger.getLogger(EdgeSegmentsImpl.class.getCanonicalName());

  /** factory to create edge segment instances */
  private final EdgeSegmentFactory edgeSegmentFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public EdgeSegmentsImpl(final IdGroupingToken groupId) {
    super(EdgeSegment::getId);
    this.edgeSegmentFactory = new EdgeSegmentFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId            to use for creating ids for instances
   * @param edgeSegmentFactory to use
   */
  public EdgeSegmentsImpl(final IdGroupingToken groupId, final EdgeSegmentFactory edgeSegmentFactory) {
    super(EdgeSegment::getId);
    this.edgeSegmentFactory = edgeSegmentFactory;
  }

  /**
   * Copy constructor, also creates a new factory with reference to this container
   * 
   * @param other top copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   * @param biConsumer when deepCopy applied to each original and copy, may be null
   */
  public EdgeSegmentsImpl(EdgeSegmentsImpl other, boolean deepCopy, BiConsumer<EdgeSegment, EdgeSegment> biConsumer) {
    super(other, deepCopy, biConsumer);
    this.edgeSegmentFactory =
            new EdgeSegmentFactoryImpl(other.edgeSegmentFactory.getIdGroupingToken(), this);
  }

  /**
   * {@inheritDoc}
   */
  public void register(final DirectedEdge parentEdge, final EdgeSegment edgeSegment, final boolean directionAB) throws PlanItException {
    parentEdge.registerEdgeSegment(edgeSegment, directionAB);
    register(edgeSegment);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentFactory getFactory() {
    return edgeSegmentFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentsImpl shallowClone() {
    return new EdgeSegmentsImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentsImpl deepClone() {
    return new EdgeSegmentsImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentsImpl deepCloneWithMapping(BiConsumer<EdgeSegment, EdgeSegment> mapper) {
    return new EdgeSegmentsImpl(this, true, mapper);
  }

}
