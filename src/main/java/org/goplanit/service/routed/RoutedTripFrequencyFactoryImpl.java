package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.service.routed.RoutedTripFrequency;
import org.goplanit.utils.service.routed.RoutedTripFrequencyFactory;
import org.goplanit.utils.service.routed.RoutedTripsFrequency;

/**
 * Factory for creating routed trips that are frequency based
 * 
 * @author markr
 */
public class RoutedTripFrequencyFactoryImpl extends RoutedTripFactoryImpl<RoutedTripFrequency> implements RoutedTripFrequencyFactory {


  /**
   * {@inheritDoc}
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
  protected RoutedTripFrequencyFactoryImpl(final IdGroupingToken tokenId, final RoutedTripsFrequency routedTrips) {
    super(tokenId, routedTrips);
  }

}
