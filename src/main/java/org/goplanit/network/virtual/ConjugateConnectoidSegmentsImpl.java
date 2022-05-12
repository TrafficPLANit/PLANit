package org.goplanit.network.virtual;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.layer.physical.LinkSegment;
import org.goplanit.utils.network.virtual.ConjugateConnectoidSegment;
import org.goplanit.utils.network.virtual.ConjugateConnectoidSegmentFactory;
import org.goplanit.utils.network.virtual.ConjugateConnectoidSegments;

/**
 * 
 * Conjugate connectoid segments primary managed container implementation
 * 
 * @author markr
 *
 */
public class ConjugateConnectoidSegmentsImpl extends ManagedIdEntitiesImpl<ConjugateConnectoidSegment> implements ConjugateConnectoidSegments {

  /** factory to use */
  private final ConjugateConnectoidSegmentFactory factory;

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   */
  public ConjugateConnectoidSegmentsImpl(final IdGroupingToken groupId) {
    super(ConjugateConnectoidSegment::getId, LinkSegment.EDGE_SEGMENT_ID_CLASS);
    this.factory = new ConjugateConnectoidSegmentFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   *
   * @param groupId to use for creating ids for instances
   * @param factory the factory to use
   */
  public ConjugateConnectoidSegmentsImpl(final IdGroupingToken groupId, ConjugateConnectoidSegmentFactory factory) {
    super(ConjugateConnectoidSegment::getId, LinkSegment.EDGE_SEGMENT_ID_CLASS);
    this.factory = factory;
  }

  /**
   * Copy constructor
   *
   * @param conjugateLinkSegmentsImpl to copy
   */
  public ConjugateConnectoidSegmentsImpl(ConjugateConnectoidSegmentsImpl conjugateLinkSegmentsImpl) {
    super(conjugateLinkSegmentsImpl);
    this.factory = conjugateLinkSegmentsImpl.factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidSegmentFactory getFactory() {
    return factory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConjugateConnectoidSegmentsImpl clone() {
    return new ConjugateConnectoidSegmentsImpl(this);
  }

}
