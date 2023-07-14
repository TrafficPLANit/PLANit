package org.goplanit.service.routed;

import org.goplanit.utils.id.IdGroupingToken;
import org.goplanit.utils.service.routed.RoutedTripDeparture;
import org.goplanit.utils.service.routed.RoutedTripFrequency;
import org.goplanit.utils.service.routed.RoutedTripsFrequency;

import java.util.function.BiConsumer;

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
   * @param mapper to use for tracking mapping between original and copied entity (may be null)
   */
  public RoutedTripsFrequencyImpl(RoutedTripsFrequencyImpl other, boolean deepCopy, BiConsumer<RoutedTripFrequency, RoutedTripFrequency> mapper) {
    super(other, deepCopy, mapper);
    setFactory(
            new RoutedTripFrequencyFactoryImpl(other.getFactory().getIdGroupingToken(), this));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsFrequencyImpl shallowClone() {
    return new RoutedTripsFrequencyImpl(this, false, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsFrequencyImpl deepClone() {
    return new RoutedTripsFrequencyImpl(this, true, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripsFrequencyImpl deepCloneWithMapping(BiConsumer<RoutedTripFrequency, RoutedTripFrequency> mapper) {
    return new RoutedTripsFrequencyImpl(this, true, mapper);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public RoutedTripFrequencyFactoryImpl getFactory() {
    return (RoutedTripFrequencyFactoryImpl) super.getFactory();
  }

}
