package org.planit.service.routed;

import org.planit.utils.id.IdGroupingToken;

/**
 * Implementation of the RoutedTripsSchedule interface.
 * 
 * @author markr
 */
public class RoutedTripsScheduleImpl extends RoutedTripsImpl<RoutedTripSchedule> implements RoutedTripsSchedule {

  /**
   * Constructor
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
