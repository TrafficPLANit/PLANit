package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;

/**
 * Implementation of the RoutedTripsFrequency interface.
 * 
 * @author markr
 */
public class RoutedTripsFrequencyImpl extends RoutedTripsImpl<RoutedTripFrequency> implements RoutedTripsFrequency {

  /**
   * Constructor
   */
  protected RoutedTripsFrequencyImpl(final IdGroupingToken tokenId) {
    super(tokenId);
    setFactory(new RoutedTripFrequencyFactory(tokenId, this));
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
  public RoutedTripFrequencyFactory getFactory() {
    return (RoutedTripFrequencyFactory) super.getFactory();
  }

}
