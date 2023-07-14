package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.service.routed.RoutedTripSchedule;
import org.goplanit.utils.service.routed.RoutedTripScheduleFactory;
import org.goplanit.utils.service.routed.RoutedTripsSchedule;

/**
 * Factory for creating routed trips that are schedule based
 * 
 * @author markr
 */
public class RoutedTripScheduleFactoryImpl extends RoutedTripFactoryImpl<RoutedTripSchedule> implements RoutedTripScheduleFactory {


  /**
   * {@inheritDoc}
   */
  @Override
  protected RoutedTripSchedule createNew() {
    return new RoutedTripScheduleImpl(getIdGroupingToken());
  }

  /**
   * Constructor
   * 
   * @param tokenId     to use
   * @param routedTrips to use
   */
  protected RoutedTripScheduleFactoryImpl(final IdGroupingToken tokenId, final RoutedTripsSchedule routedTrips) {
    super(tokenId, routedTrips);
  }

}
