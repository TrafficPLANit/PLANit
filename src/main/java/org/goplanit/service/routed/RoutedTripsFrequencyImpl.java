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
   * Copy constructor, also creates new factory with this as its underlying container
   * 
   * @param other to copy
   * @param deepCopy when true, create a deep copy, shallow copy otherwise
   */
  public RoutedTripsFrequencyImpl(RoutedTripsFrequencyImpl other, boolean deepCopy) {
    super(other, deepCopy);
    setFactory(
            new RoutedTripFrequencyFactoryImpl(other.getFactory().getIdGroupingToken(), this));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsFrequencyImpl shallowClone() {
    return new RoutedTripsFrequencyImpl(this, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsFrequencyImpl deepClone() {
    return new RoutedTripsFrequencyImpl(this, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripFrequencyFactoryImpl getFactory() {
    return (RoutedTripFrequencyFactoryImpl) super.getFactory();
  }

}
