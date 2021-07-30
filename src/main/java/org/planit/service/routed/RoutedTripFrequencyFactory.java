package org.planit.service.routed;

import org.planit.utils.id.IdGroupingToken;

/**
 * Factory for creating routed trips that are frequency based
 * 
 * @author markr
 */
public class RoutedTripFrequencyFactory extends RoutedTripFactory<RoutedTripFrequency> {

  /**
   * Create a newly created instance without registering on the container
   * 
   * @return created routed trip instance
   */
  @Override
  protected RoutedTripFrequency createNew() {
    return new RoutedTripFrequencyImpl(getIdGroupingToken());
  }

  /**
   * Constructor
   * 
   * @param tokenId     to use
   * @param routedTrips to use
   */
  protected RoutedTripFrequencyFactory(final IdGroupingToken tokenId, final RoutedTripsFrequency routedTrips) {
    super(tokenId, routedTrips);
  }

}
