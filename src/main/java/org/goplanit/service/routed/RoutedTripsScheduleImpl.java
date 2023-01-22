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
   * @param routedTripsScheduleImpl to copy
   */
  public RoutedTripsScheduleImpl(RoutedTripsScheduleImpl routedTripsScheduleImpl) {
    super(routedTripsScheduleImpl);
    setFactory(new RoutedTripScheduleFactoryImpl(routedTripsScheduleImpl.getFactory().getIdGroupingToken(), this));
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
  public RoutedTripScheduleFactoryImpl getFactory() {
    return (RoutedTripScheduleFactoryImpl) super.getFactory();
  }

}
