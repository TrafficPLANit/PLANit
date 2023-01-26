package org.goplanit.graph.directed;

import java.util.logging.Logger;

import org.goplanit.graph.GraphEntitiesImpl;
import org.goplanit.utils.exceptions.PlanItException;
import org.goplanit.utils.graph.directed.DirectedEdge;
import org.goplanit.utils.graph.directed.EdgeSegment;
import org.goplanit.utils.graph.directed.EdgeSegmentFactory;
import org.goplanit.utils.graph.directed.EdgeSegments;
import org.goplanit.utils.id.IdGroupingToken;

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
   */
  public EdgeSegmentsImpl(EdgeSegmentsImpl other, boolean deepCopy) {
    super(other, deepCopy);
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
  public EdgeSegmentsImpl clone() {
    return new EdgeSegmentsImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EdgeSegmentsImpl deepClone() {
    return new EdgeSegmentsImpl(this, true);
  }

}
