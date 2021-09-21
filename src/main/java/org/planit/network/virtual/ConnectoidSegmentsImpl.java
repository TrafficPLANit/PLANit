package org.planit.network.virtual;

import org.planit.utils.id.IdGenerator;
import org.planit.utils.id.IdGroupingToken;
import org.planit.utils.id.ManagedIdEntitiesImpl;
import org.planit.utils.network.virtual.ConnectoidSegment;
import org.planit.utils.network.virtual.ConnectoidSegmentFactory;
import org.planit.utils.network.virtual.ConnectoidSegments;

/**
 * 
 * Connectoid segments container implementation
 * 
 * @author markr
 *
 */
public class ConnectoidSegmentsImpl extends ManagedIdEntitiesImpl<ConnectoidSegment> implements ConnectoidSegments {

  /** factory to use */
  private final ConnectoidSegmentFactory connectoidSegmentFactory;

  /**
   * Constructor
   * 
   * @param groupId to use for creating ids for instances
   */
  public ConnectoidSegmentsImpl(final IdGroupingToken groupId) {
    super(ConnectoidSegment::getId, ConnectoidSegment.EDGE_SEGMENT_ID_CLASS);
    this.connectoidSegmentFactory = new ConnectoidSegmentFactoryImpl(groupId, this);
  }

  /**
   * Constructor
   * 
   * @param groupId                  to use for creating ids for instances
   * @param connectoidSegmentFactory the factory to use
   */
  public ConnectoidSegmentsImpl(final IdGroupingToken groupId, ConnectoidSegmentFactory connectoidSegmentFactory) {
    super(ConnectoidSegment::getId, ConnectoidSegment.EDGE_SEGMENT_ID_CLASS);
    this.connectoidSegmentFactory = connectoidSegmentFactory;
  }

  /**
   * Copy constructor
   * 
   * @param connectoidSegmentImpl to copy
   */
  public ConnectoidSegmentsImpl(ConnectoidSegmentsImpl connectoidSegmentImpl) {
    super(connectoidSegmentImpl);
    this.connectoidSegmentFactory = connectoidSegmentImpl.connectoidSegmentFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegmentFactory getFactory() {
    return connectoidSegmentFactory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void recreateIds(boolean resetManagedIdClass) {
    /* always reset the additional connectoid segment id class */
    IdGenerator.reset(getFactory().getIdGroupingToken(), ConnectoidSegment.CONNECTOID_SEGMENT_ID_CLASS);

    super.recreateIds(resetManagedIdClass);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegmentsImpl clone() {
    return new ConnectoidSegmentsImpl(this);
  }

  /**
   * clear the container
   */
  public void clear() {
    getMap().clear();
  }

}
