package org.planit.service.routed;

import org.planit.utils.id.IdGroupingToken;

/**
 * Factory for creating routed trips that are schedule based
 * 
 * @author markr
 */
public class RoutedTripScheduleFactory extends RoutedTripFactory<RoutedTripSchedule> {

  /**
   * Create a newly created instance without registering on the container
   * 
   * @return created routed trip instance
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
  protected RoutedTripScheduleFactory(final IdGroupingToken tokenId, final RoutedTripsSchedule routedTrips) {
    super(tokenId, routedTrips);
  }

}
