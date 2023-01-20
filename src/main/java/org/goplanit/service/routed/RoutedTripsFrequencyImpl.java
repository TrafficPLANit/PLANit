package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.service.routed.RoutedTripFrequency;
import org.goplanit.utils.service.routed.RoutedTripsFrequency;

/**
 * Implementation of the RoutedTripsFrequency interface.
 * 
 * @author markr
 */
public class RoutedTripsFrequencyImpl extends RoutedTripsImpl<RoutedTripFrequency> implements RoutedTripsFrequency {

  /**
   * Constructor
   * 
   * @param tokenId to use
   */
  protected RoutedTripsFrequencyImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    setFactory(new RoutedTripFrequencyFactoryImpl(tokenId, this));
  }

  /**
   * Copy constructor
   * 
   * @param routedTripsFrequencyImpl to copy
   */
  public RoutedTripsFrequencyImpl(RoutedTripsFrequencyImpl routedTripsFrequencyImpl) {
    super(routedTripsFrequencyImpl);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsFrequencyImpl clone() {
    return new RoutedTripsFrequencyImpl(this);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripFrequencyFactoryImpl getFactory() {
    return (RoutedTripFrequencyFactoryImpl) super.getFactory();
  }

}
