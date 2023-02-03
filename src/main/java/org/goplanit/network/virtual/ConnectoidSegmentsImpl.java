package org.goplanit.network.virtual;

import org.goplanit.utils.id.IdGenerator;
import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.id.ManagedIdEntitiesImpl;
import org.goplanit.utils.network.virtual.ConnectoidSegment;
import org.goplanit.utils.network.virtual.ConnectoidSegmentFactory;
import org.goplanit.utils.network.virtual.ConnectoidSegments;

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
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public ConnectoidSegmentsImpl(ConnectoidSegmentsImpl other, boolean deepCopy) {
    super(other, deepCopy);
    this.connectoidSegmentFactory =
            new ConnectoidSegmentFactoryImpl(other.connectoidSegmentFactory.getIdGroupingToken(), this);
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
  public ConnectoidSegmentsImpl shallowClone() {
    return new ConnectoidSegmentsImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConnectoidSegmentsImpl deepClone() {
    return new ConnectoidSegmentsImpl(this, true);
  }

  /**
   * clear the container
   */
  public void clear() {
    getMap().clear();
  }

}
