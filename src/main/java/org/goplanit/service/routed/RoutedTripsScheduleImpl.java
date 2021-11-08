package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;

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
    setFactory(new RoutedTripScheduleFactory(tokenId, this));
  }

  /**
   * Copy constructor
   * 
   * @param routedTripsScheduleImpl to copy
   */
  public RoutedTripsScheduleImpl(RoutedTripsScheduleImpl routedTripsScheduleImpl) {
    super(routedTripsScheduleImpl);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsScheduleImpl clone() {
    return new RoutedTripsScheduleImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripScheduleFactory getFactory() {
    return (RoutedTripScheduleFactory) super.getFactory();
  }

}
