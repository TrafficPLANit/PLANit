package org.planit.graph;

import java.util.logging.Logger;

import org.planit.utils.exceptions.PlanItException;
import org.planit.utils.graph.DirectedEdge;
import org.planit.utils.graph.EdgeSegment;
import org.planit.utils.graph.EdgeSegmentFactory;
import org.planit.utils.graph.EdgeSegments;
import org.planit.utils.id.IdGroupingToken;

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
   * Copy constructor
   * 
   * @param edgeSegmentsImpl top copy
   */
  public EdgeSegmentsImpl(EdgeSegmentsImpl edgeSegmentsImpl) {
    super(edgeSegmentsImpl);
    this.edgeSegmentFactory = edgeSegmentsImpl.edgeSegmentFactory;
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
    return new EdgeSegmentsImpl(this);
  }

}
