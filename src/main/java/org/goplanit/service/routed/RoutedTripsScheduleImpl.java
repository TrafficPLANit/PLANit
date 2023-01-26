package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.service.routed.RoutedTripSchedule;
import org.goplanit.utils.service.routed.RoutedTripsSchedule;

/**
 * Implementation of the RoutedTripsSchedule interface.
 * 
 * @author markr
 */
public class RoutedTripsScheduleImpl extends RoutedTripsImpl<RoutedTripSchedule> implements RoutedTripsSchedule {

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  protected RoutedTripsScheduleImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    setFactory(new RoutedTripScheduleFactoryImpl(tokenId, this));
  }

  /**
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedTripsScheduleImpl(RoutedTripsScheduleImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setFactory(new RoutedTripScheduleFactoryImpl(other.getFactory().getIdGroupingToken(), this));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsScheduleImpl clone() {
    return new RoutedTripsScheduleImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsScheduleImpl deepClone() {
    return new RoutedTripsScheduleImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripScheduleFactoryImpl getFactory() {
    return (RoutedTripScheduleFactoryImpl) super.getFactory();
  }

}
